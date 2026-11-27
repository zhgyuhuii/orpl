package com.groma.bootstrap.pipeline;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaTraceListener;
import com.groma.base.engine.GromaWorldContext;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.studio.debug.DapProtocolModels;
import com.groma.studio.debug.DapWebSocketRouter;
import com.groma.studio.debug.VirtualThreadDebugSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 推演会话 × DAP 调试协议桥（修复 DebugEndpoint 占位桩问题，落实建议 1 的"最后一公里"）：
 * <ul>
 *   <li>DebugEndpoint 收到的标准 JSON/DAP 报文 → {@link DapWebSocketRouter#handleDapRequest} 真实路由；</li>
 *   <li>断点命中（过程入口/出口软断点，基于 GromaTraceListener）→ 冻结推演时间基准
 *       （实时模式 {@code TickEventLoopGroup.freezeSession}，手动模式 requestPause）并向 WS 推送 stopped 事件；</li>
 *   <li>恢复（continue / next / stepIn / stepOut / nextSimulationTick）→ 解冻并按动作重新驱动，
 *       单步语义 = 推进一拍后重新冻结并推送新现场。</li>
 * </ul>
 * 线程模型（docs/thread-model.md 单写者铁律）：写者线程上的断点回调<strong>绝不 park</strong>——
 * 冻结只是向事件循环提交请求（拍间生效），挂起现场经不可变快照导出后在虚拟线程上推送 WS 报文；
 * 恢复动作全部由 WS/连接器线程经请求桥提交。
 * <p>
 * 行级断点说明：生成代码未植入行探针（ASM 插桩方案已被 E 系决策否决），当前实现过程入口/出口软断点 +
 * 步进级断点 + 变量检视/在线改值，行级命中需后续探针植入（与设计书 6.2 的软/硬双模断点路线一致）。
 * 断点登记语义：file = 过程 defKey（或 zhName），line = 0 表示入口断点、-1 表示出口断点。
 * </p>
 */
@Service
public class SimDebugCoordinator implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SimDebugCoordinator.class);
    /** 单步（下一拍）重新冻结的轮询上限 */
    private static final long STEP_WAIT_TIMEOUT_MS = 30_000L;

    private final GromaSessionManager sessions;
    private final GromaMcpEnginePortImpl port;
    private final DapWebSocketRouter router;
    private final ObjectMapper mapper = new ObjectMapper();
    /** WS 报文推送与单步等待专用虚拟线程池（绝不占用写者线程与 WS 容器线程） */
    private final ExecutorService async = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap<String, Attachment> attachments = new ConcurrentHashMap<>();

    private static final class Attachment {
        final VirtualThreadDebugSession debugSession;
        final GromaSessionManager.SessionHandle handle;
        GromaTraceListener previousListener;
        final List<jakarta.websocket.Session> wsSessions = new java.util.concurrent.CopyOnWriteArrayList<>();
        final AtomicBoolean suspended = new AtomicBoolean(false);

        Attachment(VirtualThreadDebugSession debugSession, GromaSessionManager.SessionHandle handle) {
            this.debugSession = debugSession;
            this.handle = handle;
        }
    }

    public SimDebugCoordinator(GromaSessionManager sessions,
                               GromaMcpEnginePortImpl port,
                               DapWebSocketRouter router) {
        this.sessions = sessions;
        this.port = port;
        this.router = router;
        this.router.setResumeListener(this::onResumeAction);
        this.router.setMutationListener(this::onVariableMutation);
        // 推演会话停止（REST/MCP/空闲回收任意路径）时清理调试桥
        this.sessions.addLifecycleListener(this::onSimulationSessionStopped);
    }

    // ---------------- WS 生命周期（由 DebugEndpoint 调用） ----------------

    /** WS 连接建立：登记调试会话、安装断点监听器，返回报文处理回调（在 WS 容器线程上执行） */
    public Consumer<String> attach(String simSessionId, jakarta.websocket.Session wsSession) {
        Attachment attachment = attachments.computeIfAbsent(simSessionId, id -> {
            GromaSessionManager.SessionHandle handle = sessions.find(id);
            if (handle == null) {
                throw new IllegalArgumentException("SESSION_NOT_FOUND: 推演会话不存在 " + id);
            }
            VirtualThreadDebugSession debugSession =
                    new VirtualThreadDebugSession(id, null, System.nanoTime());
            router.registerDebugSession(id, debugSession);
            Attachment created = new Attachment(debugSession, handle);
            installListener(created);
            return created;
        });
        attachment.wsSessions.add(wsSession);
        return raw -> handleDapMessage(attachment, raw);
    }

    /** WS 连接关闭：移除该连接；最后一个连接移除时保留调试会话（断点在推演会话生命周期内持续有效） */
    public void detach(String simSessionId, jakarta.websocket.Session wsSession) {
        Attachment attachment = attachments.get(simSessionId);
        if (attachment != null) {
            attachment.wsSessions.remove(wsSession);
        }
    }

    /** 推演会话停止：清理全部调试状态（监听器还原、冻结解除、WS 端关闭） */
    public void onSimulationSessionStopped(String simSessionId) {
        Attachment attachment = attachments.remove(simSessionId);
        if (attachment == null) {
            return;
        }
        unfreeze(attachment);
        router.unregisterDebugSession(simSessionId);
        for (jakarta.websocket.Session ws : attachment.wsSessions) {
            closeQuietly(ws, "ERR:DEBUG:SIM_SESSION_STOPPED");
        }
        LOG.info("[DebugBridge] 推演会话 {} 已停止，调试桥已清理", simSessionId);
    }

    // ---------------- DAP 报文入口（WS 容器线程） ----------------

    private void handleDapMessage(Attachment attachment, String raw) {
        String simSessionId = attachment.handle.sessionId();
        try {
            DapProtocolModels.DapRequest request = mapper.readValue(raw, DapProtocolModels.DapRequest.class);
            DapProtocolModels.DapResponse response = router.handleDapRequest(simSessionId, request);
            pushJson(attachment, response);
        } catch (RuntimeException ex) {
            // tools.jackson 抛非受检 JacksonException：畸形报文一律回 INVALID_DAP_JSON
            LOG.warn("[DebugBridge] DAP 报文解析失败: {}", ex.getMessage());
            pushJson(attachment, new DapProtocolModels.DapResponse(0, false, "parse", "INVALID_DAP_JSON", Map.of()));
        }
    }

    // ---------------- 恢复动作（Router 回调，WS 容器线程） ----------------

    private void onResumeAction(String simSessionId, VirtualThreadDebugSession.StepResumeAction action) {
        Attachment attachment = attachments.get(simSessionId);
        if (attachment == null) {
            return;
        }
        if (action == VirtualThreadDebugSession.StepResumeAction.TERMINATE) {
            onSimulationSessionStopped(simSessionId);
            return;
        }
        if (!attachment.suspended.compareAndSet(true, false)) {
            return; // 非挂起态收到恢复动作（如重复 continue）直接忽略
        }
        unfreeze(attachment);
        if (action == VirtualThreadDebugSession.StepResumeAction.CONTINUE) {
            pushStopped(attachment, "continued", 0L, Map.of());
            return;
        }
        // 单步语义：推进一拍后重新冻结并推送新现场（虚拟线程上执行，不阻塞 WS 容器线程）
        async.submit(() -> stepOneTickAndRefreeze(attachment));
    }

    /** 在线改值：instanceId.fieldName 形式的变量落回真实世界状态（经单写者桥） */
    private void onVariableMutation(DapWebSocketRouter.VariableMutation mutation) {
        int dot = mutation.name().lastIndexOf('.');
        if (dot <= 0 || dot == mutation.name().length() - 1) {
            return; // 非实体字段形式（如 $exception）忽略
        }
        String instanceId = mutation.name().substring(0, dot);
        String fieldName = mutation.name().substring(dot + 1);
        try {
            port.setInstanceValue(mutation.sessionId(), instanceId, fieldName, mutation.value());
            LOG.info("[DebugBridge] 在线改值 {}: {}.{} = {}", mutation.sessionId(), instanceId, fieldName,
                    String.valueOf(mutation.value()));
        } catch (RuntimeException ex) {
            LOG.warn("[DebugBridge] 在线改值失败 {}.{}: {}", instanceId, fieldName, ex.getMessage());
        }
    }

    // ---------------- 断点监听器（写者线程，绝不阻塞） ----------------

    private void installListener(Attachment attachment) {
        GromaTraceListener previous = attachment.handle.session().traceListener();
        attachment.previousListener = previous;
        String sessionId = attachment.handle.sessionId();
        attachment.handle.session().setTraceListener(new GromaTraceListener() {
            @Override public void onProcessStarted(GromaWorldContext c, GromaProcessTask t) {
                if (previous != null) {
                    previous.onProcessStarted(c, t);
                }
                checkProcessBreakpoint(attachment, sessionId, t, t.getDefKey(), 0, "process entry");
            }

            @Override public void onProcessFinished(GromaWorldContext c, GromaProcessTask t) {
                if (previous != null) {
                    previous.onProcessFinished(c, t);
                }
                checkProcessBreakpoint(attachment, sessionId, t, t.getDefKey(), -1, "process exit");
            }

            @Override public void onEvent(GromaWorldContext c, long tick, boolean silent,
                                          WorldSnapshot.SimEventRecord event) {
                if (previous != null) {
                    previous.onEvent(c, tick, silent, event);
                }
            }

            @Override public void onTriggerFired(GromaWorldContext c, long tick,
                                                 WorldSnapshot.SimEventRecord event, com.groma.base.engine.BaseSimEntity context,
                                                 com.groma.base.engine.BaseSimEntity hostEntity, String targetDefKey) {
                if (previous != null) {
                    previous.onTriggerFired(c, tick, event, context, hostEntity, targetDefKey);
                }
            }

            @Override public void onDefaultStartFired(GromaWorldContext c, long tick,
                                                      com.groma.base.engine.BaseSimEntity created,
                                                      com.groma.base.engine.BaseSimEntity hostEntity, String targetDefKey) {
                if (previous != null) {
                    previous.onDefaultStartFired(c, tick, created, hostEntity, targetDefKey);
                }
            }

            @Override public void onProblem(GromaWorldContext c, long tick, String message) {
                if (previous != null) {
                    previous.onProblem(c, tick, message);
                }
            }
        });
    }

    private void checkProcessBreakpoint(Attachment attachment, String sessionId, GromaProcessTask task,
                                        String defKey, int line, String reason) {
        if (attachment.suspended.get()) {
            return; // 已挂起，不再叠加冻结
        }
        boolean hit = attachment.debugSession.activeBreakpointKeys().stream()
                .anyMatch(key -> {
                    int sep = key.lastIndexOf(':');
                    String file = sep > 0 ? key.substring(0, sep) : key;
                    int bpLine = sep > 0 ? Integer.parseInt(key.substring(sep + 1)) : 0;
                    return bpLine == line
                            && (file.equals(defKey) || file.equals(task.getProcessName())
                                || file.isBlank() || file.equals("*"));
                });
        if (!hit) {
            return;
        }
        if (!attachment.suspended.compareAndSet(false, true)) {
            return;
        }
        freeze(attachment);
        WorldSnapshot snapshot = attachment.handle.session().snapshotNow();
        Map<String, Object> variables = flattenVariables(snapshot);
        long tick = snapshot.tickIndex();
        async.submit(() -> {
            List<DapProtocolModels.StackFrameDTO> stack = List.of(new DapProtocolModels.StackFrameDTO(
                    1, defKey + (line == 0 ? "@entry" : "@exit"), defKey, line, tick, "", ""));
            pushJson(attachment, DapProtocolModels.DapStoppedEvent.create(
                    "breakpoint", sessionId, defKey, line, tick, "", "", stack, variables));
            // 同步到调试会话的挂起变量表（variables / setVariable / evaluate 命令读取）
            syncSuspendedVariables(attachment.debugSession, variables);
        });
        LOG.info("[DebugBridge] 会话 {} 命中{}断点：{}（tick={}），推演已冻结", sessionId,
                line == 0 ? "入口" : "出口", defKey, tick);
    }

    // ---------------- 冻结 / 解冻 / 单步 ----------------

    private void freeze(Attachment attachment) {
        var loopGroup = sessions.loopGroup();
        if (loopGroup != null) {
            loopGroup.freezeSession(attachment.handle.sessionId());
        } else {
            attachment.handle.session().requestPause();
        }
    }

    private void unfreeze(Attachment attachment) {
        var loopGroup = sessions.loopGroup();
        if (loopGroup != null) {
            loopGroup.resumeSession(attachment.handle.sessionId());
        } else {
            attachment.handle.session().requestResume();
        }
    }

    private void stepOneTickAndRefreeze(Attachment attachment) {
        String sessionId = attachment.handle.sessionId();
        long before = attachment.handle.session().getLatestSnapshot() == null
                ? 0L : attachment.handle.session().getLatestSnapshot().tickIndex();
        var loopGroup = sessions.loopGroup();
        if (loopGroup == null) {
            // 手动模式：调用线程即写者线程，直接推进一步（经请求桥步进语义）
            sessions.stepSession(sessionId, 1);
        } else {
            // 实时模式：一拍突发配额，交给单写者事件循环执行；轮询等待拍前进
            attachment.handle.session().resumeForBurst(1);
            long deadline = System.currentTimeMillis() + STEP_WAIT_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                WorldSnapshot snap = attachment.handle.session().getLatestSnapshot();
                if (snap != null && snap.tickIndex() > before) {
                    break;
                }
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        // 重新冻结并推送新现场
        if (attachment.suspended.compareAndSet(false, true)) {
            freeze(attachment);
        }
        WorldSnapshot snapshot = attachment.handle.session().snapshotNow();
        Map<String, Object> variables = flattenVariables(snapshot);
        List<DapProtocolModels.StackFrameDTO> stack = List.of(new DapProtocolModels.StackFrameDTO(
                1, "step", "simulation-tick", 0, snapshot.tickIndex(), "", ""));
        pushJson(attachment, DapProtocolModels.DapStoppedEvent.create(
                "step", sessionId, "simulation-tick", 0, snapshot.tickIndex(), "", "", stack, variables));
        syncSuspendedVariables(attachment.debugSession, variables);
    }

    // ---------------- 现场导出与推送 ----------------

    /** 不可变快照 → 扁平变量表（instanceId.fieldName → value；上限 512 项防超大快照拖垮调试通道） */
    private Map<String, Object> flattenVariables(WorldSnapshot snapshot) {
        Map<String, Object> out = new LinkedHashMap<>();
        int budget = 512;
        for (Map.Entry<String, Map<String, Object>> entry : snapshot.instances().entrySet()) {
            for (Map.Entry<String, Object> field : entry.getValue().entrySet()) {
                out.put(entry.getKey() + "." + field.getKey(), field.getValue());
                if (--budget <= 0) {
                    return out;
                }
            }
        }
        return out;
    }

    private void syncSuspendedVariables(VirtualThreadDebugSession debugSession, Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            debugSession.mutateSuspendedVariable(entry.getKey(), entry.getValue());
        }
    }

    private void pushStopped(Attachment attachment, String reason, long tick, Map<String, Object> variables) {
        List<DapProtocolModels.StackFrameDTO> stack = List.of(new DapProtocolModels.StackFrameDTO(
                1, "continue", "", 0, tick, "", ""));
        pushJson(attachment, DapProtocolModels.DapStoppedEvent.create(
                reason, attachment.handle.sessionId(), "", 0, tick, "", "", stack, variables));
    }

    private void pushJson(Attachment attachment, Object payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (RuntimeException ex) {
            LOG.warn("[DebugBridge] 事件序列化失败: {}", ex.getMessage());
            return;
        }
        for (jakarta.websocket.Session ws : attachment.wsSessions) {
            async.submit(() -> {
                try {
                    ws.getBasicRemote().sendText(json);
                } catch (IOException ex) {
                    LOG.debug("[DebugBridge] WS 推送失败（连接可能已关闭）: {}", ex.getMessage());
                }
            });
        }
    }

    private void closeQuietly(jakarta.websocket.Session session, String reason) {
        try {
            session.close(new jakarta.websocket.CloseReason(
                    jakarta.websocket.CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException ex) {
            LOG.debug("[DebugBridge] 关闭 WS 失败: {}", ex.getMessage());
        }
    }

    /** 会话级等待兜底（预留：等待挂起完成的验收钩子） */
    public boolean awaitSuspended(String simSessionId, long timeoutMs) throws InterruptedException {
        Attachment attachment = attachments.get(simSessionId);
        if (attachment == null) {
            return false;
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (attachment.suspended.get()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(20L);
        }
        return attachment.suspended.get();
    }

    @Override
    public void close() {
        async.shutdownNow();
        for (String sessionId : List.copyOf(attachments.keySet())) {
            onSimulationSessionStopped(sessionId);
        }
    }
}

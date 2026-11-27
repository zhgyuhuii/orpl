package com.groma.bootstrap.pipeline;

import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.base.mcp.manifest.GromaToolManifest;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;

/**
 * MCP 引擎端口的生产实现（阶段 2-E）：会话控制经 GromaSessionManager 单写者线程；
 * 模型解析与会话租约统一委托 {@link GromaModelRegistry}（合包 ServiceLoader + 热挂载租约持有至会话停止）。
 * <p>
 * 线程模型（docs/thread-model.md 单写者铁律）：外部干预统一经 {@link #onWriter} 请求桥提交；
 * 实时模式下等待类接口只经桥读取状态，由事件循环自行推进，严禁 HTTP 线程驱动步进。
 * </p>
 */
@Service
public class GromaMcpEnginePortImpl implements GromaMcpEnginePort {
    private static final Logger LOG = LoggerFactory.getLogger(GromaMcpEnginePortImpl.class);

    /** 实时模式下等待条件轮询间隔（读操作经请求桥在写者线程执行，HTTP 线程零 CPU 自旋） */
    private static final long REALTIME_WAIT_POLL_MS = 20L;

    private final GromaSessionManager sessions;
    private final GromaModelRegistry registry;
    private final com.groma.base.websocket.WsRoomSubscriptionManager rooms;
    private final ObjectMapper mapper = new ObjectMapper();

    public GromaMcpEnginePortImpl(GromaSessionManager sessionManager,
                                  GromaModelRegistry modelRegistry,
                                  com.groma.base.websocket.WsRoomSubscriptionManager roomManager) {
        this.sessions = sessionManager;
        this.registry = modelRegistry;
        this.rooms = roomManager;
    }

    // ---------------- 模型与只读资源查询 ----------------

    @Override
    public List<MountedModelMetadata> listMountedModels() {
        List<MountedModelMetadata> out = new ArrayList<>();
        for (GromaModelRegistry.MountedModel mounted : registry.listMounted()) {
            out.add(metadataOf(mounted.runtime()));
        }
        return out;
    }

    @Override
    public Optional<MountedModelMetadata> findMountedModel(String modelCode, String expectedVersionOrNull) {
        return registry.find(modelCode, expectedVersionOrNull).map(m -> metadataOf(m.runtime()));
    }

    // ---------------- 会话生命周期控制 ----------------

    @Override
    public int activeSessionCount() {
        return sessions.listSessions(null).size();
    }

    @Override
    public SessionSummary startSession(String modelCode, String sceneCode, Map<String, Object> initialOverrides,
                                       Long seed, boolean startPaused) {
        String sessionId = registry.startSession(modelCode, sceneCode, initialOverrides, seed, startPaused);
        return describeSession(sessionId);
    }

    @Override
    public void stopSession(String sessionId) {
        sessions.stopSession(sessionId);
    }

    @Override
    public SessionSummary pauseSession(String sessionId) {
        sessions.pause(sessionId);
        return describeSession(sessionId);
    }

    @Override
    public SessionSummary resumeSession(String sessionId) {
        sessions.resume(sessionId);
        return describeSession(sessionId);
    }

    @Override
    public SessionSummary stepSession(String sessionId, int steps) {
        WorldSnapshot snapshot = sessions.stepSession(sessionId, steps);
        broadcastIfObserved(snapshot);
        return describeSession(sessionId);
    }

    /** 推步后的态势广播（无订阅端时 O(1) 返回） */
    private void broadcastIfObserved(WorldSnapshot snapshot) {
        if (rooms != null && snapshot != null) {
            rooms.broadcastSnapshotIfObserved(snapshot);
        }
    }

    @Override
    public SessionSummary setSessionSpeed(String sessionId, double speed) {
        sessions.setSpeed(sessionId, speed);
        return describeSession(sessionId);
    }

    /**
     * 等待会话到达目标步 / 指定过程结束。
     * 手动模式（loopGroup 为空）：调用线程即写者线程，直接逐步推进；
     * 实时模式：解除暂停交给事件循环推进，HTTP 线程仅经请求桥轮询状态（不违反单写者铁律，无 CPU 自旋）。
     */
    @Override
    public WaitOutcome waitForSessionCondition(String sessionId, Long targetTick, String waitForProcessId,
                                               String conditionExpr, long timeoutMs,
                                               DoubleConsumer progressCallback) {
        if (conditionExpr != null && !conditionExpr.isBlank()) {
            throw new IllegalArgumentException("WAIT_CONDITION_UNSUPPORTED: 条件表达式等待暂未开放，请用 targetTick 或 waitForProcessId");
        }
        boolean realtime = sessions.isRealtime();
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        if (realtime) {
            sessions.resume(sessionId);
        }
        try {
            while (System.currentTimeMillis() < deadline) {
                BridgeRead read = readViaWriter(sessionId, targetTick, waitForProcessId);
                if (read.outcome() != null) {
                    return read.outcome();
                }
                if (progressCallback != null) {
                    progressCallback.accept(read.tickIndex());
                }
                if (realtime) {
                    try {
                        Thread.sleep(REALTIME_WAIT_POLL_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // 手动模式：调用线程即写者线程，桥指令在 drain 时立即执行后直接推进一步
                    sessions.stepSession(sessionId, 1);
                }
            }
            BridgeRead finalRead = readViaWriter(sessionId, targetTick, waitForProcessId);
            return new WaitOutcome("TIMEOUT", finalRead.tickIndex(), finalRead.worldTimeMs(), "RUNNING");
        } finally {
            if (realtime) {
                sessions.pause(sessionId);
            }
        }
    }

    @Override
    public List<SessionSummary> listActiveSessions() {
        List<SessionSummary> out = new ArrayList<>();
        for (GromaSessionManager.SessionSummary s : sessions.listSessions(null)) {
            out.add(summary(s));
        }
        return out;
    }

    @Override
    public SessionSummary describeSession(String sessionId) {
        return summary(sessions.summarize(sessionId));
    }

    // ---------------- 不可变快照 ----------------

    @Override
    public WorldSnapshot getImmutableSnapshot(String sessionId) {
        return sessions.getSnapshot(sessionId);
    }

    // ---------------- 运行期干预与过程调用 ----------------

    @Override
    public void setInstanceValue(String sessionId, String instanceId, String fieldName, Object newValue) {
        onWriter(sessionId, ctx -> {
            String error = ctx.setInstanceValue(instanceId, fieldName, newValue);
            if (!error.isEmpty()) {
                throw new IllegalArgumentException("INSTANCE_SET_FAILED: " + error);
            }
            return true;
        });
    }

    @Override
    public void setInstanceState(String sessionId, String instanceId, String stateFieldName, String targetStateLabel) {
        onWriter(sessionId, ctx -> {
            BaseSimEntity entity = ctx.findInstance(instanceId);
            if (entity == null) {
                throw new IllegalArgumentException("INSTANCE_NOT_FOUND: " + instanceId);
            }
            GromaMemberMeta meta = entity.findMember(stateFieldName);
            if (meta == null) {
                throw new IllegalArgumentException("MEMBER_NOT_FOUND: " + stateFieldName);
            }
            Object enumValue = enumByZhLabel(entity, meta, targetStateLabel);
            String error = ctx.setInstanceValue(instanceId, stateFieldName, enumValue);
            if (!error.isEmpty()) {
                throw new IllegalArgumentException("INSTANCE_SET_FAILED: " + error);
            }
            return true;
        });
    }

    @Override
    public String triggerProcessOnInstance(String sessionId, String instanceId, String processRef,
                                           Map<String, Object> args) {
        return onWriter(sessionId, ctx -> {
            BaseSimEntity host = ctx.findInstance(instanceId);
            if (host == null) {
                throw new IllegalArgumentException("INSTANCE_NOT_FOUND: " + instanceId);
            }
            GromaModuleRuntime runtime = sessions.require(sessionId).runtime();
            String defKey = resolveDefKey(runtime, processRef);
            if (defKey == null) {
                throw new IllegalArgumentException("PROCESS_NOT_FOUND: " + processRef);
            }
            GromaProcessTask task = runtime.startManually(defKey, ctx, host, args == null ? Map.of() : args);
            if (task == null) {
                task = runtime.newProcess(defKey, ctx, host, args == null ? Map.of() : args);
            }
            if (task == null) {
                throw new IllegalArgumentException("PROCESS_NOT_STARTABLE: " + processRef);
            }
            ctx.launch(task);
            return task.getProcessInstanceId();
        });
    }

    @Override
    public ProcessExecutionOutcome executeProcessStateless(String modelCode, String processRef,
                                                           Map<String, Object> validatedInputs,
                                                           long wallClockTimeoutMs) {
        String sessionId = registry.startSession(modelCode, null, Map.of(), null, true);
        try {
            GromaModuleRuntime runtime = sessions.require(sessionId).runtime();
            return runProcessAndCollect(sessionId, null, processRef, validatedInputs, runtime, wallClockTimeoutMs);
        } finally {
            sessions.stopSession(sessionId);
        }
    }

    @Override
    public ProcessExecutionOutcome executeProcessInSession(String sessionId, String instanceIdOrNull,
                                                           String processRef, Map<String, Object> validatedInputs) {
        GromaModuleRuntime runtime = sessions.require(sessionId).runtime();
        return runProcessAndCollect(sessionId, instanceIdOrNull, processRef, validatedInputs, runtime, 30_000L);
    }

    // ---------------- 内部 ----------------

    /** 单写者桥上的轻量状态读取（tick / 过程完成判定），手动模式随 drain 立即执行，实时模式由事件循环消费 */
    private record BridgeRead(long tickIndex, long worldTimeMs, WaitOutcome outcome) {}

    private BridgeRead readViaWriter(String sessionId, Long targetTick, String waitForProcessId) {
        var handle = sessions.require(sessionId);
        try {
            var future = handle.session().submitCommand(world -> {
                GromaWorldContext ctx = (GromaWorldContext) world;
                WaitOutcome outcome = null;
                if (targetTick != null && ctx.getTickIndex() >= targetTick) {
                    outcome = new WaitOutcome("REACHED_STEP", ctx.getTickIndex(), ctx.currentWorldTimeMs(), "RUNNING");
                } else if (waitForProcessId != null) {
                    for (GromaProcessTask task : ctx.activeProcesses()) {
                        if (waitForProcessId.equals(task.getProcessInstanceId()) && task.finished()) {
                            outcome = new WaitOutcome("PROCESS_FINISHED", ctx.getTickIndex(), ctx.currentWorldTimeMs(),
                                    task.getStatus());
                            break;
                        }
                    }
                }
                return new BridgeRead(ctx.getTickIndex(), ctx.currentWorldTimeMs(), outcome);
            }, 5_000L);
            if (!sessions.isRealtime()) {
                handle.session().drainRequests();
            }
            return future.get(6_000L, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeCause) {
                throw runtimeCause;
            }
            throw new IllegalStateException("WAIT_READ_FAILED: " + ex.getCause().getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("WAIT_INTERRUPTED", ex);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new IllegalStateException("WAIT_READ_TIMEOUT: 单写者桥读取超时", ex);
        }
    }

    /**
     * 三大 MCP 工作流之二：无会话/会话内执行过程并收集输出与事件。
     * 组合监听器安装 + launch 一次经单写者桥提交并保持到采集结束；
     * 推进按会话模式选择驱动方式：手动模式调用线程逐步推进（调用线程即写者）；
     * 实时模式一次突发配额交给事件循环全速跑完，HTTP 线程仅桥读轮询（不违反单写者铁律）。
     */
    private ProcessExecutionOutcome runProcessAndCollect(String sessionId, String instanceIdOrNull,
                                                         String processRef, Map<String, Object> inputs,
                                                         GromaModuleRuntime runtime, long wallClockTimeoutMs) {
        GromaWorldContext ctx = sessions.require(sessionId).session();
        BaseSimEntity host = instanceIdOrNull == null ? null : ctx.findInstance(instanceIdOrNull);
        String defKey = resolveDefKey(runtime, processRef);
        if (defKey == null) {
            throw new IllegalArgumentException("PROCESS_NOT_FOUND: " + processRef);
        }
        List<WorldSnapshot.SimEventRecord> events = new ArrayList<>();
        var handle = sessions.require(sessionId);

        // 1. 写者线程上：组合既有监听器（调试桥可能已安装）、启动过程、记录 processId
        record Launched(String processId, com.groma.base.engine.GromaTraceListener previous) {}
        Launched launched = onWriter(sessionId, world -> {
            GromaWorldContext writerCtx = (GromaWorldContext) world;
            com.groma.base.engine.GromaTraceListener previous = writerCtx.traceListener();
            writerCtx.setTraceListener(compositeListener(previous, events));
            GromaProcessTask task = host == null
                    ? runtime.newProcess(defKey, writerCtx, null, inputs == null ? Map.of() : inputs)
                    : runtime.startManually(defKey, writerCtx, host, inputs == null ? Map.of() : inputs);
            if (task == null) {
                task = runtime.newProcess(defKey, writerCtx, host, inputs == null ? Map.of() : inputs);
            }
            if (task == null) {
                writerCtx.setTraceListener(previous);
                throw new IllegalArgumentException("PROCESS_NOT_STARTABLE: " + processRef);
            }
            writerCtx.launch(task);
            return new Launched(task.getProcessInstanceId(), previous);
        });

        // 2. 驱动至任务结束（不触碰 ctx 内部状态，只经桥轮询 finished）
        GromaProcessTask finished = driveUntilFinished(handle, launched.processId(), wallClockTimeoutMs);

        // 3. 写者线程上：卸载组合监听器、导出过程现场
        record Export(long elapsedMs, Map<String, Object> runtimeState) {}
        Export export = onWriter(sessionId, world -> {
            GromaWorldContext writerCtx = (GromaWorldContext) world;
            writerCtx.setTraceListener(launched.previous());
            for (GromaProcessTask task : writerCtx.activeProcesses()) {
                if (launched.processId().equals(task.getProcessInstanceId())) {
                    return new Export(task.getElapsedMs(), task.exportRuntime());
                }
            }
            return new Export(finished == null ? 0L : finished.getElapsedMs(),
                    finished == null ? Map.of() : finished.exportRuntime());
        });
        return new ProcessExecutionOutcome(processRef, instanceIdOrNull == null, sessionId,
                export.elapsedMs(), export.runtimeState(), List.copyOf(events));
    }

    /** 驱动会话直至目标过程结束或超时；返回结束时的任务快照（找不到则 null） */
    private GromaProcessTask driveUntilFinished(GromaSessionManager.SessionHandle handle,
                                                String processId, long wallClockTimeoutMs) {
        boolean realtime = sessions.isRealtime();
        if (realtime) {
            // 突发配额交给单写者事件循环全速推进，跑完自动暂停
            handle.session().resumeForBurst(Integer.MAX_VALUE);
        }
        long deadline = System.currentTimeMillis() + Math.max(1_000L, wallClockTimeoutMs);
        while (System.currentTimeMillis() < deadline) {
            GromaProcessTask task = onWriter(handle.sessionId(), world -> {
                GromaWorldContext writerCtx = (GromaWorldContext) world;
                for (GromaProcessTask t : writerCtx.activeProcesses()) {
                    if (processId.equals(t.getProcessInstanceId())) {
                        return t.finished() ? t : null;
                    }
                }
                return null; // 已不在活跃表 = 已结束
            });
            if (task != null) {
                return task;
            }
            if (realtime) {
                try {
                    Thread.sleep(REALTIME_WAIT_POLL_MS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } else {
                sessions.stepSession(handle.sessionId(), 1);
            }
        }
        return null;
    }

    /** 组合监听器：事件采集 + 透传既有监听器（调试桥） */
    private com.groma.base.engine.GromaTraceListener compositeListener(
            com.groma.base.engine.GromaTraceListener previous, List<WorldSnapshot.SimEventRecord> events) {
        return new com.groma.base.engine.GromaTraceListener() {
            @Override public void onProcessStarted(GromaWorldContext c, GromaProcessTask t) {
                if (previous != null) {
                    previous.onProcessStarted(c, t);
                }
            }
            @Override public void onProcessFinished(GromaWorldContext c, GromaProcessTask t) {
                if (previous != null) {
                    previous.onProcessFinished(c, t);
                }
            }
            @Override public void onEvent(GromaWorldContext c, long tick, boolean silent,
                                          WorldSnapshot.SimEventRecord event) {
                events.add(event);
                if (previous != null) {
                    previous.onEvent(c, tick, silent, event);
                }
            }
            @Override public void onTriggerFired(GromaWorldContext c, long tick,
                                                 WorldSnapshot.SimEventRecord event, BaseSimEntity context,
                                                 BaseSimEntity hostEntity, String targetDefKey) {
                if (previous != null) {
                    previous.onTriggerFired(c, tick, event, context, hostEntity, targetDefKey);
                }
            }
            @Override public void onDefaultStartFired(GromaWorldContext c, long tick, BaseSimEntity created,
                                                      BaseSimEntity hostEntity, String targetDefKey) {
                if (previous != null) {
                    previous.onDefaultStartFired(c, tick, created, hostEntity, targetDefKey);
                }
            }
            @Override public void onProblem(GromaWorldContext c, long tick, String message) {
                if (previous != null) {
                    previous.onProblem(c, tick, message);
                }
            }
        };
    }

    /** 外部干预统一经单写者桥执行（跨线程可见性与推演互斥） */
    private <R> R onWriter(String sessionId, java.util.function.Function<GromaWorldContext, R> operation) {
        var handle = sessions.require(sessionId);
        try {
            var future = handle.session()
                    .submitCommand(world -> operation.apply((GromaWorldContext) world), 30_000L);
            // 手动模式：调用线程即写者线程，立即执行桥指令（实时模式下由推演循环 drain）
            if (!sessions.isRealtime()) {
                handle.session().drainRequests();
            }
            return future.get(35_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeCause) {
                throw runtimeCause;
            }
            throw new IllegalStateException("WRITER_FAILED: " + ex.getCause().getMessage(), ex);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new IllegalStateException("WRITER_TIMEOUT: 单写者桥 30s 未完成", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("WRITER_INTERRUPTED", ex);
        }
    }

    private SessionSummary summary(GromaSessionManager.SessionSummary s) {
        return new SessionSummary(s.sessionId(), s.modelCode(), s.modelVersion(), s.sceneCode(), s.status(),
                s.tickIndex(), s.worldTimeMs(), s.speed(), s.instanceCount(), s.activeProcessCount());
    }

    private MountedModelMetadata metadataOf(GromaModuleRuntime runtime) {
        return new MountedModelMetadata(runtime.modelCode(), runtime.modelVersion(), runtime.modelLabel(),
                runtime.indexMarkdown(), readManifest(runtime),
                runtime.types().stream().map(t -> Map.<String, Object>of(
                        "typeCode", t.typeCode(), "zhName", t.zhName(), "entityId", t.entityId())).toList(),
                runtime.scenes().stream().map(s -> Map.<String, Object>of(
                        "sceneCode", s.sceneCode(), "zhName", s.zhName(), "description", s.description())).toList());
    }

    private GromaToolManifest readManifest(GromaModuleRuntime runtime) {
        try (InputStream in = runtime.getClass().getResourceAsStream("/META-INF/groma/mcp/manifest.json")) {
            if (in == null) {
                return null;
            }
            return mapper.readValue(new String(in.readAllBytes(), StandardCharsets.UTF_8), GromaToolManifest.class);
        } catch (IOException | RuntimeException ex) {
            LOG.warn("[McpPort] 模型 {} 工具清单读取失败: {}", runtime.modelCode(), ex.getMessage());
            return null;
        }
    }

    /** 过程引用解析：defKey 精确 → manualStarts 描述 → 过程目录中文名 */
    private String resolveDefKey(GromaModuleRuntime runtime, String processRef) {
        if (processRef == null || processRef.isBlank()) {
            return null;
        }
        boolean knownDefKey = runtime.processes().stream().anyMatch(p -> p.defKey().equals(processRef));
        if (knownDefKey) {
            return processRef;
        }
        return runtime.manualStarts().stream()
                .filter(m -> m.zhName().equals(processRef) || m.defKey().equals(processRef))
                .map(GromaModuleRuntime.ManualStartDescriptor::defKey)
                .findFirst()
                .orElseGet(() -> runtime.processes().stream()
                        .filter(p -> p.zhName().equals(processRef))
                        .map(com.groma.base.engine.GromaModuleRuntime.ProcessDescriptor::defKey)
                        .findFirst()
                        .orElse(null));
    }

    /** 按枚举成员中文名反查枚举常量（存量语义：状态成员以中文名交互） */
    private Object enumByZhLabel(BaseSimEntity entity, GromaMemberMeta meta, String targetStateLabel) {
        String typeName = meta.typeName();
        // 枚举类在生成模型模块内：必须用实体自身的类加载器（热挂载时为隔离加载器，宿主 Class.forName 不可见）
        ClassLoader modelLoader = entity.getClass().getClassLoader();
        for (String candidate : List.of(typeName, entity.getClass().getPackageName() + "." + typeName,
                entity.getClass().getPackageName().replaceAll("\\.entity$", "") + ".enums." + typeName)) {
            try {
                Class<?> enumClass = Class.forName(candidate, false, modelLoader);
                if (enumClass.isEnum()) {
                    for (Object constant : enumClass.getEnumConstants()) {
                        if (BaseSimEntity.enumZhLabel((Enum<?>) constant).equals(targetStateLabel)
                                || ((Enum<?>) constant).name().equals(targetStateLabel)) {
                            return constant;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // 尝试下一候选名
            }
        }
        throw new IllegalArgumentException("STATE_NOT_FOUND: " + meta.typeName() + " 无枚举成员 " + targetStateLabel);
    }
}

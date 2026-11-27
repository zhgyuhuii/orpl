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
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
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
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;

/**
 * MCP 引擎端口的生产实现（阶段 2-E）：会话控制经 GromaSessionManager 单写者线程；
 * 模型解析 = 合包（ServiceLoader）+ 热挂载（版本钉住租约持有，宿主生命周期内不释放）。
 */
@Service
public class GromaMcpEnginePortImpl implements GromaMcpEnginePort {
    private static final Logger LOG = LoggerFactory.getLogger(GromaMcpEnginePortImpl.class);

    private final GromaSessionManager sessions;
    private final VersionPinnedHotSwapManager hotSwap;
    private final com.groma.base.websocket.WsRoomSubscriptionManager rooms;
    private final ObjectMapper mapper = new ObjectMapper();
    /** 热挂载模型的运行时缓存（modelCode → runtime）；租约持有至宿主停止 */
    private final ConcurrentHashMap<String, GromaModuleRuntime> hotRuntimes = new ConcurrentHashMap<>();

    public GromaMcpEnginePortImpl(GromaSessionManager sessionManager,
                                  VersionPinnedHotSwapManager hotSwapManager,
                                  com.groma.base.websocket.WsRoomSubscriptionManager roomManager) {
        this.sessions = sessionManager;
        this.hotSwap = hotSwapManager;
        this.rooms = roomManager;
    }

    // ---------------- 模型与只读资源查询 ----------------

    @Override
    public List<MountedModelMetadata> listMountedModels() {
        List<MountedModelMetadata> out = new ArrayList<>();
        for (GromaModuleRuntime runtime : allRuntimes()) {
            out.add(metadataOf(runtime));
        }
        return out;
    }

    @Override
    public Optional<MountedModelMetadata> findMountedModel(String modelCode, String expectedVersionOrNull) {
        return allRuntimes().stream()
                .filter(r -> r.modelCode().equals(modelCode))
                .filter(r -> expectedVersionOrNull == null || r.modelVersion().equals(expectedVersionOrNull))
                .findFirst()
                .map(this::metadataOf);
    }

    // ---------------- 会话生命周期控制 ----------------

    @Override
    public int activeSessionCount() {
        return sessions.listSessions(null).size();
    }

    @Override
    public SessionSummary startSession(String modelCode, String sceneCode, Map<String, Object> initialOverrides,
                                       Long seed, boolean startPaused) {
        GromaModuleRuntime runtime = resolveRuntime(modelCode);
        String sessionId = sessions.startSession(runtime, sceneCode, initialOverrides, seed, startPaused, null);
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

    @Override
    public WaitOutcome waitForSessionCondition(String sessionId, Long targetTick, String waitForProcessId,
                                               String conditionExpr, long timeoutMs,
                                               DoubleConsumer progressCallback) {
        if (conditionExpr != null && !conditionExpr.isBlank()) {
            throw new IllegalArgumentException("WAIT_CONDITION_UNSUPPORTED: 条件表达式等待暂未开放，请用 targetTick 或 waitForProcessId");
        }
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            GromaWorldContext ctx = sessions.require(sessionId).session();
            if (targetTick != null && ctx.getTickIndex() >= targetTick) {
                return new WaitOutcome("REACHED_STEP", ctx.getTickIndex(), ctx.currentWorldTimeMs(), "RUNNING");
            }
            if (waitForProcessId != null) {
                for (GromaProcessTask task : ctx.activeProcesses()) {
                    if (waitForProcessId.equals(task.getProcessInstanceId()) && task.finished()) {
                        return new WaitOutcome("PROCESS_FINISHED", ctx.getTickIndex(), ctx.currentWorldTimeMs(),
                                task.getStatus());
                    }
                }
            }
            if (progressCallback != null) {
                progressCallback.accept(ctx.getTickIndex());
            }
            sessions.stepSession(sessionId, 1);
        }
        GromaWorldContext ctx = sessions.require(sessionId).session();
        return new WaitOutcome("TIMEOUT", ctx.getTickIndex(), ctx.currentWorldTimeMs(), "RUNNING");
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
        GromaModuleRuntime runtime = resolveRuntime(modelCode);
        String scene = runtime.scenes().isEmpty() ? null : runtime.scenes().get(0).sceneCode();
        String sessionId = sessions.startSession(runtime, scene, Map.of(), null, true, null);
        try {
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

    /** 三大 MCP 工作流之二：无会话/会话内执行过程并收集输出与事件（单写者线程逐步驱动至任务结束） */
    private ProcessExecutionOutcome runProcessAndCollect(String sessionId, String instanceIdOrNull,
                                                         String processRef, Map<String, Object> inputs,
                                                         GromaModuleRuntime runtime, long wallClockTimeoutMs) {
        GromaWorldContext ctx = sessions.require(sessionId).session();
        BaseSimEntity host = instanceIdOrNull == null ? null : ctx.findInstance(instanceIdOrNull);
        String defKey = resolveDefKey(runtime, processRef);
        if (defKey == null) {
            throw new IllegalArgumentException("PROCESS_NOT_FOUND: " + processRef);
        }
        GromaProcessTask task = host == null
                ? runtime.newProcess(defKey, ctx, null, inputs == null ? Map.of() : inputs)
                : runtime.startManually(defKey, ctx, host, inputs == null ? Map.of() : inputs);
        if (task == null) {
            task = runtime.newProcess(defKey, ctx, host, inputs == null ? Map.of() : inputs);
        }
        if (task == null) {
            throw new IllegalArgumentException("PROCESS_NOT_STARTABLE: " + processRef);
        }
        List<WorldSnapshot.SimEventRecord> events = new ArrayList<>();
        ctx.setTraceListener(new com.groma.base.engine.GromaTraceListener() {
            @Override public void onProcessStarted(GromaWorldContext c, GromaProcessTask t) { }
            @Override public void onProcessFinished(GromaWorldContext c, GromaProcessTask t) { }
            @Override public void onEvent(GromaWorldContext c, long tick, boolean silent,
                                          WorldSnapshot.SimEventRecord event) {
                events.add(event);
            }
            @Override public void onTriggerFired(GromaWorldContext c, long tick,
                                                  WorldSnapshot.SimEventRecord event, BaseSimEntity context,
                                                  BaseSimEntity hostEntity, String targetDefKey) { }
            @Override public void onDefaultStartFired(GromaWorldContext c, long tick, BaseSimEntity created,
                                                      BaseSimEntity hostEntity, String targetDefKey) { }
            @Override public void onProblem(GromaWorldContext c, long tick, String message) { }
        });
        ctx.launch(task);
        long deadline = System.currentTimeMillis() + Math.max(1_000L, wallClockTimeoutMs);
        while (!task.finished() && System.currentTimeMillis() < deadline) {
            sessions.stepSession(sessionId, 1);
        }
        return new ProcessExecutionOutcome(processRef, instanceIdOrNull == null, sessionId,
                task.getElapsedMs(), task.exportRuntime(), List.copyOf(events));
    }

    /** 模型解析：合包（宿主类路径）→ 热挂载（租约持有） */
    private GromaModuleRuntime resolveRuntime(String modelCode) {
        for (GromaModuleRuntime runtime : ServiceLoader.load(GromaModuleRuntime.class,
                GromaMcpEnginePortImpl.class.getClassLoader())) {
            if (runtime.modelCode().equals(modelCode)) {
                return runtime;
            }
        }
        return hotRuntimes.computeIfAbsent(modelCode, code -> {
            try (VersionPinnedHotSwapManager.SessionModelLease lease = hotSwap.acquireLatestSessionLease(code)) {
                // 宿主运行期内保持引用：租约持有至 unmount（会话租约三级在 GromaModelRegistry 完善）
                var discovered = ServiceLoader.load(GromaModuleRuntime.class, lease.loader()).iterator();
                if (discovered.hasNext()) {
                    return discovered.next();
                }
                // 兜底：直接读 services 登记文件并按类名实例化（部分嵌入式容器下 ServiceLoader 迭代不可用时）
                java.net.URL servicesFile = lease.loader()
                        .getResource("META-INF/services/com.groma.base.engine.GromaModuleRuntime");
                if (servicesFile != null) {
                    String fqcn;
                    try (java.io.InputStream in = servicesFile.openStream()) {
                        fqcn = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines()
                                .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#"))
                                .findFirst().orElse("");
                    }
                    if (!fqcn.isBlank()) {
                        return (GromaModuleRuntime) lease.loadClass(fqcn).getDeclaredConstructor().newInstance();
                    }
                }
                throw new java.util.NoSuchElementException("模型 " + code + " 的 services 登记不可读");
            } catch (IOException | RuntimeException | ReflectiveOperationException ex) {
                throw new IllegalStateException("MODEL_NOT_FOUND: 模型 " + code + " 未挂载（合包与热挂载均未找到）", ex);
            }
        });
    }

    private List<GromaModuleRuntime> allRuntimes() {
        List<GromaModuleRuntime> out = new ArrayList<>();
        for (GromaModuleRuntime runtime : ServiceLoader.load(GromaModuleRuntime.class,
                GromaMcpEnginePortImpl.class.getClassLoader())) {
            out.add(runtime);
        }
        out.addAll(hotRuntimes.values());
        return out;
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

    /** 外部干预统一经单写者桥执行（跨线程可见性与推演互斥，外部干预语义的落地） */
    private <R> R onWriter(String sessionId, java.util.function.Function<GromaWorldContext, R> operation) {
        var handle = sessions.require(sessionId);
        try {
            var future = handle.session()
                    .submitCommand(world -> operation.apply((GromaWorldContext) world), 30_000L);
            // 手动模式：调用线程即写者线程，立即执行桥指令（实时模式下由推演循环 drain）
            handle.session().drainRequests();
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
}

package com.groma.base.mcp.port;

import com.groma.base.engine.WorldSnapshot;
import com.groma.base.mcp.manifest.GromaToolManifest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleConsumer;

/**
 * MCP 服务端与底层多模型运行宿主/单写者引擎之间的统一隔离端口契约
 */
public interface GromaMcpEnginePort {

    record MountedModelMetadata(
            String modelCode,
            String modelVersion,
            String modelLabel,
            String indexMarkdown,
            GromaToolManifest toolManifest,
            List<Map<String, Object>> typesCatalog,
            List<Map<String, Object>> scenesCatalog
    ) {}

    record SessionSummary(
            String sessionId,
            String modelCode,
            String modelVersion,
            String sceneCode,
            String status,
            long tickIndex,
            long worldTimeMs,
            double speed,
            int instanceCount,
            int activeProcessCount
    ) {}

    record WaitOutcome(
            String reason,          // "REACHED_STEP", "PROCESS_FINISHED", "CONDITION_MET"
            long finalTickIndex,
            long worldTimeMs,
            String sessionStatus
    ) {}

    record ProcessExecutionOutcome(
            String processRef,
            boolean stateless,
            String sessionId,
            long elapsedSimDurationMs,
            Map<String, Object> outputs,
            List<WorldSnapshot.SimEventRecord> emittedEvents
    ) {}

    // ---------------- 模型与只读资源查询 ----------------
    List<MountedModelMetadata> listMountedModels();

    Optional<MountedModelMetadata> findMountedModel(String modelCode, String expectedVersionOrNull);

    // ---------------- 会话生命周期控制（经统一请求桥进入推演线程） ----------------
    int activeSessionCount();

    SessionSummary startSession(
            String modelCode,
            String sceneCode,
            Map<String, Object> initialOverrides,
            Long seed,
            boolean startPaused);

    void stopSession(String sessionId);

    SessionSummary pauseSession(String sessionId);

    SessionSummary resumeSession(String sessionId);

    SessionSummary stepSession(String sessionId, int steps);

    SessionSummary setSessionSpeed(String sessionId, double speed);

    WaitOutcome waitForSessionCondition(
            String sessionId,
            Long targetTick,
            String waitForProcessId,
            String conditionExpr,
            long timeoutMs,
            DoubleConsumer progressCallback);

    List<SessionSummary> listActiveSessions();

    SessionSummary describeSession(String sessionId);

    // ---------------- 不可变快照读取（零锁强一致） ----------------
    WorldSnapshot getImmutableSnapshot(String sessionId);

    // ---------------- 运行期干预与过程调用 ----------------
    void setInstanceValue(String sessionId, String instanceId, String fieldName, Object newValue);

    void setInstanceState(String sessionId, String instanceId, String stateFieldName, String targetStateLabel);

    String triggerProcessOnInstance(String sessionId, String instanceId, String processRef, Map<String, Object> args);

    ProcessExecutionOutcome executeProcessStateless(
            String modelCode,
            String processRef,
            Map<String, Object> validatedInputs,
            long wallClockTimeoutMs);

    ProcessExecutionOutcome executeProcessInSession(
            String sessionId,
            String instanceIdOrNull,
            String processRef,
            Map<String, Object> validatedInputs);
}

package com.groma.bootstrap.pipeline;

import com.groma.base.common.Result;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.archive.ArchiveCheckpointCoordinator;
import com.groma.base.engine.archive.SimulationArchiveBundle;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 通用会话 REST（阶段 2-E 第 3 项）：覆盖热挂载模型，统一 {@link Result} 包装；
 * 全部控制经 {@link GromaMcpEnginePort} 进入单写者线程（与 MCP / WS 三通道一致）。
 */
@RestController
@RequestMapping("/api/v1/studio/sessions")
public class SessionRestController {

    private final GromaMcpEnginePort port;
    private final com.groma.base.engine.runtime.GromaSessionManager sessions;
    private final ArchiveCheckpointCoordinator archiveCoordinator = new ArchiveCheckpointCoordinator();

    public SessionRestController(GromaMcpEnginePort port,
                                 com.groma.base.engine.runtime.GromaSessionManager sessions) {
        this.port = port;
        this.sessions = sessions;
    }

    /** 建会话请求：模型编码 + 场景 + 初始覆盖 + 种子 + 是否暂停起步 */
    public record StartSessionRequest(String modelCode, String sceneCode, Map<String, Object> initialOverrides,
                                      Long seed, boolean startPaused) {}

    @PostMapping
    public Result<GromaMcpEnginePort.SessionSummary> start(@RequestBody StartSessionRequest request) {
        if (request.modelCode() == null || request.sceneCode() == null) {
            return Result.fail("SESSION_BAD_REQUEST", "modelCode 与 sceneCode 必填", null);
        }
        return Result.ok(port.startSession(request.modelCode(), request.sceneCode(),
                request.initialOverrides() == null ? Map.of() : request.initialOverrides(),
                request.seed(), request.startPaused()));
    }

    @GetMapping
    public Result<List<GromaMcpEnginePort.SessionSummary>> list() {
        return Result.ok(port.listActiveSessions());
    }

    @GetMapping("/{sessionId}")
    public Result<GromaMcpEnginePort.SessionSummary> describe(@PathVariable String sessionId) {
        return Result.ok(port.describeSession(sessionId));
    }

    /** 推进步数请求体 */
    public record StepRequest(int steps) {}

    @PostMapping("/{sessionId}/step")
    public Result<GromaMcpEnginePort.SessionSummary> step(@PathVariable String sessionId,
                                                          @RequestBody StepRequest request) {
        return Result.ok(port.stepSession(sessionId, Math.max(1, request.steps())));
    }

    @PostMapping("/{sessionId}/pause")
    public Result<GromaMcpEnginePort.SessionSummary> pause(@PathVariable String sessionId) {
        return Result.ok(port.pauseSession(sessionId));
    }

    @PostMapping("/{sessionId}/resume")
    public Result<GromaMcpEnginePort.SessionSummary> resume(@PathVariable String sessionId) {
        return Result.ok(port.resumeSession(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Map<String, Object>> stop(@PathVariable String sessionId) {
        port.stopSession(sessionId);
        return Result.ok(Map.of("stopped", sessionId));
    }

    /** 倍速设置请求体 */
    public record SpeedRequest(double speed) {}

    @PostMapping("/{sessionId}/speed")
    public Result<GromaMcpEnginePort.SessionSummary> setSpeed(@PathVariable String sessionId,
                                                              @RequestBody SpeedRequest request) {
        return Result.ok(port.setSessionSpeed(sessionId, request.speed()));
    }

    /** 等待条件请求体：目标拍 / 等待过程结束，二选一 */
    public record WaitRequest(Long targetTick, String waitForProcessId, Long timeoutMs) {}

    @PostMapping("/{sessionId}/wait")
    public Result<GromaMcpEnginePort.WaitOutcome> wait(@PathVariable String sessionId,
                                                       @RequestBody WaitRequest request) {
        return Result.ok(port.waitForSessionCondition(sessionId, request.targetTick(), request.waitForProcessId(),
                null, request.timeoutMs() == null ? 30_000L : request.timeoutMs(), null));
    }

    @GetMapping("/{sessionId}/snapshot")
    public Result<WorldSnapshot> snapshot(@PathVariable String sessionId) {
        return Result.ok(port.getImmutableSnapshot(sessionId));
    }

    /** 成员写值请求体 */
    public record SetValueRequest(String fieldName, Object value) {}

    @PutMapping("/{sessionId}/instances/{instanceId}/value")
    public Result<Map<String, Object>> setValue(@PathVariable String sessionId, @PathVariable String instanceId,
                                                @RequestBody SetValueRequest request) {
        port.setInstanceValue(sessionId, instanceId, request.fieldName(), request.value());
        return Result.ok(Map.of("instanceId", instanceId, "fieldName", request.fieldName(), "written", true));
    }

    /** 状态写值请求体（枚举中文名） */
    public record SetStateRequest(String stateFieldName, String targetStateLabel) {}

    @PutMapping("/{sessionId}/instances/{instanceId}/state")
    public Result<Map<String, Object>> setState(@PathVariable String sessionId, @PathVariable String instanceId,
                                                @RequestBody SetStateRequest request) {
        port.setInstanceState(sessionId, instanceId, request.stateFieldName(), request.targetStateLabel());
        return Result.ok(Map.of("instanceId", instanceId, "state", request.targetStateLabel(), "written", true));
    }

    /** 过程触发请求体：实例可空（空 = 独立过程目录） */
    public record TriggerProcessRequest(String instanceId, String processRef, Map<String, Object> args) {}

    @PostMapping("/{sessionId}/processes")
    public Result<Map<String, Object>> trigger(@PathVariable String sessionId,
                                               @RequestBody TriggerProcessRequest request) {
        String processInstanceId = port.triggerProcessOnInstance(sessionId, request.instanceId(),
                request.processRef(), request.args());
        return Result.ok(Map.of("processInstanceId", processInstanceId));
    }

    /** 会话存档导出（当前拍边界快照） */
    @PostMapping("/{sessionId}/archive/export")
    public Result<SimulationArchiveBundle> exportArchive(@PathVariable String sessionId) {
        return Result.ok(archiveCoordinator.createCheckpointAtStepBoundary(sessionContext(sessionId)));
    }

    /** 会话存档导入（整包 JSON 载入并从存档拍续跑） */
    @PostMapping("/{sessionId}/archive/import")
    public Result<GromaMcpEnginePort.SessionSummary> importArchive(@PathVariable String sessionId,
                                                                   @RequestBody SimulationArchiveBundle archiveBundle) {
        sessionContext(sessionId).restoreFromArchive(archiveBundle);
        return Result.ok(port.describeSession(sessionId));
    }

    private com.groma.base.engine.GromaWorldContext sessionContext(String sessionId) {
        return sessions.require(sessionId).session();
    }
}

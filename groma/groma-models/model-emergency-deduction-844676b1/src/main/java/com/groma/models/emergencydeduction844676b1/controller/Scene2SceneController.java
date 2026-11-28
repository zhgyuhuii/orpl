package com.groma.models.emergencydeduction844676b1.controller;

import com.groma.base.common.Result;
import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.models.emergencydeduction844676b1.model.dto.Scene2SceneStartDTO;
import com.groma.models.emergencydeduction844676b1.repository.Scene2SceneSnapshotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.Object;
import java.lang.String;
import java.lang.Void;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 场景 REST 控制器：场景2（统一挂载 /api/v1/models/emergency-deduction-844676b1/scenes/scene2；无认证直接可达，后期由微服务网关按交接清单统一管控）
 */
@Tag(
        name = "[模型:应急决策推演-关联副本-1029] 场景2"
)
@RestController
@RequestMapping("/api/v1/models/emergency-deduction-844676b1/scenes/scene2")
@GromaMeta(
        id = "fFnAAr3WKsN0Oaghvzj",
        zhName = "场景2控制器",
        kind = "CONTROLLER"
)
public class Scene2SceneController {
    private final GromaModuleRuntime moduleRuntime;

    private final GromaSessionManager sessionManager;

    private final Scene2SceneSnapshotRepository snapshotRepository = new Scene2SceneSnapshotRepository();

    public Scene2SceneController(GromaModuleRuntime moduleRuntime,
            GromaSessionManager sessionManager) {
        this.moduleRuntime = moduleRuntime;
        this.sessionManager = sessionManager;
    }

    @Operation(
            summary = "启动场景推演会话（startPaused=true 时创建为暂停态，仅响应逐步推进）"
    )
    @PostMapping("/sessions")
    public Result<String> startSession(
            @RequestBody(required = false) Map<String, Object> startParams) {
        // 强类型启动参数收口（设计说明书 3.1 三件套）：线格式与既有 REST 契约完全一致
        Scene2SceneStartDTO dto = Scene2SceneStartDTO.fromMap(startParams);
        return Result.ok(sessionManager.startSession(moduleRuntime, "scene2", dto.getInitialOverrides(), dto.getSeed(), dto.getStartPaused()));
    }

    @Operation(
            summary = "列出本场景在途会话摘要"
    )
    @GetMapping("/sessions")
    public Result<List<GromaSessionManager.SessionSummary>> listSessions() {
        return Result.ok(sessionManager.listSessions(moduleRuntime.modelCode()).stream().filter(s -> "scene2".equals(s.sceneCode())).toList());
    }

    @Operation(
            summary = "精确推进 N 个仿真步进并停在步末，返回步进边界不可变快照"
    )
    @PostMapping("/sessions/{sessionId}/step")
    public Result<WorldSnapshot> stepSession(@PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int steps) {
        return Result.ok(sessionManager.stepSession(sessionId, steps));
    }

    @Operation(
            summary = "获取步进边界一致性快照（实例、在途过程、近期事件）"
    )
    @GetMapping("/sessions/{sessionId}/snapshot")
    public Result<WorldSnapshot> getSnapshot(@PathVariable String sessionId) {
        var snapshot = sessionManager.getSnapshot(sessionId);
        snapshotRepository.save(sessionId, snapshot);
        return Result.ok(snapshot);
    }

    @Operation(
            summary = "设置期望倍速（孪生过程运行期间由底座强制锁 1x）"
    )
    @PostMapping("/sessions/{sessionId}/speed/{speed}")
    public Result<Void> setSpeed(@PathVariable String sessionId, @PathVariable double speed) {
        sessionManager.setSpeed(sessionId, speed);
        return Result.ok(null);
    }

    @Operation(
            summary = "暂停推演"
    )
    @PostMapping("/sessions/{sessionId}/pause")
    public Result<Void> pause(@PathVariable String sessionId) {
        sessionManager.pause(sessionId);
        return Result.ok(null);
    }

    @Operation(
            summary = "恢复实时推进"
    )
    @PostMapping("/sessions/{sessionId}/resume")
    public Result<Void> resume(@PathVariable String sessionId) {
        sessionManager.resume(sessionId);
        return Result.ok(null);
    }

    @Operation(
            summary = "结束并移除推演会话"
    )
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> stopSession(@PathVariable String sessionId) {
        sessionManager.stopSession(sessionId);
        snapshotRepository.remove(sessionId);
        return Result.ok(null);
    }
}

package com.groma.models.emergencydeduction.controller;

import com.groma.base.common.Result;
import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
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
 * 场景 REST 控制器：场景1（统一挂载 /api/v1/models/emergency-deduction/scenes/scene1；无认证直接可达，后期由微服务网关按交接清单统一管控）
 */
@Tag(
        name = "[模型:应急决策推演-关联副本-1029] 场景1"
)
@RestController
@RequestMapping("/api/v1/models/emergency-deduction/scenes/scene1")
@GromaMeta(
        id = "bX4vMzSlVMdk6QjF7CD",
        zhName = "场景1控制器",
        kind = "CONTROLLER"
)
public class Scene1SceneController {
    private final GromaModuleRuntime moduleRuntime;

    private final GromaSessionManager sessionManager;

    public Scene1SceneController(GromaModuleRuntime moduleRuntime,
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
        boolean startPaused = startParams != null && Boolean.TRUE.equals(startParams.get("startPaused"));
        Long seed = startParams == null || startParams.get("seed") == null ? null
                : GromaValues.toLong(startParams.get("seed"));
        return Result.ok(sessionManager.startSession(moduleRuntime, "scene1", startParams, seed, startPaused));
    }

    @Operation(
            summary = "列出本场景在途会话摘要"
    )
    @GetMapping("/sessions")
    public Result<List<GromaSessionManager.SessionSummary>> listSessions() {
        return Result.ok(sessionManager.listSessions(moduleRuntime.modelCode()).stream().filter(s -> "scene1".equals(s.sceneCode())).toList());
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
        return Result.ok(sessionManager.getSnapshot(sessionId));
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
        return Result.ok(null);
    }
}

package com.groma.models.emergencydeduction.controller;

import com.groma.base.common.Result;
import com.groma.models.emergencydeduction.model.dto.ThreeStationSceneStartDTO;
import com.groma.models.emergencydeduction.model.vo.ThreeStationSceneSnapshotVO;
import com.groma.models.emergencydeduction.service.IThreeStationFloodTrainSceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[模型:应急决策推演] 三站两区间积水与列车运行场景")
@RestController
@RequestMapping("/api/v1/models/emergency-deduction/scenes/three-station-flood-train")
public class ThreeStationFloodTrainSceneController {

    private final IThreeStationFloodTrainSceneService sceneService;

    public ThreeStationFloodTrainSceneController(IThreeStationFloodTrainSceneService sceneService) {
        this.sceneService = sceneService;
    }

    @Operation(summary = "启动三站两区间暴雨防汛推演会话")
    @PostMapping("/sessions")
    public Result<String> start(@RequestBody(required = false) ThreeStationSceneStartDTO dto) {
        return Result.ok(sceneService.startSession(dto));
    }

    @Operation(summary = "推进 N 个仿真步进（默认每步 10 秒）")
    @PostMapping("/sessions/{sessionId}/step")
    public Result<ThreeStationSceneSnapshotVO> step(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "6") int steps) {
        return Result.ok(sceneService.stepSession(sessionId, steps));
    }

    @Operation(summary = "查询当前三站两区间水位、挡水墙与列车运行态势快照")
    @GetMapping("/sessions/{sessionId}/snapshot")
    public Result<ThreeStationSceneSnapshotVO> snapshot(@PathVariable String sessionId) {
        return Result.ok(sceneService.getSnapshot(sessionId));
    }

    @Operation(summary = "结束并清理推演会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> stop(@PathVariable String sessionId) {
        sceneService.stopSession(sessionId);
        return Result.ok(null);
    }
}

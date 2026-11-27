package com.groma.models.smartplanning.controller;

import com.groma.base.common.Result;
import com.groma.models.smartplanning.model.dto.SmartPlanningRequestDTO;
import com.groma.models.smartplanning.model.vo.SmartPlanningSnapshotVO;
import com.groma.models.smartplanning.service.ISmartPlanningSceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[模型:智能筹划应用] 智能筹划全链路场景接口")
@RestController
@RequestMapping("/api/v1/models/smart-planning/scenes/smart-planning-scene")
public class SmartPlanningSceneController {

    private final ISmartPlanningSceneService planningService;

    public SmartPlanningSceneController(ISmartPlanningSceneService planningService) {
        this.planningService = planningService;
    }

    @Operation(summary = "一键执行作战意图分析、H时同步到达航路解算与想定配置生成")
    @PostMapping("/execute")
    public Result<SmartPlanningSnapshotVO> execute(@RequestBody(required = false) SmartPlanningRequestDTO dto) {
        return Result.ok(planningService.runFullPlanningPipeline(dto));
    }
}

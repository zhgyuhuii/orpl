package com.groma.models.smartplanning.model.vo;

import com.groma.models.smartplanning.model.entity.SessionRecordManagerEntity;

import java.util.List;

public record SmartPlanningSnapshotVO(
        String sessionId,
        long tickIndex,
        String analyzedIntent,
        String planId,
        String planSummary,
        String scenarioConfigJson,
        List<RoutePlanItemVO> synchronizedRoutes,
        List<SessionRecordManagerEntity.StepLogItem> sessionRecords
) {}

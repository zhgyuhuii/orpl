package com.groma.models.emergencydeduction.model.vo;

import java.util.List;
import java.util.Map;

public record ThreeStationSceneSnapshotVO(
        String sessionId,
        long tickIndex,
        long worldTimeMs,
        String warningLevel,
        double areaWaterDepthM,
        Map<String, Double> stationWaterDepthsM,
        Map<String, Double> sectionWaterDepthsM,
        String trainPosition,
        String trainState,
        boolean sectionReleaseAllowed,
        List<String> problems
) {}

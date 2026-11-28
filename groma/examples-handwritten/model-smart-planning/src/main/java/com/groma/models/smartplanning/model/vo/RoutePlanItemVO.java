package com.groma.models.smartplanning.model.vo;

public record RoutePlanItemVO(
        String unitName,
        String baseName,
        String targetName,
        double distanceKm,
        long flightDurationSec,
        long computedTakeoffSec,
        long estimatedArrivalSec,
        long hHourDiffSec
) {}

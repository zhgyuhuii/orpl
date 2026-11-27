package com.groma.models.smartplanning.model.dto;

public record SyncStrikeForceInputDTO(
        String unitName,
        String baseName,
        double baseLatDeg,
        double baseLonDeg,
        String targetName,
        double targetLatDeg,
        double targetLonDeg,
        double cruiseSpeedKmh
) {}

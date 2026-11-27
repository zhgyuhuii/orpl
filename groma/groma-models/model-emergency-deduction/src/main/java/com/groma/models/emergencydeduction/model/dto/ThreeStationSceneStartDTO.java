package com.groma.models.emergencydeduction.model.dto;

public record ThreeStationSceneStartDTO(
        Long seed,
        Double rainfallIntensityMmPerHour,
        Boolean startPaused
) {
    public double effectiveRainfallMmPerHour() {
        return (rainfallIntensityMmPerHour == null || rainfallIntensityMmPerHour < 0.0D)
                ? 180.0D // 默认特大暴雨强度 180mm/h
                : rainfallIntensityMmPerHour;
    }
}

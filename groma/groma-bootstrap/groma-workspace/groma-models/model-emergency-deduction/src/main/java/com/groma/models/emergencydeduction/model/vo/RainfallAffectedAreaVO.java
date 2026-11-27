package com.groma.models.emergencydeduction.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Double;
import java.lang.Long;
import java.lang.String;
import java.util.Map;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：降雨与受影响区域）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "降雨与受影响区域"
)
public record RainfallAffectedAreaVO(String instanceId, boolean alive, String rainfall, String area,
        double currentCatchmentFlow, double stepCatchmentVolume, double currentRainIntensity,
        double cumulativeRainfall, Map<Long, Double> rainfallSeries) {
}

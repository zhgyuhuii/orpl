package com.groma.models.emergencydeduction844676b1.model.vo;

import com.groma.models.emergencydeduction844676b1.model.enums.FloodWallStateEnum;
import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：车站）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "车站"
)
public record StationVO(String instanceId, boolean alive, double stationWaterVolume,
        double failureThreshold, double failureDuration, double floodWallTopElevation, String name,
        double ratedDrainCapacity, double stationFloorElevation, double equivalentStorageArea,
        double stationWaterDepth, double evacuationCapacity, String position,
        WaterloggingStateEnum waterloggingState, FloodWallStateEnum floodWallState,
        double intactFlowWidth, double failedFlowWidth, double inletFloorElevation,
        double inletFlowCoeff, double overlimitDuration) {
}

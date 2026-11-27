package com.groma.models.emergencydeduction.model.vo;

import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：地铁区间）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "地铁区间"
)
public record MetroSectionVO(String instanceId, boolean alive, double length,
        double equivalentStorageWidth, double dangerDepth, double forcedStopDepth,
        double waterVolume, double representativeDepth, boolean releaseAllowed, String name,
        String position, double representativeFloorElevation,
        WaterloggingStateEnum waterloggingState) {
}

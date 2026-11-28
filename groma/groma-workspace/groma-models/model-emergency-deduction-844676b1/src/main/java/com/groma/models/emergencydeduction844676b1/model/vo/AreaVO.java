package com.groma.models.emergencydeduction844676b1.model.vo;

import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：区域）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "区域"
)
public record AreaVO(String instanceId, boolean alive, double catchmentArea, double runoffCoeff,
        double equivalentStorageArea, double groundElevation, double surfaceDrainCapacity,
        double waterVolume, double waterDepth, String name, String position,
        WaterloggingStateEnum waterloggingState) {
}

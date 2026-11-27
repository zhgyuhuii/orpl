package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.GeoRegionEntity;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取区域，编译期禁止误调写 Setter
 */
public interface AreaReadOnly {
    String getInstanceId();

    /**
     * 汇水面积
     */
    double getCatchmentArea();

    /**
     * 径流系数
     */
    double getRunoffCoeff();

    /**
     * 等效蓄水面积
     */
    double getEquivalentStorageArea();

    /**
     * 地面高程
     */
    double getGroundElevation();

    /**
     * 地表排水能力
     */
    double getSurfaceDrainCapacity();

    /**
     * 积水量
     */
    double getWaterVolume();

    /**
     * 积水深度
     */
    double getWaterDepth();

    /**
     * 名称
     */
    String getName();

    /**
     * 位置
     */
    GeoRegionEntity getPosition();

    /**
     * 积水状态
     */
    WaterloggingStateEnum getWaterloggingState();
}

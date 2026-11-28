package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction844676b1.model.enums.FloodWallStateEnum;
import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取车站，编译期禁止误调写 Setter
 */
public interface StationReadOnly {
    String getInstanceId();

    /**
     * 站内积水量
     */
    double getStationWaterVolume();

    /**
     * 失效超高阈值
     */
    double getFailureThreshold();

    /**
     * 失效持续时间
     */
    double getFailureDuration();

    /**
     * 挡水墙顶高程
     */
    double getFloodWallTopElevation();

    /**
     * 名称
     */
    String getName();

    /**
     * 排水额定能力
     */
    double getRatedDrainCapacity();

    /**
     * 站内底高程
     */
    double getStationFloorElevation();

    /**
     * 等效蓄水面积
     */
    double getEquivalentStorageArea();

    /**
     * 站内水深
     */
    double getStationWaterDepth();

    /**
     * 疏散能力
     */
    double getEvacuationCapacity();

    /**
     * 位置
     */
    GeoPointEntity getPosition();

    /**
     * 积水状态
     */
    WaterloggingStateEnum getWaterloggingState();

    /**
     * 挡水墙状态
     */
    FloodWallStateEnum getFloodWallState();

    /**
     * 完好时过水宽度
     */
    double getIntactFlowWidth();

    /**
     * 失效后过水宽度
     */
    double getFailedFlowWidth();

    /**
     * 入口底高程
     */
    double getInletFloorElevation();

    /**
     * 入口流量系数
     */
    double getInletFlowCoeff();

    /**
     * 超限累计时间
     */
    double getOverlimitDuration();
}

package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取区间与端点车站，编译期禁止误调写 Setter
 */
public interface SectionWithEndStationReadOnly {
    String getInstanceId();

    /**
     * 车站
     */
    StationEntity getStation();

    /**
     * 区间
     */
    MetroSectionEntity getSection();

    /**
     * 本步过水量
     */
    double getStepFlowVolume();

    /**
     * 连接处底高程
     */
    double getJunctionFloorElevation();

    /**
     * 有效过水宽度
     */
    double getEffectiveFlowWidth();

    /**
     * 水流系数
     */
    double getFlowCoeff();

    /**
     * 当前过水流量
     */
    double getCurrentFlowRate();
}

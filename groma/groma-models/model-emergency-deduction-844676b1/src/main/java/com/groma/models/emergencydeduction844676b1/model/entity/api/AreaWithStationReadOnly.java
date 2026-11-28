package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取区域与区域内车站，编译期禁止误调写 Setter
 */
public interface AreaWithStationReadOnly {
    String getInstanceId();

    /**
     * 区域
     */
    AreaEntity getArea();

    /**
     * 车站
     */
    StationEntity getStation();

    /**
     * 当前过水流量
     */
    double getCurrentFlowRate();

    /**
     * 本步过水量
     */
    double getStepFlowVolume();
}

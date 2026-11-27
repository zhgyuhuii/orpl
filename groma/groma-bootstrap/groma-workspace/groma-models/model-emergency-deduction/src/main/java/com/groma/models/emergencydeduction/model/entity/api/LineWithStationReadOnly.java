package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.MetroLineEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.enums.StationTypeEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取线路与车站，编译期禁止误调写 Setter
 */
public interface LineWithStationReadOnly {
    String getInstanceId();

    /**
     * 地铁线路
     */
    MetroLineEntity getMetroLine();

    /**
     * 车站
     */
    StationEntity getStation();

    /**
     * 线路里程
     */
    double getLineMileage();

    /**
     * 站点类型
     */
    StationTypeEnum getStationType();

    /**
     * 站号
     */
    long getStationNo();
}

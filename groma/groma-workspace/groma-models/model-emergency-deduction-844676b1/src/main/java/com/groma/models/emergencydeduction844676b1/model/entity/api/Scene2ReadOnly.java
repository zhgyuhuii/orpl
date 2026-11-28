package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.AreaWithStationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallAffectedAreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.SectionWithEndStationEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取场景2，编译期禁止误调写 Setter
 */
public interface Scene2ReadOnly {
    String getInstanceId();

    /**
     * 降雨关联区域
     */
    RainfallAffectedAreaEntity getRainfallRelatedArea();

    /**
     * 区域与区域内车站
     */
    AreaWithStationEntity getAreaWithStation();

    /**
     * 区间与端点车站
     */
    SectionWithEndStationEntity getSectionWithEndStation();

    /**
     * 线路与列车
     */
    LineWithTrainEntity getLineWithTrain();
}

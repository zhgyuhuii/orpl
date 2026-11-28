package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取区间与区间内列车，编译期禁止误调写 Setter
 */
public interface SectionWithTrainReadOnly {
    String getInstanceId();

    /**
     * 列车
     */
    TrainEntity getTrain();

    /**
     * 区间
     */
    MetroSectionEntity getSection();

    /**
     * 已行驶距离
     */
    double getTravelledDistance();

    /**
     * 目标车站
     */
    StationEntity getTargetStation();

    /**
     * 出发车站
     */
    StationEntity getDepartureStation();

    /**
     * 是否到达目标车站
     */
    boolean isArrivedTargetStation();
}

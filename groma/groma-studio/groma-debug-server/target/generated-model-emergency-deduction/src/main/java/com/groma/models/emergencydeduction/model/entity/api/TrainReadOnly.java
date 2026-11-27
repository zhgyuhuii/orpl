package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取列车，编译期禁止误调写 Setter
 */
public interface TrainReadOnly {
    String getInstanceId();

    /**
     * 当前速度
     */
    double getCurrentSpeed();

    /**
     * 迫停时刻
     */
    double getForcedStopTime();

    /**
     * 初始位置
     */
    GeoPointEntity getInitialPosition();

    /**
     * 名称
     */
    String getName();

    /**
     * 列车状态
     */
    TrainStateEnum getTrainState();

    /**
     * 方向
     */
    double getDirection();

    /**
     * 当前位置
     */
    GeoPointEntity getCurrentPosition();

    /**
     * 行驶速度
     */
    double getTravelSpeed();
}

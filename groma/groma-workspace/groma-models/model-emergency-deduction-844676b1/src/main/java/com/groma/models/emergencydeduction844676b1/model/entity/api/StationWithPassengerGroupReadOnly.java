package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import com.groma.models.emergencydeduction844676b1.model.enums.EvacuationStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取车站与站内乘客群，编译期禁止误调写 Setter
 */
public interface StationWithPassengerGroupReadOnly {
    String getInstanceId();

    /**
     * 乘客群
     */
    PassengerGroupEntity getPassengerGroup();

    /**
     * 车站
     */
    StationEntity getStation();

    /**
     * 当前站内人数
     */
    long getCurrentStationPeopleCount();

    /**
     * 已疏散人数
     */
    long getEvacuatedPeopleCount();

    /**
     * 疏散是否启动
     */
    boolean isEvacuationStarted();

    /**
     * 疏散状态
     */
    EvacuationStateEnum getEvacuationState();
}

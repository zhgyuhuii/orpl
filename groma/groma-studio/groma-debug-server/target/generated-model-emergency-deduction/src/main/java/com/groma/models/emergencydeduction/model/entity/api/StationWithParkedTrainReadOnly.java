package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取车站与停靠列车，编译期禁止误调写 Setter
 */
public interface StationWithParkedTrainReadOnly {
    String getInstanceId();

    /**
     * 列车
     */
    TrainEntity getTrain();

    /**
     * 车站
     */
    StationEntity getStation();

    /**
     * 实际离站时刻
     */
    long getActualDepartureTime();

    /**
     * 实际到站时刻
     */
    long getActualArrivalTime();

    /**
     * 规定停站时长
     */
    long getPrescribedStopDuration();

    /**
     * 离站是否允许
     */
    boolean isDepartureAllowed();
}

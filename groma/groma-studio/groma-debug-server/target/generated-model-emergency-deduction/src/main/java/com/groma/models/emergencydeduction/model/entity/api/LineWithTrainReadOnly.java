package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.MetroLineEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取线路与列车，编译期禁止误调写 Setter
 */
public interface LineWithTrainReadOnly {
    String getInstanceId();

    /**
     * 线路
     */
    MetroLineEntity getLine();

    /**
     * 列车
     */
    TrainEntity getTrain();

    /**
     * 列车当前在线路上的里程
     */
    double getTrainMileageOnLine();

    /**
     * 沿里程增大方向
     */
    boolean isAlongIncreasingMileage();

    /**
     * 当前停靠关联
     */
    StationWithParkedTrainEntity getCurrentParkedRelation();

    /**
     * 当前行驶关联
     */
    SectionWithTrainEntity getCurrentRunningRelation();

    /**
     * 演示往返
     */
    boolean isDemoRoundTrip();
}

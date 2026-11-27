package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取列车与车内乘客群，编译期禁止误调写 Setter
 */
public interface TrainWithPassengerGroupReadOnly {
    String getInstanceId();

    /**
     * 列车
     */
    TrainEntity getTrain();

    /**
     * 乘客群
     */
    PassengerGroupEntity getPassengerGroup();

    /**
     * 当前在车人数
     */
    long getCurrentOnBoardCount();
}

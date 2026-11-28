package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.enums.PassengerStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取乘客群，编译期禁止误调写 Setter
 */
public interface PassengerGroupReadOnly {
    String getInstanceId();

    /**
     * 名称
     */
    String getName();

    /**
     * 总人数
     */
    long getTotalPeopleCount();

    /**
     * 乘客状态
     */
    PassengerStateEnum getPassengerState();

    /**
     * 首次受困时刻
     */
    double getFirstTrappedTime();

    /**
     * 全部安全时刻
     */
    double getAllSafeTime();

    /**
     * 已安全人数
     */
    long getSafePeopleCount();

    /**
     * 是否经历危险水深
     */
    boolean isExperiencedDangerDepth();
}

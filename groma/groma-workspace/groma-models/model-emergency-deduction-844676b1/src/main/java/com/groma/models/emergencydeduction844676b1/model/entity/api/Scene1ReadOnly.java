package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.enums.TrainStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取场景1，编译期禁止误调写 Setter
 */
public interface Scene1ReadOnly {
    String getInstanceId();

    /**
     * 列车状态1
     */
    TrainStateEnum getTrainState1();

    /**
     * 文本1
     */
    boolean isText1();

    /**
     * 文本2
     */
    String getText2();
}

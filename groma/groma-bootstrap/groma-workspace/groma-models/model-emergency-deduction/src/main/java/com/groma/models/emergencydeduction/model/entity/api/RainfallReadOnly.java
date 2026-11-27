package com.groma.models.emergencydeduction.model.entity.api;

import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取降雨，编译期禁止误调写 Setter
 */
public interface RainfallReadOnly {
    String getInstanceId();
}

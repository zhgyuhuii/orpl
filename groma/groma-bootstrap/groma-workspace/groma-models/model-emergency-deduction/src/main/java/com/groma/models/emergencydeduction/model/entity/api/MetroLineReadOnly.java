package com.groma.models.emergencydeduction.model.entity.api;

import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取地铁线路，编译期禁止误调写 Setter
 */
public interface MetroLineReadOnly {
    String getInstanceId();

    /**
     * 线路名称
     */
    String getLineName();

    /**
     * 起点里程
     */
    double getStartMileage();

    /**
     * 终点里程
     */
    double getEndMileage();
}

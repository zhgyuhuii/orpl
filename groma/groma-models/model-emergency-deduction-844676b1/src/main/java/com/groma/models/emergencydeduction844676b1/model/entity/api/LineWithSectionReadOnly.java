package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.MetroLineEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取线路与区间，编译期禁止误调写 Setter
 */
public interface LineWithSectionReadOnly {
    String getInstanceId();

    /**
     * 地铁线路
     */
    MetroLineEntity getMetroLine();

    /**
     * 地铁区间
     */
    MetroSectionEntity getMetroSection();

    /**
     * 区间起点里程
     */
    double getSectionStartMileage();

    /**
     * 区间终点里程
     */
    double getSectionEndMileage();
}

package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取降雨汇水，编译期禁止误调写 Setter
 */
public interface RainfallCatchmentReadOnly {
    String getInstanceId();

    /**
     * 降雨来源
     */
    RainfallEntity getRainfallSource();

    /**
     * 汇水区域
     */
    List<AreaEntity> getCatchmentArea();
}

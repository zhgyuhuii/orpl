package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取地下进水，编译期禁止误调写 Setter
 */
public interface UndergroundInflowReadOnly {
    String getInstanceId();

    /**
     * 地面积水来源
     */
    AreaEntity getSurfaceWaterloggingSource();

    /**
     * 进水区间
     */
    List<MetroSectionEntity> getInflowSection();
}

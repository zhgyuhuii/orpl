package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.GeoPointEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取地理区域，编译期禁止误调写 Setter
 */
public interface GeoRegionReadOnly {
    String getInstanceId();

    /**
     * 坐标组
     */
    List<GeoPointEntity> getCoordinateGroup();
}

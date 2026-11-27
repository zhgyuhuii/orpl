package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取地理路线，编译期禁止误调写 Setter
 */
public interface GeoRouteReadOnly {
    String getInstanceId();

    /**
     * 坐标组
     */
    List<GeoPointEntity> getCoordinateGroup();
}

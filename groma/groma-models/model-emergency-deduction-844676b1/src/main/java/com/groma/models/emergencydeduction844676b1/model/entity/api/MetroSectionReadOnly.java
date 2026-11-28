package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.GeoRouteEntity;
import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取地铁区间，编译期禁止误调写 Setter
 */
public interface MetroSectionReadOnly {
    String getInstanceId();

    /**
     * 长度
     */
    double getLength();

    /**
     * 等效储水宽度
     */
    double getEquivalentStorageWidth();

    /**
     * 危险水深
     */
    double getDangerDepth();

    /**
     * 迫停水深
     */
    double getForcedStopDepth();

    /**
     * 积水量
     */
    double getWaterVolume();

    /**
     * 代表性水深
     */
    double getRepresentativeDepth();

    /**
     * 是否放行
     */
    boolean isReleaseAllowed();

    /**
     * 名称
     */
    String getName();

    /**
     * 位置
     */
    GeoRouteEntity getPosition();

    /**
     * 代表底高程
     */
    double getRepresentativeFloorElevation();

    /**
     * 积水状态
     */
    WaterloggingStateEnum getWaterloggingState();
}

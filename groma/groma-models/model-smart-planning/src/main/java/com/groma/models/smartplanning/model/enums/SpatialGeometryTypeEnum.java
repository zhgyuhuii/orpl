package com.groma.models.smartplanning.model.enums;

import com.groma.base.common.annotation.GromaMeta;

@GromaMeta(id = "enum_spatial_type", zhName = "空间类型", kind = "ENUM")
public enum SpatialGeometryTypeEnum {
    POINT("点位"),
    POLYLINE("航线"),
    POLYGON("区域");

    private final String zhLabel;

    SpatialGeometryTypeEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    public String getZhLabel() { return zhLabel; }
}

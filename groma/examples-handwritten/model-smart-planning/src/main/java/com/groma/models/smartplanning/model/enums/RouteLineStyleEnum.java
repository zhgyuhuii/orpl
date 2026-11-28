package com.groma.models.smartplanning.model.enums;

import com.groma.base.common.annotation.GromaMeta;

@GromaMeta(id = "enum_line_style", zhName = "线条类型", kind = "ENUM")
public enum RouteLineStyleEnum {
    SOLID("实线"),
    DASHED("虚线");

    private final String zhLabel;

    RouteLineStyleEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    public String getZhLabel() { return zhLabel; }
}

package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;

@GromaMeta(id = "ufT4BZeZh3AjkAklBo1", zhName = "预警等级", kind = "ENUM")
public enum WarningLevelEnum {
    BLUE("蓝"),
    YELLOW("黄"),
    ORANGE("橙"),
    RED("红");

    private final String zhLabel;

    WarningLevelEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    public String getZhLabel() {
        return zhLabel;
    }
}

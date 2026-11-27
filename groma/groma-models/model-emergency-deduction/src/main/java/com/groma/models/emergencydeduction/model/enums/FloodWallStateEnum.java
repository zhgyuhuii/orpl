package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;

@GromaMeta(id = "2f8e379d34e04df88c71fdcb5a6a5732", zhName = "挡水墙状态", kind = "ENUM")
public enum FloodWallStateEnum {
    INTACT("完好"),
    FAILED("失效");

    private final String zhLabel;

    FloodWallStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    public String getZhLabel() {
        return zhLabel;
    }
}

package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;

@GromaMeta(id = "p8mFk8bALYHTJQRDpxJ", zhName = "积水状态", kind = "ENUM")
public enum WaterloggingStateEnum {
    NO_WATERLOGGING("无积水"),
    WATERLOGGED("积水");

    private final String zhLabel;

    WaterloggingStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    public String getZhLabel() {
        return zhLabel;
    }
}

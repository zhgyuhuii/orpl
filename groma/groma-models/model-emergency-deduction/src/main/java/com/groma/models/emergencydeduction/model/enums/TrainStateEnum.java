package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;

@GromaMeta(id = "DFs1K7r5tqlGFbWC4gm", zhName = "列车状态", kind = "ENUM")
public enum TrainStateEnum {
    STOPPED_AT_STATION("停站"),
    RUNNING("行驶"),
    FORCED_STOP("迫停");

    private final String zhLabel;

    TrainStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    public String getZhLabel() {
        return zhLabel;
    }
}

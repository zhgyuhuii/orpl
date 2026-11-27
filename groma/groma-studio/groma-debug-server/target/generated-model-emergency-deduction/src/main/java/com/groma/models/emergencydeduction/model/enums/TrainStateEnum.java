package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：列车状态（状态顺序与画布一致；默认初始状态：待进入）
 */
@GromaMeta(
        id = "XGrwtoGP8uUCfrXqgWc",
        zhName = "列车状态",
        kind = "ENUM"
)
public enum TrainStateEnum {
    /**
     * 停站
     */
    STOPPED_AT_STATION("停站"),

    /**
     * 行驶
     */
    RUN("行驶"),

    /**
     * 迫停
     */
    FORCED_STOP("迫停"),

    /**
     * 待进入
     */
    PENDING_ENTRY("待进入");

    private final String zhLabel;

    TrainStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

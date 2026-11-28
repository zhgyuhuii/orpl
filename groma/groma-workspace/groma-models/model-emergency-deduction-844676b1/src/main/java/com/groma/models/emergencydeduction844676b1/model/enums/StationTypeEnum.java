package com.groma.models.emergencydeduction844676b1.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：站点类型（状态顺序与画布一致；默认初始状态：中间站）
 */
@GromaMeta(
        id = "zgOFG4oqsv53DQKSGM2",
        zhName = "站点类型",
        kind = "ENUM"
)
public enum StationTypeEnum {
    /**
     * 始发站
     */
    ORIGIN_STATION("始发站"),

    /**
     * 终点站
     */
    TERMINAL_STATION("终点站"),

    /**
     * 中间站
     */
    INTERMEDIATE_STATION("中间站");

    private final String zhLabel;

    StationTypeEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：预警等级（状态顺序与画布一致；默认初始状态：蓝）
 */
@GromaMeta(
        id = "tWd1eAWxb5ggu8qz447",
        zhName = "预警等级",
        kind = "ENUM"
)
public enum WarningLevelEnum {
    /**
     * 蓝
     */
    BLUE("蓝"),

    /**
     * 黄
     */
    YELLOW("黄"),

    /**
     * 橙
     */
    ORANGE("橙"),

    /**
     * 红
     */
    RED("红");

    private final String zhLabel;

    WarningLevelEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

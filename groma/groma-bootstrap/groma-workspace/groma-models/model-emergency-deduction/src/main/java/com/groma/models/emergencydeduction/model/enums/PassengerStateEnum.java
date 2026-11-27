package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：乘客状态（状态顺序与画布一致；默认初始状态：正常）
 */
@GromaMeta(
        id = "rcqM3MPGoItwHZIUhQq",
        zhName = "乘客状态",
        kind = "ENUM"
)
public enum PassengerStateEnum {
    /**
     * 正常
     */
    NORMAL("正常"),

    /**
     * 受困
     */
    TRAPPED("受困"),

    /**
     * 疏散中
     */
    EVACUATING("疏散中"),

    /**
     * 已安全
     */
    SAFE("已安全");

    private final String zhLabel;

    PassengerStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：停靠状态（状态顺序与画布一致；默认初始状态：未开始）
 */
@GromaMeta(
        id = "BT8uFA2HI1kcc0HUbrY",
        zhName = "停靠状态",
        kind = "ENUM"
)
public enum DockingStateEnum {
    /**
     * 未开始
     */
    NOT_STARTED("未开始"),

    /**
     * 停靠中
     */
    DOCKING("停靠中"),

    /**
     * 已结束
     */
    FINISHED("已结束");

    private final String zhLabel;

    DockingStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

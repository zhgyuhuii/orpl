package com.groma.models.emergencydeduction844676b1.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：通行状态（状态顺序与画布一致；默认初始状态：待进入）
 */
@GromaMeta(
        id = "hHttkvr7XJGKzH5rGWD",
        zhName = "通行状态",
        kind = "ENUM"
)
public enum PassingStateEnum {
    /**
     * 待进入
     */
    PENDING_ENTRY("待进入"),

    /**
     * 通行中
     */
    PASSING("通行中"),

    /**
     * 已完成
     */
    COMPLETED("已完成"),

    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String zhLabel;

    PassingStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

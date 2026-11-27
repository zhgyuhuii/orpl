package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：任务状态（状态顺序与画布一致；默认初始状态：待下达）
 */
@GromaMeta(
        id = "E28pEyQ2G9A9CJxdfs4",
        zhName = "任务状态",
        kind = "ENUM"
)
public enum TaskStateEnum {
    /**
     * 待下达
     */
    PENDING_ISSUE("待下达"),

    /**
     * 执行中
     */
    EXECUTING("执行中"),

    /**
     * 已完成
     */
    COMPLETED("已完成");

    private final String zhLabel;

    TaskStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

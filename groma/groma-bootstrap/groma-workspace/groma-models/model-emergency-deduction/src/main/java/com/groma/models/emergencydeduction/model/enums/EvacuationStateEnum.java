package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：疏散状态（状态顺序与画布一致；默认初始状态：待疏散）
 */
@GromaMeta(
        id = "RTMDG3oSxDG0XtzmVxC",
        zhName = "疏散状态",
        kind = "ENUM"
)
public enum EvacuationStateEnum {
    /**
     * 待疏散
     */
    PENDING_EVACUATION("待疏散"),

    /**
     * 疏散中
     */
    EVACUATING("疏散中"),

    /**
     * 已完成
     */
    COMPLETED("已完成");

    private final String zhLabel;

    EvacuationStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

package com.groma.models.emergencydeduction.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：积水状态（状态顺序与画布一致；默认初始状态：无积水）
 */
@GromaMeta(
        id = "0FspcLs0518qUherR8K",
        zhName = "积水状态",
        kind = "ENUM"
)
public enum WaterloggingStateEnum {
    /**
     * 无积水
     */
    NO_WATERLOGGING("无积水"),

    /**
     * 积水
     */
    WATERLOGGING("积水");

    private final String zhLabel;

    WaterloggingStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

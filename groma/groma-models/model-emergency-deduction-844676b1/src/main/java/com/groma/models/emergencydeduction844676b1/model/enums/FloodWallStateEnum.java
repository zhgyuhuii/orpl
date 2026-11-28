package com.groma.models.emergencydeduction844676b1.model.enums;

import com.groma.base.common.annotation.GromaMeta;
import java.lang.String;

/**
 * 方圆枚举类型：挡水墙状态（状态顺序与画布一致；默认初始状态：完好）
 */
@GromaMeta(
        id = "nrgTvUq55JNeiMMlawc",
        zhName = "挡水墙状态",
        kind = "ENUM"
)
public enum FloodWallStateEnum {
    /**
     * 完好
     */
    INTACT("完好"),

    /**
     * 失效
     */
    FAILED("失效");

    private final String zhLabel;

    FloodWallStateEnum(String zhLabel) {
        this.zhLabel = zhLabel;
    }

    /**
     * 状态中文原名
     */
    public String getZhLabel() {
        return this.zhLabel;
    }
}

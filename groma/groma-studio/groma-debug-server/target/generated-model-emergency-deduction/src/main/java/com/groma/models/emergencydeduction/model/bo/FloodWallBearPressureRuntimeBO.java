package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.enums.FloodWallStateEnum;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：挡水墙承受水压）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "6gimbvYC5CskuHCzVFQ#bo",
        zhName = "挡水墙承受水压运行帧",
        kind = "RUNTIME_BO"
)
public final class FloodWallBearPressureRuntimeBO {
    /**
     * 局部量（方圆原名：挡水墙完好）
     */
    private FloodWallStateEnum floodWallIntact = FloodWallStateEnum.INTACT;

    /**
     * 局部量（方圆原名：失效判定高程）
     */
    private double failureJudgeElevation = 0.0D;

    /**
     * 节点输出：等于
     */
    private Boolean out0;

    /**
     * 节点输出：加
     */
    private Double out1;

    /**
     * 节点输出：大于等于
     */
    private Boolean out2;

    /**
     * 节点输出：大于等于
     */
    private Boolean out5;

    /**
     * 局部量：挡水墙完好
     */
    public FloodWallStateEnum getFloodWallIntact() {
        return this.floodWallIntact;
    }

    public void setFloodWallIntact(FloodWallStateEnum value) {
        this.floodWallIntact = value;
    }

    /**
     * 局部量：失效判定高程
     */
    public double getFailureJudgeElevation() {
        return this.failureJudgeElevation;
    }

    public void setFailureJudgeElevation(double value) {
        this.failureJudgeElevation = value;
    }

    /**
     * 节点输出：等于
     */
    public Boolean getOut0() {
        return this.out0;
    }

    public void setOut0(Boolean value) {
        this.out0 = value;
    }

    /**
     * 节点输出：加
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    /**
     * 节点输出：大于等于
     */
    public Boolean getOut2() {
        return this.out2;
    }

    public void setOut2(Boolean value) {
        this.out2 = value;
    }

    /**
     * 节点输出：大于等于
     */
    public Boolean getOut5() {
        return this.out5;
    }

    public void setOut5(Boolean value) {
        this.out5 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("floodWallIntact", GromaValues.encode(this.floodWallIntact));
        state.put("failureJudgeElevation", GromaValues.encode(this.failureJudgeElevation));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out5", GromaValues.encode(this.out5));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.floodWallIntact = state.get("floodWallIntact") == null ? null : GromaValues.toEnum(FloodWallStateEnum.class, state.get("floodWallIntact"));
        this.failureJudgeElevation = (double) GromaValues.decode(state.get("failureJudgeElevation"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Boolean) GromaValues.decode(state.get("out2"), ctx);
        this.out5 = (Boolean) GromaValues.decode(state.get("out5"), ctx);
    }
}

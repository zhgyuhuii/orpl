package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.DetermineJunctionFlowProcessTask;
import com.groma.models.emergencydeduction844676b1.model.enums.FloodWallStateEnum;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：入口过水）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "zlRqcFoeLHuEKONcV2V#bo",
        zhName = "入口过水运行帧",
        kind = "RUNTIME_BO"
)
public final class InletFlowRuntimeBO {
    /**
     * 局部量（方圆原名：挡水墙完好）
     */
    private FloodWallStateEnum floodWallIntact = FloodWallStateEnum.INTACT;

    /**
     * 局部量（方圆原名：过水控制高程）
     */
    private double flowControlElevation = 0.0D;

    /**
     * 局部量（方圆原名：过水宽度）
     */
    private double flowWidth = 0.0D;

    /**
     * 节点输出：等于
     */
    private Boolean out0;

    /**
     * 节点输出：赋值
     */
    private Double out1;

    /**
     * 节点输出：赋值
     */
    private Double out2;

    /**
     * 节点输出：赋值
     */
    private Double out3;

    /**
     * 节点输出：赋值
     */
    private Double out4;

    /**
     * 子过程句柄：确定连接处过水情况
     */
    private DetermineJunctionFlowProcessTask child5;

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
     * 局部量：过水控制高程
     */
    public double getFlowControlElevation() {
        return this.flowControlElevation;
    }

    public void setFlowControlElevation(double value) {
        this.flowControlElevation = value;
    }

    /**
     * 局部量：过水宽度
     */
    public double getFlowWidth() {
        return this.flowWidth;
    }

    public void setFlowWidth(double value) {
        this.flowWidth = value;
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
     * 节点输出：赋值
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut2() {
        return this.out2;
    }

    public void setOut2(Double value) {
        this.out2 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut3() {
        return this.out3;
    }

    public void setOut3(Double value) {
        this.out3 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut4() {
        return this.out4;
    }

    public void setOut4(Double value) {
        this.out4 = value;
    }

    /**
     * 子过程句柄：确定连接处过水情况
     */
    public DetermineJunctionFlowProcessTask getChild5() {
        return this.child5;
    }

    public void setChild5(DetermineJunctionFlowProcessTask value) {
        this.child5 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("floodWallIntact", GromaValues.encode(this.floodWallIntact));
        state.put("flowControlElevation", GromaValues.encode(this.flowControlElevation));
        state.put("flowWidth", GromaValues.encode(this.flowWidth));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("child5", this.child5 == null ? "" : this.child5.getProcessInstanceId());
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.floodWallIntact = state.get("floodWallIntact") == null ? null : GromaValues.toEnum(FloodWallStateEnum.class, state.get("floodWallIntact"));
        this.flowControlElevation = (double) GromaValues.decode(state.get("flowControlElevation"), ctx);
        this.flowWidth = (double) GromaValues.decode(state.get("flowWidth"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
    }
}

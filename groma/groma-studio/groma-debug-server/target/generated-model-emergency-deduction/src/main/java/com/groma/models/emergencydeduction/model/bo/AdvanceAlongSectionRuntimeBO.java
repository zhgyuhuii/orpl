package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：沿区间前进）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "7rSlUlS0cSWjxGaiErg#bo",
        zhName = "沿区间前进运行帧",
        kind = "RUNTIME_BO"
)
public final class AdvanceAlongSectionRuntimeBO {
    /**
     * 局部量（方圆原名：本步计划行驶距离）
     */
    private double stepPlannedTravelDistance = 0.0D;

    /**
     * 局部量（方圆原名：本步实际行驶距离）
     */
    private double stepActualTravelDistance = 0.0D;

    /**
     * 局部量（方圆原名：沿里程增大方向）
     */
    private boolean alongIncreasingMileage2 = true;

    /**
     * 局部量（方圆原名：行驶进度）
     */
    private double travelProgress = 0.0D;

    /**
     * 节点输出：乘
     */
    private Double out0;

    /**
     * 节点输出：除
     */
    private Double out1;

    /**
     * 节点输出：减
     */
    private Double out2;

    /**
     * 节点输出：取小
     */
    private Double out3;

    /**
     * 节点输出：等于
     */
    private Boolean out4;

    /**
     * 节点输出：除
     */
    private Double out8;

    /**
     * 节点输出：减
     */
    private Double out9;

    /**
     * 节点输出：乘
     */
    private Double out10;

    /**
     * 节点输出：加
     */
    private Double out11;

    /**
     * 节点输出：减
     */
    private Double out12;

    /**
     * 节点输出：乘
     */
    private Double out13;

    /**
     * 节点输出：加
     */
    private Double out14;

    /**
     * 局部量：本步计划行驶距离
     */
    public double getStepPlannedTravelDistance() {
        return this.stepPlannedTravelDistance;
    }

    public void setStepPlannedTravelDistance(double value) {
        this.stepPlannedTravelDistance = value;
    }

    /**
     * 局部量：本步实际行驶距离
     */
    public double getStepActualTravelDistance() {
        return this.stepActualTravelDistance;
    }

    public void setStepActualTravelDistance(double value) {
        this.stepActualTravelDistance = value;
    }

    /**
     * 局部量：沿里程增大方向
     */
    public boolean getAlongIncreasingMileage2() {
        return this.alongIncreasingMileage2;
    }

    public void setAlongIncreasingMileage2(boolean value) {
        this.alongIncreasingMileage2 = value;
    }

    /**
     * 局部量：行驶进度
     */
    public double getTravelProgress() {
        return this.travelProgress;
    }

    public void setTravelProgress(double value) {
        this.travelProgress = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut0() {
        return this.out0;
    }

    public void setOut0(Double value) {
        this.out0 = value;
    }

    /**
     * 节点输出：除
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    /**
     * 节点输出：减
     */
    public Double getOut2() {
        return this.out2;
    }

    public void setOut2(Double value) {
        this.out2 = value;
    }

    /**
     * 节点输出：取小
     */
    public Double getOut3() {
        return this.out3;
    }

    public void setOut3(Double value) {
        this.out3 = value;
    }

    /**
     * 节点输出：等于
     */
    public Boolean getOut4() {
        return this.out4;
    }

    public void setOut4(Boolean value) {
        this.out4 = value;
    }

    /**
     * 节点输出：除
     */
    public Double getOut8() {
        return this.out8;
    }

    public void setOut8(Double value) {
        this.out8 = value;
    }

    /**
     * 节点输出：减
     */
    public Double getOut9() {
        return this.out9;
    }

    public void setOut9(Double value) {
        this.out9 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut10() {
        return this.out10;
    }

    public void setOut10(Double value) {
        this.out10 = value;
    }

    /**
     * 节点输出：加
     */
    public Double getOut11() {
        return this.out11;
    }

    public void setOut11(Double value) {
        this.out11 = value;
    }

    /**
     * 节点输出：减
     */
    public Double getOut12() {
        return this.out12;
    }

    public void setOut12(Double value) {
        this.out12 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut13() {
        return this.out13;
    }

    public void setOut13(Double value) {
        this.out13 = value;
    }

    /**
     * 节点输出：加
     */
    public Double getOut14() {
        return this.out14;
    }

    public void setOut14(Double value) {
        this.out14 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("stepPlannedTravelDistance", GromaValues.encode(this.stepPlannedTravelDistance));
        state.put("stepActualTravelDistance", GromaValues.encode(this.stepActualTravelDistance));
        state.put("alongIncreasingMileage2", GromaValues.encode(this.alongIncreasingMileage2));
        state.put("travelProgress", GromaValues.encode(this.travelProgress));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("out8", GromaValues.encode(this.out8));
        state.put("out9", GromaValues.encode(this.out9));
        state.put("out10", GromaValues.encode(this.out10));
        state.put("out11", GromaValues.encode(this.out11));
        state.put("out12", GromaValues.encode(this.out12));
        state.put("out13", GromaValues.encode(this.out13));
        state.put("out14", GromaValues.encode(this.out14));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.stepPlannedTravelDistance = (double) GromaValues.decode(state.get("stepPlannedTravelDistance"), ctx);
        this.stepActualTravelDistance = (double) GromaValues.decode(state.get("stepActualTravelDistance"), ctx);
        this.alongIncreasingMileage2 = (boolean) GromaValues.decode(state.get("alongIncreasingMileage2"), ctx);
        this.travelProgress = (double) GromaValues.decode(state.get("travelProgress"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Boolean) GromaValues.decode(state.get("out4"), ctx);
        this.out8 = (Double) GromaValues.decode(state.get("out8"), ctx);
        this.out9 = (Double) GromaValues.decode(state.get("out9"), ctx);
        this.out10 = (Double) GromaValues.decode(state.get("out10"), ctx);
        this.out11 = (Double) GromaValues.decode(state.get("out11"), ctx);
        this.out12 = (Double) GromaValues.decode(state.get("out12"), ctx);
        this.out13 = (Double) GromaValues.decode(state.get("out13"), ctx);
        this.out14 = (Double) GromaValues.decode(state.get("out14"), ctx);
    }
}

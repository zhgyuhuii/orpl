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
 * 过程运行帧（方圆过程原名：确定连接处过水情况）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "TUmeBFJE9XAot7jIGeV#bo",
        zhName = "确定连接处过水情况运行帧",
        kind = "RUNTIME_BO"
)
public final class DetermineJunctionFlowRuntimeBO {
    /**
     * 局部量（方圆原名：流出侧水面高程）
     */
    private double outflowSideWaterLevel = 0.0D;

    /**
     * 局部量（方圆原名：流入侧水面高程）
     */
    private double inflowSideWaterLevel = 0.0D;

    /**
     * 局部量（方圆原名：流出侧积水量）
     */
    private double outflowSideWaterVolume = 0.0D;

    /**
     * 局部量（方圆原名：第一侧流向第二侧）
     */
    private boolean firstFlowsToSecond2 = true;

    /**
     * 局部量（方圆原名：第二侧流向第一侧）
     */
    private boolean secondFlowsToFirst = false;

    /**
     * 局部量（方圆原名：有效龙头）
     */
    private double effectiveCrest = 0.0D;

    /**
     * 局部量（方圆原名：预计过水流量）
     */
    private double estimatedFlowRate = 0.0D;

    /**
     * 节点输出：大于
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
     * 节点输出：赋值
     */
    private Double out5;

    /**
     * 节点输出：赋值
     */
    private Double out6;

    /**
     * 节点输出：赋值
     */
    private Boolean out7;

    /**
     * 节点输出：赋值
     */
    private Boolean out8;

    /**
     * 节点输出：取大
     */
    private Double out9;

    /**
     * 节点输出：减
     */
    private Double out10;

    /**
     * 节点输出：取大
     */
    private Double out11;

    /**
     * 节点输出：乘
     */
    private Double out12;

    /**
     * 节点输出：乘
     */
    private Double out13;

    /**
     * 节点输出：取a的b次幂
     */
    private Double out14;

    /**
     * 节点输出：乘
     */
    private Double out15;

    /**
     * 节点输出：乘
     */
    private Double out16;

    /**
     * 节点输出：取小
     */
    private Double out17;

    /**
     * 节点输出：除
     */
    private Double out18;

    /**
     * 节点输出：乘
     */
    private Double out19;

    /**
     * 局部量：流出侧水面高程
     */
    public double getOutflowSideWaterLevel() {
        return this.outflowSideWaterLevel;
    }

    public void setOutflowSideWaterLevel(double value) {
        this.outflowSideWaterLevel = value;
    }

    /**
     * 局部量：流入侧水面高程
     */
    public double getInflowSideWaterLevel() {
        return this.inflowSideWaterLevel;
    }

    public void setInflowSideWaterLevel(double value) {
        this.inflowSideWaterLevel = value;
    }

    /**
     * 局部量：流出侧积水量
     */
    public double getOutflowSideWaterVolume() {
        return this.outflowSideWaterVolume;
    }

    public void setOutflowSideWaterVolume(double value) {
        this.outflowSideWaterVolume = value;
    }

    /**
     * 局部量：第一侧流向第二侧
     */
    public boolean getFirstFlowsToSecond2() {
        return this.firstFlowsToSecond2;
    }

    public void setFirstFlowsToSecond2(boolean value) {
        this.firstFlowsToSecond2 = value;
    }

    /**
     * 局部量：第二侧流向第一侧
     */
    public boolean getSecondFlowsToFirst() {
        return this.secondFlowsToFirst;
    }

    public void setSecondFlowsToFirst(boolean value) {
        this.secondFlowsToFirst = value;
    }

    /**
     * 局部量：有效龙头
     */
    public double getEffectiveCrest() {
        return this.effectiveCrest;
    }

    public void setEffectiveCrest(double value) {
        this.effectiveCrest = value;
    }

    /**
     * 局部量：预计过水流量
     */
    public double getEstimatedFlowRate() {
        return this.estimatedFlowRate;
    }

    public void setEstimatedFlowRate(double value) {
        this.estimatedFlowRate = value;
    }

    /**
     * 节点输出：大于
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
     * 节点输出：赋值
     */
    public Double getOut5() {
        return this.out5;
    }

    public void setOut5(Double value) {
        this.out5 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut6() {
        return this.out6;
    }

    public void setOut6(Double value) {
        this.out6 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Boolean getOut7() {
        return this.out7;
    }

    public void setOut7(Boolean value) {
        this.out7 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Boolean getOut8() {
        return this.out8;
    }

    public void setOut8(Boolean value) {
        this.out8 = value;
    }

    /**
     * 节点输出：取大
     */
    public Double getOut9() {
        return this.out9;
    }

    public void setOut9(Double value) {
        this.out9 = value;
    }

    /**
     * 节点输出：减
     */
    public Double getOut10() {
        return this.out10;
    }

    public void setOut10(Double value) {
        this.out10 = value;
    }

    /**
     * 节点输出：取大
     */
    public Double getOut11() {
        return this.out11;
    }

    public void setOut11(Double value) {
        this.out11 = value;
    }

    /**
     * 节点输出：乘
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
     * 节点输出：取a的b次幂
     */
    public Double getOut14() {
        return this.out14;
    }

    public void setOut14(Double value) {
        this.out14 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut15() {
        return this.out15;
    }

    public void setOut15(Double value) {
        this.out15 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut16() {
        return this.out16;
    }

    public void setOut16(Double value) {
        this.out16 = value;
    }

    /**
     * 节点输出：取小
     */
    public Double getOut17() {
        return this.out17;
    }

    public void setOut17(Double value) {
        this.out17 = value;
    }

    /**
     * 节点输出：除
     */
    public Double getOut18() {
        return this.out18;
    }

    public void setOut18(Double value) {
        this.out18 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut19() {
        return this.out19;
    }

    public void setOut19(Double value) {
        this.out19 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("outflowSideWaterLevel", GromaValues.encode(this.outflowSideWaterLevel));
        state.put("inflowSideWaterLevel", GromaValues.encode(this.inflowSideWaterLevel));
        state.put("outflowSideWaterVolume", GromaValues.encode(this.outflowSideWaterVolume));
        state.put("firstFlowsToSecond2", GromaValues.encode(this.firstFlowsToSecond2));
        state.put("secondFlowsToFirst", GromaValues.encode(this.secondFlowsToFirst));
        state.put("effectiveCrest", GromaValues.encode(this.effectiveCrest));
        state.put("estimatedFlowRate", GromaValues.encode(this.estimatedFlowRate));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        state.put("out7", GromaValues.encode(this.out7));
        state.put("out8", GromaValues.encode(this.out8));
        state.put("out9", GromaValues.encode(this.out9));
        state.put("out10", GromaValues.encode(this.out10));
        state.put("out11", GromaValues.encode(this.out11));
        state.put("out12", GromaValues.encode(this.out12));
        state.put("out13", GromaValues.encode(this.out13));
        state.put("out14", GromaValues.encode(this.out14));
        state.put("out15", GromaValues.encode(this.out15));
        state.put("out16", GromaValues.encode(this.out16));
        state.put("out17", GromaValues.encode(this.out17));
        state.put("out18", GromaValues.encode(this.out18));
        state.put("out19", GromaValues.encode(this.out19));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.outflowSideWaterLevel = (double) GromaValues.decode(state.get("outflowSideWaterLevel"), ctx);
        this.inflowSideWaterLevel = (double) GromaValues.decode(state.get("inflowSideWaterLevel"), ctx);
        this.outflowSideWaterVolume = (double) GromaValues.decode(state.get("outflowSideWaterVolume"), ctx);
        this.firstFlowsToSecond2 = (boolean) GromaValues.decode(state.get("firstFlowsToSecond2"), ctx);
        this.secondFlowsToFirst = (boolean) GromaValues.decode(state.get("secondFlowsToFirst"), ctx);
        this.effectiveCrest = (double) GromaValues.decode(state.get("effectiveCrest"), ctx);
        this.estimatedFlowRate = (double) GromaValues.decode(state.get("estimatedFlowRate"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
        this.out5 = (Double) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (Double) GromaValues.decode(state.get("out6"), ctx);
        this.out7 = (Boolean) GromaValues.decode(state.get("out7"), ctx);
        this.out8 = (Boolean) GromaValues.decode(state.get("out8"), ctx);
        this.out9 = (Double) GromaValues.decode(state.get("out9"), ctx);
        this.out10 = (Double) GromaValues.decode(state.get("out10"), ctx);
        this.out11 = (Double) GromaValues.decode(state.get("out11"), ctx);
        this.out12 = (Double) GromaValues.decode(state.get("out12"), ctx);
        this.out13 = (Double) GromaValues.decode(state.get("out13"), ctx);
        this.out14 = (Double) GromaValues.decode(state.get("out14"), ctx);
        this.out15 = (Double) GromaValues.decode(state.get("out15"), ctx);
        this.out16 = (Double) GromaValues.decode(state.get("out16"), ctx);
        this.out17 = (Double) GromaValues.decode(state.get("out17"), ctx);
        this.out18 = (Double) GromaValues.decode(state.get("out18"), ctx);
        this.out19 = (Double) GromaValues.decode(state.get("out19"), ctx);
    }
}

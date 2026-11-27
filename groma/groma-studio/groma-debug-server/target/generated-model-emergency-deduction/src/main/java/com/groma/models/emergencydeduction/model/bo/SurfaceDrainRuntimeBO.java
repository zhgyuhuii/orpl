package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：地表排水）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "Une96ICnH2KER5gu7LT#bo",
        zhName = "地表排水运行帧",
        kind = "RUNTIME_BO"
)
public final class SurfaceDrainRuntimeBO {
    /**
     * 局部量（方圆原名：当前步进秒）
     */
    private double currentStepSeconds = 0.0D;

    /**
     * 局部量（方圆原名：下一步进秒）
     */
    private double nextStepSeconds = 0.0D;

    /**
     * 局部量（方圆原名：本步秒数）
     */
    private double stepSeconds = 0.0D;

    /**
     * 节点输出：赋值
     */
    private Double out0;

    /**
     * 节点输出：赋值
     */
    private Double out1;

    /**
     * 节点输出：减
     */
    private Double out2;

    /**
     * 节点输出：除
     */
    private Double out3;

    /**
     * 节点输出：乘
     */
    private Double out4;

    /**
     * 节点输出：取小
     */
    private Double out5;

    /**
     * 节点输出：取相反数
     */
    private Double out6;

    /**
     * 局部量：当前步进秒
     */
    public double getCurrentStepSeconds() {
        return this.currentStepSeconds;
    }

    public void setCurrentStepSeconds(double value) {
        this.currentStepSeconds = value;
    }

    /**
     * 局部量：下一步进秒
     */
    public double getNextStepSeconds() {
        return this.nextStepSeconds;
    }

    public void setNextStepSeconds(double value) {
        this.nextStepSeconds = value;
    }

    /**
     * 局部量：本步秒数
     */
    public double getStepSeconds() {
        return this.stepSeconds;
    }

    public void setStepSeconds(double value) {
        this.stepSeconds = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut0() {
        return this.out0;
    }

    public void setOut0(Double value) {
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
     * 节点输出：减
     */
    public Double getOut2() {
        return this.out2;
    }

    public void setOut2(Double value) {
        this.out2 = value;
    }

    /**
     * 节点输出：除
     */
    public Double getOut3() {
        return this.out3;
    }

    public void setOut3(Double value) {
        this.out3 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut4() {
        return this.out4;
    }

    public void setOut4(Double value) {
        this.out4 = value;
    }

    /**
     * 节点输出：取小
     */
    public Double getOut5() {
        return this.out5;
    }

    public void setOut5(Double value) {
        this.out5 = value;
    }

    /**
     * 节点输出：取相反数
     */
    public Double getOut6() {
        return this.out6;
    }

    public void setOut6(Double value) {
        this.out6 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("currentStepSeconds", GromaValues.encode(this.currentStepSeconds));
        state.put("nextStepSeconds", GromaValues.encode(this.nextStepSeconds));
        state.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.currentStepSeconds = (double) GromaValues.decode(state.get("currentStepSeconds"), ctx);
        this.nextStepSeconds = (double) GromaValues.decode(state.get("nextStepSeconds"), ctx);
        this.stepSeconds = (double) GromaValues.decode(state.get("stepSeconds"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
        this.out5 = (Double) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (Double) GromaValues.decode(state.get("out6"), ctx);
    }
}

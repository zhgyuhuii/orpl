package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：计算降雨汇水）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "QZPPbXby5BPfgS5sziq#bo",
        zhName = "计算降雨汇水运行帧",
        kind = "RUNTIME_BO"
)
public final class ComputeRainfallCatchmentRuntimeBO {
    /**
     * 局部量（方圆原名：当前步进秒）
     */
    private long currentStepSeconds = 0L;

    /**
     * 局部量（方圆原名：下一步进秒）
     */
    private long nextStepSeconds = 0L;

    /**
     * 局部量（方圆原名：本步秒数）
     */
    private long stepSeconds = 0L;

    /**
     * 节点输出：大于
     */
    private Boolean out0;

    /**
     * 节点输出：除
     */
    private Double out2;

    /**
     * 节点输出：乘
     */
    private Double out4;

    /**
     * 节点输出：乘
     */
    private Double out5;

    /**
     * 节点输出：赋值
     */
    private Double out6;

    /**
     * 节点输出：赋值
     */
    private Double out7;

    /**
     * 节点输出：减
     */
    private Long out8;

    /**
     * 节点输出：乘
     */
    private Double out9;

    /**
     * 局部量：当前步进秒
     */
    public long getCurrentStepSeconds() {
        return this.currentStepSeconds;
    }

    public void setCurrentStepSeconds(long value) {
        this.currentStepSeconds = value;
    }

    /**
     * 局部量：下一步进秒
     */
    public long getNextStepSeconds() {
        return this.nextStepSeconds;
    }

    public void setNextStepSeconds(long value) {
        this.nextStepSeconds = value;
    }

    /**
     * 局部量：本步秒数
     */
    public long getStepSeconds() {
        return this.stepSeconds;
    }

    public void setStepSeconds(long value) {
        this.stepSeconds = value;
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
     * 节点输出：除
     */
    public Double getOut2() {
        return this.out2;
    }

    public void setOut2(Double value) {
        this.out2 = value;
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
     * 节点输出：乘
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
    public Double getOut7() {
        return this.out7;
    }

    public void setOut7(Double value) {
        this.out7 = value;
    }

    /**
     * 节点输出：减
     */
    public Long getOut8() {
        return this.out8;
    }

    public void setOut8(Long value) {
        this.out8 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut9() {
        return this.out9;
    }

    public void setOut9(Double value) {
        this.out9 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("currentStepSeconds", GromaValues.encode(this.currentStepSeconds));
        state.put("nextStepSeconds", GromaValues.encode(this.nextStepSeconds));
        state.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        state.put("out7", GromaValues.encode(this.out7));
        state.put("out8", GromaValues.encode(this.out8));
        state.put("out9", GromaValues.encode(this.out9));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.currentStepSeconds = (long) GromaValues.decode(state.get("currentStepSeconds"), ctx);
        this.nextStepSeconds = (long) GromaValues.decode(state.get("nextStepSeconds"), ctx);
        this.stepSeconds = (long) GromaValues.decode(state.get("stepSeconds"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
        this.out5 = (Double) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (Double) GromaValues.decode(state.get("out6"), ctx);
        this.out7 = (Double) GromaValues.decode(state.get("out7"), ctx);
        this.out8 = (Long) GromaValues.decode(state.get("out8"), ctx);
        this.out9 = (Double) GromaValues.decode(state.get("out9"), ctx);
    }
}

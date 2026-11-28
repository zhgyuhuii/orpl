package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：水流交换间隔）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "a9Ho9Zh9bCnkV8wVlfJ#bo",
        zhName = "水流交换间隔运行帧",
        kind = "RUNTIME_BO"
)
public final class FlowExchangeIntervalRuntimeBO {
    /**
     * 局部量（方圆原名：本次步进秒）
     */
    private double thisStepSeconds = 0.0D;

    /**
     * 局部量（方圆原名：下一步进秒）
     */
    private double nextStepSeconds = 0.0D;

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
     * 局部量：本次步进秒
     */
    public double getThisStepSeconds() {
        return this.thisStepSeconds;
    }

    public void setThisStepSeconds(double value) {
        this.thisStepSeconds = value;
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

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("thisStepSeconds", GromaValues.encode(this.thisStepSeconds));
        state.put("nextStepSeconds", GromaValues.encode(this.nextStepSeconds));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.thisStepSeconds = (double) GromaValues.decode(state.get("thisStepSeconds"), ctx);
        this.nextStepSeconds = (double) GromaValues.decode(state.get("nextStepSeconds"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
    }
}

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
 * 过程运行帧（方圆过程原名：水流交换间隔）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "6qnqt4O6vaFhEkh96tN#bo",
        zhName = "水流交换间隔运行帧",
        kind = "RUNTIME_BO"
)
public final class FlowExchangeIntervalRuntimeBO2 {
    /**
     * 局部量（方圆原名：本步秒）
     */
    private double stepSecond = 0.0D;

    /**
     * 局部量（方圆原名：下步秒）
     */
    private double nextStepSecond = 0.0D;

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
     * 局部量：本步秒
     */
    public double getStepSecond() {
        return this.stepSecond;
    }

    public void setStepSecond(double value) {
        this.stepSecond = value;
    }

    /**
     * 局部量：下步秒
     */
    public double getNextStepSecond() {
        return this.nextStepSecond;
    }

    public void setNextStepSecond(double value) {
        this.nextStepSecond = value;
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
        state.put("stepSecond", GromaValues.encode(this.stepSecond));
        state.put("nextStepSecond", GromaValues.encode(this.nextStepSecond));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.stepSecond = (double) GromaValues.decode(state.get("stepSecond"), ctx);
        this.nextStepSecond = (double) GromaValues.decode(state.get("nextStepSecond"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
    }
}

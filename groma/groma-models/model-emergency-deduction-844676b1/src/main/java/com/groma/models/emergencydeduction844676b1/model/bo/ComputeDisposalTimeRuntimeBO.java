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
 * 过程运行帧（方圆过程原名：计算处置时间）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "IJT778zU1zIw6nc7uYe#bo",
        zhName = "计算处置时间运行帧",
        kind = "RUNTIME_BO"
)
public final class ComputeDisposalTimeRuntimeBO {
    /**
     * 局部量（方圆原名：汇水速度）
     */
    private double catchmentSpeed = 0.0D;

    /**
     * 局部量（方圆原名：净积水速度）
     */
    private double netWaterloggingSpeed = 0.0D;

    /**
     * 节点输出：除
     */
    private Double out0;

    /**
     * 节点输出：乘
     */
    private Double out1;

    /**
     * 节点输出：乘
     */
    private Double out2;

    /**
     * 节点输出：除
     */
    private Double out3;

    /**
     * 节点输出：减
     */
    private Double out4;

    /**
     * 局部量：汇水速度
     */
    public double getCatchmentSpeed() {
        return this.catchmentSpeed;
    }

    public void setCatchmentSpeed(double value) {
        this.catchmentSpeed = value;
    }

    /**
     * 局部量：净积水速度
     */
    public double getNetWaterloggingSpeed() {
        return this.netWaterloggingSpeed;
    }

    public void setNetWaterloggingSpeed(double value) {
        this.netWaterloggingSpeed = value;
    }

    /**
     * 节点输出：除
     */
    public Double getOut0() {
        return this.out0;
    }

    public void setOut0(Double value) {
        this.out0 = value;
    }

    /**
     * 节点输出：乘
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    /**
     * 节点输出：乘
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
     * 节点输出：减
     */
    public Double getOut4() {
        return this.out4;
    }

    public void setOut4(Double value) {
        this.out4 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("catchmentSpeed", GromaValues.encode(this.catchmentSpeed));
        state.put("netWaterloggingSpeed", GromaValues.encode(this.netWaterloggingSpeed));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.catchmentSpeed = (double) GromaValues.decode(state.get("catchmentSpeed"), ctx);
        this.netWaterloggingSpeed = (double) GromaValues.decode(state.get("netWaterloggingSpeed"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
    }
}

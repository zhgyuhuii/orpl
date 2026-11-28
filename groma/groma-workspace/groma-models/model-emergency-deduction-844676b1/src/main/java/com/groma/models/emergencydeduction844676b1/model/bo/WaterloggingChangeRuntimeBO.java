package com.groma.models.emergencydeduction844676b1.model.bo;

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
 * 过程运行帧（方圆过程原名：积水变化）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "E5B8xzaV9xRnt2LDEgH#bo",
        zhName = "积水变化运行帧",
        kind = "RUNTIME_BO"
)
public final class WaterloggingChangeRuntimeBO {
    /**
     * 局部量（方圆原名：可用水量）
     */
    private double availableWaterVolume = 0.0D;

    /**
     * 节点输出：加
     */
    private Double out0;

    /**
     * 节点输出：取大
     */
    private Double out1;

    /**
     * 节点输出：大于
     */
    private Boolean out2;

    /**
     * 节点输出：赋值
     */
    private Double out5;

    /**
     * 节点输出：除
     */
    private Double out6;

    /**
     * 局部量：可用水量
     */
    public double getAvailableWaterVolume() {
        return this.availableWaterVolume;
    }

    public void setAvailableWaterVolume(double value) {
        this.availableWaterVolume = value;
    }

    /**
     * 节点输出：加
     */
    public Double getOut0() {
        return this.out0;
    }

    public void setOut0(Double value) {
        this.out0 = value;
    }

    /**
     * 节点输出：取大
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    /**
     * 节点输出：大于
     */
    public Boolean getOut2() {
        return this.out2;
    }

    public void setOut2(Boolean value) {
        this.out2 = value;
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
     * 节点输出：除
     */
    public Double getOut6() {
        return this.out6;
    }

    public void setOut6(Double value) {
        this.out6 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("availableWaterVolume", GromaValues.encode(this.availableWaterVolume));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.availableWaterVolume = (double) GromaValues.decode(state.get("availableWaterVolume"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Boolean) GromaValues.decode(state.get("out2"), ctx);
        this.out5 = (Double) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (Double) GromaValues.decode(state.get("out6"), ctx);
    }
}

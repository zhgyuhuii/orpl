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
 * 过程运行帧（方圆过程原名：降雨）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "TvGbpspuGv5vJ1zt0qX#bo",
        zhName = "降雨运行帧",
        kind = "RUNTIME_BO"
)
public final class RainfallRuntimeBO {
    /**
     * 局部量（方圆原名：可用雨量序列）
     */
    private Map<Long, Double> availableRainfallSeries = new java.util.LinkedHashMap<>(16);

    /**
     * 节点输出：包含键
     */
    private Boolean out1;

    /**
     * 节点输出：获取键对应的值
     */
    private Double out2;

    /**
     * 节点输出：除
     */
    private Double out4;

    /**
     * 局部量：可用雨量序列
     */
    public Map<Long, Double> getAvailableRainfallSeries() {
        return this.availableRainfallSeries;
    }

    public void setAvailableRainfallSeries(Map<Long, Double> value) {
        this.availableRainfallSeries = value;
    }

    /**
     * 节点输出：包含键
     */
    public Boolean getOut1() {
        return this.out1;
    }

    public void setOut1(Boolean value) {
        this.out1 = value;
    }

    /**
     * 节点输出：获取键对应的值
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
    public Double getOut4() {
        return this.out4;
    }

    public void setOut4(Double value) {
        this.out4 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("availableRainfallSeries", GromaValues.encode(this.availableRainfallSeries));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out4", GromaValues.encode(this.out4));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.availableRainfallSeries = (Map<Long, Double>) GromaValues.decode(state.get("availableRainfallSeries"), ctx);
        this.out1 = (Boolean) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
    }
}

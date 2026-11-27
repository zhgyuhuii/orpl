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
 * 过程运行帧（方圆过程原名：排水）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "QzcUew3b1XLCXLK6rIH#bo",
        zhName = "排水运行帧",
        kind = "RUNTIME_BO"
)
public final class DrainRuntimeBO {
    /**
     * 局部量（方圆原名：当前排水量）
     */
    private double currentDrainVolume = 0.0D;

    /**
     * 节点输出：除
     */
    private Double out0;

    /**
     * 节点输出：取小
     */
    private Double out1;

    /**
     * 局部量：当前排水量
     */
    public double getCurrentDrainVolume() {
        return this.currentDrainVolume;
    }

    public void setCurrentDrainVolume(double value) {
        this.currentDrainVolume = value;
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
     * 节点输出：取小
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("currentDrainVolume", GromaValues.encode(this.currentDrainVolume));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.currentDrainVolume = (double) GromaValues.decode(state.get("currentDrainVolume"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
    }
}

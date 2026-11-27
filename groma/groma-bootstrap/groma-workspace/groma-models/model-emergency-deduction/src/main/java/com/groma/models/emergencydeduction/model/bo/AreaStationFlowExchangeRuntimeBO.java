package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.DetermineBothWaterLevelsProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationFlowExchangeIntervalProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationInletFlowProcessTask;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：区域与站内水流交换）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "mXoawsnL7qgqcwNmvSw#bo",
        zhName = "区域与站内水流交换运行帧",
        kind = "RUNTIME_BO"
)
public final class AreaStationFlowExchangeRuntimeBO {
    /**
     * 局部量（方圆原名：本步秒数）
     */
    private long stepSeconds = 0L;

    /**
     * 局部量（方圆原名：区域水面高程）
     */
    private double areaWaterLevel = 0.0D;

    /**
     * 局部量（方圆原名：站内水面高程）
     */
    private double stationWaterLevel = 0.0D;

    /**
     * 子过程句柄：水流交换间隔
     */
    private StationFlowExchangeIntervalProcessTask child0;

    /**
     * 子过程句柄：确定两端水位
     */
    private DetermineBothWaterLevelsProcessTask child1;

    /**
     * 子过程句柄：入口过水
     */
    private StationInletFlowProcessTask child3;

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
     * 局部量：区域水面高程
     */
    public double getAreaWaterLevel() {
        return this.areaWaterLevel;
    }

    public void setAreaWaterLevel(double value) {
        this.areaWaterLevel = value;
    }

    /**
     * 局部量：站内水面高程
     */
    public double getStationWaterLevel() {
        return this.stationWaterLevel;
    }

    public void setStationWaterLevel(double value) {
        this.stationWaterLevel = value;
    }

    /**
     * 子过程句柄：水流交换间隔
     */
    public StationFlowExchangeIntervalProcessTask getChild0() {
        return this.child0;
    }

    public void setChild0(StationFlowExchangeIntervalProcessTask value) {
        this.child0 = value;
    }

    /**
     * 子过程句柄：确定两端水位
     */
    public DetermineBothWaterLevelsProcessTask getChild1() {
        return this.child1;
    }

    public void setChild1(DetermineBothWaterLevelsProcessTask value) {
        this.child1 = value;
    }

    /**
     * 子过程句柄：入口过水
     */
    public StationInletFlowProcessTask getChild3() {
        return this.child3;
    }

    public void setChild3(StationInletFlowProcessTask value) {
        this.child3 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        state.put("areaWaterLevel", GromaValues.encode(this.areaWaterLevel));
        state.put("stationWaterLevel", GromaValues.encode(this.stationWaterLevel));
        state.put("child0", this.child0 == null ? "" : this.child0.getProcessInstanceId());
        state.put("child1", this.child1 == null ? "" : this.child1.getProcessInstanceId());
        state.put("child3", this.child3 == null ? "" : this.child3.getProcessInstanceId());
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.stepSeconds = (long) GromaValues.decode(state.get("stepSeconds"), ctx);
        this.areaWaterLevel = (double) GromaValues.decode(state.get("areaWaterLevel"), ctx);
        this.stationWaterLevel = (double) GromaValues.decode(state.get("stationWaterLevel"), ctx);
    }
}

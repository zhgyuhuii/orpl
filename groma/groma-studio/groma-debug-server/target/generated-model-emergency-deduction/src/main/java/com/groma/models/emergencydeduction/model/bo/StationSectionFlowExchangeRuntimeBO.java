package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.DetermineBothWaterLevelsProcessTask;
import com.groma.models.emergencydeduction.engine.process.DetermineJunctionFlowProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionFlowExchangeIntervalProcessTask;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：车站与区间水流交换）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "HOI0UpBGLHJoNJjKYum#bo",
        zhName = "车站与区间水流交换运行帧",
        kind = "RUNTIME_BO"
)
public final class StationSectionFlowExchangeRuntimeBO {
    /**
     * 局部量（方圆原名：本步秒数）
     */
    private long stepSeconds = 0L;

    /**
     * 局部量（方圆原名：车站水面高程）
     */
    private double stationWaterLevel = 0.0D;

    /**
     * 局部量（方圆原名：区间水面高程）
     */
    private double sectionWaterLevel = 0.0D;

    /**
     * 子过程句柄：水流交换间隔
     */
    private MetroSectionFlowExchangeIntervalProcessTask child0;

    /**
     * 子过程句柄：确定两端水位
     */
    private DetermineBothWaterLevelsProcessTask child1;

    /**
     * 子过程句柄：确定连接处过水情况
     */
    private DetermineJunctionFlowProcessTask child2;

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
     * 局部量：车站水面高程
     */
    public double getStationWaterLevel() {
        return this.stationWaterLevel;
    }

    public void setStationWaterLevel(double value) {
        this.stationWaterLevel = value;
    }

    /**
     * 局部量：区间水面高程
     */
    public double getSectionWaterLevel() {
        return this.sectionWaterLevel;
    }

    public void setSectionWaterLevel(double value) {
        this.sectionWaterLevel = value;
    }

    /**
     * 子过程句柄：水流交换间隔
     */
    public MetroSectionFlowExchangeIntervalProcessTask getChild0() {
        return this.child0;
    }

    public void setChild0(MetroSectionFlowExchangeIntervalProcessTask value) {
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
     * 子过程句柄：确定连接处过水情况
     */
    public DetermineJunctionFlowProcessTask getChild2() {
        return this.child2;
    }

    public void setChild2(DetermineJunctionFlowProcessTask value) {
        this.child2 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        state.put("stationWaterLevel", GromaValues.encode(this.stationWaterLevel));
        state.put("sectionWaterLevel", GromaValues.encode(this.sectionWaterLevel));
        state.put("child0", this.child0 == null ? "" : this.child0.getProcessInstanceId());
        state.put("child1", this.child1 == null ? "" : this.child1.getProcessInstanceId());
        state.put("child2", this.child2 == null ? "" : this.child2.getProcessInstanceId());
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.stepSeconds = (long) GromaValues.decode(state.get("stepSeconds"), ctx);
        this.stationWaterLevel = (double) GromaValues.decode(state.get("stationWaterLevel"), ctx);
        this.sectionWaterLevel = (double) GromaValues.decode(state.get("sectionWaterLevel"), ctx);
    }
}

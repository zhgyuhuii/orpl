package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.TrainAdvanceAlongSectionProcessTask;
import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：区间行驶）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "u9xyHBv1BSldBPVP0Q7#bo",
        zhName = "区间行驶运行帧",
        kind = "RUNTIME_BO"
)
public final class SectionRunRuntimeBO {
    /**
     * 局部量（方圆原名：开始时刻）
     */
    private double startTime = 0.0D;

    /**
     * 局部量（方圆原名：当前时刻）
     */
    private double currentTime = 0.0D;

    /**
     * 局部量（方圆原名：行驶间隔）
     */
    private double travelInterval = 0.0D;

    /**
     * 节点输出：赋值
     */
    private Double out0;

    /**
     * 节点输出：赋值
     */
    private Double out2;

    /**
     * 节点输出：赋值
     */
    private Double out3;

    /**
     * 节点输出：减
     */
    private Double out4;

    /**
     * 子过程句柄：沿区间前进
     */
    private TrainAdvanceAlongSectionProcessTask child5;

    /**
     * 节点输出：大于等于
     */
    private Boolean out6;

    /**
     * 节点输出：赋值
     */
    private GeoPointEntity out7;

    /**
     * 节点输出：赋值
     */
    private Boolean out8;

    /**
     * 局部量：开始时刻
     */
    public double getStartTime() {
        return this.startTime;
    }

    public void setStartTime(double value) {
        this.startTime = value;
    }

    /**
     * 局部量：当前时刻
     */
    public double getCurrentTime() {
        return this.currentTime;
    }

    public void setCurrentTime(double value) {
        this.currentTime = value;
    }

    /**
     * 局部量：行驶间隔
     */
    public double getTravelInterval() {
        return this.travelInterval;
    }

    public void setTravelInterval(double value) {
        this.travelInterval = value;
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
    public Double getOut2() {
        return this.out2;
    }

    public void setOut2(Double value) {
        this.out2 = value;
    }

    /**
     * 节点输出：赋值
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

    /**
     * 子过程句柄：沿区间前进
     */
    public TrainAdvanceAlongSectionProcessTask getChild5() {
        return this.child5;
    }

    public void setChild5(TrainAdvanceAlongSectionProcessTask value) {
        this.child5 = value;
    }

    /**
     * 节点输出：大于等于
     */
    public Boolean getOut6() {
        return this.out6;
    }

    public void setOut6(Boolean value) {
        this.out6 = value;
    }

    /**
     * 节点输出：赋值
     */
    public GeoPointEntity getOut7() {
        return this.out7;
    }

    public void setOut7(GeoPointEntity value) {
        this.out7 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Boolean getOut8() {
        return this.out8;
    }

    public void setOut8(Boolean value) {
        this.out8 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("startTime", GromaValues.encode(this.startTime));
        state.put("currentTime", GromaValues.encode(this.currentTime));
        state.put("travelInterval", GromaValues.encode(this.travelInterval));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("child5", this.child5 == null ? "" : this.child5.getProcessInstanceId());
        state.put("out6", GromaValues.encode(this.out6));
        state.put("out7", GromaValues.encode(this.out7));
        state.put("out8", GromaValues.encode(this.out8));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.startTime = (double) GromaValues.decode(state.get("startTime"), ctx);
        this.currentTime = (double) GromaValues.decode(state.get("currentTime"), ctx);
        this.travelInterval = (double) GromaValues.decode(state.get("travelInterval"), ctx);
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out2 = (Double) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Double) GromaValues.decode(state.get("out4"), ctx);
        this.out6 = (Boolean) GromaValues.decode(state.get("out6"), ctx);
        this.out7 = (GeoPointEntity) GromaValues.decode(state.get("out7"), ctx);
        this.out8 = (Boolean) GromaValues.decode(state.get("out8"), ctx);
    }
}

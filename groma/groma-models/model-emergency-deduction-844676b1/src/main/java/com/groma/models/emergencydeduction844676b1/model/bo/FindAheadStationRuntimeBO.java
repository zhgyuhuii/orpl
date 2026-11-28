package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithStationEntity;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：查找前方车站）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "oHh6xrqSMTxFFZGd07W#bo",
        zhName = "查找前方车站运行帧",
        kind = "RUNTIME_BO"
)
public final class FindAheadStationRuntimeBO {
    /**
     * 局部量（方圆原名：方向系数）
     */
    private long directionFactor = 0L;

    /**
     * 局部量（方圆原名：车站标识）
     */
    private long stationIdentifier = 1L;

    /**
     * 局部量（方圆原名：最近距离）
     */
    private double nearestDistance = 0.0D;

    /**
     * 局部量（方圆原名：临时线路与车站）
     */
    private LineWithStationEntity tempLineWithStation = null;

    /**
     * 局部量（方圆原名：候选距离）
     */
    private double candidateDistance = 0.0D;

    /**
     * 节点输出：等于
     */
    private Boolean out0;

    /**
     * 节点输出：赋值
     */
    private Long out1;

    /**
     * 节点输出：赋值
     */
    private Long out2;

    /**
     * 节点输出：大于等于
     */
    private Boolean out3;

    /**
     * 节点输出：大于
     */
    private Boolean out4;

    /**
     * 节点输出：按标识取元素
     */
    private LineWithStationEntity out5;

    /**
     * 节点输出：赋值
     */
    private Boolean out6;

    /**
     * 节点输出：赋值
     */
    private Boolean out7;

    /**
     * 节点输出：减
     */
    private Double out8;

    /**
     * 节点输出：乘
     */
    private Double out9;

    /**
     * 节点输出：大于
     */
    private Boolean out10;

    /**
     * 节点输出：等于
     */
    private Boolean out12;

    /**
     * 节点输出：赋值
     */
    private Double out13;

    /**
     * 节点输出：小于
     */
    private Boolean out14;

    /**
     * 节点输出：赋值
     */
    private LineWithStationEntity out15;

    /**
     * 局部量：方向系数
     */
    public long getDirectionFactor() {
        return this.directionFactor;
    }

    public void setDirectionFactor(long value) {
        this.directionFactor = value;
    }

    /**
     * 局部量：车站标识
     */
    public long getStationIdentifier() {
        return this.stationIdentifier;
    }

    public void setStationIdentifier(long value) {
        this.stationIdentifier = value;
    }

    /**
     * 局部量：最近距离
     */
    public double getNearestDistance() {
        return this.nearestDistance;
    }

    public void setNearestDistance(double value) {
        this.nearestDistance = value;
    }

    /**
     * 局部量：临时线路与车站
     */
    public LineWithStationEntity getTempLineWithStation() {
        return this.tempLineWithStation;
    }

    public void setTempLineWithStation(LineWithStationEntity value) {
        this.tempLineWithStation = value;
    }

    /**
     * 局部量：候选距离
     */
    public double getCandidateDistance() {
        return this.candidateDistance;
    }

    public void setCandidateDistance(double value) {
        this.candidateDistance = value;
    }

    /**
     * 节点输出：等于
     */
    public Boolean getOut0() {
        return this.out0;
    }

    public void setOut0(Boolean value) {
        this.out0 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Long getOut1() {
        return this.out1;
    }

    public void setOut1(Long value) {
        this.out1 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Long getOut2() {
        return this.out2;
    }

    public void setOut2(Long value) {
        this.out2 = value;
    }

    /**
     * 节点输出：大于等于
     */
    public Boolean getOut3() {
        return this.out3;
    }

    public void setOut3(Boolean value) {
        this.out3 = value;
    }

    /**
     * 节点输出：大于
     */
    public Boolean getOut4() {
        return this.out4;
    }

    public void setOut4(Boolean value) {
        this.out4 = value;
    }

    /**
     * 节点输出：按标识取元素
     */
    public LineWithStationEntity getOut5() {
        return this.out5;
    }

    public void setOut5(LineWithStationEntity value) {
        this.out5 = value;
    }

    /**
     * 节点输出：赋值
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
    public Boolean getOut7() {
        return this.out7;
    }

    public void setOut7(Boolean value) {
        this.out7 = value;
    }

    /**
     * 节点输出：减
     */
    public Double getOut8() {
        return this.out8;
    }

    public void setOut8(Double value) {
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

    /**
     * 节点输出：大于
     */
    public Boolean getOut10() {
        return this.out10;
    }

    public void setOut10(Boolean value) {
        this.out10 = value;
    }

    /**
     * 节点输出：等于
     */
    public Boolean getOut12() {
        return this.out12;
    }

    public void setOut12(Boolean value) {
        this.out12 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut13() {
        return this.out13;
    }

    public void setOut13(Double value) {
        this.out13 = value;
    }

    /**
     * 节点输出：小于
     */
    public Boolean getOut14() {
        return this.out14;
    }

    public void setOut14(Boolean value) {
        this.out14 = value;
    }

    /**
     * 节点输出：赋值
     */
    public LineWithStationEntity getOut15() {
        return this.out15;
    }

    public void setOut15(LineWithStationEntity value) {
        this.out15 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("directionFactor", GromaValues.encode(this.directionFactor));
        state.put("stationIdentifier", GromaValues.encode(this.stationIdentifier));
        state.put("nearestDistance", GromaValues.encode(this.nearestDistance));
        state.put("tempLineWithStation", GromaValues.encode(this.tempLineWithStation));
        state.put("candidateDistance", GromaValues.encode(this.candidateDistance));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        state.put("out7", GromaValues.encode(this.out7));
        state.put("out8", GromaValues.encode(this.out8));
        state.put("out9", GromaValues.encode(this.out9));
        state.put("out10", GromaValues.encode(this.out10));
        state.put("out12", GromaValues.encode(this.out12));
        state.put("out13", GromaValues.encode(this.out13));
        state.put("out14", GromaValues.encode(this.out14));
        state.put("out15", GromaValues.encode(this.out15));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.directionFactor = (long) GromaValues.decode(state.get("directionFactor"), ctx);
        this.stationIdentifier = (long) GromaValues.decode(state.get("stationIdentifier"), ctx);
        this.nearestDistance = (double) GromaValues.decode(state.get("nearestDistance"), ctx);
        this.tempLineWithStation = (LineWithStationEntity) GromaValues.decode(state.get("tempLineWithStation"), ctx);
        this.candidateDistance = (double) GromaValues.decode(state.get("candidateDistance"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Long) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Long) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Boolean) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Boolean) GromaValues.decode(state.get("out4"), ctx);
        this.out5 = (LineWithStationEntity) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (Boolean) GromaValues.decode(state.get("out6"), ctx);
        this.out7 = (Boolean) GromaValues.decode(state.get("out7"), ctx);
        this.out8 = (Double) GromaValues.decode(state.get("out8"), ctx);
        this.out9 = (Double) GromaValues.decode(state.get("out9"), ctx);
        this.out10 = (Boolean) GromaValues.decode(state.get("out10"), ctx);
        this.out12 = (Boolean) GromaValues.decode(state.get("out12"), ctx);
        this.out13 = (Double) GromaValues.decode(state.get("out13"), ctx);
        this.out14 = (Boolean) GromaValues.decode(state.get("out14"), ctx);
        this.out15 = (LineWithStationEntity) GromaValues.decode(state.get("out15"), ctx);
    }
}

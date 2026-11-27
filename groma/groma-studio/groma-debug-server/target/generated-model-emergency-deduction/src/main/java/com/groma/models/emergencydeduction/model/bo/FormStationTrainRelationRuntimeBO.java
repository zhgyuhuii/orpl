package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：形成车站与列车的关联）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "eJ4DHDmPNpDiyA8MPzb#bo",
        zhName = "形成车站与列车的关联运行帧",
        kind = "RUNTIME_BO"
)
public final class FormStationTrainRelationRuntimeBO {
    /**
     * 局部量（方圆原名：标识）
     */
    private long identifier = 1L;

    /**
     * 局部量（方圆原名：线路与车站）
     */
    private LineWithStationEntity lineWithStation = null;

    /**
     * 局部量（方圆原名：临时关联）
     */
    private StationWithParkedTrainEntity tempRelation = null;

    /**
     * 节点输出：大于等于
     */
    private Boolean out0;

    /**
     * 节点输出：按标识取元素
     */
    private LineWithStationEntity out1;

    /**
     * 节点输出：等于
     */
    private Boolean out2;

    /**
     * 节点输出：指向
     */
    private TrainEntity out5;

    /**
     * 节点输出：指向
     */
    private StationEntity out6;

    /**
     * 节点输出：指向
     */
    private StationWithParkedTrainEntity out7;

    /**
     * 局部量：标识
     */
    public long getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(long value) {
        this.identifier = value;
    }

    /**
     * 局部量：线路与车站
     */
    public LineWithStationEntity getLineWithStation() {
        return this.lineWithStation;
    }

    public void setLineWithStation(LineWithStationEntity value) {
        this.lineWithStation = value;
    }

    /**
     * 局部量：临时关联
     */
    public StationWithParkedTrainEntity getTempRelation() {
        return this.tempRelation;
    }

    public void setTempRelation(StationWithParkedTrainEntity value) {
        this.tempRelation = value;
    }

    /**
     * 节点输出：大于等于
     */
    public Boolean getOut0() {
        return this.out0;
    }

    public void setOut0(Boolean value) {
        this.out0 = value;
    }

    /**
     * 节点输出：按标识取元素
     */
    public LineWithStationEntity getOut1() {
        return this.out1;
    }

    public void setOut1(LineWithStationEntity value) {
        this.out1 = value;
    }

    /**
     * 节点输出：等于
     */
    public Boolean getOut2() {
        return this.out2;
    }

    public void setOut2(Boolean value) {
        this.out2 = value;
    }

    /**
     * 节点输出：指向
     */
    public TrainEntity getOut5() {
        return this.out5;
    }

    public void setOut5(TrainEntity value) {
        this.out5 = value;
    }

    /**
     * 节点输出：指向
     */
    public StationEntity getOut6() {
        return this.out6;
    }

    public void setOut6(StationEntity value) {
        this.out6 = value;
    }

    /**
     * 节点输出：指向
     */
    public StationWithParkedTrainEntity getOut7() {
        return this.out7;
    }

    public void setOut7(StationWithParkedTrainEntity value) {
        this.out7 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("identifier", GromaValues.encode(this.identifier));
        state.put("lineWithStation", GromaValues.encode(this.lineWithStation));
        state.put("tempRelation", GromaValues.encode(this.tempRelation));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        state.put("out7", GromaValues.encode(this.out7));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.identifier = (long) GromaValues.decode(state.get("identifier"), ctx);
        this.lineWithStation = (LineWithStationEntity) GromaValues.decode(state.get("lineWithStation"), ctx);
        this.tempRelation = (StationWithParkedTrainEntity) GromaValues.decode(state.get("tempRelation"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (LineWithStationEntity) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Boolean) GromaValues.decode(state.get("out2"), ctx);
        this.out5 = (TrainEntity) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (StationEntity) GromaValues.decode(state.get("out6"), ctx);
        this.out7 = (StationWithParkedTrainEntity) GromaValues.decode(state.get("out7"), ctx);
    }
}

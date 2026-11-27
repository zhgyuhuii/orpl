package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.FindAheadStationProcessTask;
import com.groma.models.emergencydeduction.model.entity.LineWithSectionEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：形成区间与列车的关联）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "Ls7rMbkit4rP36FTZiY#bo",
        zhName = "形成区间与列车的关联运行帧",
        kind = "RUNTIME_BO"
)
public final class FormSectionTrainRelationRuntimeBO {
    /**
     * 局部量（方圆原名：区间标识）
     */
    private long sectionIdentifier = 1L;

    /**
     * 局部量（方圆原名：临时线路与区间）
     */
    private LineWithSectionEntity tempLineWithSection = null;

    /**
     * 局部量（方圆原名：本次已折返）
     */
    private boolean returnedThisTime = false;

    /**
     * 局部量（方圆原名：线路与车站）
     */
    private LineWithStationEntity lineWithStation = null;

    /**
     * 局部量（方圆原名：临时关联）
     */
    private SectionWithTrainEntity tempRelation = null;

    /**
     * 节点输出：大于等于
     */
    private Boolean out0;

    /**
     * 节点输出：按标识取元素
     */
    private LineWithSectionEntity out1;

    /**
     * 节点输出：等于
     */
    private Boolean out2;

    /**
     * 节点输出：等于
     */
    private Boolean out3;

    /**
     * 节点输出：等于
     */
    private Boolean out4;

    /**
     * 节点输出：大于等于
     */
    private Boolean out5;

    /**
     * 节点输出：大于
     */
    private Boolean out6;

    /**
     * 节点输出：赋值
     */
    private Boolean out7;

    /**
     * 节点输出：小于
     */
    private Boolean out8;

    /**
     * 节点输出：小于等于
     */
    private Boolean out9;

    /**
     * 节点输出：非
     */
    private Boolean out10;

    /**
     * 节点输出：与
     */
    private Boolean out11;

    /**
     * 节点输出：与
     */
    private Boolean out12;

    /**
     * 子过程句柄：查找前方车站
     */
    private FindAheadStationProcessTask child15;

    /**
     * 节点输出：指向
     */
    private StationEntity out17;

    /**
     * 节点输出：指向
     */
    private TrainEntity out18;

    /**
     * 节点输出：指向
     */
    private MetroSectionEntity out19;

    /**
     * 节点输出：指向
     */
    private SectionWithTrainEntity out20;

    /**
     * 局部量：区间标识
     */
    public long getSectionIdentifier() {
        return this.sectionIdentifier;
    }

    public void setSectionIdentifier(long value) {
        this.sectionIdentifier = value;
    }

    /**
     * 局部量：临时线路与区间
     */
    public LineWithSectionEntity getTempLineWithSection() {
        return this.tempLineWithSection;
    }

    public void setTempLineWithSection(LineWithSectionEntity value) {
        this.tempLineWithSection = value;
    }

    /**
     * 局部量：本次已折返
     */
    public boolean getReturnedThisTime() {
        return this.returnedThisTime;
    }

    public void setReturnedThisTime(boolean value) {
        this.returnedThisTime = value;
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
    public SectionWithTrainEntity getTempRelation() {
        return this.tempRelation;
    }

    public void setTempRelation(SectionWithTrainEntity value) {
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
    public LineWithSectionEntity getOut1() {
        return this.out1;
    }

    public void setOut1(LineWithSectionEntity value) {
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
     * 节点输出：等于
     */
    public Boolean getOut3() {
        return this.out3;
    }

    public void setOut3(Boolean value) {
        this.out3 = value;
    }

    /**
     * 节点输出：等于
     */
    public Boolean getOut4() {
        return this.out4;
    }

    public void setOut4(Boolean value) {
        this.out4 = value;
    }

    /**
     * 节点输出：大于等于
     */
    public Boolean getOut5() {
        return this.out5;
    }

    public void setOut5(Boolean value) {
        this.out5 = value;
    }

    /**
     * 节点输出：大于
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
     * 节点输出：小于
     */
    public Boolean getOut8() {
        return this.out8;
    }

    public void setOut8(Boolean value) {
        this.out8 = value;
    }

    /**
     * 节点输出：小于等于
     */
    public Boolean getOut9() {
        return this.out9;
    }

    public void setOut9(Boolean value) {
        this.out9 = value;
    }

    /**
     * 节点输出：非
     */
    public Boolean getOut10() {
        return this.out10;
    }

    public void setOut10(Boolean value) {
        this.out10 = value;
    }

    /**
     * 节点输出：与
     */
    public Boolean getOut11() {
        return this.out11;
    }

    public void setOut11(Boolean value) {
        this.out11 = value;
    }

    /**
     * 节点输出：与
     */
    public Boolean getOut12() {
        return this.out12;
    }

    public void setOut12(Boolean value) {
        this.out12 = value;
    }

    /**
     * 子过程句柄：查找前方车站
     */
    public FindAheadStationProcessTask getChild15() {
        return this.child15;
    }

    public void setChild15(FindAheadStationProcessTask value) {
        this.child15 = value;
    }

    /**
     * 节点输出：指向
     */
    public StationEntity getOut17() {
        return this.out17;
    }

    public void setOut17(StationEntity value) {
        this.out17 = value;
    }

    /**
     * 节点输出：指向
     */
    public TrainEntity getOut18() {
        return this.out18;
    }

    public void setOut18(TrainEntity value) {
        this.out18 = value;
    }

    /**
     * 节点输出：指向
     */
    public MetroSectionEntity getOut19() {
        return this.out19;
    }

    public void setOut19(MetroSectionEntity value) {
        this.out19 = value;
    }

    /**
     * 节点输出：指向
     */
    public SectionWithTrainEntity getOut20() {
        return this.out20;
    }

    public void setOut20(SectionWithTrainEntity value) {
        this.out20 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("sectionIdentifier", GromaValues.encode(this.sectionIdentifier));
        state.put("tempLineWithSection", GromaValues.encode(this.tempLineWithSection));
        state.put("returnedThisTime", GromaValues.encode(this.returnedThisTime));
        state.put("lineWithStation", GromaValues.encode(this.lineWithStation));
        state.put("tempRelation", GromaValues.encode(this.tempRelation));
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
        state.put("out11", GromaValues.encode(this.out11));
        state.put("out12", GromaValues.encode(this.out12));
        state.put("child15", this.child15 == null ? "" : this.child15.getProcessInstanceId());
        state.put("out17", GromaValues.encode(this.out17));
        state.put("out18", GromaValues.encode(this.out18));
        state.put("out19", GromaValues.encode(this.out19));
        state.put("out20", GromaValues.encode(this.out20));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.sectionIdentifier = (long) GromaValues.decode(state.get("sectionIdentifier"), ctx);
        this.tempLineWithSection = (LineWithSectionEntity) GromaValues.decode(state.get("tempLineWithSection"), ctx);
        this.returnedThisTime = (boolean) GromaValues.decode(state.get("returnedThisTime"), ctx);
        this.lineWithStation = (LineWithStationEntity) GromaValues.decode(state.get("lineWithStation"), ctx);
        this.tempRelation = (SectionWithTrainEntity) GromaValues.decode(state.get("tempRelation"), ctx);
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (LineWithSectionEntity) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Boolean) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Boolean) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Boolean) GromaValues.decode(state.get("out4"), ctx);
        this.out5 = (Boolean) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = (Boolean) GromaValues.decode(state.get("out6"), ctx);
        this.out7 = (Boolean) GromaValues.decode(state.get("out7"), ctx);
        this.out8 = (Boolean) GromaValues.decode(state.get("out8"), ctx);
        this.out9 = (Boolean) GromaValues.decode(state.get("out9"), ctx);
        this.out10 = (Boolean) GromaValues.decode(state.get("out10"), ctx);
        this.out11 = (Boolean) GromaValues.decode(state.get("out11"), ctx);
        this.out12 = (Boolean) GromaValues.decode(state.get("out12"), ctx);
        this.out17 = (StationEntity) GromaValues.decode(state.get("out17"), ctx);
        this.out18 = (TrainEntity) GromaValues.decode(state.get("out18"), ctx);
        this.out19 = (MetroSectionEntity) GromaValues.decode(state.get("out19"), ctx);
        this.out20 = (SectionWithTrainEntity) GromaValues.decode(state.get("out20"), ctx);
    }
}

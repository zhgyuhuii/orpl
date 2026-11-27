package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import java.lang.Boolean;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：停站）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "1rbne3IC1GpVoTNMwVu#bo",
        zhName = "停站运行帧",
        kind = "RUNTIME_BO"
)
public final class StoppedAtStationRuntimeBO {
    /**
     * 局部量（方圆原名：等待时长）
     */
    private long waitingDuration = 0L;

    /**
     * 局部量（方圆原名：允许离站）
     */
    private boolean departureAllowed = true;

    /**
     * 节点输出：赋值
     */
    private GeoPointEntity out0;

    /**
     * 节点输出：赋值
     */
    private GeoPointEntity out1;

    /**
     * 节点输出：赋值
     */
    private Long out2;

    /**
     * 节点输出：大于
     */
    private Boolean out3;

    /**
     * 节点输出：赋值
     */
    private Boolean out4;

    /**
     * 局部量：等待时长
     */
    public long getWaitingDuration() {
        return this.waitingDuration;
    }

    public void setWaitingDuration(long value) {
        this.waitingDuration = value;
    }

    /**
     * 局部量：允许离站
     */
    public boolean getDepartureAllowed() {
        return this.departureAllowed;
    }

    public void setDepartureAllowed(boolean value) {
        this.departureAllowed = value;
    }

    /**
     * 节点输出：赋值
     */
    public GeoPointEntity getOut0() {
        return this.out0;
    }

    public void setOut0(GeoPointEntity value) {
        this.out0 = value;
    }

    /**
     * 节点输出：赋值
     */
    public GeoPointEntity getOut1() {
        return this.out1;
    }

    public void setOut1(GeoPointEntity value) {
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
     * 节点输出：大于
     */
    public Boolean getOut3() {
        return this.out3;
    }

    public void setOut3(Boolean value) {
        this.out3 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Boolean getOut4() {
        return this.out4;
    }

    public void setOut4(Boolean value) {
        this.out4 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("waitingDuration", GromaValues.encode(this.waitingDuration));
        state.put("departureAllowed", GromaValues.encode(this.departureAllowed));
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.waitingDuration = (long) GromaValues.decode(state.get("waitingDuration"), ctx);
        this.departureAllowed = (boolean) GromaValues.decode(state.get("departureAllowed"), ctx);
        this.out0 = (GeoPointEntity) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (GeoPointEntity) GromaValues.decode(state.get("out1"), ctx);
        this.out2 = (Long) GromaValues.decode(state.get("out2"), ctx);
        this.out3 = (Boolean) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Boolean) GromaValues.decode(state.get("out4"), ctx);
    }
}

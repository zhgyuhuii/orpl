package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;

/**
 * 列车实体（对应真实模型 TYPE: 列车, eid=blmgbWSWp2tl8mLS8Hl）
 */
@GromaMeta(id = "blmgbWSWp2tl8mLS8Hl", zhName = "列车", kind = "TYPE", essence = "PHYSICAL")
public class TrainEntity {

    private final String instanceId;
    private final String name;
    @GromaMeta(id = "tr_cruise_spd", zhName = "行驶速度", unit = "m/s")
    private double cruiseSpeedMps = 18.0D;
    @GromaMeta(id = "tr_cur_spd", zhName = "当前速度", unit = "m/s")
    private double currentSpeedMps = 0.0D;
    @GromaMeta(id = "tr_pos", zhName = "当前位置")
    private String currentPosition;
    @GromaMeta(id = "tr_mileage", zhName = "区间已行驶里程", unit = "m")
    private double sectionProgressMeters = 0.0D;
    @GromaMeta(id = "tr_state", zhName = "列车状态", initState = "停站")
    private TrainStateEnum trainState = TrainStateEnum.STOPPED_AT_STATION;

    public TrainEntity(String instanceId, String name, String initialPosition) {
        this.instanceId = instanceId;
        this.name = name;
        this.currentPosition = initialPosition;
    }

    public String getInstanceId() { return instanceId; }
    public String getName() { return name; }
    public double getCruiseSpeedMps() { return cruiseSpeedMps; }
    public double getCurrentSpeedMps() { return currentSpeedMps; }
    public String getCurrentPosition() { return currentPosition; }
    public double getSectionProgressMeters() { return sectionProgressMeters; }
    public TrainStateEnum getTrainState() { return trainState; }

    public void transitionState(TrainStateEnum next, String positionDesc, double speed, AbstractWorldSession session) {
        this.currentPosition = positionDesc;
        this.currentSpeedMps = speed;
        if (this.trainState != next) {
            this.trainState = next;
            if (session != null) {
                session.enqueueObjectEvent("sa", this.instanceId, "列车状态->" + next.getZhLabel());
            }
        }
    }

    public void advanceInSection(double deltaMeters, AbstractWorldSession session) {
        this.sectionProgressMeters += Math.max(0.0D, deltaMeters);
        if (session != null) {
            session.enqueueObjectEvent("v", this.instanceId, "列车里程推进");
        }
    }

    public void resetSectionProgress() {
        this.sectionProgressMeters = 0.0D;
    }
}

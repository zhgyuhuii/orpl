package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;

/**
 * 区域实体（对应真实模型 TYPE: 区域, eid=Ap9lIerPholgB6pZsPK）
 */
@GromaMeta(id = "Ap9lIerPholgB6pZsPK", zhName = "区域", kind = "TYPE", essence = "PHYSICAL")
public class AreaEntity {

    private final String instanceId;
    private final String name;
    @GromaMeta(id = "area_catchment", zhName = "汇水面积", unit = "m2")
    private double catchmentArea = 50_000.0D;
    @GromaMeta(id = "area_runoff", zhName = "径流系数")
    private double runoffCoeff = 0.85D;
    @GromaMeta(id = "area_elevation", zhName = "地面高程", unit = "m")
    private double groundElevation = 98.5D;
    @GromaMeta(id = "area_drain_cap", zhName = "地表排水能力", unit = "m3/s")
    private double surfaceDrainCapacity = 1.2D;
    @GromaMeta(id = "area_water_vol", zhName = "积水量", unit = "m3")
    private double waterVolume = 0.0D;
    @GromaMeta(id = "area_water_depth", zhName = "积水深度", unit = "m")
    private double waterDepth = 0.0D;
    @GromaMeta(id = "area_state", zhName = "积水状态", initState = "无积水")
    private WaterloggingStateEnum waterloggingState = WaterloggingStateEnum.NO_WATERLOGGING;

    public AreaEntity(String instanceId, String name) {
        this.instanceId = instanceId;
        this.name = name;
    }

    public String getInstanceId() { return instanceId; }
    public String getName() { return name; }
    public double getCatchmentArea() { return catchmentArea; }
    public double getRunoffCoeff() { return runoffCoeff; }
    public double getGroundElevation() { return groundElevation; }
    public double getSurfaceDrainCapacity() { return surfaceDrainCapacity; }
    public double getWaterVolume() { return waterVolume; }
    public double getWaterDepth() { return waterDepth; }
    public WaterloggingStateEnum getWaterloggingState() { return waterloggingState; }

    public void updateWaterVolumeAndDepth(double newVolume, AbstractWorldSession session) {
        this.waterVolume = Math.max(0.0D, newVolume);
        this.waterDepth = this.waterVolume / Math.max(1.0D, this.catchmentArea * 0.2D);
        WaterloggingStateEnum nextState = this.waterDepth > 0.05D
                ? WaterloggingStateEnum.WATERLOGGED
                : WaterloggingStateEnum.NO_WATERLOGGING;
        if (this.waterloggingState != nextState) {
            this.waterloggingState = nextState;
            if (session != null) {
                session.enqueueObjectEvent("sa", this.instanceId, "区域积水状态->" + nextState.getZhLabel());
            }
        }
        if (session != null) {
            session.enqueueObjectEvent("v", this.instanceId, "区域积水深度变化");
        }
    }
}

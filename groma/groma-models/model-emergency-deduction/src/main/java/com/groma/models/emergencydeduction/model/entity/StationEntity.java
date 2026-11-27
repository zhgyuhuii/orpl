package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.enums.FloodWallStateEnum;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;

/**
 * 车站实体（对应真实模型 TYPE: 车站, eid=evCR8qXNrJwtoje0FEf，含海滩寺站/沙口路站/月季公园站）
 */
@GromaMeta(id = "evCR8qXNrJwtoje0FEf", zhName = "车站", kind = "TYPE", essence = "PHYSICAL")
public class StationEntity {

    private final String instanceId;
    private final String name;
    @GromaMeta(id = "st_floor_elev", zhName = "站内底高程", unit = "m")
    private double floorElevation = 86.0D;
    @GromaMeta(id = "st_entry_elev", zhName = "入口底高程", unit = "m")
    private double entranceElevation = 98.4D;
    @GromaMeta(id = "st_wall_top", zhName = "挡水墙顶高程", unit = "m")
    private double floodWallTopElevation = 99.1D;
    @GromaMeta(id = "st_area", zhName = "等效蓄水面积", unit = "m2")
    private double storageArea = 4_500.0D;
    @GromaMeta(id = "st_drain_rate", zhName = "排水额定能力", unit = "m3/s")
    private double ratedDrainRate = 0.45D;
    @GromaMeta(id = "st_water_vol", zhName = "站内积水量", unit = "m3")
    private double waterVolume = 0.0D;
    @GromaMeta(id = "st_water_depth", zhName = "站内水深", unit = "m")
    private double waterDepth = 0.0D;
    @GromaMeta(id = "st_wall_state", zhName = "挡水墙状态", initState = "完好")
    private FloodWallStateEnum floodWallState = FloodWallStateEnum.INTACT;
    @GromaMeta(id = "st_flood_state", zhName = "积水状态", initState = "无积水")
    private WaterloggingStateEnum waterloggingState = WaterloggingStateEnum.NO_WATERLOGGING;

    public StationEntity(String instanceId, String name, double floorElevation) {
        this.instanceId = instanceId;
        this.name = name;
        this.floorElevation = floorElevation;
    }

    public String getInstanceId() { return instanceId; }
    public String getName() { return name; }
    public double getFloorElevation() { return floorElevation; }
    public double getEntranceElevation() { return entranceElevation; }
    public double getFloodWallTopElevation() { return floodWallTopElevation; }
    public double getStorageArea() { return storageArea; }
    public double getRatedDrainRate() { return ratedDrainRate; }
    public double getWaterVolume() { return waterVolume; }
    public double getWaterDepth() { return waterDepth; }
    public FloodWallStateEnum getFloodWallState() { return floodWallState; }
    public WaterloggingStateEnum getWaterloggingState() { return waterloggingState; }

    public double currentWaterSurfaceElevation() {
        return floorElevation + waterDepth;
    }

    public void breakFloodWall(AbstractWorldSession session) {
        if (this.floodWallState != FloodWallStateEnum.FAILED) {
            this.floodWallState = FloodWallStateEnum.FAILED;
            if (session != null) {
                session.enqueueObjectEvent("sa", this.instanceId, "挡水墙失效");
            }
        }
    }

    public void updateStationWater(double nextVolume, AbstractWorldSession session) {
        this.waterVolume = Math.max(0.0D, nextVolume);
        this.waterDepth = this.waterVolume / Math.max(1.0D, this.storageArea);
        this.waterloggingState = this.waterDepth > 0.02D
                ? WaterloggingStateEnum.WATERLOGGED
                : WaterloggingStateEnum.NO_WATERLOGGING;
        if (session != null) {
            session.enqueueObjectEvent("v", this.instanceId, "站内水深变化");
        }
    }
}

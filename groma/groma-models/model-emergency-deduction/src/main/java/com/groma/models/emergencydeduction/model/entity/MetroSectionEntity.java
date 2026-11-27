package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;

/**
 * 地铁区间实体（对应真实模型 TYPE: 地铁区间, eid=ePti21Ff4NoXX0BEFnG；布尔属性“是否放行”遵循阿里规约命名为 releaseAllowed）
 */
@GromaMeta(id = "ePti21Ff4NoXX0BEFnG", zhName = "地铁区间", kind = "TYPE", essence = "PHYSICAL")
public class MetroSectionEntity {

    private final String instanceId;
    private final String name;
    @GromaMeta(id = "sec_len", zhName = "长度", unit = "m")
    private double lengthMeters;
    @GromaMeta(id = "sec_width", zhName = "等效储水宽度", unit = "m")
    private double equivalentWidth = 5.4D;
    @GromaMeta(id = "sec_base_elev", zhName = "代表底高程", unit = "m")
    private double baseElevation;
    @GromaMeta(id = "sec_stop_depth", zhName = "迫停水深", unit = "m")
    private double forcedStopWaterDepth = 0.15D;
    @GromaMeta(id = "sec_danger_depth", zhName = "危险水深", unit = "m")
    private double dangerWaterDepth = 0.35D;
    @GromaMeta(id = "sec_vol", zhName = "积水量", unit = "m3")
    private double waterVolume = 0.0D;
    @GromaMeta(id = "sec_depth", zhName = "代表性水深", unit = "m")
    private double representativeWaterDepth = 0.0D;
    @GromaMeta(id = "sec_release", zhName = "是否放行")
    private boolean releaseAllowed = true;
    @GromaMeta(id = "sec_state", zhName = "积水状态", initState = "无积水")
    private WaterloggingStateEnum waterloggingState = WaterloggingStateEnum.NO_WATERLOGGING;

    public MetroSectionEntity(String instanceId, String name, double lengthMeters, double baseElevation) {
        this.instanceId = instanceId;
        this.name = name;
        this.lengthMeters = lengthMeters;
        this.baseElevation = baseElevation;
    }

    public String getInstanceId() { return instanceId; }
    public String getName() { return name; }
    public double getLengthMeters() { return lengthMeters; }
    public double getBaseElevation() { return baseElevation; }
    public double getForcedStopWaterDepth() { return forcedStopWaterDepth; }
    public double getDangerWaterDepth() { return dangerWaterDepth; }
    public double getWaterVolume() { return waterVolume; }
    public double getRepresentativeWaterDepth() { return representativeWaterDepth; }
    public boolean isReleaseAllowed() { return releaseAllowed; }
    public WaterloggingStateEnum getWaterloggingState() { return waterloggingState; }

    public double currentWaterSurfaceElevation() {
        return baseElevation + representativeWaterDepth;
    }

    public void setReleaseAllowed(boolean releaseAllowed, AbstractWorldSession session) {
        if (this.releaseAllowed != releaseAllowed) {
            this.releaseAllowed = releaseAllowed;
            if (session != null) {
                session.enqueueObjectEvent("v", this.instanceId, "是否放行变更->" + releaseAllowed);
            }
        }
    }

    public void updateSectionWater(double nextVolume, AbstractWorldSession session) {
        this.waterVolume = Math.max(0.0D, nextVolume);
        double area = Math.max(1.0D, this.lengthMeters * this.equivalentWidth);
        this.representativeWaterDepth = this.waterVolume / area;
        this.waterloggingState = this.representativeWaterDepth > 0.01D
                ? WaterloggingStateEnum.WATERLOGGED
                : WaterloggingStateEnum.NO_WATERLOGGING;
        if (this.representativeWaterDepth >= this.forcedStopWaterDepth && this.releaseAllowed) {
            setReleaseAllowed(false, session);
        }
        if (session != null) {
            session.enqueueObjectEvent("v", this.instanceId, "区间水深变化");
        }
    }
}

package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.enums.FloodWallStateEnum;
import com.groma.models.emergencydeduction.service.IHydrologyAndTrainOperatorService;
import com.groma.models.emergencydeduction.utils.EmergencyConditionJudgeUtil;
import org.springframework.stereotype.Service;

/**
 * 水文汇水与连接处过水独立过程/算子实现（还原真实模型《计算降雨汇水》《确定两端水位》《确定连接处过水情况》）
 */
@Service
public class HydrologyAndTrainOperatorServiceImpl implements IHydrologyAndTrainOperatorService {

    @Override
    @GromaMeta(id = "d12b2dde86bf4788848bf1c7a0f38d09", zhName = "计算降雨汇水", kind = "OPERATOR")
    public void computeAreaRainfallRunoff(
            AbstractWorldSession ctx, AreaEntity area, double rainfallMmPerHour, double stepSeconds) {
        double rainfallMetersPerSec = (rainfallMmPerHour / 1000.0D) / 3600.0D;
        double inflowM3 = rainfallMetersPerSec * area.getCatchmentArea() * area.getRunoffCoeff() * stepSeconds;
        double drainedM3 = area.getSurfaceDrainCapacity() * stepSeconds;
        area.updateWaterVolumeAndDepth(area.getWaterVolume() + inflowM3 - drainedM3, ctx);
        // @GromaUserCode-START [d12b2dde86bf4788848bf1c7a0f38d09]
        // @GromaUserCode-END [d12b2dde86bf4788848bf1c7a0f38d09]
    }

    @Override
    @GromaMeta(id = "482a71540fef436f89877d8122f645f6", zhName = "区域与站内水流交换", kind = "OPERATOR")
    public void exchangeAreaAndStationWater(
            AbstractWorldSession ctx, AreaEntity area, StationEntity station, double stepSeconds) {
        double areaSurfElev = area.getGroundElevation() + area.getWaterDepth();
        if (EmergencyConditionJudgeUtil.shouldFloodWallFail(areaSurfElev, station.getFloodWallTopElevation())) {
            station.breakFloodWall(ctx);
        }
        double effectiveThreshold = station.getFloodWallState() == FloodWallStateEnum.INTACT
                ? station.getFloodWallTopElevation()
                : station.getEntranceElevation();
        double weirWidth = station.getFloodWallState() == FloodWallStateEnum.INTACT ? 2.0D : 8.5D;

        double flowRate = computeConnectionFlowM3PerSec(
                areaSurfElev, station.currentWaterSurfaceElevation(), effectiveThreshold, weirWidth);
        double deltaM3 = flowRate * stepSeconds;
        if (deltaM3 > 0.0D) {
            double transferable = Math.min(area.getWaterVolume(), deltaM3);
            area.updateWaterVolumeAndDepth(area.getWaterVolume() - transferable, ctx);
            double pumped = station.getRatedDrainRate() * stepSeconds;
            station.updateStationWater(station.getWaterVolume() + transferable - pumped, ctx);
        }
    }

    @Override
    @GromaMeta(id = "4b19b9f71d134f588bb6407e2dee8e49", zhName = "确定连接处过水情况", kind = "INDEPENDENT_OPERATOR")
    public double computeConnectionFlowM3PerSec(
            double elevationA, double elevationB, double controlElev, double weirWidth) {
        double high = Math.max(elevationA, elevationB);
        if (high <= controlElev) {
            return 0.0D;
        }
        double head = high - Math.max(controlElev, Math.min(elevationA, elevationB));
        double sign = elevationA >= elevationB ? 1.0D : -1.0D;
        // 宽顶堰流公式 Q = m * B * sqrt(2g) * H^(1.5)
        return sign * 0.385D * weirWidth * StrictMath.sqrt(2.0D * 9.81D) * StrictMath.pow(head, 1.5D);
    }

    @Override
    @GromaMeta(id = "cJDNo2vlnmlm63IXjzL", zhName = "车站与区间水流交换", kind = "OPERATOR")
    public void exchangeStationAndSectionWater(
            AbstractWorldSession ctx, StationEntity station, MetroSectionEntity section, double stepSeconds) {
        double flow = computeConnectionFlowM3PerSec(
                station.currentWaterSurfaceElevation(),
                section.currentWaterSurfaceElevation(),
                station.getFloorElevation() + 0.05D,
                4.2D
        );
        double deltaVol = flow * stepSeconds;
        if (Math.abs(deltaVol) > 1.0e-6D) {
            station.updateStationWater(station.getWaterVolume() - deltaVol, ctx);
            section.updateSectionWater(section.getWaterVolume() + deltaVol, ctx);
        }
    }
}

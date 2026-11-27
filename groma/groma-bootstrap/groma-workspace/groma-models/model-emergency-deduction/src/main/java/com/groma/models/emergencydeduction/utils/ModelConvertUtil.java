package com.groma.models.emergencydeduction.utils;

import com.groma.base.engine.BaseSimEntity;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.AreaWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity;
import com.groma.models.emergencydeduction.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction.model.entity.FileServiceEntity;
import com.groma.models.emergencydeduction.model.entity.FileTypeEntity;
import com.groma.models.emergencydeduction.model.entity.FloodProtectionEntity;
import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction.model.entity.GeoRegionEntity;
import com.groma.models.emergencydeduction.model.entity.GeoRouteEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithSectionEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.MetroLineEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.PlanExecutorEntity;
import com.groma.models.emergencydeduction.model.entity.PlanGeneratorEntity;
import com.groma.models.emergencydeduction.model.entity.RainfallAffectedAreaEntity;
import com.groma.models.emergencydeduction.model.entity.RainfallCatchmentEntity;
import com.groma.models.emergencydeduction.model.entity.RainfallEntity;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;
import com.groma.models.emergencydeduction.model.entity.Scene2Entity;
import com.groma.models.emergencydeduction.model.entity.SectionWithEndStationEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithPassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.SuspensionControlEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainPassingEntity;
import com.groma.models.emergencydeduction.model.entity.TrainWithPassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.UndergroundInflowEntity;
import com.groma.models.emergencydeduction.model.vo.AreaVO;
import com.groma.models.emergencydeduction.model.vo.AreaWithStationVO;
import com.groma.models.emergencydeduction.model.vo.DisposalPlanVO;
import com.groma.models.emergencydeduction.model.vo.FileInfoVO;
import com.groma.models.emergencydeduction.model.vo.FileServiceVO;
import com.groma.models.emergencydeduction.model.vo.FileTypeVO;
import com.groma.models.emergencydeduction.model.vo.FloodProtectionVO;
import com.groma.models.emergencydeduction.model.vo.GeoPointVO;
import com.groma.models.emergencydeduction.model.vo.GeoRegionVO;
import com.groma.models.emergencydeduction.model.vo.GeoRouteVO;
import com.groma.models.emergencydeduction.model.vo.LineWithSectionVO;
import com.groma.models.emergencydeduction.model.vo.LineWithStationVO;
import com.groma.models.emergencydeduction.model.vo.LineWithTrainVO;
import com.groma.models.emergencydeduction.model.vo.MetroLineVO;
import com.groma.models.emergencydeduction.model.vo.MetroSectionVO;
import com.groma.models.emergencydeduction.model.vo.PassengerGroupVO;
import com.groma.models.emergencydeduction.model.vo.PlanExecutorVO;
import com.groma.models.emergencydeduction.model.vo.PlanGeneratorVO;
import com.groma.models.emergencydeduction.model.vo.RainfallAffectedAreaVO;
import com.groma.models.emergencydeduction.model.vo.RainfallCatchmentVO;
import com.groma.models.emergencydeduction.model.vo.RainfallVO;
import com.groma.models.emergencydeduction.model.vo.Scene1VO;
import com.groma.models.emergencydeduction.model.vo.Scene2VO;
import com.groma.models.emergencydeduction.model.vo.SectionWithEndStationVO;
import com.groma.models.emergencydeduction.model.vo.SectionWithTrainVO;
import com.groma.models.emergencydeduction.model.vo.StationVO;
import com.groma.models.emergencydeduction.model.vo.StationWithParkedTrainVO;
import com.groma.models.emergencydeduction.model.vo.StationWithPassengerGroupVO;
import com.groma.models.emergencydeduction.model.vo.SuspensionControlVO;
import com.groma.models.emergencydeduction.model.vo.TrainPassingVO;
import com.groma.models.emergencydeduction.model.vo.TrainVO;
import com.groma.models.emergencydeduction.model.vo.TrainWithPassengerGroupVO;
import com.groma.models.emergencydeduction.model.vo.UndergroundInflowVO;

/**
 * 零反射 Entity -> VO 转换器
 */
public final class ModelConvertUtil {
    private ModelConvertUtil() {
    }

    public static PassengerGroupVO toPassengerGroupVO(PassengerGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PassengerGroupVO(entity.getInstanceId(), entity.isAlive(), entity.getName(), entity.getTotalPeopleCount(), entity.getPassengerState(), entity.getFirstTrappedTime(), entity.getAllSafeTime(), entity.getSafePeopleCount(), entity.isExperiencedDangerDepth());
    }

    public static GeoRouteVO toGeoRouteVO(GeoRouteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new GeoRouteVO(entity.getInstanceId(), entity.isAlive(), entity.getCoordinateGroup() == null ? null : entity.getCoordinateGroup().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList());
    }

    public static FileTypeVO toFileTypeVO(FileTypeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FileTypeVO(entity.getInstanceId(), entity.isAlive(), entity.getPath(), entity.getName(), entity.getType(), entity.getSize(), entity.getCreateTime(), entity.getModifyTime(), entity.getHashValue());
    }

    public static GeoRegionVO toGeoRegionVO(GeoRegionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new GeoRegionVO(entity.getInstanceId(), entity.isAlive(), entity.getCoordinateGroup() == null ? null : entity.getCoordinateGroup().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList());
    }

    public static MetroSectionVO toMetroSectionVO(MetroSectionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MetroSectionVO(entity.getInstanceId(), entity.isAlive(), entity.getLength(), entity.getEquivalentStorageWidth(), entity.getDangerDepth(), entity.getForcedStopDepth(), entity.getWaterVolume(), entity.getRepresentativeDepth(), entity.isReleaseAllowed(), entity.getName(), entity.getPosition() == null ? null : entity.getPosition().getInstanceId(), entity.getRepresentativeFloorElevation(), entity.getWaterloggingState());
    }

    public static SectionWithTrainVO toSectionWithTrainVO(SectionWithTrainEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SectionWithTrainVO(entity.getInstanceId(), entity.isAlive(), entity.getTrain() == null ? null : entity.getTrain().getInstanceId(), entity.getSection() == null ? null : entity.getSection().getInstanceId(), entity.getTravelledDistance(), entity.getTargetStation() == null ? null : entity.getTargetStation().getInstanceId(), entity.getDepartureStation() == null ? null : entity.getDepartureStation().getInstanceId(), entity.isArrivedTargetStation());
    }

    public static DisposalPlanVO toDisposalPlanVO(DisposalPlanEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DisposalPlanVO(entity.getInstanceId(), entity.isAlive(), entity.getPlanCode(), entity.getPlanName(), entity.getAssociatedTrain() == null ? null : entity.getAssociatedTrain().getInstanceId(), entity.getTrainPassTime(), entity.getSuspensionIssueTime(), entity.getReleaseStopTime());
    }

    public static AreaWithStationVO toAreaWithStationVO(AreaWithStationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AreaWithStationVO(entity.getInstanceId(), entity.isAlive(), entity.getArea() == null ? null : entity.getArea().getInstanceId(), entity.getStation() == null ? null : entity.getStation().getInstanceId(), entity.getCurrentFlowRate(), entity.getStepFlowVolume());
    }

    public static FileServiceVO toFileServiceVO(FileServiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FileServiceVO(entity.getInstanceId(), entity.isAlive());
    }

    public static RainfallVO toRainfallVO(RainfallEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RainfallVO(entity.getInstanceId(), entity.isAlive());
    }

    public static LineWithSectionVO toLineWithSectionVO(LineWithSectionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LineWithSectionVO(entity.getInstanceId(), entity.isAlive(), entity.getMetroLine() == null ? null : entity.getMetroLine().getInstanceId(), entity.getMetroSection() == null ? null : entity.getMetroSection().getInstanceId(), entity.getSectionStartMileage(), entity.getSectionEndMileage());
    }

    public static LineWithTrainVO toLineWithTrainVO(LineWithTrainEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LineWithTrainVO(entity.getInstanceId(), entity.isAlive(), entity.getLine() == null ? null : entity.getLine().getInstanceId(), entity.getTrain() == null ? null : entity.getTrain().getInstanceId(), entity.getTrainMileageOnLine(), entity.isAlongIncreasingMileage(), entity.getCurrentParkedRelation() == null ? null : entity.getCurrentParkedRelation().getInstanceId(), entity.getCurrentRunningRelation() == null ? null : entity.getCurrentRunningRelation().getInstanceId(), entity.isDemoRoundTrip());
    }

    public static StationWithPassengerGroupVO toStationWithPassengerGroupVO(
            StationWithPassengerGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StationWithPassengerGroupVO(entity.getInstanceId(), entity.isAlive(), entity.getPassengerGroup() == null ? null : entity.getPassengerGroup().getInstanceId(), entity.getStation() == null ? null : entity.getStation().getInstanceId(), entity.getCurrentStationPeopleCount(), entity.getEvacuatedPeopleCount(), entity.isEvacuationStarted(), entity.getEvacuationState());
    }

    public static StationWithParkedTrainVO toStationWithParkedTrainVO(
            StationWithParkedTrainEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StationWithParkedTrainVO(entity.getInstanceId(), entity.isAlive(), entity.getTrain() == null ? null : entity.getTrain().getInstanceId(), entity.getStation() == null ? null : entity.getStation().getInstanceId(), entity.getActualDepartureTime(), entity.getActualArrivalTime(), entity.getPrescribedStopDuration(), entity.isDepartureAllowed());
    }

    public static MetroLineVO toMetroLineVO(MetroLineEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MetroLineVO(entity.getInstanceId(), entity.isAlive(), entity.getLineName(), entity.getStartMileage(), entity.getEndMileage());
    }

    public static RainfallAffectedAreaVO toRainfallAffectedAreaVO(
            RainfallAffectedAreaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RainfallAffectedAreaVO(entity.getInstanceId(), entity.isAlive(), entity.getRainfall() == null ? null : entity.getRainfall().getInstanceId(), entity.getArea() == null ? null : entity.getArea().getInstanceId(), entity.getCurrentCatchmentFlow(), entity.getStepCatchmentVolume(), entity.getCurrentRainIntensity(), entity.getCumulativeRainfall(), entity.getRainfallSeries());
    }

    public static PlanExecutorVO toPlanExecutorVO(PlanExecutorEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PlanExecutorVO(entity.getInstanceId(), entity.isAlive(), entity.getActivePlan() == null ? null : entity.getActivePlan().getInstanceId(), entity.getCurrentPlanStage(), entity.getActualIssueTime(), entity.getActualEffectiveTime());
    }

    public static LineWithStationVO toLineWithStationVO(LineWithStationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LineWithStationVO(entity.getInstanceId(), entity.isAlive(), entity.getMetroLine() == null ? null : entity.getMetroLine().getInstanceId(), entity.getStation() == null ? null : entity.getStation().getInstanceId(), entity.getLineMileage(), entity.getStationType(), entity.getStationNo());
    }

    public static GeoPointVO toGeoPointVO(GeoPointEntity entity) {
        if (entity == null) {
            return null;
        }
        return new GeoPointVO(entity.getInstanceId(), entity.isAlive(), entity.getLongitude(), entity.getLatitude(), entity.getHeight());
    }

    public static AreaVO toAreaVO(AreaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AreaVO(entity.getInstanceId(), entity.isAlive(), entity.getCatchmentArea(), entity.getRunoffCoeff(), entity.getEquivalentStorageArea(), entity.getGroundElevation(), entity.getSurfaceDrainCapacity(), entity.getWaterVolume(), entity.getWaterDepth(), entity.getName(), entity.getPosition() == null ? null : entity.getPosition().getInstanceId(), entity.getWaterloggingState());
    }

    public static FileInfoVO toFileInfoVO(FileInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FileInfoVO(entity.getInstanceId(), entity.isAlive(), entity.getCreateTime(), entity.getName(), entity.getHashValue(), entity.getSize(), entity.getType(), entity.getModifyTime());
    }

    public static TrainWithPassengerGroupVO toTrainWithPassengerGroupVO(
            TrainWithPassengerGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TrainWithPassengerGroupVO(entity.getInstanceId(), entity.isAlive(), entity.getTrain() == null ? null : entity.getTrain().getInstanceId(), entity.getPassengerGroup() == null ? null : entity.getPassengerGroup().getInstanceId(), entity.getCurrentOnBoardCount());
    }

    public static PlanGeneratorVO toPlanGeneratorVO(PlanGeneratorEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PlanGeneratorVO(entity.getInstanceId(), entity.isAlive(), entity.getCandidatePlanList() == null ? null : entity.getCandidatePlanList().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList(), entity.getRainfall() == null ? null : entity.getRainfall().getInstanceId(), entity.getSurfaceWaterloggingArea() == null ? null : entity.getSurfaceWaterloggingArea().getInstanceId(), entity.getFloodWall() == null ? null : entity.getFloodWall().getInstanceId(), entity.getMetroSection() == null ? null : entity.getMetroSection().getInstanceId(), entity.getTrain() == null ? null : entity.getTrain().getInstanceId());
    }

    public static StationVO toStationVO(StationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StationVO(entity.getInstanceId(), entity.isAlive(), entity.getStationWaterVolume(), entity.getFailureThreshold(), entity.getFailureDuration(), entity.getFloodWallTopElevation(), entity.getName(), entity.getRatedDrainCapacity(), entity.getStationFloorElevation(), entity.getEquivalentStorageArea(), entity.getStationWaterDepth(), entity.getEvacuationCapacity(), entity.getPosition() == null ? null : entity.getPosition().getInstanceId(), entity.getWaterloggingState(), entity.getFloodWallState(), entity.getIntactFlowWidth(), entity.getFailedFlowWidth(), entity.getInletFloorElevation(), entity.getInletFlowCoeff(), entity.getOverlimitDuration());
    }

    public static TrainVO toTrainVO(TrainEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TrainVO(entity.getInstanceId(), entity.isAlive(), entity.getCurrentSpeed(), entity.getForcedStopTime(), entity.getInitialPosition() == null ? null : entity.getInitialPosition().getInstanceId(), entity.getName(), entity.getTrainState(), entity.getDirection(), entity.getCurrentPosition() == null ? null : entity.getCurrentPosition().getInstanceId(), entity.getTravelSpeed());
    }

    public static SectionWithEndStationVO toSectionWithEndStationVO(
            SectionWithEndStationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SectionWithEndStationVO(entity.getInstanceId(), entity.isAlive(), entity.getStation() == null ? null : entity.getStation().getInstanceId(), entity.getSection() == null ? null : entity.getSection().getInstanceId(), entity.getStepFlowVolume(), entity.getJunctionFloorElevation(), entity.getEffectiveFlowWidth(), entity.getFlowCoeff(), entity.getCurrentFlowRate());
    }

    public static Scene2VO toScene2VO(Scene2Entity entity) {
        if (entity == null) {
            return null;
        }
        return new Scene2VO(entity.getInstanceId(), entity.isAlive(), entity.getRainfallRelatedArea() == null ? null : entity.getRainfallRelatedArea().getInstanceId(), entity.getAreaWithStation() == null ? null : entity.getAreaWithStation().getInstanceId(), entity.getSectionWithEndStation() == null ? null : entity.getSectionWithEndStation().getInstanceId(), entity.getLineWithTrain() == null ? null : entity.getLineWithTrain().getInstanceId());
    }

    public static Scene1VO toScene1VO(Scene1Entity entity) {
        if (entity == null) {
            return null;
        }
        return new Scene1VO(entity.getInstanceId(), entity.isAlive(), entity.getTrainState1(), entity.isText1(), entity.getText2());
    }

    public static TrainPassingVO toTrainPassingVO(TrainPassingEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TrainPassingVO(entity.getInstanceId(), entity.isAlive(), entity.getPassingTrain() == null ? null : entity.getPassingTrain().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList(), entity.getPassingSection() == null ? null : entity.getPassingSection().getInstanceId());
    }

    public static UndergroundInflowVO toUndergroundInflowVO(UndergroundInflowEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UndergroundInflowVO(entity.getInstanceId(), entity.isAlive(), entity.getSurfaceWaterloggingSource() == null ? null : entity.getSurfaceWaterloggingSource().getInstanceId(), entity.getInflowSection() == null ? null : entity.getInflowSection().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList());
    }

    public static RainfallCatchmentVO toRainfallCatchmentVO(RainfallCatchmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RainfallCatchmentVO(entity.getInstanceId(), entity.isAlive(), entity.getRainfallSource() == null ? null : entity.getRainfallSource().getInstanceId(), entity.getCatchmentArea() == null ? null : entity.getCatchmentArea().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList());
    }

    public static FloodProtectionVO toFloodProtectionVO(FloodProtectionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FloodProtectionVO(entity.getInstanceId(), entity.isAlive(), entity.getProtectiveFloodWall() == null ? null : entity.getProtectiveFloodWall().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList(), entity.getProtectedSection() == null ? null : entity.getProtectedSection().getInstanceId());
    }

    public static SuspensionControlVO toSuspensionControlVO(SuspensionControlEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SuspensionControlVO(entity.getInstanceId(), entity.isAlive(), entity.getDecisionPlan() == null ? null : entity.getDecisionPlan().getInstanceId(), entity.getControlSection() == null ? null : entity.getControlSection().stream().map(item -> item instanceof BaseSimEntity ref ? ref.getInstanceId() : String.valueOf(item)).toList());
    }
}

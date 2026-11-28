package com.groma.models.emergencydeduction.engine.context;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.base.engine.WorldSnapshot;
import com.groma.models.emergencydeduction.engine.process.TrainSectionRunProcessTask;
import com.groma.models.emergencydeduction.engine.relation.SectionTrainRelationManager;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.enums.WarningLevelEnum;
import com.groma.models.emergencydeduction.model.vo.ThreeStationSceneSnapshotVO;
import com.groma.models.emergencydeduction.service.IHydrologyAndTrainOperatorService;
import com.groma.models.emergencydeduction.utils.EmergencyConditionJudgeUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权威场景 1 单写者推演沙盒：《三站两区间积水与列车运行》（eid=d1bcde361aa04509be8878d5de798984）
 * 严格无锁单线程演进：金水区降雨 -> 海滩寺站/沙口路站/月季公园站 -> 海沙区间/沙月区间 -> 0501次列车运行与防汛迫停
 */
@GromaMeta(id = "d1bcde361aa04509be8878d5de798984", zhName = "三站两区间积水与列车运行", kind = "SCENE_CONTEXT")
public final class ThreeStationFloodTrainWorldContext extends AbstractWorldSession {

    private final IHydrologyAndTrainOperatorService operatorService;
    private double rainfallMmPerHour;

    private AreaEntity jinshuiArea;
    private StationEntity haitansiStation;
    private StationEntity shakouluStation;
    private StationEntity yuejigongyuanStation;
    private MetroSectionEntity haishaSection;
    private MetroSectionEntity shayueSection;
    private TrainEntity metroTrain01;
    private final SectionTrainRelationManager sectionTrainRelation = new SectionTrainRelationManager();
    private TrainSectionRunProcessTask activeTrainRunTask;

    public ThreeStationFloodTrainWorldContext(
            String sessionId,
            long startEpochMs,
            long finalStepMs,
            long seed,
            double rainfallMmPerHour,
            IHydrologyAndTrainOperatorService operatorService) {
        super(sessionId, "emergency-deduction", "825", "three-station-flood-train", startEpochMs, finalStepMs, seed);
        this.rainfallMmPerHour = rainfallMmPerHour;
        this.operatorService = operatorService;
    }

    @Override
    protected void initEntitiesAndDefaultStarts() {
        this.jinshuiArea = new AreaEntity("3854f698f8f642879ef4c9adcee412ad", "金水区汇水区域");
        this.haitansiStation = new StationEntity("92d8676cc6f54745aec7d1ca8fcbf0c3", "海滩寺站", 86.2D);
        this.shakouluStation = new StationEntity("c03381604b49441d9bfd325296c6c44c", "沙口路站", 85.4D);
        this.yuejigongyuanStation = new StationEntity("a91a3271b7a04f1a80cc47c5ccfd7dfb", "月季公园站", 86.0D);

        this.haishaSection = new MetroSectionEntity("1ee1207863ba4cd59a35610c0b97f6ec", "海滩寺—沙口路区间", 1_350.0D, 84.0D);
        this.shayueSection = new MetroSectionEntity("002c4827a9d6441e81325b1f22253785", "沙口路—月季公园区间", 1_480.0D, 84.3D);

        this.metroTrain01 = new TrainEntity("a3c20fb5188145f98575d73ff5e0bf88", "0501次列车", "海滩寺站");
        this.sectionTrainRelation.bind(haishaSection, metroTrain01);
        this.activeTrainRunTask = new TrainSectionRunProcessTask(metroTrain01, haishaSection, shakouluStation);
    }

    @Override
    protected void loadArchiveState(WorldSnapshot archiveSnap) {
        initEntitiesAndDefaultStarts();
    }

    @Override
    protected void dispatchSingleEvent(WorldSnapshot.SimEventRecord event) {
        // 当列车到达沙口路站停稳且后续沙月区间允许放行时，自动触发下一区间行驶过程
        if ("sa".equals(event.eventCode()) && "沙口路站".equals(metroTrain01.getCurrentPosition())
                && shayueSection.isReleaseAllowed()) {
            sectionTrainRelation.unbind(haishaSection, metroTrain01);
            sectionTrainRelation.bind(shayueSection, metroTrain01);
            this.activeTrainRunTask = new TrainSectionRunProcessTask(metroTrain01, shayueSection, yuejigongyuanStation);
        }
    }

    @Override
    protected void executeActiveProcessesAtTick(long tick, long worldTimeMs) {
        double stepSec = Math.max(0.1D, this.finalStepMs / 1000.0D);
        operatorService.computeAreaRainfallRunoff(this, jinshuiArea, rainfallMmPerHour, stepSec);
        operatorService.exchangeAreaAndStationWater(this, jinshuiArea, haitansiStation, stepSec);
        operatorService.exchangeAreaAndStationWater(this, jinshuiArea, shakouluStation, stepSec);
        operatorService.exchangeAreaAndStationWater(this, jinshuiArea, yuejigongyuanStation, stepSec);

        operatorService.exchangeStationAndSectionWater(this, haitansiStation, haishaSection, stepSec);
        operatorService.exchangeStationAndSectionWater(this, shakouluStation, haishaSection, stepSec);
        operatorService.exchangeStationAndSectionWater(this, shakouluStation, shayueSection, stepSec);
        operatorService.exchangeStationAndSectionWater(this, yuejigongyuanStation, shayueSection, stepSec);

        if (activeTrainRunTask != null) {
            activeTrainRunTask.executeTick(this, stepSec);
        }
    }

    @Override
    protected void commitCompletedProcesses() {
        if (activeTrainRunTask != null && !"RUNNING".equals(activeTrainRunTask.getStatus())) {
            activeTrainRunTask = null;
        }
    }

    @Override
    protected void sweepIdleInstancesAtSafePoint(long tick) {
    }

    @Override
    protected boolean evaluatePersistentStopCondition() {
        return false;
    }

    @Override
    protected boolean evaluateOneShotPauseCondition() {
        return false;
    }

    @Override
    protected WorldSnapshot buildImmutableSnapshot() {
        Map<String, Map<String, Object>> instMap = new LinkedHashMap<>();
        instMap.put(jinshuiArea.getName(), Map.of("waterDepth", jinshuiArea.getWaterDepth(), "_state", jinshuiArea.getWaterloggingState().getZhLabel()));
        instMap.put(haitansiStation.getName(), Map.of("waterDepth", haitansiStation.getWaterDepth(), "wallState", haitansiStation.getFloodWallState().getZhLabel()));
        instMap.put(shakouluStation.getName(), Map.of("waterDepth", shakouluStation.getWaterDepth(), "wallState", shakouluStation.getFloodWallState().getZhLabel()));
        instMap.put(haishaSection.getName(), Map.of("waterDepth", haishaSection.getRepresentativeWaterDepth(), "releaseAllowed", haishaSection.isReleaseAllowed()));
        instMap.put(shayueSection.getName(), Map.of("waterDepth", shayueSection.getRepresentativeWaterDepth(), "releaseAllowed", shayueSection.isReleaseAllowed()));
        instMap.put(metroTrain01.getName(), Map.of("position", metroTrain01.getCurrentPosition(), "_state", metroTrain01.getTrainState().getZhLabel()));

        return new WorldSnapshot(
                this.sessionId, this.modelCode, this.modelVersion, this.sceneCode, 0L,
                this.tickIndex, currentWorldTimeMs(), effectiveSpeed(), "RUNNING",
                Map.copyOf(instMap), List.of(), List.of(), copyProblems()
        );
    }

    public ThreeStationSceneSnapshotVO toSceneVO() {
        double maxSecDepth = Math.max(haishaSection.getRepresentativeWaterDepth(), shayueSection.getRepresentativeWaterDepth());
        WarningLevelEnum warn = EmergencyConditionJudgeUtil.evaluateWarningLevel(maxSecDepth);
        return new ThreeStationSceneSnapshotVO(
                this.sessionId,
                this.tickIndex,
                currentWorldTimeMs(),
                warn.getZhLabel(),
                jinshuiArea.getWaterDepth(),
                Map.of(
                        haitansiStation.getName(), haitansiStation.getWaterDepth(),
                        shakouluStation.getName(), shakouluStation.getWaterDepth(),
                        yuejigongyuanStation.getName(), yuejigongyuanStation.getWaterDepth()
                ),
                Map.of(
                        haishaSection.getName(), haishaSection.getRepresentativeWaterDepth(),
                        shayueSection.getName(), shayueSection.getRepresentativeWaterDepth()
                ),
                metroTrain01.getCurrentPosition(),
                metroTrain01.getTrainState().getZhLabel(),
                haishaSection.isReleaseAllowed() && shayueSection.isReleaseAllowed(),
                copyProblems()
        );
    }
}

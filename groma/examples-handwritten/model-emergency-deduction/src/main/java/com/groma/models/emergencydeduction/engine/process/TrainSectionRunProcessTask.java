package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import com.groma.models.emergencydeduction.utils.EmergencyConditionJudgeUtil;

/**
 * 耗时过程状态机：列车区间行驶与水深迫停（对应真实模型 PROCESS: 区间行驶, eid=BubMgjTXeY6IhWjh0lr）
 */
@GromaMeta(id = "BubMgjTXeY6IhWjh0lr", zhName = "区间行驶", kind = "PROCESS_TASK")
public final class TrainSectionRunProcessTask {

    private final TrainEntity train;
    private final MetroSectionEntity currentSection;
    private final StationEntity nextStation;
    private String status = "RUNNING";

    public TrainSectionRunProcessTask(TrainEntity train, MetroSectionEntity currentSection, StationEntity nextStation) {
        this.train = train;
        this.currentSection = currentSection;
        this.nextStation = nextStation;
    }

    public void executeTick(AbstractWorldSession ctx, double stepSeconds) {
        if (!"RUNNING".equals(status)) {
            return;
        }
        // R21 维持条件检查：若区间水深达到迫停水深或禁止放行，列车立即转入迫停状态并中断行驶过程
        if (!currentSection.isReleaseAllowed() || EmergencyConditionJudgeUtil.shouldForceStopTrain(
                currentSection.getRepresentativeWaterDepth(), currentSection.getForcedStopWaterDepth())) {
            train.transitionState(
                    TrainStateEnum.FORCED_STOP,
                    currentSection.getName() + "(水深超限迫停@" + Math.round(train.getSectionProgressMeters()) + "m)",
                    0.0D,
                    ctx
            );
            this.status = "ABORTED";
            return;
        }

        train.transitionState(TrainStateEnum.RUNNING, currentSection.getName(), train.getCruiseSpeedMps(), ctx);
        train.advanceInSection(train.getCruiseSpeedMps() * stepSeconds, ctx);

        if (train.getSectionProgressMeters() >= currentSection.getLengthMeters()) {
            train.resetSectionProgress();
            train.transitionState(TrainStateEnum.STOPPED_AT_STATION, nextStation.getName(), 0.0D, ctx);
            this.status = "SUCCESS";
        }
    }

    public String getStatus() {
        return status;
    }
}

package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.DetermineBothWaterLevelsProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.DetermineJunctionFlowProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FindAheadStationProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.GetStepDurationProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.api.LineWithTrainReadOnly;
import com.groma.models.emergencydeduction844676b1.service.IIndependentProcessService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 独立过程服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class IndependentProcessServiceImpl implements IIndependentProcessService {
    @Override
    public boolean findAheadStation(GromaWorldContext ctx,
            LineWithTrainReadOnly currentLineWithTrain, boolean found,
            LineWithStationEntity aheadLineWithStation) {
        // @GromaUserCode-START ["oHh6xrqSMTxFFZGd07W"]
        // 过程「查找前方车站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["oHh6xrqSMTxFFZGd07W"]

        return new FindAheadStationProcessTask(ctx, currentLineWithTrain, found, aheadLineWithStation).startTopLevel();
    }

    @Override
    public boolean determineBothWaterLevels(GromaWorldContext ctx, double firstSideFloorElevation,
            double firstSideWaterLevel, double firstSideDepth, double secondSideWaterLevel,
            double secondSideFloorElevation, double secondSideDepth) {
        // @GromaUserCode-START ["Pupqb6kGlHmKlcG6aaU"]
        // 过程「确定两端水位」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["Pupqb6kGlHmKlcG6aaU"]

        return new DetermineBothWaterLevelsProcessTask(ctx, firstSideFloorElevation, firstSideWaterLevel, firstSideDepth, secondSideWaterLevel, secondSideFloorElevation, secondSideDepth).startTopLevel();
    }

    @Override
    public boolean determineJunctionFlow(GromaWorldContext ctx, double flowControlElevation,
            double flowWidth, double firstSideWaterLevel, double currentFlowRate,
            double firstSideWaterVolume, boolean firstFlowsToSecond, double secondSideWaterVolume,
            double stepFlowVolume, double secondSideWaterLevel, long stepSeconds,
            double flowCoeff) {
        // @GromaUserCode-START ["TUmeBFJE9XAot7jIGeV"]
        // 过程「确定连接处过水情况」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["TUmeBFJE9XAot7jIGeV"]

        return new DetermineJunctionFlowProcessTask(ctx, flowControlElevation, flowWidth, firstSideWaterLevel, currentFlowRate, firstSideWaterVolume, firstFlowsToSecond, secondSideWaterVolume, stepFlowVolume, secondSideWaterLevel, stepSeconds, flowCoeff).startTopLevel();
    }

    @Override
    public GromaProcessTask getStepDuration(GromaWorldContext ctx, long stepSeconds) {
        // @GromaUserCode-START ["FUrqUDTrCHxy0I97epN"]
        // 过程「获取本步时长」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["FUrqUDTrCHxy0I97epN"]

        GromaProcessTask task = new GetStepDurationProcessTask(ctx, stepSeconds);
        ctx.launch(task);
        return task;
    }
}

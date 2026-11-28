package com.groma.models.emergencydeduction844676b1.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.api.LineWithTrainReadOnly;

/**
 * 独立过程服务接口：模型内共 4 个独立过程（无宿主对象，经由 engine 层 *ProcessTask 状态机执行）
 */
public interface IIndependentProcessService {
    /**
     * 查找前方车站（方圆独立过程原名：查找前方车站）—— 独立瞬时算子：同步执行完毕
     */
    boolean findAheadStation(GromaWorldContext ctx, LineWithTrainReadOnly currentLineWithTrain,
            boolean found, LineWithStationEntity aheadLineWithStation);

    /**
     * 确定两端水位（方圆独立过程原名：确定两端水位）—— 独立瞬时算子：同步执行完毕
     */
    boolean determineBothWaterLevels(GromaWorldContext ctx, double firstSideFloorElevation,
            double firstSideWaterLevel, double firstSideDepth, double secondSideWaterLevel,
            double secondSideFloorElevation, double secondSideDepth);

    /**
     * 确定连接处过水情况（方圆独立过程原名：确定连接处过水情况）—— 独立瞬时算子：同步执行完毕
     */
    boolean determineJunctionFlow(GromaWorldContext ctx, double flowControlElevation,
            double flowWidth, double firstSideWaterLevel, double currentFlowRate,
            double firstSideWaterVolume, boolean firstFlowsToSecond, double secondSideWaterVolume,
            double stepFlowVolume, double secondSideWaterLevel, long stepSeconds, double flowCoeff);

    /**
     * 获取本步时长（方圆独立过程原名：获取本步时长）—— 独立耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask getStepDuration(GromaWorldContext ctx, long stepSeconds);
}

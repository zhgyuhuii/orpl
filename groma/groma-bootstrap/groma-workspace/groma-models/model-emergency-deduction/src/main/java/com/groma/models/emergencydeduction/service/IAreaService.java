package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;

/**
 * 区域 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IAreaService {
    /**
     * 区域（方圆过程原名：积水变化）—— 瞬时算子：同步执行完毕
     */
    boolean waterloggingChange(GromaWorldContext ctx, AreaEntity self, double waterVolumeDelta);

    /**
     * 区域（方圆过程原名：地表排水）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask surfaceDrain(GromaWorldContext ctx, AreaEntity self);

    /**
     * 区域（方圆过程原名：计算降雨汇水）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask computeRainfallCatchment(GromaWorldContext ctx, AreaEntity self,
            double currentRainIntensity, double currentCatchmentFlow, double stepCatchmentVolume);

    /**
     * 区域（方圆过程原名：转换为积水状态）—— 瞬时算子：同步执行完毕
     */
    boolean toWaterloggedState(GromaWorldContext ctx, AreaEntity self);

    /**
     * 区域（方圆过程原名：转换为无积水状态）—— 瞬时算子：同步执行完毕
     */
    boolean toNoWaterloggingState(GromaWorldContext ctx, AreaEntity self);
}

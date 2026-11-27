package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;

/**
 * 车站 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IStationService {
    /**
     * 车站（方圆过程原名：入口过水）—— 瞬时算子：同步执行完毕
     */
    boolean inletFlow(GromaWorldContext ctx, StationEntity self, boolean flowsIntoStation,
            double areaWaterLevel, long stepSeconds, double currentFlowRate,
            double stationWaterLevel, double stepFlowVolume, double areaWaterVolume);

    /**
     * 车站（方圆过程原名：水流交换间隔）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask flowExchangeInterval(GromaWorldContext ctx, StationEntity self,
            long stepDuration);

    /**
     * 车站（方圆过程原名：站内水回流地面）—— 瞬时算子：同步执行完毕
     */
    boolean stationFlowBackSurface(GromaWorldContext ctx, StationEntity self, AreaEntity area,
            double stepFlowVolume);

    /**
     * 车站（方圆过程原名：积水变化）—— 瞬时算子：同步执行完毕
     */
    boolean waterloggingChange(GromaWorldContext ctx, StationEntity self, double waterVolumeDelta);

    /**
     * 车站（方圆过程原名：挡水墙承受水压）—— 瞬时算子：同步执行完毕
     */
    boolean floodWallBearPressure(GromaWorldContext ctx, StationEntity self, long stepSeconds,
            double areaWaterLevel);

    /**
     * 车站（方圆过程原名：区域与站内水流交换）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask areaStationFlowExchange(GromaWorldContext ctx, StationEntity self,
            double currentFlowRate, double stepFlowVolume, AreaEntity area);

    /**
     * 车站（方圆过程原名：地面水流入车站）—— 瞬时算子：同步执行完毕
     */
    boolean surfaceFlowIntoStation(GromaWorldContext ctx, StationEntity self, AreaEntity area,
            double stepFlowVolume);

    /**
     * 车站（方圆过程原名：挡水墙失效）—— 瞬时算子：同步执行完毕
     */
    boolean floodWallFailure(GromaWorldContext ctx, StationEntity self);

    /**
     * 车站（方圆过程原名：车站有积水）—— 瞬时算子：同步执行完毕
     */
    boolean stationHasWaterlogging(GromaWorldContext ctx, StationEntity self);

    /**
     * 车站（方圆过程原名：车站无积水）—— 瞬时算子：同步执行完毕
     */
    boolean stationNoWaterlogging(GromaWorldContext ctx, StationEntity self);
}

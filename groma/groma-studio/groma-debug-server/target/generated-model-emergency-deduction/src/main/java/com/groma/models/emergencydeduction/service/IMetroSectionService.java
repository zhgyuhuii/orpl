package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;

/**
 * 地铁区间 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IMetroSectionService {
    /**
     * 地铁区间（方圆过程原名：车站与区间水流交换）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask stationSectionFlowExchange(GromaWorldContext ctx, MetroSectionEntity self,
            double flowCoeff, double junctionFloorElevation, double stepFlowVolume,
            StationEntity endStation, double currentFlowRate, double effectiveFlowWidth);

    /**
     * 地铁区间（方圆过程原名：水深变化）—— 瞬时算子：同步执行完毕
     */
    boolean waterDepthChange(GromaWorldContext ctx, MetroSectionEntity self);

    /**
     * 地铁区间（方圆过程原名：水流交换间隔）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask flowExchangeInterval(GromaWorldContext ctx, MetroSectionEntity self,
            long stepSeconds);

    /**
     * 地铁区间（方圆过程原名：区间水回流车站）—— 瞬时算子：同步执行完毕
     */
    boolean sectionFlowBackStation(GromaWorldContext ctx, MetroSectionEntity self,
            StationEntity station, double stepFlowVolume);

    /**
     * 地铁区间（方圆过程原名：积水变化）—— 瞬时算子：同步执行完毕
     */
    boolean waterloggingChange(GromaWorldContext ctx, MetroSectionEntity self,
            double waterVolumeChange);

    /**
     * 地铁区间（方圆过程原名：排水）—— 瞬时算子：同步执行完毕
     */
    boolean drain(GromaWorldContext ctx, MetroSectionEntity self);

    /**
     * 地铁区间（方圆过程原名：车站水流入区间）—— 瞬时算子：同步执行完毕
     */
    boolean stationFlowIntoSection(GromaWorldContext ctx, MetroSectionEntity self,
            StationEntity station, double stepFlowVolume);

    /**
     * 地铁区间（方圆过程原名：区间无积水）—— 瞬时算子：同步执行完毕
     */
    boolean sectionNoWaterlogging(GromaWorldContext ctx, MetroSectionEntity self);

    /**
     * 地铁区间（方圆过程原名：区间积水）—— 瞬时算子：同步执行完毕
     */
    boolean sectionWaterlogging(GromaWorldContext ctx, MetroSectionEntity self);
}

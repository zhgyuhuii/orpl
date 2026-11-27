package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.RainfallEntity;
import java.lang.Double;
import java.lang.Long;
import java.util.Map;

/**
 * 降雨 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IRainfallService {
    /**
     * 降雨（方圆过程原名：获取雨量序列）—— 瞬时算子：同步执行完毕
     */
    boolean getRainfallSeries(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> newRainfallSeries, Map<Long, Double> originalRainfallSeries);

    /**
     * 降雨（方圆过程原名：降雨）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask rainfall(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> rainfallSeries, double currentRainIntensity,
            double cumulativeRainfall);

    /**
     * 降雨（方圆过程原名：过程1）—— 瞬时算子：同步执行完毕
     */
    boolean procedure1(GromaWorldContext ctx, RainfallEntity self, Map<Long, Double> text1);
}

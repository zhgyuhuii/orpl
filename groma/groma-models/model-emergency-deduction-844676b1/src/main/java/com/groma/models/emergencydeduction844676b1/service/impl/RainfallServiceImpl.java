package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.RainfallGetRainfallSeriesProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.RainfallProcedure1ProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.RainfallRainfallProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import com.groma.models.emergencydeduction844676b1.service.IRainfallService;
import java.lang.Double;
import java.lang.Long;
import java.lang.Override;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 降雨 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class RainfallServiceImpl implements IRainfallService {
    @Override
    public boolean getRainfallSeries(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> newRainfallSeries, Map<Long, Double> originalRainfallSeries) {
        // @GromaUserCode-START ["JzfewV01yPJs3e4QdtE"]
        // 过程「获取雨量序列」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["JzfewV01yPJs3e4QdtE"]

        return new RainfallGetRainfallSeriesProcessTask(ctx, self, newRainfallSeries, originalRainfallSeries).startTopLevel();
    }

    @Override
    public GromaProcessTask rainfall(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> rainfallSeries, double currentRainIntensity,
            double cumulativeRainfall) {
        // @GromaUserCode-START ["TvGbpspuGv5vJ1zt0qX"]
        // 过程「降雨」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["TvGbpspuGv5vJ1zt0qX"]

        GromaProcessTask task = new RainfallRainfallProcessTask(ctx, self, rainfallSeries, currentRainIntensity, cumulativeRainfall);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean procedure1(GromaWorldContext ctx, RainfallEntity self, Map<Long, Double> text1) {
        // @GromaUserCode-START ["3uWavwYkJfsqvNp2BuZ"]
        // 过程「过程1」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["3uWavwYkJfsqvNp2BuZ"]

        return new RainfallProcedure1ProcessTask(ctx, self, text1).startTopLevel();
    }
}

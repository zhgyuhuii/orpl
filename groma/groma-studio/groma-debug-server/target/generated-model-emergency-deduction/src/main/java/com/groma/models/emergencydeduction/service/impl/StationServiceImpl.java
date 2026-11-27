package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.StationAreaStationFlowExchangeProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationFloodWallBearPressureProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationFloodWallFailureProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationFlowExchangeIntervalProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationInletFlowProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationStationFlowBackSurfaceProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationStationHasWaterloggingProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationStationNoWaterloggingProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationSurfaceFlowIntoStationProcessTask;
import com.groma.models.emergencydeduction.engine.process.StationWaterloggingChangeProcessTask;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.service.IStationService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 车站 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class StationServiceImpl implements IStationService {
    @Override
    public boolean inletFlow(GromaWorldContext ctx, StationEntity self, boolean flowsIntoStation,
            double areaWaterLevel, long stepSeconds, double currentFlowRate,
            double stationWaterLevel, double stepFlowVolume, double areaWaterVolume) {
        // @GromaUserCode-START ["zlRqcFoeLHuEKONcV2V"]
        // 过程「入口过水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["zlRqcFoeLHuEKONcV2V"]

        return new StationInletFlowProcessTask(ctx, self, flowsIntoStation, areaWaterLevel, stepSeconds, currentFlowRate, stationWaterLevel, stepFlowVolume, areaWaterVolume).startTopLevel();
    }

    @Override
    public GromaProcessTask flowExchangeInterval(GromaWorldContext ctx, StationEntity self,
            long stepDuration) {
        // @GromaUserCode-START ["a9Ho9Zh9bCnkV8wVlfJ"]
        // 过程「水流交换间隔」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["a9Ho9Zh9bCnkV8wVlfJ"]

        GromaProcessTask task = new StationFlowExchangeIntervalProcessTask(ctx, self, stepDuration);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean stationFlowBackSurface(GromaWorldContext ctx, StationEntity self,
            AreaEntity area, double stepFlowVolume) {
        // @GromaUserCode-START ["KPVmSbBvALXOLKuleXT"]
        // 过程「站内水回流地面」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["KPVmSbBvALXOLKuleXT"]

        return new StationStationFlowBackSurfaceProcessTask(ctx, self, area, stepFlowVolume).startTopLevel();
    }

    @Override
    public boolean waterloggingChange(GromaWorldContext ctx, StationEntity self,
            double waterVolumeDelta) {
        // @GromaUserCode-START ["uK01gkyWkOsTbw8coIE"]
        // 过程「积水变化」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["uK01gkyWkOsTbw8coIE"]

        return new StationWaterloggingChangeProcessTask(ctx, self, waterVolumeDelta).startTopLevel();
    }

    @Override
    public boolean floodWallBearPressure(GromaWorldContext ctx, StationEntity self,
            long stepSeconds, double areaWaterLevel) {
        // @GromaUserCode-START ["6gimbvYC5CskuHCzVFQ"]
        // 过程「挡水墙承受水压」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["6gimbvYC5CskuHCzVFQ"]

        return new StationFloodWallBearPressureProcessTask(ctx, self, stepSeconds, areaWaterLevel).startTopLevel();
    }

    @Override
    public GromaProcessTask areaStationFlowExchange(GromaWorldContext ctx, StationEntity self,
            double currentFlowRate, double stepFlowVolume, AreaEntity area) {
        // @GromaUserCode-START ["mXoawsnL7qgqcwNmvSw"]
        // 过程「区域与站内水流交换」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["mXoawsnL7qgqcwNmvSw"]

        GromaProcessTask task = new StationAreaStationFlowExchangeProcessTask(ctx, self, currentFlowRate, stepFlowVolume, area);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean surfaceFlowIntoStation(GromaWorldContext ctx, StationEntity self,
            AreaEntity area, double stepFlowVolume) {
        // @GromaUserCode-START ["d890lEbZtQNlk7z52Xa"]
        // 过程「地面水流入车站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["d890lEbZtQNlk7z52Xa"]

        return new StationSurfaceFlowIntoStationProcessTask(ctx, self, area, stepFlowVolume).startTopLevel();
    }

    @Override
    public boolean floodWallFailure(GromaWorldContext ctx, StationEntity self) {
        // @GromaUserCode-START ["FylD7oTkdptLzbz6ZeT"]
        // 过程「挡水墙失效」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["FylD7oTkdptLzbz6ZeT"]

        return new StationFloodWallFailureProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean stationHasWaterlogging(GromaWorldContext ctx, StationEntity self) {
        // @GromaUserCode-START ["IH42eYNsj0haPC4ctyQ"]
        // 过程「车站有积水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["IH42eYNsj0haPC4ctyQ"]

        return new StationStationHasWaterloggingProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean stationNoWaterlogging(GromaWorldContext ctx, StationEntity self) {
        // @GromaUserCode-START ["fHdltixbuKnXUv5r9F6"]
        // 过程「车站无积水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["fHdltixbuKnXUv5r9F6"]

        return new StationStationNoWaterloggingProcessTask(ctx, self).startTopLevel();
    }
}

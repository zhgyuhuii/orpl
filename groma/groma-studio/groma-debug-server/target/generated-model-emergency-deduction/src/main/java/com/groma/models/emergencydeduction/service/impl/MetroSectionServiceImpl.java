package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.MetroSectionDrainProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionFlowExchangeIntervalProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionSectionFlowBackStationProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionSectionNoWaterloggingProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionSectionWaterloggingProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionStationFlowIntoSectionProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionStationSectionFlowExchangeProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionWaterDepthChangeProcessTask;
import com.groma.models.emergencydeduction.engine.process.MetroSectionWaterloggingChangeProcessTask;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.service.IMetroSectionService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 地铁区间 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class MetroSectionServiceImpl implements IMetroSectionService {
    @Override
    public GromaProcessTask stationSectionFlowExchange(GromaWorldContext ctx,
            MetroSectionEntity self, double flowCoeff, double junctionFloorElevation,
            double stepFlowVolume, StationEntity endStation, double currentFlowRate,
            double effectiveFlowWidth) {
        // @GromaUserCode-START ["HOI0UpBGLHJoNJjKYum"]
        // 过程「车站与区间水流交换」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["HOI0UpBGLHJoNJjKYum"]

        GromaProcessTask task = new MetroSectionStationSectionFlowExchangeProcessTask(ctx, self, flowCoeff, junctionFloorElevation, stepFlowVolume, endStation, currentFlowRate, effectiveFlowWidth);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean waterDepthChange(GromaWorldContext ctx, MetroSectionEntity self) {
        // @GromaUserCode-START ["d3UYEBS88YPu8FoGckq"]
        // 过程「水深变化」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["d3UYEBS88YPu8FoGckq"]

        return new MetroSectionWaterDepthChangeProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public GromaProcessTask flowExchangeInterval(GromaWorldContext ctx, MetroSectionEntity self,
            long stepSeconds) {
        // @GromaUserCode-START ["6qnqt4O6vaFhEkh96tN"]
        // 过程「水流交换间隔」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["6qnqt4O6vaFhEkh96tN"]

        GromaProcessTask task = new MetroSectionFlowExchangeIntervalProcessTask(ctx, self, stepSeconds);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean sectionFlowBackStation(GromaWorldContext ctx, MetroSectionEntity self,
            StationEntity station, double stepFlowVolume) {
        // @GromaUserCode-START ["Sq99s0zjy6vmnjVSRMz"]
        // 过程「区间水回流车站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["Sq99s0zjy6vmnjVSRMz"]

        return new MetroSectionSectionFlowBackStationProcessTask(ctx, self, station, stepFlowVolume).startTopLevel();
    }

    @Override
    public boolean waterloggingChange(GromaWorldContext ctx, MetroSectionEntity self,
            double waterVolumeChange) {
        // @GromaUserCode-START ["RDFt6UgNzfxWfBN4FsT"]
        // 过程「积水变化」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["RDFt6UgNzfxWfBN4FsT"]

        return new MetroSectionWaterloggingChangeProcessTask(ctx, self, waterVolumeChange).startTopLevel();
    }

    @Override
    public boolean drain(GromaWorldContext ctx, MetroSectionEntity self) {
        // @GromaUserCode-START ["QzcUew3b1XLCXLK6rIH"]
        // 过程「排水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["QzcUew3b1XLCXLK6rIH"]

        return new MetroSectionDrainProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean stationFlowIntoSection(GromaWorldContext ctx, MetroSectionEntity self,
            StationEntity station, double stepFlowVolume) {
        // @GromaUserCode-START ["CfcyX4WesZ6Y5r66Caw"]
        // 过程「车站水流入区间」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["CfcyX4WesZ6Y5r66Caw"]

        return new MetroSectionStationFlowIntoSectionProcessTask(ctx, self, station, stepFlowVolume).startTopLevel();
    }

    @Override
    public boolean sectionNoWaterlogging(GromaWorldContext ctx, MetroSectionEntity self) {
        // @GromaUserCode-START ["AqYEeNHBfq4AjItqD8C"]
        // 过程「区间无积水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["AqYEeNHBfq4AjItqD8C"]

        return new MetroSectionSectionNoWaterloggingProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean sectionWaterlogging(GromaWorldContext ctx, MetroSectionEntity self) {
        // @GromaUserCode-START ["mhBBbVgeJS6lgSxIXKU"]
        // 过程「区间积水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["mhBBbVgeJS6lgSxIXKU"]

        return new MetroSectionSectionWaterloggingProcessTask(ctx, self).startTopLevel();
    }
}

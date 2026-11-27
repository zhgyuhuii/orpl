package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.AreaComputeRainfallCatchmentProcessTask;
import com.groma.models.emergencydeduction.engine.process.AreaSurfaceDrainProcessTask;
import com.groma.models.emergencydeduction.engine.process.AreaToNoWaterloggingStateProcessTask;
import com.groma.models.emergencydeduction.engine.process.AreaToWaterloggedStateProcessTask;
import com.groma.models.emergencydeduction.engine.process.AreaWaterloggingChangeProcessTask;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.service.IAreaService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 区域 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class AreaServiceImpl implements IAreaService {
    @Override
    public boolean waterloggingChange(GromaWorldContext ctx, AreaEntity self,
            double waterVolumeDelta) {
        // @GromaUserCode-START ["E5B8xzaV9xRnt2LDEgH"]
        // 过程「积水变化」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["E5B8xzaV9xRnt2LDEgH"]

        return new AreaWaterloggingChangeProcessTask(ctx, self, waterVolumeDelta).startTopLevel();
    }

    @Override
    public GromaProcessTask surfaceDrain(GromaWorldContext ctx, AreaEntity self) {
        // @GromaUserCode-START ["Une96ICnH2KER5gu7LT"]
        // 过程「地表排水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["Une96ICnH2KER5gu7LT"]

        GromaProcessTask task = new AreaSurfaceDrainProcessTask(ctx, self);
        ctx.launch(task);
        return task;
    }

    @Override
    public GromaProcessTask computeRainfallCatchment(GromaWorldContext ctx, AreaEntity self,
            double currentRainIntensity, double currentCatchmentFlow, double stepCatchmentVolume) {
        // @GromaUserCode-START ["QZPPbXby5BPfgS5sziq"]
        // 过程「计算降雨汇水」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["QZPPbXby5BPfgS5sziq"]

        GromaProcessTask task = new AreaComputeRainfallCatchmentProcessTask(ctx, self, currentRainIntensity, currentCatchmentFlow, stepCatchmentVolume);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean toWaterloggedState(GromaWorldContext ctx, AreaEntity self) {
        // @GromaUserCode-START ["9W9vzQHklaUg4QG70Vc"]
        // 过程「转换为积水状态」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["9W9vzQHklaUg4QG70Vc"]

        return new AreaToWaterloggedStateProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean toNoWaterloggingState(GromaWorldContext ctx, AreaEntity self) {
        // @GromaUserCode-START ["NdLStqTVimDD7Npig19"]
        // 过程「转换为无积水状态」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["NdLStqTVimDD7Npig19"]

        return new AreaToNoWaterloggingStateProcessTask(ctx, self).startTopLevel();
    }
}

package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.PassengerGroupRecordDangerExposureProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PassengerGroupUpdateEvacuationProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PassengerGroupUpdateTrappedProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction844676b1.service.IPassengerGroupService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 乘客群 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class PassengerGroupServiceImpl implements IPassengerGroupService {
    @Override
    public boolean recordDangerExposure(GromaWorldContext ctx, PassengerGroupEntity self) {
        // @GromaUserCode-START ["XUr2ph0kb7en26PZ8gp"]
        // 过程「记录危险暴露」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["XUr2ph0kb7en26PZ8gp"]

        return new PassengerGroupRecordDangerExposureProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean updateEvacuation(GromaWorldContext ctx, PassengerGroupEntity self) {
        // @GromaUserCode-START ["azSevtqA1GFz9T7YjDq"]
        // 过程「更新疏散情况」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["azSevtqA1GFz9T7YjDq"]

        return new PassengerGroupUpdateEvacuationProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean updateTrapped(GromaWorldContext ctx, PassengerGroupEntity self) {
        // @GromaUserCode-START ["nVjHf2KrmdIijCrca8a"]
        // 过程「更新受困情况」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["nVjHf2KrmdIijCrca8a"]

        return new PassengerGroupUpdateTrappedProcessTask(ctx, self).startTopLevel();
    }
}

package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.PlanExecutorExecutePlanProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.PlanExecutorEntity;
import com.groma.models.emergencydeduction844676b1.service.IPlanExecutorService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 方案执行器 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class PlanExecutorServiceImpl implements IPlanExecutorService {
    @Override
    public boolean executePlan(GromaWorldContext ctx, PlanExecutorEntity self) {
        // @GromaUserCode-START ["jtIxDyIRLVxMZw7Tq35"]
        // 过程「执行方案」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["jtIxDyIRLVxMZw7Tq35"]

        return new PlanExecutorExecutePlanProcessTask(ctx, self).startTopLevel();
    }
}

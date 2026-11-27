package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.PlanGeneratorComputeDisposalTimeProcessTask;
import com.groma.models.emergencydeduction.engine.process.PlanGeneratorFormCandidateResponsePlanProcessTask;
import com.groma.models.emergencydeduction.model.entity.PlanGeneratorEntity;
import com.groma.models.emergencydeduction.service.IPlanGeneratorService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 方案生成器 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class PlanGeneratorServiceImpl implements IPlanGeneratorService {
    @Override
    public boolean computeDisposalTime(GromaWorldContext ctx, PlanGeneratorEntity self) {
        // @GromaUserCode-START ["IJT778zU1zIw6nc7uYe"]
        // 过程「计算处置时间」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["IJT778zU1zIw6nc7uYe"]

        return new PlanGeneratorComputeDisposalTimeProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean formCandidateResponsePlan(GromaWorldContext ctx, PlanGeneratorEntity self) {
        // @GromaUserCode-START ["UkKv7XkK4ASpaf1Bnit"]
        // 过程「形成备选响应方案」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["UkKv7XkK4ASpaf1Bnit"]

        return new PlanGeneratorFormCandidateResponsePlanProcessTask(ctx, self).startTopLevel();
    }
}

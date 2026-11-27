package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.PlanGeneratorEntity;

/**
 * 方案生成器 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IPlanGeneratorService {
    /**
     * 方案生成器（方圆过程原名：计算处置时间）—— 瞬时算子：同步执行完毕
     */
    boolean computeDisposalTime(GromaWorldContext ctx, PlanGeneratorEntity self);

    /**
     * 方案生成器（方圆过程原名：形成备选响应方案）—— 瞬时算子：同步执行完毕
     */
    boolean formCandidateResponsePlan(GromaWorldContext ctx, PlanGeneratorEntity self);
}

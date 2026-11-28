package com.groma.models.emergencydeduction844676b1.service;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.PlanExecutorEntity;

/**
 * 方案执行器 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IPlanExecutorService {
    /**
     * 方案执行器（方圆过程原名：执行方案）—— 瞬时算子：同步执行完毕
     */
    boolean executePlan(GromaWorldContext ctx, PlanExecutorEntity self);
}

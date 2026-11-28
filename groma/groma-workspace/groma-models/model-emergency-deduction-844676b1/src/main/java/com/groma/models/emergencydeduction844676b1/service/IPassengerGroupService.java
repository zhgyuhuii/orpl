package com.groma.models.emergencydeduction844676b1.service;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.PassengerGroupEntity;

/**
 * 乘客群 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IPassengerGroupService {
    /**
     * 乘客群（方圆过程原名：记录危险暴露）—— 瞬时算子：同步执行完毕
     */
    boolean recordDangerExposure(GromaWorldContext ctx, PassengerGroupEntity self);

    /**
     * 乘客群（方圆过程原名：更新疏散情况）—— 瞬时算子：同步执行完毕
     */
    boolean updateEvacuation(GromaWorldContext ctx, PassengerGroupEntity self);

    /**
     * 乘客群（方圆过程原名：更新受困情况）—— 瞬时算子：同步执行完毕
     */
    boolean updateTrapped(GromaWorldContext ctx, PassengerGroupEntity self);
}

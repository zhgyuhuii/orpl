package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;

/**
 * 场景1 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IScene1Service {
    /**
     * 场景1（方圆过程原名：2把否变成是）—— 瞬时算子：同步执行完毕
     */
    boolean turnNoIntoYes2(GromaWorldContext ctx, Scene1Entity self, boolean text1);

    /**
     * 场景1（方圆过程原名：4生成对象并把是变成否）—— 瞬时算子：同步执行完毕
     */
    boolean createObjectTurnYesIntoNo4(GromaWorldContext ctx, Scene1Entity self, boolean text1);

    /**
     * 场景1（方圆过程原名：3转换为行驶）—— 瞬时算子：同步执行完毕
     */
    boolean convertToRun3(GromaWorldContext ctx, Scene1Entity self);

    /**
     * 场景1（方圆过程原名：1转换为停站）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask convertToStop1(GromaWorldContext ctx, Scene1Entity self);

    /**
     * 场景1（方圆过程原名：过程1）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask procedure1(GromaWorldContext ctx, Scene1Entity self);

    /**
     * 场景1（方圆过程原名：过程1）—— 瞬时算子：同步执行完毕
     */
    boolean procedure12(GromaWorldContext ctx, Scene1Entity self);
}

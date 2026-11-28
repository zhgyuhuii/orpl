package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.DisposalPlanEntity;
import com.groma.models.emergencydeduction844676b1.model.enums.TaskStateEnum;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取方案执行器，编译期禁止误调写 Setter
 */
public interface PlanExecutorReadOnly {
    String getInstanceId();

    /**
     * 在行方案
     */
    DisposalPlanEntity getActivePlan();

    /**
     * 当前方案阶段
     */
    TaskStateEnum getCurrentPlanStage();

    /**
     * 实际下达时刻
     */
    double getActualIssueTime();

    /**
     * 实际生效时刻
     */
    double getActualEffectiveTime();
}

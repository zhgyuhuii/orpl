package com.groma.models.emergencydeduction.model.vo;

import com.groma.models.emergencydeduction.model.enums.TaskStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：方案执行器）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "方案执行器"
)
public record PlanExecutorVO(String instanceId, boolean alive, String activePlan,
        TaskStateEnum currentPlanStage, double actualIssueTime, double actualEffectiveTime) {
}

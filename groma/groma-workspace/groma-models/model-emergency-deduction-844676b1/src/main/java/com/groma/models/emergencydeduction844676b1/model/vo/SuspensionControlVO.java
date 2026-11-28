package com.groma.models.emergencydeduction844676b1.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;
import java.util.List;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：停运控制）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "停运控制"
)
public record SuspensionControlVO(String instanceId, boolean alive, String decisionPlan,
        List<String> controlSection) {
}

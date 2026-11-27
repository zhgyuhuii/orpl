package com.groma.models.emergencydeduction.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;
import java.util.List;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：挡水防护）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "挡水防护"
)
public record FloodProtectionVO(String instanceId, boolean alive, List<String> protectiveFloodWall,
        String protectedSection) {
}

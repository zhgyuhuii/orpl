package com.groma.models.emergencydeduction844676b1.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;
import java.util.List;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：地理路线）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "地理路线"
)
public record GeoRouteVO(String instanceId, boolean alive, List<String> coordinateGroup) {
}

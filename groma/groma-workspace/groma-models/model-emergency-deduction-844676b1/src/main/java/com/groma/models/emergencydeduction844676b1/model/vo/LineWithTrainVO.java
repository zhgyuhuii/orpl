package com.groma.models.emergencydeduction844676b1.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：线路与列车）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "线路与列车"
)
public record LineWithTrainVO(String instanceId, boolean alive, String line, String train,
        double trainMileageOnLine, boolean alongIncreasingMileage, String currentParkedRelation,
        String currentRunningRelation, boolean demoRoundTrip) {
}

package com.groma.models.emergencydeduction.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：区间与区间内列车）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "区间与区间内列车"
)
public record SectionWithTrainVO(String instanceId, boolean alive, String train, String section,
        double travelledDistance, String targetStation, String departureStation,
        boolean arrivedTargetStation) {
}

package com.groma.models.emergencydeduction844676b1.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：车站与停靠列车）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "车站与停靠列车"
)
public record StationWithParkedTrainVO(String instanceId, boolean alive, String train,
        String station, long actualDepartureTime, long actualArrivalTime,
        long prescribedStopDuration, boolean departureAllowed) {
}

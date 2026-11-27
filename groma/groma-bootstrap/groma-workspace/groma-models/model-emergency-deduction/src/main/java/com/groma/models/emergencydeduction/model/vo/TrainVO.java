package com.groma.models.emergencydeduction.model.vo;

import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：列车）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "列车"
)
public record TrainVO(String instanceId, boolean alive, double currentSpeed, double forcedStopTime,
        String initialPosition, String name, TrainStateEnum trainState, double direction,
        String currentPosition, double travelSpeed) {
}

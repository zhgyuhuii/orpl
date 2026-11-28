package com.groma.models.emergencydeduction844676b1.model.vo;

import com.groma.models.emergencydeduction844676b1.model.enums.EvacuationStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：车站与站内乘客群）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "车站与站内乘客群"
)
public record StationWithPassengerGroupVO(String instanceId, boolean alive, String passengerGroup,
        String station, long currentStationPeopleCount, long evacuatedPeopleCount,
        boolean evacuationStarted, EvacuationStateEnum evacuationState) {
}

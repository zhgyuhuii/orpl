package com.groma.models.emergencydeduction844676b1.model.vo;

import com.groma.models.emergencydeduction844676b1.model.enums.StationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：线路与车站）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "线路与车站"
)
public record LineWithStationVO(String instanceId, boolean alive, String metroLine, String station,
        double lineMileage, StationTypeEnum stationType, long stationNo) {
}

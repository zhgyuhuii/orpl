package com.groma.models.emergencydeduction844676b1.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：区域与区域内车站）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "区域与区域内车站"
)
public record AreaWithStationVO(String instanceId, boolean alive, String area, String station,
        double currentFlowRate, double stepFlowVolume) {
}

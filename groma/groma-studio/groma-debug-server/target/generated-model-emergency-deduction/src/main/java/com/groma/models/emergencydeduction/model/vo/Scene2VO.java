package com.groma.models.emergencydeduction.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：场景2）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "场景2"
)
public record Scene2VO(String instanceId, boolean alive, String rainfallRelatedArea,
        String areaWithStation, String sectionWithEndStation, String lineWithTrain) {
}

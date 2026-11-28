package com.groma.models.emergencydeduction844676b1.model.vo;

import com.groma.models.emergencydeduction844676b1.model.enums.TrainStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.String;

/**
 * 对外展示不可变视图 VO（方圆对象类型原名：场景1）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用
 */
@Schema(
        title = "场景1"
)
public record Scene1VO(String instanceId, boolean alive, TrainStateEnum trainState1, boolean text1,
        String text2) {
}

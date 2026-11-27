package com.groma.models.emergencydeduction.utils;

import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;

/**
 * 线上逻辑判断工具（类型 / 场景设计视图里连线上的线上判断，原生比较、零表达式引擎开销）
 */
public final class ConditionJudgeUtil {
    private ConditionJudgeUtil() {
    }

    /**
     * 线上判断：线路与列车 上的 离站是否允许 = true
     */
    public static boolean judge9(GromaWorldContext c, BaseSimEntity context) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        return contextEntity.getCurrentParkedRelation().isDepartureAllowed();
    }

    /**
     * 线上判断：线路与列车 上的 是否到达目标车站 = true
     */
    public static boolean judge11(GromaWorldContext c, BaseSimEntity context) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        return contextEntity.getCurrentRunningRelation().isArrivedTargetStation();
    }

    /**
     * 线上判断：场景1 上的 文本1 = true
     */
    public static boolean judge18(GromaWorldContext c, BaseSimEntity context) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        return contextEntity.isText1();
    }
}

package com.groma.models.emergencydeduction844676b1.service;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationWithParkedTrainEntity;

/**
 * 线路与列车 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface ILineWithTrainService {
    /**
     * 线路与列车（方圆过程原名：形成车站与列车的关联）—— 瞬时算子：同步执行完毕
     */
    boolean formStationTrainRelation(GromaWorldContext ctx, LineWithTrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain);

    /**
     * 线路与列车（方圆过程原名：形成区间与列车的关联）—— 瞬时算子：同步执行完毕
     */
    boolean formSectionTrainRelation(GromaWorldContext ctx, LineWithTrainEntity self,
            SectionWithTrainEntity sectionWithTrain);

    /**
     * 线路与列车（方圆过程原名：创建关联）—— 瞬时算子：同步执行完毕
     */
    boolean createRelation(GromaWorldContext ctx, LineWithTrainEntity self);

    /**
     * 线路与列车（方圆过程原名：生成关联）—— 瞬时算子：同步执行完毕
     */
    boolean generateRelation(GromaWorldContext ctx, LineWithTrainEntity self);
}

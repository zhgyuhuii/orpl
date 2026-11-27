package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.entity.api.MetroSectionReadOnly;
import com.groma.models.emergencydeduction.model.entity.api.StationReadOnly;

/**
 * 列车 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface ITrainService {
    /**
     * 列车（方圆过程原名：沿区间前进）—— 瞬时算子：同步执行完毕
     */
    boolean advanceAlongSection(GromaWorldContext ctx, TrainEntity self,
            MetroSectionReadOnly metroSection, StationReadOnly targetStation, double travelInterval,
            double travelledDistance, boolean alongIncreasingMileage, double currentLineMileage);

    /**
     * 列车（方圆过程原名：离站）—— 瞬时算子：同步执行完毕
     */
    boolean depart(GromaWorldContext ctx, TrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain);

    /**
     * 列车（方圆过程原名：区间行驶）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask sectionRun(GromaWorldContext ctx, TrainEntity self,
            SectionWithTrainEntity sectionWithTrain, double currentLineMileage,
            boolean alongIncreasingMileage);

    /**
     * 列车（方圆过程原名：停站）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask stoppedAtStation(GromaWorldContext ctx, TrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain);

    /**
     * 列车（方圆过程原名：进站）—— 瞬时算子：同步执行完毕
     */
    boolean enterStation(GromaWorldContext ctx, TrainEntity self);
}

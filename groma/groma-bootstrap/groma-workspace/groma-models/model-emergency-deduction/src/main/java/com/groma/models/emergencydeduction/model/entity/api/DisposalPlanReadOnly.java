package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取处置方案，编译期禁止误调写 Setter
 */
public interface DisposalPlanReadOnly {
    String getInstanceId();

    /**
     * 方案编号
     */
    String getPlanCode();

    /**
     * 方案名称
     */
    String getPlanName();

    /**
     * 关联列车
     */
    TrainEntity getAssociatedTrain();

    /**
     * 列车通过所需时间
     */
    double getTrainPassTime();

    /**
     * 停运下达时刻
     */
    double getSuspensionIssueTime();

    /**
     * 停止放行时刻
     */
    double getReleaseStopTime();
}

package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取停运控制，编译期禁止误调写 Setter
 */
public interface SuspensionControlReadOnly {
    String getInstanceId();

    /**
     * 决策方案
     */
    DisposalPlanEntity getDecisionPlan();

    /**
     * 控制区间
     */
    List<MetroSectionEntity> getControlSection();
}

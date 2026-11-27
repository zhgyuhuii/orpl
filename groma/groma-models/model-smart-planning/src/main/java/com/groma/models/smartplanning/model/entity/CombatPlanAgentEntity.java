package com.groma.models.smartplanning.model.entity;

import com.groma.base.common.annotation.GromaMeta;

/**
 * 作战计划生成智能体（对应真实模型 TYPE: eid=1vdnTrvC00DtCevXlXo）
 */
@GromaMeta(id = "1vdnTrvC00DtCevXlXo", zhName = "作战计划生成智能体", kind = "TYPE")
public class CombatPlanAgentEntity {

    private String planId = "";
    private String generatedPlanSummary = "";
    private boolean persisted = false;

    public void recordGeneratedPlan(String planId, String summary) {
        this.planId = planId;
        this.generatedPlanSummary = summary;
        this.persisted = true;
    }

    public String getPlanId() { return planId; }
    public String getGeneratedPlanSummary() { return generatedPlanSummary; }
    public boolean isPersisted() { return persisted; }
}

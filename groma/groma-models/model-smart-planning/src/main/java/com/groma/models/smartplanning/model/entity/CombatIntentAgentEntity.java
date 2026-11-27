package com.groma.models.smartplanning.model.entity;

import com.groma.base.common.annotation.GromaMeta;

/**
 * 作战意图分析智能体（对应真实模型 TYPE: eid=dLimNZxy9VdqhZERI3L）
 */
@GromaMeta(id = "dLimNZxy9VdqhZERI3L", zhName = "作战意图分析智能体", kind = "TYPE")
public class CombatIntentAgentEntity {

    private String rawIntentText = "";
    private String constraintsText = "";
    private String analyzedIntentResult = "";

    public void updateAnalysis(String rawIntent, String constraints, String structuredResult) {
        this.rawIntentText = rawIntent;
        this.constraintsText = constraints;
        this.analyzedIntentResult = structuredResult;
    }

    public String getRawIntentText() { return rawIntentText; }
    public String getConstraintsText() { return constraintsText; }
    public String getAnalyzedIntentResult() { return analyzedIntentResult; }
}

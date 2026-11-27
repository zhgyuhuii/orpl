package com.groma.models.smartplanning.model.entity;

import com.groma.base.common.annotation.GromaMeta;

/**
 * 想定配置生成智能体（对应真实模型 TYPE: eid=DU2rsnXC44GZfqegJXF）
 */
@GromaMeta(id = "DU2rsnXC44GZfqegJXF", zhName = "想定配置生成智能体", kind = "TYPE")
public class ScenarioConfigAgentEntity {

    private String scenarioConfigJson = "{}";

    public void setScenarioConfigJson(String scenarioConfigJson) {
        this.scenarioConfigJson = scenarioConfigJson;
    }

    public String getScenarioConfigJson() { return scenarioConfigJson; }
}

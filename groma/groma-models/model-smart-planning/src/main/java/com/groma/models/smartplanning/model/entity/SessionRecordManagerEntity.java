package com.groma.models.smartplanning.model.entity;

import com.groma.base.common.annotation.GromaMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话记录管理实体（对应真实模型 TYPE: 会话记录管理, eid=0effeb2c4c6743fab5ddc2c605e99bd8）
 */
@GromaMeta(id = "0effeb2c4c6743fab5ddc2c605e99bd8", zhName = "会话记录管理", kind = "TYPE")
public class SessionRecordManagerEntity {

    public record StepLogItem(int seq, String stageName, String content, boolean filtered) {}

    private final String sessionId;
    private final List<StepLogItem> records = new ArrayList<>(16);
    private String serializedSessionJson = "[]";
    private boolean completed = false;

    public SessionRecordManagerEntity(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 对应真实模型 TEMPLATE-PROCESS《过滤数据类型》：包含 request/result/done 时标记过滤
     */
    public void appendRecord(String stageName, String dataType, String content) {
        boolean shouldFilter = dataType != null
                && (dataType.contains("request") || dataType.contains("result") || dataType.contains("done"));
        records.add(new StepLogItem(records.size() + 1, stageName, content, shouldFilter));
    }

    public void markCompleted(String jsonSummary) {
        this.completed = true;
        this.serializedSessionJson = jsonSummary;
    }

    public String getSessionId() { return sessionId; }
    public List<StepLogItem> getRecords() { return Collections.unmodifiableList(records); }
    public String getSerializedSessionJson() { return serializedSessionJson; }
    public boolean isCompleted() { return completed; }
}

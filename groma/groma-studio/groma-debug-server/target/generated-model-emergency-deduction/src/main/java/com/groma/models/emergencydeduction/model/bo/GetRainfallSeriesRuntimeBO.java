package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：获取雨量序列）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "JzfewV01yPJs3e4QdtE#bo",
        zhName = "获取雨量序列运行帧",
        kind = "RUNTIME_BO"
)
public final class GetRainfallSeriesRuntimeBO {
    /**
     * 节点输出：键值对转文本
     */
    private String out1;

    /**
     * 节点输出：键值对转文本
     */
    public String getOut1() {
        return this.out1;
    }

    public void setOut1(String value) {
        this.out1 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("out1", GromaValues.encode(this.out1));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.out1 = state.get("out1") == null ? null : GromaValues.toText(state.get("out1"));
    }
}

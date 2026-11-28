package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：积水变化）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "RDFt6UgNzfxWfBN4FsT#bo",
        zhName = "积水变化运行帧",
        kind = "RUNTIME_BO"
)
public final class WaterloggingChangeRuntimeBO3 {
    /**
     * 节点输出：加
     */
    private Double out0;

    /**
     * 节点输出：取大
     */
    private Double out1;

    /**
     * 节点输出：加
     */
    public Double getOut0() {
        return this.out0;
    }

    public void setOut0(Double value) {
        this.out0 = value;
    }

    /**
     * 节点输出：取大
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("out0", GromaValues.encode(this.out0));
        state.put("out1", GromaValues.encode(this.out1));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.out0 = (Double) GromaValues.decode(state.get("out0"), ctx);
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
    }
}

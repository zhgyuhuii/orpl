package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：地面水流入车站）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "d890lEbZtQNlk7z52Xa#bo",
        zhName = "地面水流入车站运行帧",
        kind = "RUNTIME_BO"
)
public final class SurfaceFlowIntoStationRuntimeBO {
    /**
     * 节点输出：取相反数
     */
    private Double out1;

    /**
     * 节点输出：取相反数
     */
    public Double getOut1() {
        return this.out1;
    }

    public void setOut1(Double value) {
        this.out1 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("out1", GromaValues.encode(this.out1));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.out1 = (Double) GromaValues.decode(state.get("out1"), ctx);
    }
}

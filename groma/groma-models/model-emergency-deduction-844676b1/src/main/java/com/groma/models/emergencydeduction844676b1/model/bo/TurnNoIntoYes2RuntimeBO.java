package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：2把否变成是）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "ctET0FcFZxktbhbbd9j#bo",
        zhName = "2把否变成是运行帧",
        kind = "RUNTIME_BO"
)
public final class TurnNoIntoYes2RuntimeBO {
    /**
     * 节点输出：赋值
     */
    private Boolean out0;

    /**
     * 节点输出：赋值
     */
    public Boolean getOut0() {
        return this.out0;
    }

    public void setOut0(Boolean value) {
        this.out0 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("out0", GromaValues.encode(this.out0));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.out0 = (Boolean) GromaValues.decode(state.get("out0"), ctx);
    }
}

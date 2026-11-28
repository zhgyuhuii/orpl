package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：过程1（方圆过程原名：过程1；宿主对象：降雨）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "3uWavwYkJfsqvNp2BuZ",
        zhName = "过程1",
        kind = "PROCESS_TASK"
)
public final class RainfallProcedure1ProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：降雨）
     */
    private final RainfallEntity self;

    /**
     * 非空输入形参：文本1
     */
    private Map<Long, Double> text1;

    public RainfallProcedure1ProcessTask(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> text1) {
        super(ctx, self, "ent:3uWavwYkJfsqvNp2BuZ", "过程1", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 过程1");
        this.self = self;
        this.text1 = text1;
    }

    private void setText1(Map<Long, Double> value) {
        this.text1 = value;
    }

    /**
     * 非空输入形参：文本1
     */
    public Map<Long, Double> getText1() {
        return this.text1;
    }

    @Override
    protected boolean hasFlow() {
        return false;
    }

    @Override
    protected int[] entryNodes() {
        return new int[0];
    }

    @Override
    protected void execLeafBody() {
        // 以下代码由画布过程代码框转译（中文引用已无损重写为英文访问）
        // --- 编译转译自画布过程代码 (原始行数: 1) ---
        com.groma.base.engine.BuiltinTools.clearCollection(this.text1);
    }

    @Override
    protected boolean execNode(int pc) {
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        if (pc == -1) return;
    }

    @Override
    protected boolean canStart() {
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("text1", GromaValues.encode(this.text1));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.text1 = (Map<Long, Double>) GromaValues.decode(slots.get("text1"), ctx);
    }
}

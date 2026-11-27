package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.GetRainfallSeriesRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.RainfallEntity;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：获取雨量序列（方圆过程原名：获取雨量序列；宿主对象：降雨）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "JzfewV01yPJs3e4QdtE",
        zhName = "获取雨量序列",
        kind = "PROCESS_TASK"
)
public final class RainfallGetRainfallSeriesProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：降雨）
     */
    private final RainfallEntity self;

    /**
     * 非空输入形参：雨量序列新
     */
    private Map<Long, Double> newRainfallSeries;

    /**
     * 非空输入形参：原雨量序列
     */
    private Map<Long, Double> originalRainfallSeries;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final GetRainfallSeriesRuntimeBO frame = new GetRainfallSeriesRuntimeBO();

    public RainfallGetRainfallSeriesProcessTask(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> newRainfallSeries, Map<Long, Double> originalRainfallSeries) {
        super(ctx, self, "dv:designView_3xY5i4HkUNciNe3ll79", "获取雨量序列", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 获取雨量序列");
        this.self = self;
        this.newRainfallSeries = newRainfallSeries;
        this.originalRainfallSeries = originalRainfallSeries;
    }

    private void setNewRainfallSeries(Map<Long, Double> value) {
        this.newRainfallSeries = value;
    }

    /**
     * 非空输入形参：雨量序列新
     */
    public Map<Long, Double> getNewRainfallSeries() {
        return this.newRainfallSeries;
    }

    private void setOriginalRainfallSeries(Map<Long, Double> value) {
        this.originalRainfallSeries = value;
    }

    /**
     * 非空输入形参：原雨量序列
     */
    public Map<Long, Double> getOriginalRainfallSeries() {
        return this.originalRainfallSeries;
    }

    @Override
    protected boolean hasFlow() {
        return true;
    }

    @Override
    protected int[] entryNodes() {
        return new int[] {0};
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                fireLineProbe("ELVKnvnZfwG8tYrMmsL");
                // 调用瞬时子过程：过程1 文本1 = 雨量序列新;
                if (!spawnOrRun(0, new RainfallProcedure1ProcessTask(ctx, self, this.newRainfallSeries))) return false;
            }
            case 1 -> {
                fireLineProbe("LInfR3BLsP5Z3GvJ1Ki");
                // 键值对转文本（innerCollectionMap.keyValueToText）
                String r = BuiltinTools.toJsonText(this.originalRainfallSeries);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("zAfsEfHwwoUuKB4g4r8");
                // 文本转键值对（innerCollectionMap.textToKeyValue） 文本 = 键值对转文本.文本;
                String v0 = frame.getOut1();
                BuiltinTools.textToKeyValueInto(this.newRainfallSeries, v0);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                activate(1);
            }
            case 1 -> {
                activate(2);
            }
            case 2 -> {
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.originalRainfallSeries == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("newRainfallSeries", GromaValues.encode(this.newRainfallSeries));
        slots.put("originalRainfallSeries", GromaValues.encode(this.originalRainfallSeries));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.newRainfallSeries = (Map<Long, Double>) GromaValues.decode(slots.get("newRainfallSeries"), ctx);
        this.originalRainfallSeries = (Map<Long, Double>) GromaValues.decode(slots.get("originalRainfallSeries"), ctx);
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

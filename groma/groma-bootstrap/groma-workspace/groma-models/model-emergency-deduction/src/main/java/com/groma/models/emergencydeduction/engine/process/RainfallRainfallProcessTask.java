package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.RainfallRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.RainfallEntity;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：降雨（方圆过程原名：降雨；宿主对象：降雨）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "TvGbpspuGv5vJ1zt0qX",
        zhName = "降雨",
        kind = "PROCESS_TASK"
)
public final class RainfallRainfallProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：降雨）
     */
    private final RainfallEntity self;

    /**
     * 非空输入形参：雨量序列
     */
    private Map<Long, Double> rainfallSeries;

    /**
     * 输出形参：当前雨强
     */
    private double currentRainIntensity;

    /**
     * 输出形参：累计雨量
     */
    private double cumulativeRainfall;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final RainfallRuntimeBO frame = new RainfallRuntimeBO();

    public RainfallRainfallProcessTask(GromaWorldContext ctx, RainfallEntity self,
            Map<Long, Double> rainfallSeries, double currentRainIntensity,
            double cumulativeRainfall) {
        super(ctx, self, "dv:designView_fjlFgY29HmzqjbmHxYC", "降雨", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 降雨");
        this.self = self;
        this.rainfallSeries = rainfallSeries;
        this.currentRainIntensity = currentRainIntensity;
        this.cumulativeRainfall = cumulativeRainfall;
    }

    private void setRainfallSeries(Map<Long, Double> value) {
        this.rainfallSeries = value;
    }

    /**
     * 非空输入形参：雨量序列
     */
    public Map<Long, Double> getRainfallSeries() {
        return this.rainfallSeries;
    }

    private void setCurrentRainIntensity(double value) {
        this.currentRainIntensity = value;
    }

    /**
     * 输出形参：当前雨强
     */
    public double getCurrentRainIntensity() {
        return this.currentRainIntensity;
    }

    private void setCumulativeRainfall(double value) {
        this.cumulativeRainfall = value;
    }

    /**
     * 输出形参：累计雨量
     */
    public double getCumulativeRainfall() {
        return this.cumulativeRainfall;
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
    protected boolean isSlowToolNode(int pc) {
        return pc == 3;
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                // 调用瞬时子过程：获取雨量序列 雨量序列新 = 可用雨量序列; 原雨量序列 = 雨量序列;
                if (!spawnOrRun(0, new RainfallGetRainfallSeriesProcessTask(ctx, self, frame.getAvailableRainfallSeries(), this.rainfallSeries))) return false;
            }
            case 1 -> {
                // 包含键（innerCollectionMap.containsKey） 键 = 仿真时间.时间日期;
                Long v0 = ctx.currentWorldTimeMs();
                boolean r = BuiltinTools.containsKey(frame.getAvailableRainfallSeries(), v0);
                frame.setOut1(r);
            }
            case 2 -> {
                // 获取键对应的值（innerCollectionMap.getByKey） 键 = 仿真时间.时间日期; 写出: 值 → 当前雨强;
                Long v0 = ctx.currentWorldTimeMs();
                Double r = (Double) BuiltinTools.getByKey(frame.getAvailableRainfallSeries(), v0);
                frame.setOut2((double) (r));
            }
            case 3 -> {
                // 按值递增（automatic.incByVal） a = 累计雨量; b = 除.c; 写出: a → 累计雨量;
                double v0 = this.cumulativeRainfall;
                double v1 = frame.getOut4();
                this.cumulativeRainfall = v0 + v1;
            }
            case 4 -> {
                // 除（arithmetic.divide） a = 当前雨强; b = 定值881ea9b0e2904dc99affff3803df9cb9;
                double v0 = this.currentRainIntensity;
                double v1 = 60.0D;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut4(r);
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
                if (frame.getOut1()) {
                    activate(2);
                }
                if (!(frame.getOut1())) {
                    activate(3);
                }
            }
            case 2 -> {
                this.currentRainIntensity = frame.getOut2();
                activate(4);
            }
            case 3 -> {
                activate(1);
            }
            case 4 -> {
                activate(3);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.rainfallSeries == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("rainfallSeries", GromaValues.encode(this.rainfallSeries));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.rainfallSeries = (Map<Long, Double>) GromaValues.decode(slots.get("rainfallSeries"), ctx);
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.WaterloggingChangeRuntimeBO2;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：积水变化（方圆过程原名：积水变化；宿主对象：车站）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "uK01gkyWkOsTbw8coIE",
        zhName = "积水变化",
        kind = "PROCESS_TASK"
)
public final class StationWaterloggingChangeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：车站）
     */
    private final StationEntity self;

    /**
     * 非空输入形参：水量变化值
     */
    private double waterVolumeDelta;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final WaterloggingChangeRuntimeBO2 frame = new WaterloggingChangeRuntimeBO2();

    public StationWaterloggingChangeProcessTask(GromaWorldContext ctx, StationEntity self,
            double waterVolumeDelta) {
        super(ctx, self, "dv:designView_i8XnCQtmJLFiB3KzdpL", "积水变化", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 积水变化");
        this.self = self;
        this.waterVolumeDelta = waterVolumeDelta;
    }

    private void setWaterVolumeDelta(double value) {
        this.waterVolumeDelta = value;
    }

    /**
     * 非空输入形参：水量变化值
     */
    public double getWaterVolumeDelta() {
        return this.waterVolumeDelta;
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
                fireLineProbe("G6xmqPT0TDwMP3HFhhO");
                // 加（arithmetic.add） a = 本对象.站内积水量; b = 水量变化值;
                double v0 = self.getStationWaterVolume();
                double v1 = this.waterVolumeDelta;
                double r = v0 + v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("jneXa8zlHHJAxpSJGFK");
                // 取大（arithmetic.max） a = 加.c; b = 定值69ffe6aafc6a4abfbf16fbbcff3a0c57; 写出: c → 本对象.站内积水量;
                double v0 = frame.getOut0();
                double v1 = 0.0D;
                double r = Math.max(v0, v1);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("JlLYTpcLHu8STwpuJL4");
                // 除（arithmetic.divide） a = 本对象.站内积水量; b = 本对象.等效蓄水面积; 写出: c → 本对象.站内水深;
                double v0 = self.getStationWaterVolume();
                double v1 = self.getEquivalentStorageArea();
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("sUUomPiVWbhlwAQnerQ");
                // 大于（compare.gt） a = 本对象.站内水深; b = 定值69ffe6aafc6a4abfbf16fbbcff3a0c57;
                double v0 = self.getStationWaterDepth();
                double v1 = 0.0D;
                boolean r = v0 > v1;
                frame.setOut3(r);
            }
            case 4 -> {
                fireLineProbe("KAq70NkQnpYRNOo0F3f");
                // 调用瞬时子过程：车站有积水
                if (!spawnOrRun(4, new StationStationHasWaterloggingProcessTask(ctx, self))) return false;
            }
            case 5 -> {
                fireLineProbe("QjY9Eo4lBLkSVRiJnjV");
                // 调用瞬时子过程：车站无积水
                if (!spawnOrRun(5, new StationStationNoWaterloggingProcessTask(ctx, self))) return false;
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
                self.setStationWaterVolume(frame.getOut1(), ctx);
                activate(2);
            }
            case 2 -> {
                self.setStationWaterDepth(frame.getOut2(), ctx);
                activate(3);
            }
            case 3 -> {
                if (frame.getOut3()) {
                    activate(4);
                }
                if (!(frame.getOut3())) {
                    activate(5);
                }
            }
            case 4 -> {
                // 输出转换：本对象.积水状态 → 积水
                self.setWaterloggingState(WaterloggingStateEnum.WATERLOGGING, ctx);
            }
            case 5 -> {
                // 输出转换：本对象.积水状态 → 无积水
                self.setWaterloggingState(WaterloggingStateEnum.NO_WATERLOGGING, ctx);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("waterVolumeDelta", GromaValues.encode(this.waterVolumeDelta));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("waterVolumeDelta") != null) this.waterVolumeDelta = GromaValues.toDouble(slots.get("waterVolumeDelta"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

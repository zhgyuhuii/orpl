package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.WaterloggingChangeRuntimeBO3;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：积水变化（方圆过程原名：积水变化；宿主对象：地铁区间）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "RDFt6UgNzfxWfBN4FsT",
        zhName = "积水变化",
        kind = "PROCESS_TASK"
)
public final class MetroSectionWaterloggingChangeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：地铁区间）
     */
    private final MetroSectionEntity self;

    /**
     * 非空输入形参：水量变化
     */
    private double waterVolumeChange;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final WaterloggingChangeRuntimeBO3 frame = new WaterloggingChangeRuntimeBO3();

    public MetroSectionWaterloggingChangeProcessTask(GromaWorldContext ctx, MetroSectionEntity self,
            double waterVolumeChange) {
        super(ctx, self, "dv:designView_kVXT3R1XGHTwkie2rur", "积水变化", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 积水变化");
        this.self = self;
        this.waterVolumeChange = waterVolumeChange;
    }

    private void setWaterVolumeChange(double value) {
        this.waterVolumeChange = value;
    }

    /**
     * 非空输入形参：水量变化
     */
    public double getWaterVolumeChange() {
        return this.waterVolumeChange;
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
                fireLineProbe("AKdE7Up4eFMhU1QfraP");
                // 加（arithmetic.add） a = 本对象.积水量; b = 水量变化;
                double v0 = self.getWaterVolume();
                double v1 = this.waterVolumeChange;
                double r = v0 + v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("PUhhYtDNQaSIufzDWmG");
                // 取大（arithmetic.max） a = 加.c; b = 定值9bc02657f5b44db8b09cda715db6ee94; 写出: c → 本对象.积水量;
                double v0 = frame.getOut0();
                double v1 = 0.0D;
                double r = Math.max(v0, v1);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("nfkAs618ICfcYBU74k0");
                // 调用瞬时子过程：水深变化
                if (!spawnOrRun(2, new MetroSectionWaterDepthChangeProcessTask(ctx, self))) return false;
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
                self.setWaterVolume(frame.getOut1(), ctx);
                activate(2);
            }
            case 2 -> {
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
        slots.put("waterVolumeChange", GromaValues.encode(this.waterVolumeChange));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("waterVolumeChange") != null) this.waterVolumeChange = GromaValues.toDouble(slots.get("waterVolumeChange"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

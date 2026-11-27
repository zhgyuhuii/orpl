package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.DrainRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：排水（方圆过程原名：排水；宿主对象：地铁区间）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "QzcUew3b1XLCXLK6rIH",
        zhName = "排水",
        kind = "PROCESS_TASK"
)
public final class MetroSectionDrainProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：地铁区间）
     */
    private final MetroSectionEntity self;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final DrainRuntimeBO frame = new DrainRuntimeBO();

    public MetroSectionDrainProcessTask(GromaWorldContext ctx, MetroSectionEntity self) {
        super(ctx, self, "dv:designView_kclxVkgMIrkBe36K656", "排水", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 排水");
        this.self = self;
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
        return pc == 2;
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                fireLineProbe("OBw9hw4ejbwGNANBDk1");
                // 除（arithmetic.divide） a = 定值41413d24084b41d3af3268d56dbed9fd; b = 定值fcc40fef14034824a526acaecf1cfb80;
                double v0 = BuiltinTools.toDoubleValue("");
                double v1 = 60L;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("kMccSFpujrQK0HemPww");
                // 取小（arithmetic.min） a = 除.c; b = 本对象.积水量; 写出: c → 当前排水量;
                double v0 = frame.getOut0();
                double v1 = self.getWaterVolume();
                double r = Math.min(v0, v1);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("YeWEZrjZrDxZlSIACL6");
                // 按值递减（automatic.decByVal） a = 本对象.积水量; b = 当前排水量; 写出: a → 本对象.积水量;
                double v0 = self.getWaterVolume();
                double v1 = frame.getCurrentDrainVolume();
                self.setWaterVolume(v0 - v1, ctx);
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
                frame.setCurrentDrainVolume(frame.getOut1());
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
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

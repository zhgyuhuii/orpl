package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.FlowExchangeIntervalRuntimeBO2;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：水流交换间隔（方圆过程原名：水流交换间隔；宿主对象：地铁区间）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "6qnqt4O6vaFhEkh96tN",
        zhName = "水流交换间隔",
        kind = "PROCESS_TASK"
)
public final class MetroSectionFlowExchangeIntervalProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：地铁区间）
     */
    private final MetroSectionEntity self;

    /**
     * 输出形参：本步秒数
     */
    private long stepSeconds;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FlowExchangeIntervalRuntimeBO2 frame = new FlowExchangeIntervalRuntimeBO2();

    public MetroSectionFlowExchangeIntervalProcessTask(GromaWorldContext ctx,
            MetroSectionEntity self, long stepSeconds) {
        super(ctx, self, "dv:designView_FVDIGUCMq2RHeXb9Wgr", "水流交换间隔", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 水流交换间隔");
        this.self = self;
        this.stepSeconds = stepSeconds;
    }

    private void setStepSeconds(long value) {
        this.stepSeconds = value;
    }

    /**
     * 输出形参：本步秒数
     */
    public long getStepSeconds() {
        return this.stepSeconds;
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
                fireLineProbe("Ld9DJuBtRwCoQknRp5C");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 本步秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("N88YLWSI1zflzBSBlM0");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 下步秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("WwpqNlw44LQwDJ187lW");
                // 减（arithmetic.minus） a = 下步秒; b = 本步秒; 写出: c → 本步秒数;
                double v0 = frame.getNextStepSecond();
                double v1 = frame.getStepSecond();
                double r = v0 - v1;
                frame.setOut2(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                frame.setStepSecond(frame.getOut0());
                activateAfter(1, 1L);
            }
            case 1 -> {
                frame.setNextStepSecond(frame.getOut1());
                activate(2);
            }
            case 2 -> {
                this.stepSeconds = BuiltinTools.roundToLong(frame.getOut2());
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

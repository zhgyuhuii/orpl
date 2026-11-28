package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.GetStepDurationRuntimeBO;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：获取本步时长（方圆过程原名：获取本步时长；宿主对象：独立过程）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "FUrqUDTrCHxy0I97epN",
        zhName = "获取本步时长",
        kind = "PROCESS_TASK"
)
public final class GetStepDurationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 输出形参：本步秒数
     */
    private long stepSeconds;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final GetStepDurationRuntimeBO frame = new GetStepDurationRuntimeBO();

    public GetStepDurationProcessTask(GromaWorldContext ctx, long stepSeconds) {
        super(ctx, null, "dv:designView_JTcC0L3UTKJxpFJ1ods", "获取本步时长", TIMING);
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
                fireLineProbe("iC3ZlxzuPt1nGbrif3x");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 当前步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("T39xdnlxVncxCn1lTsY");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 下一步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("hCPOb59zpS2bjDSUBkE");
                // 减（arithmetic.minus） a = 下一步进秒; b = 当前步进秒; 写出: c → 本步秒数;
                double v0 = frame.getNextStepSeconds();
                double v1 = frame.getCurrentStepSeconds();
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
                frame.setCurrentStepSeconds(frame.getOut0());
                activateAfter(1, 1L);
            }
            case 1 -> {
                frame.setNextStepSeconds(frame.getOut1());
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

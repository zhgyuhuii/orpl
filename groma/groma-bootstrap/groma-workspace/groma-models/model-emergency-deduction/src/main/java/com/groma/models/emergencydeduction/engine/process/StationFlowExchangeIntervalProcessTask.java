package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FlowExchangeIntervalRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：水流交换间隔（方圆过程原名：水流交换间隔；宿主对象：车站）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "a9Ho9Zh9bCnkV8wVlfJ",
        zhName = "水流交换间隔",
        kind = "PROCESS_TASK"
)
public final class StationFlowExchangeIntervalProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：车站）
     */
    private final StationEntity self;

    /**
     * 输出形参：本步时长
     */
    private long stepDuration;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FlowExchangeIntervalRuntimeBO frame = new FlowExchangeIntervalRuntimeBO();

    public StationFlowExchangeIntervalProcessTask(GromaWorldContext ctx, StationEntity self,
            long stepDuration) {
        super(ctx, self, "dv:designView_2aI60V2gYdhoWiHC5B3", "水流交换间隔", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 水流交换间隔");
        this.self = self;
        this.stepDuration = stepDuration;
    }

    private void setStepDuration(long value) {
        this.stepDuration = value;
    }

    /**
     * 输出形参：本步时长
     */
    public long getStepDuration() {
        return this.stepDuration;
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
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 本次步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut0(r);
            }
            case 1 -> {
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 下一步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                // 减（arithmetic.minus） a = 下一步进秒; b = 本次步进秒; 写出: c → 本步时长;
                double v0 = frame.getNextStepSeconds();
                double v1 = frame.getThisStepSeconds();
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
                frame.setThisStepSeconds(frame.getOut0());
                activate(1);
            }
            case 1 -> {
                frame.setNextStepSeconds(frame.getOut1());
                activate(2);
            }
            case 2 -> {
                this.stepDuration = BuiltinTools.roundToLong(frame.getOut2());
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

package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.ComputeDisposalTimeRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.PlanGeneratorEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：计算处置时间（方圆过程原名：计算处置时间；宿主对象：方案生成器）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "IJT778zU1zIw6nc7uYe",
        zhName = "计算处置时间",
        kind = "PROCESS_TASK"
)
public final class PlanGeneratorComputeDisposalTimeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：方案生成器）
     */
    private final PlanGeneratorEntity self;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final ComputeDisposalTimeRuntimeBO frame = new ComputeDisposalTimeRuntimeBO();

    public PlanGeneratorComputeDisposalTimeProcessTask(GromaWorldContext ctx,
            PlanGeneratorEntity self) {
        super(ctx, self, "dv:designView_P80bc3nl6S9bWISn6bg", "计算处置时间", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 计算处置时间");
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
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                // 除（arithmetic.divide） b = 换算;
                double v0 = BuiltinTools.toDoubleValue(null);
                double v1 = 3600000.0D;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut0(r);
            }
            case 1 -> {
                // 乘（arithmetic.multiply） a = 除.c; b = 本对象.地面积水区.汇水面积;
                double v0 = frame.getOut0();
                double v1 = self.getSurfaceWaterloggingArea().getCatchmentArea();
                double r = v0 * v1;
                frame.setOut1(r);
            }
            case 2 -> {
                // 乘（arithmetic.multiply） a = 乘.c; b = 本对象.地面积水区.径流系数; 写出: c → 汇水速度;
                double v0 = frame.getOut1();
                double v1 = self.getSurfaceWaterloggingArea().getRunoffCoeff();
                double r = v0 * v1;
                frame.setOut2(r);
            }
            case 3 -> {
                // 除（arithmetic.divide） a = 本对象.地面积水区.地表排水能力; b = 换算;
                double v0 = self.getSurfaceWaterloggingArea().getSurfaceDrainCapacity();
                double v1 = 60.0D;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut3(r);
            }
            case 4 -> {
                // 减（arithmetic.minus） a = 汇水速度; b = 除.c; 写出: c → 净积水速度;
                double v0 = frame.getCatchmentSpeed();
                double v1 = frame.getOut3();
                double r = v0 - v1;
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
                activate(2);
            }
            case 2 -> {
                frame.setCatchmentSpeed(frame.getOut2());
                activate(3);
            }
            case 3 -> {
                activate(4);
            }
            case 4 -> {
                frame.setNetWaterloggingSpeed(frame.getOut4());
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

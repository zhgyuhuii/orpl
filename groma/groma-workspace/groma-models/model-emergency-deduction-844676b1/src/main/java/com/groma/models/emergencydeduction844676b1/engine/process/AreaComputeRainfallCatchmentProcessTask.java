package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.ComputeRainfallCatchmentRuntimeBO;
import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：计算降雨汇水（方圆过程原名：计算降雨汇水；宿主对象：区域）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "QZPPbXby5BPfgS5sziq",
        zhName = "计算降雨汇水",
        kind = "PROCESS_TASK"
)
public final class AreaComputeRainfallCatchmentProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：区域）
     */
    private final AreaEntity self;

    /**
     * 非空输入形参：当前雨强
     */
    private double currentRainIntensity;

    /**
     * 输出形参：当前汇水流量
     */
    private double currentCatchmentFlow;

    /**
     * 输出形参：本步汇水量
     */
    private double stepCatchmentVolume;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final ComputeRainfallCatchmentRuntimeBO frame = new ComputeRainfallCatchmentRuntimeBO();

    public AreaComputeRainfallCatchmentProcessTask(GromaWorldContext ctx, AreaEntity self,
            double currentRainIntensity, double currentCatchmentFlow, double stepCatchmentVolume) {
        super(ctx, self, "dv:designView_aroqnVQzhKVnM1h7wPt", "计算降雨汇水", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 计算降雨汇水");
        this.self = self;
        this.currentRainIntensity = currentRainIntensity;
        this.currentCatchmentFlow = currentCatchmentFlow;
        this.stepCatchmentVolume = stepCatchmentVolume;
    }

    private void setCurrentRainIntensity(double value) {
        this.currentRainIntensity = value;
    }

    /**
     * 非空输入形参：当前雨强
     */
    public double getCurrentRainIntensity() {
        return this.currentRainIntensity;
    }

    private void setCurrentCatchmentFlow(double value) {
        this.currentCatchmentFlow = value;
    }

    /**
     * 输出形参：当前汇水流量
     */
    public double getCurrentCatchmentFlow() {
        return this.currentCatchmentFlow;
    }

    private void setStepCatchmentVolume(double value) {
        this.stepCatchmentVolume = value;
    }

    /**
     * 输出形参：本步汇水量
     */
    public double getStepCatchmentVolume() {
        return this.stepCatchmentVolume;
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
                fireLineProbe("f22x2E0wy1KiKgicnvy");
                // 大于（compare.gt） a = 当前雨强; b = 定值6e295c0bbbfc4eafac40328a0d9f3405;
                double v0 = this.currentRainIntensity;
                double v1 = 0L;
                boolean r = v0 > v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("OcOXdjtKn4hcWOwes2M");
                // 清空（assignment.clear） a = 当前汇水流量; 写出: a → 当前汇水流量;
                this.currentCatchmentFlow = 0.0D;
            }
            case 2 -> {
                fireLineProbe("lEiboXyj7ShlUhWWQa4");
                // 除（arithmetic.divide） a = 当前雨强; b = mm/min换算成m/s;
                double v0 = this.currentRainIntensity;
                double v1 = 60000L;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("3FOxu9ny62DtUsYrL5e");
                // 清空（assignment.clear） a = 本步汇水量; 写出: a → 本步汇水量;
                this.stepCatchmentVolume = 0.0D;
            }
            case 4 -> {
                fireLineProbe("03PuHTbm5tlsA4MM5yz");
                // 乘（arithmetic.multiply） a = 除.c; b = 本对象.汇水面积;
                double v0 = frame.getOut2();
                double v1 = self.getCatchmentArea();
                double r = v0 * v1;
                frame.setOut4(r);
            }
            case 5 -> {
                fireLineProbe("Ez0RVlynEixyH1TWOKY");
                // 乘（arithmetic.multiply） a = 乘.c; b = 本对象.径流系数; 写出: c → 当前汇水流量;
                double v0 = frame.getOut4();
                double v1 = self.getRunoffCoeff();
                double r = v0 * v1;
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("PXlvqzo1yVWtn92Gx8m");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 当前步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut6(r);
            }
            case 7 -> {
                fireLineProbe("A1JG0kjwroPdBq16yPt");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 下一步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut7(r);
            }
            case 8 -> {
                fireLineProbe("801sIHXVf4QE15adldy");
                // 减（arithmetic.minus） a = 下一步进秒; b = 当前步进秒; 写出: c → 本步秒数;
                double v0 = frame.getNextStepSeconds();
                double v1 = frame.getCurrentStepSeconds();
                double r = v0 - v1;
                frame.setOut8(BuiltinTools.roundToLong(r));
            }
            case 9 -> {
                fireLineProbe("EgsEAxkmdrF5zawLJiE");
                // 乘（arithmetic.multiply） a = 本步秒数; b = 当前汇水流量; 写出: c → 本步汇水量;
                double v0 = frame.getStepSeconds();
                double v1 = this.currentCatchmentFlow;
                double r = v0 * v1;
                frame.setOut9(r);
            }
            case 10 -> {
                fireLineProbe("srKLafKFJ4XCJWiY9qp");
                // 调用瞬时子过程：积水变化 水量变化值 = 本步汇水量;
                if (!spawnOrRun(10, new AreaWaterloggingChangeProcessTask(ctx, self, this.stepCatchmentVolume))) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                if (!(frame.getOut0())) {
                    activate(1);
                }
                if (frame.getOut0()) {
                    activate(2);
                }
            }
            case 1 -> {
                activateAfter(3, 1L);
            }
            case 2 -> {
                activate(4);
            }
            case 3 -> {
                activate(0);
            }
            case 4 -> {
                activate(5);
            }
            case 5 -> {
                this.currentCatchmentFlow = frame.getOut5();
                activate(6);
            }
            case 6 -> {
                frame.setCurrentStepSeconds(BuiltinTools.roundToLong(frame.getOut6()));
                activateAfter(7, 1L);
            }
            case 7 -> {
                frame.setNextStepSeconds(BuiltinTools.roundToLong(frame.getOut7()));
                activate(8);
            }
            case 8 -> {
                frame.setStepSeconds(frame.getOut8());
                activate(9);
            }
            case 9 -> {
                this.stepCatchmentVolume = frame.getOut9();
                activate(10);
            }
            case 10 -> {
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
        slots.put("currentRainIntensity", GromaValues.encode(this.currentRainIntensity));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("currentRainIntensity") != null) this.currentRainIntensity = GromaValues.toDouble(slots.get("currentRainIntensity"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

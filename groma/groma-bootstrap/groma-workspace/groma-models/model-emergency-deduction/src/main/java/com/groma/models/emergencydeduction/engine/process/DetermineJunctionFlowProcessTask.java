package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.DetermineJunctionFlowRuntimeBO;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：确定连接处过水情况（方圆过程原名：确定连接处过水情况；宿主对象：独立过程）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "TUmeBFJE9XAot7jIGeV",
        zhName = "确定连接处过水情况",
        kind = "PROCESS_TASK"
)
public final class DetermineJunctionFlowProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 非空输入形参：过水控制高程
     */
    private double flowControlElevation;

    /**
     * 非空输入形参：过水宽度
     */
    private double flowWidth;

    /**
     * 非空输入形参：第一侧水面高程
     */
    private double firstSideWaterLevel;

    /**
     * 输出形参：当前过水流量
     */
    private double currentFlowRate;

    /**
     * 非空输入形参：第一侧积水量
     */
    private double firstSideWaterVolume;

    /**
     * 输出形参：第一侧流向第二侧
     */
    private boolean firstFlowsToSecond;

    /**
     * 非空输入形参：第二侧积水量
     */
    private double secondSideWaterVolume;

    /**
     * 输出形参：本步过水量
     */
    private double stepFlowVolume;

    /**
     * 非空输入形参：第二侧水面高程
     */
    private double secondSideWaterLevel;

    /**
     * 非空输入形参：本步秒数
     */
    private long stepSeconds;

    /**
     * 非空输入形参：流量系数
     */
    private double flowCoeff;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final DetermineJunctionFlowRuntimeBO frame = new DetermineJunctionFlowRuntimeBO();

    public DetermineJunctionFlowProcessTask(GromaWorldContext ctx, double flowControlElevation,
            double flowWidth, double firstSideWaterLevel, double currentFlowRate,
            double firstSideWaterVolume, boolean firstFlowsToSecond, double secondSideWaterVolume,
            double stepFlowVolume, double secondSideWaterLevel, long stepSeconds,
            double flowCoeff) {
        super(ctx, null, "dv:designView_H4eBTelUToW12IqLJ8O", "确定连接处过水情况", TIMING);
        this.flowControlElevation = flowControlElevation;
        this.flowWidth = flowWidth;
        this.firstSideWaterLevel = firstSideWaterLevel;
        this.currentFlowRate = currentFlowRate;
        this.firstSideWaterVolume = firstSideWaterVolume;
        this.firstFlowsToSecond = firstFlowsToSecond;
        this.secondSideWaterVolume = secondSideWaterVolume;
        this.stepFlowVolume = stepFlowVolume;
        this.secondSideWaterLevel = secondSideWaterLevel;
        this.stepSeconds = stepSeconds;
        this.flowCoeff = flowCoeff;
    }

    private void setFlowControlElevation(double value) {
        this.flowControlElevation = value;
    }

    /**
     * 非空输入形参：过水控制高程
     */
    public double getFlowControlElevation() {
        return this.flowControlElevation;
    }

    private void setFlowWidth(double value) {
        this.flowWidth = value;
    }

    /**
     * 非空输入形参：过水宽度
     */
    public double getFlowWidth() {
        return this.flowWidth;
    }

    private void setFirstSideWaterLevel(double value) {
        this.firstSideWaterLevel = value;
    }

    /**
     * 非空输入形参：第一侧水面高程
     */
    public double getFirstSideWaterLevel() {
        return this.firstSideWaterLevel;
    }

    private void setCurrentFlowRate(double value) {
        this.currentFlowRate = value;
    }

    /**
     * 输出形参：当前过水流量
     */
    public double getCurrentFlowRate() {
        return this.currentFlowRate;
    }

    private void setFirstSideWaterVolume(double value) {
        this.firstSideWaterVolume = value;
    }

    /**
     * 非空输入形参：第一侧积水量
     */
    public double getFirstSideWaterVolume() {
        return this.firstSideWaterVolume;
    }

    private void setFirstFlowsToSecond(boolean value) {
        this.firstFlowsToSecond = value;
    }

    /**
     * 输出形参：第一侧流向第二侧
     */
    public boolean getFirstFlowsToSecond() {
        return this.firstFlowsToSecond;
    }

    private void setSecondSideWaterVolume(double value) {
        this.secondSideWaterVolume = value;
    }

    /**
     * 非空输入形参：第二侧积水量
     */
    public double getSecondSideWaterVolume() {
        return this.secondSideWaterVolume;
    }

    private void setStepFlowVolume(double value) {
        this.stepFlowVolume = value;
    }

    /**
     * 输出形参：本步过水量
     */
    public double getStepFlowVolume() {
        return this.stepFlowVolume;
    }

    private void setSecondSideWaterLevel(double value) {
        this.secondSideWaterLevel = value;
    }

    /**
     * 非空输入形参：第二侧水面高程
     */
    public double getSecondSideWaterLevel() {
        return this.secondSideWaterLevel;
    }

    private void setStepSeconds(long value) {
        this.stepSeconds = value;
    }

    /**
     * 非空输入形参：本步秒数
     */
    public long getStepSeconds() {
        return this.stepSeconds;
    }

    private void setFlowCoeff(double value) {
        this.flowCoeff = value;
    }

    /**
     * 非空输入形参：流量系数
     */
    public double getFlowCoeff() {
        return this.flowCoeff;
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
        return pc == 14;
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                // 大于（compare.gt） a = 第一侧水面高程; b = 第二侧水面高程;
                double v0 = this.firstSideWaterLevel;
                double v1 = this.secondSideWaterLevel;
                boolean r = v0 > v1;
                frame.setOut0(r);
            }
            case 1 -> {
                // 赋值（assignment.assign） a = 第一侧水面高程; 写出: b → 流出侧水面高程;
                double v0 = this.firstSideWaterLevel;
                double r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                // 赋值（assignment.assign） a = 第二侧水面高程; 写出: b → 流出侧水面高程;
                double v0 = this.secondSideWaterLevel;
                double r = v0;
                frame.setOut2(r);
            }
            case 3 -> {
                // 赋值（assignment.assign） a = 第二侧水面高程; 写出: b → 流入侧水面高程;
                double v0 = this.secondSideWaterLevel;
                double r = v0;
                frame.setOut3(r);
            }
            case 4 -> {
                // 赋值（assignment.assign） a = 第一侧水面高程; 写出: b → 流入侧水面高程;
                double v0 = this.firstSideWaterLevel;
                double r = v0;
                frame.setOut4(r);
            }
            case 5 -> {
                // 赋值（assignment.assign） a = 第一侧积水量; 写出: b → 流出侧积水量;
                double v0 = this.firstSideWaterVolume;
                double r = v0;
                frame.setOut5(r);
            }
            case 6 -> {
                // 赋值（assignment.assign） a = 第二侧积水量; 写出: b → 流出侧积水量;
                double v0 = this.secondSideWaterVolume;
                double r = v0;
                frame.setOut6(r);
            }
            case 7 -> {
                // 赋值（assignment.assign） a = 第一侧流向第二侧; 写出: b → 第一侧流向第二侧;
                boolean v0 = frame.getFirstFlowsToSecond2();
                boolean r = v0;
                frame.setOut7(r);
            }
            case 8 -> {
                // 赋值（assignment.assign） a = 第二侧流向第一侧; 写出: b → 第一侧流向第二侧;
                boolean v0 = frame.getSecondFlowsToFirst();
                boolean r = v0;
                frame.setOut8(r);
            }
            case 9 -> {
                // 取大（arithmetic.max） a = 流入侧水面高程; b = 过水控制高程;
                double v0 = frame.getInflowSideWaterLevel();
                double v1 = this.flowControlElevation;
                double r = Math.max(v0, v1);
                frame.setOut9(r);
            }
            case 10 -> {
                // 减（arithmetic.minus） a = 流出侧水面高程; b = 取大.c;
                double v0 = frame.getOutflowSideWaterLevel();
                double v1 = frame.getOut9();
                double r = v0 - v1;
                frame.setOut10(r);
            }
            case 11 -> {
                // 取大（arithmetic.max） a = 减.c; b = 定值2d9f87a1f2e148f1aaa9491526c11e90; 写出: c → 有效龙头;
                double v0 = frame.getOut10();
                double v1 = 0.0D;
                double r = Math.max(v0, v1);
                frame.setOut11(r);
            }
            case 12 -> {
                // 乘（arithmetic.multiply） a = 过水宽度; b = 流量系数;
                double v0 = this.flowWidth;
                double v1 = this.flowCoeff;
                double r = v0 * v1;
                frame.setOut12(r);
            }
            case 13 -> {
                // 乘（arithmetic.multiply） a = 乘.c; b = 定值ca8aee5600174299b43c6b63823d0242;
                double v0 = frame.getOut12();
                double v1 = 4.429D;
                double r = v0 * v1;
                frame.setOut13(r);
            }
            case 14 -> {
                // 取a的b次幂（arithmetic.power） a = 有效龙头; b = 定值12a8bf15ded2429ca2ac7aaf2a28e1ac;
                double v0 = frame.getEffectiveCrest();
                double v1 = 1.5D;
                double r = Math.pow(v0, v1);
                frame.setOut14(r);
            }
            case 15 -> {
                // 乘（arithmetic.multiply） a = 乘.c; b = 取a的b次幂.c; 写出: c → 预计过水流量;
                double v0 = frame.getOut13();
                double v1 = frame.getOut14();
                double r = v0 * v1;
                frame.setOut15(r);
            }
            case 16 -> {
                // 乘（arithmetic.multiply） a = 预计过水流量; b = 本步秒数;
                double v0 = frame.getEstimatedFlowRate();
                double v1 = this.stepSeconds;
                double r = v0 * v1;
                frame.setOut16(r);
            }
            case 17 -> {
                // 取小（arithmetic.min） a = 乘.c; b = 流出侧积水量; 写出: c → 本步过水量;
                double v0 = frame.getOut16();
                double v1 = frame.getOutflowSideWaterVolume();
                double r = Math.min(v0, v1);
                frame.setOut17(r);
            }
            case 18 -> {
                // 除（arithmetic.divide） a = 本步过水量; b = 本步秒数;
                double v0 = this.stepFlowVolume;
                double v1 = this.stepSeconds;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut18(r);
            }
            case 19 -> {
                // 乘（arithmetic.multiply） a = 定值6250aba4219d4583b81fb462976ae901; b = 除.c; 写出: c → 当前过水流量;
                double v0 = 60L;
                double v1 = frame.getOut18();
                double r = v0 * v1;
                frame.setOut19(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                if (frame.getOut0()) {
                    activate(1);
                }
                if (!(frame.getOut0())) {
                    activate(2);
                }
            }
            case 1 -> {
                frame.setOutflowSideWaterLevel(frame.getOut1());
                activate(3);
            }
            case 2 -> {
                frame.setOutflowSideWaterLevel(frame.getOut2());
                activate(4);
            }
            case 3 -> {
                frame.setInflowSideWaterLevel(frame.getOut3());
                activate(5);
            }
            case 4 -> {
                frame.setInflowSideWaterLevel(frame.getOut4());
                activate(6);
            }
            case 5 -> {
                frame.setOutflowSideWaterVolume(frame.getOut5());
                activate(7);
            }
            case 6 -> {
                frame.setOutflowSideWaterVolume(frame.getOut6());
                activate(8);
            }
            case 7 -> {
                this.firstFlowsToSecond = frame.getOut7();
                activate(9);
            }
            case 8 -> {
                this.firstFlowsToSecond = frame.getOut8();
                activate(9);
            }
            case 9 -> {
                activate(10);
            }
            case 10 -> {
                activate(11);
            }
            case 11 -> {
                frame.setEffectiveCrest(frame.getOut11());
                activate(12);
            }
            case 12 -> {
                activate(13);
            }
            case 13 -> {
                activate(14);
            }
            case 14 -> {
                activate(15);
            }
            case 15 -> {
                frame.setEstimatedFlowRate(frame.getOut15());
                activate(16);
            }
            case 16 -> {
                activate(17);
            }
            case 17 -> {
                this.stepFlowVolume = frame.getOut17();
                activate(18);
            }
            case 18 -> {
                activate(19);
            }
            case 19 -> {
                this.currentFlowRate = frame.getOut19();
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
        slots.put("flowControlElevation", GromaValues.encode(this.flowControlElevation));
        slots.put("flowWidth", GromaValues.encode(this.flowWidth));
        slots.put("firstSideWaterLevel", GromaValues.encode(this.firstSideWaterLevel));
        slots.put("firstSideWaterVolume", GromaValues.encode(this.firstSideWaterVolume));
        slots.put("secondSideWaterVolume", GromaValues.encode(this.secondSideWaterVolume));
        slots.put("secondSideWaterLevel", GromaValues.encode(this.secondSideWaterLevel));
        slots.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        slots.put("flowCoeff", GromaValues.encode(this.flowCoeff));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("flowControlElevation") != null) this.flowControlElevation = GromaValues.toDouble(slots.get("flowControlElevation"));
        if (slots.get("flowWidth") != null) this.flowWidth = GromaValues.toDouble(slots.get("flowWidth"));
        if (slots.get("firstSideWaterLevel") != null) this.firstSideWaterLevel = GromaValues.toDouble(slots.get("firstSideWaterLevel"));
        if (slots.get("firstSideWaterVolume") != null) this.firstSideWaterVolume = GromaValues.toDouble(slots.get("firstSideWaterVolume"));
        if (slots.get("secondSideWaterVolume") != null) this.secondSideWaterVolume = GromaValues.toDouble(slots.get("secondSideWaterVolume"));
        if (slots.get("secondSideWaterLevel") != null) this.secondSideWaterLevel = GromaValues.toDouble(slots.get("secondSideWaterLevel"));
        if (slots.get("stepSeconds") != null) this.stepSeconds = GromaValues.toLong(slots.get("stepSeconds"));
        if (slots.get("flowCoeff") != null) this.flowCoeff = GromaValues.toDouble(slots.get("flowCoeff"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

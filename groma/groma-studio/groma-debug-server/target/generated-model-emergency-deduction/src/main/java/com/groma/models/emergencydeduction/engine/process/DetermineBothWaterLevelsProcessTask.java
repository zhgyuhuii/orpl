package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.DetermineBothWaterLevelsRuntimeBO;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：确定两端水位（方圆过程原名：确定两端水位；宿主对象：独立过程）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "Pupqb6kGlHmKlcG6aaU",
        zhName = "确定两端水位",
        kind = "PROCESS_TASK"
)
public final class DetermineBothWaterLevelsProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 非空输入形参：第一侧底高程
     */
    private double firstSideFloorElevation;

    /**
     * 输出形参：第一侧水面高程
     */
    private double firstSideWaterLevel;

    /**
     * 非空输入形参：第一侧水深
     */
    private double firstSideDepth;

    /**
     * 输出形参：第二侧水面高程
     */
    private double secondSideWaterLevel;

    /**
     * 非空输入形参：第二侧底高程
     */
    private double secondSideFloorElevation;

    /**
     * 非空输入形参：第二侧水深
     */
    private double secondSideDepth;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final DetermineBothWaterLevelsRuntimeBO frame = new DetermineBothWaterLevelsRuntimeBO();

    public DetermineBothWaterLevelsProcessTask(GromaWorldContext ctx,
            double firstSideFloorElevation, double firstSideWaterLevel, double firstSideDepth,
            double secondSideWaterLevel, double secondSideFloorElevation, double secondSideDepth) {
        super(ctx, null, "dv:designView_BPlSUsxYsStEoFQEAGD", "确定两端水位", TIMING);
        this.firstSideFloorElevation = firstSideFloorElevation;
        this.firstSideWaterLevel = firstSideWaterLevel;
        this.firstSideDepth = firstSideDepth;
        this.secondSideWaterLevel = secondSideWaterLevel;
        this.secondSideFloorElevation = secondSideFloorElevation;
        this.secondSideDepth = secondSideDepth;
    }

    private void setFirstSideFloorElevation(double value) {
        this.firstSideFloorElevation = value;
    }

    /**
     * 非空输入形参：第一侧底高程
     */
    public double getFirstSideFloorElevation() {
        return this.firstSideFloorElevation;
    }

    private void setFirstSideWaterLevel(double value) {
        this.firstSideWaterLevel = value;
    }

    /**
     * 输出形参：第一侧水面高程
     */
    public double getFirstSideWaterLevel() {
        return this.firstSideWaterLevel;
    }

    private void setFirstSideDepth(double value) {
        this.firstSideDepth = value;
    }

    /**
     * 非空输入形参：第一侧水深
     */
    public double getFirstSideDepth() {
        return this.firstSideDepth;
    }

    private void setSecondSideWaterLevel(double value) {
        this.secondSideWaterLevel = value;
    }

    /**
     * 输出形参：第二侧水面高程
     */
    public double getSecondSideWaterLevel() {
        return this.secondSideWaterLevel;
    }

    private void setSecondSideFloorElevation(double value) {
        this.secondSideFloorElevation = value;
    }

    /**
     * 非空输入形参：第二侧底高程
     */
    public double getSecondSideFloorElevation() {
        return this.secondSideFloorElevation;
    }

    private void setSecondSideDepth(double value) {
        this.secondSideDepth = value;
    }

    /**
     * 非空输入形参：第二侧水深
     */
    public double getSecondSideDepth() {
        return this.secondSideDepth;
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
                fireLineProbe("lfKNruivWqTnwHJeSNU");
                // 加（arithmetic.add） a = 第一侧底高程; b = 第一侧水深; 写出: c → 第一侧水面高程;
                double v0 = this.firstSideFloorElevation;
                double v1 = this.firstSideDepth;
                double r = v0 + v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("oTh8esAgDeVbRmNYIDy");
                // 加（arithmetic.add） a = 第二侧底高程; b = 第二侧水深; 写出: c → 第二侧水面高程;
                double v0 = this.secondSideFloorElevation;
                double v1 = this.secondSideDepth;
                double r = v0 + v1;
                frame.setOut1(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                this.firstSideWaterLevel = frame.getOut0();
                activate(1);
            }
            case 1 -> {
                this.secondSideWaterLevel = frame.getOut1();
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
        slots.put("firstSideFloorElevation", GromaValues.encode(this.firstSideFloorElevation));
        slots.put("firstSideDepth", GromaValues.encode(this.firstSideDepth));
        slots.put("secondSideFloorElevation", GromaValues.encode(this.secondSideFloorElevation));
        slots.put("secondSideDepth", GromaValues.encode(this.secondSideDepth));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("firstSideFloorElevation") != null) this.firstSideFloorElevation = GromaValues.toDouble(slots.get("firstSideFloorElevation"));
        if (slots.get("firstSideDepth") != null) this.firstSideDepth = GromaValues.toDouble(slots.get("firstSideDepth"));
        if (slots.get("secondSideFloorElevation") != null) this.secondSideFloorElevation = GromaValues.toDouble(slots.get("secondSideFloorElevation"));
        if (slots.get("secondSideDepth") != null) this.secondSideDepth = GromaValues.toDouble(slots.get("secondSideDepth"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

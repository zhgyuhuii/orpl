package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.InletFlowRuntimeBO;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import com.groma.models.emergencydeduction844676b1.model.enums.FloodWallStateEnum;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：入口过水（方圆过程原名：入口过水；宿主对象：车站）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "zlRqcFoeLHuEKONcV2V",
        zhName = "入口过水",
        kind = "PROCESS_TASK"
)
public final class StationInletFlowProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：车站）
     */
    private final StationEntity self;

    /**
     * 输出形参：向站内流入
     */
    private boolean flowsIntoStation;

    /**
     * 非空输入形参：区域水面高程
     */
    private double areaWaterLevel;

    /**
     * 非空输入形参：本步秒数
     */
    private long stepSeconds;

    /**
     * 输出形参：当前过水流量
     */
    private double currentFlowRate;

    /**
     * 非空输入形参：站内水面高程
     */
    private double stationWaterLevel;

    /**
     * 输出形参：本步过水量
     */
    private double stepFlowVolume;

    /**
     * 非空输入形参：区域积水量
     */
    private double areaWaterVolume;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final InletFlowRuntimeBO frame = new InletFlowRuntimeBO();

    public StationInletFlowProcessTask(GromaWorldContext ctx, StationEntity self,
            boolean flowsIntoStation, double areaWaterLevel, long stepSeconds,
            double currentFlowRate, double stationWaterLevel, double stepFlowVolume,
            double areaWaterVolume) {
        super(ctx, self, "dv:designView_0joI684F5zzw3QGvIDY", "入口过水", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 入口过水");
        this.self = self;
        this.flowsIntoStation = flowsIntoStation;
        this.areaWaterLevel = areaWaterLevel;
        this.stepSeconds = stepSeconds;
        this.currentFlowRate = currentFlowRate;
        this.stationWaterLevel = stationWaterLevel;
        this.stepFlowVolume = stepFlowVolume;
        this.areaWaterVolume = areaWaterVolume;
    }

    private void setFlowsIntoStation(boolean value) {
        this.flowsIntoStation = value;
    }

    /**
     * 输出形参：向站内流入
     */
    public boolean getFlowsIntoStation() {
        return this.flowsIntoStation;
    }

    private void setAreaWaterLevel(double value) {
        this.areaWaterLevel = value;
    }

    /**
     * 非空输入形参：区域水面高程
     */
    public double getAreaWaterLevel() {
        return this.areaWaterLevel;
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

    private void setCurrentFlowRate(double value) {
        this.currentFlowRate = value;
    }

    /**
     * 输出形参：当前过水流量
     */
    public double getCurrentFlowRate() {
        return this.currentFlowRate;
    }

    private void setStationWaterLevel(double value) {
        this.stationWaterLevel = value;
    }

    /**
     * 非空输入形参：站内水面高程
     */
    public double getStationWaterLevel() {
        return this.stationWaterLevel;
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

    private void setAreaWaterVolume(double value) {
        this.areaWaterVolume = value;
    }

    /**
     * 非空输入形参：区域积水量
     */
    public double getAreaWaterVolume() {
        return this.areaWaterVolume;
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
                fireLineProbe("KEG1yTnCzrqjOGJSnNh");
                // 等于（compare.eq） a = 本对象.挡水墙状态; b = 挡水墙完好;
                FloodWallStateEnum v0 = self.getFloodWallState();
                FloodWallStateEnum v1 = frame.getFloodWallIntact();
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("7bR3q8ZiBB6kkeWadWe");
                // 赋值（assignment.assign） a = 本对象.挡水墙顶高程; 写出: b → 过水控制高程;
                double v0 = self.getFloodWallTopElevation();
                double r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("nux1nWVlGtydRBqoZP3");
                // 赋值（assignment.assign） a = 本对象.入口底高程; 写出: b → 过水控制高程;
                double v0 = self.getInletFloorElevation();
                double r = v0;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("70rYyDbxM5uKI8nKUKV");
                // 赋值（assignment.assign） a = 本对象.完好时过水宽度; 写出: b → 过水宽度;
                double v0 = self.getIntactFlowWidth();
                double r = v0;
                frame.setOut3(r);
            }
            case 4 -> {
                fireLineProbe("UTA5cSN33pfqLdfr2ns");
                // 赋值（assignment.assign） a = 本对象.失效后过水宽度; 写出: b → 过水宽度;
                double v0 = self.getFailedFlowWidth();
                double r = v0;
                frame.setOut4(r);
            }
            case 5 -> {
                fireLineProbe("a1N71fAQ1CHaYCXhiFe");
                // 调用瞬时子过程：确定连接处过水情况 第一侧积水量 = 区域积水量; 第二侧水面高程 = 站内水面高程; 第一侧水面高程 = 区域水面高程; 第二侧积水量 = 本对象.站内积水量; 本步秒数 = 本步秒数; 过水控制高程 = 过水控制高程; 过水宽度 = 过水宽度; 流量系数 = 本对象.入口流量系数;
                frame.setChild5(new DetermineJunctionFlowProcessTask(ctx, frame.getFlowControlElevation(), frame.getFlowWidth(), this.areaWaterLevel, 0.0D, this.areaWaterVolume, false, self.getStationWaterVolume(), 0.0D, this.stationWaterLevel, this.stepSeconds, self.getInletFlowCoeff()));
                if (!spawnOrRun(5, frame.getChild5())) return false;
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
                frame.setFlowControlElevation(frame.getOut1());
                activate(3);
            }
            case 2 -> {
                frame.setFlowControlElevation(frame.getOut2());
                activate(4);
            }
            case 3 -> {
                frame.setFlowWidth(frame.getOut3());
                activate(5);
            }
            case 4 -> {
                frame.setFlowWidth(frame.getOut4());
                activate(5);
            }
            case 5 -> {
                // 回写：本步过水量 ← 确定连接处过水情况.本步过水量
                this.stepFlowVolume = frame.getChild5().getStepFlowVolume();
                // 回写：向站内流入 ← 确定连接处过水情况.第一侧流向第二侧
                this.flowsIntoStation = frame.getChild5().getFirstFlowsToSecond();
                // 回写：当前过水流量 ← 确定连接处过水情况.当前过水流量
                this.currentFlowRate = frame.getChild5().getCurrentFlowRate();
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
        slots.put("areaWaterLevel", GromaValues.encode(this.areaWaterLevel));
        slots.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        slots.put("stationWaterLevel", GromaValues.encode(this.stationWaterLevel));
        slots.put("areaWaterVolume", GromaValues.encode(this.areaWaterVolume));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("areaWaterLevel") != null) this.areaWaterLevel = GromaValues.toDouble(slots.get("areaWaterLevel"));
        if (slots.get("stepSeconds") != null) this.stepSeconds = GromaValues.toLong(slots.get("stepSeconds"));
        if (slots.get("stationWaterLevel") != null) this.stationWaterLevel = GromaValues.toDouble(slots.get("stationWaterLevel"));
        if (slots.get("areaWaterVolume") != null) this.areaWaterVolume = GromaValues.toDouble(slots.get("areaWaterVolume"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }

    @Override
    protected void onChildRestored(int pc, GromaProcessTask child) {
        if (pc == 5) {
            frame.setChild5((DetermineJunctionFlowProcessTask) child);
        }
    }
}

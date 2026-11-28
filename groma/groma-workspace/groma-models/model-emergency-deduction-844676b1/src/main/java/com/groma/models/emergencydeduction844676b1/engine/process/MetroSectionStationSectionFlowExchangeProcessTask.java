package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.StationSectionFlowExchangeRuntimeBO;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：车站与区间水流交换（方圆过程原名：车站与区间水流交换；宿主对象：地铁区间）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "HOI0UpBGLHJoNJjKYum",
        zhName = "车站与区间水流交换",
        kind = "PROCESS_TASK"
)
public final class MetroSectionStationSectionFlowExchangeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：地铁区间）
     */
    private final MetroSectionEntity self;

    /**
     * 非空输入形参：水流系数
     */
    private double flowCoeff;

    /**
     * 非空输入形参：连接处底高程
     */
    private double junctionFloorElevation;

    /**
     * 输出形参：本步过水量
     */
    private double stepFlowVolume;

    /**
     * 非空输入形参：端点车站
     */
    private StationEntity endStation;

    /**
     * 输出形参：当前过水流量
     */
    private double currentFlowRate;

    /**
     * 非空输入形参：有效过水宽度
     */
    private double effectiveFlowWidth;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final StationSectionFlowExchangeRuntimeBO frame = new StationSectionFlowExchangeRuntimeBO();

    public MetroSectionStationSectionFlowExchangeProcessTask(GromaWorldContext ctx,
            MetroSectionEntity self, double flowCoeff, double junctionFloorElevation,
            double stepFlowVolume, StationEntity endStation, double currentFlowRate,
            double effectiveFlowWidth) {
        super(ctx, self, "dv:designView_5mX6EkRtocaf9rXbKu1", "车站与区间水流交换", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 车站与区间水流交换");
        this.self = self;
        this.flowCoeff = flowCoeff;
        this.junctionFloorElevation = junctionFloorElevation;
        this.stepFlowVolume = stepFlowVolume;
        this.endStation = endStation;
        this.currentFlowRate = currentFlowRate;
        this.effectiveFlowWidth = effectiveFlowWidth;
    }

    private void setFlowCoeff(double value) {
        this.flowCoeff = value;
    }

    /**
     * 非空输入形参：水流系数
     */
    public double getFlowCoeff() {
        return this.flowCoeff;
    }

    private void setJunctionFloorElevation(double value) {
        this.junctionFloorElevation = value;
    }

    /**
     * 非空输入形参：连接处底高程
     */
    public double getJunctionFloorElevation() {
        return this.junctionFloorElevation;
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

    private void setEndStation(StationEntity value) {
        this.endStation = value;
    }

    /**
     * 非空输入形参：端点车站
     */
    public StationEntity getEndStation() {
        return this.endStation;
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

    private void setEffectiveFlowWidth(double value) {
        this.effectiveFlowWidth = value;
    }

    /**
     * 非空输入形参：有效过水宽度
     */
    public double getEffectiveFlowWidth() {
        return this.effectiveFlowWidth;
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
                fireLineProbe("8PLV744ptcd7Npkl9IQ");
                // 调用耗时子过程：水流交换间隔
                frame.setChild0(new MetroSectionFlowExchangeIntervalProcessTask(ctx, self, 0L));
                if (!spawnOrRun(0, frame.getChild0())) return false;
            }
            case 1 -> {
                fireLineProbe("sLcdro3WRBwiZXrNkhz");
                // 调用瞬时子过程：确定两端水位 第二侧水深 = 本对象.代表性水深; 第一侧底高程 = 端点车站.站内底高程; 第二侧底高程 = 本对象.代表底高程; 第一侧水深 = 端点车站.站内水深;
                frame.setChild1(new DetermineBothWaterLevelsProcessTask(ctx, this.endStation.getStationFloorElevation(), 0.0D, this.endStation.getStationWaterDepth(), 0.0D, self.getRepresentativeFloorElevation(), self.getRepresentativeDepth()));
                if (!spawnOrRun(1, frame.getChild1())) return false;
            }
            case 2 -> {
                fireLineProbe("GVM6ENrexsy6H18MzuZ");
                // 调用瞬时子过程：确定连接处过水情况 过水宽度 = 有效过水宽度; 本步秒数 = 本步秒数; 第一侧积水量 = 端点车站.站内积水量; 流量系数 = 水流系数; 第二侧积水量 = 本对象.积水量; 过水控制高程 = 连接处底高程; 第二侧水面高程 = 区间水面高程; 第一侧水面高程 = 车站水面高程;
                frame.setChild2(new DetermineJunctionFlowProcessTask(ctx, this.junctionFloorElevation, this.effectiveFlowWidth, frame.getStationWaterLevel(), 0.0D, this.endStation.getStationWaterVolume(), false, self.getWaterVolume(), 0.0D, frame.getSectionWaterLevel(), frame.getStepSeconds(), this.flowCoeff));
                if (!spawnOrRun(2, frame.getChild2())) return false;
            }
            case 3 -> {
                fireLineProbe("LwrdrvP8V6an0idXpVT");
                // 调用瞬时子过程：车站水流入区间 车站 = 端点车站; 本步过水量 = 本步过水量;
                if (!spawnOrRun(3, new MetroSectionStationFlowIntoSectionProcessTask(ctx, self, this.endStation, this.stepFlowVolume))) return false;
            }
            case 4 -> {
                fireLineProbe("XjgX74UPVUCtpKgC6v3");
                // 调用瞬时子过程：区间水回流车站 车站 = 端点车站; 本步过水量 = 本步过水量;
                if (!spawnOrRun(4, new MetroSectionSectionFlowBackStationProcessTask(ctx, self, this.endStation, this.stepFlowVolume))) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                // 回写：本步秒数 ← 水流交换间隔.本步秒数
                frame.setStepSeconds(frame.getChild0().getStepSeconds());
                activate(1);
            }
            case 1 -> {
                // 回写：车站水面高程 ← 确定两端水位.第一侧水面高程
                frame.setStationWaterLevel(frame.getChild1().getFirstSideWaterLevel());
                // 回写：区间水面高程 ← 确定两端水位.第二侧水面高程
                frame.setSectionWaterLevel(frame.getChild1().getSecondSideWaterLevel());
                activate(2);
            }
            case 2 -> {
                // 回写：当前过水流量 ← 确定连接处过水情况.当前过水流量
                this.currentFlowRate = frame.getChild2().getCurrentFlowRate();
                // 回写：本步过水量 ← 确定连接处过水情况.本步过水量
                this.stepFlowVolume = frame.getChild2().getStepFlowVolume();
                if (frame.getChild2().getFirstFlowsToSecond()) {
                    activate(3);
                }
                if (!(frame.getChild2().getFirstFlowsToSecond())) {
                    activate(4);
                }
            }
            case 3 -> {
                activate(0);
            }
            case 4 -> {
                activate(0);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.endStation == null) return false;
        if (!"积水".equals(BaseSimEntity.enumZhLabel((Enum<?>) (self.getWaterloggingState())))) {
            return false;
        }
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("flowCoeff", GromaValues.encode(this.flowCoeff));
        slots.put("junctionFloorElevation", GromaValues.encode(this.junctionFloorElevation));
        slots.put("endStation", GromaValues.encode(this.endStation));
        slots.put("effectiveFlowWidth", GromaValues.encode(this.effectiveFlowWidth));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("flowCoeff") != null) this.flowCoeff = GromaValues.toDouble(slots.get("flowCoeff"));
        if (slots.get("junctionFloorElevation") != null) this.junctionFloorElevation = GromaValues.toDouble(slots.get("junctionFloorElevation"));
        this.endStation = (StationEntity) GromaValues.decode(slots.get("endStation"), ctx);
        if (slots.get("effectiveFlowWidth") != null) this.effectiveFlowWidth = GromaValues.toDouble(slots.get("effectiveFlowWidth"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }

    @Override
    protected void onChildRestored(int pc, GromaProcessTask child) {
        if (pc == 0) {
            frame.setChild0((MetroSectionFlowExchangeIntervalProcessTask) child);
        }
        if (pc == 1) {
            frame.setChild1((DetermineBothWaterLevelsProcessTask) child);
        }
        if (pc == 2) {
            frame.setChild2((DetermineJunctionFlowProcessTask) child);
        }
    }
}

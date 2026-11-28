package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.AreaStationFlowExchangeRuntimeBO;
import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：区域与站内水流交换（方圆过程原名：区域与站内水流交换；宿主对象：车站）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "mXoawsnL7qgqcwNmvSw",
        zhName = "区域与站内水流交换",
        kind = "PROCESS_TASK"
)
public final class StationAreaStationFlowExchangeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：车站）
     */
    private final StationEntity self;

    /**
     * 输出形参：当前过水流量
     */
    private double currentFlowRate;

    /**
     * 输出形参：本步过水量
     */
    private double stepFlowVolume;

    /**
     * 非空输入形参：区域
     */
    private AreaEntity area;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final AreaStationFlowExchangeRuntimeBO frame = new AreaStationFlowExchangeRuntimeBO();

    public StationAreaStationFlowExchangeProcessTask(GromaWorldContext ctx, StationEntity self,
            double currentFlowRate, double stepFlowVolume, AreaEntity area) {
        super(ctx, self, "dv:designView_xmjSEU4SGv2otlq6mLP", "区域与站内水流交换", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 区域与站内水流交换");
        this.self = self;
        this.currentFlowRate = currentFlowRate;
        this.stepFlowVolume = stepFlowVolume;
        this.area = area;
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

    private void setStepFlowVolume(double value) {
        this.stepFlowVolume = value;
    }

    /**
     * 输出形参：本步过水量
     */
    public double getStepFlowVolume() {
        return this.stepFlowVolume;
    }

    private void setArea(AreaEntity value) {
        this.area = value;
    }

    /**
     * 非空输入形参：区域
     */
    public AreaEntity getArea() {
        return this.area;
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
                fireLineProbe("IsZGRVJokMx4pw0hKIl");
                // 调用耗时子过程：水流交换间隔
                frame.setChild0(new StationFlowExchangeIntervalProcessTask(ctx, self, 0L));
                if (!spawnOrRun(0, frame.getChild0())) return false;
            }
            case 1 -> {
                fireLineProbe("jDwLi5F8weGs66qPdwj");
                // 调用瞬时子过程：确定两端水位 第一侧底高程 = 区域.地面高程; 第一侧水深 = 区域.积水深度; 第二侧底高程 = 本对象.站内底高程; 第二侧水深 = 本对象.站内水深;
                frame.setChild1(new DetermineBothWaterLevelsProcessTask(ctx, this.area.getGroundElevation(), 0.0D, this.area.getWaterDepth(), 0.0D, self.getStationFloorElevation(), self.getStationWaterDepth()));
                if (!spawnOrRun(1, frame.getChild1())) return false;
            }
            case 2 -> {
                fireLineProbe("GdHNXkOmJZin6lwZFlL");
                // 调用瞬时子过程：挡水墙承受水压 本步秒数 = 本步秒数; 区域水面高程 = 区域水面高程;
                if (!spawnOrRun(2, new StationFloodWallBearPressureProcessTask(ctx, self, frame.getStepSeconds(), frame.getAreaWaterLevel()))) return false;
            }
            case 3 -> {
                fireLineProbe("EHkReKC1xIBqUa4mfn0");
                // 调用瞬时子过程：入口过水 区域水面高程 = 区域水面高程; 本步秒数 = 本步秒数; 站内水面高程 = 站内水面高程; 区域积水量 = 区域.积水量;
                frame.setChild3(new StationInletFlowProcessTask(ctx, self, false, frame.getAreaWaterLevel(), frame.getStepSeconds(), 0.0D, frame.getStationWaterLevel(), 0.0D, this.area.getWaterVolume()));
                if (!spawnOrRun(3, frame.getChild3())) return false;
            }
            case 4 -> {
                fireLineProbe("MqAGGkBFNUxljN2PaAV");
                // 调用瞬时子过程：地面水流入车站 本步过水量 = 本步过水量; 区域 = 区域;
                if (!spawnOrRun(4, new StationSurfaceFlowIntoStationProcessTask(ctx, self, this.area, this.stepFlowVolume))) return false;
            }
            case 5 -> {
                fireLineProbe("FPChA83re9n4pXiBnU2");
                // 调用瞬时子过程：站内水回流地面 本步过水量 = 本步过水量; 区域 = 区域;
                if (!spawnOrRun(5, new StationStationFlowBackSurfaceProcessTask(ctx, self, this.area, this.stepFlowVolume))) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                // 回写：本步秒数 ← 水流交换间隔.本步时长
                frame.setStepSeconds(frame.getChild0().getStepDuration());
                activate(1);
            }
            case 1 -> {
                // 回写：区域水面高程 ← 确定两端水位.第一侧水面高程
                frame.setAreaWaterLevel(frame.getChild1().getFirstSideWaterLevel());
                // 回写：站内水面高程 ← 确定两端水位.第二侧水面高程
                frame.setStationWaterLevel(frame.getChild1().getSecondSideWaterLevel());
                activate(2);
            }
            case 2 -> {
                activate(3);
            }
            case 3 -> {
                // 回写：本步过水量 ← 入口过水.本步过水量
                this.stepFlowVolume = frame.getChild3().getStepFlowVolume();
                // 回写：当前过水流量 ← 入口过水.当前过水流量
                this.currentFlowRate = frame.getChild3().getCurrentFlowRate();
                if (frame.getChild3().getFlowsIntoStation()) {
                    activate(4);
                }
                if (!(frame.getChild3().getFlowsIntoStation())) {
                    activate(5);
                }
            }
            case 4 -> {
                activate(0);
            }
            case 5 -> {
                activate(0);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.area == null) return false;
        if (!"积水".equals(BaseSimEntity.enumZhLabel((Enum<?>) (self.getWaterloggingState())))) {
            return false;
        }
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("area", GromaValues.encode(this.area));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.area = (AreaEntity) GromaValues.decode(slots.get("area"), ctx);
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }

    @Override
    protected void onChildRestored(int pc, GromaProcessTask child) {
        if (pc == 0) {
            frame.setChild0((StationFlowExchangeIntervalProcessTask) child);
        }
        if (pc == 1) {
            frame.setChild1((DetermineBothWaterLevelsProcessTask) child);
        }
        if (pc == 3) {
            frame.setChild3((StationInletFlowProcessTask) child);
        }
    }
}

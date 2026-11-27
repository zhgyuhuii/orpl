package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.SectionRunRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：区间行驶（方圆过程原名：区间行驶；宿主对象：列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 1，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "u9xyHBv1BSldBPVP0Q7",
        zhName = "区间行驶",
        kind = "PROCESS_TASK"
)
public final class TrainSectionRunProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 1, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：列车）
     */
    private final TrainEntity self;

    /**
     * 非空输入形参：区间与区间内列车
     */
    private SectionWithTrainEntity sectionWithTrain;

    /**
     * 双向形参：当前线路里程
     */
    private double currentLineMileage;

    /**
     * 非空输入形参：是否沿里程增大方向
     */
    private boolean alongIncreasingMileage;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final SectionRunRuntimeBO frame = new SectionRunRuntimeBO();

    public TrainSectionRunProcessTask(GromaWorldContext ctx, TrainEntity self,
            SectionWithTrainEntity sectionWithTrain, double currentLineMileage,
            boolean alongIncreasingMileage) {
        super(ctx, self, "dv:designView_XLrKPtiJlmUbySIChuF", "区间行驶", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 区间行驶");
        this.self = self;
        this.sectionWithTrain = sectionWithTrain;
        this.currentLineMileage = currentLineMileage;
        this.alongIncreasingMileage = alongIncreasingMileage;
    }

    private void setSectionWithTrain(SectionWithTrainEntity value) {
        this.sectionWithTrain = value;
    }

    /**
     * 非空输入形参：区间与区间内列车
     */
    public SectionWithTrainEntity getSectionWithTrain() {
        return this.sectionWithTrain;
    }

    private void setCurrentLineMileage(double value) {
        this.currentLineMileage = value;
    }

    /**
     * 双向形参：当前线路里程
     */
    public double getCurrentLineMileage() {
        return this.currentLineMileage;
    }

    private void setAlongIncreasingMileage(boolean value) {
        this.alongIncreasingMileage = value;
    }

    /**
     * 非空输入形参：是否沿里程增大方向
     */
    public boolean getAlongIncreasingMileage() {
        return this.alongIncreasingMileage;
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
                // 赋值（assignment.assign） a = 本对象.行驶速度; 写出: b → 本对象.当前速度;
                double v0 = self.getTravelSpeed();
                double r = v0;
                frame.setOut0(r);
            }
            case 1 -> {
                // 清空（assignment.clear） a = 区间与区间内列车.已行驶距离; 写出: a → 区间与区间内列车.已行驶距离;
                this.sectionWithTrain.setTravelledDistance(0.0D, ctx);
            }
            case 2 -> {
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 开始时刻;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut2(r);
            }
            case 3 -> {
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 当前时刻;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut3(r);
            }
            case 4 -> {
                // 减（arithmetic.minus） a = 当前时刻; b = 开始时刻; 写出: c → 行驶间隔;
                double v0 = frame.getCurrentTime();
                double v1 = frame.getStartTime();
                double r = v0 - v1;
                frame.setOut4(r);
            }
            case 5 -> {
                // 调用瞬时子过程：沿区间前进 行驶间隔 = 行驶间隔; 地铁区间 = 区间与区间内列车.区间; 已行驶距离 = 区间与区间内列车.已行驶距离; 目标车站 = 区间与区间内列车.目标车站; 是否沿里程增大方向 = 是否沿里程增大方向; 当前线路里程 = 当前线路里程;
                frame.setChild5(new TrainAdvanceAlongSectionProcessTask(ctx, self, this.sectionWithTrain.getSection(), this.sectionWithTrain.getTargetStation(), frame.getTravelInterval(), this.sectionWithTrain.getTravelledDistance(), this.alongIncreasingMileage, this.currentLineMileage));
                if (!spawnOrRun(5, frame.getChild5())) return false;
            }
            case 6 -> {
                // 大于等于（compare.ge） a = 区间与区间内列车.已行驶距离; b = 区间与区间内列车.区间.长度;
                double v0 = this.sectionWithTrain.getTravelledDistance();
                double v1 = this.sectionWithTrain.getSection().getLength();
                boolean r = v0 >= v1;
                frame.setOut6(r);
            }
            case 7 -> {
                // 赋值（assignment.assign） a = 区间与区间内列车.目标车站.位置; 写出: b → 本对象.当前位置;
                GeoPointEntity v0 = this.sectionWithTrain.getTargetStation().getPosition();
                GeoPointEntity r = v0.copy(ctx);
                frame.setOut7(r);
            }
            case 8 -> {
                // 赋值（assignment.assign） a = 到达下一车战; 写出: b → 区间与区间内列车.是否到达目标车站;
                boolean v0 = true;
                boolean r = v0;
                frame.setOut8(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                self.setCurrentSpeed(frame.getOut0(), ctx);
                activate(1);
            }
            case 1 -> {
                activate(2);
            }
            case 2 -> {
                frame.setStartTime(frame.getOut2());
                activate(3);
            }
            case 3 -> {
                frame.setCurrentTime(frame.getOut3());
                activate(4);
            }
            case 4 -> {
                frame.setTravelInterval(frame.getOut4());
                activate(5);
            }
            case 5 -> {
                // 回写：区间与区间内列车.已行驶距离 ← 沿区间前进.已行驶距离
                this.sectionWithTrain.setTravelledDistance(frame.getChild5().getTravelledDistance(), ctx);
                // 回写：当前线路里程 ← 沿区间前进.当前线路里程
                this.currentLineMileage = frame.getChild5().getCurrentLineMileage();
                activate(6);
            }
            case 6 -> {
                if (!(frame.getOut6())) {
                    activate(2);
                }
                if (frame.getOut6()) {
                    activate(7);
                }
            }
            case 7 -> {
                self.setCurrentPosition(frame.getOut7(), ctx);
                activate(8);
            }
            case 8 -> {
                this.sectionWithTrain.setArrivedTargetStation(frame.getOut8(), ctx);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.sectionWithTrain == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("sectionWithTrain", GromaValues.encode(this.sectionWithTrain));
        slots.put("currentLineMileage", GromaValues.encode(this.currentLineMileage));
        slots.put("alongIncreasingMileage", GromaValues.encode(this.alongIncreasingMileage));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.sectionWithTrain = (SectionWithTrainEntity) GromaValues.decode(slots.get("sectionWithTrain"), ctx);
        if (slots.get("currentLineMileage") != null) this.currentLineMileage = GromaValues.toDouble(slots.get("currentLineMileage"));
        if (slots.get("alongIncreasingMileage") != null) this.alongIncreasingMileage = GromaValues.toBoolean(slots.get("alongIncreasingMileage"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }

    @Override
    protected void onChildRestored(int pc, GromaProcessTask child) {
        if (pc == 5) {
            frame.setChild5((TrainAdvanceAlongSectionProcessTask) child);
        }
    }
}

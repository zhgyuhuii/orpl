package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.StoppedAtStationRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：停站（方圆过程原名：停站；宿主对象：列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 1，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "1rbne3IC1GpVoTNMwVu",
        zhName = "停站",
        kind = "PROCESS_TASK"
)
public final class TrainStoppedAtStationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 1, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：列车）
     */
    private final TrainEntity self;

    /**
     * 非空输入形参：车站与停靠列车
     */
    private StationWithParkedTrainEntity stationWithParkedTrain;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final StoppedAtStationRuntimeBO frame = new StoppedAtStationRuntimeBO();

    public TrainStoppedAtStationProcessTask(GromaWorldContext ctx, TrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain) {
        super(ctx, self, "dv:designView_eHq1YpIkoLxwHKywWis", "停站", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 停站");
        this.self = self;
        this.stationWithParkedTrain = stationWithParkedTrain;
    }

    private void setStationWithParkedTrain(StationWithParkedTrainEntity value) {
        this.stationWithParkedTrain = value;
    }

    /**
     * 非空输入形参：车站与停靠列车
     */
    public StationWithParkedTrainEntity getStationWithParkedTrain() {
        return this.stationWithParkedTrain;
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
                // 赋值（assignment.assign） a = 车站与停靠列车.车站.位置; 写出: b → 本对象.初始位置;
                GeoPointEntity v0 = this.stationWithParkedTrain.getStation().getPosition();
                GeoPointEntity r = v0.copy(ctx);
                frame.setOut0(r);
            }
            case 1 -> {
                // 赋值（assignment.assign） a = 本对象.初始位置; 写出: b → 本对象.当前位置;
                GeoPointEntity v0 = self.getInitialPosition();
                GeoPointEntity r = v0.copy(ctx);
                frame.setOut1(r);
            }
            case 2 -> {
                // 赋值（assignment.assign） a = 仿真时间.时间日期; 写出: b → 车站与停靠列车.实际到站时刻;
                long v0 = ctx.currentWorldTimeMs();
                long r = v0;
                frame.setOut2(r);
            }
            case 3 -> {
                // 大于（compare.gt） a = 车站与停靠列车.规定停站时长; b = 等待时长;
                double v0 = this.stationWithParkedTrain.getPrescribedStopDuration();
                double v1 = frame.getWaitingDuration();
                boolean r = v0 > v1;
                frame.setOut3(r);
            }
            case 4 -> {
                // 赋值（assignment.assign） a = 允许离站; 写出: b → 车站与停靠列车.离站是否允许;
                boolean v0 = frame.getDepartureAllowed();
                boolean r = v0;
                frame.setOut4(r);
            }
            case 5 -> {
                // 递增（automatic.inc） a = 等待时长; 写出: a → 等待时长;
                long v0 = frame.getWaitingDuration();
                frame.setWaitingDuration(v0 + 1L);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                self.setInitialPosition(frame.getOut0(), ctx);
                activate(1);
            }
            case 1 -> {
                self.setCurrentPosition(frame.getOut1(), ctx);
                activate(2);
            }
            case 2 -> {
                this.stationWithParkedTrain.setActualArrivalTime(frame.getOut2(), ctx);
                activate(3);
            }
            case 3 -> {
                if (!(frame.getOut3())) {
                    activate(4);
                }
                if (frame.getOut3()) {
                    activate(5);
                }
            }
            case 4 -> {
                this.stationWithParkedTrain.setDepartureAllowed(frame.getOut4(), ctx);
            }
            case 5 -> {
                activate(3);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.stationWithParkedTrain == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("stationWithParkedTrain", GromaValues.encode(this.stationWithParkedTrain));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.stationWithParkedTrain = (StationWithParkedTrainEntity) GromaValues.decode(slots.get("stationWithParkedTrain"), ctx);
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.DepartRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：离站（方圆过程原名：离站；宿主对象：列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 1，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "wjMiAj779lB5502MLmW",
        zhName = "离站",
        kind = "PROCESS_TASK"
)
public final class TrainDepartProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 1, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

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
    private final DepartRuntimeBO frame = new DepartRuntimeBO();

    public TrainDepartProcessTask(GromaWorldContext ctx, TrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain) {
        super(ctx, self, "dv:designView_767z0mfmOaNst2dPwlG", "离站", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 离站");
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
                // 赋值（assignment.assign） a = 仿真时间.时间日期; 写出: b → 车站与停靠列车.实际离站时刻;
                long v0 = ctx.currentWorldTimeMs();
                long r = v0;
                frame.setOut0(r);
            }
            case 1 -> {
                // 赋值（assignment.assign） a = 本对象.行驶速度; 写出: b → 本对象.当前速度;
                double v0 = self.getTravelSpeed();
                double r = v0;
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
                this.stationWithParkedTrain.setActualDepartureTime(frame.getOut0(), ctx);
                activate(1);
            }
            case 1 -> {
                self.setCurrentSpeed(frame.getOut1(), ctx);
                // 输出转换：本对象.列车状态 → 行驶
                self.setTrainState(TrainStateEnum.RUNNING, ctx);
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

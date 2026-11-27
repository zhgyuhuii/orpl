package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：进站（方圆过程原名：进站；宿主对象：列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 1，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "zkevt3isbPbfdXv9T9I",
        zhName = "进站",
        kind = "PROCESS_TASK"
)
public final class TrainEnterStationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 1, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：列车）
     */
    private final TrainEntity self;

    public TrainEnterStationProcessTask(GromaWorldContext ctx, TrainEntity self) {
        super(ctx, self, "dv:designView_joMlLooi2InkPOjk59H", "进站", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 进站");
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
                // 清空（assignment.clear） a = 本对象.当前速度; 写出: a → 本对象.当前速度;
                self.setCurrentSpeed(0.0D, ctx);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                // 输出转换：本对象.列车状态 → 停站
                self.setTrainState(TrainStateEnum.STOPPED_AT_STATION, ctx);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
    }
}

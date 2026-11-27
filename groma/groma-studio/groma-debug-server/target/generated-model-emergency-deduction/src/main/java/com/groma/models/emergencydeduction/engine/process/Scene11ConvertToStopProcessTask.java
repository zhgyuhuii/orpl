package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：1转换为停站（方圆过程原名：1转换为停站；宿主对象：场景1）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "hzatYjpxW7rcjt42TK2",
        zhName = "1转换为停站",
        kind = "PROCESS_TASK"
)
public final class Scene11ConvertToStopProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：场景1）
     */
    private final Scene1Entity self;

    public Scene11ConvertToStopProcessTask(GromaWorldContext ctx, Scene1Entity self) {
        super(ctx, self, "dv:designView_vFHwnod6GqHo3Bde4P5", "1转换为停站", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 1转换为停站");
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
                fireLineProbe("iEs6LC1816xoczCss5m");
                // 调用耗时子过程：过程1
                if (!spawnOrRun(0, new Scene1Procedure1ProcessTask(ctx, self))) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                // 输出转换：本对象.列车状态1 → 停站
                self.setTrainState1(TrainStateEnum.STOPPED_AT_STATION, ctx);
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

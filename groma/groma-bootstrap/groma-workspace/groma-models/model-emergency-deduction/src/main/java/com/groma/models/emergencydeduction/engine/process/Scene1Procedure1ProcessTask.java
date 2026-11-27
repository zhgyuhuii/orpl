package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：过程1（方圆过程原名：过程1；宿主对象：场景1）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "QQMUU7DuY51tlC5lIlP",
        zhName = "过程1",
        kind = "PROCESS_TASK"
)
public final class Scene1Procedure1ProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：场景1）
     */
    private final Scene1Entity self;

    public Scene1Procedure1ProcessTask(GromaWorldContext ctx, Scene1Entity self) {
        super(ctx, self, "ent:QQMUU7DuY51tlC5lIlP", "过程1", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 过程1");
        this.self = self;
    }

    @Override
    protected boolean hasFlow() {
        return false;
    }

    @Override
    protected int[] entryNodes() {
        return new int[0];
    }

    @Override
    protected void execLeafBody() {
    }

    @Override
    protected boolean execNode(int pc) {
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        if (pc == -1) return;
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

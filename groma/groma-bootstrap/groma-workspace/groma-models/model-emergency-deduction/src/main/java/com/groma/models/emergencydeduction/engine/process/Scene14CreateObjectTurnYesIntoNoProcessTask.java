package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.CreateObjectTurnYesIntoNo4RuntimeBO;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：4生成对象并把是变成否（方圆过程原名：4生成对象并把是变成否；宿主对象：场景1）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "llOioPfUfaWKZf5nX3v",
        zhName = "4生成对象并把是变成否",
        kind = "PROCESS_TASK"
)
public final class Scene14CreateObjectTurnYesIntoNoProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：场景1）
     */
    private final Scene1Entity self;

    /**
     * 输出形参：文本1
     */
    private boolean text1;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final CreateObjectTurnYesIntoNo4RuntimeBO frame = new CreateObjectTurnYesIntoNo4RuntimeBO();

    public Scene14CreateObjectTurnYesIntoNoProcessTask(GromaWorldContext ctx, Scene1Entity self,
            boolean text1) {
        super(ctx, self, "dv:designView_miDwXHXHR407mRDMPzO", "4生成对象并把是变成否", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 4生成对象并把是变成否");
        this.self = self;
        this.text1 = text1;
    }

    private void setText1(boolean value) {
        this.text1 = value;
    }

    /**
     * 输出形参：文本1
     */
    public boolean getText1() {
        return this.text1;
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
                // 赋值（assignment.assign） a = 定值b2044753c2c9410bbaf95842e538fa20; 写出: b → 文本1;
                boolean v0 = false;
                boolean r = v0;
                frame.setOut0(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                this.text1 = frame.getOut0();
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
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}

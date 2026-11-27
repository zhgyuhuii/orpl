package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FileExistsRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.FileTypeEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：判断文件是否存在（方圆过程原名：判断文件是否存在；宿主对象：文件类型）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "ae6ce890734549a29aa10b5b358bab99",
        zhName = "判断文件是否存在",
        kind = "PROCESS_TASK"
)
public final class FileTypeFileExistsProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：文件类型）
     */
    private final FileTypeEntity self;

    /**
     * 输出形参：是否存在
     */
    private boolean exists;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FileExistsRuntimeBO frame = new FileExistsRuntimeBO();

    public FileTypeFileExistsProcessTask(GromaWorldContext ctx, FileTypeEntity self,
            boolean exists) {
        super(ctx, self, "dv:designView_04fb48efd78444e3bfc948e0fa91c1f3", "判断文件是否存在", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 判断文件是否存在");
        this.self = self;
        this.exists = exists;
    }

    private void setExists(boolean value) {
        this.exists = value;
    }

    /**
     * 输出形参：是否存在
     */
    public boolean getExists() {
        return this.exists;
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
                // 调用瞬时子过程：判断文件是否存在 路径/标识 = 本对象.路径;
                frame.setChild0(new FileServiceFileExistsProcessTask(ctx, frame.getFileService1(), self.getPath(), false, false));
                if (!spawnOrRun(0, frame.getChild0())) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                // 回写：是否成功 ← 判断文件是否存在.是否成功
                frame.setSucceeded(frame.getChild0().getSucceeded());
                // 回写：是否存在 ← 判断文件是否存在.是否存在
                this.exists = frame.getChild0().getExists();
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

    @Override
    protected void onChildRestored(int pc, GromaProcessTask child) {
        if (pc == 0) {
            frame.setChild0((FileServiceFileExistsProcessTask) child);
        }
    }
}

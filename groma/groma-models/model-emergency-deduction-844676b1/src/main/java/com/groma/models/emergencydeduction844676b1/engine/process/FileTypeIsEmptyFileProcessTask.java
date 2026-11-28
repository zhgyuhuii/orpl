package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.IsEmptyFileRuntimeBO;
import com.groma.models.emergencydeduction844676b1.model.entity.FileTypeEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：判断是否为空文件（方圆过程原名：判断是否为空文件；宿主对象：文件类型）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "4da6a71b5a2943d887fd9249ef48d05c",
        zhName = "判断是否为空文件",
        kind = "PROCESS_TASK"
)
public final class FileTypeIsEmptyFileProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：文件类型）
     */
    private final FileTypeEntity self;

    /**
     * 输出形参：是否为空
     */
    private boolean empty;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final IsEmptyFileRuntimeBO frame = new IsEmptyFileRuntimeBO();

    public FileTypeIsEmptyFileProcessTask(GromaWorldContext ctx, FileTypeEntity self,
            boolean empty) {
        super(ctx, self, "dv:designView_218a29d6cd004955b234ad6b1bafc7b6", "判断是否为空文件", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 判断是否为空文件");
        this.self = self;
        this.empty = empty;
    }

    private void setEmpty(boolean value) {
        this.empty = value;
    }

    /**
     * 输出形参：是否为空
     */
    public boolean getEmpty() {
        return this.empty;
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
                fireLineProbe("89ae50e7f7bd40148839c15f31183531");
                // 调用瞬时子过程：判断文件是否为空 路径/标识 = 本对象.路径;
                frame.setChild0(new FileServiceIsFileEmptyProcessTask(ctx, frame.getFileService1(), self.getPath(), false, false));
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
                // 回写：是否成功 ← 判断文件是否为空.是否成功
                frame.setSucceeded(frame.getChild0().getSucceeded());
                // 回写：是否为空 ← 判断文件是否为空.是否为空
                this.empty = frame.getChild0().getEmpty();
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
            frame.setChild0((FileServiceIsFileEmptyProcessTask) child);
        }
    }
}

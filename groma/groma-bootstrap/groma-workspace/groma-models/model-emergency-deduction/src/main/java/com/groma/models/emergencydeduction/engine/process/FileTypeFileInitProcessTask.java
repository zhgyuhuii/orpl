package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FileInitRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.FileTypeEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：文件初始化（方圆过程原名：文件初始化；宿主对象：文件类型）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "bf6717304c85425eb4e697d24ab6d95a",
        zhName = "文件初始化",
        kind = "PROCESS_TASK"
)
public final class FileTypeFileInitProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：文件类型）
     */
    private final FileTypeEntity self;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FileInitRuntimeBO frame = new FileInitRuntimeBO();

    public FileTypeFileInitProcessTask(GromaWorldContext ctx, FileTypeEntity self) {
        super(ctx, self, "dv:designView_659efee68fd74a0a86fa968e044a59aa", "文件初始化", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 文件初始化");
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
                // 调用瞬时子过程：文件初始化 路径/标识 = 本对象.路径;
                frame.setChild0(new FileServiceFileInitProcessTask(ctx, frame.getFileService(), self.getPath(), false, null, ""));
                if (!spawnOrRun(0, frame.getChild0())) return false;
            }
            case 1 -> {
                // 赋值（assignment.assign） a = 文件信息.名称; 写出: b → 本对象.名称;
                String v0 = frame.getFileInfo().getName();
                String r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                // 赋值（assignment.assign） a = 文件信息.类型; 写出: b → 本对象.类型;
                String v0 = frame.getFileInfo().getType();
                String r = v0;
                frame.setOut2(r);
            }
            case 3 -> {
                // 赋值（assignment.assign） a = 文件信息.大小; 写出: b → 本对象.大小;
                double v0 = frame.getFileInfo().getSize();
                double r = v0;
                frame.setOut3(r);
            }
            case 4 -> {
                // 赋值（assignment.assign） a = 文件信息.创建时间; 写出: b → 本对象.创建时间;
                long v0 = frame.getFileInfo().getCreateTime();
                long r = v0;
                frame.setOut4(r);
            }
            case 5 -> {
                // 赋值（assignment.assign） a = 文件信息.修改时间; 写出: b → 本对象.修改时间;
                long v0 = frame.getFileInfo().getModifyTime();
                long r = v0;
                frame.setOut5(r);
            }
            case 6 -> {
                // 赋值（assignment.assign） a = 文件信息.哈希值; 写出: b → 本对象.哈希值;
                String v0 = frame.getFileInfo().getHashValue();
                String r = v0;
                frame.setOut6(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                // 回写：文件信息 ← 文件初始化.文件信息
                frame.setFileInfo(frame.getChild0().getFileInfo());
                // 回写：错误信息 ← 文件初始化.错误信息
                frame.setErrorMessage(frame.getChild0().getErrorMessage());
                // 回写：是否成功 ← 文件初始化.是否成功
                frame.setSucceeded(frame.getChild0().getSucceeded());
                activate(1);
            }
            case 1 -> {
                self.setName(frame.getOut1(), ctx);
                activate(2);
            }
            case 2 -> {
                self.setType(frame.getOut2(), ctx);
                activate(3);
            }
            case 3 -> {
                self.setSize(frame.getOut3(), ctx);
                activate(4);
            }
            case 4 -> {
                self.setCreateTime(frame.getOut4(), ctx);
                activate(5);
            }
            case 5 -> {
                self.setModifyTime(frame.getOut5(), ctx);
                activate(6);
            }
            case 6 -> {
                self.setHashValue(frame.getOut6(), ctx);
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
            frame.setChild0((FileServiceFileInitProcessTask) child);
        }
    }
}

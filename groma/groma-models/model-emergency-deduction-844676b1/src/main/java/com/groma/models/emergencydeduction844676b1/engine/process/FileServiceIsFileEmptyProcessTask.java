package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.FileServiceEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：判断文件是否为空（方圆过程原名：判断文件是否为空；宿主对象：文件服务）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "IeK32FL7HMYxvSd4PnP",
        zhName = "判断文件是否为空",
        kind = "PROCESS_TASK"
)
public final class FileServiceIsFileEmptyProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：文件服务）
     */
    private final FileServiceEntity self;

    /**
     * 非空输入形参：路径/标识
     */
    private String pathIdentifier;

    /**
     * 输出形参：是否成功
     */
    private boolean succeeded;

    /**
     * 输出形参：是否为空
     */
    private boolean empty;

    public FileServiceIsFileEmptyProcessTask(GromaWorldContext ctx, FileServiceEntity self,
            String pathIdentifier, boolean succeeded, boolean empty) {
        super(ctx, self, "ent:IeK32FL7HMYxvSd4PnP", "判断文件是否为空", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 判断文件是否为空");
        this.self = self;
        this.pathIdentifier = pathIdentifier;
        this.succeeded = succeeded;
        this.empty = empty;
    }

    private void setPathIdentifier(String value) {
        this.pathIdentifier = value;
    }

    /**
     * 非空输入形参：路径/标识
     */
    public String getPathIdentifier() {
        return this.pathIdentifier;
    }

    private void setSucceeded(boolean value) {
        this.succeeded = value;
    }

    /**
     * 输出形参：是否成功
     */
    public boolean getSucceeded() {
        return this.succeeded;
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
        slots.put("pathIdentifier", GromaValues.encode(this.pathIdentifier));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.pathIdentifier = GromaValues.toText(slots.get("pathIdentifier"));
    }
}

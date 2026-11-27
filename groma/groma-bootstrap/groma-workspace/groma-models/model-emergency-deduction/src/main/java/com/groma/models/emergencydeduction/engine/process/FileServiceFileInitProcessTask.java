package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction.model.entity.FileServiceEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：文件初始化（方圆过程原名：文件初始化；宿主对象：文件服务）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "Ne04mUfbnKpBEjTlARK",
        zhName = "文件初始化",
        kind = "PROCESS_TASK"
)
public final class FileServiceFileInitProcessTask extends GromaProcessTask {
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
     * 输出形参：文件信息
     */
    private FileInfoEntity fileInfo;

    /**
     * 输出形参：错误信息
     */
    private String errorMessage;

    public FileServiceFileInitProcessTask(GromaWorldContext ctx, FileServiceEntity self,
            String pathIdentifier, boolean succeeded, FileInfoEntity fileInfo,
            String errorMessage) {
        super(ctx, self, "ent:Ne04mUfbnKpBEjTlARK", "文件初始化", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 文件初始化");
        this.self = self;
        this.pathIdentifier = pathIdentifier;
        this.succeeded = succeeded;
        this.fileInfo = fileInfo;
        this.errorMessage = errorMessage;
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

    private void setFileInfo(FileInfoEntity value) {
        this.fileInfo = value;
    }

    /**
     * 输出形参：文件信息
     */
    public FileInfoEntity getFileInfo() {
        return this.fileInfo;
    }

    private void setErrorMessage(String value) {
        this.errorMessage = value;
    }

    /**
     * 输出形参：错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
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

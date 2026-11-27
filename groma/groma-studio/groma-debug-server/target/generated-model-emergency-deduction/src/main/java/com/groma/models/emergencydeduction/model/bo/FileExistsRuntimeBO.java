package com.groma.models.emergencydeduction.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.FileServiceFileExistsProcessTask;
import com.groma.models.emergencydeduction.model.entity.FileServiceEntity;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：判断文件是否存在）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "ae6ce890734549a29aa10b5b358bab99#bo",
        zhName = "判断文件是否存在运行帧",
        kind = "RUNTIME_BO"
)
public final class FileExistsRuntimeBO {
    /**
     * 局部量（方圆原名：文件服务1）
     */
    private FileServiceEntity fileService1 = null;

    /**
     * 局部量（方圆原名：是否成功）
     */
    private boolean succeeded = false;

    /**
     * 子过程句柄：判断文件是否存在
     */
    private FileServiceFileExistsProcessTask child0;

    /**
     * 局部量：文件服务1
     */
    public FileServiceEntity getFileService1() {
        return this.fileService1;
    }

    public void setFileService1(FileServiceEntity value) {
        this.fileService1 = value;
    }

    /**
     * 局部量：是否成功
     */
    public boolean getSucceeded() {
        return this.succeeded;
    }

    public void setSucceeded(boolean value) {
        this.succeeded = value;
    }

    /**
     * 子过程句柄：判断文件是否存在
     */
    public FileServiceFileExistsProcessTask getChild0() {
        return this.child0;
    }

    public void setChild0(FileServiceFileExistsProcessTask value) {
        this.child0 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("fileService1", GromaValues.encode(this.fileService1));
        state.put("succeeded", GromaValues.encode(this.succeeded));
        state.put("child0", this.child0 == null ? "" : this.child0.getProcessInstanceId());
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.fileService1 = (FileServiceEntity) GromaValues.decode(state.get("fileService1"), ctx);
        this.succeeded = (boolean) GromaValues.decode(state.get("succeeded"), ctx);
    }
}

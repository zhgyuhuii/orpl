package com.groma.models.emergencydeduction844676b1.model.bo;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceFileInitProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FileServiceEntity;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 过程运行帧（方圆过程原名：文件初始化）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收
 */
@GromaMeta(
        id = "bf6717304c85425eb4e697d24ab6d95a#bo",
        zhName = "文件初始化运行帧",
        kind = "RUNTIME_BO"
)
public final class FileInitRuntimeBO {
    /**
     * 局部量（方圆原名：文件服务）
     */
    private FileServiceEntity fileService = null;

    /**
     * 局部量（方圆原名：文件信息）
     */
    private FileInfoEntity fileInfo = null;

    /**
     * 局部量（方圆原名：错误信息）
     */
    private String errorMessage = "";

    /**
     * 局部量（方圆原名：是否成功）
     */
    private boolean succeeded = false;

    /**
     * 子过程句柄：文件初始化
     */
    private FileServiceFileInitProcessTask child0;

    /**
     * 节点输出：赋值
     */
    private String out1;

    /**
     * 节点输出：赋值
     */
    private String out2;

    /**
     * 节点输出：赋值
     */
    private Double out3;

    /**
     * 节点输出：赋值
     */
    private Long out4;

    /**
     * 节点输出：赋值
     */
    private Long out5;

    /**
     * 节点输出：赋值
     */
    private String out6;

    /**
     * 局部量：文件服务
     */
    public FileServiceEntity getFileService() {
        return this.fileService;
    }

    public void setFileService(FileServiceEntity value) {
        this.fileService = value;
    }

    /**
     * 局部量：文件信息
     */
    public FileInfoEntity getFileInfo() {
        return this.fileInfo;
    }

    public void setFileInfo(FileInfoEntity value) {
        this.fileInfo = value;
    }

    /**
     * 局部量：错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String value) {
        this.errorMessage = value;
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
     * 子过程句柄：文件初始化
     */
    public FileServiceFileInitProcessTask getChild0() {
        return this.child0;
    }

    public void setChild0(FileServiceFileInitProcessTask value) {
        this.child0 = value;
    }

    /**
     * 节点输出：赋值
     */
    public String getOut1() {
        return this.out1;
    }

    public void setOut1(String value) {
        this.out1 = value;
    }

    /**
     * 节点输出：赋值
     */
    public String getOut2() {
        return this.out2;
    }

    public void setOut2(String value) {
        this.out2 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Double getOut3() {
        return this.out3;
    }

    public void setOut3(Double value) {
        this.out3 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Long getOut4() {
        return this.out4;
    }

    public void setOut4(Long value) {
        this.out4 = value;
    }

    /**
     * 节点输出：赋值
     */
    public Long getOut5() {
        return this.out5;
    }

    public void setOut5(Long value) {
        this.out5 = value;
    }

    /**
     * 节点输出：赋值
     */
    public String getOut6() {
        return this.out6;
    }

    public void setOut6(String value) {
        this.out6 = value;
    }

    public Map<String, Object> exportState() {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("fileService", GromaValues.encode(this.fileService));
        state.put("fileInfo", GromaValues.encode(this.fileInfo));
        state.put("errorMessage", GromaValues.encode(this.errorMessage));
        state.put("succeeded", GromaValues.encode(this.succeeded));
        state.put("child0", this.child0 == null ? "" : this.child0.getProcessInstanceId());
        state.put("out1", GromaValues.encode(this.out1));
        state.put("out2", GromaValues.encode(this.out2));
        state.put("out3", GromaValues.encode(this.out3));
        state.put("out4", GromaValues.encode(this.out4));
        state.put("out5", GromaValues.encode(this.out5));
        state.put("out6", GromaValues.encode(this.out6));
        return state;
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state, GromaWorldContext ctx) {
        this.fileService = (FileServiceEntity) GromaValues.decode(state.get("fileService"), ctx);
        this.fileInfo = (FileInfoEntity) GromaValues.decode(state.get("fileInfo"), ctx);
        this.errorMessage = state.get("errorMessage") == null ? null : GromaValues.toText(state.get("errorMessage"));
        this.succeeded = (boolean) GromaValues.decode(state.get("succeeded"), ctx);
        this.out1 = state.get("out1") == null ? null : GromaValues.toText(state.get("out1"));
        this.out2 = state.get("out2") == null ? null : GromaValues.toText(state.get("out2"));
        this.out3 = (Double) GromaValues.decode(state.get("out3"), ctx);
        this.out4 = (Long) GromaValues.decode(state.get("out4"), ctx);
        this.out5 = (Long) GromaValues.decode(state.get("out5"), ctx);
        this.out6 = state.get("out6") == null ? null : GromaValues.toText(state.get("out6"));
    }
}

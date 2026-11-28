package com.groma.models.emergencydeduction844676b1.service;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FileServiceEntity;
import java.lang.String;

/**
 * 文件服务 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IFileServiceService {
    /**
     * 文件服务（方圆过程原名：读取文本内容）—— 瞬时算子：同步执行完毕
     */
    boolean readTextContent(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean succeeded, String fileContent);

    /**
     * 文件服务（方圆过程原名：判断文件是否为空）—— 瞬时算子：同步执行完毕
     */
    boolean isFileEmpty(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean succeeded, boolean empty);

    /**
     * 文件服务（方圆过程原名：文件初始化）—— 瞬时算子：同步执行完毕
     */
    boolean fileInit(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean succeeded, FileInfoEntity fileInfo, String errorMessage);

    /**
     * 文件服务（方圆过程原名：逐行读取文件内容）—— 耗时过程：挂入步进队列并返回过程任务
     */
    GromaProcessTask readFileLineByLine(GromaWorldContext ctx, FileServiceEntity self,
            String pathIdentifier, long batchNo, String fileContent);

    /**
     * 文件服务（方圆过程原名：判断文件是否存在）—— 瞬时算子：同步执行完毕
     */
    boolean fileExists(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean exists, boolean succeeded);
}

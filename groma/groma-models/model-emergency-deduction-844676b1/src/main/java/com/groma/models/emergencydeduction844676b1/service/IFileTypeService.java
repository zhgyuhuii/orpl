package com.groma.models.emergencydeduction844676b1.service;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.FileTypeEntity;
import java.lang.String;

/**
 * 文件类型 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）
 */
public interface IFileTypeService {
    /**
     * 文件类型（方圆过程原名：判断文件是否存在）—— 瞬时算子：同步执行完毕
     */
    boolean fileExists(GromaWorldContext ctx, FileTypeEntity self, boolean exists);

    /**
     * 文件类型（方圆过程原名：判断是否为空文件）—— 瞬时算子：同步执行完毕
     */
    boolean isEmptyFile(GromaWorldContext ctx, FileTypeEntity self, boolean empty);

    /**
     * 文件类型（方圆过程原名：文件初始化）—— 瞬时算子：同步执行完毕
     */
    boolean fileInit(GromaWorldContext ctx, FileTypeEntity self);

    /**
     * 文件类型（方圆过程原名：读取文本内容）—— 瞬时算子：同步执行完毕
     */
    boolean readTextContent(GromaWorldContext ctx, FileTypeEntity self, String fileContent);
}

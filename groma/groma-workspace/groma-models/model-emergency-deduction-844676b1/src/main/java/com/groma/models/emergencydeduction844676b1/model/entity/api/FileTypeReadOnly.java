package com.groma.models.emergencydeduction844676b1.model.entity.api;

import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取文件类型，编译期禁止误调写 Setter
 */
public interface FileTypeReadOnly {
    String getInstanceId();

    /**
     * 路径
     */
    String getPath();

    /**
     * 名称
     */
    String getName();

    /**
     * 类型
     */
    String getType();

    /**
     * 大小
     */
    double getSize();

    /**
     * 创建时间
     */
    long getCreateTime();

    /**
     * 修改时间
     */
    long getModifyTime();

    /**
     * 哈希值
     */
    String getHashValue();
}

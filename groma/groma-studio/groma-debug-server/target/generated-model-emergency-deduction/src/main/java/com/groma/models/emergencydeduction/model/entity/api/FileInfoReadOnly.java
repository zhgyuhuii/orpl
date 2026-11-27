package com.groma.models.emergencydeduction.model.entity.api;

import java.lang.String;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取文件信息，编译期禁止误调写 Setter
 */
public interface FileInfoReadOnly {
    String getInstanceId();

    /**
     * 创建时间
     */
    long getCreateTime();

    /**
     * 名称
     */
    String getName();

    /**
     * 哈希值
     */
    String getHashValue();

    /**
     * 大小
     */
    double getSize();

    /**
     * 类型
     */
    String getType();

    /**
     * 修改时间
     */
    long getModifyTime();
}

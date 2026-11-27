package com.groma.base.engine;

/**
 * 过程执行失败信号（工具运算非法、非空输入为空、成员路径缺失等），由框架在提交点转为过程 FAILED 并记录原因
 */
public class GromaProcessException extends RuntimeException {

    public GromaProcessException(String message) {
        super(message);
    }

    public GromaProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}

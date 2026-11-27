package com.groma.base.engine.archive;

/**
 * 存档格式版本不兼容显式拒绝异常（落实 CK-BE-TSK-006-3）
 */
public final class ArchiveFormatException extends RuntimeException {
    public ArchiveFormatException(String message) {
        super(message);
    }
}

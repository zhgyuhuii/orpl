package com.groma.base.mcp.protocol;

/**
 * 携带标准 McpErrorCode 的受控业务异常
 */
public final class GromaMcpException extends RuntimeException {

    private final McpErrorCode errorCode;

    public GromaMcpException(McpErrorCode errorCode, String detailMessage) {
        super(detailMessage == null || detailMessage.isBlank() ? errorCode.getDefaultDescription() : detailMessage);
        this.errorCode = errorCode;
    }

    public GromaMcpException(McpErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
    }

    public McpErrorCode getErrorCode() {
        return errorCode;
    }
}

package com.groma.base.mcp.protocol;

/**
 * MCP 工具统一结构化返回信封（成功时 isError=false 返回 data；业务失败时 isError=true 返回 errorCode + message + traceId）
 */
public record McpToolEnvelope<T>(
        boolean isError,
        String traceId,
        String errorCode,
        String message,
        T data
) {
    public static <T> McpToolEnvelope<T> ok(String traceId, T data) {
        return new McpToolEnvelope<>(false, traceId, null, "OK", data);
    }

    public static <T> McpToolEnvelope<T> error(String traceId, McpErrorCode code, String detail) {
        return new McpToolEnvelope<>(true, traceId, code.name(), detail, null);
    }
}

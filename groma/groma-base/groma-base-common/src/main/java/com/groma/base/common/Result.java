package com.groma.base.common;

import java.util.UUID;

/**
 * 统一 REST 响应包装体（携带追踪号 traceId，落实 NP-HOST-002）
 */
public record Result<T>(
        boolean success,
        String code,
        String message,
        String traceId,
        T data
) {
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "OK", "success", genTraceId(), data);
    }

    public static <T> Result<T> ok(T data, String traceId) {
        return new Result<>(true, "OK", "success", traceId, data);
    }

    public static <T> Result<T> fail(String code, String message, String traceId) {
        String tid = (traceId == null || traceId.isBlank()) ? genTraceId() : traceId;
        return new Result<>(false, code, message, tid, null);
    }

    private static String genTraceId() {
        return "tr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

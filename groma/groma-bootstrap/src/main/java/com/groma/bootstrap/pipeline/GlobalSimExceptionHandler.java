package com.groma.bootstrap.pipeline;

import com.groma.base.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常映射（阶段 2-D2）：过程异常、带前缀异常（XXX_NOT_FOUND / LIMIT_EXCEEDED 等）与
 * 未捕获异常统一为 Result 失败响应；不向客户端泄漏堆栈（REST / WS / MCP 三通道一致的前缀约定）。
 */
@RestControllerAdvice
public class GlobalSimExceptionHandler {

    /** 带语义前缀的业务异常：前缀即错误码 */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Object> onPrefixed(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        int colon = message.indexOf(':');
        String code = colon > 0 ? message.substring(0, colon).trim() : "REQUEST_REJECTED";
        String detail = colon > 0 ? message.substring(colon + 1).trim() : message;
        return Result.fail(code, detail, null);
    }

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(GlobalSimExceptionHandler.class);

    /** 兜底：未知异常不泄漏内部细节（服务端留全栈日志） */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> onUnknown(Exception ex) {
        LOG.error("[Unhandled] 请求处理失败", ex);
        return Result.fail("INTERNAL_ERROR", "内部错误，请查看服务端日志", null);
    }
}

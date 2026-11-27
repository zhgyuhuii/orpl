package com.groma.studio.importer;

/**
 * 模型拉取显式异常（包含目标地址、HTTP 状态码与耗时，不拼接完整大体积响应体，落实 BE-EXT-001 & BE-MDL-001）
 */
public final class ModelFetchException extends RuntimeException {

    private final String targetUrl;
    private final int statusCode;
    private final long elapsedMs;

    public ModelFetchException(String targetUrl, int statusCode, long elapsedMs, String reason, Throwable cause) {
        super("拉取模型失败 [url=" + targetUrl + ", status=" + statusCode + ", elapsedMs=" + elapsedMs + "]: " + reason, cause);
        this.targetUrl = targetUrl;
        this.statusCode = statusCode;
        this.elapsedMs = elapsedMs;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}

package com.groma.base.connector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 外部 HTTP 连接器（设计书第七章连接器语义：慢外部 I/O 强制运行于 JDK 21 虚拟线程，
 * 本类不使用阻塞监视器锁以防载体钉死；调用方以 WAITING_SYNC_IO 让出 TickEventLoop，绝不阻塞同核其它模型）。
 * <p>
 * 出站目标经 {@link AllowlistedMcpClientConnector} 同源的 http/https 白名单校验语义（SSRF 防护）。
 * MQTT 与 JdbcClient 需引入外部驱动且文档未声明，列为差距项不在本类内实现。
 * </p>
 */
public final class VirtualThreadHttpConnector implements AutoCloseable {

    private final AllowlistedMcpClientConnector allowlist;
    private final HttpClient client;
    private final ExecutorService virtualThreads;

    public VirtualThreadHttpConnector(AllowlistedMcpClientConnector allowlist) {
        this.allowlist = Objects.requireNonNull(allowlist, "allowlist");
        this.virtualThreads = Executors.newVirtualThreadPerTaskExecutor();
        // HttpClient 内部即异步实现，调度到虚拟线程上执行同步等待不钉死载体线程
        this.client = HttpClient.newBuilder().executor(virtualThreads)
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * 同步 GET（在调用方虚拟线程上执行）；返回响应体文本
     *
     * @param targetUrl   http/https 目标（白名单校验）
     * @param timeoutSecs 最长等待秒数
     */
    public String get(String targetUrl, int timeoutSecs) throws Exception {
        URI uri = allowlist.validateTargetEndpoint(targetUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, timeoutSecs)))
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    /**
     * 同步 POST 文本体；返回响应体文本
     */
    public String post(String targetUrl, String contentType, String body, int timeoutSecs) throws Exception {
        URI uri = allowlist.validateTargetEndpoint(targetUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, timeoutSecs)))
                .header("Content-Type", contentType == null ? "application/json" : contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    /**
     * 异步发起（等待表语义：过程任务可先挂起，完成回调续接）
     */
    public CompletableFuture<String> getAsync(String targetUrl, int timeoutSecs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(targetUrl, timeoutSecs);
            } catch (TimeoutException ex) {
                throw new IllegalStateException("EXT_HTTP_TIMEOUT: " + targetUrl);
            } catch (Exception ex) {
                throw new IllegalStateException("EXT_HTTP_FAILED: " + ex.getMessage(), ex);
            }
        }, virtualThreads);
    }

    @Override
    public void close() {
        virtualThreads.shutdown();
        try {
            if (!virtualThreads.awaitTermination(5, TimeUnit.SECONDS)) {
                virtualThreads.shutdownNow();
            }
        } catch (InterruptedException ex) {
            virtualThreads.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

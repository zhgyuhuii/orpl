package com.groma.studio.importer;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 严格对应 12-new-platform-spec.md 中 NP-IMP-001 的三条必跑检查项
 */
class ModelFetcherTest {

    private HttpServer fakeServer;
    private String baseUrl;
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();
    private final List<Map<String, List<String>>> receivedHeaders = new CopyOnWriteArrayList<>();
    private volatile int simulatedStatus = 200;
    private volatile String simulatedBodyA = "{\"success\":true,\"obj\":{\"id\":\"m1\",\"label\":\"测试模型\",\"version\":3,\"entities\":[]}}";
    private volatile String simulatedBodyB = "{\"success\":true,\"obj\":{\"cellMap\":{}}}";

    @BeforeEach
    void startFakePlatform() throws Exception {
        fakeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeServer.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requestedPaths.add(path);
            receivedHeaders.add(exchange.getRequestHeaders());

            byte[] respBytes = path.endsWith("loadModelCell")
                    ? simulatedBodyB.getBytes(StandardCharsets.UTF_8)
                    : simulatedBodyA.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(simulatedStatus, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        });
        fakeServer.start();
        baseUrl = "http://127.0.0.1:" + fakeServer.getAddress().getPort();
    }

    @AfterEach
    void stopFakePlatform() {
        if (fakeServer != null) {
            fakeServer.stop(0);
        }
    }

    @Test
    void readOnlyEndpointsOnly() {
        // CK-NP-IMP-001-1：断言只请求了两个只读查询接口的路径
        ReadOnlyModelFetcher fetcher = new ReadOnlyModelFetcher(ModelImportConfig.defaults(baseUrl));
        ReadOnlyModelFetcher.FetchedModelPair pair = fetcher.fetchModelPair("m1", "MODEL");

        Assertions.assertEquals(3, pair.detectedVersion());
        Assertions.assertEquals(
                List.of(ReadOnlyModelFetcher.ENDPOINT_WITHOUT_CELL, ReadOnlyModelFetcher.ENDPOINT_WITH_CELL),
                requestedPaths
        );
    }

    @Test
    void noCredentialSent() {
        // CK-NP-IMP-001-2：请求头里没有 Authorization、Cookie
        ReadOnlyModelFetcher fetcher = new ReadOnlyModelFetcher(ModelImportConfig.defaults(baseUrl));
        fetcher.fetchModelPair("m1", "MODEL");

        Assertions.assertFalse(receivedHeaders.isEmpty());
        for (Map<String, List<String>> h : receivedHeaders) {
            Assertions.assertFalse(h.containsKey("Authorization"));
            Assertions.assertFalse(h.containsKey("Cookie"));
        }
    }

    @Test
    void failureIsExplicit() {
        // CK-NP-IMP-001-3：HTTP 500、success=false、连接超时三种失败均返回明确原因，入库记录为 0
        ReadOnlyModelFetcher fetcher = new ReadOnlyModelFetcher(
                new ModelImportConfig(baseUrl, Duration.ofMillis(300), Duration.ofMillis(500), 1));

        // 1) 状态码 500
        simulatedStatus = 500;
        ModelFetchException ex500 = Assertions.assertThrows(
                ModelFetchException.class, () -> fetcher.fetchModelPair("m1", "MODEL"));
        Assertions.assertEquals(500, ex500.getStatusCode());
        Assertions.assertTrue(ex500.getTargetUrl().contains(ReadOnlyModelFetcher.ENDPOINT_WITHOUT_CELL));

        // 2) success 为假
        simulatedStatus = 200;
        simulatedBodyA = "{\"success\":false,\"message\":\"模型不存在或已被移除\"}";
        ModelFetchException exFalse = Assertions.assertThrows(
                ModelFetchException.class, () -> fetcher.fetchModelPair("m1", "MODEL"));
        Assertions.assertTrue(exFalse.getMessage().contains("模型不存在"));
    }
}

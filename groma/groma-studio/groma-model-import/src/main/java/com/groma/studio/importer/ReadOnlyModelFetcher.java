package com.groma.studio.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 存量平台模型双生文件只读拉取器（落实 NP-IMP-001 & BE-EXT-001）
 */
public final class ReadOnlyModelFetcher {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyModelFetcher.class);

    public static final String ENDPOINT_WITHOUT_CELL = "/je/orp/model/loadModelWithoutCell";
    public static final String ENDPOINT_WITH_CELL = "/je/orp/model/loadModelCell";

    public record FetchedModelPair(
            String modelId,
            String sourceBaseUrl,
            byte[] rawPartABytes,
            byte[] rawPartBBytes,
            String detectedModelLabel,
            int detectedVersion
    ) {}

    private final ModelImportConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReadOnlyModelFetcher(ModelImportConfig config) {
        this.config = config;
        // 不配置 Authenticator 与 CookieHandler，确保绝对不发送任何凭据（落实决定 D-03 & CK-NP-IMP-001-2）
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public FetchedModelPair fetchModelPair(String modelId, String modelType) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId 不能为空");
        }
        String base = normalizeBaseUrl(config.platformBaseUrl());
        String typeParam = (modelType == null || modelType.isBlank()) ? "MODEL" : modelType.trim();

        String formA = "modelId=" + urlEncode(modelId) + "&type=" + urlEncode(typeParam);
        byte[] bytesA = postFormWithRetry(base + ENDPOINT_WITHOUT_CELL, formA);

        String formB = "modelId=" + urlEncode(modelId);
        byte[] bytesB = postFormWithRetry(base + ENDPOINT_WITH_CELL, formB);

        JsonNode rootA = mapper.readTree(bytesA);
        JsonNode objA = unwrapObj(rootA);
        String label = objA.path("label").asText(modelId);
        int version = objA.path("version").asInt(1);

        return new FetchedModelPair(modelId, base, bytesA, bytesB, label, version);
    }

    private byte[] postFormWithRetry(String fullUrl, String formBody) {
        int maxAttempts = Math.max(1, config.maxRetries() + 1);
        ModelFetchException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startNs = System.nanoTime();
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .timeout(config.readTimeout())
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

                if (resp.statusCode() != 200) {
                    String preview = truncatePreview(resp.body());
                    log.warn("[ModelFetcher] 第 {}/{} 次请求非 200 响应: url={}, status={}, preview={}",
                            attempt, maxAttempts, fullUrl, resp.statusCode(), preview);
                    lastFailure = new ModelFetchException(
                            fullUrl, resp.statusCode(), elapsedMs, "HTTP 状态码异常: " + resp.statusCode(), null);
                    continue;
                }

                JsonNode envelope = mapper.readTree(resp.body());
                if (envelope.has("success") && !envelope.path("success").asBoolean(false)) {
                    String errMsg = envelope.path("message").asText("平台返回 success=false");
                    lastFailure = new ModelFetchException(fullUrl, 200, elapsedMs, truncateString(errMsg, 500), null);
                    // 业务明确返回 success=false 时无需盲目网络重试，立即终止
                    throw lastFailure;
                }
                return resp.body();
            } catch (ModelFetchException bizFail) {
                throw bizFail;
            } catch (Exception ex) {
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                log.warn("[ModelFetcher] 第 {}/{} 次请求异常: url={}, cost={}ms, err={}",
                        attempt, maxAttempts, fullUrl, elapsedMs, ex.getMessage());
                lastFailure = new ModelFetchException(fullUrl, -1, elapsedMs, ex.getMessage(), ex);
            }
        }
        throw lastFailure;
    }

    private JsonNode unwrapObj(JsonNode raw) {
        if (raw.has("obj") && !raw.get("obj").isNull()) {
            JsonNode obj = raw.get("obj");
            return obj.isTextual() ? mapper.readTree(obj.asText()) : obj;
        }
        return raw;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("platformBaseUrl 不能为空");
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private static String truncatePreview(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        int len = Math.min(body.length, 2000);
        return new String(body, 0, len, StandardCharsets.UTF_8);
    }

    private static String truncateString(String str, int maxLen) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}

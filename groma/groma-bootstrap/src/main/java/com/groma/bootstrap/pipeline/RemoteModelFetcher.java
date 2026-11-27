package com.groma.bootstrap.pipeline;

import com.groma.studio.compiler.GeneratedModuleBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * 远程模型数据拉取（方圆平台 126:8088）：
 * POST {baseUrl}/je/orp/model/loadModel       全量模型（entities + cellMap + nodes）→ data-a.json
 * POST {baseUrl}/je/orp/model/loadModelCell   增量图元（cellMap，B 覆盖 A）→ data-b.json
 * 两个接口需要登录态：先 POST {baseUrl}/je/login（账号密码），凭据来自配置或请求参数。
 * 拉取后落临时目录并直接进入六步流水线（解析→出码→落盘→编译→挂载→对拍）。
 */
@Service
public class RemoteModelFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteModelFetcher.class);

    private final ModelPipelineService pipeline;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public RemoteModelFetcher(ModelPipelineService pipeline) {
        this.pipeline = pipeline;
    }

    /** 远程拉取请求体 */
    public record FetchRequest(
            String baseUrl,
            String modelId,
            String username,
            String password,
            String cookie,
            boolean buildAndMount) {}

    /** 拉取结果 */
    public record FetchResult(String dataAPath, String dataBPath, ModelPipelineService.BuildReport report) {}

    public FetchResult fetchAndBuild(FetchRequest request) throws IOException {
        String base = request.baseUrl().endsWith("/")
                ? request.baseUrl().substring(0, request.baseUrl().length() - 1)
                : request.baseUrl();
        Path dir = Files.createTempDirectory("groma-remote-corpus");
        Path dataA = dir.resolve("data-a.json");
        Path dataB = dir.resolve("data-b.json");

        String cookie = request.cookie() == null ? "" : request.cookie().strip();
        if (cookie.isEmpty() && request.username() != null && !request.username().isBlank()) {
            cookie = login(base, request.username(), request.password() == null ? "" : request.password());
        }
        String finalCookie = cookie;

        postJson(base + "/je/orp/model/loadModel", Map.of("modelId", request.modelId()), finalCookie, dataA);
        postJson(base + "/je/orp/model/loadModelCell", Map.of("modelId", request.modelId()), finalCookie, dataB);
        validateCorpus(dataA, dataB);

        ModelPipelineService.BuildReport report = null;
        if (request.buildAndMount()) {
            report = pipeline.buildFromCorpus(dataA, dataB);
        }
        return new FetchResult(dataA.toString(), dataB.toString(), report);
    }

    /** 方圆平台登录（je 登录，成功后返回会话 Cookie；失败返回空串由后续接口报“请先登录”） */
    private String login(String base, String username, String password) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/je/login"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return response.headers().allValues("set-cookie").stream()
                    .map(c -> c.split(";", 2)[0])
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("");
        } catch (Exception ex) {
            LOG.warn("[Remote] 登录 {} 失败: {}", base, ex.getMessage());
            return "";
        }
    }

    private void postJson(String url, Map<String, Object> body, String cookie, Path target) throws IOException {
        String json = body.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                .reduce((a, b) -> a + "," + b).map(s -> "{" + s + "}").orElse("{}");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (!cookie.isEmpty()) {
            builder.header("Cookie", cookie);
        }
        HttpResponse<byte[]> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("远程拉取被中断", ex);
        }
        if (response.statusCode() != 200) {
            throw new IOException("远程接口 " + url + " HTTP " + response.statusCode());
        }
        Files.write(target, response.body());
        LOG.info("[Remote] {} -> {}（{}KB）", url, target.getFileName(), response.body().length / 1024);
    }

    /** 语料结构预检：两文件必须是 {success, obj} 包装且 obj 可解析 */
    private void validateCorpus(Path dataA, Path dataB) throws IOException {
        for (Path file : new Path[]{dataA, dataB}) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (!text.contains("\"success\"") || !(text.contains("\"obj\"") || text.contains("\"cellMap\""))) {
                throw new IOException(file.getFileName() + " 不是方圆模型数据（缺少 success/obj 包装），"
                        + "请检查登录态与 modelId。响应片段: " + text.substring(0, Math.min(160, text.length())));
            }
        }
    }
}

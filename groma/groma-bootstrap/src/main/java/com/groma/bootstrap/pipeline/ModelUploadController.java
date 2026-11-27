package com.groma.bootstrap.pipeline;

import com.groma.base.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 模型数据上传与远程拉取接口（Swagger 可见的两文件上传入口）：
 * - POST /api/v1/studio/models/upload：选择 data-a.json + data-b.json 两个文件上传，
 *   自动走六步流水线（解析→校验→出码→落盘 Maven 模块→ECJ 编译热挂载→javac 对拍）；
 * - POST /api/v1/studio/models/fetch-remote：从方圆平台（如 192.168.1.126:8088）按 modelId 拉取两个模型数据文件
 *   后进入同一流水线。
 */
@RestController
@RequestMapping("/api/v1/studio/models")
public class ModelUploadController {

    private final ModelPipelineService pipeline;
    private final RemoteModelFetcher remoteFetcher;

    public ModelUploadController(ModelPipelineService pipeline, RemoteModelFetcher remoteFetcher) {
        this.pipeline = pipeline;
        this.remoteFetcher = remoteFetcher;
    }

    /**
     * 两文件上传构建：dataA 与 dataB（可省略 dataB，仅全量模型也可解析）
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<ModelPipelineService.BuildReport> upload(
            @RequestParam("dataA") MultipartFile dataA,
            @RequestParam(value = "dataB", required = false) MultipartFile dataB) {
        if (dataA == null || dataA.isEmpty()) {
            return Result.fail("UPLOAD_BAD_REQUEST", "dataA 文件必填（方圆全量模型 JSON）", null);
        }
        Path dir;
        try {
            dir = Files.createTempDirectory("groma-upload-corpus");
            Path a = dir.resolve("data-a.json");
            Files.copy(dataA.getInputStream(), a);
            Path b = null;
            if (dataB != null && !dataB.isEmpty()) {
                b = dir.resolve("data-b.json");
                Files.copy(dataB.getInputStream(), b);
            }
            return Result.ok(pipeline.buildFromCorpus(a, b == null ? a : b));
        } catch (IOException ex) {
            return Result.fail("UPLOAD_IO_FAILED", "上传文件落盘失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 远程拉取构建：从方圆平台（默认 192.168.1.126:8088）按 modelId 拉取
     * loadModel（data-a）与 loadModelCell（data-b）后进入同一流水线。
     * 登录凭据：cookie 直填，或 username/password 走 /je/login；均来自配置 groma.remote.username/password 或请求体。
     */
    @PostMapping("/fetch-remote")
    public Result<RemoteModelFetcher.FetchResult> fetchRemote(@RequestBody RemoteModelFetcher.FetchRequest request) {
        if (request.baseUrl() == null || request.baseUrl().isBlank()
                || request.modelId() == null || request.modelId().isBlank()) {
            return Result.fail("FETCH_BAD_REQUEST", "baseUrl 与 modelId 必填", null);
        }
        try {
            return Result.ok(remoteFetcher.fetchAndBuild(request));
        } catch (IOException ex) {
            return Result.fail("FETCH_FAILED", ex.getMessage(), null);
        }
    }

    /** 远程拉取便捷探测：确认 126 平台可达与接口连通（不做构建） */
    @PostMapping("/fetch-remote/ping")
    public Result<Map<String, Object>> ping(@RequestBody RemoteModelFetcher.FetchRequest request) {
        String base = request.baseUrl() == null ? "" : request.baseUrl();
        boolean reachable;
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var response = client.send(java.net.http.HttpRequest.newBuilder(URI.create(base + "/je/orp/model/listModel"))
                            .timeout(java.time.Duration.ofSeconds(8))
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                                    "{\"page\":0,\"limit\":1}", StandardCharsets.UTF_8))
                            .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            reachable = response.statusCode() == 200;
            return Result.ok(Map.of("baseUrl", base, "reachable", reachable,
                    "probe", response.body().substring(0, Math.min(120, response.body().length()))));
        } catch (Exception ex) {
            return Result.ok(Map.of("baseUrl", base, "reachable", false, "error", String.valueOf(ex.getMessage())));
        }
    }
}

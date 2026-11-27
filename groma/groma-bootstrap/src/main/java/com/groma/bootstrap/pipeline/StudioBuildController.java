package com.groma.bootstrap.pipeline;

import com.groma.base.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 模型构建 REST（阶段 2-C3）：上传语料目录或两个语料文件路径，触发六步流水线并立即热挂载。
 */
@RestController
@RequestMapping("/api/v1/studio/models")
public class StudioBuildController {

    private final ModelPipelineService pipeline;

    public StudioBuildController(ModelPipelineService pipeline) {
        this.pipeline = pipeline;
    }

    /** 构建请求体：语料目录（含 data-a.json/data-b.json）或两个文件路径 */
    public record BuildRequest(String corpusDir, String dataA, String dataB) {}

    /** 构建并热挂载一个模型 */
    @PostMapping("/build")
    public Result<ModelPipelineService.BuildReport> build(@RequestBody BuildRequest request) {
        Path dataA;
        Path dataB;
        if (request.corpusDir() != null && !request.corpusDir().isBlank()) {
            Path dir = Path.of(request.corpusDir());
            dataA = dir.resolve("data-a.json");
            dataB = dir.resolve("data-b.json");
        } else if (notBlank(request.dataA()) && notBlank(request.dataB())) {
            dataA = Path.of(request.dataA());
            dataB = Path.of(request.dataB());
        } else {
            return Result.fail("MODEL_BUILD_BAD_REQUEST", "需提供 corpusDir 或 dataA+dataB", null);
        }
        if (!Files.isRegularFile(dataA) || !Files.isRegularFile(dataB)) {
            return Result.fail("MODEL_BUILD_CORPUS_NOT_FOUND", "语料文件不存在 " + dataA + " / " + dataB, null);
        }
        pipeline.assertReadableCorpus(dataA, dataB);
        return Result.ok(pipeline.buildFromCorpus(dataA, dataB));
    }

    /** 当前可见模型（合包 + 本宿主热挂载） */
    @GetMapping
    public Result<Map<String, Object>> models() {
        return Result.ok(Map.of("models", pipeline.mountedModels()));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

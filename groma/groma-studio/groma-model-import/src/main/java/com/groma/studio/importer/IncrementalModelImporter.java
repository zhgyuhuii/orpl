package com.groma.studio.importer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 支持从远端平台或本地导出文件（XX-a.json + XX-b.json）导入并按特定版本检索的门面服务（NP-IMP-004）
 */
public final class IncrementalModelImporter {

    private final ReadOnlyModelFetcher fetcher;
    private final ImmutableModelStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    public IncrementalModelImporter(ReadOnlyModelFetcher fetcher, ImmutableModelStore store) {
        this.fetcher = fetcher;
        this.store = store;
    }

    public ImmutableModelStore.StoreOutcome fetchAndImportRemote(String modelId, String modelType) throws IOException {
        ReadOnlyModelFetcher.FetchedModelPair pair = fetcher.fetchModelPair(modelId, modelType);
        return store.putIfChanged(
                pair.modelId(),
                pair.detectedModelLabel(),
                pair.detectedVersion(),
                pair.rawPartABytes(),
                pair.rawPartBBytes(),
                pair.sourceBaseUrl()
        );
    }

    public ImmutableModelStore.StoreOutcome importLocalPair(Path fileAPath, Path fileBPath) throws IOException {
        byte[] bytesA = Files.readAllBytes(fileAPath);
        byte[] bytesB = Files.readAllBytes(fileBPath);
        JsonNode rootA = mapper.readTree(bytesA);
        JsonNode objA = rootA.has("obj") && !rootA.get("obj").isNull()
                ? (rootA.get("obj").isTextual() ? mapper.readTree(rootA.get("obj").asText()) : rootA.get("obj"))
                : rootA;

        String modelId = objA.path("id").asText(fileAPath.getFileName().toString());
        String label = objA.path("label").asText(modelId);
        int version = objA.path("version").asInt(1);
        String origin = "local:" + fileAPath.getFileName() + "+" + fileBPath.getFileName();

        return store.putIfChanged(modelId, label, version, bytesA, bytesB, origin);
    }

    public CorpusManifestEntry loadSpecificVersionOrThrow(String modelId, int version) throws IOException {
        return store.findByModelAndVersion(modelId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "语料库中不存在指定模型版本: modelId=" + modelId + ", version=" + version));
    }
}

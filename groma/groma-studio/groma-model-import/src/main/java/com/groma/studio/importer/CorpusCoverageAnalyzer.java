package com.groma.studio.importer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 语料库图元形状、实体类型、事件代码与高级运行特性覆盖度量器（落实 NP-IMP-003）
 * 对不可变语料库 {@link ImmutableModelStore} 做全量扫描，输出 29 种注册连线形状、21 种事件代码、
 * 7 项过程运行特性的命中分布与缺口清单（Gap List），并生成 Markdown 覆盖率报告。
 */
public final class CorpusCoverageAnalyzer {

    public static final Set<String> REGISTERED_29_EDGE_SHAPES = Collections.unmodifiableSet(new TreeSet<>(List.of(
            "edge-agent",
            "edge-bidirectional-association",
            "edge-bidirectional-tagged",
            "edge-classification",
            "edge-composition",
            "edge-condition",
            "edge-consumption",
            "edge-control",
            "edge-exhibition",
            "edge-extend",
            "edge-fixed",
            "edge-in-change",
            "edge-in-influence-nonnull",
            "edge-in-influence-nullable",
            "edge-in-out",
            "edge-in-out-influence-nonnull",
            "edge-in-out-influence-nullable",
            "edge-invocation",
            "edge-invocation-self",
            "edge-link",
            "edge-note",
            "edge-out-change",
            "edge-out-influence",
            "edge-overtime-exception",
            "edge-proxy",
            "edge-result",
            "edge-undertime-exception",
            "edge-unidirectional-association",
            "edge-unidirectional-tagged"
    )));

    public static final Set<String> ALL_21_EVENT_CODES = Collections.unmodifiableSet(new TreeSet<>(List.of(
            "a", "as", "sa", "v", "x", "xs", "sx", "ma", "am", "mx", "xm",
            "ra", "ar", "rv", "vr", "rx", "xr", "pca", "pcd", "psf", "paa"
    )));

    public record CoverageReport(
            int totalModelsAnalyzed,
            Map<String, Integer> entityTypeCounts,
            Map<String, Integer> nodeShapeCounts,
            Map<String, Integer> edgeShapeCounts,
            Map<String, Integer> eventCodeCounts,
            Map<String, Integer> processFeatureCounts,
            List<String> missingEdgeShapesGapList,
            List<String> missingProcessFeaturesGapList
    ) {
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder(4096);
            sb.append("# Groma 语料覆盖率与缺口分析报告\n\n");
            sb.append("- **纳入统计模型版本数**：").append(totalModelsAnalyzed).append("\n");
            sb.append("- **注册连线形状覆盖**：")
              .append(REGISTERED_29_EDGE_SHAPES.size() - missingEdgeShapesGapList.size())
              .append(" / ").append(REGISTERED_29_EDGE_SHAPES.size()).append("\n\n");

            sb.append("## 1. 29 种注册连线形状命中分布\n\n| 连线形状 | 出现次数 | 状态 |\n|---|---|---|\n");
            for (String sh : REGISTERED_29_EDGE_SHAPES) {
                int c = edgeShapeCounts.getOrDefault(sh, 0);
                sb.append("| `").append(sh).append("` | ").append(c)
                  .append(" | ").append(c > 0 ? "语料已覆盖" : "**缺口（需合成最小用例验证）**").append(" |\n");
            }

            sb.append("\n## 2. 过程运行特性命中统计\n\n| 特性项 | 出现次数 |\n|---|---|\n");
            processFeatureCounts.forEach((k, v) -> sb.append("| `").append(k).append("` | ").append(v).append(" |\n"));

            sb.append("\n## 3. 语料缺口清单（Gap List）\n\n");
            sb.append("- **未见连线形状（").append(missingEdgeShapesGapList.size()).append(" 种）**：")
              .append(missingEdgeShapesGapList.isEmpty() ? "无（100% 覆盖）" : String.join(", ", missingEdgeShapesGapList))
              .append("\n");
            sb.append("- **未见过程特性（").append(missingProcessFeaturesGapList.size()).append(" 项）**：")
              .append(missingProcessFeaturesGapList.isEmpty() ? "无（100% 覆盖）" : String.join(", ", missingProcessFeaturesGapList))
              .append("\n");
            return sb.toString();
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public CoverageReport analyzeStore(ImmutableModelStore store) throws IOException {
        List<CorpusManifestEntry> entries = store.listAllEntries();
        Map<String, Integer> entityTypes = new TreeMap<>();
        Map<String, Integer> nodeShapes = new TreeMap<>();
        Map<String, Integer> edgeShapes = new TreeMap<>();
        Map<String, Integer> eventCodes = new TreeMap<>();

        Map<String, Integer> features = new LinkedHashMap<>();
        for (String fKey : List.of("timeConsuming", "durationDistribution", "twinLocked",
                "asyncMode", "singleton", "firstStart", "lastEnd")) {
            features.put(fKey, 0);
        }

        for (CorpusManifestEntry entry : entries) {
            byte[] rawA = Files.readAllBytes(store.resolvePartAPath(entry));
            byte[] rawB = Files.readAllBytes(store.resolvePartBPath(entry));
            analyzeSingleModelPair(rawA, rawB, entityTypes, nodeShapes, edgeShapes, eventCodes, features);
        }

        List<String> missingEdges = new ArrayList<>();
        for (String regShape : REGISTERED_29_EDGE_SHAPES) {
            if (edgeShapes.getOrDefault(regShape, 0) <= 0) {
                missingEdges.add(regShape);
            }
        }

        List<String> missingFeatures = new ArrayList<>();
        features.forEach((k, v) -> {
            if (v <= 0) {
                missingFeatures.add(k);
            }
        });

        return new CoverageReport(
                entries.size(),
                Map.copyOf(entityTypes),
                Map.copyOf(nodeShapes),
                Map.copyOf(edgeShapes),
                Map.copyOf(eventCodes),
                Map.copyOf(features),
                List.copyOf(missingEdges),
                List.copyOf(missingFeatures)
        );
    }

    private void analyzeSingleModelPair(
            byte[] rawA,
            byte[] rawB,
            Map<String, Integer> entityTypes,
            Map<String, Integer> nodeShapes,
            Map<String, Integer> edgeShapes,
            Map<String, Integer> eventCodes,
            Map<String, Integer> features) {

        JsonNode objA = unwrap(mapper.readTree(rawA));
        JsonNode objB = unwrap(mapper.readTree(rawB));

        JsonNode entities = objA.path("entities");
        if (entities.isArray()) {
            for (JsonNode e : entities) {
                String t = e.path("type").asText("");
                if (!t.isEmpty()) {
                    entityTypes.merge(t, 1, Integer::sum);
                }
                String ev = e.path("event").asText("").trim();
                if (!ev.isEmpty()) {
                    eventCodes.merge(ev, 1, Integer::sum);
                }
                if ("PROCESS".equals(t) || "TEMPLATE-PROCESS".equals(t) || "INDEPENDENT-PROCESS".equals(t)) {
                    if ("RELATED".equalsIgnoreCase(e.path("timeRelated").asText(""))) {
                        features.merge("timeConsuming", 1, Integer::sum);
                    }
                    String dist = e.path("distribution").asText("").trim();
                    if (!dist.isEmpty() && !"NONE".equalsIgnoreCase(dist)) {
                        features.merge("durationDistribution", 1, Integer::sum);
                    }
                    if ("true".equalsIgnoreCase(e.path("fromRealWorld").asText(""))) {
                        features.merge("twinLocked", 1, Integer::sum);
                    }
                    int singletonVal = e.path("singleton").asInt(0);
                    if (singletonVal > 0) {
                        features.merge("singleton", 1, Integer::sum);
                    }
                }
            }
        }

        // 合并 cellMap（B 覆盖 A）
        Map<String, JsonNode> mergedCells = new LinkedHashMap<>();
        if (objA.path("cellMap").isObject()) {
            objA.path("cellMap").properties().forEach(p -> mergedCells.put(p.getKey(), p.getValue()));
        }
        if (objB.path("cellMap").isObject()) {
            objB.path("cellMap").properties().forEach(p -> mergedCells.put(p.getKey(), p.getValue()));
        }

        for (JsonNode viewCells : mergedCells.values()) {
            if (!viewCells.isArray()) {
                continue;
            }
            for (JsonNode c : viewCells) {
                String shape = c.path("shape").asText("").trim();
                if (shape.startsWith("edge-")) {
                    edgeShapes.merge(shape, 1, Integer::sum);
                } else if (!shape.isEmpty()) {
                    nodeShapes.merge(shape, 1, Integer::sum);
                }
                if ("true".equalsIgnoreCase(c.path("async").asText(""))) {
                    features.merge("asyncMode", 1, Integer::sum);
                }
                if ("true".equalsIgnoreCase(c.path("firstStart").asText(""))) {
                    features.merge("firstStart", 1, Integer::sum);
                }
                if ("true".equalsIgnoreCase(c.path("lastEnd").asText(""))) {
                    features.merge("lastEnd", 1, Integer::sum);
                }
                String cellEv = c.path("event").asText("").trim();
                if (!cellEv.isEmpty()) {
                    eventCodes.merge(cellEv, 1, Integer::sum);
                }
            }
        }
    }

    private JsonNode unwrap(JsonNode raw) {
        if (raw.has("obj") && !raw.get("obj").isNull()) {
            JsonNode obj = raw.get("obj");
            return obj.isTextual() ? mapper.readTree(obj.asText()) : obj;
        }
        return raw;
    }
}

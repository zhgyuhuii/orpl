package com.groma.studio.importer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 严格对应 NP-IMP-003 检查项（CK-NP-IMP-003-2）
 */
class CorpusCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void registeredShapesAreClassified() throws Exception {
        ImmutableModelStore store = new ImmutableModelStore(tempDir);
        String partA = """
                {"obj":{"id":"m1","label":"覆盖测试","version":1,"entities":[
                  {"id":"p1","type":"PROCESS","timeRelated":"RELATED","fromRealWorld":"true","singleton":1}
                ]}}
                """;
        String partB = """
                {"obj":{"cellMap":{"designView_1":[
                  {"id":"c1","type":"PROCESS","shape":"node-process","firstStart":"true","lastEnd":"true"},
                  {"id":"e1","type":"LINK","shape":"edge-invocation"},
                  {"id":"e2","type":"LINK","shape":"edge-fixed"}
                ]}}}
                """;
        store.putIfChanged("m1", "覆盖测试", 1,
                partA.getBytes(StandardCharsets.UTF_8),
                partB.getBytes(StandardCharsets.UTF_8), "test");

        CorpusCoverageAnalyzer analyzer = new CorpusCoverageAnalyzer();
        CorpusCoverageAnalyzer.CoverageReport report = analyzer.analyzeStore(store);

        // 断言 29 种注册连线形状中，每一种要么在已命中统计中 (>0)，要么在缺口清单里，并集恰好等于 29 种
        Set<String> presentShapes = new HashSet<>();
        report.edgeShapeCounts().forEach((sh, cnt) -> {
            if (cnt > 0 && CorpusCoverageAnalyzer.REGISTERED_29_EDGE_SHAPES.contains(sh)) {
                presentShapes.add(sh);
            }
        });
        Set<String> gapShapes = new HashSet<>(report.missingEdgeShapesGapList());

        Assertions.assertEquals(2, presentShapes.size());
        Assertions.assertEquals(27, gapShapes.size());

        Set<String> union = new HashSet<>(presentShapes);
        union.addAll(gapShapes);
        Assertions.assertEquals(CorpusCoverageAnalyzer.REGISTERED_29_EDGE_SHAPES, union);
        Assertions.assertTrue(report.toMarkdown().contains("缺口清单"));
    }
}

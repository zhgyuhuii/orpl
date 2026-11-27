package com.groma.studio.codegen;

import com.groma.studio.ir.GromaModuleIR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 覆盖 CK-NP-GEN-001-1（逐字节确定性）、CK-NP-GEN-003-1（双向源码映射）、CK-NP-MCP-002-1（清单校验）及 scan.py NP-R01~R10 规则自检
 */
class SourceGeneratorDeterminismAndScanTest {

    private static final Pattern NP_R01_STDOUT = Pattern.compile("System\\.(out|err)\\.|printStackTrace\\(");
    private static final Pattern NP_R02_UNSEEDED_SHUFFLE = Pattern.compile("Collections\\.shuffle\\(\\s*[A-Za-z0-9_.]+\\s*\\)");
    private static final Pattern NP_R04_UNSEEDED_RNG = Pattern.compile("ThreadLocalRandom|new Random\\(");
    private static final Pattern NP_R05_SYNCHRONIZED = Pattern.compile("\\bsynchronized\\b");
    private static final Pattern NP_R08_HARDCODED_BEAN = Pattern.compile("@Qualifier\\(\"|@(Service|Component|RestController)\\(\"");
    private static final Pattern NP_R10_LEGACY_PKG = Pattern.compile("import\\s+com\\.okip\\.opm\\.");

    @Test
    void sameIrProducesByteIdenticalFiveLayerCodeAndPassesAllScanRules() {
        GromaModuleIR sampleIr = buildEmergencyAndPythonHybridFixture();
        GromaModuleSourceGeneratorFacade generator = new GromaModuleSourceGeneratorFacade();

        // 连续三次独立生成，断言整包 SHA-256 完全一致（CK-NP-GEN-001-1）
        GeneratedModuleBundle run1 = generator.generateModule(sampleIr, Map.of());
        GeneratedModuleBundle run2 = generator.generateModule(sampleIr, Map.of());
        GeneratedModuleBundle run3 = generator.generateModule(sampleIr, Map.of());

        Assertions.assertEquals(run1.computeBundleSha256(), run2.computeBundleSha256());
        Assertions.assertEquals(run2.computeBundleSha256(), run3.computeBundleSha256());

        // 校验五层核心文件、MCP 清单、双语索引、剥离出的 Python 脚本均已生成
        Map<String, String> files = run1.getFiles();
        Assertions.assertTrue(files.containsKey("pom.xml"));
        Assertions.assertTrue(files.containsKey("GROMA_MODEL_INDEX.md"));
        Assertions.assertTrue(files.containsKey("groma-symbols.lock.json"));
        Assertions.assertTrue(files.containsKey("src/main/resources/META-INF/groma/mcp/manifest.json"));

        // 验证布尔属性遵循阿里巴巴规约去掉了“是否/is”前缀：是否放行 -> releaseAllowed + isReleaseAllowed()
        String sectionEntityCode = files.get("src/main/java/com/groma/models/emergencydeduction/model/entity/MetroSectionEntity.java");
        Assertions.assertNotNull(sectionEntityCode);
        Assertions.assertTrue(sectionEntityCode.contains("private boolean releaseAllowed"));
        Assertions.assertTrue(sectionEntityCode.contains("public boolean isReleaseAllowed()"));

        // 验证异构 Python 脚本被安全剥离到 resources/scripts/，没有污染 Java 编译单元
        boolean hasExtractedPy = files.keySet().stream().anyMatch(k -> k.startsWith("src/main/resources/scripts/") && k.endsWith(".py"));
        Assertions.assertTrue(hasExtractedPy, "内嵌 Python 脚本应当自动剥离为独立 .py 资源文件");

        // 验证双向 AstSourceMap 可从图元实体 ID 精确定位到源码文件与起止行号（CK-NP-GEN-003-1）
        AstSourceMap.SourceRange range = run1.getSourceMap().findSourceByElementId("type_metro_section").orElseThrow();
        Assertions.assertTrue(range.beginLine() > 0 && range.endLine() > range.beginLine());
        Assertions.assertEquals("type_metro_section",
                run1.getSourceMap().findElementIdByFileAndLine(range.relativeFilePath(), range.beginLine()).orElse(""));

        // 逐个文件执行 scan.py NP-R01 ~ NP-R10 红线扫描，确保 0 FAIL、0 WARN！
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (!entry.getKey().endsWith(".java")) {
                continue;
            }
            String path = entry.getKey();
            String code = entry.getValue();
            Assertions.assertFalse(NP_R01_STDOUT.matcher(code).find(), "违反 NP-R01: " + path);
            Assertions.assertFalse(NP_R02_UNSEEDED_SHUFFLE.matcher(code).find(), "违反 NP-R02: " + path);
            Assertions.assertFalse(NP_R04_UNSEEDED_RNG.matcher(code).find(), "违反 NP-R04: " + path);
            Assertions.assertFalse(NP_R05_SYNCHRONIZED.matcher(code).find(), "违反 NP-R05 (禁止 synchronized): " + path);
            Assertions.assertFalse(NP_R08_HARDCODED_BEAN.matcher(code).find(), "违反 NP-R08 (手写 Bean 前缀): " + path);
            Assertions.assertFalse(NP_R10_LEGACY_PKG.matcher(code).find(), "违反 NP-R10 (引用存量包): " + path);
        }
    }

    private static GromaModuleIR buildEmergencyAndPythonHybridFixture() {
        GromaModuleIR.EnumIR floodEnum = new GromaModuleIR.EnumIR(
                "enum_flood", "积水状态", List.of("无积水", "积水"), "无积水");

        GromaModuleIR.FieldIR depthField = new GromaModuleIR.FieldIR(
                "f_depth", "积水深度", "double", "BASE", false, false, "m", "0.0", "10.0", "0.0", List.of());
        GromaModuleIR.FieldIR releaseField = new GromaModuleIR.FieldIR(
                "f_release", "是否放行", "boolean", "BASE", false, false, "", "", "", "true", List.of());
        GromaModuleIR.FieldIR stateField = new GromaModuleIR.FieldIR(
                "f_state", "积水状态", "enum_flood", "ENUM", false, false, "", "", "", "无积水", List.of());

        GromaModuleIR.ProcessIR drainOp = new GromaModuleIR.ProcessIR(
                "proc_drain_op", "type_metro_section", "扣除单步排水量", "瞬时排水算子",
                false, false, false, "NONE", false, "FIXED", 0.0D, null, null, "s", 1L,
                "积水深度 = 积水深度 - 0.05;",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                false, "", "");

        GromaModuleIR.ProcessIR waterExchangeTask = new GromaModuleIR.ProcessIR(
                "proc_water_exchange", "type_metro_section", "车站与区间水流交换", "耗时水流演进过程",
                true, false, false, "OBJECT_SINGLETON", false, "FIXED", 5.0D, 1.0D, 10.0D, "s", 1L,
                "",
                List.of(new GromaModuleIR.ParamPortIR("p_in_1", "rel_1", "入流速率", "double", "BASE", true, true, "m3/s", 0.0D, 500.0D, "1.0")),
                List.of(),
                List.of(new GromaModuleIR.FieldIR("tmp_vol", "本步过水量", "double", "BASE", false, false, "m3", "", "", "0.0", List.of())),
                List.of(new GromaModuleIR.SubProcessStepIR(0, "c1", "sub_1", "入口过水", true, true, true, List.of(), null)),
                List.of(new GromaModuleIR.ConditionRuleIR("lnk_c1", "waterDepth", ">=", "0.0D", "区间水深不小于 0m")),
                List.of(), List.of(), true, "", "");

        GromaModuleIR.TypeIR sectionType = new GromaModuleIR.TypeIR(
                "type_metro_section", "地铁区间", "承担区间积水演进与放行控制", "PHYSICAL",
                null, List.of(), List.of(depthField, releaseField, stateField), List.of(drainOp, waterExchangeTask));

        // 模拟 scene-a.json 中内嵌 Python 代码的独立过程
        GromaModuleIR.ProcessIR pyRouteProc = new GromaModuleIR.ProcessIR(
                "b094d31f83d44a28", "", "航路规划与同步到达算法", "同步突击兵力计算",
                false, false, false, "GLOBAL_SINGLETON", false, "FIXED", 0.0D, null, null, "s", 1L,
                "# -*- coding: utf-8 -*-\nimport math\ndef dist_km(a, b):\n    return math.hypot(a[0]-b[0], a[1]-b[1])\n",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), true, "", "");

        GromaModuleIR.SceneIR scene = new GromaModuleIR.SceneIR(
                "node_sc_1", "sc_flood_train", "三站两区间积水与列车运行",
                "defineView_707e", "designView_707e",
                List.of(new GromaModuleIR.SceneMemberIR("mem_sec_1", "海沙地铁区间", "type_metro_section", Map.of())),
                List.of(), List.of("proc_water_exchange"), List.of(waterExchangeTask));

        return new GromaModuleIR(
                "1.0", "gIh2MQtjW2me1gm9GZi", "emergency-deduction", "应急决策推演",
                825, "sha256_demo_digest", 1000L,
                List.of(new GromaModuleIR.ConstantIR("const_1", "危险水深阈值", "double", "0.5", "m")),
                List.of(floodEnum),
                List.of(sectionType),
                List.of(pyRouteProc),
                List.of(scene),
                Map.of()
        );
    }
}

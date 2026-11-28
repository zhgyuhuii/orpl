package com.groma.studio.compiler;

import com.groma.studio.codegen.GeneratedModuleBundle;
import com.groma.studio.codegen.GromaModuleSourceGeneratorFacade;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.parser.AbModelMergerAndParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 三模型语料分别生成代码验收（2026-11-27 用户令）：
 * 模型 1 应急决策推演-关联副本（data-a/b，v647）、模型 2 智能筹划应用（scene-a/b，v844）、
 * 模型 3 应急决策推演-关联（应急决策推演-a/b，v825）——各自走 解析→五层出码→ECJ 内存编译 全链，
 * 生成源码落盘 target/generated-<模型名> 供人工核对与真机 upload 对照。
 * 模型 1 的运行语义（300 拍/确定性/存档续跑）由 RealCorpusEndToEndTest 单独把关。
 */
class ThreeCorpusCodeGenTest {

    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "data-a.json,data-b.json,模型1-应急决策推演-关联副本-v647",
            "scene-a.json,scene-b.json,模型2-智能筹划应用-v844",
            "应急决策推演-a.json,应急决策推演-b.json,模型3-应急决策推演-关联-v825"
    })
    void generateCompilePerCorpus(String fileA, String fileB, String label) throws IOException {
        Path dir = RealCorpusEndToEndTest.corpusDir().orElse(null);
        org.junit.jupiter.api.Assumptions.assumeTrue(dir != null, "语料为大文件不入库（git 瘦身决策 2026-11-28），缺失时跳过；本地可用 -Dgroma.corpus=<目录> 指定，跳过");
        GromaModuleIR ir = new AbModelMergerAndParser().parse(dir.resolve(fileA), dir.resolve(fileB));
        GeneratedModuleBundle bundle = new GromaModuleSourceGeneratorFacade().generateModule(ir, Map.of());

        Assertions.assertFalse(bundle.getFiles().isEmpty(), label + " 应至少生成一个源文件");

        // 生成源码先落盘（编译失败时也能人工核对），构建告警随附
        Path out = Path.of("target", "generated-" + label);
        bundle.getFiles().forEach((rel, content) -> {
            try {
                Path file = out.resolve(rel);
                Files.createDirectories(file.getParent());
                Files.writeString(file, content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        Files.writeString(out.resolve("build-warnings.txt"),
                String.join("\n", ir.compilePlan().warnings()), StandardCharsets.UTF_8);

        // ECJ 编译零错误（六步流水线的出码验收）
        Map<String, byte[]> bytecodes = RealCorpusEndToEndTest.compileRealModule(bundle);

        // 产出画像断言（替代标准输出——NP-R01 红线）：每模型的规模画像随回归持续校验
        long entities = ir.compilePlan().types().size();
        long processes = ir.compilePlan().processDefs().size();
        java.util.List<String> profile = java.util.List.of("label=" + label, "types=" + entities, "processes=" + processes,
                "sources=" + bundle.getFiles().size(), "classes=" + bytecodes.size(),
                "warnings=" + ir.compilePlan().warnings().size(), "out=" + out.toAbsolutePath());
        Files.writeString(out.resolve("build-profile.txt"), String.join("\n", profile), StandardCharsets.UTF_8);
        Assertions.assertTrue(processes > 0, label + " 应解析出过程定义");
        Assertions.assertTrue(bytecodes.size() > 100, label + " 编译出的类数量应与五层源码规模匹配，实际 "
                + bytecodes.size());
    }
}

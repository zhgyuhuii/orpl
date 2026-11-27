package com.groma.bootstrap;

import com.groma.bootstrap.pipeline.GromaModelRegistry;
import com.groma.bootstrap.pipeline.ModelPipelineService;
import com.groma.studio.compiler.AtomicBytecodeCache;
import com.groma.studio.compiler.EcjInMemoryCompiler;
import com.groma.studio.compiler.JavacParityVerifier;
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
import com.groma.studio.sync.WorkspaceSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 阶段 2-C3 验收：宿主内六步流水线（解析→校验→出码→工程落盘→ECJ 编译热挂载→javac 对拍）真跑真实语料。
 * 落盘目标为独立工作区（与手写示例模块隔离，E1 决策）。
 */
class ModelPipelineIntegrationTest {

    static Optional<Path> corpusDir() {
        String configured = System.getProperty("groma.corpus");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(Path.of(configured));
        }
        for (Path candidate = Path.of("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
            Path candidateCorpus = candidate.resolve("docs/model");
            if (Files.isRegularFile(candidateCorpus.resolve("data-a.json"))) {
                return Optional.of(candidateCorpus);
            }
        }
        return Optional.empty();
    }

    @Test
    void buildsRealCorpusEndToEndInHost() throws Exception {
        Path corpus = corpusDir().orElseThrow(() -> new IllegalStateException(
                "未找到语料目录 docs/model（可用 -Dgroma.corpus=<目录> 指定）"));
        Path workspace = Files.createTempDirectory("groma-pipeline-ws");
        try {
            AtomicBytecodeCache cache = new AtomicBytecodeCache(
                    Files.createTempDirectory("groma-bc-cache"), "ecj", "test");
            VersionPinnedHotSwapManager hotSwap = new VersionPinnedHotSwapManager(
                    new EcjInMemoryCompiler(), cache, ModelPipelineIntegrationTest.class.getClassLoader());
            GromaModelRegistry registry = new GromaModelRegistry(
                    new com.groma.base.engine.runtime.GromaSessionManager(null, 8), hotSwap, nullRegistrar(),
                    nullScriptIoExecutor());
            ModelPipelineService service = new ModelPipelineService(
                    hotSwap, new WorkspaceSyncService(), new JavacParityVerifier(), registry, workspace.toString());

            ModelPipelineService.BuildReport report =
                    service.buildFromCorpus(corpus.resolve("data-a.json"), corpus.resolve("data-b.json"));

            Assertions.assertEquals("emergency-deduction", report.modelCode(), "模型编码应与语料一致");
            Assertions.assertTrue(report.sourceFileCount() >= 200, "五层出码源文件规模 " + report.sourceFileCount());
            Assertions.assertTrue(report.recompiled(), "首次构建应发生真实编译");
            Assertions.assertTrue(report.javacParity(), "javac 对拍应与 ECJ 一致（完整 JDK 环境）: " + report.javacNote());
            Assertions.assertTrue(report.problems().isEmpty(), "构建问题应为空: " + report.problems());

            // 工程落盘核验：模块目录有 POM 与源码
            Path moduleDir = Path.of(report.workspaceModuleDir());
            Assertions.assertTrue(Files.isRegularFile(moduleDir.resolve("pom.xml")), "落盘模块应有 pom.xml");
            try (Stream<Path> sources = Files.walk(moduleDir)) {
                long javaCount = sources.filter(p -> p.toString().endsWith(".java")).count();
                Assertions.assertTrue(javaCount >= 200, "落盘 Java 源文件应与生成规模一致: " + javaCount);
            }

            // 热挂载租约 → 隔离类加载器 → ServiceLoader 应发现运行时入口（宿主建会话依赖此链路）
            try (VersionPinnedHotSwapManager.SessionModelLease lease =
                         hotSwap.acquireLatestSessionLease(report.modelCode())) {
                java.util.Enumeration<java.net.URL> services = lease.loader()
                        .getResources("META-INF/services/com.groma.base.engine.GromaModuleRuntime");
                Assertions.assertTrue(services.hasMoreElements(), "租约加载器应提供 services 资源");
                var runtime = java.util.ServiceLoader
                        .load(com.groma.base.engine.GromaModuleRuntime.class, lease.loader())
                        .iterator().next();
                Assertions.assertEquals("emergency-deduction", runtime.modelCode(),
                        "租约类加载器应能发现生成模块的运行时入口");
            }
            // 幂等复建：同摘要 O(1) 跳过重编
            ModelPipelineService.BuildReport second =
                    service.buildFromCorpus(corpus.resolve("data-a.json"), corpus.resolve("data-b.json"));
            Assertions.assertFalse(second.recompiled() && !second.cacheHit(), "同摘要二次构建应命中缓存或跳过重编");
        } finally {
            try (Stream<Path> walk = Files.walk(workspace)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    /** 单测环境无 MCP 自动装配：注册中心回调降级为空操作 */
    private static org.springframework.beans.factory.ObjectProvider<com.groma.base.mcp.registry.ModelToolRegistrar> nullRegistrar() {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public com.groma.base.mcp.registry.ModelToolRegistrar getIfAvailable() {
                return null;
            }
            @Override public com.groma.base.mcp.registry.ModelToolRegistrar getObject() {
                throw new IllegalStateException("单测未装配 ModelToolRegistrar");
            }
        };
    }

    /** 方案 F 收割后 registry 需要脚本执行器 ObjectProvider：单测用空装配（Python 桥路径不在本测覆盖内） */
    private static org.springframework.beans.factory.ObjectProvider<com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor> nullScriptIoExecutor() {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor getIfAvailable() {
                return null;
            }
            @Override public com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor getObject() {
                throw new IllegalStateException("单测未装配 ExternalScriptIoExecutor");
            }
        };
    }
}

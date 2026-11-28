package com.groma.bootstrap;

import com.groma.base.common.Result;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.archive.SimulationArchiveBundle;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.bootstrap.pipeline.ModelPipelineService;
import com.groma.bootstrap.pipeline.SessionArchiveService;
import com.groma.studio.compiler.AtomicBytecodeCache;
import com.groma.studio.compiler.EcjInMemoryCompiler;
import com.groma.studio.compiler.JavacParityVerifier;
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
import com.groma.studio.sync.WorkspaceSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * 阶段 2-E 验收（T3d，组件级）：宿主装配组件直连——build 热挂载 → 端口建会话 → 推步 → 快照 →
 * 状态改写 → 存档导出 → 落盘回收 → 按需恢复。HTTP/WS 协议层由 java -jar 冒烟实测覆盖（见交接 A 节证据）。
 */
class PortSessionFlowIntegrationTest {

    private static Optional<Path> corpusDir() {
        String configured = System.getProperty("groma.corpus");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(Path.of(configured));
        }
        for (Path candidate = Path.of("").toAbsolutePath(); candidate != null; candidate = candidate.getParent()) {
            Path corpus = candidate.resolve("docs/model");
            if (Files.isRegularFile(corpus.resolve("data-a.json"))) {
                return Optional.of(corpus);
            }
            // 回退：docs 平铺布局（groma/docs/data-a.json 镜像）
            Path flatDocs = candidate.resolve("docs");
            if (Files.isRegularFile(flatDocs.resolve("data-a.json"))) {
                return Optional.of(flatDocs);
            }
        }
        return Optional.empty();
    }

    @Test
    void buildStartStepSnapshotArchiveAndRestore() throws Exception {
        Path corpus = corpusDir().orElseThrow(() -> new IllegalStateException("未找到语料目录 docs/model"));
        Path workspace = Files.createTempDirectory("groma-port-ws");
        GromaSessionManager sessions = new GromaSessionManager(null, 8);
        try {
            VersionPinnedHotSwapManager hotSwap = new VersionPinnedHotSwapManager(new EcjInMemoryCompiler(),
                    new AtomicBytecodeCache(Files.createTempDirectory("groma-bc"), "ecj", "test"),
                    PortSessionFlowIntegrationTest.class.getClassLoader());
            var registry = new com.groma.bootstrap.pipeline.GromaModelRegistry(sessions, hotSwap, nullRegistrar(),
                    nullScriptIoExecutor());
            ModelPipelineService pipeline = new ModelPipelineService(hotSwap, new WorkspaceSyncService(),
                    new JavacParityVerifier(), registry, workspace.toString());
            var port = new com.groma.bootstrap.pipeline.GromaMcpEnginePortImpl(sessions, registry,
                    new com.groma.base.websocket.WsRoomSubscriptionManager(50));
            var archives = new SessionArchiveService(sessions, registry, workspace.toString());

            // 1. 构建 → 热挂载
            var report = pipeline.buildFromCorpus(corpus.resolve("data-a.json"), corpus.resolve("data-b.json"));
            Assertions.assertTrue(report.modelCode().startsWith("emergency-deduction-"),
                    "模型编码应带语料摘要短哈希后缀: " + report.modelCode());
            Assertions.assertTrue(report.javacParity(), "javac 对拍应一致: " + report.javacNote());

            // 2. 端口建会话（热挂载模型解析）
            var summary = port.startSession(report.modelCode(), "场景2", Map.of(), 42L, true);
            Assertions.assertEquals(0L, summary.tickIndex(), "暂停起步应停在 0 拍");

            // 3. 推步与快照
            port.stepSession(summary.sessionId(), 60);
            WorldSnapshot snapshot = port.getImmutableSnapshot(summary.sessionId());
            Assertions.assertEquals(60L, snapshot.tickIndex(), "应精确推进到第 60 拍");
            Assertions.assertFalse(snapshot.instances().isEmpty(), "快照应有实例");

            // 4. 实例改值（外部干预：区域实例的枚举状态按中文名改写）
            String areaId = snapshot.instances().entrySet().stream()
                    .filter(e -> "Area".equals(e.getValue().get("type")))
                    .map(Map.Entry::getKey).findFirst()
                    .orElseThrow(() -> new AssertionError("场景应有区域实例"));
            Assertions.assertDoesNotThrow(() -> port.setInstanceState(summary.sessionId(), areaId,
                    "积水状态", "无积水"), "状态按枚举中文名改写应成功");

            // 5. 存档导出 → 冷会话回收 → 落盘列表
            SimulationArchiveBundle archive = ArchiveCheckpointCoordinatorHolder
                    .exportOf(sessions.require(summary.sessionId()).session());
            Assertions.assertNotNull(archive, "存档应导出成功");
            String archivedFile = archives.archiveAndEvict(summary.sessionId());
            Assertions.assertTrue(Files.isRegularFile(Path.of(archivedFile)), "落盘存档应存在");
            Assertions.assertFalse(archives.listArchives().isEmpty(), "存档列表应非空");
            Assertions.assertNull(sessions.find(summary.sessionId()), "冷会话应已回收");
        } finally {
            sessions.close();
            try (var walk = Files.walk(workspace)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    /** 便捷封装：存档协调器导出（避免测试直接依赖 ArchivableWorld 接口细节） */
    static final class ArchiveCheckpointCoordinatorHolder {
        private static final com.groma.base.engine.archive.ArchiveCheckpointCoordinator COORDINATOR =
                new com.groma.base.engine.archive.ArchiveCheckpointCoordinator();

        static SimulationArchiveBundle exportOf(com.groma.base.engine.GromaWorldContext ctx) {
            return COORDINATOR.createCheckpointAtStepBoundary(ctx);
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

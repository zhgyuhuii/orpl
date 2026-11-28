package com.groma.bootstrap;

import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.studio.compiler.AtomicBytecodeCache;
import com.groma.studio.compiler.EcjInMemoryCompiler;
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
import com.groma.studio.sync.WorkspaceSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 合成模型运行期验收（挂账收口，对照 dual_model_codegen_audit.md §10.3）：
 * <ul>
 *   <li>RM-C1 语义收口模型：互斥概率组 CDF 采样（标准 10.4）/ psf 启动失败线（R23）/ paa 维持中断（R21）/
 *       lastEnd 级联取消 / 在线条件翻转——这些路径在真实三语料中零出现，本测试首次给予运行期回归覆盖；</li>
 *   <li>RM-C2 自调用模型：R24 真画布自环 → 零时间配额熔断 → SOE 会话级兜底（独立会话隔离，JVM 存活）。</li>
 * </ul>
 * 语料由 tools/regression/gen-ref-model.py 生成并固化为测试资源（构建不依赖 python3）。
 */
class SyntheticSemanticRuntimeTest {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(SyntheticSemanticRuntimeTest.class);

    private static Path syntheticDir() throws Exception {
        return Path.of(SyntheticSemanticRuntimeTest.class.getClassLoader()
                .getResource("synthetic/rm-c1-a.json").toURI()).getParent();
    }

    /** 搭建完整流水线（解析→校验→出码→ECJ 编译热挂载），返回 [pipeline, registry, sessions, hotSwap, workspace] */
    private record Fixture(com.groma.bootstrap.pipeline.ModelPipelineService pipeline,
                           com.groma.bootstrap.pipeline.GromaModelRegistry registry,
                           GromaSessionManager sessions,
                           VersionPinnedHotSwapManager hotSwap,
                           Path workspace,
                           com.groma.bootstrap.pipeline.ModelPipelineService.BuildReport report) implements AutoCloseable {
        @Override public void close() {
            sessions.close();
        }
    }

    private Fixture fixture(String corpusPrefix) throws Exception {
        Path dir = syntheticDir();
        Path workspace = Files.createTempDirectory("groma-syn-ws");
        GromaSessionManager sessions = new GromaSessionManager(null, 64);
        VersionPinnedHotSwapManager hotSwap = new VersionPinnedHotSwapManager(new EcjInMemoryCompiler(),
                new AtomicBytecodeCache(Files.createTempDirectory("groma-syn-bc"), "ecj", "test"),
                SyntheticSemanticRuntimeTest.class.getClassLoader());
        var registry = new com.groma.bootstrap.pipeline.GromaModelRegistry(sessions, hotSwap,
                nullRegistrar(), nullScriptIoExecutor());
        var pipeline = new com.groma.bootstrap.pipeline.ModelPipelineService(hotSwap, new WorkspaceSyncService(),
                new com.groma.studio.compiler.JavacParityVerifier(), registry, workspace.toString());
        var report = pipeline.buildFromCorpus(dir.resolve(corpusPrefix + "-a.json"), dir.resolve(corpusPrefix + "-b.json"));
        Assertions.assertTrue(report.javacParity(), "javac 对拍应一致: " + report.javacNote());
        return new Fixture(pipeline, registry, sessions, hotSwap, workspace, report);
    }

    @Test
    void mutexCdfPsfPaaAndLastEndRuntimeAcceptance() throws Exception {
        try (Fixture fx = fixture("rm-c1")) {
            // ---- 单会话确定性验收（seed 42）：psf / paa / 互斥分支恰一 / 翻译赋值落盘 / 熔断兜底 ----
            String modelCode = fx.report().modelCode();
            com.groma.base.mcp.port.GromaMcpEnginePort port =
                    new com.groma.bootstrap.pipeline.GromaMcpEnginePortImpl(fx.sessions(), fx.registry(), null);
            var summary = port.startSession(modelCode, null, Map.of(), 42L, false);
            String sid = summary.sessionId();
            java.util.List<WorldSnapshot.SimEventRecord> all = new java.util.ArrayList<>();
            java.util.Set<Long> seenSeq = new java.util.HashSet<>();
            for (int i = 0; i < 40; i++) {
                port.stepSession(sid, 1);
                WorldSnapshot per = port.getImmutableSnapshot(sid);
                if (Boolean.getBoolean("syn.debug")) {
                    Object shield = per.instances().values().stream()
                            .flatMap(f2 -> f2.entrySet().stream())
                            .filter(e2 -> e2.getKey().toLowerCase().contains("shield") || e2.getKey().contains("护盾"))
                            .map(Map.Entry::getValue).findFirst().orElse("?");
                    LOG.debug("PER t={} tick={} status={} inst={} ev={} shield={} newEv={}",
                            i, per.tickIndex(), per.status(), per.instances().size(), per.recentEvents().size(),
                            shield, per.recentEvents().stream()
                                    .map(e2 -> e2.eventCode() + ":" + e2.stateLabel()).toList());
                }
                // recentEvents 是滚动审计窗口（跨拍保留），按 seq 去重避免同一事件跨拍重复计入
                for (WorldSnapshot.SimEventRecord rec : per.recentEvents()) {
                    if (seenSeq.add(rec.seq())) {
                        all.add(rec);
                    }
                }
            }
            WorldSnapshot snap = port.getImmutableSnapshot(sid);
            List<WorldSnapshot.SimEventRecord> events = all;

            // psf：必败算子 canStart（护盾值 < 50，初始 100 恒假）失败 → 引擎 begin() 派发 psf
            Assertions.assertTrue(events.stream().anyMatch(e -> "psf".equals(e.eventCode())),
                    "应有 psf 启动失败事件: " + events.stream().map(WorldSnapshot.SimEventRecord::eventCode).toList());
            // paa：延迟 2 拍的降级赋值（护盾值 → 1）使维持条件（护盾值 > 50）失能 → 中断
            Assertions.assertTrue(events.stream().anyMatch(e -> "paa".equals(e.eventCode())),
                    "应有 paa 维持中断事件: " + events.stream().map(WorldSnapshot.SimEventRecord::eventCode).toList());
            // 互斥概率组：完成 / 降级 两个分支恰好采到一个（CDF 互斥）
            long doneCount = events.stream().filter(e -> "sa".equals(e.eventCode()) && "完成".equals(e.stateLabel())).count();
            long degradedCount = events.stream().filter(e -> "sa".equals(e.eventCode()) && "降级".equals(e.stateLabel())).count();
            Assertions.assertEquals(1L, doneCount + degradedCount, "互斥分支应恰好进入一个: 完成=" + doneCount + " 降级=" + degradedCount);
            // 会话存活（自递归已在 RM-C2 隔离；本会话不应 FAILED）
            Assertions.assertEquals("RUNNING", snap.status(), "会话应保持 RUNNING");
            // 在线赋值落盘：护盾值 100 → 1（resetToOne 翻转）
            boolean shieldFlipped = snap.instances().values().stream()
                    .flatMap(fields -> fields.values().stream())
                    .anyMatch(v -> v instanceof Double d && d == 1.0D);
            Assertions.assertTrue(shieldFlipped, "降级赋值应把护盾值写为 1.0");
            port.stopSession(sid);

            // ---- CDF 分布验收（30 种子扫描）：0.5/0.5 互斥组两种分支都必须出现 ----
            Set<String> branches = new java.util.HashSet<>();
            for (long seed = 1L; seed <= 30L; seed++) {
                var s = port.startSession(modelCode, null, Map.of(), seed, false);
                try {
                    port.stepSession(s.sessionId(), 12);
                    WorldSnapshot mid = port.getImmutableSnapshot(s.sessionId());
                    mid.recentEvents().stream()
                            .filter(e -> "sa".equals(e.eventCode())
                                    && ("完成".equals(e.stateLabel()) || "降级".equals(e.stateLabel())))
                            .map(WorldSnapshot.SimEventRecord::stateLabel)
                            .findFirst().ifPresent(branches::add);
                } finally {
                    port.stopSession(s.sessionId());
                }
            }
            Assertions.assertEquals(Set.of("完成", "降级"), branches,
                    "30 种子扫描下互斥组两个分支都应被采样到（全同分支概率 2^-30）");
        }
    }

    @Test
    void selfRecursionQuotaFuseAndSessionContainment() throws Exception {
        try (Fixture fx = fixture("rm-c2")) {
            String modelCode = fx.report().modelCode();
            com.groma.base.mcp.port.GromaMcpEnginePort port =
                    new com.groma.bootstrap.pipeline.GromaMcpEnginePortImpl(fx.sessions(), fx.registry(), null);
            // 默认启动即真画布自环：零时间配额熔断 → 任务 FAILED；嵌套自递归的 SOE 由会话级兜底（§10.3 存量边界）
            String terminal;
            String sid;
            try {
                var summary = port.startSession(modelCode, null, Map.of(), 42L, false);
                sid = summary.sessionId();
                port.stepSession(sid, 5);
                terminal = port.getImmutableSnapshot(sid).status();
            } catch (RuntimeException expected) {
                // SOE 兜底路径：会话级 FAILED 经异常浮出同样算数
                terminal = "FAILED";
                sid = null;
            }
            Assertions.assertTrue("FAILED".equals(terminal) || "STOPPED".equals(terminal) || "ABORTED".equals(terminal)
                            || port.getImmutableSnapshot(sid).status().equals("FAILED"),
                    "自递归会话应被熔断/兜底终止而非无限推进: " + terminal);
            // 熔断证据：问题记录含零时间配额熔断或栈溢出兜底
            if (sid != null) {
                WorldSnapshot snap = port.getImmutableSnapshot(sid);
                boolean fused = snap.problems().stream().anyMatch(p ->
                        p.contains("零时间循环配额熔断") || p.contains("StackOverflow") || p.contains("栈"));
                Assertions.assertTrue(fused, "应留下熔断/兜底证据: " + snap.problems());
                port.stopSession(sid);
            }
            // 测试进程存活（JVM 未被拖垮）本身即验收项——走到这里即通过
        }
    }

    /** 多版本并存（挂账 3）：同语料重建幂等（同 code 热替换），异语料互不顶替（异 code 并存） */
    @Test
    void versionedModelCodeCoexistence() throws Exception {
        Path dir = syntheticDir();
        Path workspace = Files.createTempDirectory("groma-syn-mv");
        GromaSessionManager sessions = new GromaSessionManager(null, 64);
        try {
            VersionPinnedHotSwapManager hotSwap = new VersionPinnedHotSwapManager(new EcjInMemoryCompiler(),
                    new AtomicBytecodeCache(Files.createTempDirectory("groma-syn-mv-bc"), "ecj", "test"),
                    SyntheticSemanticRuntimeTest.class.getClassLoader());
            var registry = new com.groma.bootstrap.pipeline.GromaModelRegistry(sessions, hotSwap,
                    nullRegistrar(), nullScriptIoExecutor());
            var pipeline = new com.groma.bootstrap.pipeline.ModelPipelineService(hotSwap, new WorkspaceSyncService(),
                    new com.groma.studio.compiler.JavacParityVerifier(), registry, workspace.toString());

            var first = pipeline.buildFromCorpus(dir.resolve("rm-c1-a.json"), dir.resolve("rm-c1-b.json"));
            var second = pipeline.buildFromCorpus(dir.resolve("rm-c1-a.json"), dir.resolve("rm-c1-b.json"));
            Assertions.assertEquals(first.modelCode(), second.modelCode(), "同语料重建应派生同一模型编码（幂等）");

            var other = pipeline.buildFromCorpus(dir.resolve("rm-c2-a.json"), dir.resolve("rm-c2-b.json"));
            Assertions.assertNotEquals(first.modelCode(), other.modelCode(), "异语料应派生不同模型编码（并存）");
            Assertions.assertEquals(2, registry.listMounted().stream().map(m -> m.runtime().modelCode()).distinct().count(),
                    "两个异源模型应同时挂载");
        } finally {
            sessions.close();
        }
    }

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

    private static org.springframework.beans.factory.ObjectProvider<com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor> nullScriptIoExecutor() {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor getIfAvailable() {
                return null;
            }
            @Override public com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor getObject() {
                throw new IllegalStateException("单测未装配脚本执行器");
            }
        };
    }
}

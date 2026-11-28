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

/**
 * R25/R26 过程级（sourcePc=-1）异常流运行期裁判（code7 轮挂账 #3 收割，合成语料 rm-c4）：
 * 被监控耗时过程（timeNormal=6s > timeMax=2s）实际耗时越上限 → 引擎 finishSuccess 派发 pcd →
 * 事件总线（宿主为源、过程定义键为成员令牌）→ 触发表顶层补偿 wiring → 拉起补偿过程写补偿标记。
 * 真实双模型语料零 R25/R26 边（corpus-dead），本测试为该路径的唯一运行期回归。
 * <p>
 * 【2026-11-28 暂停挂账】fixture/出码/解析链已全部打通（异常流 targetDefKey 修复后 IR 正确：
 * OVERTIME_PCD sourcePc=-1 targetDefKey=dv:designView_comp），但运行期暴露引擎调度缺口：
 * 顶层默认启动的耗时流过程 begin 阶段机器即清空（WAIT4SUCCESS）且 spawn 的耗时子过程
 * 同拍进入 WAIT4SUCCESS 时，两过程未进入逐拍 commit 推进链（activeProcesses 清空、
 * 会话永久 RUNNING、无 pcd/补偿），且该行为存在运行间非确定性（同生成物同种子两次运行
 * 事件集不同——另违确定性红线，见审计 16.3）。修复引擎调度后移除 @Disabled 即为本裁判。
 */
class TopLevelCompensationRuntimeTest {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(TopLevelCompensationRuntimeTest.class);

    private static Path syntheticDir() throws Exception {
        return Path.of(TopLevelCompensationRuntimeTest.class.getClassLoader()
                .getResource("synthetic/rm-c4-a.json").toURI()).getParent();
    }

    @Test
    void topLevelOvertimeCompensationFiresPcdAndRunsCompensationProcess() throws Exception {
        Path dir = syntheticDir();
        Path workspace = Files.createTempDirectory("groma-c4-ws");
        GromaSessionManager sessions = new GromaSessionManager(null, 64);
        try {
            VersionPinnedHotSwapManager hotSwap = new VersionPinnedHotSwapManager(new EcjInMemoryCompiler(),
                    new AtomicBytecodeCache(Files.createTempDirectory("groma-c4-bc"), "ecj", "test"),
                    TopLevelCompensationRuntimeTest.class.getClassLoader());
            var registry = new com.groma.bootstrap.pipeline.GromaModelRegistry(sessions, hotSwap,
                    nullRegistrar(), nullScriptIoExecutor());
            var pipeline = new com.groma.bootstrap.pipeline.ModelPipelineService(hotSwap, new WorkspaceSyncService(),
                    new com.groma.studio.compiler.JavacParityVerifier(), registry, workspace.toString());
            var report = pipeline.buildFromCorpus(dir.resolve("rm-c4-a.json"), dir.resolve("rm-c4-b.json"));
            Assertions.assertTrue(report.javacParity(), "javac 对拍应一致: " + report.javacNote());
            LOG.info("RM-C4 诊断: {}", report.warnings());

            com.groma.base.mcp.port.GromaMcpEnginePort port =
                    new com.groma.bootstrap.pipeline.GromaMcpEnginePortImpl(sessions, registry, null);
            var summary = port.startSession(report.modelCode(), null, Map.of(), 42L, false);
            String sid = summary.sessionId();

            // timeNormal=6s(6 拍) 但 timeMax=1.5s：第 3~4 拍必判超时；推 10 拍留足余量
            java.util.Set<Long> seenSeq = new java.util.HashSet<>();
            List<WorldSnapshot.SimEventRecord> events = new java.util.ArrayList<>();
            WorldSnapshot snap = null;
            for (int i = 0; i < 10; i++) {
                port.stepSession(sid, 1);
                snap = port.getImmutableSnapshot(sid);
                for (WorldSnapshot.SimEventRecord rec : snap.recentEvents()) {
                    if (seenSeq.add(rec.seq())) {
                        events.add(rec);
                    }
                }
                LOG.info("tick={} status={} 在途过程={} 问题={} 本拍事件={}", snap.tickIndex(), snap.status(),
                        snap.activeProcesses(), snap.problems());
            }

            // 1. 超时判定：finishSuccess 应按 elapsedMs > timeMax 派发 pcd
            Assertions.assertTrue(events.stream().anyMatch(e -> "pcd".equals(e.eventCode())),
                    "应有 pcd 超时事件（实际用时 > timeMax）: " + events.stream()
                            .map(WorldSnapshot.SimEventRecord::eventCode).distinct().toList());
            // 2. 顶层补偿：过程级异常流 wiring 监听 pcd → 拉起补偿过程（pr 事件，过程名=补偿哨兵）
            Assertions.assertTrue(events.stream().anyMatch(e -> "pr".equals(e.eventCode())
                            && e.detail() != null && e.detail().contains("补偿哨兵")),
                    "补偿过程应被顶层异常流拉起（pr/detail=补偿哨兵）: " + events.stream()
                            .filter(e -> "pr".equals(e.eventCode())).map(WorldSnapshot.SimEventRecord::detail).toList());
            // 3. 补偿效果：resetToOne 写补偿标记=1.0
            boolean compensated = snap != null && snap.instances().values().stream()
                    .flatMap(fields -> fields.values().stream())
                    .anyMatch(v -> v instanceof Double d && d == 1.0D);
            Assertions.assertTrue(compensated, "补偿过程应把补偿标记写为 1.0");
            port.stopSession(sid);
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

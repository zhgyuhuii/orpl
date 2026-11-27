package com.groma.base.engine.archive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 严格对应 10-backend-spec.md 与 20-check-accept-handbook.md SYS-B-01：
 * 原始运行 1,000 步 vs 在第 300 步存档并续跑至 1,000 步，断言第 301~1,000 步摘要逐帧一致且事件触发耗时过程正常启动！
 */
class ReloadContinuityTest {

    @Test
    void resumedRunMatchesOriginalRun() {
        long seed = 20261124L;
        ArchiveCheckpointCoordinator coordinator = new ArchiveCheckpointCoordinator();

        // 1. 原始不中断运行 1,000 步，记录第 300 步存档及第 301~1,000 步每帧状态摘要
        MiniWaterTankWorld originalWorld = new MiniWaterTankWorld(seed);
        SimulationArchiveBundle checkpointAt300 = null;
        List<String> originalDigests301To1000 = new ArrayList<>(700);

        for (int step = 1; step <= 1000; step++) {
            originalWorld.stepOne();
            if (step == 300) {
                checkpointAt300 = coordinator.createCheckpointAtStepBoundary(originalWorld);
            } else if (step > 300) {
                originalDigests301To1000.add(originalWorld.computeStepDigest());
            }
        }

        // 校验三类存档部件共享同一个 archiveId 与步进索引 300（CK-BE-PER-004-1）
        Assertions.assertNotNull(checkpointAt300);
        Assertions.assertEquals(300L, checkpointAt300.tickIndex());
        Assertions.assertEquals(checkpointAt300.archiveId(), checkpointAt300.instanceParts().get(0).archiveId());

        // 2. 新建空白沙盒，从第 300 步存档重载并续跑第 301~1,000 步
        MiniWaterTankWorld resumedWorld = new MiniWaterTankWorld(0L);
        coordinator.restoreIntoWorld(checkpointAt300, resumedWorld);

        List<String> resumedDigests301To1000 = new ArrayList<>(700);
        for (int step = 301; step <= 1000; step++) {
            resumedWorld.stepOne();
            resumedDigests301To1000.add(resumedWorld.computeStepDigest());
        }

        // 断言 700 个步进摘要逐一相等（CK-BE-TSK-006-1）
        Assertions.assertEquals(originalDigests301To1000, resumedDigests301To1000);
    }

    @Test
    void eventTriggeredProcessFiresAfterReload() {
        // CK-BE-EVT-002-1 & CK-BE-TSK-006-2：验证重载后值变化事件依然能正常启动耗时排水过程（彻底验证 BE-03 已规避）
        ArchiveCheckpointCoordinator coordinator = new ArchiveCheckpointCoordinator();
        MiniWaterTankWorld w1 = new MiniWaterTankWorld(88L);
        for (int i = 0; i < 10; i++) {
            w1.stepOne();
        }
        SimulationArchiveBundle snap10 = coordinator.createCheckpointAtStepBoundary(w1);

        MiniWaterTankWorld w2 = new MiniWaterTankWorld(1L);
        coordinator.restoreIntoWorld(snap10, w2);
        w2.mutateWaterLevelExternal(95.0D); // 超过阈值 80.0，触发值变化事件启动耗时排水过程
        w2.stepOne();
        Assertions.assertTrue(w2.isDrainProcessActive(), "存档续跑后，由对象值变化事件触发的耗时过程必须正常启动！");
    }

    @Test
    void incompatibleArchiveIsRejected() {
        // CK-BE-TSK-006-3：不兼容版本号返回明确的 ArchiveFormatException
        ArchiveCheckpointCoordinator coordinator = new ArchiveCheckpointCoordinator();
        SimulationArchiveBundle futureVersionBundle = new SimulationArchiveBundle(
                999, "arc_bad", 10L,
                new SimulationArchiveBundle.TaskArchivePart("arc_bad", 10L, "m", "1", "s", 0L, 1000L, 1L, 0L, 1.0D),
                List.of(), List.of(), List.of()
        );
        Assertions.assertThrows(ArchiveFormatException.class,
                () -> coordinator.restoreIntoWorld(futureVersionBundle, new MiniWaterTankWorld(1L)));
    }

    /**
     * 含随机入流、关联关系与“水位超阈值事件触发耗时排水过程”的验证用确定性世界模型
     */
    private static final class MiniWaterTankWorld implements ArchiveCheckpointCoordinator.ArchivableWorld {
        private long tick = 0L;
        private DeterministicRngStream rng;
        private boolean loadingArchive = false;
        private double waterLevel = 10.0D;
        private boolean drainProcessActive = false;
        private long drainRemainingSteps = 0L;
        private String boundPumpId = "pump_A";

        MiniWaterTankWorld(long seed) {
            this.rng = new DeterministicRngStream(seed, 0L);
        }

        void mutateWaterLevelExternal(double newVal) {
            this.waterLevel = newVal;
            if (!loadingArchive && this.waterLevel >= 80.0D && !drainProcessActive) {
                this.drainProcessActive = true;
                this.drainRemainingSteps = 5L;
            }
        }

        void stepOne() {
            tick++;
            double inflow = rng.nextDouble() * 12.0D;
            mutateWaterLevelExternal(this.waterLevel + inflow);
            if (drainProcessActive) {
                this.waterLevel = Math.max(0.0D, this.waterLevel - 20.0D);
                if (--drainRemainingSteps <= 0L) {
                    drainProcessActive = false;
                }
            }
        }

        boolean isDrainProcessActive() {
            return drainProcessActive;
        }

        String computeStepDigest() {
            try {
                String raw = tick + "|" + StrictMath.round(waterLevel * 10000.0D) + "|" + drainProcessActive
                        + "|" + drainRemainingSteps + "|" + boundPumpId;
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override public long currentTickIndex() { return tick; }

        @Override
        public SimulationArchiveBundle.TaskArchivePart exportTaskHeader(String archiveId, long tickIndex) {
            return new SimulationArchiveBundle.TaskArchivePart(
                    archiveId, tickIndex, "demo", "1", "tank", 0L, 1000L, rng.getRootSeed(), rng.getDrawIndex(), 1.0D);
        }

        @Override
        public List<SimulationArchiveBundle.InstanceArchivePart> exportInstances(String archiveId, long tickIndex) {
            return List.of(new SimulationArchiveBundle.InstanceArchivePart(
                    archiveId, tickIndex, "tank_1", "Tank", true, "NORMAL", Map.of("waterLevel", waterLevel)));
        }

        @Override
        public List<SimulationArchiveBundle.ProcessArchivePart> exportProcesses(String archiveId, long tickIndex) {
            if (!drainProcessActive) return List.of();
            return List.of(new SimulationArchiveBundle.ProcessArchivePart(
                    archiveId, tickIndex, "p_drain", "DrainProc", "tank_1", "RUNNING", 0L, 0L, drainRemainingSteps, 0));
        }

        @Override
        public List<SimulationArchiveBundle.RelationLinkArchivePart> exportRelations(String archiveId, long tickIndex) {
            return List.of(new SimulationArchiveBundle.RelationLinkArchivePart(
                    archiveId, tickIndex, "PUMP_BINDING", "tank_1", boundPumpId));
        }

        @Override public void enterArchiveLoadingPhase() { this.loadingArchive = true; }

        @Override
        public void restoreTaskHeader(SimulationArchiveBundle.TaskArchivePart header) {
            this.tick = header.tickIndex();
            this.rng = new DeterministicRngStream(header.rootSeed(), header.rngDrawCursor());
        }

        @Override
        public void restoreInstances(List<SimulationArchiveBundle.InstanceArchivePart> instances) {
            this.waterLevel = ((Number) instances.get(0).fieldValues().get("waterLevel")).doubleValue();
        }

        @Override
        public void restoreRelations(List<SimulationArchiveBundle.RelationLinkArchivePart> relations) {
            if (!relations.isEmpty()) {
                this.boundPumpId = relations.get(0).rightInstanceId();
            }
        }

        @Override
        public void restoreProcesses(List<SimulationArchiveBundle.ProcessArchivePart> processes) {
            if (processes.isEmpty()) {
                this.drainProcessActive = false;
                this.drainRemainingSteps = 0L;
            } else {
                this.drainProcessActive = true;
                this.drainRemainingSteps = processes.get(0).expectedMs();
            }
        }

        @Override
        public void exitArchiveLoadingPhaseAndEnableEvents() {
            this.loadingArchive = false;
        }
    }
}

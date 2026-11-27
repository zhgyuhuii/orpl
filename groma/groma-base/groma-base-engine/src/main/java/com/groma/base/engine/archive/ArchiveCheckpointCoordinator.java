package com.groma.base.engine.archive;

import java.util.List;
import java.util.UUID;

/**
 * 步进边界存档导出与无损续跑恢复器（落实 BE-TSK-006，彻底规避 BE-03 与 BL-01）
 */
public final class ArchiveCheckpointCoordinator {

    public interface ArchivableWorld {
        long currentTickIndex();
        SimulationArchiveBundle.TaskArchivePart exportTaskHeader(String archiveId, long tickIndex);
        List<SimulationArchiveBundle.InstanceArchivePart> exportInstances(String archiveId, long tickIndex);
        List<SimulationArchiveBundle.ProcessArchivePart> exportProcesses(String archiveId, long tickIndex);
        List<SimulationArchiveBundle.RelationLinkArchivePart> exportRelations(String archiveId, long tickIndex);

        void enterArchiveLoadingPhase();
        void restoreTaskHeader(SimulationArchiveBundle.TaskArchivePart header);
        void restoreInstances(List<SimulationArchiveBundle.InstanceArchivePart> instances);
        void restoreRelations(List<SimulationArchiveBundle.RelationLinkArchivePart> relations);
        void restoreProcesses(List<SimulationArchiveBundle.ProcessArchivePart> processes);
        void exitArchiveLoadingPhaseAndEnableEvents();
    }

    /**
     * 必须在单写者推演循环的步进边界调用，保证导出的三部分状态完全一致
     */
    public SimulationArchiveBundle createCheckpointAtStepBoundary(ArchivableWorld world) {
        String archiveId = "arc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long tick = world.currentTickIndex();
        return new SimulationArchiveBundle(
                SimulationArchiveBundle.CURRENT_SCHEMA_VERSION,
                archiveId,
                tick,
                world.exportTaskHeader(archiveId, tick),
                List.copyOf(world.exportInstances(archiveId, tick)),
                List.copyOf(world.exportProcesses(archiveId, tick)),
                List.copyOf(world.exportRelations(archiveId, tick))
        );
    }

    /**
     * 从存档重载：校验格式兼容性 -> 恢复头/实例/关联关系(修复 BL-01)/活跃过程 -> 立即退出装载态恢复事件触发(修复 BE-03)
     */
    public void restoreIntoWorld(SimulationArchiveBundle bundle, ArchivableWorld targetWorld) {
        validateCompatibility(bundle);
        targetWorld.enterArchiveLoadingPhase();
        try {
            targetWorld.restoreTaskHeader(bundle.taskPart());
            targetWorld.restoreInstances(bundle.instanceParts());
            targetWorld.restoreRelations(bundle.relationParts());
            targetWorld.restoreProcesses(bundle.processParts());
        } finally {
            // 无论如何必须在退出装载阶段时复位装载标记，确保运行期所有对象事件触发耗时过程 100% 正常工作！
            targetWorld.exitArchiveLoadingPhaseAndEnableEvents();
        }
    }

    public void validateCompatibility(SimulationArchiveBundle bundle) {
        if (bundle == null) {
            throw new ArchiveFormatException("存档数据为空");
        }
        if (bundle.schemaVersion() < SimulationArchiveBundle.MIN_COMPATIBLE_SCHEMA_VERSION
                || bundle.schemaVersion() > SimulationArchiveBundle.CURRENT_SCHEMA_VERSION) {
            throw new ArchiveFormatException("不兼容的存档格式版本: schemaVersion=" + bundle.schemaVersion()
                    + "（支持范围: " + SimulationArchiveBundle.MIN_COMPATIBLE_SCHEMA_VERSION
                    + "~" + SimulationArchiveBundle.CURRENT_SCHEMA_VERSION + "）");
        }
        String aid = bundle.archiveId();
        long tick = bundle.tickIndex();
        if (!aid.equals(bundle.taskPart().archiveId()) || tick != bundle.taskPart().tickIndex()) {
            throw new ArchiveFormatException("存档头标识或步进索引不一致");
        }
    }
}

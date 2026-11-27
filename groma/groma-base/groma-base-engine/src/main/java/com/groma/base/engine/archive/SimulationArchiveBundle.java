package com.groma.base.engine.archive;

import java.util.List;
import java.util.Map;

/**
 * 步进边界三合一不可变存档包（落实 BE-TSK-006 & BE-PER-004：三部分共享同一 archiveId 与 tickIndex，完整保留关联关系）
 */
public record SimulationArchiveBundle(
        int schemaVersion,
        String archiveId,
        long tickIndex,
        TaskArchivePart taskPart,
        List<InstanceArchivePart> instanceParts,
        List<ProcessArchivePart> processParts,
        List<RelationLinkArchivePart> relationParts
) {
    public static final int CURRENT_SCHEMA_VERSION = 2; // v2 追加 TaskArchivePart.instanceSeqCursor（向后兼容：旧存档缺字段缺省 0，恢复取 max 不改变行为）
    public static final int MIN_COMPATIBLE_SCHEMA_VERSION = 1;

    public record TaskArchivePart(
            String archiveId,
            long tickIndex,
            String modelCode,
            String modelVersion,
            String sceneCode,
            long startEpochMs,
            long finalStepMs,
            long rootSeed,
            long rngDrawCursor,
            double requestedSpeed,
            long instanceSeqCursor
    ) {}

    public record InstanceArchivePart(
            String archiveId,
            long tickIndex,
            String instanceId,
            String typeCode,
            boolean alive,
            String currentState,
            Map<String, Object> fieldValues
    ) {}

    /**
     * runtimeState 保存无栈状态机的续跑现场（就绪 pc 队列、等待表、形参槽位与局部量），由编译产出的过程任务零反射导出与回填。
     * wiringContextInstanceId 为触发接线启动时的 context 实例标识（空串 = 无接线上下文）；
     * 续跑重建任务后由生成代码按它重挂输出影响回写钩子（T1.5 分叉根因二）。
     */
    public record ProcessArchivePart(
            String archiveId,
            long tickIndex,
            String processInstanceId,
            String processDefId,
            String hostInstanceId,
            String status,
            long localSteps,
            long elapsedMs,
            long expectedMs,
            int subGraphPc,
            Map<String, Object> runtimeState,
            String wiringContextInstanceId
    ) {
        public ProcessArchivePart(
                String archiveId, long tickIndex, String processInstanceId, String processDefId, String hostInstanceId,
                String status, long localSteps, long elapsedMs, long expectedMs, int subGraphPc) {
            this(archiveId, tickIndex, processInstanceId, processDefId, hostInstanceId, status,
                    localSteps, elapsedMs, expectedMs, subGraphPc, Map.of());
        }

        public ProcessArchivePart(
                String archiveId, long tickIndex, String processInstanceId, String processDefId, String hostInstanceId,
                String status, long localSteps, long elapsedMs, long expectedMs, int subGraphPc,
                Map<String, Object> runtimeState) {
            this(archiveId, tickIndex, processInstanceId, processDefId, hostInstanceId, status,
                    localSteps, elapsedMs, expectedMs, subGraphPc, runtimeState, "");
        }

        /** 兼容旧存档 JSON：缺 wiringContextInstanceId 字段时视为无接线上下文 */
        public ProcessArchivePart {
            if (wiringContextInstanceId == null) {
                wiringContextInstanceId = "";
            }
        }
    }

    public record RelationLinkArchivePart(
            String archiveId,
            long tickIndex,
            String relationType,
            String leftInstanceId,
            String rightInstanceId
    ) {}
}

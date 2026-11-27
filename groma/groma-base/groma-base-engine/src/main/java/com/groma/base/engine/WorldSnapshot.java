package com.groma.base.engine;

import java.util.List;
import java.util.Map;

/**
 * 步进边界原子发布的不可变世界快照（REST、WebSocket、MCP 三通道共享同一份快照，落实 NP-ENG-002 / NP-HOST-003）
 */
public record WorldSnapshot(
        String sessionId,
        String modelCode,
        String modelVersion,
        String sceneCode,
        long seed,
        long tickIndex,
        long worldTimeMs,
        double currentSpeed,
        String status,
        Map<String, Map<String, Object>> instances,
        List<ProcessSnapItem> activeProcesses,
        List<SimEventRecord> recentEvents,
        List<String> problems
) {
    /**
     * 活跃过程快照项。processId 为过程实例标识，defKey / hostInstanceId 给出过程定义与宿主对象（编译产出模型填充）。
     */
    public record ProcessSnapItem(
            String processId, String name, String status, long elapsedMs, long expectedMs, String defKey, String hostInstanceId) {
        public ProcessSnapItem(String processId, String name, String status, long elapsedMs, long expectedMs) {
            this(processId, name, status, elapsedMs, expectedMs, "", "");
        }
    }

    /**
     * 对象事件记录。memberToken / stateLabel / subjectId 是编译产出模型运行期的富事件载荷：
     * memberToken 定位发生事件的成员，stateLabel 记录状态进入/退出(sa/sx)的状态中文原名，subjectId 记录创建事件(a)对应的新生对象。
     */
    public record SimEventRecord(
            long seq,
            long tick,
            String eventCode,
            String sourceId,
            String detail,
            String memberToken,
            String stateLabel,
            String subjectId
    ) {
        public SimEventRecord(long seq, long tick, String eventCode, String sourceId, String detail) {
            this(seq, tick, eventCode, sourceId, detail, "", "", "");
        }
    }
}

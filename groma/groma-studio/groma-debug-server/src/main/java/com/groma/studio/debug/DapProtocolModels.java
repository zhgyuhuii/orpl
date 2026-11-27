package com.groma.studio.debug;

import java.util.List;
import java.util.Map;

/**
 * 标准 Debug Adapter Protocol (DAP) 协议帧与方圆图码三维三向联动扩展字段
 */
public final class DapProtocolModels {

    private DapProtocolModels() {
    }

    public record BreakpointSpec(
            String relativeFile,
            int line,
            String conditionExpression,
            String logMessageTemplate
    ) {}

    public record StackFrameDTO(
            int frameId,
            String methodName,
            String relativeFile,
            int line,
            long simulationTick,
            String canvasElementId,
            String focusEntityId
    ) {}

    public record DapRequest(
            int seq,
            String command,
            Map<String, Object> arguments
    ) {}

    public record DapResponse(
            int requestSeq,
            boolean success,
            String command,
            String message,
            Map<String, Object> body
    ) {}

    public record DapStoppedEvent(
            String type,
            String event,
            String reason,
            String sessionId,
            String relativeFile,
            int line,
            long simulationTick,
            String highlightedCanvasElementId,
            String highlighted3dEntityId,
            List<StackFrameDTO> callStack,
            Map<String, Object> inlineVariables
    ) {
        public static DapStoppedEvent create(
                String reason,
                String sessionId,
                String relativeFile,
                int line,
                long tick,
                String canvasId,
                String entityId,
                List<StackFrameDTO> stack,
                Map<String, Object> vars) {
            return new DapStoppedEvent(
                    "event", "stopped", reason, sessionId, relativeFile, line,
                    tick, canvasId, entityId, stack, vars);
        }
    }
}

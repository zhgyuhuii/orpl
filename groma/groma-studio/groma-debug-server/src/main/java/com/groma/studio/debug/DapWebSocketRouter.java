package com.groma.studio.debug;

import com.groma.base.connector.ExpressionSandboxEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准 DAP 调试协议命令路由器（对接前端 CodeMirror 6 断点编辑器与调试控制栏）
 */
public final class DapWebSocketRouter {

    private final ConcurrentHashMap<String, VirtualThreadDebugSession> sessions = new ConcurrentHashMap<>();
    private final ExpressionSandboxEvaluator watchEvaluator = new ExpressionSandboxEvaluator(true);

    public void registerDebugSession(String sessionId, VirtualThreadDebugSession session) {
        sessions.put(sessionId, session);
    }

    public void unregisterDebugSession(String sessionId) {
        VirtualThreadDebugSession removed = sessions.remove(sessionId);
        if (removed != null) {
            removed.resumeExecution(VirtualThreadDebugSession.StepResumeAction.TERMINATE);
        }
    }

    @SuppressWarnings("unchecked")
    public DapProtocolModels.DapResponse handleDapRequest(String sessionId, DapProtocolModels.DapRequest req) {
        VirtualThreadDebugSession session = sessions.get(sessionId);
        if (session == null) {
            return new DapProtocolModels.DapResponse(req.seq(), false, req.command(), "DEBUG_SESSION_NOT_FOUND", Map.of());
        }
        Map<String, Object> args = req.arguments() == null ? Map.of() : req.arguments();

        return switch (req.command()) {
            case "initialize" -> new DapProtocolModels.DapResponse(req.seq(), true, "initialize", "OK", Map.of(
                    "supportsConditionalBreakpoints", true,
                    "supportsLogPoints", true,
                    "supportsSetVariable", true,
                    "supportsNextSimulationTick", true
            ));
            case "setBreakpoints" -> {
                String file = String.valueOf(args.getOrDefault("file", ""));
                List<Map<String, Object>> rawBps = (List<Map<String, Object>>) args.getOrDefault("breakpoints", List.of());
                List<DapProtocolModels.BreakpointSpec> specs = new ArrayList<>(rawBps.size());
                for (Map<String, Object> b : rawBps) {
                    int line = ((Number) b.getOrDefault("line", 1)).intValue();
                    String cond = (String) b.getOrDefault("condition", "");
                    String logMsg = (String) b.getOrDefault("logMessage", "");
                    specs.add(new DapProtocolModels.BreakpointSpec(file, line, cond, logMsg));
                }
                session.replaceFileBreakpoints(file, specs);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "setBreakpoints", "OK", Map.of("verifiedCount", specs.size()));
            }
            case "continue" -> {
                session.resumeExecution(VirtualThreadDebugSession.StepResumeAction.CONTINUE);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "continue", "OK", Map.of());
            }
            case "next" -> {
                session.resumeExecution(VirtualThreadDebugSession.StepResumeAction.STEP_OVER);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "next", "OK", Map.of());
            }
            case "stepIn" -> {
                session.resumeExecution(VirtualThreadDebugSession.StepResumeAction.STEP_INTO);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "stepIn", "OK", Map.of());
            }
            case "stepOut" -> {
                session.resumeExecution(VirtualThreadDebugSession.StepResumeAction.STEP_OUT);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "stepOut", "OK", Map.of());
            }
            case "nextSimulationTick" -> {
                session.resumeExecution(VirtualThreadDebugSession.StepResumeAction.NEXT_SIMULATION_TICK);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "nextSimulationTick", "OK", Map.of());
            }
            case "variables" -> new DapProtocolModels.DapResponse(
                    req.seq(), true, "variables", "OK", Map.of("variables", session.currentSuspendedVariables()));
            case "setVariable" -> {
                String name = String.valueOf(args.getOrDefault("name", ""));
                Object val = args.get("value");
                boolean ok = session.mutateSuspendedVariable(name, val);
                yield new DapProtocolModels.DapResponse(req.seq(), ok, "setVariable", ok ? "OK" : "NOT_SUSPENDED", Map.of("value", String.valueOf(val)));
            }
            case "evaluate" -> {
                String expr = String.valueOf(args.getOrDefault("expression", ""));
                boolean result = watchEvaluator.evaluateBoolean(expr, session.currentSuspendedVariables());
                yield new DapProtocolModels.DapResponse(req.seq(), true, "evaluate", "OK", Map.of("result", result));
            }
            case "disconnect" -> {
                unregisterDebugSession(sessionId);
                yield new DapProtocolModels.DapResponse(req.seq(), true, "disconnect", "OK", Map.of());
            }
            default -> new DapProtocolModels.DapResponse(req.seq(), false, req.command(), "UNSUPPORTED_DAP_COMMAND", Map.of());
        };
    }
}

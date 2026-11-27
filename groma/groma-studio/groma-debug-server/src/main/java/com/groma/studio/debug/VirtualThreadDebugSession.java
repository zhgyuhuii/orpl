package com.groma.studio.debug;

import com.groma.base.connector.ExpressionSandboxEvaluator;
import com.groma.studio.codegen.AstSourceMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * JDK 21 虚拟线程非阻塞软断点会话引擎（支持普通断点、条件断点、日志断点、异常断点、在线改值与下一仿真步进断点）
 */
public final class VirtualThreadDebugSession {

    public enum StepResumeAction {
        CONTINUE,
        STEP_OVER,
        STEP_INTO,
        STEP_OUT,
        NEXT_SIMULATION_TICK,
        TERMINATE
    }

    private final String sessionId;
    private final AstSourceMap astSourceMap;
    private final ClockFreezeCompensator clockCompensator;
    private final ExpressionSandboxEvaluator conditionEvaluator = new ExpressionSandboxEvaluator(true);

    private final ConcurrentHashMap<String, DapProtocolModels.BreakpointSpec> activeBreakpoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> mutableFrameVariables = new ConcurrentHashMap<>();
    private final List<String> logpointConsoleBuffer = new ArrayList<>();
    private final AtomicReference<CompletableFuture<StepResumeAction>> parkGate = new AtomicReference<>();

    private volatile boolean breakOnUncaughtException = true;
    private volatile boolean stepSingleLineRequested = false;
    private volatile Long pauseAtTargetTick = null;

    public VirtualThreadDebugSession(String sessionId, AstSourceMap astSourceMap, long initialNano) {
        this.sessionId = sessionId;
        this.astSourceMap = astSourceMap;
        this.clockCompensator = new ClockFreezeCompensator(initialNano);
    }

    public void replaceFileBreakpoints(String relativeFile, List<DapProtocolModels.BreakpointSpec> specs) {
        String normFile = normalizePath(relativeFile);
        activeBreakpoints.keySet().removeIf(k -> k.startsWith(normFile + ":"));
        for (DapProtocolModels.BreakpointSpec spec : specs) {
            activeBreakpoints.put(normFile + ":" + spec.line(), spec);
        }
    }

    public void setBreakOnUncaughtException(boolean enabled) {
        this.breakOnUncaughtException = enabled;
    }

    /**
     * 在线热修改当前栈帧中的局部变量或实体属性值（Set Value 能力）
     */
    public boolean mutateSuspendedVariable(String varName, Object newValue) {
        if (parkGate.get() == null || varName == null || varName.isBlank()) {
            return false;
        }
        mutableFrameVariables.put(varName, newValue);
        return true;
    }

    public Map<String, Object> currentSuspendedVariables() {
        return Map.copyOf(mutableFrameVariables);
    }

    public List<String> drainLogpointOutputs() {
        List<String> copy = List.copyOf(logpointConsoleBuffer);
        logpointConsoleBuffer.clear();
        return copy;
    }

    public ClockFreezeCompensator clockCompensator() {
        return clockCompensator;
    }

    /**
     * 业务代码行探针钩子：仅在命中条件时挂起当前调试虚拟线程，绝不阻塞底层平台线程
     */
    public Map<String, Object> onLineProbe(
            String relativeFile,
            int line,
            String methodName,
            long currentSimTick,
            String currentEntityId,
            Map<String, Object> liveVars,
            Consumer<DapProtocolModels.DapStoppedEvent> eventSink) throws Exception {

        String normFile = normalizePath(relativeFile);
        DapProtocolModels.BreakpointSpec bp = activeBreakpoints.get(normFile + ":" + line);

        boolean hitTickBoundary = (pauseAtTargetTick != null && currentSimTick >= pauseAtTargetTick);
        boolean hitStep = stepSingleLineRequested;
        boolean hitBreakpoint = false;

        if (bp != null) {
            // 1. 先检查条件断点表达式
            boolean condPassed = (bp.conditionExpression() == null || bp.conditionExpression().isBlank())
                    || conditionEvaluator.evaluateBoolean(bp.conditionExpression(), liveVars);
            if (condPassed) {
                // 2. 若配置了 Logpoint 日志模板，则求值输出日志且不挂起推演！
                if (bp.logMessageTemplate() != null && !bp.logMessageTemplate().isBlank()) {
                    logpointConsoleBuffer.add(renderLogpoint(bp.logMessageTemplate(), liveVars, currentSimTick));
                } else {
                    hitBreakpoint = true;
                }
            }
        }

        if (!hitTickBoundary && !hitStep && !hitBreakpoint) {
            return liveVars;
        }

        stepSingleLineRequested = false;
        if (hitTickBoundary) {
            pauseAtTargetTick = null;
        }

        String reason = hitBreakpoint ? "breakpoint" : (hitTickBoundary ? "nextSimulationTick" : "step");
        return suspendAndWaitResume(reason, normFile, line, methodName, currentSimTick, currentEntityId, liveVars, eventSink);
    }

    /**
     * 未捕获异常断点钩子：在抛异常行保留完整现场挂起
     */
    public void onExceptionProbe(
            Throwable thrown,
            String relativeFile,
            int line,
            String methodName,
            long currentSimTick,
            String currentEntityId,
            Map<String, Object> liveVars,
            Consumer<DapProtocolModels.DapStoppedEvent> eventSink) throws Exception {
        if (!breakOnUncaughtException) {
            return;
        }
        Map<String, Object> enriched = new LinkedHashMap<>(liveVars);
        enriched.put("$exception", thrown.getClass().getSimpleName() + ": " + thrown.getMessage());
        suspendAndWaitResume("exception", normalizePath(relativeFile), line, methodName, currentSimTick, currentEntityId, enriched, eventSink);
    }

    private final java.util.concurrent.atomic.AtomicInteger suspendCount = new java.util.concurrent.atomic.AtomicInteger();

    /** 调试挂起次数（演练/验收用） */
    public int suspendCount() {
        return suspendCount.get();
    }

    /** 当前断点键（演练/验收用） */
    public java.util.List<String> activeBreakpointKeys() {
        return java.util.List.copyOf(activeBreakpoints.keySet());
    }

    private Map<String, Object> suspendAndWaitResume(
            String reason,
            String normFile,
            int line,
            String methodName,
            long currentSimTick,
            String currentEntityId,
            Map<String, Object> liveVars,
            Consumer<DapProtocolModels.DapStoppedEvent> eventSink) throws Exception {

        mutableFrameVariables.clear();
        mutableFrameVariables.putAll(liveVars);
        suspendCount.incrementAndGet();
        clockCompensator.enterDebugFreeze(System.nanoTime());

        String canvasElementId = astSourceMap == null
                ? ""
                : astSourceMap.findElementIdByFileAndLine(normFile, line).orElse("");

        List<DapProtocolModels.StackFrameDTO> stack = List.of(
                new DapProtocolModels.StackFrameDTO(
                        1, methodName, normFile, line, currentSimTick, canvasElementId, currentEntityId == null ? "" : currentEntityId)
        );

        CompletableFuture<StepResumeAction> gate = new CompletableFuture<>();
        parkGate.set(gate);

        if (eventSink != null) {
            eventSink.accept(DapProtocolModels.DapStoppedEvent.create(
                    reason,
                    sessionId,
                    normFile,
                    line,
                    currentSimTick,
                    canvasElementId,
                    currentEntityId == null ? "" : currentEntityId,
                    stack,
                    Map.copyOf(mutableFrameVariables)
            ));
        }

        StepResumeAction action = gate.get(30, TimeUnit.MINUTES);
        clockCompensator.exitDebugFreezeAndShiftDeadline(System.nanoTime());

        if (action == StepResumeAction.STEP_OVER || action == StepResumeAction.STEP_INTO || action == StepResumeAction.STEP_OUT) {
            stepSingleLineRequested = true;
        } else if (action == StepResumeAction.NEXT_SIMULATION_TICK) {
            pauseAtTargetTick = currentSimTick + 1L;
        }
        return Map.copyOf(mutableFrameVariables);
    }

    public void resumeExecution(StepResumeAction action) {
        CompletableFuture<StepResumeAction> gate = parkGate.getAndSet(null);
        if (gate != null) {
            gate.complete(action);
        }
    }

    private static String renderLogpoint(String template, Map<String, Object> vars, long tick) {
        String rendered = "[Tick=" + tick + "] " + template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    private static String normalizePath(String p) {
        return p == null ? "" : p.replace('\\', '/');
    }
}

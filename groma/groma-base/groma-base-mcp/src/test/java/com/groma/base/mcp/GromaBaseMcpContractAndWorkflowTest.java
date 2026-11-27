package com.groma.base.mcp;

import com.groma.base.engine.WorldSnapshot;
import com.groma.base.mcp.config.GromaMcpProperties;
import com.groma.base.mcp.context.GatewayIdentityContext;
import com.groma.base.mcp.manifest.GromaToolManifest;
import com.groma.base.mcp.observability.GromaMcpMetricsRecorder;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.mcp.protocol.GromaMcpException;
import com.groma.base.mcp.protocol.McpErrorCode;
import com.groma.base.mcp.protocol.McpToolEnvelope;
import com.groma.base.mcp.protocol.PagedResult;
import com.groma.base.mcp.registry.GromaModelResourceProvider;
import com.groma.base.mcp.registry.ModelToolRegistrar;
import com.groma.base.mcp.tools.GromaFixedMcpTools;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;

/**
 * 覆盖 NP-MCP-001~010、十类错误码全路径、七类只读资源、动态注册与三大智能体工作流（SYS-M-01~05）的完整契约单测
 */
class GromaBaseMcpContractAndWorkflowTest {

    private SimpleMeterRegistry meterRegistry;
    private GromaMcpProperties properties;
    private InMemoryFakeEnginePort fakeEngine;
    private GromaMcpMetricsRecorder metricsRecorder;
    private GromaFixedMcpTools fixedTools;
    private GromaModelResourceProvider resourceProvider;
    private ModelToolRegistrar registrar;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new GromaMcpProperties();
        properties.setMaxConcurrentSessions(3);
        properties.setDefaultPageSize(2);
        properties.setMaxPageSize(5);

        fakeEngine = new InMemoryFakeEnginePort();
        metricsRecorder = new GromaMcpMetricsRecorder(meterRegistry, fakeEngine);
        fixedTools = new GromaFixedMcpTools(fakeEngine, properties, metricsRecorder);
        resourceProvider = new GromaModelResourceProvider(fakeEngine);
        registrar = new ModelToolRegistrar(null, fakeEngine, properties, metricsRecorder);
    }

    @Test
    void workflow1_observationAgentReads7ResourcesStartsWaitsPaginatesAndStops() {
        // SYS-M-01 观察型工作流 + NP-MCP-008 七类资源全解析 + SYS-M-05 分页截断校验
        Assertions.assertTrue(resourceProvider.readModelIndex("emergency-deduction").contains("应急决策推演"));
        Assertions.assertTrue(resourceProvider.readAllTypes("emergency-deduction").contains("MetroSection"));
        Assertions.assertTrue(resourceProvider.readSingleType("emergency-deduction", "MetroSection").contains("waterDepth"));
        Assertions.assertTrue(resourceProvider.readAllProcesses("emergency-deduction").contains("calc_drain_flow"));
        Assertions.assertTrue(resourceProvider.readSingleProcess("emergency-deduction", "calc_drain_flow").contains("inputSchema"));
        Assertions.assertTrue(resourceProvider.readAllScenes("emergency-deduction").contains("flood_scene"));
        Assertions.assertTrue(resourceProvider.readSingleScene("emergency-deduction", "flood_scene").contains("flood_scene"));

        McpToolEnvelope<GromaMcpEnginePort.SessionSummary> started =
                fixedTools.startSession("emergency-deduction", "flood_scene", Map.of(), 42L, false);
        Assertions.assertFalse(started.isError());
        String sid = started.data().sessionId();

        McpToolEnvelope<GromaMcpEnginePort.WaitOutcome> waited =
                fixedTools.waitForCondition(null, "prog_1", sid, 10L, null, null, 5000L);
        Assertions.assertFalse(waited.isError());
        Assertions.assertEquals(10L, waited.data().finalTickIndex());

        // 分页与字段裁剪验证（共 3 个实例，defaultPageSize=2 -> 第一页 truncated=true 且返回 nextCursor="2"）
        McpToolEnvelope<PagedResult<Map<String, Object>>> page1 =
                fixedTools.queryInstances(sid, null, null, null, List.of("waterDepth"), null, null);
        Assertions.assertFalse(page1.isError());
        Assertions.assertEquals(2, page1.data().items().size());
        Assertions.assertTrue(page1.data().truncated());
        Assertions.assertEquals("2", page1.data().nextCursor());
        Assertions.assertFalse(page1.data().items().get(0).containsKey("hiddenAttr"));

        McpToolEnvelope<PagedResult<Map<String, Object>>> page2 =
                fixedTools.queryInstances(sid, null, null, null, List.of("waterDepth"), page1.data().nextCursor(), null);
        Assertions.assertEquals(1, page2.data().items().size());
        Assertions.assertFalse(page2.data().truncated());

        Assertions.assertFalse(fixedTools.getProcesses(sid, "RUNNING", null, null).isError());
        Assertions.assertFalse(fixedTools.stopSession(sid).isError());
        Assertions.assertEquals(0, fakeEngine.activeSessionCount());
    }

    @Test
    void workflow2_computationAgentDescribesAndRunsStatelessWithoutSessionLeak() {
        // SYS-M-02 计算型工作流：查结构 -> 无状态调用算子 -> 零残留会话
        McpToolEnvelope<GromaToolManifest.ProcessToolDef> desc =
                fixedTools.describeProcess("emergency-deduction", "calc_drain_flow", "825");
        Assertions.assertFalse(desc.isError());
        Assertions.assertEquals("operator", desc.data().kind());

        McpToolEnvelope<GromaMcpEnginePort.ProcessExecutionOutcome> out =
                fixedTools.runProcess("emergency-deduction", "calc_drain_flow", Map.of("inflowRate", 12.5D), null, null, null);
        Assertions.assertFalse(out.isError());
        Assertions.assertTrue(out.data().stateless());
        Assertions.assertEquals(25.0D, out.data().outputs().get("drainedVolume"));
        Assertions.assertEquals(0, fakeEngine.activeSessionCount(), "无状态调用结束后临时上下文必须清理干净");
    }

    @Test
    void workflow3_interventionAgentStepsMutatesValueAndTriggersProcess() {
        // SYS-M-03 干预型工作流：启动 -> 单步 -> 改值/切状态 -> 手动执行过程 -> 查询
        String sid = fixedTools.startSession("emergency-deduction", "flood_scene", null, 99L, true).data().sessionId();
        Assertions.assertEquals(5L, fixedTools.stepSession(sid, 5).data().tickIndex());
        Assertions.assertFalse(fixedTools.setInstanceValue(sid, "sec_1", "waterDepth", 0.85D).isError());
        Assertions.assertFalse(fixedTools.setInstanceState(sid, "sec_1", "floodState", "积水").isError());
        Assertions.assertFalse(fixedTools.triggerProcessInSession(sid, "sec_1", "calc_drain_flow", Map.of("inflowRate", 5.0D)).isError());

        Map<String, Object> updatedInst = fixedTools.getInstance(sid, "sec_1").data();
        Assertions.assertEquals(0.85D, updatedInst.get("waterDepth"));
        Assertions.assertEquals("积水", updatedInst.get("_state"));
        fixedTools.stopSession(sid);
    }

    @Test
    void dynamicRegistrarPrefixesNamesAndCleansUpOnUnmountAndExtractsGatewayHeaders() {
        // CK-NP-MCP-003-1~3 & CK-NP-MCP-010-2
        GromaToolManifest manifest = fakeEngine.findMountedModel("emergency-deduction", null).orElseThrow().toolManifest();
        List<String> mounted = registrar.onModelMountedSync(manifest);
        Assertions.assertEquals(List.of("emergency_deduction__run_calc_drain_flow"), mounted);
        Assertions.assertTrue(registrar.listRegisteredToolNamesForModel("emergency-deduction")
                .contains("emergency_deduction__run_calc_drain_flow"));

        List<String> removed = registrar.onModelUnmounted("emergency-deduction");
        Assertions.assertEquals(1, removed.size());
        Assertions.assertTrue(registrar.listRegisteredToolNamesForModel("emergency-deduction").isEmpty());

        // 验证网关透传请求头提取钩子
        GatewayIdentityContext.GatewayHeaders gh = GatewayIdentityContext.extractFromMap(Map.of(
                "X-Request-Id", "req_abc_123",
                "X-Gateway-Subject", "planner_01",
                "X-Agent-Id", "emergency_agent"
        ));
        GatewayIdentityContext.bind(gh);
        try {
            McpToolEnvelope<List<GromaMcpEnginePort.SessionSummary>> res = fixedTools.listSessions();
            Assertions.assertEquals("req_abc_123", res.traceId());
            Assertions.assertEquals("emergency_agent", GatewayIdentityContext.current().agentId());
        } finally {
            GatewayIdentityContext.clear();
        }
    }

    @Test
    void allTenMcpErrorCodesHaveTriggerPathsAndRecordMetrics() {
        // CK-NP-MCP-009-1 & 2：验证错误码表全部 10 个枚举码均可精确触发且被指标统计捕获
        Set<McpErrorCode> observedCodes = EnumSet.noneOf(McpErrorCode.class);

        // 1. MODEL_NOT_FOUND
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.describeProcess("unknown-model", "p1", null).errorCode()));
        // 2. MODEL_VERSION_MISMATCH
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.describeProcess("emergency-deduction", "calc_drain_flow", "9999").errorCode()));
        // 3. SCENE_NOT_FOUND
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.startSession("emergency-deduction", "missing_scene", null, null, false).errorCode()));
        // 4. SESSION_NOT_FOUND
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.describeSession("non_existent_sid").errorCode()));
        // 5. INVALID_ARGUMENT（入参超出 Schema maximum=500）
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.runProcess("emergency-deduction", "calc_drain_flow", Map.of("inflowRate", 9999.0D), null, null, null).errorCode()));
        // 6. PRECONDITION_FAILED
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.runProcess("emergency-deduction", "calc_drain_flow", Map.of("inflowRate", 1.0D, "simulateMode", "PRECOND_FAIL"), null, null, null).errorCode()));
        // 7. MAINTAIN_BROKEN
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.runProcess("emergency-deduction", "calc_drain_flow", Map.of("inflowRate", 1.0D, "simulateMode", "MAINTAIN_FAIL"), null, null, null).errorCode()));
        // 8. PROCESS_FAILED
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.runProcess("emergency-deduction", "calc_drain_flow", Map.of("inflowRate", 1.0D, "simulateMode", "PROC_CRASH"), null, null, null).errorCode()));
        // 9. TIMEOUT
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.runProcess("emergency-deduction", "calc_drain_flow", Map.of("inflowRate", 1.0D, "simulateMode", "HANG_TIMEOUT"), null, null, 50L).errorCode()));
        // 10. LIMIT_EXCEEDED（并发上限设为 3，连续创建第 4 个触发）
        fixedTools.startSession("emergency-deduction", "flood_scene", null, 1L, false);
        fixedTools.startSession("emergency-deduction", "flood_scene", null, 2L, false);
        fixedTools.startSession("emergency-deduction", "flood_scene", null, 3L, false);
        observedCodes.add(McpErrorCode.valueOf(
                fixedTools.startSession("emergency-deduction", "flood_scene", null, 4L, false).errorCode()));

        Assertions.assertEquals(EnumSet.allOf(McpErrorCode.class), observedCodes, "10 类标准业务错误码必须 100% 覆盖");
        Assertions.assertTrue(meterRegistry.find("groma.mcp.tool.calls").counters().stream()
                .mapToDouble(c -> c.count()).sum() >= 10.0D);
        Assertions.assertTrue(meterRegistry.find("groma.mcp.tool.failures").counters().stream()
                .mapToDouble(c -> c.count()).sum() >= 10.0D);
    }

    /**
     * 用于契约单测的内存单写者端口桩实现
     */
    private static final class InMemoryFakeEnginePort implements GromaMcpEnginePort {
        private final MountedModelMetadata demoModel;
        private final ConcurrentHashMap<String, MutableSimState> sessions = new ConcurrentHashMap<>();
        private final AtomicInteger idGen = new AtomicInteger(0);

        InMemoryFakeEnginePort() {
            Map<String, Object> inSchema = Map.of(
                    "type", "object",
                    "required", List.of("inflowRate"),
                    "properties", Map.of(
                            "inflowRate", Map.of("type", "number", "minimum", 0.0D, "maximum", 500.0D, "unit", "m3/s"),
                            "simulateMode", Map.of("type", "string")
                    )
            );
            Map<String, Object> outSchema = Map.of(
                    "type", "object",
                    "properties", Map.of("drainedVolume", Map.of("type", "number"))
            );
            GromaToolManifest manifest = new GromaToolManifest(
                    "1.0", "emergency-deduction", "825", "应急决策推演", "sha_demo",
                    List.of(new GromaToolManifest.ProcessToolDef(
                            "calc_drain_flow", "p_01", "计算区间排水量", "根据入流速率计算单步排水体积",
                            "operator", false, inSchema, outSchema, List.of("区间存活且水深>=0")
                    ))
            );
            this.demoModel = new MountedModelMetadata(
                    "emergency-deduction", "825", "应急决策推演",
                    "# GROMA 模型全局语义导航索引：应急决策推演\n",
                    manifest,
                    List.of(Map.of("code", "MetroSection", "name", "地铁区间", "fields", List.of("waterDepth"))),
                    List.of(Map.of("code", "flood_scene", "name", "三站两区间积水与列车运行"))
            );
        }

        @Override
        public List<MountedModelMetadata> listMountedModels() {
            return List.of(demoModel);
        }

        @Override
        public Optional<MountedModelMetadata> findMountedModel(String modelCode, String expectedVersionOrNull) {
            if (!demoModel.modelCode().equals(modelCode)) {
                return Optional.empty();
            }
            if (expectedVersionOrNull != null && !expectedVersionOrNull.isBlank()
                    && !demoModel.modelVersion().equals(expectedVersionOrNull)) {
                return Optional.empty();
            }
            return Optional.of(demoModel);
        }

        @Override
        public int activeSessionCount() {
            return sessions.size();
        }

        @Override
        public SessionSummary startSession(String modelCode, String sceneCode, Map<String, Object> init, Long seed, boolean paused) {
            findMountedModel(modelCode, null)
                    .orElseThrow(() -> new GromaMcpException(McpErrorCode.MODEL_NOT_FOUND, modelCode));
            if (!"flood_scene".equals(sceneCode)) {
                throw new GromaMcpException(McpErrorCode.SCENE_NOT_FOUND, sceneCode);
            }
            String sid = "sim_" + idGen.incrementAndGet();
            sessions.put(sid, new MutableSimState(sid, paused ? "PAUSED" : "RUNNING"));
            return requireState(sid).toSummary();
        }

        @Override
        public void stopSession(String sessionId) {
            if (sessions.remove(sessionId) == null) {
                throw new GromaMcpException(McpErrorCode.SESSION_NOT_FOUND, sessionId);
            }
        }

        @Override
        public SessionSummary pauseSession(String sessionId) {
            MutableSimState s = requireState(sessionId);
            s.status = "PAUSED";
            return s.toSummary();
        }

        @Override
        public SessionSummary resumeSession(String sessionId) {
            MutableSimState s = requireState(sessionId);
            s.status = "RUNNING";
            return s.toSummary();
        }

        @Override
        public SessionSummary stepSession(String sessionId, int steps) {
            MutableSimState s = requireState(sessionId);
            s.tick += steps;
            s.status = "PAUSED";
            return s.toSummary();
        }

        @Override
        public SessionSummary setSessionSpeed(String sessionId, double speed) {
            MutableSimState s = requireState(sessionId);
            s.speed = speed;
            return s.toSummary();
        }

        @Override
        public WaitOutcome waitForSessionCondition(
                String sessionId, Long targetTick, String waitForProcessId,
                String conditionExpr, long timeoutMs, DoubleConsumer progressCallback) {
            MutableSimState s = requireState(sessionId);
            if (progressCallback != null) {
                progressCallback.accept(0.5D);
            }
            if (targetTick != null && targetTick > s.tick) {
                s.tick = targetTick;
            }
            if (progressCallback != null) {
                progressCallback.accept(1.0D);
            }
            return new WaitOutcome("REACHED_STEP", s.tick, s.tick * 1000L, s.status);
        }

        @Override
        public List<SessionSummary> listActiveSessions() {
            return sessions.values().stream().map(MutableSimState::toSummary).toList();
        }

        @Override
        public SessionSummary describeSession(String sessionId) {
            return requireState(sessionId).toSummary();
        }

        @Override
        public WorldSnapshot getImmutableSnapshot(String sessionId) {
            return requireState(sessionId).toSnapshot();
        }

        @Override
        public void setInstanceValue(String sessionId, String instanceId, String fieldName, Object newValue) {
            MutableSimState s = requireState(sessionId);
            Map<String, Object> inst = s.instances.get(instanceId);
            if (inst == null) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "实例不存在: " + instanceId);
            }
            inst.put(fieldName, newValue);
        }

        @Override
        public void setInstanceState(String sessionId, String instanceId, String stateFieldName, String targetStateLabel) {
            MutableSimState s = requireState(sessionId);
            Map<String, Object> inst = s.instances.get(instanceId);
            if (inst == null) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "实例不存在: " + instanceId);
            }
            inst.put("_state", targetStateLabel);
        }

        @Override
        public String triggerProcessOnInstance(String sessionId, String instanceId, String processRef, Map<String, Object> args) {
            requireState(sessionId);
            return "proc_inst_" + idGen.incrementAndGet();
        }

        @Override
        public ProcessExecutionOutcome executeProcessStateless(
                String modelCode, String processRef, Map<String, Object> validatedInputs, long wallClockTimeoutMs) {
            String mode = String.valueOf(validatedInputs.getOrDefault("simulateMode", "NORMAL"));
            return switch (mode) {
                case "PRECOND_FAIL" -> throw new GromaMcpException(McpErrorCode.PRECONDITION_FAILED, "启动条件未满足");
                case "MAINTAIN_FAIL" -> throw new GromaMcpException(McpErrorCode.MAINTAIN_BROKEN, "维持条件中断");
                case "PROC_CRASH" -> throw new GromaMcpException(McpErrorCode.PROCESS_FAILED, "算术除零异常");
                case "HANG_TIMEOUT" -> throw new GromaMcpException(McpErrorCode.TIMEOUT, "超过墙钟超时 " + wallClockTimeoutMs + "ms");
                default -> {
                    double rate = ((Number) validatedInputs.get("inflowRate")).doubleValue();
                    yield new ProcessExecutionOutcome(processRef, true, null, 0L, Map.of("drainedVolume", rate * 2.0D), List.of());
                }
            };
        }

        @Override
        public ProcessExecutionOutcome executeProcessInSession(
                String sessionId, String instanceIdOrNull, String processRef, Map<String, Object> validatedInputs) {
            requireState(sessionId);
            double rate = ((Number) validatedInputs.get("inflowRate")).doubleValue();
            return new ProcessExecutionOutcome(processRef, false, sessionId, 1000L, Map.of("drainedVolume", rate * 2.0D), List.of());
        }

        private MutableSimState requireState(String sid) {
            MutableSimState s = sessions.get(sid);
            if (s == null) {
                throw new GromaMcpException(McpErrorCode.SESSION_NOT_FOUND, "会话不存在: " + sid);
            }
            return s;
        }
    }

    private static final class MutableSimState {
        final String sessionId;
        long tick = 0L;
        double speed = 1.0D;
        String status;
        final Map<String, Map<String, Object>> instances = new LinkedHashMap<>();

        MutableSimState(String sessionId, String status) {
            this.sessionId = sessionId;
            this.status = status;
            for (int i = 1; i <= 3; i++) {
                Map<String, Object> attrs = new LinkedHashMap<>();
                attrs.put("id", "sec_" + i);
                attrs.put("_type", "MetroSection");
                attrs.put("_state", "无积水");
                attrs.put("waterDepth", 0.1D * i);
                attrs.put("hiddenAttr", "internal");
                instances.put("sec_" + i, attrs);
            }
        }

        GromaMcpEnginePort.SessionSummary toSummary() {
            return new GromaMcpEnginePort.SessionSummary(
                    sessionId, "emergency-deduction", "825", "flood_scene",
                    status, tick, tick * 1000L, speed, instances.size(), 1);
        }

        WorldSnapshot toSnapshot() {
            Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            instances.forEach((k, v) -> copy.put(k, Map.copyOf(v)));
            return new WorldSnapshot(
                    sessionId, "emergency-deduction", "825", "flood_scene", 42L,
                    tick, tick * 1000L, speed, status, Map.copyOf(copy),
                    List.of(new WorldSnapshot.ProcessSnapItem("p_run_1", "水流交换", "RUNNING", 1000L, 5000L)),
                    List.of(new WorldSnapshot.SimEventRecord(1L, tick, "v", "sec_1", "水深变化")),
                    List.of()
            );
        }
    }
}

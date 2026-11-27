package com.groma.base.mcp.tools;

import com.groma.base.engine.WorldSnapshot;
import com.groma.base.mcp.config.GromaMcpProperties;
import com.groma.base.mcp.manifest.GromaToolManifest;
import com.groma.base.mcp.manifest.ManifestSchemaValidator;
import com.groma.base.mcp.observability.GromaMcpMetricsRecorder;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.mcp.protocol.GromaMcpException;
import com.groma.base.mcp.protocol.McpErrorCode;
import com.groma.base.mcp.protocol.McpToolEnvelope;
import com.groma.base.mcp.protocol.PagedResult;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.McpProgressToken;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 宿主提供的 18 个固定 MCP 工具集（落实 NP-MCP-004、NP-MCP-005、NP-MCP-006、NP-MCP-007）
 * 注意：工具注解显式声明 readOnlyHint / destructiveHint / idempotentHint，辅助智能体安全规划动作。
 */
public class GromaFixedMcpTools {

    private final GromaMcpEnginePort enginePort;
    private final GromaMcpProperties properties;
    private final GromaMcpMetricsRecorder metrics;

    public GromaFixedMcpTools(
            GromaMcpEnginePort enginePort,
            GromaMcpProperties properties,
            GromaMcpMetricsRecorder metrics) {
        this.enginePort = enginePort;
        this.properties = properties;
        this.metrics = metrics;
    }

    // =========================================================================
    // 第一组：会话生命周期工具（9 个，NP-MCP-004）
    // =========================================================================

    @McpTool(
            name = "sim.start",
            description = "启动一个方圆推演场景会话，可指定初始覆盖值、确定性随机种子及是否以暂停态启动。会话用完请调用 sim.stop。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public McpToolEnvelope<GromaMcpEnginePort.SessionSummary> startSession(
            @McpToolParam(description = "模型编码，如 emergency-deduction", required = true) String modelCode,
            @McpToolParam(description = "场景编码，如 three_station_two_section_flood_train", required = true) String sceneCode,
            @McpToolParam(description = "可选的实例初始属性覆盖表", required = false) Map<String, Object> initialValues,
            @McpToolParam(description = "可选的确定性随机种子（相同种子保证推演完全可复现）", required = false) Long seed,
            @McpToolParam(description = "是否以暂停状态启动，默认 false", required = false) Boolean startPaused) {

        return metrics.executeTracked("sim.start", modelCode, () -> {
            if (enginePort.activeSessionCount() >= properties.getMaxConcurrentSessions()) {
                throw new GromaMcpException(McpErrorCode.LIMIT_EXCEEDED,
                        "当前并发推演会话数已达实例配额上限: " + properties.getMaxConcurrentSessions());
            }
            return enginePort.startSession(
                    requireNonBlank(modelCode, "modelCode"),
                    requireNonBlank(sceneCode, "sceneCode"),
                    initialValues == null ? Map.of() : initialValues,
                    seed,
                    Boolean.TRUE.equals(startPaused)
            );
        });
    }

    @McpTool(
            name = "sim.stop",
            description = "不可逆地停止并销毁指定的推演会话，释放内存与类加载器租约。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true)
    )
    public McpToolEnvelope<Map<String, Object>> stopSession(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId) {

        return metrics.executeTracked("sim.stop", null, () -> {
            String sid = requireNonBlank(sessionId, "sessionId");
            enginePort.stopSession(sid);
            return Map.of("sessionId", sid, "stopped", true);
        });
    }

    @McpTool(
            name = "sim.pause",
            description = "暂停指定推演会话的时间推进（暂停期间仍可查询状态或下发干预改值）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<GromaMcpEnginePort.SessionSummary> pauseSession(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId) {

        return metrics.executeTracked("sim.pause", null, () ->
                enginePort.pauseSession(requireNonBlank(sessionId, "sessionId")));
    }

    @McpTool(
            name = "sim.resume",
            description = "恢复已暂停的推演会话继续推进仿真时间。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<GromaMcpEnginePort.SessionSummary> resumeSession(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId) {

        return metrics.executeTracked("sim.resume", null, () ->
                enginePort.resumeSession(requireNonBlank(sessionId, "sessionId")));
    }

    @McpTool(
            name = "sim.step",
            description = "将推演会话精确推进 N 步后自动停在步进末尾（误差为 0）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public McpToolEnvelope<GromaMcpEnginePort.SessionSummary> stepSession(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "推进步数，取值范围 1 ~ 10000", required = true) int steps) {

        return metrics.executeTracked("sim.step", null, () -> {
            if (steps < 1 || steps > 10_000) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "推进步数 steps 必须在 1 到 10000 之间");
            }
            return enginePort.stepSession(requireNonBlank(sessionId, "sessionId"), steps);
        });
    }

    @McpTool(
            name = "sim.set_speed",
            description = "设置推演会话的运行倍速（如 1.0、4.0；若存在运行中的孪生过程则引擎自动锁定为 1.0 倍速）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<GromaMcpEnginePort.SessionSummary> setSpeed(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "期望运行倍速，必须大于 0", required = true) double speed) {

        return metrics.executeTracked("sim.set_speed", null, () -> {
            if (speed <= 0.0D || Double.isNaN(speed)) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "倍速 speed 必须大于 0");
            }
            return enginePort.setSessionSpeed(requireNonBlank(sessionId, "sessionId"), speed);
        });
    }

    @McpTool(
            name = "sim.wait",
            description = "等待推演到达指定步进、指定过程完成、满足表达式条件或超时（任一先发生即返回，并向客户端推送进度通知）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<GromaMcpEnginePort.WaitOutcome> waitForCondition(
            McpSyncServerExchange exchange,
            @McpProgressToken String progressId,
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "可选：等待到达的目标步进索引", required = false) Long targetTick,
            @McpToolParam(description = "可选：等待结束的过程实例编号", required = false) String processId,
            @McpToolParam(description = "可选：等待满足的条件表达式", required = false) String condition,
            @McpToolParam(description = "最大等待超时毫秒数，默认 30000", required = false) Long timeoutMs) {

        return metrics.executeTracked("sim.wait", null, () -> {
            String sid = requireNonBlank(sessionId, "sessionId");
            long effectiveTimeout = (timeoutMs == null || timeoutMs <= 0L) ? 30_000L : Math.min(timeoutMs, 85_000L);
            return enginePort.waitForSessionCondition(
                    sid,
                    targetTick,
                    processId,
                    condition,
                    effectiveTimeout,
                    ratio -> emitProgressNotificationSafely(exchange, progressId, ratio, "Waiting on session " + sid)
            );
        });
    }

    @McpTool(
            name = "sim.list_sessions",
            description = "列出当前服务实例上所有活跃的推演会话摘要信息。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<List<GromaMcpEnginePort.SessionSummary>> listSessions() {
        return metrics.executeTracked("sim.list_sessions", null, enginePort::listActiveSessions);
    }

    @McpTool(
            name = "sim.describe_session",
            description = "查询单个推演会话的状态、当前步进、仿真世界时间、实际倍速、实例总数、活跃过程数及绑定的模型版本。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<GromaMcpEnginePort.SessionSummary> describeSession(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId) {

        return metrics.executeTracked("sim.describe_session", null, () ->
                enginePort.describeSession(requireNonBlank(sessionId, "sessionId")));
    }

    // =========================================================================
    // 第二组：态势观测工具（4 个，只读不可变快照 + 游标分页，NP-MCP-005）
    // =========================================================================

    @McpTool(
            name = "sim.query_instances",
            description = "分页查询会话中的对象实例列表，支持按类型、状态、路径前缀过滤，并支持用 fields 参数裁剪返回字段以节约上下文。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<PagedResult<Map<String, Object>>> queryInstances(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "可选：按对象类型过滤", required = false) String typeFilter,
            @McpToolParam(description = "可选：按当前状态标签过滤", required = false) String stateFilter,
            @McpToolParam(description = "可选：按实例名称或路径前缀过滤", required = false) String pathPrefix,
            @McpToolParam(description = "可选：仅返回指定的字段名列表", required = false) List<String> fields,
            @McpToolParam(description = "可选：上一页返回的 nextCursor 游标", required = false) String cursor,
            @McpToolParam(description = "可选：每页条数，默认 200", required = false) Integer limit) {

        return metrics.executeTracked("sim.query_instances", null, () -> {
            WorldSnapshot snap = enginePort.getImmutableSnapshot(requireNonBlank(sessionId, "sessionId"));
            Set<String> fieldSet = (fields == null || fields.isEmpty()) ? Set.of() : Set.copyOf(fields);
            List<Map<String, Object>> matched = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : snap.instances().entrySet()) {
                String pathOrKey = entry.getKey();
                Map<String, Object> attrs = entry.getValue();
                if (pathPrefix != null && !pathPrefix.isBlank() && !pathOrKey.startsWith(pathPrefix)) {
                    continue;
                }
                if (typeFilter != null && !typeFilter.isBlank()
                        && !typeFilter.equals(String.valueOf(attrs.getOrDefault("_type", "")))) {
                    continue;
                }
                if (stateFilter != null && !stateFilter.isBlank()
                        && !stateFilter.equals(String.valueOf(attrs.getOrDefault("_state", "")))) {
                    continue;
                }
                Map<String, Object> projected = new LinkedHashMap<>();
                projected.put("_instanceKey", pathOrKey);
                if (fieldSet.isEmpty()) {
                    projected.putAll(attrs);
                } else {
                    for (String f : fieldSet) {
                        if (attrs.containsKey(f)) {
                            projected.put(f, attrs.get(f));
                        }
                    }
                }
                matched.add(projected);
            }
            return PagedResult.slice(matched, cursor, limit, properties.getDefaultPageSize(), properties.getMaxPageSize());
        });
    }

    @McpTool(
            name = "sim.get_instance",
            description = "读取指定单个对象实例的完整成员属性值与状态。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<Map<String, Object>> getInstance(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "实例标识或路径名", required = true) String instanceKey) {

        return metrics.executeTracked("sim.get_instance", null, () -> {
            WorldSnapshot snap = enginePort.getImmutableSnapshot(requireNonBlank(sessionId, "sessionId"));
            String key = requireNonBlank(instanceKey, "instanceKey");
            Map<String, Object> found = snap.instances().get(key);
            if (found == null) {
                // 尝试按内部 id 字段二次检索
                for (Map<String, Object> item : snap.instances().values()) {
                    if (key.equals(String.valueOf(item.get("id")))) {
                        found = item;
                        break;
                    }
                }
            }
            if (found == null) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "会话内未找到实例: " + key);
            }
            return found;
        });
    }

    @McpTool(
            name = "sim.get_processes",
            description = "分页查询推演会话中的过程实例运行状态列表，可按状态（RUNNING / WAIT4SUCCESS / FAILED 等）过滤。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<PagedResult<WorldSnapshot.ProcessSnapItem>> getProcesses(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "可选：过程状态过滤值", required = false) String statusFilter,
            @McpToolParam(description = "可选：分页游标", required = false) String cursor,
            @McpToolParam(description = "可选：每页条数，默认 200", required = false) Integer limit) {

        return metrics.executeTracked("sim.get_processes", null, () -> {
            WorldSnapshot snap = enginePort.getImmutableSnapshot(requireNonBlank(sessionId, "sessionId"));
            List<WorldSnapshot.ProcessSnapItem> filtered = snap.activeProcesses().stream()
                    .filter(p -> statusFilter == null || statusFilter.isBlank() || statusFilter.equalsIgnoreCase(p.status()))
                    .toList();
            return PagedResult.slice(filtered, cursor, limit, properties.getDefaultPageSize(), properties.getMaxPageSize());
        });
    }

    @McpTool(
            name = "sim.get_events",
            description = "按事件序号 sinceSeq 增量拉取会话产生的事件与运行诊断问题列表（分页返回，不拉取全量历史）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<Map<String, Object>> getEvents(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "可选：仅拉取序号大于 sinceSeq 的增量事件，默认 0", required = false) Long sinceSeq,
            @McpToolParam(description = "可选：分页游标", required = false) String cursor,
            @McpToolParam(description = "可选：每页条数，默认 200", required = false) Integer limit) {

        return metrics.executeTracked("sim.get_events", null, () -> {
            WorldSnapshot snap = enginePort.getImmutableSnapshot(requireNonBlank(sessionId, "sessionId"));
            long minSeq = sinceSeq == null ? 0L : sinceSeq;
            List<WorldSnapshot.SimEventRecord> incremental = snap.recentEvents().stream()
                    .filter(ev -> ev.seq() > minSeq)
                    .toList();
            PagedResult<WorldSnapshot.SimEventRecord> page = PagedResult.slice(
                    incremental, cursor, limit, properties.getDefaultPageSize(), properties.getMaxPageSize());
            return Map.of(
                    "tickIndex", snap.tickIndex(),
                    "eventsPage", page,
                    "problems", snap.problems()
            );
        });
    }

    // =========================================================================
    // 第三组：干预工具（3 个，仅影响指定会话，下一步安全点生效，NP-MCP-006）
    // =========================================================================

    @McpTool(
            name = "sim.set_value",
            description = "在指定推演会话中修改某个实例的属性值（经单写者请求队列在下一步安全点生效）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public McpToolEnvelope<Map<String, Object>> setInstanceValue(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "目标实例标识", required = true) String instanceId,
            @McpToolParam(description = "属性英文名或中文名", required = true) String fieldName,
            @McpToolParam(description = "新的属性值", required = true) Object value) {

        return metrics.executeTracked("sim.set_value", null, () -> {
            String sid = requireNonBlank(sessionId, "sessionId");
            String iid = requireNonBlank(instanceId, "instanceId");
            String fld = requireNonBlank(fieldName, "fieldName");
            if (value == null) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "属性值 value 不能为空");
            }
            enginePort.setInstanceValue(sid, iid, fld, value);
            return Map.of("sessionId", sid, "instanceId", iid, "fieldName", fld, "queued", true);
        });
    }

    @McpTool(
            name = "sim.set_state",
            description = "在指定推演会话中切换某个实例的状态枚举值（自动按标准触发状态退出 sx 与状态进入 sa 事件）。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public McpToolEnvelope<Map<String, Object>> setInstanceState(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "目标实例标识", required = true) String instanceId,
            @McpToolParam(description = "状态属性名", required = true) String stateFieldName,
            @McpToolParam(description = "目标状态值", required = true) String targetState) {

        return metrics.executeTracked("sim.set_state", null, () -> {
            String sid = requireNonBlank(sessionId, "sessionId");
            String iid = requireNonBlank(instanceId, "instanceId");
            String sf = requireNonBlank(stateFieldName, "stateFieldName");
            String ts = requireNonBlank(targetState, "targetState");
            enginePort.setInstanceState(sid, iid, sf, ts);
            return Map.of("sessionId", sid, "instanceId", iid, "targetState", ts, "queued", true);
        });
    }

    @McpTool(
            name = "sim.run_process",
            description = "在已运行的推演会话指定实例上手动触发一个过程。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public McpToolEnvelope<Map<String, Object>> triggerProcessInSession(
            @McpToolParam(description = "推演会话编号", required = true) String sessionId,
            @McpToolParam(description = "目标实例标识", required = true) String instanceId,
            @McpToolParam(description = "过程引用名 ref", required = true) String processRef,
            @McpToolParam(description = "可选的过程入参对象", required = false) Map<String, Object> arguments) {

        return metrics.executeTracked("sim.run_process", null, () -> {
            String sid = requireNonBlank(sessionId, "sessionId");
            String iid = requireNonBlank(instanceId, "instanceId");
            String ref = requireNonBlank(processRef, "processRef");
            String procInstId = enginePort.triggerProcessOnInstance(
                    sid, iid, ref, arguments == null ? Map.of() : arguments);
            return Map.of("sessionId", sid, "processInstanceId", procInstId, "started", true);
        });
    }

    // =========================================================================
    // 第四组：模型过程描述与通用过程执行工具（2 个，NP-MCP-007）
    // =========================================================================

    @McpTool(
            name = "model.describe_process",
            description = "查询指定模型中某个过程的详细入参 Schema、出参 Schema、类别（算子/耗时过程）与前置启动条件。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public McpToolEnvelope<GromaToolManifest.ProcessToolDef> describeProcess(
            @McpToolParam(description = "模型编码", required = true) String modelCode,
            @McpToolParam(description = "过程引用名 ref", required = true) String processRef,
            @McpToolParam(description = "可选：期望核对的模型版本号", required = false) String modelVersion) {

        return metrics.executeTracked("model.describe_process", modelCode, () -> {
            GromaMcpEnginePort.MountedModelMetadata meta = resolveModelOrThrow(modelCode, modelVersion);
            return findProcessDefOrThrow(meta.toolManifest(), processRef);
        });
    }

    @McpTool(
            name = "run.process",
            description = "按模型工具清单调用一个过程：不传 sessionId 时执行无状态计算并自动清理临时上下文；传 sessionId 时在该会话内执行。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false)
    )
    public McpToolEnvelope<GromaMcpEnginePort.ProcessExecutionOutcome> runProcess(
            @McpToolParam(description = "模型编码", required = true) String modelCode,
            @McpToolParam(description = "过程引用名 ref", required = true) String processRef,
            @McpToolParam(description = "符合该过程 inputSchema 的入参对象", required = false) Map<String, Object> inputs,
            @McpToolParam(description = "可选：绑定已有推演会话编号（不传则执行无状态调用）", required = false) String sessionId,
            @McpToolParam(description = "可选：绑定会话时的宿主实例编号", required = false) String instanceId,
            @McpToolParam(description = "可选：无状态耗时过程的墙钟超时毫秒数，默认 60000", required = false) Long timeoutMs) {

        return metrics.executeTracked("run.process", modelCode, () -> {
            String mc = requireNonBlank(modelCode, "modelCode");
            String ref = requireNonBlank(processRef, "processRef");

            // 若传入了会话编号，按会话实际锁定的模型版本查找清单做入参校验（03-mcp-analysis.md 4.8）
            String pinnedVersion = null;
            if (sessionId != null && !sessionId.isBlank()) {
                pinnedVersion = enginePort.describeSession(sessionId.trim()).modelVersion();
            }
            GromaMcpEnginePort.MountedModelMetadata meta = resolveModelOrThrow(mc, pinnedVersion);
            GromaToolManifest.ProcessToolDef procDef = findProcessDefOrThrow(meta.toolManifest(), ref);

            Map<String, Object> safeInputs = inputs == null ? Map.of() : inputs;
            ManifestSchemaValidator.validateArgumentsAgainstSchema(procDef.inputSchema(), safeInputs);

            if (sessionId == null || sessionId.isBlank()) {
                long wallTimeout = (timeoutMs == null || timeoutMs <= 0L)
                        ? properties.getStatelessProcessTimeoutMs()
                        : Math.min(timeoutMs, 85_000L);
                return enginePort.executeProcessStateless(mc, ref, safeInputs, wallTimeout);
            } else {
                return enginePort.executeProcessInSession(sessionId.trim(), instanceId, ref, safeInputs);
            }
        });
    }

    private GromaMcpEnginePort.MountedModelMetadata resolveModelOrThrow(String modelCode, String expectedVersion) {
        String mc = requireNonBlank(modelCode, "modelCode");
        return enginePort.findMountedModel(mc, expectedVersion)
                .orElseThrow(() -> {
                    if (expectedVersion != null && !expectedVersion.isBlank()
                            && enginePort.findMountedModel(mc, null).isPresent()) {
                        return new GromaMcpException(McpErrorCode.MODEL_VERSION_MISMATCH,
                                "模型 [" + mc + "] 当前挂载版本与请求的预期版本 [" + expectedVersion + "] 不一致");
                    }
                    return new GromaMcpException(McpErrorCode.MODEL_NOT_FOUND, "模型未挂载: " + mc);
                });
    }

    private static GromaToolManifest.ProcessToolDef findProcessDefOrThrow(GromaToolManifest manifest, String ref) {
        String target = requireNonBlank(ref, "processRef");
        if (manifest != null && manifest.processes() != null) {
            for (GromaToolManifest.ProcessToolDef p : manifest.processes()) {
                if (target.equals(p.ref())) {
                    return p;
                }
            }
        }
        throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "模型清单内不存在已发布过程: " + target);
    }

    private static void emitProgressNotificationSafely(
            McpSyncServerExchange exchange,
            String progressId,
            double progressRatio,
            String message) {
        if (exchange == null || progressId == null || progressId.isBlank()) {
            return;
        }
        try {
            exchange.progressNotification(new McpSchema.ProgressNotification(
                    progressId, progressRatio, 1.0D, message));
        } catch (Exception ignored) {
            // 客户端不支持进度通知时静默忽略，不影响等待结果
        }
    }

    private static String requireNonBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "必填参数不能为空: " + paramName);
        }
        return value.trim();
    }
}

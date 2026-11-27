package com.groma.base.mcp.observability;

import com.groma.base.mcp.context.GatewayIdentityContext;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.mcp.protocol.GromaMcpException;
import com.groma.base.mcp.protocol.McpErrorCode;
import com.groma.base.mcp.protocol.McpToolEnvelope;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * MCP 工具调用统一指标记录与异常边界转换器（落实 NP-MCP-009）
 */
public final class GromaMcpMetricsRecorder {

    private static final Logger log = LoggerFactory.getLogger(GromaMcpMetricsRecorder.class);

    private final MeterRegistry meterRegistry;

    public GromaMcpMetricsRecorder(MeterRegistry meterRegistry, GromaMcpEnginePort enginePort) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("groma.mcp.sessions.active", enginePort, GromaMcpEnginePort::activeSessionCount)
                .description("当前运行宿主内活跃的推演会话总数")
                .register(meterRegistry);
    }

    /**
     * 包装任意 MCP 工具逻辑：自动生成/继承 traceId、记录耗时与成功/失败计数器、并将异常转换为 isError=true 结构化信封
     */
    public <T> McpToolEnvelope<T> executeTracked(String toolName, String modelTag, Supplier<T> action) {
        String safeModel = (modelTag == null || modelTag.isBlank()) ? "_host_" : modelTag;
        String traceId = GatewayIdentityContext.currentTraceId();
        long startNs = System.nanoTime();

        Counter.builder("groma.mcp.tool.calls")
                .tag("tool", toolName)
                .tag("model", safeModel)
                .register(meterRegistry)
                .increment();

        try {
            T payload = action.get();
            recordLatency(toolName, safeModel, "SUCCESS", System.nanoTime() - startNs);
            return McpToolEnvelope.ok(traceId, payload);
        } catch (GromaMcpException bizEx) {
            recordFailure(toolName, safeModel, bizEx.getErrorCode().name(), System.nanoTime() - startNs);
            log.warn("[MCP][trace={}] 工具 {} 业务拒绝 [{}]: {}",
                    traceId, toolName, bizEx.getErrorCode(), truncateLog(bizEx.getMessage()));
            return McpToolEnvelope.error(traceId, bizEx.getErrorCode(), bizEx.getMessage());
        } catch (Exception unexpected) {
            recordFailure(toolName, safeModel, McpErrorCode.PROCESS_FAILED.name(), System.nanoTime() - startNs);
            log.error("[MCP][trace={}] 工具 {} 未预期异常: {}",
                    traceId, toolName, truncateLog(unexpected.getMessage()), unexpected);
            return McpToolEnvelope.error(traceId, McpErrorCode.PROCESS_FAILED,
                    "执行失败: " + truncateLog(unexpected.getMessage()));
        }
    }

    private void recordFailure(String toolName, String modelTag, String errorCode, long durationNs) {
        Counter.builder("groma.mcp.tool.failures")
                .tag("tool", toolName)
                .tag("model", modelTag)
                .tag("error_code", errorCode)
                .register(meterRegistry)
                .increment();
        recordLatency(toolName, modelTag, "ERROR", durationNs);
    }

    private void recordLatency(String toolName, String modelTag, String outcome, long durationNs) {
        Timer.builder("groma.mcp.tool.latency")
                .tag("tool", toolName)
                .tag("model", modelTag)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(durationNs, TimeUnit.NANOSECONDS);
    }

    private static String truncateLog(String msg) {
        if (msg == null) {
            return "";
        }
        // 落实 BE-SEC-004：日志内容限长在 2,000 字符以内
        return msg.length() > 1800 ? msg.substring(0, 1800) + "...(truncated)" : msg;
    }
}

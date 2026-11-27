package com.groma.base.mcp.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 网关透传请求头上下文（当前仅提取存档并传递追踪号，不做任何鉴权阻断）
 */
public final class GatewayIdentityContext {

    public record GatewayHeaders(
            String traceId,
            String subjectId,
            String tenantId,
            String agentId,
            Map<String, String> rawForwardedHeaders
    ) {
        public static GatewayHeaders anonymousWithNewTrace() {
            return new GatewayHeaders(newTraceId(), "", "", "", Map.of());
        }
    }

    private static final ThreadLocal<GatewayHeaders> HOLDER = new ThreadLocal<>();

    private GatewayIdentityContext() {
    }

    public static void bind(GatewayHeaders headers) {
        HOLDER.set(headers);
    }

    public static GatewayHeaders current() {
        GatewayHeaders h = HOLDER.get();
        return h != null ? h : GatewayHeaders.anonymousWithNewTrace();
    }

    public static String currentTraceId() {
        GatewayHeaders h = HOLDER.get();
        return (h != null && h.traceId() != null && !h.traceId().isBlank()) ? h.traceId() : newTraceId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static GatewayHeaders extractFromMap(Map<String, String> incomingHeaders) {
        if (incomingHeaders == null || incomingHeaders.isEmpty()) {
            return GatewayHeaders.anonymousWithNewTrace();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        incomingHeaders.forEach((k, v) -> {
            if (k != null && v != null) {
                normalized.put(k.trim().toLowerCase(), sanitizeHeaderValue(v));
            }
        });

        String trace = firstNonBlank(normalized.get("x-request-id"), normalized.get("x-trace-id"), newTraceId());
        String subject = firstNonBlank(normalized.get("x-gateway-subject"), normalized.get("x-forwarded-user"), "");
        String tenant = firstNonBlank(normalized.get("x-gateway-tenant"), normalized.get("x-tenant-id"), "");
        String agent = firstNonBlank(normalized.get("x-agent-id"), "");

        Map<String, String> kept = new LinkedHashMap<>();
        normalized.forEach((k, v) -> {
            if (k.startsWith("x-gateway-") || k.startsWith("x-forwarded-") || "x-agent-id".equals(k) || "x-tenant-id".equals(k)) {
                kept.put(k, v);
            }
        });
        return new GatewayHeaders(trace, subject, tenant, agent, Collections.unmodifiableMap(kept));
    }

    private static String sanitizeHeaderValue(String val) {
        String clean = val.replace('\r', ' ').replace('\n', ' ').trim();
        return clean.length() > 256 ? clean.substring(0, 256) : clean;
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return "";
    }

    private static String newTraceId() {
        return "tr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

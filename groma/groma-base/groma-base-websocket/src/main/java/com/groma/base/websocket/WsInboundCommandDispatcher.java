package com.groma.base.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 浏览器上行前缀指令解析器（最长前缀匹配 + 畸形消息防护 + 递归深度熔断，落实 BE-PSH-003）
 */
public final class WsInboundCommandDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WsInboundCommandDispatcher.class);
    public static final int MAX_QUERY_DEPTH = 16;
    private static final int MAX_INBOUND_CHARS = 524_288;

    public interface SessionControlGateway {
        void pause(String sessionId);
        void resume(String sessionId);
        void stop(String sessionId);
        void step(String sessionId, int steps);
        void setSpeed(String sessionId, double factor);
        void heartbeat(String sessionId);
        String querySubtreeJson(String sessionId, String rootId, int maxDepth);
    }

    private record RouteRule(String prefix, boolean writeOperation, BiFunction<String, String, String> handler) {}

    private final List<RouteRule> sortedRoutes;

    public WsInboundCommandDispatcher(SessionControlGateway gateway) {
        List<RouteRule> rules = List.of(
                new RouteRule("HRT:", false, (sid, body) -> {
                    gateway.heartbeat(sid);
                    return "ACK:HRT";
                }),
                new RouteRule("CTL:P", true, (sid, body) -> {
                    gateway.pause(sid);
                    return "ACK:CTL:P";
                }),
                new RouteRule("CTL:R", true, (sid, body) -> {
                    gateway.resume(sid);
                    return "ACK:CTL:R";
                }),
                new RouteRule("CTL:S", true, (sid, body) -> {
                    gateway.stop(sid);
                    return "ACK:CTL:S";
                }),
                new RouteRule("CTL:G:", true, (sid, body) -> {
                    int steps = parsePositiveInt(body, "CTL:G");
                    gateway.step(sid, steps);
                    return "ACK:CTL:G:" + steps;
                }),
                new RouteRule("SP:D", true, (sid, body) -> {
                    gateway.setSpeed(sid, 2.0D);
                    return "ACK:SP:D";
                }),
                new RouteRule("SP:N", true, (sid, body) -> {
                    gateway.setSpeed(sid, 1.0D);
                    return "ACK:SP:N";
                }),
                new RouteRule("SP:H", true, (sid, body) -> {
                    gateway.setSpeed(sid, 0.5D);
                    return "ACK:SP:H";
                }),
                new RouteRule("T/I/TREE:", false, (sid, body) -> {
                    if (body.isBlank()) {
                        return "ERR:T/I/TREE:MISSING_ROOT_ID";
                    }
                    String[] parts = body.split(":", 2);
                    String rootId = parts[0].trim();
                    int reqDepth = parts.length > 1 ? parsePositiveInt(parts[1], "T/I/TREE") : 4;
                    if (reqDepth > MAX_QUERY_DEPTH) {
                        return "ERR:T/I/TREE:DEPTH_EXCEEDED_MAX_" + MAX_QUERY_DEPTH;
                    }
                    return "DATA:TREE:" + gateway.querySubtreeJson(sid, rootId, reqDepth);
                })
        );
        this.sortedRoutes = rules.stream()
                .sorted(Comparator.comparingInt((RouteRule r) -> r.prefix().length()).reversed())
                .toList();
    }

    public String dispatch(String sessionId, String rawMessage) {
        if (sessionId == null || sessionId.isBlank()) {
            return "ERR:GLOBAL:INVALID_SESSION";
        }
        if (rawMessage == null || rawMessage.isBlank()) {
            return "ERR:EMPTY:EMPTY_COMMAND";
        }
        if (rawMessage.length() > MAX_INBOUND_CHARS) {
            return "ERR:OVERSIZE:EXCEEDS_512KB";
        }
        String trimmed = rawMessage.trim();
        for (RouteRule route : sortedRoutes) {
            if (trimmed.startsWith(route.prefix())) {
                String payload = trimmed.substring(route.prefix().length()).trim();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("[WS][sid={}] 指令前缀={}", sessionId, route.prefix());
                    }
                    return route.handler().apply(sessionId, payload);
                } catch (IllegalArgumentException badArg) {
                    return "ERR:" + route.prefix() + ":" + badArg.getMessage();
                } catch (Exception ex) {
                    log.warn("[WS][sid={}] 处理指令 {} 失败: {}", sessionId, route.prefix(), ex.getMessage());
                    return "ERR:" + route.prefix() + ":INTERNAL_REJECTED";
                }
            }
        }
        return "ERR:UNKNOWN_CMD:" + trimmed.substring(0, Math.min(16, trimmed.length()));
    }

    public Map<String, String> exportGatewayWriteClassification() {
        return Map.of(
                "HRT:", "READ_ONLY",
                "T/I/TREE:", "READ_ONLY",
                "CTL:P", "WRITE",
                "CTL:R", "WRITE",
                "CTL:S", "WRITE",
                "CTL:G:", "WRITE",
                "SP:D", "WRITE",
                "SP:N", "WRITE",
                "SP:H", "WRITE"
        );
    }

    private static int parsePositiveInt(String text, String cmd) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("MISSING_INT_FOR_" + cmd);
        }
        try {
            int val = Integer.parseInt(text.trim());
            if (val <= 0 || val > 10_000) {
                throw new IllegalArgumentException("OUT_OF_RANGE_FOR_" + cmd);
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("TYPE_MISMATCH_FOR_" + cmd);
        }
    }
}

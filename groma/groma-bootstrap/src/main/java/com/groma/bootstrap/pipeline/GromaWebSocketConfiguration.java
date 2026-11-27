package com.groma.bootstrap.pipeline;

import com.groma.base.engine.WorldSnapshot;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.websocket.WsInboundCommandDispatcher;
import com.groma.base.websocket.WsRoomSubscriptionManager;
import com.groma.studio.debug.DapWebSocketRouter;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * WebSocket 装配（阶段 2-E 第 4 项，协议以 groma/docs/ws-protocol.md 为准；base-websocket 为 JSR-356 组件）：
 * 态势与控制信令共用 /ws/groma/{modelCode}/{sceneCode}/{sessionId}（单会话默认最多 50 订阅端），
 * DAP 调试通道 /ws/debug/{sessionId}。帧协议 TP:v1 由 WsRoomSubscriptionManager 合流广播。
 * <p>
 * 依赖注入方式（修复原 static volatile 桥接在多 Spring Context 并行时的覆盖缺陷）：
 * JSR-356 容器不归 Spring 实例化，端点依赖在注册期写入 {@link ServerEndpointConfig#getUserProperties()}，
 * 端点实例经 {@link EndpointConfig#getUserProperties()} 读取——彻底移除静态全局变量。
 * </p>
 */
@Configuration
public class GromaWebSocketConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GromaWebSocketConfiguration.class);

    /** userProperties 中各依赖的键名 */
    private static final String KEY_ROOMS = "groma.ws.rooms";
    private static final String KEY_DISPATCHER = "groma.ws.dispatcher";
    private static final String KEY_PORT = "groma.ws.port";
    private static final String KEY_DEBUG_BRIDGE = "groma.ws.debugBridge";

    @Bean
    public WsRoomSubscriptionManager wsRoomSubscriptionManager(
            @Value("${groma.ws.max-subscribers-per-session:50}") int maxSubscribers) {
        return new WsRoomSubscriptionManager(maxSubscribers);
    }

    @Bean
    public WsInboundCommandDispatcher wsInboundCommandDispatcher(GromaMcpEnginePort port) {
        return new WsInboundCommandDispatcher(new PortControlGateway(port));
    }

    @Bean
    public DapWebSocketRouter dapWebSocketRouter() {
        return new DapWebSocketRouter();
    }

    /**
     * 把程序化端点注册进嵌入式 Tomcat 的 WS 容器（依赖随端点配置下发，不经静态变量）。
     * ServerContainer 属性由容器 SCI 在上下文启动末段设置，因此挂到 WebServerInitializedEvent 后置注册。
     */
    @Bean
    public org.springframework.context.ApplicationListener<org.springframework.boot.web.server.context.WebServerInitializedEvent>
            gromaWsEndpointRegistrar(WsRoomSubscriptionManager rooms,
                                     WsInboundCommandDispatcher dispatcher,
                                     GromaMcpEnginePort port,
                                     SimDebugCoordinator debugBridge) {
        return event -> {
            jakarta.servlet.ServletContext servletContext =
                    ((org.springframework.web.context.WebApplicationContext) event.getApplicationContext())
                            .getServletContext();
            ServerContainer container = (ServerContainer) servletContext
                    .getAttribute("jakarta.websocket.server.ServerContainer");
            if (container == null) {
                LOG.warn("[WS] 未找到 jakarta ServerContainer，WebSocket 端点未注册");
                return;
            }
            try {
                container.addEndpoint(endpoint(SituationEndpoint.class, "/ws/groma/{modelCode}/{sceneCode}/{sessionId}",
                        Map.of(KEY_ROOMS, rooms, KEY_DISPATCHER, dispatcher, KEY_PORT, port)));
                container.addEndpoint(endpoint(DebugEndpoint.class, "/ws/debug/{sessionId}",
                        Map.of(KEY_DEBUG_BRIDGE, debugBridge)));
                LOG.info("[WS] 端点已注册：/ws/groma/{{modelCode}}/{{sceneCode}}/{{sessionId}} 与 /ws/debug/{{sessionId}}");
            } catch (jakarta.websocket.DeploymentException ex) {
                throw new IllegalStateException("WS 端点注册失败", ex);
            }
        };
    }

    private static ServerEndpointConfig endpoint(Class<? extends Endpoint> type, String path,
                                                 Map<String, Object> dependencies) {
        ServerEndpointConfig config = ServerEndpointConfig.Builder.create(type, path).build();
        config.getUserProperties().putAll(dependencies);
        return config;
    }

    /** 上行控制指令 → 引擎端口（与 REST/MCP 三通道同一语义） */
    static final class PortControlGateway implements WsInboundCommandDispatcher.SessionControlGateway {
        private static final ObjectMapper JSON = new ObjectMapper();
        private final GromaMcpEnginePort port;

        PortControlGateway(GromaMcpEnginePort port) {
            this.port = port;
        }

        @Override public void pause(String sessionId) {
            port.pauseSession(sessionId);
        }

        @Override public void resume(String sessionId) {
            port.resumeSession(sessionId);
        }

        @Override public void stop(String sessionId) {
            port.stopSession(sessionId);
        }

        @Override public void step(String sessionId, int steps) {
            port.stepSession(sessionId, steps);
        }

        @Override public void setSpeed(String sessionId, double factor) {
            port.setSessionSpeed(sessionId, factor);
        }

        @Override public void heartbeat(String sessionId) {
            port.describeSession(sessionId);
        }

        /**
         * 子树查询：以 Jackson 序列化实例状态（修复原 Map.toString() 输出 {a=1, b=2} 非法 JSON 导致前端
         * JSON.parse 必然失败的缺陷）。maxDepth 作为合法性与后续层级展开的上界校验保留
         * （WorldSnapshot 为扁平实例表，无父子边，当前返回根实例自身）。
         */
        @Override public String querySubtreeJson(String sessionId, String rootId, int maxDepth) {
            WorldSnapshot snapshot = port.getImmutableSnapshot(sessionId);
            Map<String, Object> state = snapshot.instances().get(rootId);
            if (state == null) {
                return "{}";
            }
            try {
                return JSON.writeValueAsString(state);
            } catch (RuntimeException ex) {
                LOG.warn("[WS] 子树状态序列化失败 sid={} root={}: {}", sessionId, rootId, ex.getMessage());
                return "{}";
            }
        }
    }

    /** 态势订阅 + 控制信令端点（URI 模板末段为推演会话 id） */
    public static class SituationEndpoint extends Endpoint {

        private WsRoomSubscriptionManager rooms;
        private WsInboundCommandDispatcher dispatcher;
        private GromaMcpEnginePort port;

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            rooms = (WsRoomSubscriptionManager) config.getUserProperties().get(KEY_ROOMS);
            dispatcher = (WsInboundCommandDispatcher) config.getUserProperties().get(KEY_DISPATCHER);
            port = (GromaMcpEnginePort) config.getUserProperties().get(KEY_PORT);
            String simSessionId = session.getPathParameters().get("sessionId");
            if (rooms == null || dispatcher == null || port == null
                    || simSessionId == null || simSessionId.isBlank()
                    || !rooms.registerSubscriber(simSessionId, session)) {
                closeQuietly(session, "ERR:LIMIT_EXCEEDED: 会话不存在或订阅端已满");
                return;
            }
            session.addMessageHandler((MessageHandler.Whole<String>) raw -> {
                String reply = dispatcher.dispatch(simSessionId, raw);
                try {
                    session.getBasicRemote().sendText(reply);
                } catch (IOException ex) {
                    LOG.warn("[WS] 回执发送失败: {}", ex.getMessage());
                }
                try {
                    rooms.broadcastSnapshotIfObserved(port.getImmutableSnapshot(simSessionId));
                } catch (RuntimeException gone) {
                    LOG.debug("[WS] 会话 {} 广播跳过: {}", simSessionId, gone.getMessage());
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason reason) {
            String simSessionId = session.getPathParameters().get("sessionId");
            if (rooms != null && simSessionId != null) {
                rooms.unregisterSubscriber(simSessionId, session.getId());
            }
        }
    }

    /** DAP 调试通道端点：标准 JSON/DAP 报文 → SimDebugCoordinator / DapWebSocketRouter 真实路由 */
    public static class DebugEndpoint extends Endpoint {

        private SimDebugCoordinator debugBridge;

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            debugBridge = (SimDebugCoordinator) config.getUserProperties().get(KEY_DEBUG_BRIDGE);
            String simSessionId = session.getPathParameters().get("sessionId");
            if (debugBridge == null || simSessionId == null || simSessionId.isBlank()) {
                closeQuietly(session, "ERR:DEBUG:NOT_REGISTERED");
                return;
            }
            Consumer<String> dapRoute;
            try {
                dapRoute = debugBridge.attach(simSessionId, session);
            } catch (IllegalArgumentException notFound) {
                closeQuietly(session, "ERR:DEBUG:" + notFound.getMessage());
                return;
            }
            session.addMessageHandler((MessageHandler.Whole<String>) dapRoute::accept);
        }

        @Override
        public void onClose(Session session, CloseReason reason) {
            String simSessionId = session.getPathParameters().get("sessionId");
            if (debugBridge != null && simSessionId != null) {
                debugBridge.detach(simSessionId, session);
            }
        }
    }

    private static void closeQuietly(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException ex) {
            LOG.warn("[WS] 拒绝连接时关闭失败: {}", ex.getMessage());
        }
    }
}

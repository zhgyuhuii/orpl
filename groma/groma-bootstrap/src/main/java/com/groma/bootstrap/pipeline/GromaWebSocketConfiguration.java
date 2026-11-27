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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 装配（阶段 2-E 第 4 项，协议以 groma/docs/ws-protocol.md 为准；base-websocket 为 JSR-356 组件）：
 * 态势与控制信令共用 /ws/groma/{modelCode}/{sceneCode}/{sessionId}（单会话默认最多 50 订阅端），
 * DAP 调试通道 /ws/debug/{sessionId}。帧协议 TP:v1 由 WsRoomSubscriptionManager 合流广播。
 */
@Configuration
public class GromaWebSocketConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GromaWebSocketConfiguration.class);

    /** JSR-356 容器不归 Spring 实例化，端点依赖经静态桥接（宿主单例，@Bean 装配时赋值） */
    static volatile WsRoomSubscriptionManager ROOMS;
    static volatile WsInboundCommandDispatcher DISPATCHER;
    static volatile GromaMcpEnginePort PORT;
    static volatile DapWebSocketRouter DAP_ROUTER;

    @Bean
    public WsRoomSubscriptionManager wsRoomSubscriptionManager(
            @Value("${groma.ws.max-subscribers-per-session:50}") int maxSubscribers) {
        WsRoomSubscriptionManager manager = new WsRoomSubscriptionManager(maxSubscribers);
        ROOMS = manager;
        return manager;
    }

    @Bean
    public WsInboundCommandDispatcher wsInboundCommandDispatcher(GromaMcpEnginePort port) {
        WsInboundCommandDispatcher dispatcher = new WsInboundCommandDispatcher(new PortControlGateway(port));
        DISPATCHER = dispatcher;
        PORT = port;
        return dispatcher;
    }

    @Bean
    public DapWebSocketRouter dapWebSocketRouter() {
        DapWebSocketRouter router = new DapWebSocketRouter();
        DAP_ROUTER = router;
        return router;
    }

    /**
     * 把程序化端点注册进嵌入式 Tomcat 的 WS 容器。
     * ServerContainer 属性由容器 SCI 在上下文启动末段设置，因此挂到 WebServerInitializedEvent 后置注册。
     */
    @Bean
    public org.springframework.context.ApplicationListener<org.springframework.boot.web.server.context.WebServerInitializedEvent>
            gromaWsEndpointRegistrar() {
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
                container.addEndpoint(ServerEndpointConfig.Builder
                        .create(SituationEndpoint.class, "/ws/groma/{modelCode}/{sceneCode}/{sessionId}")
                        .build());
                container.addEndpoint(ServerEndpointConfig.Builder
                        .create(DebugEndpoint.class, "/ws/debug/{sessionId}")
                        .build());
                LOG.info("[WS] 端点已注册：/ws/groma/{{modelCode}}/{{sceneCode}}/{{sessionId}} 与 /ws/debug/{{sessionId}}");
            } catch (jakarta.websocket.DeploymentException ex) {
                throw new IllegalStateException("WS 端点注册失败", ex);
            }
        };
    }

    /** 上行控制指令 → 引擎端口（与 REST/MCP 三通道同一语义） */
    static final class PortControlGateway implements WsInboundCommandDispatcher.SessionControlGateway {
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

        @Override public String querySubtreeJson(String sessionId, String rootId, int maxDepth) {
            WorldSnapshot snapshot = port.getImmutableSnapshot(sessionId);
            Map<String, Object> state = snapshot.instances().get(rootId);
            return state == null ? "{}" : state.toString();
        }
    }

    /** 态势订阅 + 控制信令端点（URI 模板末段为推演会话 id） */
    public static class SituationEndpoint extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            String simSessionId = session.getPathParameters().get("sessionId");
            if (simSessionId == null || simSessionId.isBlank()
                    || !ROOMS.registerSubscriber(simSessionId, session)) {
                closeQuietly(session, "ERR:LIMIT_EXCEEDED: 会话不存在或订阅端已满");
                return;
            }
            session.addMessageHandler((MessageHandler.Whole<String>) raw -> {
                String reply = DISPATCHER.dispatch(simSessionId, raw);
                try {
                    session.getBasicRemote().sendText(reply);
                } catch (IOException ex) {
                    LOG.warn("[WS] 回执发送失败: {}", ex.getMessage());
                }
                try {
                    ROOMS.broadcastSnapshotIfObserved(PORT.getImmutableSnapshot(simSessionId));
                } catch (RuntimeException gone) {
                    LOG.debug("[WS] 会话 {} 广播跳过: {}", simSessionId, gone.getMessage());
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason reason) {
            String simSessionId = lastSegmentFromRequest(session);
            if (simSessionId != null) {
                ROOMS.unregisterSubscriber(simSessionId, session.getId());
            }
        }

        private static String lastSegmentFromRequest(Session session) {
            Map<String, java.util.List<String>> path = session.getRequestParameterMap();
            // 端点路径模板的参数在容器侧通过 request URI 提取；直接从 request URI 取末段
            Object requestUri = session.getUserProperties().get("jakarta.websocket.endpoint.remoteAddress");
            String uri = requestUri != null ? requestUri.toString() : null;
            if (uri == null && session.getRequestURI() != null) {
                uri = session.getRequestURI().getPath();
            }
            if (uri == null || uri.isBlank()) {
                return path.isEmpty() ? null : null;
            }
            int slash = uri.lastIndexOf('/');
            return slash >= 0 && slash < uri.length() - 1 ? uri.substring(slash + 1) : null;
        }
    }

    /** DAP 调试通道端点：登记状态回执（完整 DAP 协议栈属调试服务器模块验收项） */
    public static class DebugEndpoint extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            String debugSessionId = session.getPathParameters().get("sessionId");
            session.addMessageHandler((MessageHandler.Whole<String>) raw -> {
                boolean registered = debugSessionId != null && DAP_ROUTER != null;
                try {
                    session.getBasicRemote().sendText(registered
                            ? "ACK:DEBUG:ATTACHED:" + debugSessionId
                            : "ERR:DEBUG:NOT_REGISTERED");
                } catch (IOException ex) {
                    LOG.warn("[WS][DAP] 回执发送失败: {}", ex.getMessage());
                }
            });
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

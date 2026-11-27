package com.groma.base.websocket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 对应 CK-BE-PSH-003-1 & 2：畸形消息永不抛未捕获异常，递归查询受深度上限保护
 */
class WsCommandDispatchTest {

    private final WsInboundCommandDispatcher dispatcher = new WsInboundCommandDispatcher(
            new WsInboundCommandDispatcher.SessionControlGateway() {
                @Override public void pause(String sessionId) {}
                @Override public void resume(String sessionId) {}
                @Override public void stop(String sessionId) {}
                @Override public void step(String sessionId, int steps) {}
                @Override public void setSpeed(String sessionId, double factor) {}
                @Override public void heartbeat(String sessionId) {}
                @Override public String querySubtreeJson(String sessionId, String rootId, int maxDepth) {
                    return "{\"root\":\"" + rootId + "\"}";
                }
            }
    );

    @Test
    void malformedBodyNeverThrows() {
        Assertions.assertTrue(dispatcher.dispatch("s1", "").startsWith("ERR:"));
        Assertions.assertTrue(dispatcher.dispatch("s1", "CTL:G:").startsWith("ERR:"));
        Assertions.assertTrue(dispatcher.dispatch("s1", "CTL:G:not_a_number").startsWith("ERR:"));
        Assertions.assertTrue(dispatcher.dispatch("s1", "UNKNOWN_PREFIX:123").startsWith("ERR:"));
    }

    @Test
    void recursiveQueryDepthLimited() {
        String overDepth = dispatcher.dispatch("s1", "T/I/TREE:node_1:999");
        Assertions.assertTrue(overDepth.startsWith("ERR:T/I/TREE:DEPTH_EXCEEDED"));

        String okResp = dispatcher.dispatch("s1", "T/I/TREE:node_1:4");
        Assertions.assertTrue(okResp.startsWith("DATA:TREE:"));
    }
}

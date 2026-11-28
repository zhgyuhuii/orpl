package com.groma.models.emergencydeduction844676b1.ws;

import java.lang.String;

/**
 * 场景1 场景 WebSocket 频道路由契约（设计说明书 1.x：groma-base-websocket 统一路由
 * /ws/groma/{modelCode}/{sceneCode}/{sessionId}，本类为该场景的静态路由元数据）。
 */
public final class Scene1SceneWsChannel {
    public static final String MODEL_CODE = "emergency-deduction-844676b1";

    public static final String SCENE_CODE = "scene1";

    public static final String WS_PATH_TEMPLATE = "/ws/groma/emergency-deduction-844676b1/scene1/{sessionId}";

    private Scene1SceneWsChannel() {
    }

    /**
     * 该会话的实时态势频道路由（前端 Three.js/PixiJS 视口直连地址）
     */
    public static String channelPath(String sessionId) {
        return "/ws/groma/emergency-deduction-844676b1/scene1/" + sessionId;
    }
}

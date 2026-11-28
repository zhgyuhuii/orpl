package com.groma.models.emergencydeduction844676b1.repository;

import com.groma.base.engine.WorldSnapshot;
import java.lang.String;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景1 场景会话快照内存仓储（设计说明书标准五层 repository 落点）：
 * 步进边界快照的进程内存取；数据库五层逆向生成（说明书 M5~M6）时以同签名 JdbcClient
 * 实现替换本内存版，控制器与业务代码零改动。
 */
public final class Scene1SceneSnapshotRepository {
    private final Map<String, WorldSnapshot> snapshots = new ConcurrentHashMap<>();

    public void save(String sessionId, WorldSnapshot snapshot) {
        snapshots.put(sessionId, snapshot);
    }

    public WorldSnapshot find(String sessionId) {
        return snapshots.get(sessionId);
    }

    public void remove(String sessionId) {
        snapshots.remove(sessionId);
    }
}

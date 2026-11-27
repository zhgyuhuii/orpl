package com.groma.base.websocket;

import com.groma.base.engine.WorldSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * 对应 CK-BE-PSH-002-3：无订阅者时不执行快照帧序列化
 */
class PushConflationTest {

    @Test
    void noSerializationWithoutSubscribers() {
        WsRoomSubscriptionManager mgr = new WsRoomSubscriptionManager(10);
        WorldSnapshot snap = new WorldSnapshot(
                "sid_silent", "uav", "1", "island", 1L, 5L, 5000L, 1.0D,
                "RUNNING", Map.of(), List.of(), List.of(), List.of());

        mgr.broadcastSnapshotIfObserved(snap);
        Assertions.assertEquals(0L, mgr.getSerializationCount(), "零观察者时序列化计数必须为 0");
    }
}

package com.groma.base.websocket;

import com.groma.base.engine.WorldSnapshot;
import jakarta.websocket.Session;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 推演会话级 WebSocket 订阅房间管理器（含零观察者静默开关与单槽背压广播）
 */
public final class WsRoomSubscriptionManager {

    public static final String FRAME_PROTOCOL_VERSION = "v1";

    private final int maxSubscribersPerSession;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Function<WorldSnapshot, String> frameSerializer;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConflationWsSender>> rooms = new ConcurrentHashMap<>();
    private final AtomicLong serializationCounter = new AtomicLong(0L);

    public WsRoomSubscriptionManager(int maxSubscribersPerSession) {
        this(maxSubscribersPerSession, null);
    }

    public WsRoomSubscriptionManager(int maxSubscribersPerSession, Function<WorldSnapshot, String> customSerializer) {
        this.maxSubscribersPerSession = maxSubscribersPerSession <= 0 ? 50 : maxSubscribersPerSession;
        this.frameSerializer = customSerializer != null ? customSerializer : this::defaultSerializeFrame;
    }

    public boolean registerSubscriber(String simSessionId, Session wsSession) {
        ConcurrentHashMap<String, ConflationWsSender> subscribers =
                rooms.computeIfAbsent(simSessionId, k -> new ConcurrentHashMap<>());
        if (subscribers.size() >= maxSubscribersPerSession) {
            return false;
        }
        subscribers.put(wsSession.getId(), new ConflationWsSender(wsSession));
        return true;
    }

    public void unregisterSubscriber(String simSessionId, String wsSessionId) {
        ConcurrentHashMap<String, ConflationWsSender> subscribers = rooms.get(simSessionId);
        if (subscribers != null) {
            ConflationWsSender removed = subscribers.remove(wsSessionId);
            if (removed != null) {
                removed.close();
            }
        }
    }

    public int subscriberCount(String simSessionId) {
        ConcurrentHashMap<String, ConflationWsSender> subscribers = rooms.get(simSessionId);
        return subscribers == null ? 0 : subscribers.size();
    }

    /**
     * 推演循环每步末调用：无观察者时 0 序列化开销；有观察者时仅序列化一次并向各连接单槽无阻塞投递
     */
    public void broadcastSnapshotIfObserved(WorldSnapshot snapshot) {
        ConcurrentHashMap<String, ConflationWsSender> subscribers = rooms.get(snapshot.sessionId());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        serializationCounter.incrementAndGet();
        String frame = frameSerializer.apply(snapshot);
        for (ConflationWsSender sender : subscribers.values()) {
            sender.offerLatestFrame(frame);
        }
    }

    public void closeRoomOnSessionEnd(String simSessionId) {
        ConcurrentHashMap<String, ConflationWsSender> subscribers = rooms.remove(simSessionId);
        if (subscribers != null) {
            for (ConflationWsSender sender : subscribers.values()) {
                sender.close();
            }
            subscribers.clear();
        }
    }

    public long getSerializationCount() {
        return serializationCounter.get();
    }

    private String defaultSerializeFrame(WorldSnapshot snap) {
        Map<String, Object> envelope = Map.of(
                "ver", FRAME_PROTOCOL_VERSION,
                "type", "TP",
                "sessionId", snap.sessionId(),
                "tick", snap.tickIndex(),
                "worldTimeMs", snap.worldTimeMs(),
                "speed", snap.currentSpeed(),
                "status", snap.status(),
                "instances", snap.instances(),
                "processes", snap.activeProcesses()
        );
        return "TP:" + mapper.writeValueAsString(envelope);
    }
}

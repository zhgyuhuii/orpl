package com.groma.base.websocket;

import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 慢客户端单槽最新帧合并器（落实 BE-PSH-002，完全异步虚拟线程推送，零阻塞推演线程）
 */
public final class ConflationWsSender implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConflationWsSender.class);
    private static final ExecutorService PUSH_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Session wsSession;
    private final AtomicReference<String> latestFrameSlot = new AtomicReference<>(null);
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ConflationWsSender(Session wsSession) {
        this.wsSession = wsSession;
        // 严格遵守 NP-R09：单条消息缓冲上限设为 512KB（远小于 1MB 红线）
        this.wsSession.setMaxTextMessageBufferSize(524_288);
        this.wsSession.setMaxBinaryMessageBufferSize(524_288);
        this.wsSession.setMaxIdleTimeout(600_000L);
    }

    /**
     * 推演线程调用此方法：纳秒级无锁写入槽位即刻返回，慢客户端旧帧自动被最新帧覆盖！
     */
    public void offerLatestFrame(String frameJson) {
        if (closed.get() || !wsSession.isOpen()) {
            return;
        }
        latestFrameSlot.set(frameJson);
        scheduleDrainIfNeeded();
    }

    private void scheduleDrainIfNeeded() {
        if (drainScheduled.compareAndSet(false, true)) {
            PUSH_EXECUTOR.submit(this::drainLoop);
        }
    }

    private void drainLoop() {
        try {
            String payload;
            while (!closed.get() && wsSession.isOpen() && (payload = latestFrameSlot.getAndSet(null)) != null) {
                wsSession.getAsyncRemote().sendText(payload, result -> {
                    if (!result.isOK() && log.isDebugEnabled()) {
                        log.debug("[WS={}] 异步推送帧未送达: {}", wsSession.getId(),
                                result.getException() != null ? result.getException().getMessage() : "unknown");
                    }
                });
            }
        } finally {
            drainScheduled.set(false);
            if (!closed.get() && latestFrameSlot.get() != null) {
                scheduleDrainIfNeeded();
            }
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            latestFrameSlot.set(null);
            try {
                if (wsSession.isOpen()) {
                    wsSession.close();
                }
            } catch (Exception ex) {
                log.debug("关闭 WebSocket 会话异常: {}", ex.getMessage());
            }
        }
    }
}

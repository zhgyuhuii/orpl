package com.groma.bootstrap.hygiene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 客户端心跳自动停止监控器与过期任务工作目录清理器（落实 BE-TSK-007 & BE-CMP-006）
 */
public final class HeartbeatAndWorkDirCleaner {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatAndWorkDirCleaner.class);

    private final boolean heartbeatStopEnabled;
    private final long heartbeatTimeoutMs;
    private final Duration finishedDirRetention;
    private final ConcurrentHashMap<String, Long> lastHeartbeatMillis = new ConcurrentHashMap<>();

    public HeartbeatAndWorkDirCleaner(
            boolean heartbeatStopEnabled,
            long heartbeatTimeoutSeconds,
            Duration finishedDirRetention) {
        this.heartbeatStopEnabled = heartbeatStopEnabled;
        // 标准要求心跳超时不短于 60 秒
        this.heartbeatTimeoutMs = Math.max(60L, heartbeatTimeoutSeconds) * 1_000L;
        this.finishedDirRetention = finishedDirRetention == null ? Duration.ofHours(24) : finishedDirRetention;
    }

    public void touchHeartbeat(String sessionId) {
        lastHeartbeatMillis.put(sessionId, System.currentTimeMillis());
    }

    public void onSessionRemoved(String sessionId, Path sessionWorkDir) throws IOException {
        lastHeartbeatMillis.remove(sessionId);
        if (sessionWorkDir != null && Files.exists(sessionWorkDir)) {
            deleteRecursively(sessionWorkDir);
        }
    }

    public List<String> checkExpiredHeartbeats(long nowMillis, Consumer<String> timeoutStopHandler) {
        if (!heartbeatStopEnabled) {
            return List.of();
        }
        List<String> timedOut = new ArrayList<>();
        for (Map.Entry<String, Long> entry : lastHeartbeatMillis.entrySet()) {
            if (nowMillis - entry.getValue() > heartbeatTimeoutMs) {
                String sid = entry.getKey();
                if (lastHeartbeatMillis.remove(sid) != null) {
                    timedOut.add(sid);
                    timeoutStopHandler.accept(sid);
                    log.info("[Heartbeat] 会话 {} 超过 {}ms 未收到心跳，已自动停止", sid, heartbeatTimeoutMs);
                }
            }
        }
        return timedOut;
    }

    public int sweepStaleWorkDirs(Path workRoot, Instant now, java.util.Set<String> activeSessionIds) throws IOException {
        if (workRoot == null || !Files.isDirectory(workRoot)) {
            return 0;
        }
        int cleaned = 0;
        try (Stream<Path> dirs = Files.list(workRoot)) {
            for (Path sub : dirs.toList()) {
                String name = sub.getFileName().toString();
                if (activeSessionIds.contains(name)) {
                    continue;
                }
                Instant mtime = Files.getLastModifiedTime(sub).toInstant();
                if (Duration.between(mtime, now).compareTo(finishedDirRetention) >= 0) {
                    deleteRecursively(sub);
                    cleaned++;
                }
            }
        }
        return cleaned;
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}

package com.groma.base.engine.runtime;

import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaWorldContext;
import com.groma.base.engine.TickEventLoopGroup;
import com.groma.base.engine.WorldSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * 多模型统一会话管理器（设计书第一章 groma-bootstrap/registry “全局会话租约管理器”的运行时内核）
 * <p>
 * 所有场景（无论来自 Maven 合包模块还是 ECJ 热加载模块）的推演会话都由它创建、推进与回收，
 * 因此生成的场景控制器、宿主的通用 REST、WebSocket 与 MCP 端口共享同一份会话表。
 * </p>
 * 三级会话租约（设计书第七章陷阱 8）：最大步数上限、空闲超时自动停止、超限冷会话最先被回收。
 * 实时模式把会话固定到 {@link TickEventLoopGroup} 的单写者线程；手动模式（未配置事件循环组）
 * 由调用线程逐步推进，便于单测与确定性回归。
 */
public class GromaSessionManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GromaSessionManager.class);

    /** 会话句柄 */
    public record SessionHandle(
            String sessionId,
            GromaModuleRuntime runtime,
            String sceneCode,
            String sceneZhName,
            GromaWorldContext session,
            long createdEpochMs,
            long maxSteps
    ) {}

    /** 会话摘要（不含世界状态） */
    public record SessionSummary(
            String sessionId, String modelCode, String modelVersion, String sceneCode, String sceneZhName,
            String status, long tickIndex, long worldTimeMs, double speed, int instanceCount, int activeProcessCount,
            boolean paused, long lastAccessEpochMs
    ) {}

    /**
     * 会话停止监听器：宿主侧（模型注册中心）用它释放会话持有的模型版本租约。
     * 回调在调用 stopSession 的线程上触发（事件循环停止路径亦然），实现方必须快速返回。
     */
    public interface SessionLifecycleListener {
        void onSessionStopped(String sessionId);
    }

    private final TickEventLoopGroup loopGroup;
    private final int maxConcurrentSessions;
    private final ConcurrentHashMap<String, SessionHandle> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastAccess = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();
    private volatile long maxStepsPerSession = 1_000_000L;
    private volatile Duration idleTimeout = Duration.ofMinutes(30);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<SessionLifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    /** 注册会话停止监听器（宿主释放会话级资源，如热挂载模型租约） */
    public void addLifecycleListener(SessionLifecycleListener listener) {
        if (listener != null) {
            lifecycleListeners.add(listener);
        }
    }

    public GromaSessionManager(TickEventLoopGroup loopGroup, int maxConcurrentSessions) {
        this.loopGroup = loopGroup;
        this.maxConcurrentSessions = Math.max(1, maxConcurrentSessions);
    }

    public TickEventLoopGroup loopGroup() {
        return loopGroup;
    }

    public void setMaxStepsPerSession(long maxSteps) {
        this.maxStepsPerSession = Math.max(1L, maxSteps);
    }

    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout == null ? Duration.ofMinutes(30) : idleTimeout;
    }

    /** 是否实时模式（会话由固定核数事件循环推进） */
    public boolean isRealtime() {
        return loopGroup != null;
    }

    /**
     * 启动场景会话
     *
     * @param startPaused true = 创建后保持暂停，仅响应逐步推进接口；false = 按倍速实时推进
     */
    public String startSession(GromaModuleRuntime runtime, String sceneCode, Map<String, Object> overrides,
                               Long seed, boolean startPaused) {
        return startSession(runtime, sceneCode, overrides, seed, startPaused, null);
    }

    /**
     * 启动场景会话（可在世界初始化之前定制会话对象）
     *
     * @param beforeInitialize 世界初始化之前的定制回调（安装追踪监听器、设置外部脚本执行器等），可空
     */
    public String startSession(GromaModuleRuntime runtime, String sceneCode, Map<String, Object> overrides,
                               Long seed, boolean startPaused, java.util.function.Consumer<GromaWorldContext> beforeInitialize) {
        GromaModuleRuntime.SceneDescriptor scene = runtime.scenes().stream()
                .filter(s -> s.sceneCode().equalsIgnoreCase(sceneCode) || s.zhName().equals(sceneCode)
                        || s.entityId().equals(sceneCode))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "SCENE_NOT_FOUND: 模型 " + runtime.modelCode() + " 不存在场景 " + sceneCode));
        evictIdleSessions();
        if (sessions.size() >= maxConcurrentSessions) {
            throw new IllegalStateException("LIMIT_EXCEEDED: 当前并发推演会话数已达上限 " + maxConcurrentSessions);
        }
        String sessionId = runtime.modelCode().replace('-', '_') + "_" + seq.incrementAndGet();
        long finalSeed = seed != null ? seed : System.nanoTime();
        long stepMs = runtime.defaultStepMs();
        long startEpoch = System.currentTimeMillis();
        GromaWorldContext session = runtime.createSession(scene.sceneCode(), sessionId, startEpoch, stepMs, finalSeed,
                overrides == null ? Map.of() : overrides);
        long maxSteps = maxStepsPerSession;
        session.setStopCondition(() -> session.getTickIndex() >= maxSteps);
        if (beforeInitialize != null) {
            beforeInitialize.accept(session);
        }
        SessionHandle handle = new SessionHandle(sessionId, runtime, scene.sceneCode(), scene.zhName(), session,
                startEpoch, maxSteps);
        if (loopGroup != null) {
            loopGroup.registerAndStartSession(session);
        } else {
            session.bindLoopThread(Thread.currentThread());
            try {
                session.initializeOnLoop();
            } catch (StackOverflowError runaway) {
                // 初始化期默认启动自递归的 SOE 兜底：记录证据并终止会话，不向调用线程裸抛
                session.recordPublicProblem("SESSION_STACK_OVERFLOW: 初始化栈溢出兜底（疑似自递归深度超限），会话已终止");
                session.requestTerminate();
                throw new IllegalStateException("SESSION_STACK_OVERFLOW: 初始化栈溢出兜底，会话已终止", null);
            } finally {
                session.bindLoopThread(null);
            }
        }
        if (startPaused) {
            session.requestPause();
        }
        sessions.put(sessionId, handle);
        lastAccess.put(sessionId, new AtomicLong(System.currentTimeMillis()));
        log.info("[SessionManager] 会话 {} 已启动：模型 {} 场景 {} 种子 {} {}", sessionId, runtime.modelCode(),
                scene.zhName(), finalSeed, startPaused ? "(暂停)" : "");
        return sessionId;
    }

    public SessionHandle require(String sessionId) {
        SessionHandle handle = sessions.get(sessionId);
        if (handle == null) {
            throw new NoSuchElementException("SESSION_NOT_FOUND: 会话不存在 " + sessionId);
        }
        lastAccess.computeIfAbsent(sessionId, k -> new AtomicLong()).set(System.currentTimeMillis());
        return handle;
    }

    public SessionHandle find(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 精确推进 N 步并停在步末：实时模式经请求桥交给单写者线程以突发模式执行，手动模式在调用线程推进
     */
    public WorldSnapshot stepSession(String sessionId, int steps) {
        SessionHandle handle = require(sessionId);
        int n = Math.max(1, Math.min(steps, 1_000_000));
        GromaWorldContext session = handle.session();
        long targetTick = session.getTickIndex() + n;
        if (loopGroup == null) {
            session.bindLoopThread(Thread.currentThread());
            try {
                session.stepExactly(n);
                for (int i = 0; i < n; i++) {
                    try {
                        if (!session.stepOneTickOnLoop()) {
                            break;
                        }
                    } catch (StackOverflowError runaway) {
                        // SOE 会话级兜底（合成模型 RM-C2 实证：真画布自递归裸穿调用线程）：
                        // 记录问题、终止会话，绝不把 StackOverflowError 抛给 REST/MCP 调用线程
                        session.recordPublicProblem("SESSION_STACK_OVERFLOW: 会话栈溢出兜底（疑似自递归深度超限），会话已终止");
                        session.requestTerminate();
                        throw new IllegalStateException("SESSION_STACK_OVERFLOW: 会话栈溢出兜底，会话已终止", null);
                    }
                }
            } finally {
                session.bindLoopThread(null);
            }
            return session.getLatestSnapshot();
        }
        session.resumeForBurst(n);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
        while (session.getTickIndex() < targetTick && session.isActive()) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("TIMEOUT: 推进 " + n + " 步超时，当前步进 " + session.getTickIndex());
            }
            Thread.onSpinWait();
        }
        return session.getLatestSnapshot();
    }

    /** 实时模式：解除暂停并按给定步数突发推进（跑完配额自动暂停） */
    public void pokeBurst(String sessionId) {
        SessionHandle handle = require(sessionId);
        handle.session().resumeForBurst(1);
    }

    public WorldSnapshot getSnapshot(String sessionId) {
        // 实时导出：外部干预写值后立即读回必须反映最新世界状态（步进缓存快照仅供广播合流）
        return require(sessionId).session().snapshotNow();
    }

    public SessionSummary summarize(String sessionId) {
        SessionHandle handle = require(sessionId);
        GromaWorldContext session = handle.session();
        return toSummary(handle);
    }

    public List<SessionSummary> listSessions(String modelCodeOrNull) {
        List<SessionSummary> out = new ArrayList<>();
        for (SessionHandle handle : sessions.values()) {
            if (modelCodeOrNull == null || modelCodeOrNull.isBlank()
                    || modelCodeOrNull.equalsIgnoreCase(handle.runtime().modelCode())) {
                out.add(toSummary(handle));
            }
        }
        return out;
    }

    public void stopSession(String sessionId) {
        SessionHandle handle = sessions.remove(sessionId);
        lastAccess.remove(sessionId);
        if (handle == null) {
            return;
        }
        if (loopGroup != null) {
            loopGroup.unregisterSession(sessionId);
        } else {
            handle.session().requestTerminate();
        }
        for (SessionLifecycleListener listener : lifecycleListeners) {
            try {
                listener.onSessionStopped(sessionId);
            } catch (RuntimeException ex) {
                log.warn("[SessionManager] 会话 {} 停止监听器异常: {}", sessionId, ex.getMessage());
            }
        }
        log.info("[SessionManager] 会话 {} 已停止", sessionId);
    }

    public void setSpeed(String sessionId, double speed) {
        require(sessionId).session().requestSpeed(speed);
    }

    public void pause(String sessionId) {
        require(sessionId).session().requestPause();
    }

    public void resume(String sessionId) {
        require(sessionId).session().requestResume();
    }

    public int sessionCount() {
        return sessions.size();
    }

    /**
     * 空闲超时回收：超过 idleTimeout 没有任何访问的会话自动停止
     *
     * @return 被回收的会话标识
     */
    public List<String> evictIdleSessions() {
        List<String> evicted = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, AtomicLong> entry : lastAccess.entrySet()) {
            if (now - entry.getValue().get() > idleTimeout.toMillis()) {
                String sessionId = entry.getKey();
                SessionHandle handle = sessions.get(sessionId);
                if (handle != null && !handle.session().isActive()) {
                    evicted.add(sessionId);
                }
            }
        }
        for (String sessionId : evicted) {
            stopSession(sessionId);
        }
        return evicted;
    }

    /** 通用 REST / WebSocket / MCP 使用的控制入口 */
    public String control(String sessionId, String command, String payload) {
        SessionHandle handle = require(sessionId);
        GromaWorldContext session = handle.session();
        switch (command == null ? "" : command.trim().toUpperCase(Locale.ROOT)) {
            case "PAUSE" -> session.requestPause();
            case "RESUME" -> session.requestResume();
            case "SPEED" -> session.requestSpeed(Double.parseDouble(payload));
            case "STOP" -> stopSession(sessionId);
            default -> throw new IllegalArgumentException("未知控制指令: " + command);
        }
        return "OK";
    }

    private SessionSummary toSummary(SessionHandle handle) {
        GromaWorldContext session = handle.session();
        WorldSnapshot snap = session.getLatestSnapshot();
        AtomicLong access = lastAccess.get(handle.sessionId());
        return new SessionSummary(
                handle.sessionId(), handle.runtime().modelCode(), handle.runtime().modelVersion(), handle.sceneCode(),
                handle.sceneZhName(), session.getLifecycleStatus(), session.getTickIndex(),
                snap == null ? 0L : snap.worldTimeMs(), session.effectiveSpeedPublic(),
                session.instanceCount(), session.activeProcessCount(), session.isPaused(),
                access == null ? handle.createdEpochMs() : access.get());
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (String sessionId : List.copyOf(sessions.keySet())) {
            stopSession(sessionId);
        }
        // 实时模式下事件循环组由会话管理器一并关闭（宿主 Bean destroyMethod=close 的完整生命周期）
        if (loopGroup != null) {
            loopGroup.close();
        }
    }
}

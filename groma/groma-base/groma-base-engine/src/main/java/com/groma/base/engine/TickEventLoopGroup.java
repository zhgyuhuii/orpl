package com.groma.base.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 固定核数单写者推演事件循环组（落实 NP-ENG-001 与设计书 2.1.3 节）
 * <p>
 * 整机只创建等于 CPU 核数的常驻计算线程，会话按一致性哈希固定绑定到一条线程；每条线程维护一张 O(1) 分层纳秒时间轮，
 * 会话在步进间隙挂在时间轮上，线程 park 到最近的截止时刻，空闲期 0% CPU、0 额外线程。
 * 精确推进 N 步（突发模式）的会话不做节拍等待，全速跑完配额后自动暂停。
 * </p>
 */
public final class TickEventLoopGroup implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TickEventLoopGroup.class);

    /** 暂停会话轮询请求桥的间隔（暂停期间查询与改值请求仍要被服务） */
    private static final long PAUSED_POLL_NS = 20_000_000L;
    /** 最长一次 park 时长，保证即使没有任何会话到期线程也能周期性自检 */
    private static final long MAX_PARK_NS = 1_000_000_000L;

    private final TickWorker[] workers;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TickEventLoopGroup(int configuredThreads) {
        int threads = configuredThreads > 0 ? configuredThreads : Math.max(2, Runtime.getRuntime().availableProcessors());
        this.workers = new TickWorker[threads];
        long originNano = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            workers[i] = new TickWorker("groma-tick-loop-" + i, originNano);
            workers[i].start();
        }
        log.info("[TickEventLoopGroup] 已启动 {} 条单写者推演线程", threads);
    }

    public void registerAndStartSession(AbstractWorldSession session) {
        if (closed.get()) {
            throw new IllegalStateException("TickEventLoopGroup 已关闭");
        }
        workers[Math.floorMod(session.sessionId.hashCode(), workers.length)].attachSession(session);
    }

    public void unregisterSession(String sessionId) {
        workers[Math.floorMod(sessionId.hashCode(), workers.length)].detachSession(sessionId);
    }

    public int workerCount() {
        return workers.length;
    }

    public int sessionCount() {
        int count = 0;
        for (TickWorker w : workers) {
            count += w.sessionCount();
        }
        return count;
    }

    /** 命中断点：会话挂起不再推进步进，世界时间基准进入冻结 */
    public void freezeSession(String sessionId) {
        workers[Math.floorMod(sessionId.hashCode(), workers.length)].freeze(sessionId, true);
    }

    /** 断点恢复：冻结停留时长整体顺延到时间基准，消除追帧 */
    public void resumeSession(String sessionId) {
        workers[Math.floorMod(sessionId.hashCode(), workers.length)].freeze(sessionId, false);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (TickWorker w : workers) {
                w.shutdown();
            }
        }
    }

    // ------------------------------------------------------------------ 时间轮

    /**
     * 会话调度句柄
     */
    static final class ScheduledSession {
        final AbstractWorldSession session;
        final GromaTimeSystem clock;
        long deadlineNano;
        boolean debugFrozen;
        boolean removed;

        ScheduledSession(AbstractWorldSession session, long nowNano) {
            this.session = session;
            this.clock = new GromaTimeSystem(session.getFinalStepMs(), nowNano);
        }
    }

    /**
     * 单线程拥有的纳秒时间轮：1ms 一格、2048 格一圈，超过一圈的截止时刻放入溢出表，轮到时再落格。
     * 入轮与出轮都是 O(1)（溢出表只在会话下一拍间隔超过一圈时使用）。
     */
    static final class GromaTimingWheel {
        private static final long TICK_NS = 1_000_000L;
        private static final int WHEEL_SIZE = 2048;

        private final List<List<ScheduledSession>> buckets = new ArrayList<>(WHEEL_SIZE);
        private final List<ScheduledSession> overflow = new ArrayList<>();
        private final long originNano;
        private long cursorTick;

        GromaTimingWheel(long nowNano) {
            this.originNano = nowNano;
            for (int i = 0; i < WHEEL_SIZE; i++) {
                buckets.add(new ArrayList<>(4));
            }
        }

        void add(ScheduledSession scheduled, long nowNano) {
            scheduled.removed = false;
            long ticksAhead = (scheduled.deadlineNano - nowNano) / TICK_NS;
            if (ticksAhead < 0L) {
                ticksAhead = 0L;
            }
            if (ticksAhead < WHEEL_SIZE) {
                buckets.get((int) ((cursorTick + ticksAhead) % WHEEL_SIZE)).add(scheduled);
            } else {
                overflow.add(scheduled);
            }
        }

        void remove(ScheduledSession scheduled) {
            scheduled.removed = true;
            overflow.removeIf(s -> s == scheduled);
        }

        /**
         * 推进到当前时刻，取出全部到期会话
         */
        List<ScheduledSession> advance(long nowNano) {
            List<ScheduledSession> due = new ArrayList<>(8);
            long nowTick = (nowNano - originNano) / TICK_NS;
            while (cursorTick <= nowTick) {
                List<ScheduledSession> bucket = buckets.get((int) (cursorTick % WHEEL_SIZE));
                if (!bucket.isEmpty()) {
                    due.addAll(bucket);
                    bucket.clear();
                }
                cursorTick++;
            }
            if (!overflow.isEmpty()) {
                long horizon = nowNano + (long) WHEEL_SIZE * TICK_NS;
                for (int i = overflow.size() - 1; i >= 0; i--) {
                    ScheduledSession s = overflow.get(i);
                    if (s.removed) {
                        overflow.remove(i);
                    } else if (s.deadlineNano < horizon) {
                        overflow.remove(i);
                        add(s, nowNano);
                    }
                }
            }
            return due;
        }

        /**
         * 最近的唤醒时刻；没有任何会话时返回 -1
         */
        long nextWakeupNano() {
            for (int i = 0; i < WHEEL_SIZE; i++) {
                if (!buckets.get((int) ((cursorTick + i) % WHEEL_SIZE)).isEmpty()) {
                    return originNano + (cursorTick + i) * TICK_NS;
                }
            }
            long min = -1L;
            for (ScheduledSession s : overflow) {
                if (!s.removed && (min < 0L || s.deadlineNano < min)) {
                    min = s.deadlineNano;
                }
            }
            return min;
        }

        boolean isEmpty() {
            if (!overflow.isEmpty()) {
                return false;
            }
            for (List<ScheduledSession> bucket : buckets) {
                if (!bucket.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ 工作线程

    private static final class TickWorker {
        private final Thread loopThread;
        private final Queue<AbstractWorldSession> pendingInit = new ConcurrentLinkedQueue<>();
        private final Set<String> pendingDetach = ConcurrentHashMap.newKeySet();
        private final Map<String, Boolean> freezeRequests = new ConcurrentHashMap<>();
        private final Map<String, ScheduledSession> sessions = new HashMap<>();
        private final Map<String, ScheduledSession> frozenSessions = new HashMap<>();
        private final GromaTimingWheel wheel;
        private final AtomicBoolean running = new AtomicBoolean(true);

        TickWorker(String name, long originNano) {
            this.wheel = new GromaTimingWheel(originNano);
            this.loopThread = Thread.ofPlatform().name(name).daemon(true).unstarted(this::runLoop);
        }

        void start() {
            loopThread.start();
        }

        int sessionCount() {
            return sessions.size() + pendingInit.size();
        }

        void attachSession(AbstractWorldSession session) {
            pendingInit.offer(session);
            LockSupport.unpark(loopThread);
        }

        void detachSession(String sessionId) {
            pendingDetach.add(sessionId);
            LockSupport.unpark(loopThread);
        }

        void freeze(String sessionId, boolean frozen) {
            freezeRequests.put(sessionId, frozen);
            LockSupport.unpark(loopThread);
        }

        void shutdown() {
            running.set(false);
            LockSupport.unpark(loopThread);
        }

        private void runLoop() {
            while (running.get()) {
                long now = System.nanoTime();
                acceptNewSessions(now);
                applyDetachRequests();
                applyFreezeRequests(now);
                stepDueSessions(now);
                parkUntilNextWake();
            }
        }

        private void acceptNewSessions(long now) {
            AbstractWorldSession session;
            while ((session = pendingInit.poll()) != null) {
                if (pendingDetach.remove(session.sessionId)) {
                    session.abortPendingRequests("会话在挂载前已被停止");
                    continue;
                }
                ScheduledSession scheduled = new ScheduledSession(session, now);
                try {
                    session.bindLoopThread(loopThread);
                    session.initializeOnLoop();
                    sessions.put(session.sessionId, scheduled);
                    scheduled.deadlineNano = now;
                    wheel.add(scheduled, now);
                } catch (RuntimeException ex) {
                    log.error("[Session={}] 初始化失败，会话未被挂载", session.sessionId, ex);
                    session.abortPendingRequests("会话初始化失败");
                }
            }
        }

        private void applyDetachRequests() {
            for (String sessionId : pendingDetach) {
                if (!pendingDetach.remove(sessionId)) {
                    continue;
                }
                ScheduledSession removed = sessions.remove(sessionId);
                if (removed == null) {
                    removed = frozenSessions.remove(sessionId);
                }
                if (removed != null) {
                    removed.removed = true;
                    wheel.remove(removed);
                    removed.session.abortPendingRequests("会话已停止");
                }
            }
        }

        private void applyFreezeRequests(long now) {
            for (Map.Entry<String, Boolean> request : freezeRequests.entrySet()) {
                String sessionId = request.getKey();
                Boolean frozen = request.getValue();
                if (!freezeRequests.remove(sessionId, frozen)) {
                    continue;
                }
                if (frozen) {
                    ScheduledSession scheduled = sessions.get(sessionId);
                    if (scheduled != null) {
                        scheduled.debugFrozen = true;
                        scheduled.clock.enterDebugFreeze(now);
                        wheel.remove(scheduled);
                        frozenSessions.put(sessionId, scheduled);
                    }
                } else {
                    ScheduledSession scheduled = frozenSessions.remove(sessionId);
                    if (scheduled != null) {
                        scheduled.debugFrozen = false;
                        long shifted = scheduled.clock.exitDebugFreezeAndShiftDeadline(now);
                        log.info("[Session={}] 断点恢复，时间基准已顺延 {}ms", sessionId, shifted / 1_000_000L);
                        scheduled.removed = false;
                        scheduled.deadlineNano = now + GromaTimeSystem.MIN_MACHINE_GAP_NS;
                        wheel.add(scheduled, now);
                    }
                }
            }
        }

        private void stepDueSessions(long now) {
            List<ScheduledSession> due = wheel.advance(now);
            for (ScheduledSession scheduled : due) {
                if (scheduled.removed) {
                    continue;
                }
                AbstractWorldSession session = scheduled.session;
                boolean keepAlive;
                try {
                    keepAlive = session.stepOneTickOnLoop();
                } catch (Throwable ex) {
                    log.error("[Session={}] 步进线程级异常终止", session.sessionId, ex);
                    keepAlive = false;
                }
                if (!keepAlive) {
                    sessions.remove(session.sessionId);
                    scheduled.removed = true;
                    continue;
                }
                scheduleNext(scheduled, now);
            }
        }

        private void scheduleNext(ScheduledSession scheduled, long now) {
            AbstractWorldSession session = scheduled.session;
            if (session.isBurstStepping()) {
                // 精确推进 N 步：不做节拍等待，跑完配额后自动暂停
                scheduled.deadlineNano = now;
            } else if (session.isPaused()) {
                scheduled.deadlineNano = now + PAUSED_POLL_NS;
            } else {
                double speed = Math.min(session.effectiveSpeed(), scheduled.clock.maxReachableSpeed());
                scheduled.deadlineNano = scheduled.clock.nextDeadlineNano(now, session.getTickIndex() + 1L, speed);
            }
            wheel.add(scheduled, now);
        }

        private void parkUntilNextWake() {
            if (!pendingInit.isEmpty() || !pendingDetach.isEmpty() || !freezeRequests.isEmpty()) {
                return;
            }
            if (wheel.isEmpty()) {
                LockSupport.parkNanos(MAX_PARK_NS);
                return;
            }
            long wakeup = wheel.nextWakeupNano();
            long waitNs = wakeup < 0L ? MAX_PARK_NS : wakeup - System.nanoTime();
            if (waitNs > 0L) {
                LockSupport.parkNanos(Math.min(waitNs, MAX_PARK_NS));
            }
        }
    }
}

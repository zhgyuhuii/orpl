package com.groma.base.engine;

import com.groma.base.common.util.SeededSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 单写者推演会话抽象基类（落实 BE-SCH-001 ~ 006、BE-EVT-001 ~ 004、BE-TSK-006）
 * 严格无阻塞监视器锁：推演内部状态仅由所属 TickEventLoop 线程访问；对外仅暴露 AtomicReference<WorldSnapshot>。
 */
public abstract class AbstractWorldSession {

    private static final Logger log = LoggerFactory.getLogger(AbstractWorldSession.class);

    protected final String sessionId;
    protected final String modelCode;
    protected final String modelVersion;
    protected final String sceneCode;
    protected long startEpochMs;

    /**
     * 存档续跑时回填世界墙钟基准（时间日期类成员依赖 startEpochMs + tick × 步长）
     */
    public final void restoreWorldClock(long restoredStartEpochMs) {
        this.startEpochMs = restoredStartEpochMs;
    }
    protected final long finalStepMs;
    protected final SeededSampler sampler;

    // 仅由单写者 EventLoop 线程访问的状态
    private Thread boundLoopThread;
    protected long tickIndex = -1L;
    protected boolean initializing = true;
    private final ArrayDeque<WorldSnapshot.SimEventRecord> eventQueue = new ArrayDeque<>(256);
    private final List<String> boundedProblems = new ArrayList<>(64);
    private long eventSeqGenerator = 0L;
    private int maxEventsPerStep = 100_000;
    private long maxDispatchDurationNs = 50_000_000L;

    // 跨线程无锁原子控制位与快照槽
    private final MpscRequestBridge<AbstractWorldSession> requestBridge = new MpscRequestBridge<>();
    private final AtomicReference<WorldSnapshot> latestSnapshot = new AtomicReference<>();
    private final AtomicReference<String> lifecycleStatus = new AtomicReference<>("CREATED");
    private final AtomicLong activeTwinCount = new AtomicLong(0L);
    /** 倍速控制与孪生过程 1x 锁速仲裁器 */
    private final SpeedManager speedArbitrator;
    private final AtomicInteger subscriberCount = new AtomicInteger(0);
    private volatile double requestedSpeed = 1.0D;
    private volatile boolean paused = false;
    private volatile long remainingStepQuota = -1L; // 用于 sim.step(N) 精确停在第 N 步末
    private Consumer<WorldSnapshot> snapshotSink = s -> {};

    protected AbstractWorldSession(String sessionId, String modelCode, String modelVersion,
                                   String sceneCode, long startEpochMs, long finalStepMs, long seed) {
        this.sessionId = sessionId;
        this.modelCode = modelCode;
        this.modelVersion = modelVersion;
        this.sceneCode = sceneCode;
        this.startEpochMs = startEpochMs;
        this.finalStepMs = finalStepMs;
        this.sampler = new SeededSampler(seed);
        this.speedArbitrator = new SpeedManager(finalStepMs);
    }

    public final void bindLoopThread(Thread loopThread) {
        this.boundLoopThread = loopThread;
    }

    protected final void assertSingleWriter() {
        Thread current = Thread.currentThread();
        if (boundLoopThread != null && current != boundLoopThread) {
            throw new IllegalStateException("违反单写者约束！当前线程 " + current.getName()
                    + " 越界访问属于 " + boundLoopThread.getName() + " 的推演状态");
        }
    }

    /**
     * 起步准备段（DS_INDEX = -1）：初始化阶段静默设初值不产生值变化事件（标准 10.2.1）
     */
    public final void initializeOnLoop() {
        assertSingleWriter();
        this.initializing = true;
        initEntitiesAndDefaultStarts();
        this.initializing = false;
        this.tickIndex = 0L;
        this.lifecycleStatus.set("RUNNING");
        publishSnapshotIfNeeded(true);
    }

    /**
     * 从存档快照恢复（落实 BE-TSK-006 / 彻底修复存量 BE-03 续跑后 isReloaded 永久短路事件触发的 P0 缺陷！）
     * 装载期间 initializing = true 抑制重放噪音，装载完毕立即复位为 false，运行期所有事件触发 100% 正常工作！
     */
    public final void restoreFromArchiveOnLoop(WorldSnapshot archiveSnap) {
        assertSingleWriter();
        this.initializing = true;
        loadArchiveState(archiveSnap);
        this.tickIndex = archiveSnap.tickIndex();
        this.initializing = false;
        this.lifecycleStatus.set("RUNNING");
        publishSnapshotIfNeeded(true);
    }

    /**
     * 单步主循环（严格遵循 BE-SCH-001 阶段顺序，且每步仅发布一次不可变快照）
     */
    public final boolean stepOneTickOnLoop() {
        assertSingleWriter();
        String st = lifecycleStatus.get();
        if ("STOPPED".equals(st) || "FAILED".equals(st)) {
            requestBridge.drainAndAbortAll(st);
            return false;
        }
        try {
            // 阶段 1：安全点排空外部请求桥（即使暂停期间也继续处理查询/改值请求，满足 CK-BE-SCH-005-3）
            requestBridge.drainOnLoopThread(this, 1024);

            // 阶段 2：派发队列事件（含熔断保护）
            if (!drainEventsWithCircuitBreaker()) {
                return false;
            }

            // 阶段 3：暂停与单步配额判定
            if (paused && remainingStepQuota <= 0L) {
                return true;
            }

            // 阶段 4：执行当前步活跃耗时过程与算子
            executeActiveProcessesAtTick(tickIndex, currentWorldTimeMs());

            // 阶段 5：提交本步收尾动作（消耗/生成/状态转换仅在过程成功后发生，标准 10.1.3）并再次派发衍生事件
            commitCompletedProcesses();
            if (!drainEventsWithCircuitBreaker()) {
                return false;
            }

            // 阶段 6：空闲实例安全点回收（落实 BE-INS-003，在推演线程内回收，绝不跨线程并发删集合）
            sweepIdleInstancesAtSafePoint(tickIndex);

            // 阶段 7：终止条件与一次性暂停条件评估（标准 12.5.3）
            if (evaluatePersistentStopCondition()) {
                lifecycleStatus.set("STOPPED");
                publishSnapshotIfNeeded(true);
                requestBridge.drainAndAbortAll("满足终止条件正常结束");
                return false;
            }
            if (evaluateOneShotPauseCondition()) {
                this.paused = true;
            }

            // 阶段 8：推进步进索引，计算无漂移世界时间（start + index * step，标准 14.5）
            this.tickIndex++;
            if (remainingStepQuota > 0L) {
                remainingStepQuota--;
                if (remainingStepQuota == 0L) {
                    this.paused = true;
                }
            }

            // 阶段 9：每步仅发布一次不可变快照（零观察者时跳过昂贵序列化，满足 NP-ENG-002）
            publishSnapshotIfNeeded(false);
            return true;
        } catch (Throwable ex) {
            log.error("[Session={}] 步进 {} 执行异常终止", sessionId, tickIndex, ex);
            recordProblem("FATAL: " + ex.getMessage());
            lifecycleStatus.set("FAILED");
            publishSnapshotIfNeeded(true);
            requestBridge.drainAndAbortAll(ex.getMessage());
            return false;
        }
    }

    private boolean drainEventsWithCircuitBreaker() {
        int count = 0;
        long startNs = System.nanoTime();
        while (!eventQueue.isEmpty()) {
            count++;
            if (count > maxEventsPerStep || (System.nanoTime() - startNs) > maxDispatchDurationNs) {
                String msg = "事件风暴熔断：单步事件派发超过上限 (" + count + " 条)，任务自动保护停止";
                recordProblem(msg);
                lifecycleStatus.set("FAILED");
                publishSnapshotIfNeeded(true);
                requestBridge.drainAndAbortAll(msg);
                return false;
            }
            WorldSnapshot.SimEventRecord ev = eventQueue.poll();
            dispatchSingleEvent(ev);
        }
        return true;
    }

    public final void enqueueObjectEvent(String eventCode, String sourceInstanceId, String detail) {
        assertSingleWriter();
        if (initializing && "v".equals(eventCode)) {
            // 标准 10.2.1：初始化阶段不产生对象值变化事件
            return;
        }
        eventQueue.offer(new WorldSnapshot.SimEventRecord(
                ++eventSeqGenerator, tickIndex, eventCode, sourceInstanceId, detail));
    }

    /**
     * 编译产出模型运行期的富事件入队：携带成员标识、状态中文原名与创建主体。
     * 是否抑制由子上下文按“初始化静默设初值、默认启动后正常发事件”的规则自行判定，这里不做初始化期过滤。
     */
    public final void enqueueMemberEvent(String eventCode, String sourceInstanceId, String memberToken,
                                         String stateLabel, String subjectId, String detail) {
        assertSingleWriter();
        eventQueue.offer(new WorldSnapshot.SimEventRecord(
                ++eventSeqGenerator, tickIndex, eventCode, sourceInstanceId, detail, memberToken, stateLabel, subjectId));
    }

    /**
     * 在单写者线程内立即排空当前已入队的事件（存量引擎在提交阶段子过程收尾后同样会立刻派发一次事件）。
     * 带独立的事件风暴熔断预算，返回 false 表示已触发熔断并终止会话。
     */
    public final boolean drainPendingEventsNow() {
        assertSingleWriter();
        int count = 0;
        long startNs = System.nanoTime();
        while (!eventQueue.isEmpty()) {
            count++;
            if (count > maxEventsPerStep || (System.nanoTime() - startNs) > maxDispatchDurationNs) {
                String msg = "事件风暴熔断：提交阶段事件派发超过上限 (" + count + " 条)，任务自动保护停止";
                recordProblem(msg);
                lifecycleStatus.set("FAILED");
                publishSnapshotIfNeeded(true);
                requestBridge.drainAndAbortAll(msg);
                return false;
            }
            dispatchSingleEvent(eventQueue.poll());
        }
        return true;
    }

    public final int pendingEventCount() {
        return eventQueue.size();
    }

    /**
     * 丢弃 mark 之后入队的全部事件（单写者线程内，mark 由 pendingEventCount 取得）。
     * 用于零时间循环熔断：让失控的过程自己失败，而不让它堆积的事件触发整个会话的事件风暴熔断。
     */
    public final void discardEventsAfter(int mark) {
        assertSingleWriter();
        while (eventQueue.size() > mark) {
            eventQueue.pollLast();
        }
    }

    // ---------------- 跨线程控制位（REST / WebSocket / MCP / DAP 经请求桥或直接置位） ----------------

    public final void requestPause() {
        this.paused = true;
    }

    public final void requestResume() {
        this.remainingStepQuota = -1L;
        this.paused = false;
    }

    public final void requestSpeed(double speed) {
        if (speed > 0.0D && !Double.isNaN(speed) && !Double.isInfinite(speed)) {
            // 经倍速仲裁器夹取到机器可达上限（finalStepMs / 10ms）；孪生过程运行期 effectiveSpeed() 强制返回 1.0
            this.requestedSpeed = speedArbitrator.requestSpeed(speed);
        }
    }

    /**
     * 请求全速（孪生过程运行期间禁止进入全速模式，返回 false）
     */
    public final boolean requestFullSpeed() {
        if (speedArbitrator.requestFullSpeed()) {
            this.requestedSpeed = speedArbitrator.getAppliedSpeed();
            return true;
        }
        return false;
    }

    /** 会话卸载：排空并拒绝全部等待中的外部请求，绝不悬挂调用方 */
    public final void abortPendingRequests(String reason) {
        requestBridge.drainAndAbortAll(reason);
    }

    public final double getRequestedSpeed() {
        return requestedSpeed;
    }

    public final boolean isPaused() {
        return paused;
    }

    /**
     * 是否处于“精确推进 N 步”的突发模式：此时事件循环不做实时节拍等待，全速跑完配额后自动暂停
     */
    public final boolean isBurstStepping() {
        return remainingStepQuota > 0L;
    }

    /**
     * 单写者线程上执行：解除暂停并按给定步数突发推进（跑完配额后自动暂停），用于“精确推进 N 步”接口
     */
    public final void resumeForBurst(int steps) {
        assertSingleWriter();
        this.remainingStepQuota = Math.max(1, steps);
        this.paused = false;
    }

    /** 会话是否仍在推演（未终止、未失败） */
    public final boolean isActive() {
        String st = lifecycleStatus.get();
        return !"STOPPED".equals(st) && !"FAILED".equals(st);
    }

    /** 对外只读的当前生效倍速（含孪生锁速仲裁结果） */
    public final double effectiveSpeedPublic() {
        return effectiveSpeed();
    }

    public final String getLifecycleStatus() {
        return lifecycleStatus.get();
    }

    public final long getTickIndex() {
        return tickIndex;
    }

    public final long getFinalStepMs() {
        return finalStepMs;
    }

    protected final boolean isInitializingPhase() {
        return initializing;
    }

    /**
     * 装载完成：复位装载标志、恢复步进索引并发布首帧快照（与 initializeOnLoop 的收尾语义一致，保证装载后事件触发 100% 正常）
     */
    protected final void completeRestore(long restoredTickIndex) {
        assertSingleWriter();
        this.initializing = false;
        this.tickIndex = restoredTickIndex;
        this.lifecycleStatus.set("RUNNING");
        publishSnapshotIfNeeded(true);
    }

    public final void setSnapshotSink(Consumer<WorldSnapshot> sink) {
        this.snapshotSink = sink != null ? sink : s -> {};
    }

    public final void setObserverCount(int count) {
        this.subscriberCount.set(Math.max(0, count));
    }

    public final void setEventStormLimits(int maxEventsPerStep, long maxDispatchDurationNs) {
        this.maxEventsPerStep = Math.max(1, maxEventsPerStep);
        this.maxDispatchDurationNs = Math.max(1_000_000L, maxDispatchDurationNs);
    }

    /**
     * 外部请求终止（用户停止、心跳超时、会话租约到期）；在下一个步进安全点生效
     */
    public final void requestTerminate() {
        this.lifecycleStatus.set("STOPPED");
        requestBridge.drainAndAbortAll("会话已被请求终止");
    }

    /**
     * 孪生过程引用计数锁速（落实 BE-SCH-004：运行期锁 1.0x，全部结束后恢复用户最新期望倍速）
     */
    public final double effectiveSpeed() {
        return activeTwinCount.get() > 0L ? 1.0D : requestedSpeed;
    }

    public final void onTwinProcessStarted() {
        activeTwinCount.incrementAndGet();
        speedArbitrator.onTwinStarted();
    }

    public final void onTwinProcessEnded() {
        long left = activeTwinCount.updateAndGet(v -> Math.max(0L, v - 1L));
        if (left == 0L) {
            speedArbitrator.onTwinFinished();
        }
    }

    public final long currentWorldTimeMs() {
        return startEpochMs + Math.max(0L, tickIndex) * finalStepMs;
    }

    public final <R> CompletableFuture<R> submitCommand(java.util.function.Function<AbstractWorldSession, R> cmd, long timeoutMs) {
        return requestBridge.submit(cmd, timeoutMs);
    }

    public final void stepExactly(int steps) {
        this.remainingStepQuota = steps;
        this.paused = false;
    }

    public final WorldSnapshot getLatestSnapshot() {
        return latestSnapshot.get();
    }

    /** 实时导出不可变快照（不经步进缓存：外部干预写值后立即读回需调用本方法） */
    public final WorldSnapshot snapshotNow() {
        return buildImmutableSnapshot();
    }

    /** 手动模式下由调用线程（即当前写者线程）立即执行已提交的桥指令，无需等待下一次步进 */
    public final void drainRequests() {
        requestBridge.drainOnLoopThread(this, 256);
    }

    private void publishSnapshotIfNeeded(boolean force) {
        if (!force && subscriberCount.get() <= 0 && latestSnapshot.get() != null) {
            // 仍更新轻量内存引用供 REST/MCP 即时读取，但通知下游跳过 JSON 序列化广播
            WorldSnapshot snap = buildImmutableSnapshot();
            latestSnapshot.set(snap);
            return;
        }
        WorldSnapshot snap = buildImmutableSnapshot();
        latestSnapshot.set(snap);
        snapshotSink.accept(snap);
    }

    protected final void recordProblem(String problem) {
        if (boundedProblems.size() >= 200) {
            boundedProblems.remove(0);
        }
        boundedProblems.add(problem);
    }

    protected List<String> copyProblems() {
        return List.copyOf(boundedProblems);
    }

    protected abstract void initEntitiesAndDefaultStarts();
    protected abstract void loadArchiveState(WorldSnapshot archiveSnap);
    protected abstract void dispatchSingleEvent(WorldSnapshot.SimEventRecord event);
    protected abstract void executeActiveProcessesAtTick(long tick, long worldTimeMs);
    protected abstract void commitCompletedProcesses();
    protected abstract void sweepIdleInstancesAtSafePoint(long tick);
    protected abstract boolean evaluatePersistentStopCondition();
    protected abstract boolean evaluateOneShotPauseCondition();
    protected abstract WorldSnapshot buildImmutableSnapshot();
}

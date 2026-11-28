package com.groma.base.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 编译产出过程任务的无栈状态机基类（落实设计书 3.2 节“switch (runtimeBO.getPc()) 状态转移表”与 R23 调用关系）
 * <p>
 * 一个过程的设计视图被编译成一张节点表：{@link #execNode(int)} 是单个节点的语义（工具运算或子过程调用），
 * {@link #nodeDone(int)} 是节点成功后的后置动作（R13/R14 状态转换、R12 生成）与后继激活。
 * 运行期用“就绪 pc 队列 + 等待表”代替调用栈：瞬时节点同步推进；耗时节点或耗时子过程挂入等待表，
 * 由所属世界上下文逐拍推进，子任务成功后在提交阶段续接后继，因此可以跨步进挂起与恢复，也可无损存档。
 * </p>
 * 时间规则与存量引擎一致：
 * <ul>
 *   <li>T1 —— 子过程是否耗时 = 自身耗时 且 父过程耗时；瞬时过程里的全部子过程都是瞬时算子，同步执行完毕；</li>
 *   <li>耗时叶过程每个执行精度拍运行一次方法体，累计时长达到期望时长后成功；</li>
 *   <li>耗时复合过程在全部子任务结束、后继耗尽时成功；</li>
 *   <li><b>回边按拍推进</b>：耗时过程里，本拍已执行过的节点被回边再次激活时挂起到下一拍续跑——
 *       “耗时过程 + 瞬时节点循环”每拍执行一圈，循环出口所在的那一拍自然成功。
 *       存量引擎对这种结构没有循环保护（要么空转成功、要么死循环），这里给出确定的按拍节律；</li>
 *   <li>瞬时过程（算子）里的回边循环仍在同一拍内同步跑完，由零时间循环配额熔断保护。</li>
 * </ul>
 * 每个步进 = 运行阶段（推进耗时叶过程）+ 提交阶段（子任务成功后续接后继）。
 */
public abstract class GromaProcessTask {

    public static final String READY = "READY";
    public static final String RUNNING = "RUNNING";
    public static final String WAIT4SUCCESS = "WAIT4SUCCESS";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String ABORTED = "ABORTED";

    /** 单次推进允许经过的最多节点数（零时间循环配额熔断，落实设计书 5.2 节陷阱 7 与 R24 自调用保护） */
    public static final int ZERO_TIME_NODE_QUOTA = 10_000;
    /** 同步模式（算子）允许的最外层驱动次数 */
    private static final int MAX_SYNC_DRIVES = 10_000;

    /** 耗时工具节点子任务使用的时长属性：按步数计时，持续一个步进 */
    static final GromaProcessTask.Timing ONE_STEP = new GromaProcessTask.Timing(
            true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 过程的时间与并发属性（来自画布实体/图元）
     *
     * @param timeConsuming        是否耗时过程
     * @param twinLocked           R2 来自真实世界的孪生过程：运行期强制锁 1x 倍速
     * @param asyncMode            异步外部等待过程
     * @param singletonMode        0 非单例 / 1 实例单例 / 3 类型单例
     * @param durationParamType    STEP（按步数）或 FIXED（按时间单位）
     * @param timeNormal           期望持续时长（步数或时间单位数）
     * @param timeUnit             时长单位
     * @param timeMin              最短时长（时间单位数，可空）
     * @param timeMax              最长时长（时间单位数，可空）
     * @param distribution         分布：空 / uniform / normal / exponential / triangular
     * @param distMin              均匀分布下限
     * @param distMax              均匀分布上限
     * @param distAvg              正态分布均值
     * @param distSd               正态分布标准差
     * @param distParam            指数分布均值 / 三角分布众数
     * @param accuracyMs           执行精度（毫秒，可空）
     * @param executionPeriodTicks 执行周期 P = max(1, ceil(精度 / 步长))
     */
    public record Timing(
            boolean timeConsuming,
            boolean twinLocked,
            boolean asyncMode,
            int singletonMode,
            String durationParamType,
            double timeNormal,
            String timeUnit,
            Double timeMin,
            Double timeMax,
            String distribution,
            Double distMin,
            Double distMax,
            Double distAvg,
            Double distSd,
            Double distParam,
            Long accuracyMs,
            long executionPeriodTicks
    ) {
        public static final Timing INSTANT = new Timing(
                false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);
    }

    protected final GromaWorldContext ctx;
    protected final BaseSimEntity host;
    private final String defKey;
    private final String processName;
    private final String processInstanceId;
    private final Timing timing;

    private volatile String status = READY;
    private volatile String failReason = "";
    private boolean syncMode;
    private boolean flowEntered;
    private boolean finishNotified;
    private long stepCount;
    private long elapsedMs;
    private long expectedMs;
    private long precision = 1L;
    private long startTick;
    private int nodeBudget = ZERO_TIME_NODE_QUOTA;
    private GromaProcessTask parent;
    private int parentPc = -1;
    private final List<GromaProcessTask> children = new ArrayList<>();
    private final ArrayDeque<Integer> ready = new ArrayDeque<>();
    /** 回边按拍推进：本拍已执行过的节点（再次激活即挂起） */
    private final Set<Integer> executedThisTick = new HashSet<>();
    /** 连线延迟：pc → 剩余等待拍数（activateAfter 挂入，每拍递减，到期激活） */
    private final Map<Integer, Long> timedDeferred = new LinkedHashMap<>();
    /** 挂起到下一拍的回边激活 */
    private final LinkedHashSet<Integer> deferred = new LinkedHashSet<>();
    private long machineTick = Long.MIN_VALUE;
    /** 存档续跑标记：恢复现场已把在途子任务计入 waiting，子任务 linkToParent 时应认领而非再累加 */
    private boolean runtimeImported;
    private final Map<Integer, Integer> restoredWaitingUnclaimed = new HashMap<>();
    private final LinkedHashMap<Integer, Integer> waiting = new LinkedHashMap<>();
    private final Map<Integer, Boolean> nodeOk = new HashMap<>();
    /** 子过程出口事件（R23 事件线 / R25R26 异常边消费用）：调用节点 pc → 最近一个已结束子过程的出口码 */
    private final Map<Integer, String> childExit = new HashMap<>();
    /** 本过程自身的出口事件：ok / pca（提前完成）/ pcd（超时完成），finishSuccess 时按 TIMING 上下限归类 */
    private String exitEvent = "ok";
    /** lastEnd 结束点收尾：实际用时不代表完整执行周期，豁免 pca/pcd 归类 */
    private boolean finishedByLastEnd;
    private String lockedKey;
    private boolean twinCounted;
    private Runnable completionHook;

    protected GromaProcessTask(GromaWorldContext ctx, BaseSimEntity host, String defKey, String processName, Timing timing) {
        this.ctx = ctx;
        this.host = host;
        this.defKey = defKey;
        this.processName = processName;
        this.timing = timing == null ? Timing.INSTANT : timing;
        this.processInstanceId = ctx.nextProcessInstanceId();
    }

    // ------------------------------------------------------------------ 生成代码必须实现的节点表

    /** 是否含编译出的数据流图（false = 叶过程：仅 javaCode 或纯等待时长） */
    protected abstract boolean hasFlow();

    /**
     * 入口节点集合（标准 9.4.7：全部 firstStart 子过程并行激活；只有一个子过程时它即入口）；无流程图返回空数组
     */
    protected abstract int[] entryNodes();

    /**
     * 执行单个节点的语义：工具节点把结果写入输出槽位（不激活后继）；调用节点发起子过程。
     *
     * @return true 表示该节点已同步完成（框架随后调用 nodeDone）；false 表示已挂起等待耗时子任务
     */
    protected abstract boolean execNode(int pc);

    /** 节点成功后的后置动作（回写实参、状态转换、生成）与后继激活 */
    protected abstract void nodeDone(int pc);

    /** 该节点自身的工具体是否为耗时节点（画布上标记为时间有关的工具过程），仅在耗时过程中生效 */
    protected boolean isSlowToolNode(int pc) {
        return false;
    }

    /** 叶过程的 javaCode 体：耗时叶过程每个执行精度拍调用一次；瞬时叶过程调用一次 */
    protected void execLeafBody() {
    }

    /** 启动前校验（R22 启动条件、非空输入守卫）；返回 false 则启动失败(psf) */
    protected boolean canStart() {
        return true;
    }

    /**
     * 维持条件逐拍校验（R21 agent 关系，方案 D 收割）：耗时过程每个执行拍在推进前调用，
     * 生成代码在条件不满足时走 abortByMaintainBroken（级联终止 + paa 事件）。默认无维持条件。
     */
    protected void checkMaintain() {
    }

    /**
     * R06 结构控制：本过程关注的过程使能位集合（类型上指向本过程的控制规则位按位或；
     * 生成代码在类型带控制规则时覆写）。0 = 无控制规则约束（默认，门点零开销）。
     */
    protected long processEnableBits() {
        return 0L;
    }

    /** R06 控制失能中断：级联取消并派发 paa（与维持中断同一收尾语义） */
    protected final void abortByControlDisabled() {
        ctx.emitProcessEvent(this, "paa", "R06 结构控制失能中断");
        cancelCascade("R06 结构控制失能");
    }

    /** 过程成功收尾：回写双向形参、输出形参（R18/R19/R20），由生成代码实现 */
    protected void onSuccess() {
    }

    /** 过程失败收尾 */
    protected void onFail() {
    }

    /** 导出形参槽位、局部量与节点输出（存档续跑用） */
    protected void exportSlots(Map<String, Object> slots) {
    }

    /** 回填形参槽位、局部量与节点输出（存档续跑用） */
    protected void importSlots(Map<String, Object> slots) {
    }

    // ------------------------------------------------------------------ 对生成代码开放的框架能力

    /**
     * 激活后继节点。耗时过程里，指向本拍已执行过节点的激活是回边，挂起到下一拍（每拍一圈）；
     * 其余情况立即进入就绪队列。
     */
    protected final void activate(int pc) {
        if (timing.timeConsuming() && !syncMode && executedThisTick.contains(pc)) {
            deferred.add(pc);
            return;
        }
        ready.addLast(pc);
    }

    /**
     * 连线延迟激活：目标 pc 等待 ticks 拍后进入就绪队列（存量引擎“等待”子过程的等价语义）
     */
    protected final void activateAfter(int pc, long ticks) {
        if (ticks <= 0) {
            activate(pc);
            return;
        }
        timedDeferred.merge(pc, ticks, Long::sum);
    }

    /** 节点是否成功完成（供带启动失败分支的后继判断） */
    protected final boolean nodeOk(int pc) {
        return !Boolean.FALSE.equals(nodeOk.get(pc));
    }

    /**
     * 调用节点最近一个已结束子过程的出口事件码（R23 事件线 / R25R26 异常边消费，方案 D 收割）：
     * ok 正常成功 / psf 启动或执行失败 / paa 被维持中断或级联终止 / pca 提前完成 / pcd 超时完成。
     */
    protected final String childExitEvent(int pc) {
        return childExit.getOrDefault(pc, "ok");
    }

    private static String classifyExit(GromaProcessTask child) {
        if (SUCCESS.equals(child.status)) {
            return child.exitEvent;
        }
        if (ABORTED.equals(child.status)) {
            return "paa";
        }
        return "psf";
    }

    /**
     * lastEnd 结束点级联（方案 D 收割）：结束节点完成后立即终止本过程的在途旁路子任务
     * （真级联：子任务置 ABORTED 收尾并释放单例锁/孪生计数，僵尸任务会锁死同类型单例并污染存档），
     * 清空未走队列，置 WAIT4SUCCESS 让提交阶段即刻收尾（成功回写与钩子照常执行）。
     */
    protected final void finishWithLastEnd() {
        for (GromaProcessTask child : List.copyOf(children)) {
            if (!child.finished()) {
                child.cancelCascade("lastEnd 结束点级联取消");
            }
        }
        children.clear();
        waiting.clear();
        ready.clear();
        deferred.clear();
        timedDeferred.clear();
        finishedByLastEnd = true;
        exitEvent = "ok";
        if (RUNNING.equals(status)) {
            status = WAIT4SUCCESS;
        }
    }

    /**
     * 调用子过程：瞬时上下文里同步跑完；耗时上下文里耗时子过程挂为子任务并在成功后续接本节点后继
     *
     * @return true 表示子过程已同步结束（成功或启动失败）；false 表示已挂起等待
     */
    protected final boolean spawnOrRun(int pc, GromaProcessTask subProcess) {
        subProcess.parent = this;
        subProcess.parentPc = pc;
        if (syncMode || !subProcess.timing.timeConsuming()) {
            // T1：父过程为瞬时（或正被同步驱动）时，子过程一律按瞬时算子同步执行
            subProcess.syncMode = true;
            nodeOk.put(pc, subProcess.runToCompletionSync());
            childExit.put(pc, classifyExit(subProcess));
            return true;
        }
        children.add(subProcess);
        boolean started = subProcess.begin();
        if (!started || subProcess.finished()) {
            children.remove(subProcess);
            nodeOk.put(pc, SUCCESS.equals(subProcess.status));
            childExit.put(pc, classifyExit(subProcess));
            return true;
        }
        waiting.merge(pc, 1, Integer::sum);
        return false;
    }

    /** 记录失败原因并立即收尾 */
    protected final void fail(String reason) {
        if (finished()) {
            return;
        }
        this.failReason = reason == null ? "" : reason;
        this.status = FAILED;
        finishNow();
    }

    protected final void fail(Throwable cause) {
        if (finished()) {
            return;
        }
        if (cause != null && !(cause instanceof GromaProcessException)) {
            GromaWorldContext.LOG.warn("[Process={}] 过程 {} 执行异常", processInstanceId, processName, cause);
        }
        String msg = cause == null ? "" : cause.getMessage();
        fail(cause == null ? "" : cause.getClass().getSimpleName() + (msg == null || msg.isBlank() ? "" : ": " + msg));
    }

    /** 当前上下文世界 */
    protected final GromaWorldContext world() {
        return ctx;
    }

    /** 宿主对象（独立过程为 null） */
    protected final BaseSimEntity host() {
        return host;
    }

    /** 顶层过程成功后的附加动作（事件接线 / 默认启动调用点上的 out-change、result、consumption） */
    public final void setCompletionHook(Runnable hook) {
        this.completionHook = hook;
    }

    /**
     * DAP 源码级探针（第六章 6.2）：宿主/调试会话挂载后，每个画布图元节点执行前回调
     * （elementId = 画布图元 id；调试会话在此判断断点并挂起虚拟线程）
     */
    public final void setDebugProbe(java.util.function.Consumer<String> probe) {
        this.debugProbe = probe;
    }

    private volatile java.util.function.Consumer<String> debugProbe;

    protected final void fireLineProbe(String elementId) {
        java.util.function.Consumer<String> probe = debugProbe;
        if (probe != null) {
            probe.accept(elementId);
        }
    }

    public final String getProcessInstanceId() {
        return processInstanceId;
    }

    public final String getDefKey() {
        return defKey;
    }

    public final String getProcessName() {
        return processName;
    }

    public final String getStatus() {
        return status;
    }

    public final String getFailReason() {
        return failReason;
    }

    public final boolean isTimeRelated() {
        return timing.timeConsuming;
    }

    public final Timing timing() {
        return timing;
    }

    public final long getElapsedMs() {
        return elapsedMs;
    }

    public final long getStepCount() {
        return stepCount;
    }

    public final long getExpectedMs() {
        return expectedMs;
    }

    public final long getStartTick() {
        return startTick;
    }

    public final GromaProcessTask getParent() {
        return parent;
    }

    /** 过程宿主对象（独立过程为 null）——调试适配协议与追踪使用 */
    public final BaseSimEntity hostEntity() {
        return host;
    }

    public final int getParentPc() {
        return parentPc;
    }

    public final List<GromaProcessTask> getChildren() {
        return List.copyOf(children);
    }

    public final boolean finished() {
        return SUCCESS.equals(status) || FAILED.equals(status) || ABORTED.equals(status);
    }

    // ------------------------------------------------------------------ 生命周期

    /**
     * 顶层启动入口（事件接线、默认启动、外部干预）：瞬时过程同步跑完，耗时过程挂入步进队列
     *
     * @return false 表示启动失败
     */
    public final boolean startTopLevel() {
        if (timing.timeConsuming()) {
            return begin();
        }
        this.syncMode = true;
        return runToCompletionSync();
    }

    /**
     * 算子入口：在当前调用栈上同步跑完整个过程（含其全部瞬时子过程链），返回是否成功
     */
    public final boolean runToCompletionSync() {
        this.syncMode = true;
        if (!begin()) {
            return false;
        }
        int drives = 0;
        while (!finished()) {
            if (++drives > MAX_SYNC_DRIVES) {
                fail("零时间循环配额熔断：同步过程推进超过上限");
                return false;
            }
            commit();
        }
        return SUCCESS.equals(status);
    }

    /**
     * 启动：单例约束 → 启动校验 → 采样期望时长 → 进入首个启动节点
     *
     * @return false 表示启动失败（单例约束拒绝或启动条件未满足）
     */
    public final boolean begin() {
        if (status != READY) {
            return SUCCESS.equals(status);
        }
        int singletonMode = timing.singletonMode();
        if (timing.timeConsuming() && singletonMode != 0) {
            lockedKey = singletonMode == 3
                    ? defKey + "@type"
                    : defKey + "@" + (host != null ? host.getInstanceId() : "global");
            if (!ctx.tryLockProcess(lockedKey)) {
                status = FAILED;
                failReason = "单实例约束拒绝启动";
                ctx.emitProcessEvent(this, "psf", failReason);
                finishNow();
                return false;
            }
        }
        status = RUNNING;
        startTick = ctx.getTickIndex();
        ctx.onProcessStarted(this);
        if (timing.timeConsuming()) {
            expectedMs = ctx.sampleExpectedDurationMs(timing, processInstanceId);
            precision = Math.max(1L, timing.executionPeriodTicks());
        }
        try {
            if (!canStart()) {
                status = FAILED;
                failReason = "启动条件未满足";
                ctx.emitProcessEvent(this, "psf", failReason);
                finishNow();
                return false;
            }
            if (hasFlow()) {
                stepMachine();
            } else if (syncMode) {
                execLeafBody();
                if (RUNNING.equals(status)) {
                    status = WAIT4SUCCESS;
                }
            }
        } catch (RuntimeException ex) {
            fail(ex);
            return false;
        }
        return true;
    }

    /**
     * 运行阶段（每个步进一次）：推进耗时叶过程与耗时子任务；瞬时节点不在这里执行
     */
    public final void runProcess() {
        if (finished()) {
            return;
        }
        if (WAIT4SUCCESS.equals(status)) {
            // 等待提交阶段：宿主消亡与 R21 维持条件仍须逐拍校验（维持中断贯穿全程生命周期），
            // 通过后提交收尾。修复：begin 阶段即清机的耗时流过程（机器无等待子节点，如仅含
            // 单个瞬时比较节点的受护任务）曾永久停留此阶段——既不收尾、也不做维持校验。
            if (host != null && !host.isAlive()) {
                fail("宿主实例已消亡，过程无法继续执行");
                return;
            }
            try {
                checkMaintain();
            } catch (RuntimeException ex) {
                fail(ex);
                return;
            }
            if (finished()) {
                return;
            }
            if (!children.isEmpty()) {
                for (GromaProcessTask child : List.copyOf(children)) {
                    if (!child.finished()) {
                        child.commit();
                    }
                }
                if (!children.isEmpty()) {
                    collectFinishedChildren();
                }
            }
            if (!finished() && children.isEmpty()) {
                finishSuccess();
            }
            return;
        }
        if (host != null && !host.isAlive()) {
            fail("宿主实例已消亡，过程无法继续执行");
            return;
        }
        // R06 结构控制门点（收割自 code3.html G2 方案，按门点求值落地）：
        // 推进前重算宿主控制掩码，关注的控制位全部失能时按中断级联终止。
        // 求值为宿主枚举状态的纯函数，单写者线程内执行，确定性/存档安全。
        if (host != null && processEnableBits() != 0L) {
            host.refreshControlMask(ctx);
            if (!host.processEnabled(processEnableBits())) {
                abortByControlDisabled();
                return;
            }
        }
        // R21 维持条件逐拍校验（方案 D 收割）：只对运行中的耗时过程检查，中断即级联终止
        if (!hasFlow() || flowEntered) {
            try {
                checkMaintain();
            } catch (RuntimeException ex) {
                fail(ex);
                return;
            }
            if (finished()) {
                return;
            }
        }
        if (!children.isEmpty()) {
            for (GromaProcessTask child : List.copyOf(children)) {
                child.runProcess();
            }
        }
        if (!hasFlow() && RUNNING.equals(status) && stepCount % precision == 0L) {
            try {
                execLeafBody();
            } catch (RuntimeException ex) {
                fail(ex);
                return;
            }
        }
        if (!RUNNING.equals(status)) {
            return;
        }
        stepCount++;
        if (timing.timeConsuming()) {
            elapsedMs += ctx.getFinalStepMs();
            if (!hasFlow() && elapsedMs >= expectedMs) {
                status = WAIT4SUCCESS;
            }
        }
        if (hasFlow() && !deferred.isEmpty()) {
            // 回边按拍推进：上一拍挂起的回边在本拍续跑一圈
            ready.addAll(deferred);
            deferred.clear();
            try {
                stepMachine();
            } catch (RuntimeException ex) {
                fail(ex);
            }
        }
        if (hasFlow() && !timedDeferred.isEmpty()) {
            // 连线延迟按拍递减：到期的后继本拍激活
            List<Integer> due = new ArrayList<>();
            for (var e : timedDeferred.entrySet()) {
                long left = e.getValue() - 1;
                if (left <= 0) {
                    due.add(e.getKey());
                } else {
                    e.setValue(left);
                }
            }
            for (Integer pc : due) {
                timedDeferred.remove(pc);
                activate(pc);
            }
            if (!due.isEmpty() && !ready.isEmpty()) {
                try {
                    stepMachine();
                } catch (RuntimeException ex) {
                    fail(ex);
                }
            }
        }
    }

    /**
     * 提交阶段（每个步进一次）：先提交子任务，再收集已结束的子任务并续接其后继，最后在无子任务等待时收尾
     */
    public final void commit() {
        if (finished()) {
            return;
        }
        if (!children.isEmpty()) {
            for (GromaProcessTask child : List.copyOf(children)) {
                if (!child.finished()) {
                    child.commit();
                }
            }
            if (parent == null) {
                // 存量引擎：顶层过程在子过程提交后立刻派发一轮事件
                ctx.drainPendingEvents();
                if (finished()) {
                    return;
                }
            }
            if (!children.isEmpty()) {
                collectFinishedChildren();
            }
        }
        if (!finished() && children.isEmpty()) {
            if (WAIT4SUCCESS.equals(status)) {
                finishSuccess();
            } else if (FAILED.equals(status) || ABORTED.equals(status)) {
                finishNow();
            }
        }
    }

    private void finishSuccess() {
        if (finished()) {
            return;
        }
        this.status = SUCCESS;
        // R25/R26 出口归类（方案 D 收割）：实际用时超出 TIMING 上下限记 pcd/pca 并派发过程事件。
        // lastEnd 提前收尾不参与归类（实际用时不代表完整执行周期）；未知时长单位安全跳过
        if (!finishedByLastEnd && timing.timeConsuming() && timing.timeMax() != null) {
            try {
                long maxMs = com.groma.base.common.util.TimeUnitUtils.toMillis(timing.timeMax(), timing.timeUnit());
                if (maxMs > 0L && elapsedMs > maxMs) {
                    exitEvent = "pcd";
                    ctx.emitProcessEvent(this, "pcd", "过程超时完成: " + elapsedMs + "ms > " + maxMs + "ms");
                }
            } catch (IllegalArgumentException unknownUnit) {
                // 未知单位：跳过归类（不炸会话）
            }
        }
        if (!finishedByLastEnd && "ok".equals(exitEvent) && timing.timeConsuming() && timing.timeMin() != null) {
            try {
                long minMs = com.groma.base.common.util.TimeUnitUtils.toMillis(timing.timeMin(), timing.timeUnit());
                if (minMs > 0L && elapsedMs < minMs) {
                    exitEvent = "pca";
                    ctx.emitProcessEvent(this, "pca", "过程提前完成: " + elapsedMs + "ms < " + minMs + "ms");
                }
            } catch (IllegalArgumentException unknownUnit) {
                // 未知单位：跳过归类（不炸会话）
            }
        }
        try {
            onSuccess();
            if (completionHook != null) {
                completionHook.run();
            }
        } catch (RuntimeException ex) {
            this.status = FAILED;
            this.failReason = "后置动作失败: " + ex.getMessage();
            GromaWorldContext.LOG.warn("[Process={}] 过程 {} 后置动作执行异常", processInstanceId, processName, ex);
        }
        finishNow();
    }

    private void collectFinishedChildren() {
        List<GromaProcessTask> finishedChildren = new ArrayList<>(children.size());
        for (GromaProcessTask child : children) {
            if (child.finished()) {
                finishedChildren.add(child);
            }
        }
        if (finishedChildren.isEmpty()) {
            return;
        }
        children.removeAll(finishedChildren);
        boolean anySuccess = false;
        for (GromaProcessTask child : finishedChildren) {
            int pc = child.parentPc;
            Integer left = waiting.get(pc);
            if (left == null) {
                continue;
            }
            if (left > 1) {
                waiting.put(pc, left - 1);
                continue;
            }
            waiting.remove(pc);
            nodeOk.put(pc, SUCCESS.equals(child.status));
            childExit.put(pc, classifyExit(child));
            if (SUCCESS.equals(child.status)) {
                anySuccess = true;
                try {
                    nodeDone(pc);
                } catch (RuntimeException ex) {
                    fail(ex);
                    return;
                }
            } else {
                nodeFailed(pc, child);
            }
        }
        if (anySuccess && !finished()) {
            try {
                stepMachine();
            } catch (RuntimeException ex) {
                fail(ex);
            }
        }
    }

    /** 子任务失败后的分支（启动失败/执行失败调用线，画布上无此类连线时为空） */
    protected void nodeFailed(int pc, GromaProcessTask child) {
    }

    private void stepMachine() {
        if (!hasFlow() || finished()) {
            return;
        }
        if (!flowEntered) {
            flowEntered = true;
            for (int entry : entryNodes()) {
                ready.addLast(entry);
            }
        }
        long tick = ctx.getTickIndex();
        if (tick != machineTick) {
            machineTick = tick;
            executedThisTick.clear();
        }
        nodeBudget = ZERO_TIME_NODE_QUOTA;
        int eventMark = ctx.pendingEventCount();
        Integer pc;
        while ((pc = ready.poll()) != null) {
            if (finished()) {
                ready.clear();
                return;
            }
            if (--nodeBudget < 0) {
                // 纯瞬时回边循环：丢弃本次推进期间堆积的事件，只让本过程失败，不拖垮整个会话
                ready.clear();
                ctx.discardEventsAfter(eventMark);
                fail("零时间循环配额熔断：单次推进经过的节点数超过上限 " + ZERO_TIME_NODE_QUOTA);
                return;
            }
            runNode(pc);
        }
        if (RUNNING.equals(status) && waiting.isEmpty() && deferred.isEmpty() && timedDeferred.isEmpty()) {
            status = WAIT4SUCCESS;
        }
    }

    private void runNode(int pc) {
        executedThisTick.add(pc);
        if (!syncMode && timing.timeConsuming() && isSlowToolNode(pc)) {
            // 画布上标记为时间有关的工具节点：其语义体在耗时子任务中执行，成功后再续接后继
            spawnOrRun(pc, new ToolNodeTask(pc));
            return;
        }
        boolean doneNow = execNode(pc);
        if (finished() || !doneNow) {
            return;
        }
        if (nodeOk(pc)) {
            nodeDone(pc);
        } else {
            nodeFailed(pc, null);
        }
    }

    private void finishNow() {
        if (finishNotified) {
            return;
        }
        finishNotified = true;
        if (!SUCCESS.equals(status)) {
            try {
                onFail();
            } catch (RuntimeException ex) {
                GromaWorldContext.LOG.warn("[Process={}] 过程 {} 失败收尾异常", processInstanceId, processName, ex);
            }
            if (RUNNING.equals(status) || WAIT4SUCCESS.equals(status)) {
                status = FAILED;
            }
            ctx.recordProcessProblem(this, "过程未成功结束(" + status + "): " + failReason);
        }
        releaseResources();
        ctx.onProcessFinished(this);
        abortChildren();
    }

    private void abortChildren() {
        if (children.isEmpty()) {
            return;
        }
        for (GromaProcessTask child : List.copyOf(children)) {
            if (!child.finished()) {
                child.status = ABORTED;
                child.failReason = "父过程已结束，级联终止";
                child.finishNow();
            }
        }
        children.clear();
        waiting.clear();
        ready.clear();
        deferred.clear();
    }

    private void releaseResources() {
        if (lockedKey != null) {
            ctx.unlockProcess(lockedKey);
            lockedKey = null;
        }
        if (twinCounted) {
            twinCounted = false;
            ctx.onTwinProcessEnded();
        }
    }

    /**
     * 级联终止：取消全部子任务并置为 ABORTED（R21 维持条件中断、lastEnd 结束点旁路取消都走这里）
     */
    public final void cancelCascade(String reason) {
        if (finished()) {
            return;
        }
        this.status = ABORTED;
        this.failReason = reason == null ? "" : reason;
        finishNow();
    }

    /**
     * 维持条件中断（R21 agent 关系）：由生成代码在维持条件不满足时调用
     */
    protected final void abortByMaintainBroken(String reason) {
        cancelCascade("维持条件中断: " + reason);
    }

    /** 框架在过程启动成功后登记孪生计数（生成代码无需调用） */
    final void markTwinCounted() {
        this.twinCounted = true;
    }

    /**
     * 登记接线上下文对象（触发接线启动时的 context 实体）：顶层过程成功后的输出影响回写（afterN）
     * 写的就是它。存档续跑重建任务时经 {@link #wiringContext()} 导出、由生成代码重挂 afterN 钩子
     * （否则续跑后的在途过程成功时不回写成员——T1.5 分叉根因二：在途任务对齐）。
     */
    public final void setWiringContext(BaseSimEntity context) {
        this.wiringContext = context;
    }

    private volatile BaseSimEntity wiringContext;

    /** 触发接线启动时的 context 实体（手动启动/子过程为 null） */
    public final BaseSimEntity wiringContext() {
        return wiringContext;
    }

    /**
     * 存档续跑：把本任务重新挂回父过程的指定节点（恢复等待表计数），并让父过程把运行帧里的子任务句柄指回来
     */
    final void linkToParent(GromaProcessTask parentTask, int pc) {
        this.parent = parentTask;
        this.parentPc = pc;
        parentTask.children.add(this);
        if (!parentTask.claimRestoredWaiting(pc)) {
            // 父任务恢复现场没有为该节点预留计数（或已认领完）：按新建子任务累加
            parentTask.waiting.merge(pc, 1, Integer::sum);
        }
        this.status = RUNNING;
        this.flowEntered = true;
        parentTask.onChildRestored(pc, this);
    }

    /** 认领父任务恢复现场的等待计数：有未认领余量则扣减并返回 true（防止恢复后 waiting 双计卡死父过程） */
    private boolean claimRestoredWaiting(int pc) {
        Integer left = restoredWaitingUnclaimed.get(pc);
        if (left != null && left > 0) {
            restoredWaitingUnclaimed.put(pc, left - 1);
            return true;
        }
        return false;
    }

    /**
     * 存档续跑：把单实例锁键重新绑定到恢复的任务。锁本身已随 PROCESS_LOCK 关系恢复进锁表，
     * 但 lockedKey 是 begin() 里的运行期字段、不随 exportRuntime 导出——不重绑则恢复的任务
     * 结束时解不掉锁，同类型单例的新实例会永远被“单实例约束拒绝启动”（T1.5 分叉根因三）。
     */
    final void restoreSingletonLock() {
        int singletonMode = timing.singletonMode();
        if (timing.timeConsuming() && singletonMode != 0) {
            lockedKey = singletonMode == 3
                    ? defKey + "@type"
                    : defKey + "@" + (host != null ? host.getInstanceId() : "global");
            ctx.tryLockProcess(lockedKey);
        }
    }

    /**
     * 存档续跑时子任务重建完毕的回调：生成代码把运行帧里对应节点的子过程句柄重新指向 child
     */
    protected void onChildRestored(int pc, GromaProcessTask child) {
    }

    // ------------------------------------------------------------------ 存档

    /**
     * 导出续跑现场：状态机 pc、计时、子任务关联与槽位值
     */
    public final Map<String, Object> exportRuntime() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("defKey", defKey);
        state.put("status", status);
        state.put("failReason", failReason);
        state.put("stepCount", stepCount);
        state.put("elapsedMs", elapsedMs);
        state.put("expectedMs", expectedMs);
        state.put("precision", precision);
        state.put("startTick", startTick);
        state.put("flowEntered", flowEntered);
        state.put("parentPc", parentPc);
        state.put("parentInstanceId", parent == null ? "" : parent.processInstanceId);
        state.put("ready", new ArrayList<>(ready));
        state.put("deferred", new ArrayList<>(deferred));
        state.put("executedThisTick", new ArrayList<>(executedThisTick));
        Map<String, Long> timedExport = new LinkedHashMap<>();
        timedDeferred.forEach((k, v) -> timedExport.put(String.valueOf(k), v));
        state.put("timedDeferred", timedExport);
        Map<String, Integer> waitingExport = new LinkedHashMap<>();
        waiting.forEach((k, v) -> waitingExport.put(String.valueOf(k), v));
        state.put("waiting", waitingExport);
        Map<String, Boolean> okExport = new LinkedHashMap<>();
        nodeOk.forEach((k, v) -> okExport.put(String.valueOf(k), v));
        state.put("nodeOk", okExport);
        Map<String, Object> slots = new LinkedHashMap<>();
        exportSlots(slots);
        state.put("slots", slots);
        return state;
    }

    /**
     * 回填续跑现场（世界上下文在重建全部任务后统一恢复父子关联）
     */
    @SuppressWarnings("unchecked")
    public final void importRuntime(Map<String, Object> state) {
        if (state == null) {
            return;
        }
        this.status = String.valueOf(state.getOrDefault("status", RUNNING));
        this.failReason = String.valueOf(state.getOrDefault("failReason", ""));
        this.stepCount = toLong(state.get("stepCount"));
        this.elapsedMs = toLong(state.get("elapsedMs"));
        this.expectedMs = toLong(state.get("expectedMs"));
        this.precision = Math.max(1L, toLong(state.get("precision")));
        this.startTick = toLong(state.get("startTick"));
        this.flowEntered = Boolean.TRUE.equals(state.get("flowEntered"));
        this.parentPc = (int) toLong(state.get("parentPc"));
        ready.clear();
        Object readyExport = state.get("ready");
        if (readyExport instanceof List<?> list) {
            for (Object item : list) {
                ready.addLast((int) toLong(item));
            }
        }
        deferred.clear();
        Object deferredExport = state.get("deferred");
        if (deferredExport instanceof List<?> list) {
            for (Object item : list) {
                deferred.add((int) toLong(item));
            }
        }
        executedThisTick.clear();
        Object executedExport = state.get("executedThisTick");
        if (executedExport instanceof List<?> list) {
            for (Object item : list) {
                executedThisTick.add((int) toLong(item));
            }
        }
        timedDeferred.clear();
        Object timedExport2 = state.get("timedDeferred");
        if (timedExport2 instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                timedDeferred.put((int) toLong(e.getKey()), toLong(e.getValue()));
            }
        }
        machineTick = ctx.getTickIndex();
        waiting.clear();
        Object waitingExport = state.get("waiting");
        if (waitingExport instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                waiting.put((int) toLong(e.getKey()), (int) toLong(e.getValue()));
            }
        }
        nodeOk.clear();
        Object okExport = state.get("nodeOk");
        if (okExport instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                nodeOk.put((int) toLong(e.getKey()), Boolean.TRUE.equals(e.getValue()));
            }
        }
        // 恢复现场的等待计数视为“已含在途子任务”：子任务 linkToParent 时认领而非累加
        this.runtimeImported = true;
        this.restoredWaitingUnclaimed.clear();
        this.restoredWaitingUnclaimed.putAll(this.waiting);
        Object slots = state.get("slots");
        if (slots instanceof Map<?, ?> map) {
            importSlots((Map<String, Object>) map);
        }
    }

    static long toLong(Object raw) {
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s && !s.isBlank()) {
            return Long.parseLong(s.trim());
        }
        return 0L;
    }

    /**
     * 耗时工具节点的子任务：语义体由所属过程在每拍执行，成功后所属过程续接该节点后继
     */
    private final class ToolNodeTask extends GromaProcessTask {
        private final int ownerPc;

        ToolNodeTask(int ownerPc) {
            super(GromaProcessTask.this.ctx, GromaProcessTask.this.host,
                    GromaProcessTask.this.defKey + "#node" + ownerPc,
                    GromaProcessTask.this.processName + "#" + ownerPc, ONE_STEP);
            this.ownerPc = ownerPc;
        }

        @Override
        protected boolean hasFlow() {
            return false;
        }

        @Override
        protected int[] entryNodes() {
            return new int[0];
        }

        @Override
        protected boolean execNode(int pc) {
            return true;
        }

        @Override
        protected void nodeDone(int pc) {
        }

        @Override
        protected void execLeafBody() {
            GromaProcessTask.this.execNode(ownerPc);
        }
    }
}

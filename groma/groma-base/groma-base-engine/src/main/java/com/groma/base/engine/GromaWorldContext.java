package com.groma.base.engine;

import com.groma.base.common.util.RandomDistUtils;
import com.groma.base.common.util.TimeUnitUtils;
import com.groma.base.engine.archive.ArchiveCheckpointCoordinator;
import com.groma.base.engine.archive.DeterministicRngStream;
import com.groma.base.engine.archive.SimulationArchiveBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * 编译产出模型场景沙盒的通用调度基类：实例注册表、事件接线派发、过程调度、存档续跑都在这里实现一次，
 * 各模型子模块生成的 {Scene}WorldContext 只需提供初始世界构建。
 * <p>
 * 严格单写者：除少量 volatile 控制位外，全部状态只由所属 TickEventLoop 线程访问（落实设计书第七章陷阱 1）。
 * </p>
 */
public abstract class GromaWorldContext extends AbstractWorldSession
        implements ArchiveCheckpointCoordinator.ArchivableWorld {

    static final Logger LOG = LoggerFactory.getLogger(GromaWorldContext.class);

    private static final int MAX_RECENT_EVENTS = 200;
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    private final GromaModuleRuntime runtime;
    private final long seed;
    private final DeterministicRngStream rng;

    // ---- 仅单写者线程访问 ----
    private final Map<String, BaseSimEntity> instancesById = new LinkedHashMap<>();
    private final List<BaseSimEntity> instancesInCreationOrder = new ArrayList<>();
    private final Map<String, String> rootMemberTokens = new LinkedHashMap<>();
    /** 反向引用索引：被引用对象 id -> 成员标识 -> 引用它的对象 id 集合 */
    private final Map<String, Map<String, Set<String>>> referrers = new HashMap<>();
    private final List<GromaProcessTask> topProcesses = new ArrayList<>();
    private final Map<String, GromaProcessTask> processById = new LinkedHashMap<>();
    private final Set<String> processLocks = new HashSet<>();
    private final Deque<WorldSnapshot.SimEventRecord> recentEvents = new ArrayDeque<>();
    private long processSeq;
    private long instanceSeq;
    private String processIdOverride;
    private boolean silent = true;
    private BooleanSupplier stopCondition;
    private BooleanSupplier pauseCondition;
    private volatile GromaTraceListener traceListener;

    protected GromaWorldContext(GromaModuleRuntime runtime, String sessionId, String sceneCode,
                                long startEpochMs, long finalStepMs, long seed) {
        super(sessionId, runtime.modelCode(), runtime.modelVersion(), sceneCode, startEpochMs, finalStepMs, seed);
        this.runtime = runtime;
        this.seed = seed;
        this.rng = new DeterministicRngStream(seed, 0L);
    }

    // ------------------------------------------------------------------ 生成代码必须实现的钩子

    /**
     * 构建初始世界：场景成员与命名实例对象图（静默期，设初值不产生值变化事件）
     */
    protected abstract void buildInitialWorld();

    /**
     * 按实例标识构建一个命名实例（模型“对象实体”目录下的对象）；未知的标识返回 null。
     * 实例内部引用其他命名实例时请调用 {@link #namedInstance(String)}，由框架做记忆化与成环保护。
     */
    protected abstract BaseSimEntity newNamedInstance(String instanceId);

    /**
     * 按实例标识构建一个命名集合实例（模型“对象实体”目录下的数组 / 映射对象）；未知的标识返回 null。
     * 元素引用其他命名实例时同样经 {@link #namedInstance(String)} 取。
     */
    protected abstract Object newNamedCollection(String instanceId);

    public final GromaModuleRuntime modelRuntime() {
        return runtime;
    }

    /** 安装推演追踪监听器（调试适配协议、异步日志使用；传 null 卸载） */
    public final void setTraceListener(GromaTraceListener listener) {
        this.traceListener = listener;
    }

    /** 当前追踪监听器（宿主做监听器组合时读取；调用方需自行保证与 setTraceListener 的写者线程互斥） */
    public final GromaTraceListener traceListener() {
        return traceListener;
    }

    public final long getSeed() {
        return seed;
    }

    // ------------------------------------------------------------------ 实例注册表

    /** 登记一个已构造的实体（幂等） */
    public final boolean register(BaseSimEntity entity) {
        if (entity == null || instancesById.containsKey(entity.getInstanceId())) {
            return false;
        }
        instancesById.put(entity.getInstanceId(), entity);
        entity.openCreation();
        instancesInCreationOrder.add(entity);
        return true;
    }

    /**
     * 运行期生成对象（R12 结果关系）：登记并入队创建事件，创建事件派发时自动启动该对象的默认启动过程
     */
    public final <T extends BaseSimEntity> T spawn(T entity) {
        if (register(entity) && !silent) {
            enqueueMemberEvent("a", entity.getInstanceId(), "", "", entity.getInstanceId(), "created");
            // 被动视角出现事件（方案 C 收割）：不带默认启动语义，供订阅方按事件码区分主动/被动
            enqueueMemberEvent("as", entity.getInstanceId(), "", "", entity.getInstanceId(), "created");
        }
        return entity;
    }

    private final Map<String, BaseSimEntity> namedInstances = new HashMap<>();
    private final Set<String> namedBuilding = new HashSet<>();

    /**
     * 命名实例：首次访问时构建并登记（之后同一标识始终返回同一对象）；构建过程成环时返回 null 并记录问题
     */
    public final BaseSimEntity namedInstance(String instanceId) {
        if (instanceId == null || instanceId.isEmpty()) {
            return null;
        }
        BaseSimEntity known = instancesById.get(instanceId);
        if (known != null) {
            return known;
        }
        BaseSimEntity cached = namedInstances.get(instanceId);
        if (cached != null) {
            return cached;
        }
        if (!namedBuilding.add(instanceId)) {
            problem("命名实例对象图存在循环引用: " + instanceId);
            return null;
        }
        try {
            BaseSimEntity created = newNamedInstance(instanceId);
            if (created == null) {
                problem("未知的命名实例: " + instanceId);
                return null;
            }
            namedInstances.put(instanceId, created);
            spawn(created);
            return created;
        } finally {
            namedBuilding.remove(instanceId);
        }
    }

    private final Map<String, Object> namedCollections = new HashMap<>();

    /**
     * 命名集合实例（数组 / 映射）：首次访问时构建并记忆（之后同一标识始终返回同一对象）；构建过程成环时返回 null 并记录问题
     */
    public final Object namedCollection(String instanceId) {
        if (instanceId == null || instanceId.isEmpty()) {
            return null;
        }
        Object known = namedCollections.get(instanceId);
        if (known != null) {
            return known;
        }
        if (!namedBuilding.add(instanceId)) {
            problem("命名集合实例对象图存在循环引用: " + instanceId);
            return null;
        }
        try {
            Object created = newNamedCollection(instanceId);
            if (created == null) {
                problem("未知的命名集合实例: " + instanceId);
                return null;
            }
            namedCollections.put(instanceId, created);
            return created;
        } finally {
            namedBuilding.remove(instanceId);
        }
    }

    /** 运行期生成对象的实例标识：类型编码 + 递增序号 */
    public final String nextInstanceId(String typeCode) {
        return (typeCode == null || typeCode.isBlank() ? "obj" : typeCode) + "_" + (++instanceSeq);
    }

    public final BaseSimEntity findInstance(String instanceId) {
        return instanceId == null ? null : instancesById.get(instanceId);
    }

    /** 全部实例（登记顺序） */
    public final List<BaseSimEntity> instances() {
        return Collections.unmodifiableList(instancesInCreationOrder);
    }

    /** 按类型编码筛选存活实例（登记顺序） */
    public final List<BaseSimEntity> instancesOfType(String typeCode) {
        List<BaseSimEntity> out = new ArrayList<>(8);
        for (BaseSimEntity e : instancesInCreationOrder) {
            if (e.getTypeCode().equals(typeCode) && e.isAlive()) {
                out.add(e);
            }
        }
        return out;
    }

    public final int instanceCount() {
        return instancesInCreationOrder.size();
    }

    /** 场景直接成员：成员标识 -> 实体 */
    public final Map<String, BaseSimEntity> rootMembers() {
        LinkedHashMap<String, BaseSimEntity> out = new LinkedHashMap<>();
        rootMemberTokens.forEach((token, id) -> {
            BaseSimEntity e = instancesById.get(id);
            if (e != null) {
                out.put(token, e);
            }
        });
        return out;
    }

    /** 声明场景直接成员 */
    protected final void addRootMember(String memberToken, BaseSimEntity entity) {
        register(entity);
        rootMemberTokens.put(memberToken, entity.getInstanceId());
    }

    /** 反向引用查询：通过成员 token 引用 target 的全部对象 id */
    final Set<String> referrersOf(String targetInstanceId, String memberToken) {
        Map<String, Set<String>> byToken = referrers.get(targetInstanceId);
        if (byToken == null) {
            return Set.of();
        }
        Set<String> ids = byToken.get(memberToken);
        return ids == null ? Set.of() : ids;
    }

    private void indexReferrer(BaseSimEntity target, String memberToken, BaseSimEntity owner) {
        referrers.computeIfAbsent(target.getInstanceId(), k -> new HashMap<>())
                .computeIfAbsent(memberToken, k -> new LinkedHashSet<>()).add(owner.getInstanceId());
    }

    private void dropReferrer(BaseSimEntity target, String memberToken, BaseSimEntity owner) {
        Map<String, Set<String>> byToken = referrers.get(target.getInstanceId());
        if (byToken == null) {
            return;
        }
        Set<String> ids = byToken.get(memberToken);
        if (ids == null) {
            return;
        }
        ids.remove(owner.getInstanceId());
    }

    // ------------------------------------------------------------------ 成员事件（生成的 Setter 统一回调）

    /**
     * 成员写入后由生成的 Setter 回调：维护反向引用索引，并按成员种类发出对应事件码
     * （枚举：sx → v → sa；对象引用：a / ra / rv / rx；基础值：v）。
     * 引用事件三选一，与存量引擎一致：从无到有发 ra，从旧对象改指新对象只发 rv，从有到无发 rx。
     */
    public final void onMemberChanged(BaseSimEntity owner, String memberToken, Object oldValue, Object newValue) {
        BaseSimEntity oldRef = oldValue instanceof BaseSimEntity o ? o : null;
        BaseSimEntity newRef = newValue instanceof BaseSimEntity n ? n : null;
        if (oldRef != null && oldRef != newRef) {
            dropReferrer(oldRef, memberToken, owner);
        }
        if (newRef != null) {
            indexReferrer(newRef, memberToken, owner);
        }
        if (newRef != null && newRef.isCreationOpen() && oldRef == null) {
            // 新建对象首次挂到成员上：对象出现事件（携带新建主体）
            fire("a", owner, memberToken, "", newRef.getInstanceId(), "");
        }
        if (silent || owner.isCreationOpen()) {
            // 初始化静默期与对象创建期：设初值不产生值变化事件（标准 10.2.1）
            return;
        }
        if (newRef != null && oldRef == null) {
            // 获得引用（ra）：成员从无指向变为指向某对象（存量引擎 onMemberRefGain）；
            // 被动视角：新目标同步发 ar（被建立指向，code4.html M4 收割，标准 21 类事件全集）
            fire("ra", owner, memberToken, "", "", "");
            firePassiveOnTarget(newRef, "ar", owner);
            return;
        }
        if (newRef != null) {
            // 指向变化（rv）：成员从指向旧对象变为指向另一对象（存量引擎 onMemberRefChanged → VALUE_REF）；
            // 不再同时发 ra；被动视角：旧目标发 xr（被解除指向）、新目标发 vr（引用改向被动）
            fire("rv", owner, memberToken, "", "", "");
            firePassiveOnTarget(oldRef, "xr", owner);
            firePassiveOnTarget(newRef, "vr", owner);
            return;
        }
        if (oldRef != null) {
            // 失去引用（rx）；被动视角：旧目标发 xr（被解除指向）
            fire("rx", owner, memberToken, "", "", "");
            firePassiveOnTarget(oldRef, "xr", owner);
            return;
        }
        boolean enumChange = oldValue instanceof Enum<?> || newValue instanceof Enum<?>;
        if (enumChange) {
            if (oldValue instanceof Enum<?> oldState) {
                fire("sx", owner, memberToken, BaseSimEntity.enumZhLabel(oldState), "", "");
            }
            fire("v", owner, memberToken, "", "", newValue == null ? "" : BaseSimEntity.enumZhLabel((Enum<?>) newValue));
            if (newValue instanceof Enum<?> newState) {
                fire("sa", owner, memberToken, BaseSimEntity.enumZhLabel(newState), "", "");
            }
            return;
        }
        fire("v", owner, memberToken, "", "", newValue == null ? "" : String.valueOf(newValue));
    }

    /** 集合成员增删（R05 集合成员代理：ma 元素加入 / mx 元素移除） */
    public final void onCollectionChanged(BaseSimEntity owner, String memberToken, String eventCode) {
        if (!silent) {
            fire(eventCode, owner, memberToken, "", "", "");
        }
    }

    /** 对象被消耗（R11）：主动 x + 被动 xs 双视角（方案 C 收割） */
    final void onEntityConsumed(BaseSimEntity entity) {
        enqueueObjectEvent("x", entity.getInstanceId(), "对象被消耗");
        enqueueMemberEvent("xs", entity.getInstanceId(), "", "", entity.getInstanceId(), "对象被消耗");
    }

    /**
     * R12 结果关系：成员位置生成了新对象（基础类型成员即重置为新值），发出对象出现事件 a
     */
    public final void onMemberGenerated(BaseSimEntity owner, String memberToken) {
        if (!silent) {
            fire("a", owner, memberToken, "", "", "");
        }
    }

    /** 模型内嵌的外部脚本执行器（画布过程里直接编写的 Python 等脚本被剥离为资源文件后由此执行）；未安装时记录问题 */
    @FunctionalInterface
    public interface ExternalScriptExecutor {
        void run(GromaWorldContext ctx, String scriptName, String processId);
    }

    private volatile ExternalScriptExecutor externalScriptExecutor;

    public final void setExternalScriptExecutor(ExternalScriptExecutor executor) {
        this.externalScriptExecutor = executor;
    }

    /**
     * 执行模型内嵌的外部脚本（已剥离为 resources/scripts/ 下的受控资源文件）
     */
    public final void invokeExternalScript(String scriptName, String processId) {
        ExternalScriptExecutor executor = externalScriptExecutor;
        if (executor == null) {
            problem("过程 " + processId + " 的外部脚本 " + scriptName + " 未执行：宿主未安装外部脚本执行器");
            return;
        }
        executor.run(this, scriptName, processId);
    }

    // ------------------------------------------------------------------ 双视角关系事件辅助（方案 C 收割，R07~R10 关联管理器与集合代理使用）

    /**
     * 被动视角事件派发（code4.html M4 收割，标准 21 类事件全集收口）：
     * 在被指向/被操作目标实体上派发 am/xm/ar/vr/xr，成员令牌固定为类型级通配 "*"——
     * 被动事件的订阅定义在目标类型自身视图（无持有方成员键），
     * GromaTriggerTable.dispatch 对令牌 "*" 精确匹配类型级通配注册、对持有方令牌事件走通配兜底。
     * 目标必须存活（xs 语义由消耗线专用路径处理）；静默期已在调用侧拦截。
     */
    final void firePassiveOnTarget(BaseSimEntity target, String passiveEventCode, BaseSimEntity peer) {
        if (target == null || !target.isAlive()) {
            return;
        }
        fire(passiveEventCode, target, "*", "", peer == null ? "" : peer.getInstanceId(), "被动视角");
    }

    /**
     * 关联建立/重指向双视角事件：主动端发 ra（建立指向）或 rv（变更指向），
     * 被动目标端同步发 ar / vr（收割外部设计稿并适配真实事件总线 fire → enqueueMemberEvent → dispatchSingleEvent）。
     */
    public void emitRelationLinked(String assocId, BaseSimEntity left, String leftMemberId,
                                   BaseSimEntity right, String rightMemberId, boolean retargeted) {
        String activeCode = retargeted ? "rv" : "ra";
        String passiveCode = retargeted ? "vr" : "ar";
        fire(activeCode, left, leftMemberId, "", right.getInstanceId(), assocId);
        fire(passiveCode, right, rightMemberId, "", left.getInstanceId(), assocId);
    }

    /** 关联解除双视角事件：主动端发 rx（解除指向），被动目标端同步发 xr（被解除指向） */
    public void emitRelationUnlinked(String assocId, BaseSimEntity left, String leftMemberId,
                                     BaseSimEntity right, String rightMemberId) {
        fire("rx", left, leftMemberId, "", right.getInstanceId(), assocId);
        fire("xr", right, rightMemberId, "", left.getInstanceId(), assocId);
    }

    /**
     * 集合元素增删双视角事件：持有方发 ma/mx，被操作元素实体同步发被动事件 am（加入集合）/ xm（移出集合）。
     * 元素不是实体时仅发持有方视角。
     */
    public void emitCollectionElementAdded(BaseSimEntity owner, String collectionMemberId, Object element) {
        String elemId = (element instanceof BaseSimEntity se) ? se.getInstanceId() : "";
        fire("ma", owner, collectionMemberId, "", elemId, "");
        if (element instanceof BaseSimEntity elemEntity) {
            fire("am", elemEntity, collectionMemberId, "", owner.getInstanceId(), "");
        }
    }

    public void emitCollectionElementRemoved(BaseSimEntity owner, String collectionMemberId, Object element) {
        String elemId = (element instanceof BaseSimEntity se) ? se.getInstanceId() : "";
        fire("mx", owner, collectionMemberId, "", elemId, "");
        if (element instanceof BaseSimEntity elemEntity) {
            fire("xm", elemEntity, collectionMemberId, "", owner.getInstanceId(), "");
        }
    }

    /**
     * 集合成员的增删事件汇（方案 C 收割，供生成实体把集合字段接到双视角事件总线）：
     * 汇收到 ObservableEntityList 的 "ma"/"mx" 元素操作码，转调双视角派发（持有方 ma/mx + 元素 am/xm）。
     */
    public final <T> java.util.function.BiConsumer<String, T> collectionSink(BaseSimEntity owner,
                                                                             String collectionMemberId) {
        return collectionSinkWithProxies(owner, collectionMemberId, java.util.List.of());
    }

    /**
     * 集合成员的增删事件汇（R05 集合代理消费，方案 C 收割）：除集合成员自身的双视角事件外，
     * 对代理该集合的 PROXY 定义（proxyMemberIds = 代理图元实体 id 列表）同步派发同码事件——
     * 事件源仍是持有方、成员标识换成代理 id，使画布上经由代理图元引用集合元素的订阅方可被触发。
     */
    public final <T> java.util.function.BiConsumer<String, T> collectionSinkWithProxies(
            BaseSimEntity owner, String collectionMemberId, java.util.List<String> proxyMemberIds) {
        return (code, element) -> {
            if ("ma".equals(code)) {
                emitCollectionElementAdded(owner, collectionMemberId, element);
                for (String proxyId : proxyMemberIds) {
                    emitCollectionElementAdded(owner, proxyId, element);
                }
            } else if ("mx".equals(code)) {
                emitCollectionElementRemoved(owner, collectionMemberId, element);
                for (String proxyId : proxyMemberIds) {
                    emitCollectionElementRemoved(owner, proxyId, element);
                }
            }
        };
    }

    /**
     * 过程级四类异常与边界事件统一派发入口（psf 启动失败 / paa 维持中断 / pca 提前完成 / pcd 超时完成）。
     */
    public void emitProcessEvent(GromaProcessTask task, String eventCode, String detail) {
        BaseSimEntity host = task.hostEntity();
        if (host == null) {
            // 过程事件以宿主为事件源挂入总线；独立过程（无宿主）无处挂靠，记追踪问题即止
            problem("过程 " + task.getProcessInstanceId() + "（无宿主）发生 " + eventCode + ": " + detail);
            return;
        }
        fire(eventCode, host, task.getDefKey(), "", task.getProcessInstanceId(), detail);
    }

    /** 带输入输出的外部脚本执行器（Python 算法双向数据桥）：inputs 以 JSON 传入脚本标准输入，返回值取自标准输出 JSON */
    public interface ExternalScriptIoExecutor {
        java.util.Map<String, Object> run(GromaWorldContext ctx, String scriptName, String processId,
                                          java.util.Map<String, Object> inputs);
    }

    private volatile ExternalScriptIoExecutor externalScriptIoExecutor;

    public final void setExternalScriptIoExecutor(ExternalScriptIoExecutor executor) {
        this.externalScriptIoExecutor = executor;
    }

    /**
     * 带输入输出的受控脚本调用（修复 Python 剥离后"只调空壳、丢弃入参与返回值"断层）：
     * IN/IN_OUT 形参打包为 inputs；返回的映射由生成代码按中文形参名回写 OUT/IN_OUT 槽位。
     * 宿主只装了旧式执行器时退化为无参调用并记录问题。
     */
    public final java.util.Map<String, Object> invokeExternalScriptWithIo(String scriptName, String processId,
                                                                          java.util.Map<String, Object> inputs) {
        ExternalScriptIoExecutor ioExecutor = externalScriptIoExecutor;
        if (ioExecutor != null) {
            try {
                java.util.Map<String, Object> out = ioExecutor.run(this, scriptName, processId,
                        inputs == null ? Map.of() : inputs);
                return out == null ? Map.of() : out;
            } catch (RuntimeException ex) {
                problem("过程 " + processId + " 的外部脚本 " + scriptName + " 执行失败: " + ex.getMessage());
                return Map.of();
            }
        }
        invokeExternalScript(scriptName, processId);
        return Map.of();
    }

    /** 记录一条世界级问题（接线路径缺失、外部干预失败等） */
    public final void recordPublicProblem(String message) {
        problem(message);
    }

    /** 记录世界级问题并通知追踪监听器 */
    private void problem(String message) {
        recordProblem(message);
        GromaTraceListener listener = traceListener;
        if (listener != null) {
            listener.onProblem(this, tickIndex, message);
        }
    }

    private void fire(String code, BaseSimEntity owner, String memberToken, String stateLabel, String subjectId, String detail) {
        enqueueMemberEvent(code, owner.getInstanceId(), memberToken, stateLabel, subjectId, detail);
    }

    @Override
    protected final void dispatchSingleEvent(WorldSnapshot.SimEventRecord event) {
        recentEvents.addLast(event);
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.removeFirst();
        }
        GromaTraceListener trace = traceListener;
        if (trace != null) {
            trace.onEvent(this, tickIndex, silent, event);
        }
        BaseSimEntity source = instancesById.get(event.sourceId());
        if (source == null) {
            return;
        }
        if ("a".equals(event.eventCode())) {
            BaseSimEntity subject = event.subjectId().isEmpty() ? null : instancesById.get(event.subjectId());
            if (subject != null && subject.isCreationOpen()) {
                // 创建事件：先启动新对象的默认启动过程，再派发挂在成员上的对象出现触发器（与存量引擎顺序一致）
                subject.closeCreation();
                if (!silent) {
                    runtime.triggerTable().fireDefaultStarts(this, subject);
                }
            }
            if (subject == source) {
                return;
            }
        }
        // x 与 xs（被动消亡视角）的源实体在派发时必然已消亡，同样豁免存活守卫
        if (!source.isAlive() && !"x".equals(event.eventCode()) && !"xs".equals(event.eventCode())) {
            return;
        }
        runtime.triggerTable().dispatch(this, source, event);
    }

    // ------------------------------------------------------------------ 过程调度

    /**
     * 启动一个顶层过程（瞬时过程同步跑完，耗时过程挂入步进队列），返回该过程任务
     */
    public final GromaProcessTask launch(GromaProcessTask task) {
        topProcesses.add(task);
        boolean started = task.startTopLevel();
        if (!started || task.finished()) {
            topProcesses.remove(task);
        }
        return task;
    }

    final String nextProcessInstanceId() {
        if (processIdOverride != null) {
            String explicit = processIdOverride;
            processIdOverride = null;
            return explicit;
        }
        return "p" + (++processSeq);
    }

    final boolean tryLockProcess(String lockKey) {
        return processLocks.add(lockKey);
    }

    final void unlockProcess(String lockKey) {
        processLocks.remove(lockKey);
    }

    final void onProcessStarted(GromaProcessTask task) {
        processById.put(task.getProcessInstanceId(), task);
        GromaTraceListener trace = traceListener;
        if (trace != null) {
            trace.onProcessStarted(this, task);
        }
        if (task.isTimeRelated() && task.timing().twinLocked()) {
            // R2 孪生过程：运行期强制锁 1x 倍速，全部结束后恢复期望倍速
            onTwinProcessStarted();
            task.markTwinCounted();
        }
        enqueueObjectEvent("pr", task.getProcessInstanceId(), task.getProcessName());
    }

    final void onProcessFinished(GromaProcessTask task) {
        topProcesses.remove(task);
        processById.remove(task.getProcessInstanceId());
        GromaTraceListener trace = traceListener;
        if (trace != null) {
            trace.onProcessFinished(this, task);
        }
    }

    final void recordProcessProblem(GromaProcessTask task, String message) {
        problem("[过程 " + task.getProcessInstanceId() + " " + task.getProcessName() + "] " + message);
    }

    /** 全部在途过程任务（顶层与其子孙，启动顺序） */
    public final List<GromaProcessTask> activeProcesses() {
        return List.copyOf(processById.values());
    }

    public final int activeProcessCount() {
        return processById.size();
    }

    /** 按过程实例标识查找在途过程 */
    public final GromaProcessTask findProcess(String processInstanceId) {
        return processById.get(processInstanceId);
    }

    /** 按任务级种子派生的随机流采样期望时长（均匀 / 正态 / 指数 / 三角分布，缺省取固定时长） */
    final long sampleExpectedDurationMs(GromaProcessTask.Timing timing, String processInstanceId) {
        long stepMs = getFinalStepMs();
        double normal = timing.timeNormal();
        String type = timing.durationParamType() == null ? "STEP" : timing.durationParamType().trim();
        long base;
        if ("FIXED".equalsIgnoreCase(type)) {
            base = normal > 0.0D ? TimeUnitUtils.toMillis(normal, timing.timeUnit()) : stepMs;
        } else {
            base = normal > 0.0D ? Math.max(1L, StrictMath.round(normal * stepMs)) : stepMs;
        }
        String dist = timing.distribution() == null ? "" : timing.distribution().trim();
        if (dist.isEmpty()) {
            return base;
        }
        double unitMs = TimeUnitUtils.toMillis(1.0D, timing.timeUnit());
        Double min = timing.distMin();
        Double max = timing.distMax();
        Double avg = timing.distAvg();
        Double sd = timing.distSd();
        Double param = timing.distParam();
        Double sampled = switch (dist) {
            case "uniform" -> (min != null && max != null && max > min)
                    ? RandomDistUtils.uniform(rng::nextDouble, min * unitMs, max * unitMs) : null;
            case "normal" -> (avg != null && sd != null && sd > 0.0D)
                    ? RandomDistUtils.normal(rng::nextGaussian, avg * unitMs, sd * unitMs) : null;
            case "exponential" -> (param != null && param > 0.0D)
                    ? RandomDistUtils.exponential(rng::nextDouble, param * unitMs) : null;
            case "triangular" -> (min != null && max != null && param != null && max > min)
                    ? RandomDistUtils.triangular(rng::nextDouble, min * unitMs, param * unitMs, max * unitMs) : null;
            default -> null;
        };
        if (sampled == null) {
            problem("过程 " + processInstanceId + " 的时长分布 " + dist + " 参数不完整，已回退为固定时长");
            return base;
        }
        return Math.max(1L, StrictMath.round(sampled));
    }

    /** 确定性洗牌（任务级随机流派生，保证同种子多次推演与存档续跑逐帧一致） */
    public final <T> void shuffle(List<T> items) {
        for (int i = items.size(); i > 1; i--) {
            int j = (int) Math.floor(rng.nextDouble() * i);
            T tmp = items.get(i - 1);
            items.set(i - 1, items.get(j));
            items.set(j, tmp);
        }
    }

    /** 任务级确定性随机流（算子里的随机数工具、分组概率采样统一走这里） */
    public final DeterministicRngStream randomStream() {
        return rng;
    }

    public final void drainPendingEvents() {
        drainPendingEventsNow();
    }

    // ------------------------------------------------------------------ 步进钩子

    @Override
    protected final void initEntitiesAndDefaultStarts() {
        this.silent = true;
        buildInitialWorld();
        // 想定覆盖参数（修复 initialOverrides 悬空缺陷）：初始世界构建完成后、仍在静默期内应用，
        // 覆盖初值不产生值变化事件。键语义：`实例ID.成员名` 定点覆盖；`成员名` 覆盖场景根对象成员。
        applyInitialOverrides();
        this.silent = false;
        // 起步准备段：默认启动算子立即同步执行；默认启动耗时过程挂入第 0 步队列（初始化期不产生值变化事件）
        for (BaseSimEntity entity : List.copyOf(instancesInCreationOrder)) {
            if (entity.isCreationOpen()) {
                entity.closeCreation();
                runtime.triggerTable().fireDefaultStarts(this, entity);
            }
        }
    }

    // ------------------------------------------------------------------ 想定覆盖参数

    private Map<String, Object> initialOverrides = Map.of();

    /** 由生成的场景 WorldContext 构造器调用：登记启动覆盖参数（构造期登记，初始化完成后统一应用） */
    public final void setInitialOverrides(Map<String, Object> overrides) {
        this.initialOverrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    /** 场景根对象（生成子类返回 this.self；基类默认无）——裸成员名覆盖键的作用目标 */
    protected BaseSimEntity sceneRootEntity() {
        return null;
    }

    /**
     * 应用想定覆盖：仍处静默期（初始化期），写入失败记录为问题不中断启动。
     * 数值做 Integer→Double、字符串→数值 的尽力转换，兼容 JSON 反序列化类型差异。
     */
    private void applyInitialOverrides() {
        if (initialOverrides.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : initialOverrides.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                String error;
                int dot = key.indexOf('.');
                if (dot > 0) {
                    error = applyOverrideValue(key.substring(0, dot), key.substring(dot + 1), value);
                } else if (sceneRootEntity() != null) {
                    error = applyOverrideValue(sceneRootEntity().getInstanceId(), key, value);
                } else {
                    error = "场景根对象未就绪，裸成员名覆盖键无法定位实例";
                }
                if (!error.isEmpty()) {
                    recordProblem("[OVERRIDE] " + key + ": " + error);
                }
            } catch (RuntimeException ex) {
                recordProblem("[OVERRIDE] " + key + ": " + ex.getMessage());
            }
        }
    }

    private String applyOverrideValue(String instanceId, String memberName, Object value) {
        String error = setInstanceValue(instanceId, memberName, value);
        if (!error.isEmpty() && value != null) {
            // 尽力类型转换后重试一次（JSON 反序列化常见 Integer/String 与字段 double 不匹配）
            Object coerced = coerceNumber(value);
            if (coerced != value) {
                error = setInstanceValue(instanceId, memberName, coerced);
            }
        }
        return error;
    }

    private static Object coerceNumber(Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Float) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            try {
                if (trimmed.matches("-?\\d+\\.\\d+[Ee]?\\d*")) {
                    return Double.parseDouble(trimmed);
                }
                if (trimmed.matches("-?\\d+")) {
                    return Double.parseDouble(trimmed);
                }
                if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                    return Boolean.parseBoolean(trimmed);
                }
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        return value;
    }

    @Override
    protected final void loadArchiveState(WorldSnapshot archiveSnap) {
        // 存档续跑统一走 ArchivableWorld 接口（见 restoreFromArchive），这里无需实现
    }

    @Override
    protected final void executeActiveProcessesAtTick(long tick, long worldTimeMs) {
        if (topProcesses.isEmpty()) {
            return;
        }
        List<GromaProcessTask> batch = new ArrayList<>(topProcesses);
        if (batch.size() > 1) {
            shuffle(batch);
        }
        for (GromaProcessTask task : batch) {
            if (!task.finished()) {
                task.runProcess();
            }
        }
    }

    @Override
    protected final void commitCompletedProcesses() {
        if (topProcesses.isEmpty()) {
            return;
        }
        for (GromaProcessTask task : new ArrayList<>(topProcesses)) {
            task.commit();
        }
    }

    @Override
    protected void sweepIdleInstancesAtSafePoint(long tick) {
        // 存量 BE-INS-003：非根、无引用且已消亡的实例在安全点回收
        if (tick % 64L != 0L) {
            return;
        }
        Set<String> referenced = new HashSet<>();
        for (Map<String, Set<String>> byToken : referrers.values()) {
            for (Set<String> ids : byToken.values()) {
                referenced.addAll(ids);
            }
        }
        List<String> removed = new ArrayList<>();
        for (BaseSimEntity entity : instancesInCreationOrder) {
            boolean root = rootMemberTokens.containsValue(entity.getInstanceId());
            if (!root && !entity.isAlive() && !referenced.contains(entity.getInstanceId())) {
                removed.add(entity.getInstanceId());
            }
        }
        for (String id : removed) {
            instancesById.remove(id);
            referrers.remove(id);
            instancesInCreationOrder.removeIf(e -> e.getInstanceId().equals(id));
        }
    }

    @Override
    protected final boolean evaluatePersistentStopCondition() {
        return stopCondition != null && stopCondition.getAsBoolean();
    }

    @Override
    protected final boolean evaluateOneShotPauseCondition() {
        return pauseCondition != null && pauseCondition.getAsBoolean();
    }

    /** 设置持续终止条件（在单写者线程求值，为真时推演正常结束） */
    public final void setStopCondition(BooleanSupplier condition) {
        this.stopCondition = condition;
    }

    /** 设置一次性暂停条件（命中一次后自动失效） */
    public final void setOneShotPauseCondition(BooleanSupplier condition) {
        BooleanSupplier inner = condition;
        this.pauseCondition = new BooleanSupplier() {
            private boolean consumed;

            @Override
            public boolean getAsBoolean() {
                if (consumed) {
                    return false;
                }
                if (inner.getAsBoolean()) {
                    consumed = true;
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    protected final WorldSnapshot buildImmutableSnapshot() {
        Map<String, Map<String, Object>> instMap = new LinkedHashMap<>(instancesInCreationOrder.size() * 2);
        for (BaseSimEntity entity : instancesInCreationOrder) {
            instMap.put(entity.getInstanceId(), entity.toSnapshotMap());
        }
        List<WorldSnapshot.ProcessSnapItem> procItems = new ArrayList<>(processById.size());
        for (GromaProcessTask task : processById.values()) {
            procItems.add(new WorldSnapshot.ProcessSnapItem(
                    task.getProcessInstanceId(),
                    task.getProcessName(),
                    task.getStatus(),
                    task.getElapsedMs(),
                    task.getExpectedMs(),
                    task.getDefKey(),
                    task.host() == null ? "" : task.host().getInstanceId()));
        }
        return new WorldSnapshot(
                sessionId, modelCode, modelVersion, sceneCode, seed, tickIndex, currentWorldTimeMs(), effectiveSpeed(),
                getLifecycleStatus(), Map.copyOf(instMap), List.copyOf(procItems), List.copyOf(recentEvents), copyProblems());
    }

    // ------------------------------------------------------------------ 世界时钟（供“仿真时间”内置对象读取）

    /** 仿真世界当前时间日期文本（yyyy-MM-dd HH:mm:ss） */
    public final String worldDateTime() {
        return DATETIME_FORMAT.format(Instant.ofEpochMilli(currentWorldTimeMs()));
    }

    /** 仿真开始以来持续运行的秒数（整数刻度：tickIndex * stepMs） */
    public final double elapsedSeconds() {
        return Math.max(0L, tickIndex) * getFinalStepMs() / 1000.0D;
    }

    // ------------------------------------------------------------------ 外部干预（必须经请求桥在单写者线程执行）

    /**
     * 按成员中文名 / 英文成员名 / 成员标识写入实例成员值
     *
     * @return 成功返回空串，失败返回原因
     */
    public final String setInstanceValue(String instanceId, String memberNameOrToken, Object newValue) {
        BaseSimEntity entity = instancesById.get(instanceId);
        if (entity == null) {
            return "实例不存在: " + instanceId;
        }
        GromaMemberMeta meta = entity.findMember(memberNameOrToken);
        if (meta == null) {
            return "成员不存在: " + memberNameOrToken;
        }
        boolean ok;
        try {
            ok = entity.setMemberValue(meta.token(), newValue, this);
        } catch (RuntimeException ex) {
            return "写入失败: " + ex.getMessage();
        }
        return ok ? "" : "成员值类型不匹配: " + meta.typeName();
    }

    // ------------------------------------------------------------------ 存档续跑（ArchivableWorld）

    /**
     * 从存档包无损续跑（单写者线程调用）
     */
    public final void restoreFromArchive(SimulationArchiveBundle bundle) {
        new ArchiveCheckpointCoordinator().restoreIntoWorld(bundle, this);
        completeRestore(bundle.tickIndex());
    }

    @Override
    public long currentTickIndex() {
        return tickIndex;
    }

    @Override
    public SimulationArchiveBundle.TaskArchivePart exportTaskHeader(String archiveId, long tickIndex) {
        return new SimulationArchiveBundle.TaskArchivePart(
                archiveId, tickIndex, modelCode, modelVersion, sceneCode, startEpochMs, finalStepMs, seed,
                rng.getDrawIndex(), getRequestedSpeed(), instanceSeq);
    }

    @Override
    public List<SimulationArchiveBundle.InstanceArchivePart> exportInstances(String archiveId, long tickIndex) {
        List<SimulationArchiveBundle.InstanceArchivePart> out = new ArrayList<>(instancesInCreationOrder.size());
        for (BaseSimEntity entity : instancesInCreationOrder) {
            out.add(new SimulationArchiveBundle.InstanceArchivePart(
                    archiveId, tickIndex, entity.getInstanceId(), entity.getTypeCode(), entity.isAlive(), "",
                    entity.exportState()));
        }
        return out;
    }

    @Override
    public List<SimulationArchiveBundle.ProcessArchivePart> exportProcesses(String archiveId, long tickIndex) {
        List<SimulationArchiveBundle.ProcessArchivePart> out = new ArrayList<>(processById.size());
        for (GromaProcessTask task : processById.values()) {
            out.add(new SimulationArchiveBundle.ProcessArchivePart(
                    archiveId, tickIndex, task.getProcessInstanceId(), task.getDefKey(),
                    task.host() == null ? "" : task.host().getInstanceId(), task.getStatus(),
                    task.getStepCount(), task.getElapsedMs(), task.getExpectedMs(), task.getParentPc(), task.exportRuntime(),
                    task.wiringContext() == null ? "" : task.wiringContext().getInstanceId()));
        }
        return out;
    }

    @Override
    public List<SimulationArchiveBundle.RelationLinkArchivePart> exportRelations(String archiveId, long tickIndex) {
        List<SimulationArchiveBundle.RelationLinkArchivePart> out = new ArrayList<>(processLocks.size());
        for (String lock : processLocks) {
            out.add(new SimulationArchiveBundle.RelationLinkArchivePart(archiveId, tickIndex, "PROCESS_LOCK", lock, ""));
        }
        return out;
    }

    @Override
    public void enterArchiveLoadingPhase() {
        assertSingleWriter();
        this.silent = true;
        instancesById.clear();
        instancesInCreationOrder.clear();
        namedInstances.clear();
        namedCollections.clear();
        namedBuilding.clear();
        referrers.clear();
        topProcesses.clear();
        processById.clear();
        processLocks.clear();
        recentEvents.clear();
        // 待派发事件队列一并清空：恢复前初始化入队的过期事件不得在续跑首拍派发（T1.5 分叉根因一，事件重放）
        clearPendingEventsForArchiveRestore();
    }

    @Override
    public void restoreTaskHeader(SimulationArchiveBundle.TaskArchivePart header) {
        requestSpeed(header.requestedSpeed());
        rng.setCursor(Math.max(0L, header.rngDrawCursor()));
        processSeq = 0L;
        instanceSeq = 0L;
        // 墙钟基准随存档恢复：时间日期成员（世界时间）必须与中断前一致，否则时序类比较全部错位
        restoreWorldClock(header.startEpochMs());
        // 全局单调实例分配计数器随存档恢复（修复中文/UUID 命名实例不推进序号导致新实例撞号分叉）：
        // 与 restoreInstances 的实例 id 数字后缀扫描取最大值双保险（旧 v1/v2 存档无游标缺省 0，不改变行为）
        instanceSeq = Math.max(instanceSeq, header.instanceSeqCursor());
    }

    @Override
    public void restoreInstances(List<SimulationArchiveBundle.InstanceArchivePart> instances) {
        // 第一遍：按原实例标识重建全部空对象
        List<SimulationArchiveBundle.InstanceArchivePart> restored = new ArrayList<>(instances.size());
        for (SimulationArchiveBundle.InstanceArchivePart part : instances) {
            BaseSimEntity entity = runtime.newEntity(part.typeCode(), part.instanceId());
            if (entity == null) {
                problem("存档实例类型不存在: " + part.typeCode());
                continue;
            }
            register(entity);
            entity.closeCreation();
            restored.add(part);
            int sep = part.instanceId().lastIndexOf('_');
            if (sep > 0 && sep < part.instanceId().length() - 1) {
                try {
                    instanceSeq = Math.max(instanceSeq, Long.parseLong(part.instanceId().substring(sep + 1)));
                } catch (NumberFormatException ignored) {
                    // 命名实例标识不是“类型_序号”形态，无需推进序号
                }
            }
        }
        // 第二遍：回填成员值（此时全部对象已存在，对象引用可直接解析）
        for (SimulationArchiveBundle.InstanceArchivePart part : restored) {
            BaseSimEntity entity = instancesById.get(part.instanceId());
            entity.importState(part.fieldValues(), this);
            if (!part.alive()) {
                entity.markDead();
            }
        }
    }

    @Override
    public void restoreRelations(List<SimulationArchiveBundle.RelationLinkArchivePart> relations) {
        for (SimulationArchiveBundle.RelationLinkArchivePart rel : relations) {
            if ("PROCESS_LOCK".equals(rel.relationType())) {
                processLocks.add(rel.leftInstanceId());
            }
        }
    }

    @Override
    public void restoreProcesses(List<SimulationArchiveBundle.ProcessArchivePart> processes) {
        Map<String, GromaProcessTask> byId = new LinkedHashMap<>();
        Map<String, String> parentIds = new HashMap<>();
        for (SimulationArchiveBundle.ProcessArchivePart part : processes) {
            BaseSimEntity host = part.hostInstanceId().isEmpty() ? null : instancesById.get(part.hostInstanceId());
            processIdOverride = part.processInstanceId();
            GromaProcessTask task;
            try {
                task = runtime.newProcess(part.processDefId(), this, host, Map.of());
            } catch (RuntimeException rebuildFailure) {
                // 宿主实例可能已在存档前被安全点回收（生成的构造器 requireNonNull 抛出）：
                // 该过程无法重建，记问题跳过而不是让整个恢复中断
                task = null;
                problem("存档过程 " + part.processInstanceId() + " 重建失败: " + rebuildFailure.getMessage());
            } finally {
                processIdOverride = null;
            }
            if (task == null) {
                continue;
            }
            Object parentRaw = part.runtimeState() == null ? null : part.runtimeState().get("parentInstanceId");
            String parentInstanceId = parentRaw == null ? "" : String.valueOf(parentRaw);
            task.importRuntime(part.runtimeState());
            // 单实例锁重绑：恢复的任务结束时必须能解掉自己持有的锁（T1.5 分叉根因三）
            task.restoreSingletonLock();
            // 接线上下文随存档恢复：顶层任务重挂输出影响回写钩子（生成的 afterN 是接线侧闭包，
            // 不重挂则续跑后的过程成功不回写成员——T1.5 分叉根因二「在途任务对齐」）
            if (!part.wiringContextInstanceId().isEmpty()) {
                BaseSimEntity wiringContext = instancesById.get(part.wiringContextInstanceId());
                if (wiringContext != null) {
                    task.setWiringContext(wiringContext);
                    runtime.reattachWiringHook(part.processDefId(), this, wiringContext, host, task);
                } else {
                    problem("存档过程的接线上下文实例不存在: " + part.wiringContextInstanceId());
                }
            }
            byId.put(task.getProcessInstanceId(), task);
            processById.put(task.getProcessInstanceId(), task);
            parentIds.put(task.getProcessInstanceId(), parentInstanceId);
            // 过程实例序号随存档推进：续跑后新建过程的实例标识不得与恢复的任务撞名
            advanceProcessSeq(part.processInstanceId());
            if (task.isTimeRelated() && task.timing().twinLocked()) {
                onTwinProcessStarted();
                task.markTwinCounted();
            }
        }
        for (GromaProcessTask task : byId.values()) {
            String parentId = parentIds.get(task.getProcessInstanceId());
            if (parentId.isEmpty()) {
                topProcesses.add(task);
            } else {
                GromaProcessTask parent = byId.get(parentId);
                if (parent == null) {
                    problem("存档过程的父过程缺失: " + parentId);
                    topProcesses.add(task);
                } else {
                    task.linkToParent(parent, task.getParentPc());
                }
            }
        }
    }

    /** 按恢复的过程实例标识（“p<序号>”形态）推进过程实例序号游标，避免续跑新建过程撞名 */
    private void advanceProcessSeq(String processInstanceId) {
        if (processInstanceId != null && processInstanceId.startsWith("p")) {
            try {
                processSeq = Math.max(processSeq, Long.parseLong(processInstanceId.substring(1)));
            } catch (NumberFormatException ignored) {
                // 非序号形态的过程实例标识，无需推进
            }
        }
    }

    @Override
    public void exitArchiveLoadingPhaseAndEnableEvents() {
        this.silent = false;
    }
}

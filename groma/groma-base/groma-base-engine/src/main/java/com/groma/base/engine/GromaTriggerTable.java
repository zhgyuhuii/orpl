package com.groma.base.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模型级事件接线表（落实设计书第五章 R23 “对象事件触发直接编译进 WorldContext 事件接线逻辑”与 3.2 节三级启动链）
 * <p>
 * 编译产出代码在模型装载时把画布上每一条“成员/状态 --事件码+线上判断--> 过程调用”连线登记为一条触发器，
 * 没有被订阅的事件零代码、零运行期开销。事件沿对象聚合关系向上冒泡：成员实例上发生的事件，
 * 同样会命中聚合它的上层对象类型视图里登记的触发器（与存量引擎 fireOperatorByChangeInHost 的语义一致）。
 * </p>
 */
public final class GromaTriggerTable {

    /** 线上判断（以接线所属对象为上下文求值） */
    @FunctionalInterface
    public interface Judge {
        boolean test(GromaWorldContext ctx, BaseSimEntity source);
    }

    /**
     * 默认启动项
     *
     * @param typeCode 对象类型编码
     * @param hostPath 从新建对象到宿主对象的成员路径（空 = 宿主就是新建对象自身）
     * @param starter  过程启动器
     */
    public record DefaultStart(String typeCode, List<String> hostPath, GromaProcessStarter starter, String targetName) {
        public DefaultStart(String typeCode, List<String> hostPath, GromaProcessStarter starter) {
            this(typeCode, hostPath, starter, "");
        }
    }

    private record Trigger(
            String memberToken,
            String eventCode,
            String stateLabel,
            Judge judge,
            List<String> contextPath,
            List<String> hostPath,
            GromaProcessStarter starter,
            String targetName
    ) {}

    private final Map<String, List<Trigger>> index = new HashMap<>();
    private final Map<String, List<DefaultStart>> defaultStarts = new HashMap<>();
    private int triggerCount;

    /**
     * 登记一条事件触发连线
     *
     * @param sourceTypeCode 事件源对象（成员所属对象）的类型编码
     * @param memberToken    发生事件的成员标识
     * @param eventCode      事件码：a 对象出现 / v 值变化 / sa 状态进入 / sx 状态退出 / ra,rv,rx 引用变化
     * @param stateLabel     sa/sx 事件限定的状态中文原名（其余事件传空串）
     * @param judge          线上判断（以接线所属对象为上下文求值，可空）
     * @param contextPath    从接线所属对象到事件源对象的成员路径；源对象就是接线对象时传空列表
     * @param hostPath       从接线所属对象到目标过程宿主的成员路径；宿主即接线对象时传空列表
     * @param starter        目标过程启动器
     */
    public void onEvent(String sourceTypeCode, String memberToken, String eventCode, String stateLabel,
                        Judge judge, List<String> contextPath, List<String> hostPath, GromaProcessStarter starter) {
        onEvent(sourceTypeCode, memberToken, eventCode, stateLabel, judge, contextPath, hostPath, starter, "");
    }

    /**
     * 登记一条事件触发连线（带目标过程名，供运行期追踪输出）
     *
     * @param targetName 目标过程的中文名（仅用于追踪与调试展示）
     */
    public void onEvent(String sourceTypeCode, String memberToken, String eventCode, String stateLabel,
                        Judge judge, List<String> contextPath, List<String> hostPath, GromaProcessStarter starter,
                        String targetName) {
        index.computeIfAbsent(sourceTypeCode + "|" + memberToken + "|" + eventCode, k -> new ArrayList<>(4))
                .add(new Trigger(memberToken, eventCode, stateLabel == null ? "" : stateLabel, judge,
                        contextPath == null ? List.of() : List.copyOf(contextPath),
                        hostPath == null ? List.of() : List.copyOf(hostPath), starter,
                        targetName == null ? "" : targetName));
        triggerCount++;
    }

    /**
     * 登记一条默认启动（对象创建时自动运行的过程）
     *
     * @param typeCode 新建对象的类型编码
     * @param hostPath 从新建对象到过程宿主的成员路径（宿主即新建对象时传空列表）
     */
    public void onDefaultStart(String typeCode, List<String> hostPath, GromaProcessStarter starter) {
        onDefaultStart(typeCode, hostPath, starter, "");
    }

    /** 登记一条默认启动（带目标过程名，供运行期追踪输出） */
    public void onDefaultStart(String typeCode, List<String> hostPath, GromaProcessStarter starter, String targetName) {
        defaultStarts.computeIfAbsent(typeCode, k -> new ArrayList<>(4))
                .add(new DefaultStart(typeCode, hostPath == null ? List.of() : List.copyOf(hostPath), starter,
                        targetName == null ? "" : targetName));
    }

    /** 被动事件码（标准 21 类事件中的反向/被动视角：个体进出集合与被建立/改向/解除指向） */
    private static final Set<String> PASSIVE_EVENT_CODES =
            Set.of("am", "xm", "ar", "vr", "xr");

    /**
     * 派发一条对象事件，返回被触发启动的过程数。
     * <p>
     * 被动事件通配兜底（code4.html M4 收割）：元素型被动事件（am/xm）以持有方集合成员令牌派发、
     * 引用型被动事件（ar/vr/xr）以 "*" 类型级令牌派发；当目标类型视图以通配令牌订阅自身被动事件
     * （注册键 {@code type|*|code}）而事件携带具体成员令牌时，精确键落空后按类型级通配键兜底匹配。
     */
    public int dispatch(GromaWorldContext ctx, BaseSimEntity source, WorldSnapshot.SimEventRecord event) {
        List<Trigger> triggers = index.get(source.getTypeCode() + "|" + event.memberToken() + "|" + event.eventCode());
        if ((triggers == null || triggers.isEmpty())
                && PASSIVE_EVENT_CODES.contains(event.eventCode())
                && !"*".equals(event.memberToken())) {
            // 类型级通配兜底：目标类型自身视图订阅的被动事件不携带成员键
            triggers = index.get(source.getTypeCode() + "|*|" + event.eventCode());
        }
        if (triggers == null || triggers.isEmpty()) {
            return 0;
        }
        int fired = 0;
        List<BaseSimEntity> contexts = new ArrayList<>(4);
        for (Trigger trigger : triggers) {
            if (!trigger.stateLabel().isEmpty() && !trigger.stateLabel().equals(event.stateLabel())) {
                continue;
            }
            contexts.clear();
            collectContexts(ctx, source, trigger.contextPath(), trigger.contextPath().size() - 1, contexts);
            for (BaseSimEntity contextEntity : contexts) {
                // 线上判断以接线所属对象为上下文求值（与存量引擎 hostInst.doJudge 一致）
                if (trigger.judge() != null && !trigger.judge().test(ctx, contextEntity)) {
                    continue;
                }
                BaseSimEntity host = resolveHost(ctx, contextEntity, trigger.hostPath());
                if (host != null) {
                    traceTriggerFired(ctx, event, contextEntity, host, trigger.targetName());
                    trigger.starter().start(ctx, contextEntity, host);
                    fired++;
                }
            }
        }
        return fired;
    }

    /** 对象创建时启动其全部默认启动过程，返回启动数 */
    public int fireDefaultStarts(GromaWorldContext ctx, BaseSimEntity entity) {
        int fired = 0;
        for (DefaultStart ds : defaultStartsFor(entity.getTypeCode())) {
            BaseSimEntity host = resolveHost(ctx, entity, ds.hostPath());
            if (host != null) {
                traceDefaultStartFired(ctx, entity, host, ds.targetName());
                ds.starter().start(ctx, entity, host);
                fired++;
            }
        }
        return fired;
    }

    private static void traceTriggerFired(GromaWorldContext ctx, WorldSnapshot.SimEventRecord event,
                                           BaseSimEntity context, BaseSimEntity host, String targetName) {
        GromaTraceListener listener = ctx.traceListener();
        if (listener != null) {
            listener.onTriggerFired(ctx, ctx.getTickIndex(), event, context, host, targetName);
        }
    }

    private static void traceDefaultStartFired(GromaWorldContext ctx, BaseSimEntity created, BaseSimEntity host,
                                               String targetName) {
        GromaTraceListener listener = ctx.traceListener();
        if (listener != null) {
            listener.onDefaultStartFired(ctx, ctx.getTickIndex(), created, host, targetName);
        }
    }

    /**
     * 沿成员路径找到过程宿主；路径缺失时记录问题并返回 null
     */
    public static BaseSimEntity resolveHost(GromaWorldContext ctx, BaseSimEntity context, List<String> hostPath) {
        BaseSimEntity host = context;
        for (String token : hostPath) {
            Object next = host.getMemberValue(token);
            if (!(next instanceof BaseSimEntity ref)) {
                ctx.recordPublicProblem("过程宿主路径缺失: " + host.getTypeZhName() + "." + token);
                return null;
            }
            host = ref;
        }
        return host;
    }

    /**
     * 沿成员路径的反向引用逐跳找到接线所属对象
     */
    private static void collectContexts(GromaWorldContext ctx, BaseSimEntity node, List<String> path, int idx,
                                        List<BaseSimEntity> out) {
        if (idx < 0) {
            out.add(node);
            return;
        }
        String token = path.get(idx);
        for (String referrerId : ctx.referrersOf(node.getInstanceId(), token)) {
            BaseSimEntity referrer = ctx.findInstance(referrerId);
            if (referrer != null && referrer.isAlive()) {
                collectContexts(ctx, referrer, path, idx - 1, out);
            }
        }
    }

    public List<DefaultStart> defaultStartsFor(String typeCode) {
        List<DefaultStart> list = defaultStarts.get(typeCode);
        return list == null ? List.of() : List.copyOf(list);
    }

    public int triggerCount() {
        return triggerCount;
    }

    public int defaultStartCount() {
        int count = 0;
        for (List<DefaultStart> list : defaultStarts.values()) {
            count += list.size();
        }
        return count;
    }
}

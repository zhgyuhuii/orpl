package com.groma.base.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 被动视角事件派发与通配路由回归（code4.html M4 收割，标准 21 类事件全集收口）：
 * <ul>
 *   <li>引用成员变更：持有方主动 ra/rv/rx 之外，被指向目标同步收到 ar/vr/xr（类型级通配令牌 "*"）；</li>
 *   <li>集合增删：元素侧 am/xm 以持有方集合成员令牌派发，目标类型视图以 {@code type|*|code}
 *       通配订阅时经 GromaTriggerTable.dispatch 通配兜底命中；持有方成员精确订阅不受影响。</li>
 * </ul>
 * 真实双模型语料无被动事件订阅（corpus-dead），本测试为该路径的唯一运行期回归。
 */
class PassiveEventDispatchTest {

    /** 最小测试实体：对象引用成员 + 集合成员（成员写入走引擎 onMemberChanged / 集合辅助派发） */
    private static final class StubEntity extends BaseSimEntity {
        private final String typeCode;
        private final Map<String, Object> members = new LinkedHashMap<>();

        StubEntity(String instanceId, String typeCode) {
            super(instanceId);
            this.typeCode = typeCode;
        }

        @Override public BaseSimEntity copy(GromaWorldContext ctx) {
            return new StubEntity(getInstanceId() + "_copy", typeCode);
        }

        @Override public String getTypeCode() {
            return typeCode;
        }

        @Override public String getTypeZhName() {
            return typeCode + "中文名";
        }

        @Override public List<GromaMemberMeta> memberMetas() {
            return List.of(new GromaMemberMeta("attr_target", "指向目标", "target", "BASE", "ref", ""),
                    new GromaMemberMeta("attr_cards", "卡片集合", "cards", "BASE", "collection", ""));
        }

        @Override public Object getMemberValue(String memberToken) {
            return members.get(memberToken);
        }

        @Override public boolean setMemberValue(String memberToken, Object rawValue, GromaWorldContext ctx) {
            Object old = members.put(memberToken, rawValue);
            ctx.onMemberChanged(this, memberToken, old, rawValue);
            return true;
        }

        /** 运行期引用写入（带事件）：生成实体的 ctx-setter 同构 */
        void setTarget(GromaWorldContext ctx, StubEntity newTarget) {
            Object old = members.put("attr_target", newTarget);
            ctx.onMemberChanged(this, "attr_target", old, newTarget);
        }

        /** 运行期集合加入（带事件）：collectionSink 汇的同构直调 */
        void addCard(GromaWorldContext ctx, StubEntity card) {
            cardList().add(card);
            ctx.emitCollectionElementAdded(this, "attr_cards", card);
        }

        void removeCard(GromaWorldContext ctx, StubEntity card) {
            cardList().remove(card);
            ctx.emitCollectionElementRemoved(this, "attr_cards", card);
        }

        @SuppressWarnings("unchecked")
        private List<Object> cardList() {
            return (List<Object>) members.computeIfAbsent("attr_cards", k -> new ArrayList<>());
        }
    }

    /** 最小测试世界：两卡片 + 一容器（与测试用例共享同一桩运行时/触发表） */
    private static final class StubContext extends GromaWorldContext {
        StubEntity card1;
        StubEntity card2;
        StubEntity box;

        StubContext(StubRuntime runtime) {
            super(runtime, "sess-passive", "场景1", 0L, 1000L, 42L);
        }

        @Override protected void buildInitialWorld() {
            card1 = new StubEntity("card_1", "Card");
            card2 = new StubEntity("card_2", "Card");
            box = new StubEntity("box_1", "Box");
            register(card1);
            register(card2);
            register(box);
        }

        @Override protected BaseSimEntity newNamedInstance(String instanceId) {
            return new StubEntity(instanceId, "Card");
        }

        @Override protected Object newNamedCollection(String instanceId) {
            return new ArrayList<>();
        }
    }

    /** 触发表登记的桩运行时（仅通表；其余描述接口空实现） */
    private static final class StubRuntime implements GromaModuleRuntime {
        final GromaTriggerTable table = new GromaTriggerTable();

        @Override public String modelCode() {
            return "passive-probe";
        }

        @Override public String modelVersion() {
            return "1";
        }

        @Override public String modelLabel() {
            return "被动事件探针";
        }

        @Override public String modelId() {
            return "RM_PASSIVE";
        }

        @Override public long defaultStepMs() {
            return 1000L;
        }

        @Override public List<SceneDescriptor> scenes() {
            return List.of();
        }

        @Override public GromaTriggerTable triggerTable() {
            return table;
        }

        @Override public GromaWorldContext createSession(String sceneCode, String sessionId, long startEpochMs,
                                                         long finalStepMs, long seed,
                                                         Map<String, Object> initialOverrides) {
            return new StubContext(this);
        }

        @Override public BaseSimEntity newEntity(String typeCode, String instanceId) {
            return new StubEntity(instanceId, typeCode);
        }

        @Override public GromaProcessTask newProcess(String defKey, GromaWorldContext ctx, BaseSimEntity host,
                                                     Map<String, Object> args) {
            throw new UnsupportedOperationException("桩运行时不装配过程");
        }

        @Override public List<TypeDescriptor> types() {
            return List.of();
        }

        @Override public List<ProcessDescriptor> processes() {
            return List.of();
        }

        @Override public String indexMarkdown() {
            return "";
        }
    }

    private record Fired(AtomicInteger count, AtomicReference<String> hostId) {
        Fired() {
            this(new AtomicInteger(), new AtomicReference<>(""));
        }

        GromaProcessStarter starter() {
            return (ctx, context, host) -> {
                count.incrementAndGet();
                hostId.set(host == null ? "" : host.getInstanceId());
            };
        }
    }

    @Test
    void referenceChangeDispatchesPassiveArVrXrOnTargets() {
        StubRuntime runtime = new StubRuntime();
        Fired arAny = new Fired();
        Fired vrAny = new Fired();
        Fired xrAny = new Fired();
        // 被动订阅定义在目标类型自身视图：类型级通配令牌 "*"（xr 订阅对该类型全部实例生效，
        // 具体作用目标以事件宿主分辨）
        runtime.table.onEvent("Card", "*", "ar", "", null, List.of(), List.of(), arAny.starter(), "被指向");
        runtime.table.onEvent("Card", "*", "vr", "", null, List.of(), List.of(), vrAny.starter(), "引用改向被动");
        runtime.table.onEvent("Card", "*", "xr", "", null, List.of(), List.of(), xrAny.starter(), "被解除指向");

        StubContext ctx = new StubContext(runtime);
        java.util.List<String> seen = new java.util.ArrayList<>();
        ctx.setTraceListener(new GromaTraceListener() {
            @Override public void onEvent(GromaWorldContext c, long tick, boolean silent,
                                          WorldSnapshot.SimEventRecord event) {
                seen.add(event.eventCode() + "|token=" + event.memberToken() + "|src=" + event.sourceId());
            }
        });
        ctx.initializeOnLoop();
        ctx.drainPendingEventsNow();
        seen.clear();

        // 建立：box → card1，card1 收 ar
        ctx.box.setTarget(ctx, ctx.card1);
        org.junit.jupiter.api.Assertions.assertTrue(ctx.pendingEventCount() > 0,
                "引用变更应入队事件，实际待派发=" + ctx.pendingEventCount());
        ctx.drainPendingEventsNow();
        Assertions.assertEquals(1, arAny.count().get(),
                "建立指向应向新目标派发 ar，派发事件=" + seen);
        Assertions.assertEquals("card_1", arAny.hostId().get(), "ar 事件宿主应为被动目标自身");

        // 改指：box → card2，card1 收 xr、card2 收 vr
        ctx.box.setTarget(ctx, ctx.card2);
        ctx.drainPendingEventsNow();
        Assertions.assertEquals(1, xrAny.count().get(), "改指应向旧目标派发 xr");
        Assertions.assertEquals("card_1", xrAny.hostId().get(), "xr 宿主应为旧目标");
        Assertions.assertEquals(1, vrAny.count().get(), "改指应向新目标派发 vr");
        Assertions.assertEquals("card_2", vrAny.hostId().get(), "vr 宿主应为新目标");

        // 解除：box → null，card2 收 xr
        ctx.box.setTarget(ctx, null);
        ctx.drainPendingEventsNow();
        Assertions.assertEquals(2, xrAny.count().get(), "解除指向再次派发 xr（两次改指累计）");
        Assertions.assertEquals("card_2", xrAny.hostId().get(), "第二次 xr 宿主应为被解除的 card2");
        Assertions.assertEquals(1, arAny.count().get(), "改指与解除不应误发 ar");
    }

    @Test
    void collectionPassiveEventsRouteThroughWildcardFallback() {
        StubRuntime runtime = new StubRuntime();
        // 元素类型视图通配订阅（注册键 Card|*|am）：事件携带持有方成员令牌，须走通配兜底
        Fired amWildcard = new Fired();
        Fired xmWildcard = new Fired();
        runtime.table.onEvent("Card", "*", "am", "", null, List.of(), List.of(), amWildcard.starter(), "加入集合被动");
        runtime.table.onEvent("Card", "*", "xm", "", null, List.of(), List.of(), xmWildcard.starter(), "移出集合被动");

        StubContext ctx = new StubContext(runtime);
        ctx.initializeOnLoop();
        ctx.drainPendingEventsNow();

        ctx.box.addCard(ctx, ctx.card1);
        ctx.drainPendingEventsNow();
        Assertions.assertEquals(1, amWildcard.count().get(), "元素加入应以通配兜底命中元素类型视图订阅");

        ctx.box.removeCard(ctx, ctx.card1);
        ctx.drainPendingEventsNow();
        Assertions.assertEquals(1, xmWildcard.count().get(), "元素移出应以通配兜底命中元素类型视图订阅");
    }

    @Test
    void holderExactTokenSubscriptionStillMatchesAm() {
        StubRuntime runtime = new StubRuntime();
        // 持有方视图精确订阅（注册键 Card|attr_cards|am）：精确键直配，不依赖兜底
        Fired amExact = new Fired();
        runtime.table.onEvent("Card", "attr_cards", "am", "", null, List.of(), List.of(), amExact.starter(), "持有方精确");

        StubContext ctx = new StubContext(runtime);
        ctx.initializeOnLoop();
        ctx.drainPendingEventsNow();

        ctx.box.addCard(ctx, ctx.card1);
        ctx.drainPendingEventsNow();
        Assertions.assertEquals(1, amExact.count().get(), "持有方成员令牌精确订阅应直接命中");
    }
}

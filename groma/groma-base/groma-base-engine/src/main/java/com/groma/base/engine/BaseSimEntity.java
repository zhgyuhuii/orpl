package com.groma.base.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全部编译产出领域实体的公共基类（落实设计书 3.1 节“UavEntity extends BaseSimEntity”与 R06 控制掩码）
 * <p>
 * 实例标识、存活位、脏掩码、控制掩码只在这里维护一份；成员的强类型 Getter/Setter 由各模型子模块的 Entity 生成，
 * 另外零反射地实现按成员标识读写的 switch 入口，供事件接线、快照、MCP 干预与存档续跑使用。
 * </p>
 */
public abstract class BaseSimEntity {

    /** getMemberValue 未命中成员时返回的哨兵，用于区分“成员不存在”与“成员值为 null” */
    public static final Object MEMBER_NOT_FOUND = new Object() {
        @Override
        public String toString() {
            return "MEMBER_NOT_FOUND";
        }
    };

    public static final String REF_PREFIX = "@ref:";
    public static final String ENUM_PREFIX = "@enum:";

    private final String instanceId;
    private boolean alive = true;
    private long dirtyMask;
    private long controlMask = -1L;
    private boolean creationOpen;

    protected BaseSimEntity(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * R02 / 赋值运算（assignment.assign：将 a 实例的值赋给 b，赋值后二者为不同实例）：
     * 生成代码按成员逐一复制基础值与枚举值，对象引用成员按引用复制；副本以新实例标识登记进世界
     */
    public abstract BaseSimEntity copy(GromaWorldContext ctx);

    /** 对象类型英文编码（如 Station），与类型接线表、默认启动表的主键一致 */
    public abstract String getTypeCode();

    /** 对象类型中文原名（如 车站） */
    public abstract String getTypeZhName();

    /** 本类型的全部成员元数据（声明顺序稳定） */
    public abstract List<GromaMemberMeta> memberMetas();

    /** 按成员标识读取成员值；未命中返回 MEMBER_NOT_FOUND */
    public abstract Object getMemberValue(String memberToken);

    /**
     * 按成员标识写入成员值（自动做字符串到基础类型的转换），成功返回 true。
     * 存档回填时对象引用以 "@ref:实例标识"、枚举以 "@enum:常量名" 的字符串形式传入，集合内的元素同理。
     */
    public abstract boolean setMemberValue(String memberToken, Object rawValue, GromaWorldContext ctx);

    public String getInstanceId() {
        return instanceId;
    }

    public final boolean isAlive() {
        return alive;
    }

    public final long getDirtyMask() {
        return dirtyMask;
    }

    public final void clearDirtyMask() {
        this.dirtyMask = 0L;
    }

    public final long getControlMask() {
        return controlMask;
    }

    /** R06 结构控制关系：源状态变化时 O(1) 刷新成员可用位与过程使能位 */
    public final void setControlMask(long controlMask) {
        this.controlMask = controlMask;
    }

    protected final void markDirty(int bitIndex) {
        this.dirtyMask |= (1L << Math.min(62, Math.max(0, bitIndex)));
    }

    protected final void requireAlive() {
        if (!alive) {
            throw new IllegalStateException("已消亡实例禁止写入属性: " + instanceId);
        }
    }

    final boolean isCreationOpen() {
        return creationOpen;
    }

    /** 进入创建期：创建期内的成员赋值不产生事件（供生成的 copy() 等构造副本的代码使用） */
    protected final void openCreation() {
        this.creationOpen = true;
    }

    final void closeCreation() {
        this.creationOpen = false;
    }

    final void markDead() {
        this.alive = false;
    }

    /**
     * R11 消耗关系：叶过程成功后消耗对象（置亡并发 x 事件）；重复消耗立即失败
     */
    public void markConsumed(GromaWorldContext ctx) {
        if (!alive) {
            throw new IllegalStateException("实例已被消耗销毁，禁止二次消耗: " + instanceId);
        }
        this.alive = false;
        this.dirtyMask |= 1L;
        if (ctx != null) {
            ctx.onEntityConsumed(this);
        }
    }

    /**
     * 按中文原名、英文成员名或成员标识定位成员元数据
     */
    public final GromaMemberMeta findMember(String nameOrToken) {
        if (nameOrToken == null) {
            return null;
        }
        String key = nameOrToken.trim();
        for (GromaMemberMeta meta : memberMetas()) {
            if (meta.token().equals(key) || meta.zhName().equals(key) || meta.enName().equals(key)) {
                return meta;
            }
        }
        return null;
    }

    /**
     * 对外快照：以中文成员名为键；枚举输出状态中文原名，对象引用输出对端实例标识（VO 层扁平化，杜绝 JSON 循环引用）
     */
    public Map<String, Object> toSnapshotMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("instanceId", instanceId);
        map.put("type", getTypeCode());
        map.put("typeZhName", getTypeZhName());
        map.put("alive", alive);
        for (GromaMemberMeta meta : memberMetas()) {
            map.put(meta.zhName(), snapshotValue(getMemberValue(meta.token())));
        }
        return map;
    }

    /**
     * 存档导出：以成员标识为键；对象引用序列化为 @ref:实例标识，枚举序列化为 @enum:常量名，集合逐元素编码
     */
    public Map<String, Object> exportState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("alive", alive);
        for (GromaMemberMeta meta : memberMetas()) {
            Object raw = getMemberValue(meta.token());
            if (raw == MEMBER_NOT_FOUND) {
                continue;
            }
            state.put(meta.token(), encodeValue(raw));
        }
        return state;
    }

    protected final Object encodeValue(Object raw) {
        return GromaValues.encode(raw);
    }

    /**
     * 存档回填：调用前提是全部实例已按原实例标识重建为空对象，因此对象引用可以直接解析。
     * 回填期间上下文处于静默期，不产生值变化事件。
     */
    public final void importState(Map<String, Object> state, GromaWorldContext ctx) {
        if (state == null) {
            return;
        }
        Object aliveFlag = state.get("alive");
        if (Boolean.FALSE.equals(aliveFlag)) {
            this.alive = false;
        }
        for (GromaMemberMeta meta : memberMetas()) {
            Object raw = state.get(meta.token());
            if (raw == null) {
                continue;
            }
            setMemberValue(meta.token(), decodeValue(raw, ctx), ctx);
        }
    }

    protected final Object decodeValue(Object raw, GromaWorldContext ctx) {
        return GromaValues.decode(raw, ctx);
    }

    /**
     * 快照取值：枚举 → 状态中文原名；对象 → 实例标识；集合逐元素转换
     */
    protected final Object snapshotValue(Object raw) {
        if (raw == MEMBER_NOT_FOUND || raw == null) {
            return null;
        }
        if (raw instanceof Enum<?> en) {
            return enumZhLabel(en);
        }
        if (raw instanceof BaseSimEntity ref) {
            return ref.getInstanceId();
        }
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(snapshotValue(e.getKey())), snapshotValue(e.getValue()));
            }
            return out;
        }
        if (raw instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(snapshotValue(item));
            }
            return out;
        }
        return raw;
    }

    /**
     * 枚举状态中文名：编译产出的枚举统一带 getZhLabel()
     */
    public static String enumZhLabel(Enum<?> en) {
        try {
            return (String) en.getClass().getMethod("getZhLabel").invoke(en);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return en.name();
        }
    }

    @Override
    public String toString() {
        return getTypeCode() + "(" + instanceId + ")";
    }
}

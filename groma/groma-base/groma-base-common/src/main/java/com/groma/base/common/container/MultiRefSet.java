package com.groma.base.common.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 带基数上下限校验的多重关联集合（R07，保持插入确定性顺序）
 * <p>
 * code5.html 3.1 收割增强：构建期静默批量装配（{@link #silentAddAll}，初态/存档恢复对称路径，
 * 不派发事件不触发基数拦截）、存档对称导出（{@link #exportOrderedList}），运行期 add/remove 语义不变。
 */
public final class MultiRefSet<T> {

    private final int minCardinality;
    private final int maxCardinality;
    private final LinkedHashSet<T> targets = new LinkedHashSet<>();
    private final BiConsumer<String, T> eventSink;

    public MultiRefSet(int minCardinality, int maxCardinality, BiConsumer<String, T> eventSink) {
        this.minCardinality = Math.max(0, minCardinality);
        this.maxCardinality = maxCardinality <= 0 ? Integer.MAX_VALUE : maxCardinality;
        this.eventSink = eventSink != null ? eventSink : (c, t) -> {};
    }

    public boolean addLink(T target) {
        if (targets.contains(target)) {
            return false;
        }
        if (targets.size() >= maxCardinality) {
            throw new IllegalStateException("超出关联基数上限 max=" + maxCardinality);
        }
        targets.add(target);
        eventSink.accept("ra", target);
        return true;
    }

    public boolean removeLink(T target) {
        if (!targets.contains(target)) {
            return false;
        }
        if (targets.size() <= minCardinality) {
            throw new IllegalStateException("低于关联基数下限 min=" + minCardinality);
        }
        targets.remove(target);
        eventSink.accept("rx", target);
        return true;
    }

    /** 构建期/存档恢复专用：静默批量装配（保持顺序、无事件、无基数拦截；后置基数断言供审计） */
    public void silentAddAll(Iterable<T> restored) {
        for (T item : restored) {
            targets.add(item);
        }
    }

    /** 存档导出：插入顺序快照 */
    public List<T> exportOrderedList() {
        return new ArrayList<>(targets);
    }

    /** 当前基数（供 canStart 的 1..n 下限守卫与一致性审计） */
    public int size() {
        return targets.size();
    }

    public Set<T> view() {
        return Collections.unmodifiableSet(targets);
    }
}

package com.groma.base.common.container;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 带基数上下限校验的多重关联集合（R07，保持插入确定性顺序）
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

    public Set<T> view() {
        return Collections.unmodifiableSet(targets);
    }
}

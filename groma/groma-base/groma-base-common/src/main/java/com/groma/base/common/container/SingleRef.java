package com.groma.base.common.container;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 单向单重关联引用槽（R07 unidirectional-association，自动派发指向建立 ra、指向变更 rv、指向解除 rx 事件）
 */
public final class SingleRef<T> {

    private T target;
    private final BiConsumer<String, T> eventSink;

    public SingleRef(BiConsumer<String, T> eventSink) {
        this.eventSink = eventSink != null ? eventSink : (c, t) -> {};
    }

    public T get() {
        return target;
    }

    public void linkTo(T newTarget) {
        if (Objects.equals(this.target, newTarget)) {
            return;
        }
        T old = this.target;
        this.target = newTarget;
        if (old == null && newTarget != null) {
            eventSink.accept("ra", newTarget);
        } else if (old != null && newTarget == null) {
            eventSink.accept("rx", old);
        } else {
            eventSink.accept("rv", newTarget);
        }
    }
}

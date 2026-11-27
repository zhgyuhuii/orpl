package com.groma.base.common.container;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 支持集合成员代理（R05 proxy）的可观测列表：增删成员自动向队列投递 ma / mx 事件（落实 BE-INS-005）
 */
public final class ObservableEntityList<T> extends AbstractList<T> {

    private final List<T> delegate = new ArrayList<>(16);
    private final BiConsumer<String, T> eventQueueSink;

    public ObservableEntityList(BiConsumer<String, T> eventQueueSink) {
        this.eventQueueSink = eventQueueSink != null ? eventQueueSink : (code, item) -> {};
    }

    @Override
    public T get(int index) {
        return delegate.get(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void add(int index, T element) {
        delegate.add(index, element);
        eventQueueSink.accept("ma", element);
    }

    @Override
    public T remove(int index) {
        T removed = delegate.remove(index);
        eventQueueSink.accept("mx", removed);
        return removed;
    }
}

package com.groma.base.common.container;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 支持集合成员代理（R05 proxy）的可观测列表：增删成员自动向队列投递 ma / mx 事件（落实 BE-INS-005）
 */
public final class ObservableEntityList<T> extends AbstractList<T> {

    private final List<T> delegate;
    private BiConsumer<String, Object> eventQueueSink;

    public ObservableEntityList(BiConsumer<String, T> eventQueueSink) {
        this(new ArrayList<>(16), eventQueueSink);
    }

    /**
     * 以既有列表为后備的受观察视图（方案 C 收割）：生成实体把集合字段接到本视图后，
     * 过程代码经 getter 的 add/remove 会同时投递持有方（ma/mx）与元素（am/xm）双视角事件。
     */
    @SuppressWarnings("unchecked")
    public ObservableEntityList(List<T> backing, BiConsumer<String, T> eventQueueSink) {
        this.delegate = backing;
        this.eventQueueSink = eventQueueSink == null ? (code, item) -> {}
                : (BiConsumer<String, Object>) eventQueueSink;
    }

    /** 已是受观察列表则换绑事件汇后返回（归属随新持有方走），否则包一层带事件的委托视图 */
    @SuppressWarnings("unchecked")
    public static <T> ObservableEntityList<T> observed(List<T> backing, BiConsumer<String, T> eventQueueSink) {
        if (backing instanceof ObservableEntityList<T> o) {
            o.eventQueueSink = eventQueueSink == null ? (code, item) -> {}
                    : (BiConsumer<String, Object>) eventQueueSink;
            return o;
        }
        return new ObservableEntityList<>(backing, eventQueueSink);
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

    @Override
    public T set(int index, T element) {
        T replaced = delegate.set(index, element);
        // 位置替换 = 移出旧元素 + 加入新元素（双视角事件随 add/remove 一致派发）
        eventQueueSink.accept("mx", replaced);
        eventQueueSink.accept("ma", element);
        return replaced;
    }
}

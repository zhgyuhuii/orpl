package com.groma.base.common.container;

/**
 * 单向输出影响参数槽（R18 out-influence）
 */
public final class OutSlot<T> {
    private T value;
    private boolean written;

    public void set(T value) {
        this.value = value;
        this.written = true;
    }

    public T get() {
        return value;
    }

    public boolean isWritten() {
        return written;
    }
}

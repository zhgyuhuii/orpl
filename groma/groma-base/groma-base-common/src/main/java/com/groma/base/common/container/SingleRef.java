package com.groma.base.common.container;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 单向单重关联引用槽（R07 unidirectional-association，自动派发指向建立 ra、指向变更 rv、指向解除 rx 事件）
 * <p>
 * code5.html 3.1 收割增强：
 * <ul>
 *   <li>1..1 必选基数：运行期禁止把必选引用置空（linkTo(null) 记入基数违规回调），0..1 可空；</li>
 *   <li>构建期静默：{@link #silentLinkTo} 供初态装配/存档恢复对称回放（不派发事件），运行期写入才走事件；</li>
 *   <li>存档对称：{@link #exportTarget}/{@link #importTarget} 以目标标识导出/回放，恢复走静默路径。</li>
 * </ul>
 */
public final class SingleRef<T> {

    private T target;
    private final BiConsumer<String, T> eventSink;
    /** 基数违规回调（1..1 被置空时触发；参数=成员令牌），容器不感知平台问题通道，由事件汇桥接 */
    private final Runnable minCardinalityViolation;
    private final boolean required;

    public SingleRef(BiConsumer<String, T> eventSink) {
        this(eventSink, false, null);
    }

    /** @param required 1..1 必选关联（运行期置空记基数违规）；0..1 可空传 false */
    public SingleRef(BiConsumer<String, T> eventSink, boolean required, Runnable minCardinalityViolation) {
        this.eventSink = eventSink != null ? eventSink : (c, t) -> {};
        this.required = required;
        this.minCardinalityViolation = minCardinalityViolation;
    }

    public T get() {
        return target;
    }

    public boolean isPresent() {
        return target != null;
    }

    public void linkTo(T newTarget) {
        if (Objects.equals(this.target, newTarget)) {
            return;
        }
        if (required && newTarget == null) {
            // R07 1..1 下限契约：运行期禁止置空必选关联（静默拒绝，交回调记账）
            if (minCardinalityViolation != null) {
                minCardinalityViolation.run();
            }
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

    /** 构建期/存档恢复专用：静默写入，不派发事件、不做基数拦截（初态装配对称路径） */
    public void silentLinkTo(T newTarget) {
        this.target = newTarget;
    }

    /** 存档导出：返回目标引用（由导出层转实例标识），null = 空槽 */
    public T exportTarget() {
        return target;
    }

    /** 存档恢复：静默回放目标（与 exportTarget 对称） */
    public void importTarget(T restored) {
        this.target = restored;
    }
}

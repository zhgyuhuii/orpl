package com.groma.base.engine;

/**
 * 过程启动器：由编译产出代码实现，负责按接线对象的当前成员值绑定实参并启动目标过程
 * （瞬时算子同步执行；耗时过程经 {@link GromaWorldContext#launch} 挂入步进队列）
 */
@FunctionalInterface
public interface GromaProcessStarter {

    /**
     * @param ctx      世界上下文
     * @param context  接线所属对象（事件接线 / 默认启动登记所在的类型实例，实参相对它取值）
     * @param host     目标过程的宿主对象（组成线指定的成员；未指定时即接线对象自身）
     */
    void start(GromaWorldContext ctx, BaseSimEntity context, BaseSimEntity host);
}

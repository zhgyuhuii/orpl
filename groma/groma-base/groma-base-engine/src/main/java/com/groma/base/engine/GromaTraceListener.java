package com.groma.base.engine;

/**
 * 推演追踪监听器：世界上下文在单写者线程上回调过程启停、成员事件与触发器命中（落实设计书第六章 DAP 调试架构的运行期事件面）。
 * <p>
 * 全部回调都在单写者线程上同步执行，实现方必须立即返回、不得读写世界状态；
 * 供调试适配协议推送断点续跑轨迹、宿主异步日志与环形缓冲落盘使用。
 * </p>
 */
public interface GromaTraceListener {

    /** 过程任务启动（进入 RUNNING） */
    default void onProcessStarted(GromaWorldContext ctx, GromaProcessTask task) {
    }

    /** 过程任务结束（成功 / 失败 / 终止；失败与终止附带原因） */
    default void onProcessFinished(GromaWorldContext ctx, GromaProcessTask task) {
    }

    /**
     * 成员事件入队
     *
     * @param silent true 表示处于初始化静默期或对象创建期（事件仍会入队，但不触发派发）
     */
    default void onEvent(GromaWorldContext ctx, long tick, boolean silent, WorldSnapshot.SimEventRecord event) {
    }

    /** 事件触发器命中并启动目标过程 */
    default void onTriggerFired(GromaWorldContext ctx, long tick, WorldSnapshot.SimEventRecord event,
                                BaseSimEntity contextEntity, BaseSimEntity host, String targetDefKey) {
    }

    /** 默认启动命中并启动目标过程 */
    default void onDefaultStartFired(GromaWorldContext ctx, long tick, BaseSimEntity created, BaseSimEntity host,
                                     String targetDefKey) {
    }

    /** 世界级问题记录（过程失败、接线缺失、宿主路径缺失等） */
    default void onProblem(GromaWorldContext ctx, long tick, String message) {
    }
}

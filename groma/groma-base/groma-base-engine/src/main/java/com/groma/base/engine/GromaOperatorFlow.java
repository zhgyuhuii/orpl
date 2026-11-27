package com.groma.base.engine;

/**
 * 瞬时算子（不消耗仿真时间）的流程基类：由 ServiceImpl 的算子方法在调用栈上同步驱动
 * （落实设计书 3.1 节第 6 行“算子 → I{Owner}Service 接口声明 + {Owner}ServiceImpl 方法实现，瞬时执行零状态机开销”）
 */
public abstract class GromaOperatorFlow extends GromaProcessTask {

    protected GromaOperatorFlow(GromaWorldContext ctx, BaseSimEntity host, String defKey, String processName) {
        super(ctx, host, defKey, processName, Timing.INSTANT);
    }

    /**
     * 同步执行整个算子流程，返回是否成功
     */
    public final boolean runSync() {
        return runToCompletionSync();
    }
}

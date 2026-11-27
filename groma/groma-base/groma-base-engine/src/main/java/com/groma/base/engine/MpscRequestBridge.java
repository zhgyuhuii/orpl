package com.groma.base.engine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多生产者单消费者无锁请求桥（落实 BE-TSK-003 / NP-ENG-002，全过程零阻塞监视器锁）
 * 所有来自 REST / WebSocket / MCP / 异步连接器的写请求必须经此队列进入推演线程安全点串行执行。
 */
public final class MpscRequestBridge<C> {

    public record Envelope<C, R>(Function<C, R> action, CompletableFuture<R> future) {
        void executeOnLoop(C context) {
            if (future.isCancelled()) {
                return;
            }
            try {
                R result = action.apply(context);
                future.complete(result);
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        }

        void abort(Throwable reason) {
            future.completeExceptionally(reason);
        }
    }

    private final ConcurrentLinkedQueue<Envelope<C, ?>> queue = new ConcurrentLinkedQueue<>();

    public <R> CompletableFuture<R> submit(Function<C, R> mutation, long timeoutMs) {
        CompletableFuture<R> future = new CompletableFuture<R>()
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        queue.offer(new Envelope<>(mutation, future));
        return future;
    }

    /**
     * 仅由绑定的单写者推演线程在步进安全点调用
     */
    public int drainOnLoopThread(C context, int maxBatch) {
        int processed = 0;
        Envelope<C, ?> env;
        while (processed < maxBatch && (env = queue.poll()) != null) {
            env.executeOnLoop(context);
            processed++;
        }
        return processed;
    }

    /**
     * 会话异常或终止退出时排空全部等待者，绝不悬挂调用方（落实 CK-BE-TSK-003-1）
     */
    public void drainAndAbortAll(String reason) {
        IllegalStateException ex = new IllegalStateException("推演会话已结束: " + reason);
        Envelope<C, ?> env;
        while ((env = queue.poll()) != null) {
            env.abort(ex);
        }
    }
}

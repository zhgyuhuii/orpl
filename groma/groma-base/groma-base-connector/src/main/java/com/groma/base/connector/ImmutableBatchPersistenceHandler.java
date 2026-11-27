package com.groma.base.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 线程安全不可变批次落库处理器（落实 BE-PER-002，彻底消除存量 BE-04 共享缓冲区并发篡改与提前 Ack 缺陷）
 */
public final class ImmutableBatchPersistenceHandler<T> implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ImmutableBatchPersistenceHandler.class);

    public record AckableMessage<T>(T payload, Runnable ackCallback) {}

    private final BlockingQueue<AckableMessage<T>> boundedQueue;
    private final int batchSize;
    private final int maxRetries;
    private final Consumer<List<T>> batchWriter;
    private final Consumer<List<T>> deadLetterSink;
    private final ExecutorService persistExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicLong deadLetterCount = new AtomicLong(0L);

    public ImmutableBatchPersistenceHandler(
            int queueCapacity,
            int batchSize,
            int maxRetries,
            Consumer<List<T>> batchWriter,
            Consumer<List<T>> deadLetterSink) {
        this.boundedQueue = new LinkedBlockingQueue<>(Math.max(16, queueCapacity));
        this.batchSize = Math.max(1, batchSize);
        this.maxRetries = Math.max(0, maxRetries);
        this.batchWriter = batchWriter;
        this.deadLetterSink = deadLetterSink != null ? deadLetterSink : batch -> {};
    }

    /**
     * 入队时不执行 ackCallback（必须推迟到落库成功之后！）
     */
    public boolean offer(T payload, Runnable ackCallback) {
        return boundedQueue.offer(new AckableMessage<>(payload, ackCallback));
    }

    public CompletableFuture<Integer> flushNextBatchAsync() {
        List<AckableMessage<T>> drained = new ArrayList<>(batchSize);
        boundedQueue.drainTo(drained, batchSize);
        if (drained.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }
        // 创建完全独立的不可变批次快照交给后台虚拟线程，后续任何刷新绝不会触碰此列表！
        List<AckableMessage<T>> immutableEnvelopeBatch = List.copyOf(drained);
        List<T> immutablePayloadBatch = immutableEnvelopeBatch.stream().map(AckableMessage::payload).toList();

        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    batchWriter.accept(immutablePayloadBatch);
                    // 落库成功后才执行消费确认（CK-BE-PER-002-2）
                    for (AckableMessage<T> msg : immutableEnvelopeBatch) {
                        if (msg.ackCallback() != null) {
                            msg.ackCallback().run();
                        }
                    }
                    return immutablePayloadBatch.size();
                } catch (Exception ex) {
                    log.warn("[BatchPersist] 第 {}/{} 次落库失败: {}", attempt + 1, maxRetries + 1, ex.getMessage());
                }
            }
            deadLetterCount.addAndGet(immutablePayloadBatch.size());
            deadLetterSink.accept(immutablePayloadBatch);
            return 0;
        }, persistExecutor);
    }

    public long getDeadLetterCount() {
        return deadLetterCount.get();
    }

    @Override
    public void close() {
        persistExecutor.close();
    }
}

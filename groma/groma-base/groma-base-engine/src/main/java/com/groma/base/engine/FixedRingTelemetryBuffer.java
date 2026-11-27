package com.groma.base.engine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 定长 32KB 内存环形事件与消息缓冲槽（落实 BE-OBS-003：有界内存不膨胀，超出自动淘汰最旧条目）
 */
public final class FixedRingTelemetryBuffer {

    private static final int DEFAULT_CAPACITY = 256;
    private static final int MAX_BYTE_BUDGET = 32 * 1024;

    private final String[] slotRing;
    private int head = 0;
    private int count = 0;

    public FixedRingTelemetryBuffer() {
        this(DEFAULT_CAPACITY);
    }

    public FixedRingTelemetryBuffer(int maxEntries) {
        int cap = Math.max(16, Math.min(maxEntries, 1024));
        this.slotRing = new String[cap];
    }

    public void record(String entry) {
        if (entry == null) {
            return;
        }
        byte[] bytes = entry.getBytes(StandardCharsets.UTF_8);
        String bounded = bytes.length > 256 ? new String(bytes, 0, 256, StandardCharsets.UTF_8) : entry;
        int writeIdx = (head + count) % slotRing.length;
        if (count == slotRing.length) {
            slotRing[head] = bounded;
            head = (head + 1) % slotRing.length;
        } else {
            slotRing[writeIdx] = bounded;
            count++;
        }
    }

    public List<String> snapshotLatest() {
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(slotRing[(head + i) % slotRing.length]);
        }
        return List.copyOf(list);
    }

    public int size() {
        return count;
    }

    public int maxByteBudget() {
        return MAX_BYTE_BUDGET;
    }
}

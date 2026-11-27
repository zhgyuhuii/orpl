package com.groma.base.engine;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 倍速控制与孪生过程 1x 锁速仲裁器（落实 BE-SCH-003 & BE-SCH-004，修复存量 BL-05 恢复旧值错误）
 * 机器步进物理间隔下限为 10ms，最高可达倍速 = finalStepMs / 10.0。
 */
public final class SpeedManager {

    private static final long MIN_MACHINE_GAP_MS = 10L;

    private final long finalStepMs;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong activeTwinProcesses = new AtomicLong(0L);
    private volatile double desiredSpeed = 1.0D;
    private volatile double appliedSpeed = 1.0D;
    private volatile boolean desiredFullSpeed = false;

    public SpeedManager(long finalStepMs) {
        this.finalStepMs = Math.max(1L, finalStepMs);
    }

    public double requestSpeed(double requested) {
        if (requested <= 0.0D || Double.isNaN(requested)) {
            throw new IllegalArgumentException("倍速必须大于 0");
        }
        double maxReachable = Math.max(1.0D, (double) finalStepMs / MIN_MACHINE_GAP_MS);
        double clamped = Math.min(requested, maxReachable);
        lock.lock();
        try {
            this.desiredFullSpeed = false;
            this.desiredSpeed = clamped;
            return activeTwinProcesses.get() > 0L ? 1.0D : clamped;
        } finally {
            lock.unlock();
        }
    }

    public boolean requestFullSpeed() {
        lock.lock();
        try {
            if (activeTwinProcesses.get() > 0L) {
                return false; // 标准 R2：孪生过程运行期间禁止进入全速模式
            }
            this.desiredFullSpeed = true;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void onTwinStarted() {
        activeTwinProcesses.incrementAndGet();
        this.appliedSpeed = 1.0D;
    }

    public void onTwinFinished() {
        long left = activeTwinProcesses.updateAndGet(c -> Math.max(0L, c - 1L));
        if (left == 0L) {
            this.appliedSpeed = this.desiredSpeed;
        }
    }

    /**
     * 在下一步准备阶段生效最新请求的倍速，并返回本步需等待的物理毫秒间隔
     */
    public long applyAtStepBoundaryAndComputeWaitMs() {
        if (activeTwinProcesses.get() > 0L) {
            this.appliedSpeed = 1.0D;
            return finalStepMs;
        }
        if (desiredFullSpeed) {
            this.appliedSpeed = Math.max(1.0D, (double) finalStepMs / MIN_MACHINE_GAP_MS);
            return 0L;
        }
        this.appliedSpeed = this.desiredSpeed;
        return Math.max(MIN_MACHINE_GAP_MS, StrictMath.round(finalStepMs / this.appliedSpeed));
    }

    public double getAppliedSpeed() {
        return appliedSpeed;
    }
}

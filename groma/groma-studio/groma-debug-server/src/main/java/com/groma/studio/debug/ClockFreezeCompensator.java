package com.groma.studio.debug;

/**
 * 时统断点冻结无漂移补偿器（落实设计说明书 V12 第 6.3.6 节：扣除断点思考停顿时长，杜绝恢复后疯狂补帧）
 */
public final class ClockFreezeCompensator {

    private long nextTickDeadlineNano;
    private long pauseStartNano;
    private long totalFrozenAccumulatedNano;
    private boolean debugFrozen;

    public ClockFreezeCompensator(long initialDeadlineNano) {
        this.nextTickDeadlineNano = initialDeadlineNano;
    }

    public void enterDebugFreeze(long currentNano) {
        if (!this.debugFrozen) {
            this.pauseStartNano = currentNano;
            this.debugFrozen = true;
        }
    }

    public long exitDebugFreezeAndShiftDeadline(long currentNano) {
        if (this.debugFrozen) {
            long frozenDelta = Math.max(0L, currentNano - this.pauseStartNano);
            this.nextTickDeadlineNano += frozenDelta;
            this.totalFrozenAccumulatedNano += frozenDelta;
            this.debugFrozen = false;
        }
        return this.nextTickDeadlineNano;
    }

    public long advanceOneStepDeadline(long stepIntervalNano) {
        this.nextTickDeadlineNano += Math.max(1L, stepIntervalNano);
        return this.nextTickDeadlineNano;
    }

    public boolean isDebugFrozen() {
        return debugFrozen;
    }

    public long getNextTickDeadlineNano() {
        return nextTickDeadlineNano;
    }

    public long getTotalFrozenAccumulatedNano() {
        return totalFrozenAccumulatedNano;
    }
}

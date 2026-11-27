package com.groma.base.engine;

/**
 * 整数刻度无漂移时统（落实设计书 2.1.3 节 GromaTimeSystem）
 * <p>
 * 世界时间只由整数运算得到：worldTimeMs = startEpochMs + tickIndex * stepMs，不使用浮点累加，因此不存在跨 CPU 分叉与累加漂移。
 * 墙钟侧只负责回答一个问题：“第 N 步应该在什么时刻执行”。倍速变化时以当前时刻重新定位基准，
 * 避免恢复或调速后出现追帧；调试断点冻结期间的整体顺延由 {@link #exitDebugFreezeAndShiftDeadline(long)} 完成。
 * </p>
 */
public final class GromaTimeSystem {

    private final long stepNs;
    private long originNano;
    private double lastSpeed = 1.0D;
    private long freezeStartNano;
    private long totalFrozenNs;

    public GromaTimeSystem(long stepMs, long originNano) {
        this.stepNs = Math.max(1L, stepMs) * 1_000_000L;
        this.originNano = originNano;
    }

    public long getStepNs() {
        return stepNs;
    }

    /**
     * 进入调试冻结（断点命中时调用）
     */
    public void enterDebugFreeze(long currentNano) {
        if (freezeStartNano == 0L) {
            freezeStartNano = currentNano;
        }
    }

    /**
     * 退出调试冻结：把冻结停留时长整体顺延到时间基准上，恢复后按原倍速平滑推进，绝不追帧
     *
     * @return 本次冻结停留的纳秒数
     */
    public long exitDebugFreezeAndShiftDeadline(long currentNano) {
        if (freezeStartNano == 0L) {
            return 0L;
        }
        long frozen = Math.max(0L, currentNano - freezeStartNano);
        originNano += frozen;
        totalFrozenNs += frozen;
        freezeStartNano = 0L;
        return frozen;
    }

    public boolean isDebugFrozen() {
        return freezeStartNano != 0L;
    }

    public long getTotalFrozenAccumulatedNano() {
        return totalFrozenNs;
    }

    /**
     * 计算下一拍的墙钟截止时刻
     *
     * @param nowNano       当前 monotonic 纳秒时刻
     * @param nextStepOrder 即将执行的是第几步（已完成的步数 + 1）
     * @param speed         期望倍速（已含孪生锁速仲裁结果）
     */
    public long nextDeadlineNano(long nowNano, long nextStepOrder, double speed) {
        double sp = (speed > 0.0D && Double.isFinite(speed)) ? speed : 1.0D;
        if (sp != lastSpeed) {
            // 倍速变化：以当前时刻重定位基准，下一步距现在恰好一个步进间隔
            lastSpeed = sp;
            originNano = nowNano - ticksToNano(nextStepOrder - 1L, sp);
        }
        return originNano + ticksToNano(nextStepOrder, sp);
    }

    /**
     * 单步在给定倍速下的墙钟间隔（纳秒）；机器步进物理间隔下限 10ms（与 SpeedManager 一致）
     */
    public long stepIntervalNano(double speed) {
        double sp = (speed > 0.0D && Double.isFinite(speed)) ? speed : 1.0D;
        return Math.max(MIN_MACHINE_GAP_NS, (long) (stepNs / sp));
    }

    /**
     * 当前可达的最高倍速：stepMs / 10ms
     */
    public double maxReachableSpeed() {
        return Math.max(1.0D, (double) stepNs / MIN_MACHINE_GAP_NS);
    }

    /** 机器步进物理间隔下限（10ms） */
    public static final long MIN_MACHINE_GAP_NS = 10_000_000L;

    private long ticksToNano(long ticks, double speed) {
        if (ticks <= 0L) {
            return 0L;
        }
        return (long) (ticks * (double) stepNs / speed);
    }

    /**
     * 世界时间（毫秒）：startEpochMs + tickIndex * stepMs，整数刻度
     */
    public static long worldTimeMs(long startEpochMs, long stepMs, long tickIndex) {
        return startEpochMs + Math.max(0L, tickIndex) * stepMs;
    }
}

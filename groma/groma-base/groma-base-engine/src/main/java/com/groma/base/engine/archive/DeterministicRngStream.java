package com.groma.base.engine.archive;

import java.util.SplittableRandom;

/**
 * 支持游标保存与重放恢复的确定性随机源（确保第 N 步存档续跑后的随机抽样值与不中断运行完全一致）
 */
public final class DeterministicRngStream {

    private final long rootSeed;
    private long drawIndex;

    public DeterministicRngStream(long rootSeed, long initialDrawIndex) {
        this.rootSeed = rootSeed;
        this.drawIndex = Math.max(0L, initialDrawIndex);
    }

    public double nextDouble() {
        long stepSeed = mix64(rootSeed + (++drawIndex) * 0x9E3779B97F4A7C15L);
        return new SplittableRandom(stepSeed).nextDouble();
    }

    public long nextLongBounded(long minInclusive, long maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        double r = nextDouble();
        return minInclusive + StrictMath.round(r * (maxInclusive - minInclusive));
    }

    /**
     * 标准正态样本（Box-Muller，每次消耗两游标），供正态时长分布采样使用
     */
    public double nextGaussian() {
        double u1 = Math.max(Double.MIN_VALUE, nextDouble());
        double u2 = nextDouble();
        return StrictMath.sqrt(-2.0D * StrictMath.log(u1)) * StrictMath.cos(2.0D * StrictMath.PI * u2);
    }

    /**
     * 回拨游标（存档续跑时恢复到存档记录的抽样位置）
     */
    public void setCursor(long drawIndex) {
        this.drawIndex = Math.max(0L, drawIndex);
    }

    public long getRootSeed() {
        return rootSeed;
    }

    public long getDrawIndex() {
        return drawIndex;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}

package com.groma.base.common.util;

import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * 任务级确定性伪随机派生器（落实 BE-SCH-002 / BE-EVT-005 / BE-MDL-004，严守 NP-R02 & NP-R04）
 */
public final class SeededSampler {

    private final SplittableRandom rng;

    public SeededSampler(long seed) {
        this.rng = new SplittableRandom(seed);
    }

    public SeededSampler fork() {
        return new SeededSampler(rng.split().nextLong());
    }

    public <T> void shuffle(List<T> items) {
        RandomGenerator child = rng.split();
        Collections.shuffle(items, java.util.Random.from(child));
    }

    public boolean hitIndependent(double probability) {
        if (probability >= 1.0D) {
            return true;
        }
        if (probability <= 0.0D) {
            return false;
        }
        return rng.nextDouble() < probability;
    }

    public int sampleGroupedCdf(double[] groupProbabilities) {
        double r = rng.nextDouble();
        double acc = 0.0D;
        for (int i = 0; i < groupProbabilities.length; i++) {
            acc += groupProbabilities[i];
            if (r < acc) {
                return i;
            }
        }
        return groupProbabilities.length - 1;
    }

    public long sampleUniformMs(double minVal, double maxVal, String durationUnit, long fallbackStepMs, Consumer<String> warningSink) {
        if (minVal <= 0.0D || maxVal <= minVal) {
            if (warningSink != null) {
                warningSink.accept("WARN [TIME-DIST-UNIFORM] 非法均匀分布参数 min=" + minVal + ", max=" + maxVal + "，已回退为一个步长");
            }
            return fallbackStepMs;
        }
        long minMs = TimeUnitUtils.toMillis(minVal, durationUnit);
        long maxMs = TimeUnitUtils.toMillis(maxVal, durationUnit);
        return minMs + StrictMath.round(rng.nextDouble() * (maxMs - minMs));
    }

    public long sampleNormalMs(double meanVal, double sdVal, String durationUnit, long fallbackStepMs, Consumer<String> warningSink) {
        if (meanVal <= 0.0D || sdVal <= 0.0D) {
            if (warningSink != null) {
                warningSink.accept("WARN [TIME-DIST-NORMAL] 非法正态分布参数 mean=" + meanVal + ", sd=" + sdVal + "，已回退为一个步长");
            }
            return fallbackStepMs;
        }
        double meanMs = TimeUnitUtils.toMillis(meanVal, durationUnit);
        double sdMs = TimeUnitUtils.toMillis(sdVal, durationUnit);
        return Math.max(1L, StrictMath.round(meanMs + rng.nextGaussian() * sdMs));
    }

    public long sampleExponentialMs(double meanInDurationUnit, String durationUnit, long fallbackStepMs, Consumer<String> warningSink) {
        if (meanInDurationUnit <= 0.0D) {
            if (warningSink != null) {
                warningSink.accept("WARN [TIME-DIST-EXP] 非法指数分布参数 param=" + meanInDurationUnit + "，已回退为一个步长");
            }
            return fallbackStepMs;
        }
        double meanMs = TimeUnitUtils.toMillis(meanInDurationUnit, durationUnit);
        double u = Math.max(1.0e-12D, 1.0D - rng.nextDouble());
        return Math.max(1L, StrictMath.round(-meanMs * StrictMath.log(u)));
    }
}

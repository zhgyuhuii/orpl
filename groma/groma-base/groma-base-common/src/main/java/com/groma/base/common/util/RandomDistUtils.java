package com.groma.base.common.util;

import java.util.function.DoubleSupplier;

/**
 * 确定性伪随机分布采样工具（落实设计书 2.1.1 节 RandomDistUtils）
 * <p>
 * 全部方法只依赖调用方传入的随机源（通常是任务级种子派生的 {@code DeterministicRngStream}），
 * 自身不持有任何随机状态，因此同一种子、同一调用序列必然得到同一结果，存档续跑也能逐值复现。
 * </p>
 */
public final class RandomDistUtils {

    private RandomDistUtils() {
    }

    /** 均匀分布：[min, max) */
    public static double uniform(DoubleSupplier random, double min, double max) {
        requireUsable(random);
        if (max <= min) {
            throw new IllegalArgumentException("均匀分布要求 max > min，实际 min=" + min + ", max=" + max);
        }
        return min + random.getAsDouble() * (max - min);
    }

    /** 正态分布：均值 mean、标准差 sd（random 需提供标准正态样本） */
    public static double normal(DoubleSupplier random, double mean, double sd) {
        requireUsable(random);
        if (sd <= 0.0D || Double.isNaN(sd)) {
            throw new IllegalArgumentException("正态分布要求标准差大于 0，实际 sd=" + sd);
        }
        return mean + random.getAsDouble() * sd;
    }

    /** 指数分布：均值 mean（random 提供 [0,1) 均匀样本） */
    public static double exponential(DoubleSupplier random, double mean) {
        requireUsable(random);
        if (mean <= 0.0D || Double.isNaN(mean)) {
            throw new IllegalArgumentException("指数分布要求均值大于 0，实际 mean=" + mean);
        }
        double u = Math.max(Double.MIN_VALUE, random.getAsDouble());
        return -mean * StrictMath.log(u);
    }

    /** 三角分布：下限 min、众数 mode、上限 max（random 提供 [0,1) 均匀样本） */
    public static double triangular(DoubleSupplier random, double min, double mode, double max) {
        requireUsable(random);
        if (!(max > min) || mode < min || mode > max) {
            throw new IllegalArgumentException(
                    "三角分布要求 min <= mode <= max 且 max > min，实际 min=" + min + ", mode=" + mode + ", max=" + max);
        }
        double u = random.getAsDouble();
        double fc = (mode - min) / (max - min);
        if (u < fc) {
            return min + StrictMath.sqrt(u * (max - min) * (mode - min));
        }
        return max - StrictMath.sqrt((1.0D - u) * (max - min) * (max - mode));
    }

    /**
     * 多项式累积概率 CDF 采样：按各组概率累积分布返回命中组下标；概率和不完整时按归一化处理
     */
    public static int sampleCdf(DoubleSupplier random, double[] groupProbabilities) {
        requireUsable(random);
        if (groupProbabilities == null || groupProbabilities.length == 0) {
            throw new IllegalArgumentException("分组概率数组不能为空");
        }
        double total = 0.0D;
        for (double p : groupProbabilities) {
            if (p < 0.0D || Double.isNaN(p)) {
                throw new IllegalArgumentException("分组概率必须是非负有限数");
            }
            total += p;
        }
        if (total <= 0.0D) {
            return groupProbabilities.length - 1;
        }
        double r = random.getAsDouble() * total;
        double acc = 0.0D;
        for (int i = 0; i < groupProbabilities.length; i++) {
            acc += groupProbabilities[i];
            if (r < acc) {
                return i;
            }
        }
        return groupProbabilities.length - 1;
    }

    private static void requireUsable(DoubleSupplier random) {
        if (random == null) {
            throw new IllegalArgumentException("随机源不能为空");
        }
    }
}

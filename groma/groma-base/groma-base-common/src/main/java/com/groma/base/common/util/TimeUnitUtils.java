package com.groma.base.common.util;

import java.util.Locale;

/**
 * 统一时间换算与步长修正器（落实 BE-MDL-004，彻底规避存量 BE-05 两处时长计算错误）
 */
public final class TimeUnitUtils {

    private TimeUnitUtils() {
    }

    public static long toMillis(double value, String unit) {
        if (value <= 0.0D || Double.isNaN(value)) {
            return 0L;
        }
        String u = unit == null ? "ms" : unit.trim().toLowerCase(Locale.ROOT);
        double factor = switch (u) {
            case "ns" -> 1.0e-6D;
            case "us" -> 1.0e-3D;
            case "ms", "毫秒" -> 1.0D;
            case "s", "sec", "秒" -> 1_000.0D;
            case "m", "min", "分", "分钟" -> 60_000.0D;
            case "h", "hour", "时", "小时" -> 3_600_000.0D;
            case "d", "day", "天", "日" -> 86_400_000.0D;
            default -> throw new IllegalArgumentException("未知时间单位: " + unit);
        };
        return Math.max(1L, StrictMath.round(value * factor));
    }

    public static long resolveFinalStepMs(long configuredStepMs, Long minModelAccuracyMs) {
        if (configuredStepMs <= 0L) {
            throw new IllegalArgumentException("configuredStepMs 必须大于 0");
        }
        if (minModelAccuracyMs == null || minModelAccuracyMs <= 0L) {
            return configuredStepMs;
        }
        return Math.min(configuredStepMs, minModelAccuracyMs);
    }

    public static long computeExecutionPeriod(Long accuracyMs, long finalStepMs) {
        if (accuracyMs == null || accuracyMs <= 0L) {
            return 1L;
        }
        return Math.max(1L, (accuracyMs + finalStepMs - 1L) / finalStepMs);
    }

    public static long stepCountToMillis(double stepCount, long finalStepMs) {
        if (stepCount <= 0.0D) {
            return finalStepMs;
        }
        return Math.max(finalStepMs, StrictMath.round(stepCount * finalStepMs));
    }
}

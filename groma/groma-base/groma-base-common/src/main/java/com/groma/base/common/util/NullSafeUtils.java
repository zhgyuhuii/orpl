package com.groma.base.common.util;

import java.util.function.Function;

/**
 * 锚点链式安全取值工具（支撑 R28 fixed 锚点路径透传）
 */
public final class NullSafeUtils {

    private NullSafeUtils() {
    }

    public static <S, T> T getOrDefault(S source, Function<S, T> getter, T defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        T val = getter.apply(source);
        return val != null ? val : defaultValue;
    }
}

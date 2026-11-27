package com.groma.base.engine;

import java.util.function.BooleanSupplier;

/**
 * 暂停条件（一次性触发后自动失效）与终止条件（持续有效）求值器（落实标准 12.5.3 & BE-SCH-006）
 */
public final class StopConditionEvaluator {

    private final BooleanSupplier pauseRule;
    private final BooleanSupplier stopRule;
    private boolean oneShotPauseConsumed = false;

    public StopConditionEvaluator(BooleanSupplier pauseRule, BooleanSupplier stopRule) {
        this.pauseRule = pauseRule;
        this.stopRule = stopRule;
    }

    public boolean shouldPauseNow() {
        if (oneShotPauseConsumed || pauseRule == null) {
            return false;
        }
        if (pauseRule.getAsBoolean()) {
            oneShotPauseConsumed = true;
            return true;
        }
        return false;
    }

    public boolean shouldTerminateNow() {
        return stopRule != null && stopRule.getAsBoolean();
    }
}

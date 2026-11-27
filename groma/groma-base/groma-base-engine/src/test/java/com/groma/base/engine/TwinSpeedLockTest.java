package com.groma.base.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 严格对应 CK-BE-SCH-004-1 & 2 及 CK-BE-SCH-006-1 & 2
 */
class TwinSpeedLockTest {

    @Test
    void lockAndRestoreToLatestRequestedSpeed() {
        SpeedManager sm = new SpeedManager(1000L);
        sm.onTwinStarted();
        Assertions.assertFalse(sm.requestFullSpeed(), "孪生运行期禁止全速");
        sm.requestSpeed(4.0D);
        sm.applyAtStepBoundaryAndComputeWaitMs();
        Assertions.assertEquals(1.0D, sm.getAppliedSpeed(), "孪生运行期锁定 1x 倍速");

        sm.onTwinFinished();
        sm.applyAtStepBoundaryAndComputeWaitMs();
        Assertions.assertEquals(4.0D, sm.getAppliedSpeed(), "孪生结束后恢复为用户最新请求的 4x 倍速");
    }

    @Test
    void nestedTwinProcesses() {
        SpeedManager sm = new SpeedManager(1000L);
        sm.requestSpeed(2.0D);
        sm.onTwinStarted();
        sm.onTwinStarted();
        sm.onTwinFinished();
        sm.applyAtStepBoundaryAndComputeWaitMs();
        Assertions.assertEquals(1.0D, sm.getAppliedSpeed(), "重叠孪生过程未全部结束前仍保持 1x");
        sm.onTwinFinished();
        sm.applyAtStepBoundaryAndComputeWaitMs();
        Assertions.assertEquals(2.0D, sm.getAppliedSpeed());
    }

    @Test
    void pauseIsOneShotWhileStopIsPersistent() {
        StopConditionEvaluator eval = new StopConditionEvaluator(() -> true, () -> true);
        Assertions.assertTrue(eval.shouldPauseNow());
        Assertions.assertFalse(eval.shouldPauseNow(), "一次性暂停条件触发后必须自动失效");
        Assertions.assertTrue(eval.shouldTerminateNow());
        Assertions.assertTrue(eval.shouldTerminateNow(), "终止条件持续有效");
    }
}

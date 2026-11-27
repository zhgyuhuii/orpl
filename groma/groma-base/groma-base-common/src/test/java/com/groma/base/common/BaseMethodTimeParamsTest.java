package com.groma.base.common;

import com.groma.base.common.util.SeededSampler;
import com.groma.base.common.util.TimeUnitUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class BaseMethodTimeParamsTest {

    @Test
    void exponentialDurationRespectsDurationUnit() {
        // CK-BE-MDL-004-1：持续时间单位为秒、均值 2 -> 10,000 次抽样平均值落在 [1900, 2100] 毫秒之间
        SeededSampler sampler = new SeededSampler(20261124L);
        long sum = 0L;
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            sum += sampler.sampleExponentialMs(2.0D, "s", 1000L, null);
        }
        double avg = (double) sum / n;
        Assertions.assertTrue(avg >= 1900.0D && avg <= 2100.0D, "指数分布采样均值应在 2000ms 左右，实际: " + avg);
    }

    @Test
    void stepCountDurationUsesCorrectedStep() {
        // CK-BE-MDL-004-2：设定步长 1000ms，最小精度 100ms，某过程时长 "10 步" -> 换算结果为 1000ms
        long finalStep = TimeUnitUtils.resolveFinalStepMs(1_000L, 100L);
        Assertions.assertEquals(100L, finalStep);
        Assertions.assertEquals(1_000L, TimeUnitUtils.stepCountToMillis(10.0D, finalStep));
    }

    @Test
    void periodFormula() {
        // CK-BE-MDL-004-3：步长 2000、精度 5000 时 P 为 3；精度 500 时 P 为 1
        Assertions.assertEquals(3L, TimeUnitUtils.computeExecutionPeriod(5_000L, 2_000L));
        Assertions.assertEquals(1L, TimeUnitUtils.computeExecutionPeriod(500L, 2_000L));
    }

    @Test
    void invalidDistributionParamsFallBackWithWarning() {
        // CK-BE-MDL-004-4：三种分布各给非法参数 -> 时长回退为一个步长且记录警告，无异常抛出
        SeededSampler sampler = new SeededSampler(1L);
        List<String> warnings = new ArrayList<>();
        Assertions.assertEquals(500L, sampler.sampleUniformMs(10.0D, 5.0D, "s", 500L, warnings::add));
        Assertions.assertEquals(500L, sampler.sampleNormalMs(-1.0D, 0.0D, "s", 500L, warnings::add));
        Assertions.assertEquals(500L, sampler.sampleExponentialMs(0.0D, "s", 500L, warnings::add));
        Assertions.assertEquals(3, warnings.size());
    }
}

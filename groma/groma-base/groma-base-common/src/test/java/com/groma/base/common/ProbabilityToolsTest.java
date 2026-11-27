package com.groma.base.common;

import com.groma.base.common.util.SeededSampler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ProbabilityToolsTest {

    @Test
    void independentProbabilities() {
        // CK-BE-EVT-005-1：两条独立 0.5 关系，10,000 次抽样四种组合各占 22% ~ 28%
        SeededSampler s = new SeededSampler(42L);
        int tt = 0, tf = 0, ft = 0, ff = 0;
        int total = 10_000;
        for (int i = 0; i < total; i++) {
            boolean a = s.hitIndependent(0.5D);
            boolean b = s.hitIndependent(0.5D);
            if (a && b) tt++;
            else if (a) tf++;
            else if (b) ft++;
            else ff++;
        }
        for (int c : new int[]{tt, tf, ft, ff}) {
            Assertions.assertTrue(c >= 2200 && c <= 2800, "组合频次异常: " + c);
        }
    }

    @Test
    void groupedProbabilities() {
        // CK-BE-EVT-005-2：同标签 0.3 与 0.7，每次恰好命中一条且占比约 30% / 70%
        SeededSampler s = new SeededSampler(99L);
        int hit0 = 0, hit1 = 0;
        for (int i = 0; i < 10_000; i++) {
            int idx = s.sampleGroupedCdf(new double[]{0.3D, 0.7D});
            if (idx == 0) hit0++;
            else hit1++;
        }
        Assertions.assertEquals(10_000, hit0 + hit1);
        Assertions.assertTrue(hit0 >= 2750 && hit0 <= 3250);
    }

    @Test
    void sameSeedSameSequence() {
        // CK-BE-EVT-005-3：相同种子抽样序列完全一致
        SeededSampler s1 = new SeededSampler(777L);
        SeededSampler s2 = new SeededSampler(777L);
        List<Long> seq1 = new ArrayList<>(), seq2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            seq1.add(s1.sampleExponentialMs(3.0D, "s", 100L, null));
            seq2.add(s2.sampleExponentialMs(3.0D, "s", 100L, null));
        }
        Assertions.assertEquals(seq1, seq2);
    }
}

package com.groma.studio.naming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ExplicitEncodeClassifier 词形鉴别回归（code5.html 3.5 收割）：
 * 手写英文放行 / 平台自动拼音码回落 / 技术缩写白名单 / 双语料真实自动码全拒。
 */
class ExplicitEncodeClassifierTest {

    @Test
    void genuineUserEnglishEncodesAreAccepted() {
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("floodWall"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("uav_patrol"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("CruiseSpeed"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("route-plan"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("stationWaterDepth"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("targetArea2"));
    }

    @Test
    void platformAutoShortcutCodesAreRejected() {
        // 双语料真实自动码形态（12.2 H4 取证：encode 100% 为平台自动码）
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("dxlx1"));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("gccy1"));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("znsczzjhznt1"));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("wb12"));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("gldx"));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("wrjsydl"));
        // 纯数字/空串/null/非法字符
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode(null));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode(""));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("1abc"));
        Assertions.assertFalse(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("含中文"));
    }

    @Test
    void techAbbreviationsAndShortWordsAreNotMisjudged() {
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("id"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("srcDst"));
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("sql"));
        // 有元音的短词非白名单也放行
        Assertions.assertTrue(ExplicitEncodeClassifier.isGenuineUserEnglishEncode("area"));
    }
}

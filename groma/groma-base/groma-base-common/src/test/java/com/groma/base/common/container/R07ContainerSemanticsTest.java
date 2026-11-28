package com.groma.base.common.container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * R07 单向关联容器语义回归（code5.html 3.1 收割增强）：
 * 1..1 必选基数置空拦截 / ra-rv-rx 事件三态 / 构建期静默对称 / 存档导出恢复 / 集合基数上下限。
 */
class R07ContainerSemanticsTest {

    @Test
    void singleRefEmitsRaRvRxByTransition() {
        List<String> events = new ArrayList<>();
        SingleRef<String> ref = new SingleRef<>((code, t) -> events.add(code + ":" + t));
        ref.linkTo("A");
        ref.linkTo("B");
        ref.linkTo(null);
        Assertions.assertEquals(List.of("ra:A", "rv:B", "rx:B"), events, "事件三态：建立/改指/解除");
        Assertions.assertFalse(ref.isPresent());
    }

    @Test
    void requiredRefRejectsRuntimeNullButAllowsSilentPath() {
        AtomicInteger violations = new AtomicInteger();
        SingleRef<String> ref = new SingleRef<>((code, t) -> { }, true, violations::incrementAndGet);
        ref.silentLinkTo("init");           // 构建期静默：合法
        Assertions.assertEquals("init", ref.get());
        ref.linkTo(null);                    // 运行期置空必选：拦截
        Assertions.assertEquals(1, violations.get(), "1..1 置空应记基数违规");
        Assertions.assertEquals("init", ref.get(), "被拦截的置空不得生效");
        ref.linkTo("next");                  // 正常改指放行
        Assertions.assertEquals("next", ref.get());
    }

    @Test
    void archiveExportImportIsSilentSymmetric() {
        List<String> events = new ArrayList<>();
        SingleRef<String> ref = new SingleRef<>((code, t) -> events.add(code));
        ref.linkTo("A");
        events.clear();
        // 模拟存档往返：导出 → 新容器静默恢复
        String exported = ref.exportTarget();
        SingleRef<String> restored = new SingleRef<>((code, t) -> events.add(code));
        restored.importTarget(exported);
        Assertions.assertEquals("A", restored.get());
        Assertions.assertTrue(events.isEmpty(), "存档恢复必须静默（不重放事件）");
    }

    @Test
    void multiRefSetEnforcesBoundsAndSilentRestore() {
        List<String> events = new ArrayList<>();
        MultiRefSet<String> set = new MultiRefSet<>(1, 2, (code, t) -> events.add(code + ":" + t));
        set.addLink("A");
        set.addLink("B");
        Assertions.assertThrows(IllegalStateException.class, () -> set.addLink("C"), "超上限应拒绝");
        Assertions.assertEquals(2, set.size());
        set.removeLink("A");
        Assertions.assertThrows(IllegalStateException.class, () -> set.removeLink("B"), "降破下限应拒绝");
        Assertions.assertEquals(List.of("ra:A", "ra:B", "rx:A"), events);

        // 存档对称：导出顺序 + 静默批量恢复（无事件、不受基数拦截）
        List<String> exported = set.exportOrderedList();
        MultiRefSet<String> restored = new MultiRefSet<>(99, 2, (code, t) -> events.add(code));
        restored.silentAddAll(exported);
        Assertions.assertEquals(List.of("B"), restored.exportOrderedList());
        Assertions.assertEquals(1, restored.size());
    }
}

package com.groma.base.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * T1.4 底座单元测试：集合工具的存量语义（数组标识一基序号 + 越界提示 [1,n]）与
 * 文本键值对互转（键值对转文本/文本转键值对，T1.2 补齐的工具）。
 * 存量语义依据：runtime/clazz/ArrayClazz（getAt/setAt/removeAt/addAt 的 index = value - 1，越界提示“范围应为：[1, n]”）。
 */
class BuiltinToolsCollectionsTest {

    private static List<Object> sample() {
        return new ArrayList<>(List.of("a", "b", "c"));
    }

    @Test
    void getByIndexIsOneBased() {
        Assertions.assertEquals("a", BuiltinTools.getByIndex((Object) sample(), 1L), "标识 1 应取第一个元素");
        Assertions.assertEquals("c", BuiltinTools.getByIndex((Object) sample(), 3L), "标识 3 应取最后一个元素");
    }

    @Test
    void setByIndexIsOneBased() {
        List<Object> list = sample();
        BuiltinTools.setByIndex(list, 2L, "B");
        Assertions.assertEquals("B", list.get(1), "标识 2 应写第二个元素");
    }

    @Test
    void removeAtIsOneBased() {
        List<Object> list = sample();
        Object removed = BuiltinTools.removeAt(list, 1L);
        Assertions.assertEquals("a", removed, "标识 1 应移除第一个元素");
        Assertions.assertEquals(List.of("b", "c"), list);
    }

    @Test
    void addAtAcceptsAppendPosition() {
        List<Object> list = sample();
        BuiltinTools.addAt(list, 4L, "d");
        Assertions.assertEquals(List.of("a", "b", "c", "d"), list, "标识 n+1 应为追加");
        BuiltinTools.addAt(list, 1L, "z");
        Assertions.assertEquals(List.of("z", "a", "b", "c", "d"), list, "标识 1 应插入到最前");
    }

    @Test
    void outOfRangeReportsOneBasedSpan() {
        GromaProcessException low = Assertions.assertThrows(GromaProcessException.class,
                () -> BuiltinTools.getByIndex((Object) sample(), 0L));
        Assertions.assertTrue(low.getMessage().contains("[1, 3]"), "越界提示应为一基范围 [1, n]: " + low.getMessage());
        GromaProcessException high = Assertions.assertThrows(GromaProcessException.class,
                () -> BuiltinTools.removeAt(sample(), 4L));
        Assertions.assertTrue(high.getMessage().contains("[1, 3]"), "越界提示应为一基范围 [1, n]: " + high.getMessage());
        GromaProcessException addHigh = Assertions.assertThrows(GromaProcessException.class,
                () -> BuiltinTools.addAt(sample(), 5L, "x"));
        Assertions.assertTrue(addHigh.getMessage().contains("[1, 4]"), "插入的越界上界应为 n+1: " + addHigh.getMessage());
    }

    @Test
    void textToKeyValueParsesJsonPairs() {
        String text = BuiltinTools.toJsonText(Map.of(1L, 2.5D, 3L, 0.0D));
        Map<Long, Double> parsed = BuiltinTools.textToKeyValue(text);
        Assertions.assertEquals(2, parsed.size(), "往返解析应还原全部键值对");
        Assertions.assertEquals(2.5D, parsed.get(1L));
        Assertions.assertEquals(0.0D, parsed.get(3L));
    }

    @Test
    void textToKeyValueToleratesBlankAndBadEntries() {
        Assertions.assertTrue(BuiltinTools.textToKeyValue(null).isEmpty(), "空输入应为空映射");
        Assertions.assertTrue(BuiltinTools.textToKeyValue("").isEmpty(), "空文本应为空映射");
        Assertions.assertTrue(BuiltinTools.textToKeyValue("not-json").isEmpty(), "非数组文本应为空映射");
        Map<Long, Double> partial = BuiltinTools.textToKeyValue(
                "[{\"key\":1,\"value\":2.0},{\"key\":\"bad\",\"value\":3.0},{\"key\":2,\"value\":4.5}]");
        Assertions.assertEquals(Map.of(1L, 2.0D, 2L, 4.5D), partial, "无法解析的项应跳过，其余保留");
    }

    @Test
    void textToKeyValueIntoMergesIntoHostMap() {
        List<Object> host = new ArrayList<>();
        host.add(new java.util.LinkedHashMap<Long, Double>());
        BuiltinTools.textToKeyValueInto(host.get(0), "[{\"key\":5,\"value\":1.0},{\"key\":6,\"value\":2.0}]");
        Assertions.assertEquals(Map.of(5L, 1.0D, 6L, 2.0D), host.get(0), "解析结果应并入 host 映射（画布组成线语义）");
        BuiltinTools.textToKeyValueInto(host.get(0), "[{\"key\":7,\"value\":3.0}]");
        Assertions.assertEquals(3, ((Map<?, ?>) host.get(0)).size(), "再次解析应累积而非覆盖");
    }
}

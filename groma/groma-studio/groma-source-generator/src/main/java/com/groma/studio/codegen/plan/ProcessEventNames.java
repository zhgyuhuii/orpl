package com.groma.studio.codegen.plan;

import java.util.Map;

/**
 * 方圆标准事件码 → 中文名（写进生成代码的 Javadoc 与模型语义索引）
 */
final class ProcessEventNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("a", "对象出现"), Map.entry("v", "值变化"), Map.entry("x", "对象消亡"),
            Map.entry("sa", "状态进入"), Map.entry("sx", "状态退出"),
            Map.entry("ra", "获得引用"), Map.entry("rv", "引用改向"), Map.entry("rx", "失去引用"),
            Map.entry("ma", "元素加入"), Map.entry("mx", "元素移除"),
            Map.entry("ps", "过程成功"), Map.entry("pf", "过程失败"), Map.entry("psf", "启动失败"),
            Map.entry("pcd", "超时完成"), Map.entry("pca", "提前完成"), Map.entry("paa", "过程终止"));

    private ProcessEventNames() {
    }

    /** 逗号分隔的事件码 → 中文名（如 "ra,rv" → "获得引用/引用改向"） */
    static String of(String eventCodes) {
        if (eventCodes == null || eventCodes.isBlank()) {
            return "事件";
        }
        StringBuilder sb = new StringBuilder();
        for (String code : eventCodes.split(",")) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(NAMES.getOrDefault(code.trim(), "事件 " + code.trim()));
        }
        return sb.append('(').append(eventCodes).append(')').toString();
    }
}

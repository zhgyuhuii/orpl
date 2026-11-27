package com.groma.studio.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 画布 29 种连线形状与 21 种事件代码显式全映射表（落实 BE-MDL-006 & NP-PRS-002）
 */
public final class CanvasMappingTables {

    public static final Map<String, String> SHAPE_TO_RELATION;
    public static final Map<String, String> EVENT_CODE_TO_STANDARD_TYPE;

    static {
        Map<String, String> shapes = new LinkedHashMap<>();
        shapes.put("edge-extend", "R01_EXTEND");
        shapes.put("edge-composition", "R02_COMPOSITION");
        shapes.put("edge-exhibition", "R03_EXHIBITION");
        shapes.put("edge-classification", "R04_CLASSIFICATION");
        shapes.put("edge-proxy", "R05_PROXY");
        shapes.put("edge-control", "R06_CONTROL_EXT"); // 平台扩展关系
        shapes.put("edge-unidirectional-association", "R07_UNIDIRECTIONAL_ASSOC");
        shapes.put("edge-bidirectional-association", "R08_BIDIRECTIONAL_ASSOC");
        shapes.put("edge-unidirectional-tagged", "R09_UNIDIRECTIONAL_TAGGED");
        shapes.put("edge-bidirectional-tagged", "R10_BIDIRECTIONAL_TAGGED");
        shapes.put("edge-consumption", "R11_CONSUMPTION");
        shapes.put("edge-result", "R12_RESULT");
        shapes.put("edge-in-change", "R13_IN_CHANGE");
        shapes.put("edge-out-change", "R14_OUT_CHANGE");
        shapes.put("edge-in-out", "R15_IN_OUT_NORMALIZED");
        shapes.put("edge-in-influence-nullable", "R16_IN_INFLUENCE_NULLABLE");
        shapes.put("edge-in-influence-nonnull", "R17_IN_INFLUENCE_NONNULL");
        shapes.put("edge-out-influence", "R18_OUT_INFLUENCE");
        shapes.put("edge-in-out-influence-nullable", "R19_IN_OUT_INFLUENCE_NULLABLE");
        shapes.put("edge-in-out-influence-nonnull", "R20_IN_OUT_INFLUENCE_NONNULL");
        shapes.put("edge-agent", "R21_AGENT_MAINTAIN");
        shapes.put("edge-condition", "R22_START_CONDITION");
        shapes.put("edge-invocation", "R23_INVOCATION");
        shapes.put("edge-invocation-self", "R24_INVOCATION_SELF");
        shapes.put("edge-overtime-exception", "R25_OVERTIME_SIGNAL");
        shapes.put("edge-undertime-exception", "R26_UNDERTIME_SIGNAL");
        shapes.put("edge-note", "R27_NOTE");
        shapes.put("edge-fixed", "R28_FIXED_BINDING");
        shapes.put("edge-link", "R00_GENERIC_LINK");
        SHAPE_TO_RELATION = Collections.unmodifiableMap(shapes);

        Map<String, String> events = new LinkedHashMap<>();
        events.put("a", "OBJECT_APPEARED");
        events.put("as", "OBJECT_APPEARED_IN_STATE");
        events.put("sa", "STATE_ENTERED");
        events.put("v", "VALUE_CHANGED");
        events.put("x", "OBJECT_CONSUMED");
        events.put("xs", "OBJECT_CONSUMED_FROM_STATE");
        events.put("sx", "STATE_EXITED");
        events.put("ma", "COLLECTION_MEMBER_ADDED");
        events.put("am", "ADDED_TO_COLLECTION");
        events.put("mx", "COLLECTION_MEMBER_REMOVED");
        events.put("xm", "REMOVED_FROM_COLLECTION");
        events.put("ra", "REFERENCE_LINKED");
        events.put("ar", "LINKED_BY_REFERENCE");
        events.put("rv", "REFERENCE_RETARGETED");
        events.put("vr", "RETARGETED_BY_REFERENCE");
        events.put("rx", "REFERENCE_UNLINKED");
        events.put("xr", "UNLINKED_BY_REFERENCE");
        events.put("pca", "PROCESS_COMPLETED_EARLY");
        events.put("pcd", "PROCESS_COMPLETED_OVERTIME");
        events.put("psf", "PROCESS_START_FAILED");
        events.put("paa", "PROCESS_ABORTED");
        EVENT_CODE_TO_STANDARD_TYPE = Collections.unmodifiableMap(events);
    }

    private CanvasMappingTables() {
    }

    public static String resolveEdgeShapeOrThrow(String shape, String viewId, String cellId) {
        String rel = SHAPE_TO_RELATION.get(shape);
        if (rel == null) {
            throw new IllegalArgumentException(
                    "[MDL-MAP-001] 未知连线形状: shape=" + shape + ", viewId=" + viewId + ", cellId=" + cellId);
        }
        return rel;
    }

    public static String resolveEventCodeOrThrow(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            return "NONE";
        }
        String mapped = EVENT_CODE_TO_STANDARD_TYPE.get(eventCode.trim());
        if (mapped == null) {
            throw new IllegalArgumentException("[MDL-MAP-002] 未知画布事件代码: " + eventCode);
        }
        return mapped;
    }
}

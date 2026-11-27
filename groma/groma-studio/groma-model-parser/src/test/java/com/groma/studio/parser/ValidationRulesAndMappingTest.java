package com.groma.studio.parser;

import com.groma.studio.ir.GromaModuleIR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * 覆盖 CK-BE-MDL-002-2（四种环）、CK-BE-MDL-003-1~3（编码）、CK-BE-MDL-005-1（规则拒绝与通过）、CK-BE-MDL-006-1~3（形状与事件映射）
 */
class ValidationRulesAndMappingTest {

    @Test
    void everyShapeAndEventCodeMappedAndUnknownRejected() {
        Assertions.assertEquals(29, CanvasMappingTables.SHAPE_TO_RELATION.size());
        Assertions.assertEquals(21, CanvasMappingTables.EVENT_CODE_TO_STANDARD_TYPE.size());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CanvasMappingTables.resolveEdgeShapeOrThrow("edge-illegal-foo", "v1", "c1"));
    }

    @Test
    void fourCycleTypesDetectedWithoutStackOverflow() {
        Map<String, List<String>> cycle = Map.of("A", List.of("B"), "B", List.of("C"), "C", List.of("A"));
        for (String kind : List.of("INHERITANCE", "MEMBER", "CALL", "PATH")) {
            Assertions.assertTrue(GraphCycleAndDepthGuard.detectCycleOrExcessDepth(kind, cycle, 32).isPresent());
        }
    }

    @Test
    void rejectsIllegalEncodeAndDeduplicatesDeterministically() {
        for (String illegal : List.of("1abc", "class", "a-b", "无人机")) {
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> UniqueIdentifierGuard.validateExplicitEncodeOrThrow(illegal));
        }
        UniqueIdentifierGuard guard = new UniqueIdentifierGuard();
        String c1 = guard.allocateDeterministicUnique("", "auto");
        String c2 = guard.allocateDeterministicUnique("", "auto");
        String c3 = guard.allocateDeterministicUnique("wb1", "auto");
        String c4 = guard.allocateDeterministicUnique("wb1", "auto");
        Assertions.assertEquals(4, java.util.Set.of(c1, c2, c3, c4).size());
    }

    @Test
    void operatorWithTimeConsumingChildRejectedAndValidModelAccepted() {
        StaticModelValidator validator = new StaticModelValidator();

        GromaModuleIR.ProcessIR badOperator = new GromaModuleIR.ProcessIR(
                "p_bad", "t1", "违规算子", "", false, false, false, "NONE", false, "FIXED",
                0.0D, null, null, "s", 1L, "", List.of(), List.of(), List.of(),
                List.of(new GromaModuleIR.SubProcessStepIR(0, "c1", "sub1", "耗时子过程", true, true, false, List.of(), null)),
                List.of(), List.of(), List.of(), false, "", "");
        GromaModuleIR badIr = new GromaModuleIR(
                "1.0", "m1", "m1", "坏模型", 1, "sha", 1000L, List.of(), List.of(),
                List.of(new GromaModuleIR.TypeIR("t1", "测试类", "", "PHYSICAL", null, List.of(), List.of(), List.of(badOperator))),
                List.of(), List.of(), Map.of());

        StaticModelValidator.ValidationSummary badSum = validator.validate(badIr);
        Assertions.assertFalse(badSum.passedWithoutErrors());
        Assertions.assertTrue(badSum.issues().stream().anyMatch(i -> "VAL-001".equals(i.ruleId())));

        // 修正父过程为 timeConsuming=true 后应通过校验
        GromaModuleIR.ProcessIR goodProc = new GromaModuleIR.ProcessIR(
                "p_ok", "t1", "合规父过程", "", true, false, false, "NONE", false, "FIXED",
                5.0D, null, null, "s", 1L, "", List.of(), List.of(), List.of(),
                List.of(new GromaModuleIR.SubProcessStepIR(0, "c1", "sub1", "耗时子过程", true, true, false, List.of(), null)),
                List.of(), List.of(), List.of(), false, "", "");
        GromaModuleIR goodIr = new GromaModuleIR(
                "1.0", "m1", "m1", "好模型", 1, "sha", 1000L, List.of(), List.of(),
                List.of(new GromaModuleIR.TypeIR("t1", "测试类", "", "PHYSICAL", null, List.of(), List.of(), List.of(goodProc))),
                List.of(), List.of(), Map.of());
        Assertions.assertTrue(validator.validate(goodIr).passedWithoutErrors());
    }
}

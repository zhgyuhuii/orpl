package com.groma.studio.parser;

import com.groma.studio.ir.GromaModuleIR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * VAL-006/007/008/010 四条补齐规则的单元测试（对照 docs/validation-rules.md 标准 7.3 / 8.2 / 10.4 / 9.6）
 */
class ValidationRulesVal006to010Test {

    private final StaticModelValidator validator = new StaticModelValidator();

    private static GromaModuleIR irOf(List<GromaModuleIR.ConstantIR> constants,
                                      List<GromaModuleIR.TypeIR> types,
                                      Map<String, GromaModuleIR.ViewGraphIR> views) {
        return new GromaModuleIR("1.0", "m1", "m1", "测试模型", 1, "sha", 1000L,
                constants, List.of(), types, List.of(), List.of(), views);
    }

    private static GromaModuleIR.TypeIR typeOf(String entityId, List<GromaModuleIR.ProcessIR> methods) {
        return new GromaModuleIR.TypeIR(entityId, "测试类-" + entityId, "", "PHYSICAL",
                null, List.of(), List.of(), methods);
    }

    private static GromaModuleIR.ProcessIR procOf(String entityId, List<GromaModuleIR.PostActionIR> posts) {
        return new GromaModuleIR.ProcessIR(
                entityId, "t1", "过程-" + entityId, "", false, false, false, "NONE", false, "FIXED",
                0.0D, null, null, "s", 1L, "", List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), posts, false, "", "");
    }

    // ---------------- VAL-006：定值禁连 ----------------

    @Test
    void constantConnectedToForbiddenRelationsRejected() {
        GromaModuleIR.NodeCellIR constantNode = new GromaModuleIR.NodeCellIR(
                "c_const", "const_min", "CONST", "node-constant", "最低电量", false, false, false, 0, 0);
        GromaModuleIR.NodeCellIR entityNode = new GromaModuleIR.NodeCellIR(
                "c_uav", "uav", "ENTITY", "node-entity", "无人机", false, false, false, 10, 10);
        GromaModuleIR.EdgeCellIR forbidden = new GromaModuleIR.EdgeCellIR(
                "e_bad", "link1", "R11_CONSUMPTION", "c_const", "c_uav", "");
        GromaModuleIR.EdgeCellIR allowed = new GromaModuleIR.EdgeCellIR(
                "e_ok", "link2", "R16_IN_INFLUENCE_NULLABLE", "c_const", "c_uav", "");
        GromaModuleIR.ViewGraphIR view = new GromaModuleIR.ViewGraphIR(
                "v1", true, List.of(constantNode, entityNode), List.of(forbidden, allowed));
        GromaModuleIR.ConstantIR constant = new GromaModuleIR.ConstantIR(
                "const_min", "最低电量", "double", "20.0", "%");

        var summary = validator.validate(irOf(List.of(constant), List.of(typeOf("t1", List.of())), Map.of("v1", view)));
        Assertions.assertFalse(summary.passedWithoutErrors());
        Assertions.assertTrue(summary.issues().stream().anyMatch(i ->
                "VAL-006".equals(i.ruleId()) && "e_bad".equals(i.elementId())));
        Assertions.assertTrue(summary.issues().stream().noneMatch(i ->
                "VAL-006".equals(i.ruleId()) && "e_ok".equals(i.elementId())));
    }

    // ---------------- VAL-007：显式标识符 ----------------

    @Test
    void explicitAsciiEncodeViolatingKeywordRejected() {
        var bad = validator.validate(irOf(List.of(), List.of(typeOf("class", List.of())), Map.of()));
        Assertions.assertTrue(bad.issues().stream().anyMatch(i -> "VAL-007".equals(i.ruleId())));

        var good = validator.validate(irOf(List.of(), List.of(typeOf("wb1", List.of())), Map.of()));
        Assertions.assertTrue(good.issues().stream().noneMatch(i -> "VAL-007".equals(i.ruleId())));

        // 纯中文原名不拦截（交由命名引擎确定性转译）
        var chinese = validator.validate(irOf(List.of(), List.of(typeOf("无人机编队", List.of())), Map.of()));
        Assertions.assertTrue(chinese.issues().stream().noneMatch(i -> "VAL-007".equals(i.ruleId())));
    }

    // ---------------- VAL-008：互斥分支概率和 ----------------

    @Test
    void mutexBranchProbabilitySumOutOfRangeRejected() {
        GromaModuleIR.PostActionIR bad1 = new GromaModuleIR.PostActionIR("CONSUME", "t1", "", 0.3D, "mutex_a");
        GromaModuleIR.PostActionIR bad2 = new GromaModuleIR.PostActionIR("RESULT", "t1", "", 0.5D, "mutex_a");
        var bad = validator.validate(irOf(List.of(), List.of(typeOf("t1", List.of(procOf("p1", List.of(bad1, bad2))))), Map.of()));
        Assertions.assertFalse(bad.passedWithoutErrors());
        Assertions.assertTrue(bad.issues().stream().anyMatch(i ->
                "VAL-008".equals(i.ruleId()) && i.severity() == StaticModelValidator.Severity.ERROR));

        GromaModuleIR.PostActionIR ok1 = new GromaModuleIR.PostActionIR("CONSUME", "t1", "", 0.5D, "mutex_b");
        GromaModuleIR.PostActionIR ok2 = new GromaModuleIR.PostActionIR("RESULT", "t1", "", 0.5D, "mutex_b");
        var good = validator.validate(irOf(List.of(), List.of(typeOf("t1", List.of(procOf("p1", List.of(ok1, ok2))))), Map.of()));
        Assertions.assertTrue(good.issues().stream().noneMatch(i ->
                "VAL-008".equals(i.ruleId()) && i.severity() == StaticModelValidator.Severity.ERROR));

        // 单成员命名组：WARNING 不阻断
        GromaModuleIR.PostActionIR single = new GromaModuleIR.PostActionIR("CONSUME", "t1", "", 0.3D, "mutex_c");
        var warn = validator.validate(irOf(List.of(), List.of(typeOf("t1", List.of(procOf("p1", List.of(single))))), Map.of()));
        Assertions.assertTrue(warn.passedWithoutErrors());
        Assertions.assertTrue(warn.issues().stream().anyMatch(i ->
                "VAL-008".equals(i.ruleId()) && i.severity() == StaticModelValidator.Severity.WARNING));
    }

    // ---------------- VAL-010：成员链深度 ----------------

    @Test
    void memberPathDepthBeyondLimitRejected() {
        StringBuilder deep = new StringBuilder("a");
        for (int i = 0; i < StaticModelValidator.MAX_MEMBER_PATH_DEPTH; i++) {
            deep.append(".f").append(i);
        }
        GromaModuleIR.FieldIR field = new GromaModuleIR.FieldIR(
                "f_depth", "深链字段", "boolean", "BASE", false, false, "", "", "", "",
                List.of(new GromaModuleIR.TriggerBindingIR(
                        "l1", "v", "ready && " + deep + " > 1", 1.0D, "", "p1", false)));
        GromaModuleIR.TypeIR ty = new GromaModuleIR.TypeIR("t1", "深链类", "", "PHYSICAL",
                null, List.of(), List.of(field), List.of());
        var bad = validator.validate(irOf(List.of(), List.of(ty), Map.of()));
        Assertions.assertFalse(bad.passedWithoutErrors());
        Assertions.assertTrue(bad.issues().stream().anyMatch(i -> "VAL-010".equals(i.ruleId())));

        StringBuilder shallow = new StringBuilder("a");
        for (int i = 0; i < 5; i++) {
            shallow.append(".f").append(i);
        }
        GromaModuleIR.FieldIR shallowField = new GromaModuleIR.FieldIR(
                "f_shallow", "浅链字段", "boolean", "BASE", false, false, "", "", "", "",
                List.of(new GromaModuleIR.TriggerBindingIR(
                        "l1", "v", shallow + " > 1", 1.0D, "", "p1", false)));
        GromaModuleIR.TypeIR shallowTy = new GromaModuleIR.TypeIR("t1", "浅链类", "", "PHYSICAL",
                null, List.of(), List.of(shallowField), List.of());
        var good = validator.validate(irOf(List.of(), List.of(shallowTy), Map.of()));
        Assertions.assertTrue(good.issues().stream().noneMatch(i -> "VAL-010".equals(i.ruleId())));
    }
}

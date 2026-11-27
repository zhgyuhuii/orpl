package com.groma.studio.parser;

import com.groma.studio.ir.CompilePlanIR;
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

    /**
     * VAL-007 补扫（code3.html E5b 收割）：建模面板显式填写的 encode 字段此前不在校验视野。
     * 语料实证存量 encode 均为平台自动码（dxlx1/wb1 等，合法标识符形态）——必须零误杀；
     * 用户显式填写 Java 保留字（如 class）必须拦截。
     */
    @Test
    void explicitEncodeFieldValidatedWithZeroCorpusFalsePositive() {
        // 保留字 encode：ERROR
        CompilePlanIR.TypeDefIR keywordType = new CompilePlanIR.TypeDefIR(
                "t_kw", "保留字类型", "", "", "TYPE", "", List.of(), List.of(),
                List.of(), List.of(), "class", List.of());
        var bad = validator.validate(irOf(List.of(), List.of(typeOf("t_ok", List.of())), Map.of())
                .withCompilePlan(new CompilePlanIR(1000L, List.of(keywordType),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));
        Assertions.assertTrue(bad.issues().stream().anyMatch(i ->
                "VAL-007".equals(i.ruleId()) && i.message().contains("class")));

        // 存量语料形态（合法标识符但为平台自动码）：零误杀
        CompilePlanIR.TypeDefIR corpusShaped = new CompilePlanIR.TypeDefIR(
                "t_corpus", "语料形态类型", "", "", "TYPE", "", List.of(), List.of(),
                List.of(new CompilePlanIR.MemberIR("m1", "自动码成员", "", "double", "BASE", "",
                        false, "", "", "", "", "", "", "", "double", "dxlx1")),
                List.of(), "gccy1", List.of());
        var good = validator.validate(irOf(List.of(), List.of(typeOf("t_ok2", List.of())), Map.of())
                .withCompilePlan(new CompilePlanIR(1000L, List.of(corpusShaped),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));
        Assertions.assertTrue(good.passedWithoutErrors());
        Assertions.assertTrue(good.issues().stream().noneMatch(i -> "VAL-007".equals(i.ruleId())));
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

    /**
     * VAL-008 补扫（code3.html E5a 收割）：复合过程内子过程调用分支（CompilePlanIR.NextIR）携带的
     * 互斥概率组此前不在校验视野，而出码侧恰按"同节点内同组"消费 CDF 采样。校验口径与消费口径对齐：
     * 组名作用域 = 拆分点（节点内），不同节点的同名组不合并。
     */
    @Test
    void nextBranchProbabilityGroupScannedPerNode() {
        CompilePlanIR.NextIR overA = new CompilePlanIR.NextIR(1, false, "", List.of(), "", "", 0L, 0.6D, "g_over");
        CompilePlanIR.NextIR overB = new CompilePlanIR.NextIR(2, false, "", List.of(), "", "", 0L, 0.7D, "g_over");
        CompilePlanIR.NodeIR overNode = new CompilePlanIR.NodeIR(0, "cell_over", "ent_over", "入口", "TOOL",
                "x.y", "", false, false, true, false, null, List.of(), List.of(), "",
                List.of(overA, overB), List.of(), 0, 0);
        CompilePlanIR.ProcessDefIR overProc = new CompilePlanIR.ProcessDefIR(
                "proc_over", "proc_over", "超和过程", "", "TYPE", "t1", "测试类-t1",
                true, false, false, 0, "FIXED", 1.0D, "s", null, null, "", null, null, null, null, null,
                1000L, "", "", List.of(), List.of(), List.of(overNode), List.of(0),
                List.of(), List.of());
        var bad = validator.validate(irOf(List.of(), List.of(typeOf("t1", List.of())), Map.of())
                .withCompilePlan(new CompilePlanIR(1000L, List.of(), List.of(overProc),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));
        Assertions.assertFalse(bad.passedWithoutErrors());
        Assertions.assertTrue(bad.issues().stream().anyMatch(i ->
                "VAL-008".equals(i.ruleId()) && i.severity() == StaticModelValidator.Severity.ERROR
                        && i.elementId().contains("pc0")));

        // 不同节点的同名组不跨节点合并：各 1 分支 → 两条 WARNING 而非 1 条 ERROR
        CompilePlanIR.NextIR single1 = new CompilePlanIR.NextIR(1, false, "", List.of(), "", "", 0L, 0.4D, "g_same");
        CompilePlanIR.NextIR single2 = new CompilePlanIR.NextIR(2, false, "", List.of(), "", "", 0L, 0.6D, "g_same");
        CompilePlanIR.NodeIR node1 = new CompilePlanIR.NodeIR(0, "c1", "e1", "甲", "TOOL",
                "x.y", "", false, false, true, false, null, List.of(), List.of(), "",
                List.of(single1), List.of(), 0, 0);
        CompilePlanIR.NodeIR node2 = new CompilePlanIR.NodeIR(1, "c2", "e2", "乙", "TOOL",
                "x.y", "", false, false, false, false, null, List.of(), List.of(), "",
                List.of(single2), List.of(), 0, 0);
        CompilePlanIR.ProcessDefIR twoNodeProc = new CompilePlanIR.ProcessDefIR(
                "proc_two", "proc_two", "双节点过程", "", "TYPE", "t1", "测试类-t1",
                true, false, false, 0, "FIXED", 1.0D, "s", null, null, "", null, null, null, null, null,
                1000L, "", "", List.of(), List.of(), List.of(node1, node2), List.of(0),
                List.of(), List.of());
        var scoped = validator.validate(irOf(List.of(), List.of(typeOf("t1", List.of())), Map.of())
                .withCompilePlan(new CompilePlanIR(1000L, List.of(), List.of(twoNodeProc),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));
        Assertions.assertTrue(scoped.passedWithoutErrors());
        Assertions.assertEquals(2, scoped.issues().stream().filter(i ->
                "VAL-008".equals(i.ruleId()) && i.severity() == StaticModelValidator.Severity.WARNING).count(),
                "同组名不同节点应各自给出单分支 WARNING");
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

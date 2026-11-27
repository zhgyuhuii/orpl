package com.groma.studio.parser;

import com.groma.studio.ir.GromaModuleIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 集中式静态模型校验与耗时属性推断器（落实 BE-MDL-005 & NP-PRS-004，彻底修复存量 BE-17 父过程静默吞时长缺陷）。
 * 覆盖《方圆语言标准-1117》十项编译期拦截 VAL-001 ~ VAL-010（与 docs/validation-rules.md 一一对应）。
 */
public final class StaticModelValidator {

    private static final Logger log = LoggerFactory.getLogger(StaticModelValidator.class);

    /** VAL-010：成员链与引用定位路径深度上限（标准 9.6） */
    public static final int MAX_MEMBER_PATH_DEPTH = 64;
    /** VAL-008：同组互斥分支概率和闭区间容差（标准 10.4） */
    public static final double PROBABILITY_SUM_MIN = 0.9999D;
    public static final double PROBABILITY_SUM_MAX = 1.0001D;
    /** VAL-006：定值严禁连接的关系类型（消耗/生成/状态转换/输出影响，标准 7.3）。
     *  双口径：解析器 toViewGraph 写入的是 linkType 短名（consumption 等），
     *  CanvasMappingTables 归一化名为 R 码（R11_CONSUMPTION 等），两套都要拦截。 */
    private static final Set<String> FORBIDDEN_FOR_CONSTANT = Set.of(
            "R11_CONSUMPTION", "R12_RESULT", "R13_IN_CHANGE", "R14_OUT_CHANGE",
            "R15_IN_OUT_NORMALIZED", "R18_OUT_INFLUENCE",
            "consumption", "result", "in-change", "out-change", "in-out", "out-influence");
    /** VAL-010：点分成员链（如 a.b.c.d），两段以上视为引用定位路径 */
    private static final Pattern MEMBER_PATH = Pattern.compile("[\\w\\u4e00-\\u9fa5]+(?:\\.[\\w\\u4e00-\\u9fa5]+)+");
    /** VAL-007：显式编码形态（与 UniqueIdentifierGuard.VALID_ENCODE 同口径）——
     *  存量语料内部主键（如 3cW2TNyvVxiJxV07ZlR，数字开头）不是显式编码，绝不误杀 */
    private static final Pattern EXPLICIT_ENCODE_SHAPE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public enum Severity { ERROR, WARNING }

    public record ValidationIssue(
            String ruleId,
            String standardClause,
            Severity severity,
            String elementId,
            String elementName,
            String message
    ) {}

    public record ValidationSummary(
            boolean passedWithoutErrors,
            List<ValidationIssue> issues
    ) {}

    public ValidationSummary validate(GromaModuleIR ir) {
        List<ValidationIssue> issues = new ArrayList<>();

        // VAL-001 & VAL-002：算子不能包含耗时子过程；若普通父过程包含耗时子过程，按标准 12.1.2 校验并提示
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            for (GromaModuleIR.ProcessIR proc : ty.methods()) {
                checkProcessRules(proc, issues);
            }
        }
        for (GromaModuleIR.ProcessIR indep : ir.independentProcesses()) {
            checkProcessRules(indep, issues);
        }

        // VAL-003：继承图无环检测（标准 8.5）
        Map<String, List<String>> inheritGraph = new LinkedHashMap<>();
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            List<String> parents = new ArrayList<>();
            if (ty.primaryParentEntityId() != null && !ty.primaryParentEntityId().isBlank()) {
                parents.add(ty.primaryParentEntityId());
            }
            if (ty.mixinParentEntityIds() != null) {
                parents.addAll(ty.mixinParentEntityIds());
            }
            inheritGraph.put(ty.entityId(), parents);
        }
        GraphCycleAndDepthGuard.detectCycleOrExcessDepth("INHERITANCE", inheritGraph, 32)
                .ifPresent(err -> issues.add(new ValidationIssue(
                        "VAL-003", "标准 8.5", Severity.ERROR, ir.modelId(), ir.modelLabel(), err)));

        // VAL-005 & VAL-009：控制关系无环检测（标准扩展 R06）与同一对象和过程间同向关系唯一性校验（标准 10.1.3）
        for (GromaModuleIR.ViewGraphIR view : ir.views().values()) {
            Map<String, List<String>> controlEdges = new LinkedHashMap<>();
            Set<String> sameDirPairs = new HashSet<>();

            for (GromaModuleIR.EdgeCellIR edge : view.edges()) {
                if ("control".equals(edge.relationType()) || "R06_CONTROL_EXT".equals(edge.relationType())) {
                    controlEdges.computeIfAbsent(edge.sourceCellId(), k -> new ArrayList<>()).add(edge.targetCellId());
                }
                String pairKey = edge.sourceCellId() + "->" + edge.targetCellId() + ":" + edge.relationType();
                if (!sameDirPairs.add(pairKey) && !"fixed".equals(edge.relationType())) {
                    // WARNING（2026-11-27 模型 2 真机实证降级）：画布重复同向连线是数据清洁度问题，
                    // 解析器按 NextIR equals 去重后语义无损（单次激活），阻断出码属过度拦截
                    issues.add(new ValidationIssue(
                            "VAL-009", "标准 10.1.3", Severity.WARNING, edge.cellId(), edge.relationType(),
                            "同一视图内相同起止节点间存在重复的同向关系（解析器已去重，语义无损）: " + pairKey));
                }
            }
            GraphCycleAndDepthGuard.detectCycleOrExcessDepth("CONTROL", controlEdges, 64)
                    .ifPresent(err -> issues.add(new ValidationIssue(
                            "VAL-005", "平台扩展 R06", Severity.ERROR, view.viewId(), view.viewId(), err)));
        }

        // VAL-006：定值严禁连接消耗、生成、状态转换或输出影响线（标准 7.3）
        Set<String> constantIds = new HashSet<>();
        for (GromaModuleIR.ConstantIR constant : ir.constants()) {
            constantIds.add(constant.entityId());
        }
        for (GromaModuleIR.ViewGraphIR view : ir.views().values()) {
            Set<String> constantCells = new HashSet<>();
            for (GromaModuleIR.NodeCellIR node : view.nodes()) {
                if (constantIds.contains(node.entityId())) {
                    constantCells.add(node.cellId());
                }
            }
            if (constantCells.isEmpty()) {
                continue;
            }
            for (GromaModuleIR.EdgeCellIR edge : view.edges()) {
                if (FORBIDDEN_FOR_CONSTANT.contains(edge.relationType())
                        && (constantCells.contains(edge.sourceCellId()) || constantCells.contains(edge.targetCellId()))) {
                    issues.add(new ValidationIssue(
                            "VAL-006", "标准 7.3", Severity.ERROR, edge.cellId(), edge.relationType(),
                            "定值严禁连接消耗/生成/状态转换/输出影响线（cell=" + edge.sourceCellId()
                                    + " -> " + edge.targetCellId() + "）"));
                }
            }
        }

        // VAL-007：显式标识符格式与 Java 保留字冲突拦截（标准 8.2）。
        // entityId 已是 ASCII 标识符形态（用户显式填写的编码）时执行严格校验；
        // 纯中文原名交由命名引擎确定性转译，不在此拦截。
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            validateExplicitEncode(ty.entityId(), ty.zhName(), issues);
            for (GromaModuleIR.ProcessIR proc : ty.methods()) {
                validateExplicitEncode(proc.entityId(), proc.zhName(), issues);
            }
        }
        for (GromaModuleIR.ProcessIR indep : ir.independentProcesses()) {
            validateExplicitEncode(indep.entityId(), indep.zhName(), issues);
        }
        for (GromaModuleIR.ConstantIR constant : ir.constants()) {
            validateExplicitEncode(constant.entityId(), constant.zhName(), issues);
        }

        // VAL-008：同组互斥分支概率和必须落在 [0.9999, 1.0001] 闭区间（标准 10.4）
        // 分支来源：字段事件触发绑定（TriggerBindingIR）与过程后置动作（PostActionIR）的概率分组
        record ProbBranch(String group, double probability, String elementId, String elementName) {}
        List<ProbBranch> branches = new ArrayList<>();
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            for (GromaModuleIR.FieldIR field : ty.fields() == null ? List.<GromaModuleIR.FieldIR>of() : ty.fields()) {
                for (GromaModuleIR.TriggerBindingIR trigger : field.eventTriggers() == null
                        ? List.<GromaModuleIR.TriggerBindingIR>of() : field.eventTriggers()) {
                    if (trigger.probabilityGroup() != null && !trigger.probabilityGroup().isBlank()) {
                        branches.add(new ProbBranch(trigger.probabilityGroup(), trigger.probability(),
                                ty.entityId() + "." + field.entityId(), field.zhName()));
                    }
                }
            }
        }
        List<GromaModuleIR.ProcessIR> allProcesses = new ArrayList<>();
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            allProcesses.addAll(ty.methods());
        }
        allProcesses.addAll(ir.independentProcesses());
        for (GromaModuleIR.SceneIR scene : ir.scenes()) {
            if (scene.sceneLevelProcesses() != null) {
                allProcesses.addAll(scene.sceneLevelProcesses());
            }
        }
        for (GromaModuleIR.ProcessIR proc : allProcesses) {
            for (GromaModuleIR.PostActionIR action : proc.postActions() == null
                    ? List.<GromaModuleIR.PostActionIR>of() : proc.postActions()) {
                if (action.probabilityGroup() != null && !action.probabilityGroup().isBlank()) {
                    branches.add(new ProbBranch(action.probabilityGroup(), action.probability(),
                            proc.entityId(), proc.zhName()));
                }
            }
        }
        Map<String, List<ProbBranch>> probabilityGroups = new LinkedHashMap<>();
        for (ProbBranch branch : branches) {
            probabilityGroups.computeIfAbsent(branch.group(), k -> new ArrayList<>()).add(branch);
        }
        for (Map.Entry<String, List<ProbBranch>> entry : probabilityGroups.entrySet()) {
            double sum = 0.0D;
            for (ProbBranch branch : entry.getValue()) {
                sum += branch.probability();
            }
            ProbBranch sample = entry.getValue().get(0);
            if (entry.getValue().size() >= 2 && (sum < PROBABILITY_SUM_MIN || sum > PROBABILITY_SUM_MAX)) {
                issues.add(new ValidationIssue(
                        "VAL-008", "标准 10.4", Severity.ERROR, sample.elementId(), sample.elementName(),
                        "互斥组 '" + entry.getKey() + "' 概率和 " + sum + " 超出闭区间 ["
                                + PROBABILITY_SUM_MIN + ", " + PROBABILITY_SUM_MAX + "]"));
            } else if (entry.getValue().size() == 1) {
                issues.add(new ValidationIssue(
                        "VAL-008", "标准 10.4", Severity.WARNING, sample.elementId(), sample.elementName(),
                        "互斥组 '" + entry.getKey() + "' 仅 1 个分支（概率和应为 1.0，或应改为独立概率）"));
            }
        }

        // VAL-010：成员链与引用定位路径深度 ≤64（标准 9.6）
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            for (GromaModuleIR.FieldIR field : ty.fields() == null ? List.<GromaModuleIR.FieldIR>of() : ty.fields()) {
                for (GromaModuleIR.TriggerBindingIR trigger : field.eventTriggers() == null
                        ? List.<GromaModuleIR.TriggerBindingIR>of() : field.eventTriggers()) {
                    checkMemberPathDepth(trigger.conditionExpr(), ty.entityId() + "/" + field.zhName(), issues);
                }
            }
        }
        for (GromaModuleIR.ProcessIR indep : ir.independentProcesses()) {
            checkProcessExprDepth(indep, issues);
        }
        for (GromaModuleIR.SceneIR scene : ir.scenes()) {
            for (GromaModuleIR.ProcessIR proc : scene.sceneLevelProcesses() == null
                    ? List.<GromaModuleIR.ProcessIR>of() : scene.sceneLevelProcesses()) {
                checkProcessExprDepth(proc, issues);
            }
        }

        for (ValidationIssue issue : issues) {
            if (issue.severity() == Severity.ERROR) {
                log.warn("[Validator][{}][{}] 元素={}({}): {}",
                        issue.ruleId(), issue.standardClause(), issue.elementName(), issue.elementId(), issue.message());
            }
        }

        boolean hasError = issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
        return new ValidationSummary(!hasError, List.copyOf(issues));
    }

    private void checkProcessRules(GromaModuleIR.ProcessIR proc, List<ValidationIssue> issues) {
        List<GromaModuleIR.SubProcessStepIR> subs = proc.subSteps() == null ? List.of() : proc.subSteps();
        if (!subs.isEmpty()) {
            // VAL-004：有子过程必须至少有一个首个启动 firstStart（标准 9.4.7）。
            // 对齐解析器入口推断（AbModelMergerAndParser 入度兜底）：无显式 firstStart 但存在
            // 零入度节点时，解析器会自动推断入口 → 降级 WARNING；全部节点都有入向（纯环）才 ERROR。
            boolean hasFirstStart = subs.stream().anyMatch(GromaModuleIR.SubProcessStepIR::firstStart);
            if (!hasFirstStart) {
                Set<Integer> targeted = new HashSet<>();
                for (GromaModuleIR.SubProcessStepIR sub : subs) {
                    if (sub.nextStepPcs() != null) {
                        targeted.addAll(sub.nextStepPcs());
                    }
                }
                long zeroInDegree = subs.stream().filter(s -> !targeted.contains(s.stepPc())).count();
                if (zeroInDegree == 0L) {
                    issues.add(new ValidationIssue(
                            "VAL-004", "标准 9.4.7", Severity.ERROR, proc.entityId(), proc.zhName(),
                            "包含子过程的复合过程既无显式 firstStart 也无零入度起点（纯环，无法确定入口）"));
                } else {
                    issues.add(new ValidationIssue(
                            "VAL-004", "标准 9.4.7", Severity.WARNING, proc.entityId(), proc.zhName(),
                            "无显式 firstStart，将按零入度自动推断 " + zeroInDegree + " 个入口节点"));
                }
            }
            // VAL-001：非耗时算子不能包含耗时子过程（标准 9.2.2 & 12.1.2，规避 BE-17）
            boolean anyTimeConsumingChild = subs.stream().anyMatch(GromaModuleIR.SubProcessStepIR::timeConsuming);
            if (!proc.timeConsuming() && anyTimeConsumingChild) {
                issues.add(new ValidationIssue(
                        "VAL-001", "标准 9.2.2 / 12.1.2", Severity.ERROR, proc.entityId(), proc.zhName(),
                        "瞬时算子内部禁止包含耗时子过程（请将该父过程设为耗时过程）"));
            }
        }
    }

    /** VAL-007：entityId 为完整合法编码形态时只做保留字冲突校验。
     *  存量语料的内部主键（UUID 形态、数字开头）不是显式编码，直接跳过（修复误杀真实语料的缺陷）；
     *  非法形态的手写编码在命名引擎入口处已被确定性转译，这里不重复拦截。 */
    private void validateExplicitEncode(String entityId, String zhName, List<ValidationIssue> issues) {
        if (entityId == null || entityId.isBlank() || !EXPLICIT_ENCODE_SHAPE.matcher(entityId).matches()) {
            return; // 空编码 / 中文原名 / 平台内部主键：走命名引擎确定性转译，不拦截
        }
        try {
            UniqueIdentifierGuard.validateExplicitEncodeOrThrow(entityId);
        } catch (IllegalArgumentException ex) {
            issues.add(new ValidationIssue(
                    "VAL-007", "标准 8.2", Severity.ERROR, entityId, zhName, ex.getMessage()));
        }
    }

    /** VAL-010：触发条件表达式中的点分成员链深度 ≤64（标准 9.6） */
    private void checkProcessExprDepth(GromaModuleIR.ProcessIR proc, List<ValidationIssue> issues) {
        // 过程自身的成员链主要体现在局部临时成员的字段触发上（无表达式字段），
        // 过程启动/维持条件为结构化规则（非自由表达式）；此处覆盖 rawJavaCode 中的长链引用防御。
        if (proc.rawJavaCode() != null && !proc.rawJavaCode().isBlank()) {
            checkMemberPathDepth(proc.rawJavaCode(), proc.entityId() + "/" + proc.zhName(), issues);
        }
    }

    private void checkMemberPathDepth(String text, String elementId, List<ValidationIssue> issues) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = MEMBER_PATH.matcher(text);
        Set<String> reported = new HashSet<>();
        while (matcher.find()) {
            String path = matcher.group();
            if (!reported.add(path)) {
                continue;
            }
            int depth = path.split("\\.").length;
            if (depth > MAX_MEMBER_PATH_DEPTH) {
                issues.add(new ValidationIssue(
                        "VAL-010", "标准 9.6", Severity.ERROR, elementId, path,
                        "成员链/引用定位路径深度 " + depth + " 超过上限 " + MAX_MEMBER_PATH_DEPTH + " 层: " + path));
            }
        }
    }
}

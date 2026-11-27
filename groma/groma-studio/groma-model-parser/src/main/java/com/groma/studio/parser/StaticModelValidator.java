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

/**
 * 集中式静态模型校验与耗时属性推断器（落实 BE-MDL-005 & NP-PRS-004，彻底修复存量 BE-17 父过程静默吞时长缺陷）
 */
public final class StaticModelValidator {

    private static final Logger log = LoggerFactory.getLogger(StaticModelValidator.class);

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
                    issues.add(new ValidationIssue(
                            "VAL-009", "标准 10.1.3", Severity.ERROR, edge.cellId(), edge.relationType(),
                            "同一视图内相同起止节点间存在重复的同向关系: " + pairKey));
                }
            }
            GraphCycleAndDepthGuard.detectCycleOrExcessDepth("CONTROL", controlEdges, 64)
                    .ifPresent(err -> issues.add(new ValidationIssue(
                            "VAL-005", "平台扩展 R06", Severity.ERROR, view.viewId(), view.viewId(), err)));
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
            // VAL-004：有子过程必须至少有一个首个启动 firstStart（标准 9.4.7）
            boolean hasFirstStart = subs.stream().anyMatch(GromaModuleIR.SubProcessStepIR::firstStart);
            if (!hasFirstStart) {
                issues.add(new ValidationIssue(
                        "VAL-004", "标准 9.4.7", Severity.ERROR, proc.entityId(), proc.zhName(),
                        "包含子过程的复合过程必须标记至少一个首个启动（firstStart）子过程"));
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
}

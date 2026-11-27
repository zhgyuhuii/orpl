package com.groma.studio.parser;

import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.ArgBindingIR;
import com.groma.studio.ir.CompilePlanIR.MemberPathIR;
import com.groma.studio.ir.CompilePlanIR.NodeIR;
import com.groma.studio.ir.CompilePlanIR.OperandIR;
import com.groma.studio.ir.CompilePlanIR.PostIR;
import com.groma.studio.ir.CompilePlanIR.ProcessDefIR;
import com.groma.studio.ir.CompilePlanIR.TriggerIR;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.ir.GromaTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 真实语料回归：XX-a.json + XX-b.json 双流合并解析的结构断言（CK-NP-PRS-001-1 ~ 4）。
 * 语料目录由系统属性 groma.corpus 指定，未指定时自动探测仓库内 docs/model。
 */
class AbModelMergerAndParserTest {

    static Optional<Path> corpusDir() {
        String configured = System.getProperty("groma.corpus");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(Path.of(configured));
        }
        Path cwd = Path.of("").toAbsolutePath();
        for (Path p = cwd; p != null; p = p.getParent()) {
            Path candidate = p.resolve("docs/model");
            if (Files.isRegularFile(candidate.resolve("data-a.json"))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    static GromaModuleIR parseCorpus() {
        Path dir = corpusDir().orElseThrow(() -> new IllegalStateException(
                "未找到语料目录 docs/model（可用 -Dgroma.corpus=<目录> 指定）"));
        return new AbModelMergerAndParser().parse(dir.resolve("data-a.json"), dir.resolve("data-b.json"));
    }

    @Test
    void realCorpusParsesDeterministicallyWithCompletePlan() {
        GromaModuleIR ir = parseCorpus();
        GromaModuleIR again = parseCorpus();
        Assertions.assertEquals(ir.sourceDigest(), again.sourceDigest(), "同一份语料两次解析摘要必须一致");
        Assertions.assertEquals(ir.compilePlan(), again.compilePlan(), "同一份语料两次解析编译计划逐字段相等");

        Assertions.assertEquals("应急决策推演-关联副本-1029", ir.modelLabel());
        Assertions.assertEquals(647, ir.modelVersion());
        CompilePlanIR plan = ir.compilePlan();
        // 编译计划落盘为核对产物：target/compile-plan-dump.txt
        try {
            Files.createDirectories(Path.of("target"));
            Files.writeString(Path.of("target", "compile-plan-dump.txt"), describe(plan), StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
        }

        // 场景来自组织树权威列表（保持组织树顺序），不得混入 entities 里的“默认场景”占位实体
        List<String> scenes = plan.types().stream().filter(t -> "SCENE".equals(t.kind())).map(t -> t.zhName()).toList();
        Assertions.assertEquals(2, scenes.size());
        Assertions.assertTrue(scenes.contains("场景1") && scenes.contains("场景2"), scenes.toString());

        // 领域对象类型：对象类型 20 个 + 通用类型 4 个 + 通用组件提供的 文件服务 / 文件信息
        long domainTypes = plan.types().stream().filter(t -> "TYPE".equals(t.kind())).count();
        Assertions.assertEquals(26L, domainTypes, "领域对象类型数量");
        Assertions.assertTrue(plan.types().stream().noneMatch(t -> "循环处理".equals(t.zhName())),
                "内置组件提供的类型不进入业务模型");
        Assertions.assertEquals(10L, ir.enums().size(), "枚举数量");
        Assertions.assertEquals(5L, plan.associationTypes().size(), "关联类型数量");

        // 过程定义以设计视图去重
        Assertions.assertEquals(39L, plan.processDefs().stream().filter(d -> !d.nodes().isEmpty()).count(),
                "含数据流图的过程定义数量");
        Assertions.assertTrue(plan.processDefs().size() > 39L, "另含无设计视图的叶过程");

        ProcessDefIR stopAtStation = plan.processDefs().stream()
                .filter(d -> "停站".equals(d.zhName()) && "列车".equals(d.ownerZhName())).findFirst().orElseThrow();
        Assertions.assertTrue(stopAtStation.timeConsuming());
        Assertions.assertEquals(1, stopAtStation.singletonMode());
        Assertions.assertEquals(1, stopAtStation.params().size(), "停站只有一个形参：车站与停靠列车");
        Assertions.assertEquals("车站与停靠列车", stopAtStation.params().get(0).zhName());
        Assertions.assertEquals("IN", stopAtStation.params().get(0).direction());
        Assertions.assertFalse(stopAtStation.locals().isEmpty(), "停站含局部量：等待时长 / 允许离站");

        // 耗时子过程沿区间前进：入参绑定齐全
        ProcessDefIR advance = plan.processDefs().stream()
                .filter(d -> "沿区间前进".equals(d.zhName())).findFirst().orElseThrow();
        Assertions.assertEquals(6, advance.params().size());
        Assertions.assertTrue(advance.params().stream().anyMatch(p -> "IN_OUT".equals(p.direction()) && "已行驶距离".equals(p.zhName())),
                "已行驶距离是双向形参");

        // 事件接线：列车.列车状态 进入 停站 → 启动 停站过程
        TriggerIR stateTrigger = plan.triggers().stream()
                .filter(t -> "sa".equals(t.eventCode()) && "停站".equals(t.stateLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, stateTrigger.contextPath().size(), "事件源是成员 列车 的成员 列车状态");
        ProcessDefIR stopDef = plan.processDefs().stream()
                .filter(d -> d.defKey().equals(stateTrigger.targetDefKey())).findFirst().orElseThrow();
        Assertions.assertEquals("停站", stopDef.zhName());

        // 默认启动：线路与列车 创建后启动 形成车站与列车的关联
        boolean defaultStart = plan.defaultStarts().stream().anyMatch(ds ->
                ds.targetDefKey().equals(plan.processDefs().stream()
                        .filter(d -> "形成车站与列车的关联".equals(d.zhName())).findFirst().orElseThrow().defKey()));
        Assertions.assertTrue(defaultStart, "形成车站与列车的关联是默认启动过程");

        // 命名实例对象图：场景2 的 4 个成员绑定到命名实例
        CompilePlanIR.InstanceIR scene2 = plan.instances().stream()
                .filter(i -> i.spec() instanceof CompilePlanIR.ObjectSpec obj && "场景2".equals(i.zhName()))
                .findFirst().orElseThrow();
        Assertions.assertEquals(4, ((CompilePlanIR.ObjectSpec) scene2.spec()).members().size());

        // 解析期没有把结构弄丢：无数据流图的叶过程必须带 javaCode 或为空体
        for (ProcessDefIR def : plan.processDefs()) {
            if (def.nodes().isEmpty()) {
                Assertions.assertTrue(def.javaCode() != null);
            }
        }
    }

    @Test
    void operatorAndWriteBackBindingsAreResolved() {
        CompilePlanIR plan = parseCorpus().compilePlan();
        ProcessDefIR advance = plan.processDefs().stream()
                .filter(d -> "沿区间前进".equals(d.zhName())).findFirst().orElseThrow();
        // 乘 → 除 → 减 → 取小 → 等于 的调用顺序
        List<String> kinds = advance.nodes().stream().map(NodeIR::toolKey).toList();
        Assertions.assertTrue(kinds.contains("arithmetic.multiply"));
        Assertions.assertTrue(kinds.contains("arithmetic.divide"));
        Assertions.assertTrue(kinds.contains("compare.eq"));
        Assertions.assertEquals(3L, advance.nodes().stream().filter(NodeIR::slow).count(),
                "沿区间前进 含 3 个画布上标记为时间有关的工具节点（按值递增 / 按值递减 / 按值递增）");
        // 耗时过程普遍用回边循环建模：地表排水 的末尾调用 积水变化 后回到起点
        ProcessDefIR drainage = plan.processDefs().stream()
                .filter(d -> "地表排水".equals(d.zhName())).findFirst().orElseThrow();
        Assertions.assertTrue(drainage.timeConsuming(), "实体 timeRelated=RELATED 必须被识别为耗时过程");

        // 除 的结果写回 本步计划行驶距离（局部量）
        NodeIR divide = advance.nodes().stream()
                .filter(n -> "arithmetic.divide".equals(n.toolKey()) && !n.writes().isEmpty()).findFirst().orElseThrow();
        Assertions.assertEquals("c", divide.writes().get(0).argName(), "除 的输出端口 c 绑定了写出目标");
        Assertions.assertTrue(divide.writes().get(0).operand() instanceof CompilePlanIR.LocalIR local
                && "本步计划行驶距离".equals(local.zhName()), "写出目标：本步计划行驶距离（模板成员 → 局部量）");

        // 积水变化：大于 → 车站有积水 / 车站无积水，并把 积水状态 转到 积水 / 无积水
        ProcessDefIR waterlogging = plan.processDefs().stream()
                .filter(d -> "积水变化".equals(d.zhName())).findFirst().orElseThrow();
        NodeIR greater = waterlogging.nodes().stream().filter(n -> "compare.gt".equals(n.toolKey())).findFirst().orElseThrow();
        Assertions.assertEquals(2, greater.nexts().size());
        Assertions.assertTrue(greater.nexts().stream().allMatch(n -> n.conditional()), "两条后继都带线上判断");
        long statePosts = waterlogging.nodes().stream().flatMap(n -> n.postActions().stream())
                .filter(p -> "OUT_CHANGE".equals(p.kind())).count();
        Assertions.assertEquals(2L, statePosts, "车站有积水 → 积水，车站无积水 → 无积水");

        // 上游输出喂下游：乘 的输出直接进入 除 的 a 入参
        NodeIR multiply = advance.nodes().stream().filter(n -> "arithmetic.multiply".equals(n.toolKey()))
                .min((x, y) -> Double.compare(x.y(), y.y())).orElseThrow();
        boolean feedsDivide = divide.inputs().stream().anyMatch(arg -> arg.operand() instanceof CompilePlanIR.NodeOutputIR out
                && out.cellId().equals(multiply.cellId()));
        Assertions.assertTrue(feedsDivide, "乘 的输出端口直接接到 除 的入参");
    }

    @Test
    void diagnosticsReportUnresolvableStructureInsteadOfThrowing() {
        CompilePlanIR plan = parseCorpus().compilePlan();
        for (String warning : plan.warnings()) {
            Assertions.assertFalse(warning.isBlank());
        }
        // 真实语料上不应出现“无法解析的操作数”级别的告警
        Assertions.assertTrue(plan.warnings().stream().noneMatch(w -> w.contains("不是可解析的操作数")),
                "真实语料的全部操作数都应可解析: " + plan.warnings());
    }

    /** 便于人工核对：打印编译计划摘要 */
    static String describe(CompilePlanIR plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("types=").append(plan.types().size()).append(", processDefs=").append(plan.processDefs().size())
          .append(", instances=").append(plan.instances().size()).append(", triggers=").append(plan.triggers().size())
          .append(", defaultStarts=").append(plan.defaultStarts().size()).append(", warnings=").append(plan.warnings().size()).append('\n');
        for (ProcessDefIR def : plan.processDefs()) {
            sb.append("  [").append(def.ownerKind()).append("] ").append(def.zhName())
              .append(def.timeConsuming() ? " (耗时)" : " (瞬时)").append(" params=")
              .append(def.params().stream().map(p -> p.direction().charAt(0) + ":" + p.zhName() + "<" + p.javaType() + ">").toList())
              .append(" nodes=").append(def.nodes().size()).append('\n');
            for (NodeIR node : def.nodes()) {
                sb.append("      pc").append(node.pc()).append(' ').append(node.toolKey().isEmpty() ? "CALL " + node.zhName() : node.toolKey())
                  .append(node.slow() ? " [slow]" : "").append(" in=").append(descArgs(node.inputs()))
                  .append(" out=").append(descArgs(node.writes()));
                for (PostIR post : node.postActions()) {
                    sb.append(" post=").append(post.kind()).append('(').append(post.stateLabel()).append(')');
                }
                for (var next : node.nexts()) {
                    sb.append(" -> pc").append(next.targetPc()).append(next.conditional() ? "?" : "");
                }
                sb.append('\n');
            }
        }
        for (TriggerIR trigger : plan.triggers()) {
            sb.append("  TRIGGER ").append(trigger.describe()).append(" : ")
              .append(String.join(".", trigger.contextPath())).append('/').append(trigger.memberToken())
              .append(' ').append(trigger.eventCode()).append(' ').append(trigger.stateLabel()).append('\n');
        }
        for (var ds : plan.defaultStarts()) {
            sb.append("  DEFAULT_START type=").append(ds.typeId()).append(" hostPath=").append(ds.hostPath())
              .append(" -> ").append(ds.targetDefKey()).append('\n');
        }
        for (String warning : plan.warnings()) {
            sb.append("  WARN ").append(warning).append('\n');
        }
        return sb.toString();
    }

    private static List<String> descArgs(List<ArgBindingIR> bindings) {
        return bindings.stream().map(b -> b.argName() + "=" + desc(b.operand())).toList();
    }

    private static String desc(OperandIR op) {
        if (op instanceof MemberPathIR m) {
            return GromaTypes.describe(m) + "<" + m.javaType() + ">";
        }
        return GromaTypes.describe(op);
    }
}

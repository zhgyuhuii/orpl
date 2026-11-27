package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.ProcessCodeAstTranslator;
import com.groma.studio.codegen.plan.OperandRenderer.Value;
import com.groma.studio.codegen.plan.PlanModel.EnumInfo;
import com.groma.studio.codegen.plan.PlanModel.LocalSlot;
import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.NodeSlot;
import com.groma.studio.codegen.plan.PlanModel.ParamSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.ArgBindingIR;
import com.groma.studio.ir.CompilePlanIR.NodeIR;
import com.groma.studio.ir.CompilePlanIR.ProcessDefIR;
import com.groma.studio.ir.CompilePlanIR.NextIR;
import com.groma.studio.ir.CompilePlanIR.PostIR;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 第 3 层（Engine 耗时过程状态机层）生成器：
 * 每个过程定义生成一个 {@code *ProcessTask}（继承底座 GromaProcessTask，execNode / nodeDone 两张 switch(pc) 节点表）
 * 以及一个 {@code *RuntimeBO}（过程运行帧：局部量、节点输出槽位与子过程句柄）。
 * 铁律：本层落在 /engine/ 路径下，100% 禁止出现原生监视器锁、无种子随机数与标准输出。
 */
public final class ProcessTaskEmitter {

    private static final ClassName TIMING_TYPE = ClassName.get("com.groma.base.engine", "GromaProcessTask", "Timing");

    public record Result(JavaFile task, JavaFile runtimeBo, List<String[]> scripts) {}

    /** 过程任务内部的操作数渲染：self / this.形参 / frame.局部量 */
    static final class TaskScope extends OperandRenderer {
        TaskScope(PlanModel model, ProcInfo proc) {
            super(model, proc);
        }

        @Override
        protected CodeBlock ctxRef() {
            return CodeBlock.of("ctx");
        }

        @Override
        protected CodeBlock selfRef(TypeName selfType) {
            return proc != null && proc.owner() != null ? CodeBlock.of("self") : CodeBlock.of("null");
        }

        @Override
        protected CodeBlock readParam(ParamSlot slot) {
            return CodeBlock.of("this.$L", slot.field());
        }

        @Override
        protected CodeBlock readLocal(LocalSlot slot) {
            return CodeBlock.of("frame.get$L()", slot.cap());
        }

        @Override
        protected CodeBlock writeLocal(LocalSlot slot, CodeBlock value) {
            return CodeBlock.of("frame.set$L($L);\n", slot.cap(), value);
        }

        @Override
        protected CodeBlock namedInstanceRef(String instanceId) {
            return CodeBlock.of("ctx.namedInstance($S)", instanceId);
        }

        @Override
        protected CodeBlock namedCollectionRef(String instanceId) {
            return CodeBlock.of("ctx.namedCollection($S)", instanceId);
        }
    }

    public Result emit(PlanModel model, ProcInfo proc, ProcessCodeAstTranslator translator) {
        ProcessDefIR def = proc.def();
        TypeInfo owner = proc.owner();
        TaskScope scope = new TaskScope(model, proc);
        boolean hasFlow = !proc.nodes().isEmpty();

        TypeSpec.Builder tb = TypeSpec.classBuilder(proc.taskCn())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(PlanModel.TASK_BASE)
                .addJavadoc("$L过程状态机：$L（方圆过程原名：$L；宿主对象：$L）\n<p>\n执行周期 P = $L，单例模式 = $L，孪生锁速 = $L。\n"
                        + "耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。\n</p>\n",
                        def.timeConsuming() ? "耗时" : "瞬时", def.zhName(), def.zhName(),
                        owner == null ? "独立过程" : owner.zhName(), "1", def.singletonMode(), def.twinLocked())
                .addAnnotation(meta(def.entityId(), def.zhName(), "PROCESS_TASK"));
        tb.addField(FieldSpec.builder(TIMING_TYPE, "TIMING", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(model.timingValue(def)).build());
        if (owner != null) {
            tb.addField(FieldSpec.builder(owner.entity(), "self", Modifier.PRIVATE, Modifier.FINAL)
                    .addJavadoc("宿主对象（方圆对象类型原名：$L）\n", owner.zhName()).build());
        }
        for (ParamSlot param : proc.params()) {
            tb.addField(FieldSpec.builder(param.type(), param.field(), Modifier.PRIVATE)
                    .addJavadoc("$L：$L\n", directionZh(param.direction()), param.zhName()).build());
        }
        if (proc.needsBo()) {
            tb.addField(FieldSpec.builder(proc.boCn(), "frame", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T()", proc.boCn()).addJavadoc("过程运行帧（局部量 / 节点输出 / 子过程句柄）\n").build());
        }

        // 构造器：(ctx, [self], 形参…)
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addParameter(PlanModel.WORLD_CTX, "ctx");
        if (owner != null) {
            ctor.addParameter(owner.entity(), "self");
        }
        for (ParamSlot param : proc.params()) {
            ctor.addParameter(param.type(), param.field());
        }
        ctor.addStatement("super(ctx, $L, $S, $S, TIMING)", owner == null ? "null" : "self", def.defKey(), def.zhName());
        if (owner != null) {
            ctor.addStatement("java.util.Objects.requireNonNull(self, $S)", "宿主对象不能为空: " + def.zhName());
            ctor.addStatement("this.self = self");
        }
        for (ParamSlot param : proc.params()) {
            ctor.addStatement("this.$L = $L", param.field(), param.field());
        }
        tb.addMethod(ctor.build());

        // 形参写入方法（手写代码转译时 setter 目标；运行期外部干预也用它回填）
        for (ParamSlot param : proc.params()) {
            tb.addMethod(MethodSpec.methodBuilder(param.setter())
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(param.type(), "value")
                    .addStatement("this.$L = value", param.field())
                    .build());
            // 读取形参当前值：调用方在子过程成功后经它取得输出 / 双向形参
            tb.addMethod(MethodSpec.methodBuilder(param.getter())
                    .addModifiers(Modifier.PUBLIC).returns(param.type())
                    .addJavadoc("$L：$L\n", directionZh(param.direction()), param.zhName())
                    .addStatement("return this.$L", param.field())
                    .build());
        }

        tb.addMethod(MethodSpec.methodBuilder("hasFlow").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(TypeName.BOOLEAN)
                .addStatement("return $L", hasFlow).build());
        if (hasFlow) {
            tb.addMethod(MethodSpec.methodBuilder("entryNodes").addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED).returns(int[].class)
                    .addStatement("return new int[] {$L}", entriesValues(proc)).build());
            if (proc.slowPcs().length > 0) {
                CodeBlock.Builder slow = CodeBlock.builder();
                for (int i = 0; i < proc.slowPcs().length; i++) {
                    slow.add(i == 0 ? "return pc == $L" : " || pc == $L", proc.slowPcs()[i]);
                }
                tb.addMethod(MethodSpec.methodBuilder("isSlowToolNode").addAnnotation(Override.class)
                        .addModifiers(Modifier.PROTECTED).returns(TypeName.BOOLEAN).addParameter(int.class, "pc")
                        .addStatement("$L", slow.build()).build());
            }
            tb.addMethod(emitExecNode(model, proc, scope));
            tb.addMethod(emitNodeDone(model, proc, scope));
        } else {
            tb.addMethod(MethodSpec.methodBuilder("entryNodes").addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED).returns(int[].class)
                    .addStatement("return new int[0]").build());
            LeafBodyEmitter.Outcome leaf = LeafBodyEmitter.emit(model, proc, translator);
            tb.addMethod(MethodSpec.methodBuilder("execLeafBody").addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED).addCode(leaf.body.build()).build());
            // 叶过程没有数据流节点表：节点回调为空实现
            tb.addMethod(MethodSpec.methodBuilder("execNode").addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED).returns(TypeName.BOOLEAN).addParameter(int.class, "pc")
                    .addStatement("return true").build());
            tb.addMethod(MethodSpec.methodBuilder("nodeDone").addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED).addParameter(int.class, "pc")
                    .addStatement("if (pc == $L) return", proc.entries().length == 0 ? -1 : proc.entries()[0]).build());
            tb.addMethod(emitCanStart(model, proc));
            MethodSpec maintain = emitCheckMaintain(model, proc);
            if (maintain != null) {
                tb.addMethod(maintain);
            }
            MethodSpec leafControlBits = emitProcessEnableBits(model, proc);
            if (leafControlBits != null) {
                tb.addMethod(leafControlBits);
            }
            JavaFile bo = emitRuntimeBo(model, proc);
            JavaFile task = JavaFile.builder(proc.taskCn().packageName(), addCommonTail(model, proc, tb, scope, false).build())
                    .indent("    ").build();
            return new Result(task, bo, leaf.scripts);
        }
        tb.addMethod(emitCanStart(model, proc));
        MethodSpec maintain = emitCheckMaintain(model, proc);
        if (maintain != null) {
            tb.addMethod(maintain);
        }
        MethodSpec controlBits = emitProcessEnableBits(model, proc);
        if (controlBits != null) {
            tb.addMethod(controlBits);
        }
        MethodSpec failed = emitNodeFailed(model, proc, scope);
        if (failed != null) {
            tb.addMethod(failed);
        }
        JavaFile bo = emitRuntimeBo(model, proc);
        JavaFile task = JavaFile.builder(proc.taskCn().packageName(), addCommonTail(model, proc, tb, scope, true).build())
                .indent("    ").build();
        return new Result(task, bo, List.of());
    }

    /** 入口节点数组内容（如 "0, 2"） */
    private static String entriesValues(ProcInfo proc) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < proc.entries().length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(proc.entries()[i]);
        }
        return sb.toString();
    }

    /**
     * R06 结构控制（收割）：宿主类型上指向本过程的控制规则位按位或为关注位集合；
     * 无控制规则时不覆写（基类返回 0 = 无约束，runProcess 门点零开销）。
     */
    private MethodSpec emitProcessEnableBits(PlanModel model, ProcInfo proc) {
        if (proc.owner() == null || proc.owner().controls() == null) {
            return null;
        }
        long bits = 0L;
        for (CompilePlanIR.ControlRuleIR rule : proc.owner().controls()) {
            if ("PROCESS".equals(rule.targetKind()) && rule.targetEntityId().equals(proc.def().entityId())) {
                bits |= 1L << rule.bitIndex();
            }
        }
        if (bits == 0L) {
            return null;
        }
        return MethodSpec.methodBuilder("processEnableBits").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(long.class)
                .addJavadoc("R06 控制规则位（宿主类型 $L）\n", proc.owner().zhName())
                .addStatement("return $L", "0x" + Long.toHexString(bits) + "L")
                .build();
    }

    /** 启动条件与非空输入守卫（收割：R22 启动条件 / R13/R15 源状态前置 / R21 初始维持条件接入 canStart） */
    private MethodSpec emitCanStart(PlanModel model, ProcInfo proc) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder("canStart").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(TypeName.BOOLEAN);
        boolean any = false;
        for (ParamSlot param : proc.params()) {
            if (param.nonnull() && param.reads() && !PlanModel.KIND_BASE.equals(param.kind())) {
                mb.addStatement("if (this.$L == null) return false", param.field());
                any = true;
            }
        }
        // 画布启动/维持/源状态前置条件：状态前置走 enumZhLabel 中文比对（与事件接线表同一语义）；
        // 条件组与组合表达式经 conditionJudgeCode 完整编译（G6 收割：复合表达式 1&&(2||3) 此前降注释失效）
        List<CompilePlanIR.ConditionRuleIR> starts = proc.def().startConditions();
        if (starts != null && !starts.isEmpty()) {
            TaskScope condScope = new TaskScope(model, proc);
            for (CompilePlanIR.ConditionRuleIR rule : starts) {
                OperandRenderer.Value value = tryRenderConditionOperand(model, proc, condScope, rule, mb, "启动条件");
                if (value == null) {
                    continue;
                }
                if (rule.stateLabel() != null && !rule.stateLabel().isBlank()) {
                    mb.beginControlFlow("if (!\"$L\".equals($T.enumZhLabel((Enum<?>) ($L))))",
                            rule.stateLabel(), PlanModel.BASE_ENTITY, value.code());
                    mb.addStatement("return false");
                    mb.endControlFlow();
                }
                CodeBlock judge = conditionJudgeCode(model, rule.conditions(), rule.expression(), value, "启动条件");
                if (judge != null) {
                    mb.beginControlFlow("if (!($L))", judge);
                    mb.addStatement("return false");
                    mb.endControlFlow();
                } else if (rule.conditions() != null && !rule.conditions().isEmpty()) {
                    mb.addStatement("// 启动条件[$L]含不支持的记号，为避免语义错误不再按 AND 直译: $L",
                            rule.describe(), String.valueOf(rule.expression()));
                }
            }
        }
        mb.addStatement("return true");
        return mb.build();
    }

    /**
     * R21 维持条件逐拍校验（方案 D 收割）：maintainConditions 编译为布尔守卫，
     * 不满足时派发 paa 并级联终止（abortByMaintainBroken）。无维持条件时不生成（基类空实现零开销）。
     */
    private MethodSpec emitCheckMaintain(PlanModel model, ProcInfo proc) {
        List<CompilePlanIR.ConditionRuleIR> maintains = proc.def().maintainConditions();
        if (maintains == null || maintains.isEmpty()) {
            return null;
        }
        MethodSpec.Builder mb = MethodSpec.methodBuilder("checkMaintain")
                .addAnnotation(Override.class).addModifiers(Modifier.PROTECTED);
        TaskScope condScope = new TaskScope(model, proc);
        boolean any = false;
        for (CompilePlanIR.ConditionRuleIR rule : maintains) {
            OperandRenderer.Value value = tryRenderConditionOperand(model, proc, condScope, rule, mb, "维持条件");
            if (value == null) {
                continue;
            }
            // 状态前置 + 条件组/复合表达式（G6 收割：与 canStart 同一编译路径，修复原实现里
            // judges.isEmpty 双重判断导致降级注释不可达的死代码）
            List<CodeBlock> judges = new ArrayList<>();
            if (rule.stateLabel() != null && !rule.stateLabel().isBlank()) {
                judges.add(CodeBlock.of("\"$L\".equals($T.enumZhLabel((Enum<?>) ($L)))",
                        rule.stateLabel(), PlanModel.BASE_ENTITY, value.code()));
            }
            CodeBlock conditionJudge = conditionJudgeCode(model, rule.conditions(), rule.expression(), value, "维持条件");
            if (conditionJudge != null) {
                judges.add(conditionJudge);
            } else if (rule.conditions() != null && !rule.conditions().isEmpty()) {
                model.diagnostic("过程 " + proc.zhName() + " 维持条件[" + rule.describe()
                        + "]的表达式含不支持的记号，该规则不参与逐拍校验");
                continue;
            }
            if (judges.isEmpty()) {
                continue;
            }
            CodeBlock.Builder judge = CodeBlock.builder().add(judges.get(0));
            for (int i = 1; i < judges.size(); i++) {
                judge.add(" && ").add(judges.get(i));
            }
            mb.beginControlFlow("if (!($L))", judge.build());
            mb.addStatement("// R21 维持条件中断：$L", rule.describe().replace("$", "＄"));
            mb.addStatement("ctx.emitProcessEvent(this, \"paa\", $S)", "维持条件中断: " + rule.describe());
            mb.addStatement("abortByMaintainBroken($S)", rule.describe());
            mb.addStatement("return");
            mb.endControlFlow();
            any = true;
        }
        if (!any) {
            return null;
        }
        return mb.build();
    }

    /**
     * R23 失败事件线（方案 D 收割）：调用线携带 psf/paa 的后继按子过程出口路由到失败分支。
     * 子过程失败时基座调用 nodeFailed（pc = 调用节点），这里按 childExitEvent 激活对应后继。
     */
    private MethodSpec emitNodeFailed(PlanModel model, ProcInfo proc, TaskScope scope) {
        Map<Integer, List<NextIR>> byPc = new java.util.LinkedHashMap<>();
        for (NodeSlot slot : proc.nodes()) {
            if (!slot.isCall()) {
                continue;
            }
            for (NextIR next : slot.node().nexts()) {
                if (targetNode(proc, next) == null || next.eventCode().isEmpty()
                        || !(next.eventCode().equals("psf") || next.eventCode().equals("paa"))) {
                    continue;
                }
                byPc.computeIfAbsent(slot.pc(), k -> new ArrayList<>()).add(next);
            }
        }
        if (byPc.isEmpty()) {
            return null;
        }
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.beginControlFlow("switch (pc)");
        for (Map.Entry<Integer, List<NextIR>> entry : byPc.entrySet()) {
            cb.beginControlFlow("case $L ->", entry.getKey());
            NodeSlot failedSlot = proc.node(entry.getKey());
            for (NextIR next : entry.getValue()) {
                emitGuardedActivation(model, proc, failedSlot, scope, next, cb);
            }
            cb.endControlFlow();
        }
cb.add("default -> { }\n");
        cb.endControlFlow();
        return MethodSpec.methodBuilder("nodeFailed")
                .addAnnotation(Override.class).addModifiers(Modifier.PROTECTED)
                .addParameter(int.class, "pc").addParameter(PlanModel.PROCESS_TASK, "child")
                .addCode(cb.build()).build();
    }

    // ------------------------------------------------------------------ execNode

    private MethodSpec emitExecNode(PlanModel model, ProcInfo proc, TaskScope scope) {
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.beginControlFlow("switch (pc)");
        for (NodeSlot slot : proc.nodes()) {
            cb.beginControlFlow("case $L ->", slot.pc());
            // DAP 源码级探针：按画布图元 id 上报行执行（第六章 6.2；debug-session 挂载时在此断点挂起）
            cb.addStatement("fireLineProbe($S)", slot.node().cellId());
            if (slot.isCall()) {
                emitCallExec(model, proc, slot, scope, cb);
            } else {
                cb.add(ToolSemantics.emit(model, proc, slot, scope));
            }
            cb.endControlFlow();
        }
        cb.add("default -> throw new IllegalStateException($S + pc);\n", "未知的过程节点: pc=");
        cb.endControlFlow();
        cb.addStatement("return true");
        return MethodSpec.methodBuilder("execNode").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(TypeName.BOOLEAN).addParameter(int.class, "pc")
                .addCode(cb.build()).build();
    }

    /** 调用节点：按当前取值绑定实参并发起子过程 */
    private void emitCallExec(PlanModel model, ProcInfo proc, NodeSlot slot, TaskScope scope, CodeBlock.Builder cb) {
        ProcInfo callee = model.calleeOf(slot);
        NodeIR node = slot.node();
        if (callee == null) {
            cb.addStatement("fail($S)", "被调过程未注册: " + node.zhName());
            return;
        }
        StringBuilder note = new StringBuilder("调用").append(callee.timeConsuming() ? "耗时" : "瞬时").append("子过程：")
                .append(callee.zhName());
        for (ArgBindingIR binding : node.inputs()) {
            note.append(" ").append(binding.argName()).append(" = ").append(scope.describeZh(binding.operand())).append(";");
        }
        cb.add("// $L\n", note.toString().replace("$", "＄"));

        List<CodeBlock> args = new ArrayList<>();
        if (callee.owner() != null) {
            args.add(callHost(model, proc, callee, slot, scope));
        }
        for (ParamSlot param : callee.params()) {
            ArgBindingIR binding = findBinding(node.inputs(), param.zhName());
            if (param.reads() && binding != null) {
                Value raw = scope.render(binding.operand());
                args.add(scope.coerce(raw, param.type(), param.kind()));
            } else {
                args.add(CodeBlock.of(param.defaultLiteral()));
            }
        }
        CodeBlock ctor = CodeBlock.of("new $T(ctx$L)", callee.taskCn(), joinArgs(args));
        if (slot.storeChild()) {
            cb.addStatement("frame.set$L($L)", slot.childCap(), ctor);
            cb.addStatement("if (!spawnOrRun($L, frame.get$L())) return false", slot.pc(), slot.childCap());
        } else {
            cb.addStatement("if (!spawnOrRun($L, $L)) return false", slot.pc(), ctor);
        }
    }

    /** 被调过程的宿主：组成线指定的成员；未指定时沿用当前宿主 */
    private CodeBlock callHost(PlanModel model, ProcInfo proc, ProcInfo callee, NodeSlot slot, TaskScope scope) {
        if (slot.node().host() != null) {
            Value host = scope.render(slot.node().host());
            return scope.coerce(host, callee.owner().entity(), PlanModel.KIND_REF);
        }
        if (proc.owner() != null) {
            return CodeBlock.of("self");
        }
        model.diagnostic("过程 " + proc.zhName() + " 调用有宿主的过程 " + callee.zhName() + " 时没有可用的宿主对象");
        return CodeBlock.of("null");
    }

    private static CodeBlock joinArgs(List<CodeBlock> args) {
        CodeBlock.Builder joined = CodeBlock.builder();
        for (CodeBlock arg : args) {
            joined.add(", $L", arg);
        }
        return joined.build();
    }

    private static ArgBindingIR findBinding(List<ArgBindingIR> bindings, String argName) {
        for (ArgBindingIR binding : bindings) {
            if (binding.argName().equals(argName)) {
                return binding;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ nodeDone

    private MethodSpec emitNodeDone(PlanModel model, ProcInfo proc, TaskScope scope) {
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.beginControlFlow("switch (pc)");
        for (NodeSlot slot : proc.nodes()) {
            cb.beginControlFlow("case $L ->", slot.pc());
            emitNodeDoneCase(model, proc, slot, scope, cb);
            cb.endControlFlow();
        }
        cb.add("default -> throw new IllegalStateException($S + pc);\n", "未知的过程节点: pc=");
        cb.endControlFlow();
        return MethodSpec.methodBuilder("nodeDone").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).addParameter(int.class, "pc").addCode(cb.build()).build();
    }

    /** 节点成功：回写实参 → 后置动作（状态转换 / 生成 / 消耗）→ 激活后继 */
    private void emitNodeDoneCase(PlanModel model, ProcInfo proc, NodeSlot slot, TaskScope scope, CodeBlock.Builder cb) {
        NodeIR node = slot.node();
        ProcInfo callee = slot.isCall() ? model.calleeOf(slot) : null;

        // 1. 回写
        if (slot.isCall()) {
            for (ArgBindingIR write : node.writes()) {
                ParamSlot outParam = callee == null ? null : callee.param(write.argName());
                if (outParam == null) {
                    model.diagnostic("过程 " + proc.zhName() + "：子过程 " + node.zhName() + " 没有输出形参 " + write.argName() + "，回写已跳过");
                    continue;
                }
                cb.add("// 回写：$L ← $L.$L\n", scope.describeZh(write.operand()), node.zhName(), write.argName());
                cb.addStatement("$L", scope.assign(write.operand(), new Value(
                        CodeBlock.of("frame.get$L().$L()", slot.childCap(), outParam.getter()),
                        outParam.type(), outParam.kind(), outParam.placeholder())));
            }
        } else if (slot.storeOutput()) {
            for (ArgBindingIR write : node.writes()) {
                cb.addStatement("$L", scope.assign(write.operand(), new Value(CodeBlock.of("frame.get$L()", slot.outCap()),
                        slot.outType(), PlanModel.kindOf(node.resultJavaType()), node.resultJavaType())));
            }
        }

        // 2. 后置动作
        emitPostActions(model, proc, scope, node.postActions(), cb);

        // 3. 后继：互斥概率组（标准 10.4 CDF 采样）→ 无组连线（条件判断 / R23 事件线 / 独立概率）
        java.util.Set<NextIR> grouped = emitProbabilityGroups(model, proc, slot, cb);
        for (NextIR next : node.nexts()) {
            NodeSlot target = proc.node(next.targetPc());
            if (target == null) {
                continue;
            }
            if (grouped.contains(next)) {
                continue; // 已在概率组内统一采样（带条件/事件的组员不入组，走独立线路由）
            }
            if (!next.conditional()) {
                if (!next.eventCode().isEmpty()) {
                    // R23 事件线（方案 D 收割）：pca/pcd 出口在成功路径路由（子过程超时/提前完成后继）；
                    // psf/paa 出口由 nodeFailed 失败分支路由（见 emitNodeFailed）
                    if (next.eventCode().equals("pca") || next.eventCode().equals("pcd")) {
                        emitGuardedActivation(model, proc, slot, scope, next, cb);
                    } else if (!next.eventCode().equals("psf") && !next.eventCode().equals("paa")) {
                        model.diagnostic("过程 " + proc.zhName() + "：" + node.zhName() + " → " + target.node().zhName()
                                + " 的调用线携带事件 " + next.eventCode() + " 非过程出口码，暂未编译，已跳过");
                    }
                    continue;
                }
                emitActivation(cb, next);
            } else {
                Value output = outputValue(model, proc, slot, scope, next.conditionSource());
                CodeBlock judge = output == null ? CodeBlock.of("true") : judgeCode(model, next, output);
                if (judge.toString().equals("true")) {
                    emitActivation(cb, next);
                } else {
                    cb.beginControlFlow("if ($L)", judge);
                    emitActivation(cb, next);
                    cb.endControlFlow();
                }
            }
        }

        // 4. R25/R26 异常控制流边（方案 D 收割）：被监控耗时子过程超时/提前完成 → 激活异常处理路径
        for (CompilePlanIR.ExceptionFlowIR flow : proc.def().exceptionFlows() == null
                ? java.util.List.<CompilePlanIR.ExceptionFlowIR>of() : proc.def().exceptionFlows()) {
            if (flow.sourcePc() < 0) {
                // 过程级监控（sourcePc=-1）：成功后激活已无机器可走，只保留引擎侧 pca/pcd 事件
                if (slot.pc() == proc.nodes().get(0).pc()) {
                    model.diagnostic("过程 " + proc.zhName() + " 的过程级异常流（" + flow.eventKind()
                            + "）只派发事件，不激活节点（成功后流程已收尾）");
                }
                continue;
            }
            if (flow.sourcePc() != slot.pc() || proc.node(flow.targetPc()) == null) {
                continue;
            }
            String exitCode = "OVERTIME_PCD".equals(flow.eventKind()) ? "pcd" : "pca";
            cb.beginControlFlow("if ($S.equals(childExitEvent($L)))", exitCode, slot.pc());
            cb.addStatement("activate($L)", flow.targetPc());
            cb.endControlFlow();
        }

        // 5. lastEnd 结束点（方案 D 收割）：完成后立即收尾并级联取消同层旁路子过程
        if (node.lastEnd()) {
            cb.addStatement("finishWithLastEnd()");
        }
    }

    /** 后继激活（含连线延迟：存量“等待”子过程语义，延迟 N 拍后激活） */
    private static void emitActivation(CodeBlock.Builder cb, NextIR next) {
        if (next.delayTicks() > 0) {
            cb.addStatement("activateAfter($L, $LL)", next.targetPc(), next.delayTicks());
        } else {
            cb.addStatement("activate($L)", next.targetPc());
        }
    }

    /** 事件线 / 概率连线的守卫激活：出口码匹配（可选）+ 独立概率伯努利（可选）+ 条件判断（可选） */
    private void emitGuardedActivation(PlanModel model, ProcInfo proc, NodeSlot slot, TaskScope scope,
                                       NextIR next, CodeBlock.Builder cb) {
        int pc = slot.pc();
        List<CodeBlock> guards = new ArrayList<>();
        if (!next.eventCode().isEmpty()) {
            guards.add(CodeBlock.of("$S.equals(childExitEvent($L))", next.eventCode(), pc));
        }
        if (!next.probabilityGroup().isEmpty()) {
            model.diagnostic("过程 " + proc.zhName() + "：pc=" + pc + " 的概率组连线同时携带事件/条件，按独立连线处理");
        } else if (next.probability() > 0.0D && next.probability() < 0.9999D) {
            guards.add(CodeBlock.of("ctx.randomStream().nextDouble() < $L", String.valueOf(next.probability())));
        }
        if (next.conditional()) {
            Value output = outputValue(model, proc, slot, scope, next.conditionSource());
            guards.add(judgeCode(model, next, output));
        }
        if (guards.isEmpty()) {
            emitActivation(cb, next);
            return;
        }
        CodeBlock.Builder judge = CodeBlock.builder().add(guards.get(0));
        for (int i = 1; i < guards.size(); i++) {
            judge.add(" && ").add(guards.get(i));
        }
        cb.beginControlFlow("if ($L)", judge.build());
        emitActivation(cb, next);
        cb.endControlFlow();
    }

    private static NodeSlot targetNode(ProcInfo proc, NextIR next) {
        return proc.node(next.targetPc());
    }

    /**
     * 互斥概率组（标准 10.4，方案 D 收割）：同组连线按 probability 归一后做 CDF 采样，
     * 恰好激活一个后继。采样走 ctx.randomStream()（任务级确定性随机流，存档游标随存档恢复）。
     */
    private java.util.Set<NextIR> emitProbabilityGroups(PlanModel model, ProcInfo proc, NodeSlot slot,
                                                        CodeBlock.Builder cb) {
        java.util.Set<NextIR> grouped = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Map<String, List<NextIR>> groups = new java.util.LinkedHashMap<>();
        for (NextIR next : slot.node().nexts()) {
            if (targetNode(proc, next) == null || next.probabilityGroup().isEmpty()
                    || next.probability() <= 0.0D) {
                continue;
            }
            if (next.conditional() || !next.eventCode().isEmpty()) {
                model.diagnostic("过程 " + proc.zhName() + "：概率组连线同时携带条件/事件，按独立连线处理");
                continue;
            }
            groups.computeIfAbsent(next.probabilityGroup(), k -> new ArrayList<>()).add(next);
            grouped.add(next);
        }
        for (Map.Entry<String, List<NextIR>> entry : groups.entrySet()) {
            if (entry.getValue().size() < 2) {
                model.diagnostic("过程 " + proc.zhName() + "：概率组 " + entry.getKey() + " 只有 1 个成员，按独立连线处理");
                grouped.removeAll(entry.getValue());
                continue;
            }
            double sum = 0.0D;
            for (NextIR next : entry.getValue()) {
                sum += next.probability();
            }
            cb.addStatement("// 概率组 $L（$L 个成员，概率和 $L，CDF 互斥采样）", entry.getKey(),
                    entry.getValue().size(), String.valueOf(sum));
            cb.addStatement("double groupR$L = ctx.randomStream().nextDouble()", sanitizeGroup(entry.getKey()));
            double acc = 0.0D;
            for (int i = 0; i < entry.getValue().size(); i++) {
                NextIR next = entry.getValue().get(i);
                acc += sum > 0.0D ? next.probability() / sum : 1.0D / entry.getValue().size();
                String bound = String.valueOf(acc);
                if (i == entry.getValue().size() - 1) {
                    // 末成员兜底：概率归一后落在前面所有成员之外（浮点余量）时必中
                    cb.addStatement("activate($L)", next.targetPc());
                } else {
                    // palantir javapoet 0.5.0：else-if 链必须 nextControlFlow（endControlFlow("else")
                    // 是 do-while 收尾语义，生成 `} else;` 空语句，互斥采样会静默失效——实测踩雷）
                    cb.beginControlFlow("if (groupR$L < $L)", sanitizeGroup(entry.getKey()), bound);
                    emitActivation(cb, next);
                    cb.nextControlFlow("else");
                }
            }
        }
        return grouped;
    }

    /** 任意文本单行化 + $ 转义（进生成代码注释的安全形态） */
    /**
     * 条件操作数渲染（canStart / checkMaintain 共用）：渲染失败或渲染为字面 null
     * （OperandRenderer 对静态不可解析成员路径"按空值处理"——对数据流合理，对守卫无效）
     * 一律降级为注释跳过，绝不把 null 拼进比较表达式（合成模型 RM-C1 实证：null < 50 编译失败）。
     */
    private OperandRenderer.Value tryRenderConditionOperand(PlanModel model, ProcInfo proc,
                                                            TaskScope condScope, CompilePlanIR.ConditionRuleIR rule,
                                                            MethodSpec.Builder mb, String kind) {
        OperandRenderer.Value value;
        try {
            value = condScope.render(rule.operand());
        } catch (RuntimeException ex) {
            model.diagnostic("过程 " + proc.zhName() + " " + kind + "[" + rule.describe() + "]的操作数无法解析，已降级注释: "
                    + ex.getMessage());
            mb.addStatement("// " + kind + "[$L]的操作数无法解析，已跳过: $L", rule.describe(),
                    oneLineText(String.valueOf(ex.getMessage())));
            return null;
        }
        if (value != null && "null".equals(value.code().toString())) {
            model.diagnostic("过程 " + proc.zhName() + " " + kind + "[" + rule.describe() + "]的操作数静态不可解析"
                    + "（按空值处理路径），守卫降级注释");
            mb.addStatement("// " + kind + "[$L]的操作数静态不可解析（成员路径按空值处理），守卫降级: $L",
                    rule.describe(), oneLineText(String.valueOf(rule.operand().javaType())));
            return null;
        }
        return value;
    }

    private static String oneLineText(String text) {
        return (text == null ? "" : text.replaceAll("\\s*\\r?\\n\\s*", " ")).replace("$", "＄");
    }

    /** 概率组名转合法 Java 标识片段（生成局部变量用） */
    private static String sanitizeGroup(String group) {
        return group.codePoints().allMatch(ch -> Character.isJavaIdentifierPart(ch))
                ? group
                : "g" + Integer.toUnsignedString(group.hashCode());
    }

    /** 被线上判断引用的端口输出：工具节点的输出槽位，或子过程的输出形参 */
    private Value outputValue(PlanModel model, ProcInfo proc, NodeSlot slot, TaskScope scope, String argName) {
        if (!slot.isCall()) {
            return new Value(CodeBlock.of("frame.get$L()", slot.outCap()), slot.outType(),
                    PlanModel.kindOf(slot.node().resultJavaType()), slot.node().resultJavaType());
        }
        ProcInfo callee = model.calleeOf(slot);
        ParamSlot param = callee == null ? null : callee.param(argName);
        if (param == null) {
            model.diagnostic("过程 " + proc.zhName() + "：调用线判断引用的输出形参 " + argName + " 不存在");
            return new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
        }
        return new Value(CodeBlock.of("frame.get$L().$L()", slot.childCap(), param.getter()), param.type(), param.kind(),
                param.placeholder());
    }

    /** 画布线上判断（logic.conditions + expression）→ 布尔表达式 */
    static CodeBlock judgeCode(PlanModel model, NextIR next, Value value) {
        if (next.conditions() == null || next.conditions().isEmpty()) {
            return CodeBlock.of("true");
        }
        CodeBlock judge = conditionJudgeCode(model, next.conditions(), next.expression(), value, "线上判断");
        return judge == null ? allJudges(model, next.conditions(), value) : judge;
    }

    /**
     * 条件组 + 组合表达式 → 布尔表达式（收割 G6：启动/维持条件此前不支持复合表达式，一律降注释失效；
     * 现与线上判断同一词法白名单 [0-9 ()&|!] 完整编译。表达式为空 = 条件全部 AND；
     * 纯数字 = 仅取该序号条件；含不支持的记号时返回 null 由调用方降级（绝不拼出语义错误的 AND）。
     */
    static CodeBlock conditionJudgeCode(PlanModel model, List<CompilePlanIR.ConditionIR> conditions,
                                        String expression, Value value, String sceneLabel) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        String expr = expression == null ? "" : expression.trim();
        if (expr.isEmpty()) {
            return allJudges(model, conditions, value);
        }
        if (expr.matches("\\d+")) {
            int index = Integer.parseInt(expr);
            CompilePlanIR.ConditionIR condition = conditionAt(conditions, index);
            return singleJudge(model, value, condition, condition);
        }
        if (!expr.matches("[\\d\\s&|!()]+")) {
            model.diagnostic(sceneLabel + "表达式 " + expr + " 含不支持的记号，无法编译");
            return null;
        }
        CodeBlock.Builder composed = CodeBlock.builder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                int j = i;
                while (j < expr.length() && Character.isDigit(expr.charAt(j))) {
                    j++;
                }
                int index = Integer.parseInt(expr.substring(i, j));
                CompilePlanIR.ConditionIR condition = conditionAt(conditions, index);
                composed.add(singleJudge(model, value, condition, null));
                i = j - 1;
            } else {
                composed.add(Character.toString(c));
            }
        }
        return composed.build();
    }

    private static CodeBlock allJudges(PlanModel model, List<CompilePlanIR.ConditionIR> conditions, Value value) {
        CodeBlock.Builder composed = CodeBlock.builder();
        boolean first = true;
        for (CompilePlanIR.ConditionIR condition : conditions) {
            if (!first) {
                composed.add(" && ");
            }
            first = false;
            composed.add(singleJudge(model, value, condition, null));
        }
        return composed.build();
    }

    private static CompilePlanIR.ConditionIR conditionAt(List<CompilePlanIR.ConditionIR> conditions, int index) {
        for (CompilePlanIR.ConditionIR condition : conditions) {
            if (condition.index() == index) {
                return condition;
            }
        }
        return conditions.get(Math.max(0, Math.min(conditions.size() - 1, index - 1)));
    }

    private static CodeBlock singleJudge(PlanModel model, Value value, CompilePlanIR.ConditionIR condition, Object unused) {
        String op = condition.op() == null || condition.op().isEmpty() ? "=" : condition.op();
        String literal = condition.literal() == null ? "" : condition.literal();
        boolean booleanValue = value.type() == TypeName.BOOLEAN
                || "boolean".equals(value.type().toString()) || "Boolean".equals(value.type().toString());
        if (booleanValue && ("=".equals(op) || "!=".equals(op)) && ("true".equals(literal) || "false".equals(literal))) {
            boolean positive = "true".equals(literal) ^ "!=".equals(op);
            return positive ? value.code() : CodeBlock.of("!($L)", value.code());
        }
        return CodeBlock.of("$T.judge($L, $S, $S)", PlanModel.BUILTIN_TOOLS, value.code(), op, literal);
    }

    // ------------------------------------------------------------------ 后置动作

    /** 后置动作：out-change 状态转换（R14）、result 生成（R12）、consumption 消耗（R11） */
    static void emitPostActions(PlanModel model, ProcInfo proc, OperandRenderer scope,
                                List<PostIR> postActions, CodeBlock.Builder cb) {
        // 互斥概率组（标准 10.4，合成模型 RM-C1 实证缺口）：共享 probabilityGroup 的 OUT_CHANGE 分支
        // 按 CDF 互斥采样（ctx.randomStream 任务级确定性随机流，存档游标跟踪），绝不顺序直译
        java.util.Set<PostIR> grouped = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Map<String, List<PostIR>> mutexGroups = new java.util.LinkedHashMap<>();
        for (PostIR post : postActions) {
            if ("OUT_CHANGE".equals(post.kind()) && post.target() != null
                    && post.probabilityGroup() != null && !post.probabilityGroup().isBlank()
                    && post.probability() > 0.0D) {
                mutexGroups.computeIfAbsent(post.probabilityGroup(), k -> new ArrayList<>()).add(post);
                grouped.add(post);
            }
        }
        for (Map.Entry<String, List<PostIR>> entry : mutexGroups.entrySet()) {
            if (entry.getValue().size() < 2) {
                grouped.removeAll(entry.getValue()); // 单成员组按独立概率处理
                continue;
            }
            double sum = 0.0D;
            for (PostIR post : entry.getValue()) {
                sum += post.probability();
            }
            String tag = entry.getKey().replaceAll("[^A-Za-z0-9]", "_");
            cb.add("// 概率组 $L（$L 个互斥分支，概率和 $L，CDF 采样）\n", entry.getKey(),
                    entry.getValue().size(), String.valueOf(sum));
            cb.addStatement("double groupR$L = ctx.randomStream().nextDouble()", tag);
            double acc = 0.0D;
            for (int i = 0; i < entry.getValue().size(); i++) {
                PostIR post = entry.getValue().get(i);
                acc += sum > 0.0D ? post.probability() / sum : 1.0D / entry.getValue().size();
                if (i == entry.getValue().size() - 1) {
                    emitOneOutChange(model, proc, scope, post, cb);
                } else {
                    // palantir javapoet 0.5.0：else-if 链必须 nextControlFlow（endControlFlow("else")
                    // 生成 `} else;`，互斥采样静默失效——实测踩雷）
                    cb.beginControlFlow("if (groupR$L < $L)", tag, String.valueOf(acc));
                    emitOneOutChange(model, proc, scope, post, cb);
                    cb.nextControlFlow("else");
                    if (i == entry.getValue().size() - 2) {
                        emitOneOutChange(model, proc, scope, entry.getValue().get(i + 1), cb);
                        cb.endControlFlow();
                        break;
                    }
                }
            }
        }
        for (PostIR post : postActions) {
            if (grouped.contains(post) || post.target() == null) {
                continue;
            }
            if ("OUT_CHANGE".equals(post.kind())) {
                EnumInfo info = model.enums.get(post.enumTypeId());
                String constant = info == null ? null : info.constantOf(post.stateLabel());
                if (constant == null) {
                    model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "输出转换的目标状态 " + post.stateLabel() + " 不在枚举里，已跳过");
                    continue;
                }
                // 独立概率分支：probability < 1 包伯努利门控（任务级确定性随机流）
                if (post.probability() > 0.0D && post.probability() < 1.0D) {
                    cb.beginControlFlow("if (ctx.randomStream().nextDouble() < $L)", String.valueOf(post.probability()));
                    cb.add("// 输出转换（独立概率 $L）：$L → $L\n", String.valueOf(post.probability()),
                            scope.describeZh(post.target()), post.stateLabel());
                    cb.addStatement("$L", scope.assign(post.target(), enumValue(info, constant)));
                    cb.endControlFlow();
                } else {
                    cb.add("// 输出转换：$L → $L\n", scope.describeZh(post.target()), post.stateLabel());
                    cb.addStatement("$L", scope.assign(post.target(), enumValue(info, constant)));
                }
            } else if ("RESULT".equals(post.kind())) {
                emitResult(model, proc, scope, post, cb);
            } else if ("WRITE_BACK".equals(post.kind())) {
                emitWriteBack(model, proc, scope, post, cb);
            } else if ("CONSUME".equals(post.kind())) {
                OperandRenderer.WriteTarget target = scope.writeTarget(post.target());
                if (target == null || target.owner() == null) {
                    model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "消耗关系的目标不是对象，已跳过");
                    continue;
                }
                cb.add("// 消耗：$L\n", scope.describeZh(post.target()));
                cb.addStatement("(($T) $L).markConsumed($L)", PlanModel.BASE_ENTITY, target.owner().code(), scope.ctxRef());
            }
        }
    }

    /** 单条 OUT_CHANGE 写出（互斥组分支内复用） */
    private static void emitOneOutChange(PlanModel model, ProcInfo proc, OperandRenderer scope, PostIR post,
                                         CodeBlock.Builder cb) {
        EnumInfo info = model.enums.get(post.enumTypeId());
        String constant = info == null ? null : info.constantOf(post.stateLabel());
        if (constant == null) {
            model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "输出转换的目标状态 " + post.stateLabel() + " 不在枚举里，已跳过");
            return;
        }
        cb.add("// 输出转换：$L → $L\n", scope.describeZh(post.target()), post.stateLabel());
        cb.addStatement("$L", scope.assign(post.target(), enumValue(info, constant)));
    }

    private static Value enumValue(EnumInfo info, String constant) {
        return new Value(CodeBlock.of("$T.$L", info.cn(), constant),
                info.cn(), PlanModel.KIND_ENUM,
                com.groma.studio.ir.GromaTypes.enumType(info.enumId()));
    }

    /** 输出影响 / 双向影响回写：过程成功后把输出形参的终值写回调用点上绑定的成员 */
    private static void emitWriteBack(PlanModel model, ProcInfo proc, OperandRenderer scope, PostIR post,
                                      CodeBlock.Builder cb) {
        if (proc == null || !(post.value() instanceof CompilePlanIR.ParamIR outRef)) {
            return;
        }
        ParamSlot outParam = proc.param(outRef.zhName());
        if (outParam == null) {
            model.diagnostic(proc.zhName() + "：输出影响回写引用的形参 " + outRef.zhName() + " 不存在，已跳过");
            return;
        }
        cb.add("// 输出影响回写：$L ← 过程「$L」的输出 $L\n", scope.describeZh(post.target()), proc.zhName(),
                outParam.zhName());
        cb.addStatement("$L", scope.assign(post.target(), new Value(scope.readCompletedParam(proc, outParam),
                outParam.type(), outParam.kind(), outParam.placeholder())));
    }

    /** R12 结果关系：成员位置生成新对象（基础类型成员即重置为新值），并发出对象出现事件 a */
    private static void emitResult(PlanModel model, ProcInfo proc, OperandRenderer scope, PostIR post, CodeBlock.Builder cb) {
        OperandRenderer.WriteTarget target = scope.writeTarget(post.target());
        if (target == null) {
            model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "生成关系的目标不可写，已跳过");
            return;
        }
        cb.add("// 生成：$L\n", scope.describeZh(post.target()));
        Value type = scope.targetType(post.target());
        String kind = target.member() != null ? target.member().kind() : target.local() != null
                ? target.local().kind() : target.param().kind();
        TypeName javaType = type == null ? PlanModel.OBJECT_TYPE : type.type();
        CodeBlock spawned;
        if (PlanModel.KIND_REF.equals(kind) && PlanModel.isEntity(javaType)) {
            TypeInfo info = model.typeOf(javaType);
            spawned = CodeBlock.of("$L.spawn(new $T($L.nextInstanceId($S)))", scope.ctxRef(), info.entity(),
                    scope.ctxRef(), info.code());
        } else if (PlanModel.KIND_LIST.equals(kind) || PlanModel.KIND_MAP.equals(kind)) {
            spawned = CodeBlock.of(model.defaultLiteral(javaType, kind));
        } else {
            spawned = CodeBlock.of(model.defaultLiteral(javaType, kind));
        }
        cb.addStatement("$L", scope.assign(post.target(), new Value(spawned, javaType, kind, "")));
        if (target.member() != null && PlanModel.KIND_BASE.equals(kind)) {
            cb.addStatement("$L.onMemberGenerated($L, $S)", scope.ctxRef(), target.owner().code(), target.member().tokenId());
        }
    }

    // ------------------------------------------------------------------ 存档槽位

    private TypeSpec.Builder addCommonTail(PlanModel model, ProcInfo proc, TypeSpec.Builder tb, TaskScope scope,
                                           boolean hasFlow) {
        MethodSpec.Builder export = MethodSpec.methodBuilder("exportSlots").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(PlanModel.mapOfObjects(), "slots");
        MethodSpec.Builder importSpec = MethodSpec.methodBuilder("importSlots").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
                .addParameter(PlanModel.mapOfObjects(), "slots");
        for (ParamSlot param : proc.params()) {
            if (param.reads()) {
                export.addStatement("slots.put($S, $T.encode(this.$L))", param.field(), PlanModel.VALUES, param.field());
                if (param.type() == TypeName.DOUBLE) {
                    importSpec.addStatement("if (slots.get($S) != null) this.$L = $T.toDouble(slots.get($S))",
                        param.field(), param.field(), PlanModel.VALUES, param.field());
                } else if (param.type() == TypeName.LONG) {
                    importSpec.addStatement("if (slots.get($S) != null) this.$L = $T.toLong(slots.get($S))",
                        param.field(), param.field(), PlanModel.VALUES, param.field());
                } else if (param.type() == TypeName.BOOLEAN) {
                    importSpec.addStatement("if (slots.get($S) != null) this.$L = $T.toBoolean(slots.get($S))",
                        param.field(), param.field(), PlanModel.VALUES, param.field());
                } else if (PlanModel.KIND_ENUM.equals(param.kind())) {
                    importSpec.addStatement("this.$L = slots.get($S) == null ? null : $T.toEnum($T.class, slots.get($S))",
                        param.field(), param.field(), PlanModel.VALUES, param.type(), param.field());
                } else if (String.class.getName().equals(param.type().toString())) {
                    importSpec.addStatement("this.$L = $T.toText(slots.get($S))", param.field(), PlanModel.VALUES, param.field());
                } else {
                    importSpec.addStatement("this.$L = ($T) $T.decode(slots.get($S), ctx)",
                        param.field(), param.type(), PlanModel.VALUES, param.field());
                }
            }
        }
        if (proc.needsBo()) {
            export.addStatement("slots.put($S, frame.exportState())", "frame");
            importSpec.addStatement("if (slots.get($S) instanceof Map<?, ?> frameState) "
                    + "frame.importState((java.util.Map<String, Object>) frameState, ctx)", "frame");
        }
        tb.addMethod(export.build());
        tb.addMethod(importSpec.build());

        // 存档续跑：子任务重建后把句柄重新挂回运行帧
        if (hasFlow) {
            MethodSpec.Builder restored = MethodSpec.methodBuilder("onChildRestored").addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED).addParameter(int.class, "pc")
                    .addParameter(PlanModel.PROCESS_TASK, "child");
            boolean any = false;
            for (NodeSlot slot : proc.nodes()) {
                if (!slot.storeChild()) {
                    continue;
                }
                ProcInfo callee = model.calleeOf(slot);
                if (callee == null) {
                    continue;
                }
                any = true;
                restored.beginControlFlow("if (pc == $L)", slot.pc());
                restored.addStatement("frame.set$L(($T) child)", slot.childCap(), callee.taskCn());
                restored.endControlFlow();
            }
            if (any) {
                tb.addMethod(restored.build());
            }
        }
        return tb;
    }

    // ------------------------------------------------------------------ RuntimeBO

    /** 运行帧：局部量、节点输出槽位、子过程句柄，以及存档导出 / 回填 */
    JavaFile emitRuntimeBo(PlanModel model, ProcInfo proc) {
        if (!proc.needsBo()) {
            return null;
        }
        TypeSpec.Builder bb = TypeSpec.classBuilder(proc.boCn()).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("过程运行帧（方圆过程原名：$L）：局部临时对象、节点输出槽位与子过程句柄，随过程结束整体回收\n", proc.zhName())
                .addAnnotation(meta(proc.def().entityId() + "#bo", proc.zhName() + "运行帧", "RUNTIME_BO"));
        record Slot(String name, TypeName type, String kind, String zhName, boolean child) {}
        List<Slot> slots = new ArrayList<>();
        for (LocalSlot local : proc.locals()) {
            slots.add(new Slot(local.field(), local.type(), local.kind(), "局部量：" + local.zhName(), false));
            bb.addField(FieldSpec.builder(local.type(), local.field(), Modifier.PRIVATE)
                    .addJavadoc("$L（方圆原名：$L）\n", "局部量", local.zhName()).initializer(local.initLiteral()).build());
        }
        for (NodeSlot slot : proc.nodes()) {
            if (slot.storeOutput()) {
                String field = "out" + slot.pc();
                slots.add(new Slot(field, slot.outType().box(), slot.outType().box() instanceof ParameterizedTypeName
                        ? PlanModel.KIND_LIST : PlanModel.KIND_BASE, "节点输出：" + slot.node().zhName(), false));
                bb.addField(FieldSpec.builder(slot.outType().box(), field, Modifier.PRIVATE)
                        .addJavadoc("节点输出：$L\n", slot.node().zhName()).build());
            }
            if (slot.storeChild()) {
                ProcInfo callee = model.calleeOf(slot);
                if (callee == null) {
                    continue;
                }
                String field = "child" + slot.pc();
                TypeName type = callee.taskCn();
                slots.add(new Slot(field, type, "TASK", "子过程句柄：" + slot.node().zhName(), true));
                bb.addField(FieldSpec.builder(type, field, Modifier.PRIVATE)
                        .addJavadoc("子过程句柄：$L\n", slot.node().zhName()).build());
            }
        }
        for (Slot slot : slots) {
            String cap = PlanModel.cap(slot.name());
            String getter = slot.type() == PlanModel.BOXED_BOOLEAN ? "is" + cap : "get" + cap;
            bb.addMethod(MethodSpec.methodBuilder(getter).addModifiers(Modifier.PUBLIC)
                    .addJavadoc("$L\n", slot.zhName()).returns(slot.type())
                    .addStatement("return this.$L", slot.name()).build());
            bb.addMethod(MethodSpec.methodBuilder("set" + cap).addModifiers(Modifier.PUBLIC)
                    .addParameter(slot.type(), "value")
                    .addStatement("this.$L = value", slot.name()).build());
        }

        MethodSpec.Builder export = MethodSpec.methodBuilder("exportState").addModifiers(Modifier.PUBLIC)
                .returns(PlanModel.mapOfObjects())
                .addStatement("$T<String, Object> state = new java.util.LinkedHashMap<>()", Map.class);
        MethodSpec.Builder importSpec = MethodSpec.methodBuilder("importState").addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
                .addParameter(PlanModel.mapOfObjects(), "state")
                .addParameter(PlanModel.WORLD_CTX, "ctx");
        for (Slot slot : slots) {
            if ("TASK".equals(slot.kind())) {
                // 子过程句柄：只导出过程实例标识；续跑时由世界上下文重建任务树并回调 onChildRestored 挂回
                export.addStatement("state.put($S, this.$L == null ? \"\" : this.$L.getProcessInstanceId())",
                        slot.name(), slot.name(), slot.name());
                continue;
            }
            export.addStatement("state.put($S, $T.encode(this.$L))", slot.name(), PlanModel.VALUES, slot.name());
            TypeName type = slot.type();
            if (type == PlanModel.BOXED_DOUBLE) {
                importSpec.addStatement("if (state.get($S) != null) this.$L = $T.toDouble(state.get($S))",
                        slot.name(), slot.name(), PlanModel.VALUES, slot.name());
            } else if (type == PlanModel.BOXED_LONG) {
                importSpec.addStatement("if (state.get($S) != null) this.$L = $T.toLong(state.get($S))",
                        slot.name(), slot.name(), PlanModel.VALUES, slot.name());
            } else if (type == PlanModel.BOXED_BOOLEAN) {
                importSpec.addStatement("if (state.get($S) != null) this.$L = $T.toBoolean(state.get($S))",
                        slot.name(), slot.name(), PlanModel.VALUES, slot.name());
            } else if (model.enumOf(type) != null) {
                importSpec.addStatement("this.$L = state.get($S) == null ? null : $T.toEnum($T.class, state.get($S))",
                        slot.name(), slot.name(), PlanModel.VALUES, type, slot.name());
            } else if (String.class.getName().equals(type.toString())) {
                importSpec.addStatement("this.$L = state.get($S) == null ? null : $T.toText(state.get($S))",
                        slot.name(), slot.name(), PlanModel.VALUES, slot.name());
            } else {
                // 领域对象引用与集合：解码 @ref: 实例标识后强转回槽位类型
                importSpec.addStatement("this.$L = ($T) $T.decode(state.get($S), ctx)",
                        slot.name(), type, PlanModel.VALUES, slot.name());
            }
        }
        export.addStatement("return state");
        bb.addMethod(export.build());
        bb.addMethod(importSpec.build());
        return JavaFile.builder(proc.boCn().packageName(), bb.build()).indent("    ").build();
    }

    // ------------------------------------------------------------------ 小工具

    static AnnotationSpec meta(String id, String zhName, String kind) {
        return AnnotationSpec.builder(PlanModel.GROMA_META)
                .addMember("id", "$S", id)
                .addMember("zhName", "$S", zhName)
                .addMember("kind", "$S", kind)
                .build();
    }

    static String directionZh(String direction) {
        return switch (direction) {
            case "IN" -> "非空输入形参";
            case "OUT" -> "输出形参";
            case "IN_OUT" -> "双向形参";
            default -> "形参";
        };
    }
}

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
            tb.addMethod(emitCanStart(proc));
            JavaFile bo = emitRuntimeBo(model, proc);
            JavaFile task = JavaFile.builder(proc.taskCn().packageName(), addCommonTail(model, proc, tb, scope, false).build())
                    .indent("    ").build();
            return new Result(task, bo, leaf.scripts);
        }
        tb.addMethod(emitCanStart(proc));
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

    /** 启动条件与非空输入守卫 */
    private MethodSpec emitCanStart(ProcInfo proc) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder("canStart").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(TypeName.BOOLEAN);
        boolean any = false;
        for (ParamSlot param : proc.params()) {
            if (param.nonnull() && param.reads() && !PlanModel.KIND_BASE.equals(param.kind())) {
                mb.addStatement("if (this.$L == null) return false", param.field());
                any = true;
            }
        }
        mb.addStatement("return true");
        return mb.build();
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

        // 3. 后继（条件调用线各自按其出发端口的输出值判断）
        for (NextIR next : node.nexts()) {
            NodeSlot target = proc.node(next.targetPc());
            if (target == null) {
                continue;
            }
            if (!next.conditional()) {
                if (!next.eventCode().isEmpty()) {
                    model.diagnostic("过程 " + proc.zhName() + "：" + node.zhName() + " → " + target.node().zhName()
                            + " 的调用线携带过程事件 " + next.eventCode() + "，暂未编译，已跳过");
                    continue;
                }
                if (next.delayTicks() > 0) {
                    // 连线延迟（存量“等待”子过程语义）：延迟 N 拍后激活后继
                    cb.addStatement("activateAfter($L, $LL)", next.targetPc(), next.delayTicks());
                } else {
                    cb.addStatement("activate($L)", next.targetPc());
                }
            } else {
                Value output = outputValue(model, proc, slot, scope, next.conditionSource());
                CodeBlock judge = output == null ? CodeBlock.of("true") : judgeCode(model, next, output);
                if (judge.toString().equals("true")) {
                    cb.addStatement("activate($L)", next.targetPc());
                } else {
                    cb.beginControlFlow("if ($L)", judge);
                    cb.addStatement("activate($L)", next.targetPc());
                    cb.endControlFlow();
                }
            }
        }
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
        List<CompilePlanIR.ConditionIR> conditions = next.conditions();
        if (conditions == null || conditions.isEmpty()) {
            return CodeBlock.of("true");
        }
        String expression = next.expression() == null ? "" : next.expression().trim();
        if (expression.isEmpty() || expression.matches("\\d+")) {
            int index = expression.isEmpty() ? 1 : Integer.parseInt(expression);
            CompilePlanIR.ConditionIR condition = conditionAt(conditions, index);
            return singleJudge(model, value, condition, condition);
        }
        if (!expression.matches("[\\d\\s&|!()]+")) {
            model.diagnostic("线上判断表达式 " + expression + " 含不支持的记号，按条件全部满足处理");
            return allJudges(model, next, value);
        }
        CodeBlock.Builder composed = CodeBlock.builder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isDigit(c)) {
                int j = i;
                while (j < expression.length() && Character.isDigit(expression.charAt(j))) {
                    j++;
                }
                int index = Integer.parseInt(expression.substring(i, j));
                CompilePlanIR.ConditionIR condition = conditionAt(conditions, index);
                composed.add(singleJudge(model, value, condition, null));
                i = j - 1;
            } else {
                composed.add(Character.toString(c));
            }
        }
        return composed.build();
    }

    private static CodeBlock allJudges(PlanModel model, NextIR next, Value value) {
        CodeBlock.Builder composed = CodeBlock.builder();
        boolean first = true;
        for (CompilePlanIR.ConditionIR condition : next.conditions()) {
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
        for (PostIR post : postActions) {
            if (post.target() == null) {
                continue;
            }
            if ("OUT_CHANGE".equals(post.kind())) {
                EnumInfo info = model.enums.get(post.enumTypeId());
                String constant = info == null ? null : info.constantOf(post.stateLabel());
                if (constant == null) {
                    model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "输出转换的目标状态 " + post.stateLabel() + " 不在枚举里，已跳过");
                    continue;
                }
                cb.add("// 输出转换：$L → $L\n", scope.describeZh(post.target()), post.stateLabel());
                cb.addStatement("$L", scope.assign(post.target(), new Value(CodeBlock.of("$T.$L", info.cn(), constant),
                        info.cn(), PlanModel.KIND_ENUM,
                        com.groma.studio.ir.GromaTypes.enumType(post.enumTypeId()))));
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

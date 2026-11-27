package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.PlanModel.EnumInfo;
import com.groma.studio.codegen.plan.PlanModel.InstanceInfo;
import com.groma.studio.codegen.plan.PlanModel.LocalSlot;
import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.NodeSlot;
import com.groma.studio.codegen.plan.PlanModel.ParamSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.GromaTypes;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

/**
 * 编译计划操作数 → 带静态类型的 Java 表达式
 * <p>
 * 不同生成位置对“宿主自身 / 形参 / 局部量 / 上游节点输出”的写法不同（过程任务里是 self 与 frame，
 * 接线启动器里是 contextEntity），由子类通过几个钩子给出；成员路径、常量、内置对象与赋值语句的规则在这里统一实现。
 * </p>
 */
public abstract class OperandRenderer {

    protected final PlanModel model;
    /** 当前所属过程（解析形参、局部量与上游节点输出时使用；接线与实例构建场景下为 null） */
    protected ProcInfo proc;

    protected OperandRenderer(PlanModel model, ProcInfo proc) {
        this.model = model;
        this.proc = proc;
    }

    /**
     * 带静态类型的表达式：placeholder 是类型占位（便于推出集合元素类型），kind 是 BASE / ENUM / REF / LIST / MAP
     */
    public record Value(CodeBlock code, TypeName type, String kind, String placeholder) {
        public Value(CodeBlock code, TypeName type, String kind) {
            this(code, type, kind, PlanModel.KIND_BASE.equals(kind) ? type.toString() : "");
        }
    }

    /** 集合的元素类型占位（非集合返回空串） */
    public String elementPlaceholder(Value value) {
        return value == null ? "" : com.groma.studio.ir.GromaTypes.elementTypeOf(value.placeholder());
    }

    // ------------------------------------------------------------------ 子类钩子

    /** 世界上下文在当前生成位置的表达式（如 ctx / c / this） */
    protected abstract CodeBlock ctxRef();

    /** 宿主对象自身 */
    protected abstract CodeBlock selfRef(TypeName selfType);

    /** 读取形参槽位 */
    protected abstract CodeBlock readParam(ParamSlot slot);

    /** 读取局部量槽位 */
    protected abstract CodeBlock readLocal(LocalSlot slot);

    /** 写入局部量槽位 */
    protected abstract CodeBlock writeLocal(LocalSlot slot, CodeBlock value);

    /** 命名实例访问表达式（未强转） */
    protected abstract CodeBlock namedInstanceRef(String instanceId);

    /** 命名集合实例访问表达式（未强转；数组 / 映射实例） */
    protected abstract CodeBlock namedCollectionRef(String instanceId);

    /**
     * 读取已成功结束的被调过程的形参终值（调用点后置动作里的输出影响回写用）。
     * 过程任务内部走节点回写（frame 里的子任务句柄），只有接线启动器的后置钩子需要它；默认不支持。
     */
    protected CodeBlock readCompletedParam(ProcInfo callee, ParamSlot slot) {
        throw new IllegalStateException("当前位置不支持读取被调过程的输出形参: " + slot.zhName());
    }

    // ------------------------------------------------------------------ 入口

    public Value render(CompilePlanIR.OperandIR operand) {
        if (operand == null) {
            return new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
        }
        if (operand instanceof CompilePlanIR.SelfIR self) {
            TypeName type = model.resolve(self.javaType());
            return new Value(selfRef(type), type, PlanModel.KIND_REF, self.javaType());
        }
        if (operand instanceof CompilePlanIR.MemberPathIR path) {
            return renderPath(path);
        }
        if (operand instanceof CompilePlanIR.ParamIR param) {
            ParamSlot slot = proc == null ? null : paramByEntityId(param.entityId());
            if (slot == null) {
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "形参 " + param.zhName() + " 未在定义视图中声明，按空值处理");
                return new Value(CodeBlock.of("null"), model.resolve(param.javaType()), PlanModel.kindOf(param.javaType()));
            }
            return new Value(readParam(slot), slot.type(), slot.kind(), slot.placeholder());
        }
        if (operand instanceof CompilePlanIR.LocalIR local) {
            LocalSlot slot = localByEntityId(local.entityId());
            if (slot == null) {
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "局部量 " + local.zhName() + " 未声明，按空值处理");
                return new Value(CodeBlock.of("null"), model.resolve(local.javaType()), PlanModel.kindOf(local.javaType()));
            }
            return new Value(readLocal(slot), slot.type(), slot.kind(), slot.placeholder());
        }
        if (operand instanceof CompilePlanIR.ConstIR constant) {
            return renderConstant(constant);
        }
        if (operand instanceof CompilePlanIR.NodeOutputIR output) {
            return renderNodeOutput(output);
        }
        if (operand instanceof CompilePlanIR.NamedInstanceIR named) {
            TypeInfo type = model.types.get(named.typeId());
            TypeName targetType = type == null ? model.resolve(named.javaType()) : type.entity();
            boolean known = model.instances.stream().anyMatch(i -> i.instanceId().equals(named.instanceId()));
            if (!known) {
                model.diagnostic("命名实例 " + named.instanceId() + " 不在模型对象图里，按空值处理");
                return new Value(CodeBlock.of("null"), targetType, PlanModel.KIND_REF);
            }
            if (GromaTypes.isCollection(named.javaType())) {
                // 数组 / 映射实例：不是实体，经命名集合实例访问器取（元素仍是命名实例）
                TypeName collectionType = model.resolve(named.javaType());
                String kind = GromaTypes.isMap(named.javaType()) ? PlanModel.KIND_MAP : PlanModel.KIND_LIST;
                return new Value(CodeBlock.of("($T) $L", collectionType, namedCollectionRef(named.instanceId())),
                        collectionType, kind, named.javaType());
            }
            return new Value(CodeBlock.of("($T) $L", targetType, namedInstanceRef(named.instanceId())), targetType,
                    PlanModel.KIND_REF, named.javaType());
        }
        if (operand instanceof CompilePlanIR.BuiltinIR builtin) {
            return renderBuiltin(builtin);
        }
        model.diagnostic("未知操作数类型 " + operand.getClass().getSimpleName());
        return new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
    }

    /** 命名实例的中文名（找不到时退回实例标识） */
    protected final String namedZh(String instanceId) {
        for (InstanceInfo instance : model.instances) {
            if (instance.instanceId().equals(instanceId)) {
                return instance.zhName().isEmpty() ? instanceId : instance.zhName();
            }
        }
        return instanceId;
    }

    /** 操作数的中文描述（写进生成代码的注释，便于对照画布） */
    public String describeZh(CompilePlanIR.OperandIR operand) {
        if (operand == null) {
            return "空";
        }
        if (operand instanceof CompilePlanIR.SelfIR) {
            return "本对象";
        }
        if (operand instanceof CompilePlanIR.MemberPathIR path) {
            StringBuilder sb = new StringBuilder(describeZh(path.root()));
            for (String memberId : path.memberIds()) {
                sb.append('.').append(memberZh(memberId));
            }
            return sb.toString();
        }
        if (operand instanceof CompilePlanIR.ParamIR param) {
            return param.zhName();
        }
        if (operand instanceof CompilePlanIR.LocalIR local) {
            return local.zhName();
        }
        if (operand instanceof CompilePlanIR.ConstIR constant) {
            return constant.zhName() == null || constant.zhName().isEmpty() ? constant.literal() : constant.zhName();
        }
        if (operand instanceof CompilePlanIR.NodeOutputIR output) {
            NodeSlot source = proc == null ? null : proc.node(pcOfCell(output.cellId()));
            return (source == null ? "上游节点" : source.node().zhName()) + "." + output.argName();
        }
        if (operand instanceof CompilePlanIR.NamedInstanceIR named) {
            return "实例" + namedZh(named.instanceId());
        }
        if (operand instanceof CompilePlanIR.BuiltinIR builtin) {
            String key = switch (builtin.key()) {
                case "SIM_DATETIME" -> "仿真时间.时间日期";
                case "SIM_ELAPSED_SECONDS" -> "仿真时间.持续运行时长";
                case "SIM_STEP_SECONDS" -> "ΔT（本步秒数）";
                case "LENGTH" -> describeZh(builtin.base()) + ".长度";
                default -> builtin.key();
            };
            return key;
        }
        return "?";
    }

    /** 成员 id → 中文原名 */
    public String memberZh(String memberId) {
        for (TypeInfo type : model.types.values()) {
            MemberInfo member = type.member(memberId);
            if (member != null) {
                return member.zhName();
            }
        }
        return memberId;
    }

    public Value renderNull() {
        return new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
    }

    // ------------------------------------------------------------------ 成员路径

    private Value renderPath(CompilePlanIR.MemberPathIR path) {
        Value current = render(path.root());
        for (String memberId : path.memberIds()) {
            if (PlanModel.KIND_BASE.equals(current.kind()) || PlanModel.KIND_ENUM.equals(current.kind())) {
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "成员路径在基础值 " + current.type() + " 上继续取成员 " + memberId + "，按空值处理");
                return new Value(CodeBlock.of("null"), model.resolve(path.javaType()), PlanModel.kindOf(path.javaType()));
            }
            TypeInfo ownerType = model.typeOf(current.type());
            MemberInfo member = ownerType == null ? null : ownerType.member(memberId);
            if (member == null) {
                // 静态类型未知（占位为 Object）时按目标成员自身类型处理，交给运行期
                TypeName fallback = model.resolve(path.javaType());
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "成员路径的宿主类型未知，成员 " + memberId + " 无法静态解析，按空值处理");
                return new Value(CodeBlock.of("null"), fallback, PlanModel.kindOf(path.javaType()));
            }
            current = new Value(CodeBlock.of("$L.$L()", current.code(), member.getter()), member.type(), member.kind(),
                    member.placeholder());
        }
        return current;
    }

    /** 路径除最后一步外的对象表达式（即最后成员的宿主） */
    private Value renderPathBase(CompilePlanIR.MemberPathIR path) {
        if (path.memberIds().isEmpty()) {
            return render(path.root());
        }
        Value current = render(path.root());
        for (int i = 0; i < path.memberIds().size() - 1; i++) {
            TypeInfo ownerType = model.typeOf(current.type());
            MemberInfo member = ownerType == null ? null : ownerType.member(path.memberIds().get(i));
            if (member == null) {
                return null;
            }
            current = new Value(CodeBlock.of("$L.$L()", current.code(), member.getter()), member.type(), member.kind(),
                    member.placeholder());
        }
        return current;
    }

    // ------------------------------------------------------------------ 常量、节点输出、内置对象

    private Value renderConstant(CompilePlanIR.ConstIR constant) {
        String placeholder = constant.javaType() == null || constant.javaType().isEmpty()
                ? GromaTypes.OBJECT : constant.javaType();
        String kind = PlanModel.kindOf(placeholder);
        TypeName type = model.resolve(placeholder);
        if (PlanModel.KIND_ENUM.equals(kind)) {
            EnumInfo info = model.enumOf(type);
            String constantName = info == null ? null : info.constantOf(constant.literal());
            if (constantName == null && (constant.literal() == null || constant.literal().isBlank())) {
                constantName = info == null ? null : info.initialConst();
            }
            if (constantName != null) {
                return new Value(CodeBlock.of("$T.$L", info.cn(), constantName), type, kind, placeholder);
            }
            if (info != null) {
                return new Value(CodeBlock.of("$T.toEnum($T.class, $S)", PlanModel.VALUES, info.cn(), constant.literal()), type, kind, placeholder);
            }
            return new Value(CodeBlock.of("null"), type, kind, placeholder);
        }
        if (PlanModel.KIND_REF.equals(kind)) {
            return new Value(CodeBlock.of("null"), type, kind, placeholder);
        }
        if (type == TypeName.DOUBLE) {
            return new Value(CodeBlock.of(PlanModel.doubleLiteral(PlanModel.parseDouble(constant.literal()))), type, kind, placeholder);
        }
        if (type == TypeName.LONG) {
            Long epoch = PlanModel.parseLong(constant.literal()) != null ? PlanModel.parseLong(constant.literal())
                    : PlanModel.parseEpochMillis(constant.literal());
            if (epoch == null) {
                Double parsed = PlanModel.parseDouble(constant.literal());
                epoch = parsed == null ? 0L : Math.round(parsed);
            }
            return new Value(CodeBlock.of(epoch + "L"), type, kind, placeholder);
        }
        if (type == TypeName.BOOLEAN) {
            boolean flag = "true".equalsIgnoreCase(constant.literal()) || "1".equals(constant.literal()) || "是".equals(constant.literal());
            return new Value(CodeBlock.of(flag ? "true" : "false"), type, kind, placeholder);
        }
        return new Value(CodeBlock.of("$S", constant.literal() == null ? "" : constant.literal()), type, kind, placeholder);
    }

    private Value renderNodeOutput(CompilePlanIR.NodeOutputIR output) {
        NodeSlot source = proc == null ? null : proc.node(pcOfCell(output.cellId()));
        if (source == null) {
            model.diagnostic("上游节点输出引用了未知节点 " + output.cellId());
            return new Value(CodeBlock.of("null"), model.resolve(output.javaType()), PlanModel.kindOf(output.javaType()));
        }
        if (!source.isCall()) {
            return new Value(CodeBlock.of("frame.$L()", getterOfOut(source)), source.outType(),
                    PlanModel.kindOf(source.node().resultJavaType()), source.node().resultJavaType());
        }
        ProcInfo callee = model.calleeOf(source);
        ParamSlot outParam = callee == null ? null : callee.param(output.argName());
        if (callee == null || outParam == null) {
            model.diagnostic("子过程输出形参 " + output.argName() + " 在被调过程里不存在，按空值处理");
            return new Value(CodeBlock.of("null"), model.resolve(output.javaType()), PlanModel.kindOf(output.javaType()));
        }
        return new Value(CodeBlock.of("frame.$L().$L()", getterOfChild(source), outParam.getter()), outParam.type(),
                outParam.kind(), outParam.placeholder());
    }

    private Value renderBuiltin(CompilePlanIR.BuiltinIR builtin) {
        CodeBlock ctx = ctxRef();
        return switch (builtin.key()) {
            case "SIM_DATETIME" -> new Value(CodeBlock.of("$L.currentWorldTimeMs()", ctx), TypeName.LONG, PlanModel.KIND_BASE);
            case "SIM_ELAPSED_SECONDS" -> new Value(CodeBlock.of("$L.elapsedSeconds()", ctx), TypeName.DOUBLE, PlanModel.KIND_BASE);
            case "SIM_STEP_SECONDS" -> new Value(CodeBlock.of("$L.getFinalStepMs() / 1000.0D", ctx), TypeName.DOUBLE, PlanModel.KIND_BASE);
            case "LENGTH" -> {
                Value base = render(builtin.base());
                yield new Value(CodeBlock.of("$T.length($L)", PlanModel.BUILTIN_TOOLS, base.code()), TypeName.LONG, PlanModel.KIND_BASE);
            }
            case "INDEX" -> new Value(CodeBlock.of("0L"), TypeName.LONG, PlanModel.KIND_BASE);
            default -> {
                model.diagnostic("内置对象取值 " + builtin.key() + " 暂不支持，按空值处理");
                yield new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
            }
        };
    }

    // ------------------------------------------------------------------ 类型转换与赋值

    /**
     * 把表达式转换为目标类型（数值取整、文本转枚举、对象强转、布尔与文本的相互转换）
     */
    public CodeBlock coerce(Value value, TypeName target, String targetKind) {
        if (value == null) {
            return CodeBlock.of(model.defaultLiteral(target, targetKind));
        }
        if (target instanceof com.palantir.javapoet.ParameterizedTypeName pt
                && !(value.type() instanceof com.palantir.javapoet.ParameterizedTypeName)) {
            // 集合形参收到标量实参（模型 2 实证）：出码原样必然类型不匹配，回退形参默认值并记诊断
            if (value.type() == TypeName.BOOLEAN || value.type() == TypeName.DOUBLE || value.type() == TypeName.LONG
                    || "java.lang.String".equals(String.valueOf(value.type()))
                    || value.type().toString().startsWith("java.lang.String")) {
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "集合形参收到标量实参（来源 "
                        + value.type() + "），已回退默认值");
                return CodeBlock.of(model.defaultLiteral(target, targetKind));
            }
            return value.code();
        }
        if (target instanceof com.palantir.javapoet.ParameterizedTypeName pt
                && value.type() instanceof com.palantir.javapoet.ParameterizedTypeName vt && !pt.equals(vt)) {
            // 集合元素类型不同（模型 2 实证：List<子类> 实参传 List<父类> 形参）：
            // 元素同源（实参元素是形参元素的后代）时包一层新集合完成协变转换，无关系则降级默认值
            TypeName targetElem = pt.typeArguments().isEmpty() ? null : pt.typeArguments().get(0);
            TypeName valueElem = vt.typeArguments().isEmpty() ? null : vt.typeArguments().get(0);
            if (PlanModel.isEntity(targetElem) && PlanModel.isEntity(valueElem)) {
                TypeInfo tInfo = model.typeOf(targetElem);
                TypeInfo vInfo = model.typeOf(valueElem);
                for (TypeInfo t = vInfo; t != null && tInfo != null; t = t.parent()) {
                    if (t == tInfo) {
                        return CodeBlock.of("new $T<>($L)", java.util.ArrayList.class, value.code());
                    }
                }
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "集合形参元素类型与实参不匹配（"
                        + valueElem + " → " + targetElem + "），已回退默认值");
                return CodeBlock.of(model.defaultLiteral(target, targetKind));
            }
        }
        TypeName source = value.type();
        if (target.isBoxedPrimitive()) {
            // 运行帧槽位是装箱类型：按对应原生类型转换，调用处自动装箱
            target = target.unbox();
        }
        boolean sourceNumeric = source == TypeName.DOUBLE || source == TypeName.LONG;
        if (target == TypeName.DOUBLE && sourceNumeric) {
            return value.code();
        }
        if (target == TypeName.DOUBLE && source.isBoxedPrimitive()) {
            return CodeBlock.of("(double) ($L)", value.code());
        }
        if (target == TypeName.DOUBLE) {
            return CodeBlock.of("$T.toDoubleValue($L)", PlanModel.BUILTIN_TOOLS, value.code());
        }
        if (target == TypeName.LONG && source == TypeName.LONG) {
            return value.code();
        }
        if (target == TypeName.LONG && source == TypeName.DOUBLE) {
            return CodeBlock.of("$T.roundToLong($L)", PlanModel.BUILTIN_TOOLS, value.code());
        }
        if (target == TypeName.LONG && source.isBoxedPrimitive()) {
            return CodeBlock.of("(long) ($L)", value.code());
        }
        if (target == TypeName.LONG) {
            return CodeBlock.of("$T.toLong($L)", PlanModel.VALUES, value.code());
        }
        if (target == TypeName.BOOLEAN) {
            return source == TypeName.BOOLEAN ? value.code()
                    : CodeBlock.of("$T.isTrue($L)", PlanModel.BUILTIN_TOOLS, value.code());
        }
        boolean targetIsString = String.class.getName().equals(target.toString());
        if (targetIsString) {
            if (String.class.getName().equals(source.toString())) {
                return value.code();
            }
            if (PlanModel.KIND_ENUM.equals(value.kind())) {
                return CodeBlock.of("$T.enumZhLabel($L)", PlanModel.BASE_ENTITY, value.code());
            }
            return CodeBlock.of("String.valueOf($L)", value.code());
        }
        if (PlanModel.KIND_ENUM.equals(targetKind)) {
            if (source.equals(target)) {
                return value.code();
            }
            EnumInfo info = model.enumOf(target);
            if (info == null) {
                return CodeBlock.of("null");
            }
            return CodeBlock.of("$T.toEnum($T.class, $L)", PlanModel.VALUES, info.cn(), value.code());
        }
        if (PlanModel.KIND_REF.equals(targetKind) && PlanModel.isEntity(target) && !source.equals(target)) {
            if (PlanModel.isObject(source) || PlanModel.isEntity(source)) {
                return CodeBlock.of("($T) ($L)", target, value.code());
            }
        }
        return value.code();
    }

    /** 写入目标：成员路径 → 调用宿主对象上带事件写入的 Setter；形参 / 局部量 → 写槽位 */
    public CodeBlock assign(CompilePlanIR.OperandIR target, Value value) {
        if (target == null) {
            return CodeBlock.of("");
        }
        if (target instanceof CompilePlanIR.MemberPathIR path) {
            Value base = renderPathBase(path);
            String lastId = path.memberIds().get(path.memberIds().size() - 1);
            TypeInfo ownerType = base == null ? null : model.typeOf(base.type());
            MemberInfo member = ownerType == null ? null : ownerType.member(lastId);
            if (member == null) {
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "写出目标的成员 " + lastId + " 无法静态解析，已跳过");
                return CodeBlock.of("");
            }
            // 集合成员收到非集合来源（模型 2 实证：画布把文本赋给 List 成员）：类型不匹配的赋值
            // 若原样出码必编译失败——按“模块始终可编译”红线降级为注释并记诊断
            if ((PlanModel.KIND_LIST.equals(member.kind()) || PlanModel.KIND_MAP.equals(member.kind()))
                    && !(value.type() instanceof com.palantir.javapoet.ParameterizedTypeName)) {
                model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "把非集合值赋给集合成员 " + lastId
                        + "（来源 " + value.type() + "），已降级注释跳过");
                return CodeBlock.of("");
            }
            return CodeBlock.of("$L.$L($L, $L)", base.code(), member.setterCtx(), coerce(value, member.type(), member.kind()), ctxRef());
        }
        if (target instanceof CompilePlanIR.ParamIR param) {
            ParamSlot slot = proc == null ? null : paramByEntityId(param.entityId());
            if (slot == null) {
                return CodeBlock.of("");
            }
            return CodeBlock.of("this.$L = $L", slot.field(), coerce(value, slot.type(), slot.kind()));
        }
        if (target instanceof CompilePlanIR.LocalIR local) {
            LocalSlot slot = localByEntityId(local.entityId());
            if (slot == null) {
                return CodeBlock.of("");
            }
            return CodeBlock.of("frame.set$L($L)", slot.cap(), coerce(value, slot.type(), slot.kind()));
        }
        model.diagnostic((proc == null ? "" : proc.zhName() + "：") + "写出目标 " + GromaTypes.describe(target) + " 不是可写位置，已跳过");
        return CodeBlock.of("");
    }

    /** 成员路径目标的宿主对象与成员（用于“生成对象”等需要宿主与成员标识的后置动作） */
    public record WriteTarget(Value owner, MemberInfo member, LocalSlot local, ParamSlot param) {}

    public WriteTarget writeTarget(CompilePlanIR.OperandIR target) {
        if (target instanceof CompilePlanIR.MemberPathIR path) {
            Value base = renderPathBase(path);
            String lastId = path.memberIds().get(path.memberIds().size() - 1);
            TypeInfo ownerType = base == null ? null : model.typeOf(base.type());
            MemberInfo member = ownerType == null ? null : ownerType.member(lastId);
            return member == null ? null : new WriteTarget(base, member, null, null);
        }
        if (target instanceof CompilePlanIR.LocalIR local) {
            LocalSlot slot = localByEntityId(local.entityId());
            return slot == null ? null : new WriteTarget(null, null, slot, null);
        }
        if (target instanceof CompilePlanIR.ParamIR param) {
            ParamSlot slot = proc == null ? null : paramByEntityId(param.entityId());
            return slot == null ? null : new WriteTarget(null, null, null, slot);
        }
        return null;
    }

    /** 目标成员的类型信息 */
    public Value targetType(CompilePlanIR.OperandIR target) {
        if (target instanceof CompilePlanIR.LocalIR local) {
            LocalSlot slot = localByEntityId(local.entityId());
            return slot == null ? null : new Value(CodeBlock.of(""), slot.type(), slot.kind());
        }
        if (target instanceof CompilePlanIR.ParamIR param) {
            ParamSlot slot = paramByEntityId(param.entityId());
            return slot == null ? null : new Value(CodeBlock.of(""), slot.type(), slot.kind());
        }
        if (target instanceof CompilePlanIR.MemberPathIR path) {
            WriteTarget wt = writeTarget(path);
            return wt == null || wt.member() == null ? null : new Value(CodeBlock.of(""), wt.member().type(), wt.member().kind());
        }
        return null;
    }

    // ------------------------------------------------------------------ 查找辅助

    public int pcOfCell(String cellId) {
        if (proc == null) {
            return -1;
        }
        for (NodeSlot slot : proc.nodes()) {
            if (slot.node().cellId().equals(cellId)) {
                return slot.pc();
            }
        }
        return -1;
    }

    public ParamSlot paramByEntityId(String entityId) {
        if (proc == null || entityId == null) {
            return null;
        }
        for (ParamSlot slot : proc.params()) {
            if (slot.entityId().equals(entityId)) {
                return slot;
            }
        }
        return null;
    }

    public LocalSlot localByEntityId(String entityId) {
        if (proc == null || entityId == null) {
            return null;
        }
        for (LocalSlot slot : proc.locals()) {
            if (slot.entityId().equals(entityId)) {
                return slot;
            }
        }
        return null;
    }

    public static String getterOfOut(NodeSlot slot) {
        return "get" + slot.outCap();
    }

    public static String getterOfChild(NodeSlot slot) {
        return "get" + slot.childCap();
    }

    public static String setterOfOut(NodeSlot slot) {
        return "set" + slot.outCap();
    }

    public static String setterOfChild(NodeSlot slot) {
        return "set" + slot.childCap();
    }

    /** 供生成器在接线 / 实例构建位置复用：清空当前过程上下文 */
    public OperandRenderer detached() {
        this.proc = null;
        return this;
    }
}

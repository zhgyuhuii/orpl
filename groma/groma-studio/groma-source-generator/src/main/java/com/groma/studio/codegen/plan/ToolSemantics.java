package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.OperandRenderer.Value;
import com.groma.studio.codegen.plan.PlanModel.NodeSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import java.util.Set;

/**
 * 画布“工具过程”库的 Java 语义表（落实设计书第五章“横切语义”与 3.2 节“全算子组合直接内联编译为 ServiceImpl 内的原生代码块”）
 * <p>
 * 每个内置工具（innerClass.innerType）对应一段原生 Java：算术 / 比较 / 逻辑直接内联为运算符，
 * 除法与集合、精度、时间类运算走底座 {@code BuiltinTools} 的零反射方法。
 * 依赖外部本体数据或尚无编译语义的工具不会静默忽略：生成的代码以明确原因让过程失败。
 * </p>
 * 两类工具：
 * <ul>
 *   <li>有值工具：结果写入过程运行帧的输出槽位，由 nodeDone 回写到锚点线绑定的目标；</li>
 *   <li>无值工具（自增减、清空、集合修改、打印）：在 execNode 里直接完成写入。</li>
 * </ul>
 */
public final class ToolSemantics {

    /** 无返回值、直接完成写入的工具 */
    private static final Set<String> VOID_TOOLS = Set.of(
            "automatic.inc", "automatic.dec", "automatic.incByVal", "automatic.decByVal",
            "assignment.clear", "assignment.clearRef", "assignment.resetToOne",
            "innerCollectionArray.addHead", "innerCollectionArray.addTail", "innerCollectionArray.addAt",
            "innerCollectionArray.setByIndex", "innerCollectionArray.clear", "innerCollectionArray.copyElement",
            "innerCollectionSet.add", "innerCollectionSet.remove", "innerCollectionSet.clear",
            "innerCollectionSet.copyElement",
            "innerCollectionMap.putKeyValue", "innerCollectionMap.remove", "innerCollectionMap.clear",
            "innerCollectionMap.copyKeyValue", "types.updateOntologyData", "other.print");

    private ToolSemantics() {
    }

    public static boolean isVoidTool(String toolKey) {
        return toolKey != null && VOID_TOOLS.contains(toolKey);
    }

    /** 是否已知语义（未知工具按“暂不支持”生成失败分支） */
    public static boolean isSupported(String key) {
        if (key == null) {
            return false;
        }
        String head = key.substring(0, key.indexOf('.') + 1);
        return switch (head) {
            case "arithmetic.", "compare.", "logic.", "automatic.", "bitwise.", "shift.", "trigonometric.", "accuracy." -> true;
            case "assignment." -> !key.equals("assignment.setValue");
            case "isNull." -> true;
            case "innerCollectionArray." -> !key.equals("innerCollectionArray.textToElement");
            case "innerCollectionSet.", "innerCollectionMap." -> true;
            case "text." -> Set.of("text.becomeString", "text.containText", "text.extractText", "text.getTextLength",
                    "text.replaceText", "text.splicingText", "text.textToBoolean", "text.textToDate", "text.textToDateTime",
                    "text.textToDouble", "text.textToLong", "text.textToTime", "text.joinText").contains(key);
            case "time." -> !key.equals("time.formatDuration");
            case "other." -> Set.of("other.print", "other.finishTask", "other.getPkCode").contains(key);
            default -> false;
        };
    }

    /**
     * 生成工具节点 execNode 分支的语句体
     */
    public static CodeBlock emit(PlanModel model, ProcInfo proc, NodeSlot slot, OperandRenderer ops) {
        Emitter e = new Emitter(model, proc, slot, ops);
        String key = slot.node().toolKey();
        CodeBlock.Builder cb = CodeBlock.builder();

        // 中文语义注释：工具名 + 各入参的画布含义
        StringBuilder note = new StringBuilder(slot.node().zhName()).append("（").append(key).append("）");
        for (CompilePlanIR.ArgBindingIR binding : slot.node().inputs()) {
            note.append(" ").append(binding.argName()).append(" = ").append(ops.describeZh(binding.operand())).append(";");
        }
        if (!slot.node().writes().isEmpty()) {
            note.append(" 写出:");
            for (CompilePlanIR.ArgBindingIR binding : slot.node().writes()) {
                note.append(" ").append(binding.argName()).append(" → ").append(ops.describeZh(binding.operand())).append(";");
            }
        }
        cb.add("// $L\n", note.toString().replace("$", "＄"));

        if (!isSupported(key)) {
            cb.addStatement("fail($T.unsupported($S))", PlanModel.BUILTIN_TOOLS, key);
            return cb.build();
        }
        e.emit(key);
        for (CodeBlock pre : e.pre) {
            cb.add(pre);
        }
        if (e.resultExpr != null) {
            // 结果先按工具自身的类型求值，再按运行帧槽位类型转换后存入（槽位为装箱类型，调用处自动装箱）
            cb.addStatement("$T $L = $L", e.resultType, "r", e.resultExpr);
            if (slot.storeOutput()) {
                OperandRenderer.Value computed = new OperandRenderer.Value(
                        CodeBlock.of("r"), e.resultType, PlanModel.KIND_BASE, e.resultType.toString());
                cb.addStatement("frame.$L($L)", OperandRenderer.setterOfOut(slot),
                        ops.coerce(computed, slot.outType(), PlanModel.KIND_BASE));
            }
        }
        for (CodeBlock effect : e.effects) {
            cb.add(effect);
        }
        return cb.build();
    }

    // ====================================================================================== 语义发射

    private static final class Emitter {
        private final PlanModel model;
        private final ProcInfo proc;
        private final NodeSlot slot;
        private final OperandRenderer ops;
        /** 入参局部变量声明 */
        private final java.util.List<CodeBlock> pre = new java.util.ArrayList<>();
        /** 无值工具的直接写入语句 */
        private final java.util.List<CodeBlock> effects = new java.util.ArrayList<>();
        private TypeName resultType;
        private CodeBlock resultExpr;
        private int counter;

        Emitter(PlanModel model, ProcInfo proc, NodeSlot slot, OperandRenderer ops) {
            this.model = model;
            this.proc = proc;
            this.slot = slot;
            this.ops = ops;
        }

        // ---------------- 局部变量

        private Value raw(String argName) {
            CompilePlanIR.ArgBindingIR binding = binding(slot.node().inputs(), argName);
            if (binding == null) {
                model.diagnostic(proc.zhName() + "：工具 " + slot.node().toolKey() + " 的入参 " + argName + " 未绑定，按零值处理");
                return new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
            }
            return ops.render(binding.operand());
        }

        private static CompilePlanIR.ArgBindingIR binding(java.util.List<CompilePlanIR.ArgBindingIR> list, String argName) {
            for (CompilePlanIR.ArgBindingIR b : list) {
                if (b.argName().equals(argName)) {
                    return b;
                }
            }
            return null;
        }

        /** 以指定类型声明一个入参局部变量 */
        private Value local(String argName, TypeName type, String kind, String placeholder) {
            Value src = raw(argName);
            String name = "v" + (counter++);
            pre.add(CodeBlock.of("$T $L = $L;\n", type, name, ops.coerce(src, type, kind)));
            return new Value(CodeBlock.of(name), type, kind, placeholder);
        }

        private Value numeric(String argName) {
            return local(argName, TypeName.DOUBLE, PlanModel.KIND_BASE, "double");
        }

        private Value integral(String argName) {
            return local(argName, TypeName.LONG, PlanModel.KIND_BASE, "long");
        }

        private Value bool(String argName) {
            return local(argName, TypeName.BOOLEAN, PlanModel.KIND_BASE, "boolean");
        }

        private Value text(String argName) {
            return local(argName, ClassName.get(String.class), PlanModel.KIND_BASE, "String");
        }

        /** 保持操作数自身类型 */
        private Value same(String argName) {
            Value src = raw(argName);
            return local(argName, src.type(), src.kind(), src.placeholder());
        }

        private Value object(String argName) {
            return local(argName, PlanModel.OBJECT_TYPE, PlanModel.KIND_REF, "Object");
        }

        private Value any(String argName) {
            Value src = raw(argName);
            String name = "v" + (counter++);
            pre.add(CodeBlock.of("$T $L = $L;\n", src.type().box(), name, src.code()));
            return new Value(CodeBlock.of(name), src.type().box(), src.kind(), src.placeholder());
        }

        private Value host() {
            if (slot.node().host() != null) {
                return ops.render(slot.node().host());
            }
            return binding(slot.node().inputs(), "a") != null ? same("a") : new Value(CodeBlock.of("null"), PlanModel.OBJECT_TYPE, PlanModel.KIND_REF);
        }

        private void result(TypeName type, CodeBlock expr) {
            this.resultType = type;
            this.resultExpr = expr;
        }

        /** 集合取值返回 Object：按元素类型强转（元素类型未知时保持 Object） */
        private void resultFromObject(TypeName type, CodeBlock expr) {
            if (type == PlanModel.OBJECT_TYPE) {
                result(type, expr);
            } else {
                result(type, CodeBlock.of("($T) $L", type, expr));
            }
        }

        private void effect(CodeBlock code) {
            effects.add(code);
        }

        /** 第一个写出目标（双向端口 a 的回写位置） */
        private CompilePlanIR.OperandIR firstWriteTarget() {
            for (CompilePlanIR.ArgBindingIR b : slot.node().writes()) {
                return b.operand();
            }
            return null;
        }

        private void writeAll(CodeBlock value, TypeName type, String kind) {
            for (CompilePlanIR.ArgBindingIR b : slot.node().writes()) {
                effect(CodeBlock.of("$L;\n", ops.assign(b.operand(), new Value(value, type, kind))));
            }
        }

        // ---------------- 语义表

        private void emit(String key) {
            String tool = key.substring(key.indexOf('.') + 1);
            if (key.startsWith("arithmetic.")) {
                arithmetic(tool);
            } else if (key.startsWith("compare.")) {
                compare(tool);
            } else if (key.startsWith("logic.")) {
                logic(tool);
            } else if (key.equals("isNull.isNull")) {
                Value a = any("a");
                result(TypeName.BOOLEAN, CodeBlock.of("$L == null", a.code()));
            } else if (key.startsWith("assignment.")) {
                assignment(tool);
            } else if (key.startsWith("automatic.")) {
                automatic(tool);
            } else if (key.startsWith("bitwise.")) {
                bitwise(tool);
            } else if (key.startsWith("shift.")) {
                shift(tool);
            } else if (key.startsWith("accuracy.")) {
                accuracy(tool);
            } else if (key.startsWith("trigonometric.")) {
                trigonometric(tool);
            } else if (key.startsWith("text.")) {
                textTool(tool);
            } else if (key.startsWith("time.")) {
                timeTool(tool);
            } else if (key.startsWith("innerCollectionArray.")) {
                arrayTool(tool);
            } else if (key.startsWith("innerCollectionSet.")) {
                setTool(tool);
            } else if (key.startsWith("innerCollectionMap.")) {
                mapTool(tool);
            } else if (key.equals("other.print")) {
                Value a = any("a");
                effect(CodeBlock.of("$L.recordPublicProblem($S + $L);\n", ops.ctxRef(), "[打印] ", a.code()));
            } else if (key.equals("other.finishTask")) {
                effect(CodeBlock.of("$L.requestTerminate();\n", ops.ctxRef()));
            } else if (key.equals("other.getPkCode")) {
                result(ClassName.get(String.class), CodeBlock.of("$L.nextInstanceId($S)", ops.ctxRef(), "uuid"));
            } else {
                throw new IllegalStateException("工具语义缺失: " + key);
            }
        }

        private void arithmetic(String tool) {
            switch (tool) {
                case "add" -> binary("$L + $L");
                case "minus" -> binary("$L - $L");
                case "multiply" -> binary("$L * $L");
                case "divide" -> {
                    Value a = numeric("a");
                    Value b = numeric("b");
                    result(TypeName.DOUBLE, CodeBlock.of("$T.divide($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                case "power" -> binary(TypeName.DOUBLE, "$T.pow($L, $L)", java.lang.Math.class);
                case "max" -> binary(TypeName.DOUBLE, "$T.max($L, $L)", java.lang.Math.class);
                case "min" -> binary(TypeName.DOUBLE, "$T.min($L, $L)", java.lang.Math.class);
                case "reverse" -> {
                    Value src = raw("a");
                    boolean integralValue = src.type() == TypeName.LONG;
                    Value a = integralValue ? integral("a") : numeric("a");
                    result(integralValue ? TypeName.LONG : TypeName.DOUBLE, CodeBlock.of("-$L", a.code()));
                }
                case "absolute" -> {
                    Value src = raw("a");
                    boolean integralValue = src.type() == TypeName.LONG;
                    Value a = integralValue ? integral("a") : numeric("a");
                    result(integralValue ? TypeName.LONG : TypeName.DOUBLE, CodeBlock.of("$T.abs($L)", java.lang.Math.class, a.code()));
                }
                case "sqrt" -> unaryDouble("$T.sqrt($L)", java.lang.Math.class);
                case "cbrt" -> unaryDouble("$T.cbrt($L)", java.lang.Math.class);
                case "mod" -> {
                    Value a = integral("a");
                    Value b = integral("b");
                    result(TypeName.LONG, CodeBlock.of("$T.mod($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                case "rand" -> {
                    Value a = numeric("a");
                    Value b = numeric("b");
                    result(TypeName.DOUBLE, CodeBlock.of("$T.rand($L, $L, $L)", PlanModel.BUILTIN_TOOLS, ops.ctxRef(), a.code(), b.code()));
                }
                default -> throw new IllegalStateException("算术工具语义缺失: " + tool);
            }
        }

        private void binary(String pattern) {
            binary(TypeName.DOUBLE, pattern);
        }

        private void binary(TypeName type, String pattern) {
            binary(type, pattern, null);
        }

        private void binary(TypeName type, String pattern, Class<?> math) {
            Value a = numeric("a");
            Value b = numeric("b");
            CodeBlock expr = math == null
                    ? CodeBlock.of(pattern, a.code(), b.code())
                    : CodeBlock.of(pattern, ClassName.get(math), a.code(), b.code());
            result(type, expr);
        }

        private void unaryDouble(String pattern, Class<?> math) {
            Value a = numeric("a");
            result(TypeName.DOUBLE, CodeBlock.of(pattern, ClassName.get(math), a.code()));
        }

        private void compare(String tool) {
            switch (tool) {
                case "gt" -> binary(TypeName.BOOLEAN, "$L > $L");
                case "ge" -> binary(TypeName.BOOLEAN, "$L >= $L");
                case "lt" -> binary(TypeName.BOOLEAN, "$L < $L");
                case "le" -> binary(TypeName.BOOLEAN, "$L <= $L");
                case "eq" -> {
                    Value a = any("a");
                    Value b = any("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$T.eq($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                case "ne" -> {
                    Value a = any("a");
                    Value b = any("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$T.ne($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                case "isSameObject" -> {
                    Value a = any("a");
                    Value b = any("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$L == $L", a.code(), b.code()));
                }
                case "isNotSameObject" -> {
                    Value a = any("a");
                    Value b = any("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$L != $L", a.code(), b.code()));
                }
                default -> throw new IllegalStateException("比较工具语义缺失: " + tool);
            }
        }

        private void logic(String tool) {
            switch (tool) {
                case "logAnd" -> {
                    Value a = bool("a");
                    Value b = bool("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$L && $L", a.code(), b.code()));
                }
                case "logOr" -> {
                    Value a = bool("a");
                    Value b = bool("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$L || $L", a.code(), b.code()));
                }
                case "logXor" -> {
                    Value a = bool("a");
                    Value b = bool("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$L ^ $L", a.code(), b.code()));
                }
                case "logNot" -> {
                    Value a = bool("a");
                    result(TypeName.BOOLEAN, CodeBlock.of("!$L", a.code()));
                }
                default -> throw new IllegalStateException("逻辑工具语义缺失: " + tool);
            }
        }

        private void assignment(String tool) {
            switch (tool) {
                case "assign" -> {
                    Value a = same("a");
                    if (PlanModel.KIND_REF.equals(a.kind()) && PlanModel.isEntity(a.type())) {
                        // 赋值：将 a 实例的值赋给 b，赋值后二者为不同实例
                        result(a.type(), CodeBlock.of("$L.copy($L)", a.code(), ops.ctxRef()));
                    } else {
                        result(a.type(), a.code());
                    }
                }
                case "assignRef" -> {
                    // 指向：b 与 a 为同一实例
                    Value a = same("a");
                    result(a.type(), a.code());
                }
                case "clear", "resetToOne", "clearRef" -> {
                    CompilePlanIR.OperandIR target = firstWriteTarget();
                    if (target == null) {
                        model.diagnostic(proc.zhName() + "：" + tool + " 没有写出目标");
                        return;
                    }
                    Value type = ops.targetType(target);
                    String literal = "resetToOne".equals(tool)
                            ? oneLiteral(type.type(), type.kind())
                            : model.defaultLiteral(type.type(), "clearRef".equals(tool) ? PlanModel.KIND_REF : type.kind());
                    effect(CodeBlock.of("$L;\n", ops.assign(target, new Value(CodeBlock.of(literal), type.type(), type.kind()))));
                }
                default -> throw new IllegalStateException("赋值工具语义缺失: " + tool);
            }
        }

        private String oneLiteral(TypeName type, String kind) {
            if (PlanModel.KIND_LIST.equals(kind) || PlanModel.KIND_MAP.equals(kind)) {
                return model.defaultLiteral(type, kind);
            }
            if (type == TypeName.DOUBLE) {
                return "1.0D";
            }
            if (type == TypeName.LONG) {
                return "1L";
            }
            if (type == TypeName.BOOLEAN) {
                return "true";
            }
            if (String.class.getName().equals(type.toString())) {
                return "\"1\"";
            }
            return "null";
        }

        private void automatic(String tool) {
            CompilePlanIR.OperandIR target = firstWriteTarget();
            if (target == null) {
                model.diagnostic(proc.zhName() + "：自增减工具 " + tool + " 没有回写目标");
                return;
            }
            Value src = raw("a");
            boolean integralValue = src.type() == TypeName.LONG;
            Value a = integralValue ? integral("a") : numeric("a");
            String unit = integralValue ? "1L" : "1.0D";
            switch (tool) {
                case "inc" -> writeAll(CodeBlock.of("$L + " + unit, a.code()), a.type(), a.kind());
                case "dec" -> writeAll(CodeBlock.of("$L - " + unit, a.code()), a.type(), a.kind());
                case "incByVal" -> {
                    Value b = integralValue ? integral("b") : numeric("b");
                    writeAll(CodeBlock.of("$L + $L", a.code(), b.code()), a.type(), a.kind());
                }
                case "decByVal" -> {
                    Value b = integralValue ? integral("b") : numeric("b");
                    writeAll(CodeBlock.of("$L - $L", a.code(), b.code()), a.type(), a.kind());
                }
                default -> throw new IllegalStateException("自增减工具语义缺失: " + tool);
            }
        }

        private void bitwise(String tool) {
            Value a = integral("a");
            String op = switch (tool) {
                case "bitAnd" -> "&";
                case "bitOr" -> "|";
                case "bitXor" -> "^";
                case "bitNot" -> "~";
                default -> throw new IllegalStateException("位运算工具语义缺失: " + tool);
            };
            if ("bitNot".equals(tool)) {
                result(TypeName.LONG, CodeBlock.of("$L$L", op, a.code()));
            } else {
                Value b = integral("b");
                result(TypeName.LONG, CodeBlock.of("$L $L $L", a.code(), op, b.code()));
            }
        }

        private void shift(String tool) {
            Value a = integral("a");
            Value b = integral("b");
            String op = switch (tool) {
                case "lShift" -> "<<";
                case "rShift" -> ">>";
                case "ruShift" -> ">>>";
                default -> throw new IllegalStateException("移位工具语义缺失: " + tool);
            };
            result(TypeName.LONG, CodeBlock.of("$L $L $L", a.code(), op, b.code()));
        }

        private void accuracy(String tool) {
            java.math.RoundingMode mode = switch (tool) {
                case "decimalHalfCarry", "halfUp" -> java.math.RoundingMode.HALF_UP;
                case "halfEven", "decimalHalfEvenAlign" -> java.math.RoundingMode.HALF_EVEN;
                case "decimalCutAll", "decimalFloorAlign", "roundDown", "floor" -> java.math.RoundingMode.FLOOR;
                case "decimalCeilAlign", "decimalCarryAll", "roundUp", "ceil" -> java.math.RoundingMode.CEILING;
                default -> throw new IllegalStateException("精度工具语义缺失: " + tool);
            };
            boolean withDigits = tool.startsWith("decimal");
            Value a = numeric("a");
            CodeBlock digits = withDigits ? integral("b").code() : CodeBlock.of("0L");
            result(TypeName.DOUBLE, CodeBlock.of("$T.round($L, $L, $T.$L)", PlanModel.BUILTIN_TOOLS, a.code(), digits,
                    java.math.RoundingMode.class, mode.name()));
        }

        private void trigonometric(String tool) {
            Value a = numeric("a");
            Class<?> math = java.lang.Math.class;
            CodeBlock expr = switch (tool) {
                case "sin" -> CodeBlock.of("$T.sin($L)", math, a.code());
                case "cos" -> CodeBlock.of("$T.cos($L)", math, a.code());
                case "tan" -> CodeBlock.of("$T.tan($L)", math, a.code());
                case "cot" -> CodeBlock.of("1.0D / $T.tan($L)", math, a.code());
                case "sec" -> CodeBlock.of("1.0D / $T.cos($L)", math, a.code());
                case "csc" -> CodeBlock.of("1.0D / $T.sin($L)", math, a.code());
                case "arcsin" -> CodeBlock.of("$T.asin($L)", math, a.code());
                case "arccos" -> CodeBlock.of("$T.acos($L)", math, a.code());
                case "arctan" -> CodeBlock.of("$T.atan($L)", math, a.code());
                case "arccot" -> CodeBlock.of("$T.PI / 2.0D - $T.atan($L)", math, math, a.code());
                case "arcsec" -> CodeBlock.of("$T.acos(1.0D / $L)", math, a.code());
                case "arccsc" -> CodeBlock.of("$T.asin(1.0D / $L)", math, a.code());
                case "toAngle" -> CodeBlock.of("$T.toDegrees($L)", math, a.code());
                case "toRadians" -> CodeBlock.of("$T.toRadians($L)", math, a.code());
                default -> throw new IllegalStateException("三角函数工具语义缺失: " + tool);
            };
            result(TypeName.DOUBLE, expr);
        }

        private void textTool(String tool) {
            ClassName string = ClassName.get(String.class);
            switch (tool) {
                case "becomeString" -> {
                    Value a = any("a");
                    result(string, CodeBlock.of("String.valueOf($L)", a.code()));
                }
                case "containText" -> {
                    Value a = text("a");
                    Value b = text("b");
                    result(TypeName.BOOLEAN, CodeBlock.of("$L.contains($L)", a.code(), b.code()));
                }
                case "extractText" -> {
                    Value a = text("a");
                    Value b = integral("b");
                    Value c = integral("c");
                    result(string, CodeBlock.of("$T.extractText($L, $L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code(), c.code()));
                }
                case "getTextLength" -> {
                    Value a = text("a");
                    result(TypeName.LONG, CodeBlock.of("(long) $L.length()", a.code()));
                }
                case "replaceText" -> {
                    Value a = text("a");
                    Value b = text("b");
                    Value c = text("c");
                    result(string, CodeBlock.of("$L.replace($L, $L)", a.code(), b.code(), c.code()));
                }
                case "splicingText" -> {
                    Value a = text("a");
                    Value b = text("b");
                    Value c = text("c");
                    result(string, CodeBlock.of("$L + $L + $L", a.code(), b.code(), c.code()));
                }
                case "textToBoolean" -> {
                    Value a = text("a");
                    result(TypeName.BOOLEAN, CodeBlock.of("$T.toBoolean($L)", PlanModel.VALUES, a.code()));
                }
                case "textToDouble" -> {
                    Value a = text("a");
                    result(TypeName.DOUBLE, CodeBlock.of("$T.toDouble($L)", PlanModel.VALUES, a.code()));
                }
                case "textToLong" -> {
                    Value a = text("a");
                    result(TypeName.LONG, CodeBlock.of("$T.toLong($L)", PlanModel.VALUES, a.code()));
                }
                case "textToDate", "textToDateTime", "textToTime" -> {
                    Value a = text("a");
                    Value b = text("b");
                    result(TypeName.LONG, CodeBlock.of("$T.parseTime($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                case "joinText" -> {
                    Value a = any("a");
                    Value b = text("b");
                    result(string, CodeBlock.of("$T.joinText($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                default -> throw new IllegalStateException("文本工具语义缺失: " + tool);
            }
        }

        private void timeTool(String tool) {
            switch (tool) {
                case "addBySeconds", "addByDays" -> {
                    Value a = integral("a");
                    Value b = numeric("b");
                    long unit = "addBySeconds".equals(tool) ? 1_000L : 86_400_000L;
                    result(TypeName.LONG, CodeBlock.of("$L + $T.roundToLong($L * $L)", a.code(), PlanModel.BUILTIN_TOOLS, b.code(), CodeBlock.of(unit + "D")));
                }
                case "reduceBySeconds", "reduceByDays" -> {
                    Value a = integral("a");
                    Value b = numeric("b");
                    long unit = "reduceBySeconds".equals(tool) ? 1_000L : 86_400_000L;
                    result(TypeName.LONG, CodeBlock.of("$L - $T.roundToLong($L * $L)", a.code(), PlanModel.BUILTIN_TOOLS, b.code(), CodeBlock.of(unit + "D")));
                }
                case "timeDifference" -> {
                    Value a = integral("a");
                    Value b = integral("b");
                    result(TypeName.DOUBLE, CodeBlock.of("($L - $L) / 1000.0D", a.code(), b.code()));
                }
                case "formatDate", "formatDateTime", "formatTime" -> {
                    Value a = integral("a");
                    Value b = text("b");
                    result(ClassName.get(String.class), CodeBlock.of("$T.formatTime($L, $L)", PlanModel.BUILTIN_TOOLS, a.code(), b.code()));
                }
                default -> {
                    double factor = switch (tool) {
                        case "hourToSeconds" -> 3600.0D;
                        case "minutesToSeconds" -> 60.0D;
                        case "daysToSeconds" -> 86400.0D;
                        case "secondsToMinutes" -> 1.0D / 60.0D;
                        case "secondsToHour" -> 1.0D / 3600.0D;
                        case "secondsToDays" -> 1.0D / 86400.0D;
                        default -> throw new IllegalStateException("时间工具语义缺失: " + tool);
                    };
                    Value a = numeric("a");
                    result(TypeName.DOUBLE, CodeBlock.of("$L * " + PlanModel.doubleLiteral(factor), a.code()));
                }
            }
        }

        private void arrayTool(String tool) {
            ClassName string = ClassName.get(String.class);
            switch (tool) {
                case "getByIndex" -> {
                    Value list = host();
                    Value idx = integral("标识");
                    resultFromObject(elementTypeOf(list), CodeBlock.of("$T.getByIndex($L, $L)", PlanModel.BUILTIN_TOOLS, list.code(), idx.code()));
                }
                case "contains" -> {
                    Value list = host();
                    Value e = any("元素");
                    result(TypeName.BOOLEAN, CodeBlock.of("$T.containsElement($L, $L)", PlanModel.BUILTIN_TOOLS, list.code(), e.code()));
                }
                case "addHead" -> {
                    Value list = host();
                    Value e = any("元素");
                    effect(CodeBlock.of("$T.addHead($L, $L);\n", PlanModel.BUILTIN_TOOLS, list.code(), e.code()));
                }
                case "addTail" -> {
                    Value list = host();
                    Value e = any("元素");
                    effect(CodeBlock.of("$T.addTail($L, $L);\n", PlanModel.BUILTIN_TOOLS, list.code(), e.code()));
                }
                case "addAt" -> {
                    Value list = host();
                    Value e = any("元素");
                    Value idx = integral("标识");
                    effect(CodeBlock.of("$T.addAt($L, $L, $L);\n", PlanModel.BUILTIN_TOOLS, list.code(), idx.code(), e.code()));
                }
                case "setByIndex" -> {
                    Value list = host();
                    Value e = any("元素");
                    Value idx = integral("标识");
                    effect(CodeBlock.of("$T.setByIndex($L, $L, $L);\n", PlanModel.BUILTIN_TOOLS, list.code(), idx.code(), e.code()));
                }
                case "removeAt" -> {
                    Value list = host();
                    Value idx = integral("标识");
                    resultFromObject(elementTypeOf(list), CodeBlock.of("$T.removeAt($L, $L)", PlanModel.BUILTIN_TOOLS, list.code(), idx.code()));
                }
                case "removeHead" -> {
                    Value list = host();
                    resultFromObject(elementTypeOf(list), CodeBlock.of("$T.removeHead($L)", PlanModel.BUILTIN_TOOLS, list.code()));
                }
                case "removeTail" -> {
                    Value list = host();
                    resultFromObject(elementTypeOf(list), CodeBlock.of("$T.removeTail($L)", PlanModel.BUILTIN_TOOLS, list.code()));
                }
                case "clear" -> {
                    Value list = host();
                    effect(CodeBlock.of("$T.clearCollection($L);\n", PlanModel.BUILTIN_TOOLS, list.code()));
                }
                case "copyElement" -> {
                    Value list = host();
                    Value src = any("来源组");
                    effect(CodeBlock.of("$T.copyElements($L, $L);\n", PlanModel.BUILTIN_TOOLS, list.code(), src.code()));
                }
                case "elementToText" -> {
                    Value list = host();
                    result(string, CodeBlock.of("$T.toJsonText($L)", PlanModel.BUILTIN_TOOLS, list.code()));
                }
                default -> throw new IllegalStateException("对象组工具语义缺失: " + tool);
            }
        }

        private void setTool(String tool) {
            switch (tool) {
                case "add" -> {
                    Value set = host();
                    Value e = any("元素");
                    effect(CodeBlock.of("$T.addToSet($L, $L);\n", PlanModel.BUILTIN_TOOLS, set.code(), e.code()));
                }
                case "remove" -> {
                    Value set = host();
                    Value e = any("元素");
                    effect(CodeBlock.of("$T.removeFromSet($L, $L);\n", PlanModel.BUILTIN_TOOLS, set.code(), e.code()));
                }
                case "contains" -> {
                    Value set = host();
                    Value e = any("元素");
                    result(TypeName.BOOLEAN, CodeBlock.of("$T.containsElement($L, $L)", PlanModel.BUILTIN_TOOLS, set.code(), e.code()));
                }
                case "clear" -> {
                    Value set = host();
                    effect(CodeBlock.of("$T.clearCollection($L);\n", PlanModel.BUILTIN_TOOLS, set.code()));
                }
                case "copyElement" -> {
                    Value set = host();
                    Value src = any("来源集");
                    effect(CodeBlock.of("$T.copyElements($L, $L);\n", PlanModel.BUILTIN_TOOLS, set.code(), src.code()));
                }
                case "toList" -> {
                    Value set = host();
                    TypeName listType = com.palantir.javapoet.ParameterizedTypeName.get(
                            ClassName.get(java.util.List.class), elementTypeOf(set));
                    result(listType, CodeBlock.of("new java.util.ArrayList<>($L)", set.code()));
                }
                default -> throw new IllegalStateException("对象集工具语义缺失: " + tool);
            }
        }

        private void mapTool(String tool) {
            ClassName string = ClassName.get(String.class);
            switch (tool) {
                case "getByKey" -> {
                    Value map = host();
                    Value k = any("键");
                    resultFromObject(elementTypeOf(map), CodeBlock.of("$T.getByKey($L, $L)", PlanModel.BUILTIN_TOOLS, map.code(), k.code()));
                }
                case "containsKey" -> {
                    Value map = host();
                    Value k = any("键");
                    result(TypeName.BOOLEAN, CodeBlock.of("$T.containsKey($L, $L)", PlanModel.BUILTIN_TOOLS, map.code(), k.code()));
                }
                case "putKeyValue" -> {
                    Value map = host();
                    Value k = any("键");
                    Value v = any("值");
                    effect(CodeBlock.of("$T.putValue($L, $L, $L);\n", PlanModel.BUILTIN_TOOLS, map.code(), k.code(), v.code()));
                }
                case "remove" -> {
                    Value map = host();
                    Value k = any("键");
                    effect(CodeBlock.of("$T.removeKey($L, $L);\n", PlanModel.BUILTIN_TOOLS, map.code(), k.code()));
                }
                case "clear" -> {
                    Value map = host();
                    effect(CodeBlock.of("$T.clearCollection($L);\n", PlanModel.BUILTIN_TOOLS, map.code()));
                }
                case "copyKeyValue" -> {
                    Value map = host();
                    Value src = any("来源映射");
                    effect(CodeBlock.of("$T.copyElements($L, $L);\n", PlanModel.BUILTIN_TOOLS, map.code(), src.code()));
                }
                case "getKeyArray" -> {
                    Value map = host();
                    result(PlanModel.OBJECT_TYPE, CodeBlock.of("$T.keyArrayOf($L)", PlanModel.BUILTIN_TOOLS, map.code()));
                }
                case "keyValueToText" -> {
                    Value map = host();
                    result(string, CodeBlock.of("$T.toJsonText($L)", PlanModel.BUILTIN_TOOLS, map.code()));
                }
                case "textToKeyValue" -> {
                    // 画布语义：组成线绑定 host 集合，解析文本并入其中（无值工具）
                    Value map = host();
                    Value text = any("文本");
                    effect(CodeBlock.of("$T.textToKeyValueInto($L, $L);\n",
                            PlanModel.BUILTIN_TOOLS, map.code(), text.code()));
                }
                default -> throw new IllegalStateException("映射集工具语义缺失: " + tool);
            }
        }

        /** 集合的元素类型（未知时为 Object） */
        private TypeName elementTypeOf(Value collection) {
            String element = ops.elementPlaceholder(collection);
            return element.isEmpty() ? PlanModel.OBJECT_TYPE : model.resolve(element).box();
        }
    }
}

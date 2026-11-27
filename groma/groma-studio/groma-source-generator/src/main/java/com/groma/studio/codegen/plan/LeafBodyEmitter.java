package com.groma.studio.codegen.plan;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.groma.studio.codegen.ProcessCodeAstTranslator;
import com.groma.studio.codegen.plan.PlanModel.LocalSlot;
import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.ParamSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 叶过程（画布过程代码框里的手写代码）的编译：
 * 中文成员 / 参数引用经 {@link ProcessCodeAstTranslator} 无损重写为英文 Getter / Setter 调用；
 * 混入的 Python 等异构脚本被剥离为受控资源文件；引用了类型上不存在的方法时，生成的过程以明确原因失败，
 * 保证整个模型模块始终可以编译。
 */
final class LeafBodyEmitter {

    private LeafBodyEmitter() {
    }

    static final class Outcome {
        final CodeBlock.Builder body = CodeBlock.builder();
        final List<String[]> scripts = new ArrayList<>();
    }

    static Outcome emit(PlanModel model, ProcInfo proc, ProcessCodeAstTranslator translator) {
        String raw = proc.def().javaCode();
        Outcome outcome = new Outcome();
        if (raw == null || raw.isBlank()) {
            return outcome;
        }
        // 中文参数 / 成员名 -> 英文访问表达式
        Map<String, ProcessCodeAstTranslator.VarBinding> bindings = new HashMap<>();
        for (ParamSlot param : proc.params()) {
            bindings.put(param.zhName(), new ProcessCodeAstTranslator.VarBinding(
                    param.zhName(), "this." + param.field(), "this." + param.setter()));
        }
        for (LocalSlot local : proc.locals()) {
            // 读表达式带调用括号：裸中文成员/局部量读取改写后必须是方法调用（模型 2 实证链式读取缺括号）
            bindings.put(local.zhName(), new ProcessCodeAstTranslator.VarBinding(
                    local.zhName(), "frame.get" + local.cap() + "()", "frame.set" + local.cap()));
        }
        if (proc.owner() != null) {
            for (MemberInfo member : proc.owner().allMembers()) {
                bindings.put(member.zhName(), new ProcessCodeAstTranslator.VarBinding(
                        member.zhName(), "self." + member.getter() + "()", "self." + member.setterCtx()));
            }
        }
        ProcessCodeAstTranslator.TranslationResult translated;
        try {
            translated = translator.translate(proc.def().entityId(), proc.methodName(), raw, bindings);
        } catch (RuntimeException ex) {
            model.diagnostic("过程 " + proc.zhName() + " 的手写代码无法解析，已降级为运行期失败分支: " + ex.getMessage());
            return failed(outcome, "手写代码无法解析: " + oneLine(ex.getMessage()));
        }
        if (translated.extractedPythonScriptName() != null) {
            outcome.scripts.add(new String[] {translated.extractedPythonScriptName(), translated.extractedPythonContent()});
            outcome.body.addStatement("// 画布过程代码框内的异构脚本已剥离为受控资源文件 scripts/$L", translated.extractedPythonScriptName());
            // Python 双向数据桥（修复"只调空壳、丢弃入参与返回值"断层）：
            // IN/IN_OUT 标量形参打包为 inputs → invokeExternalScriptWithIo 受控执行 → 按中文形参名回写 OUT/IN_OUT
            outcome.body.addStatement("$T scriptInput = new $T<>()",
                    java.util.Map.class, java.util.LinkedHashMap.class);
            for (ParamSlot param : proc.params()) {
                if (param.reads() && !param.isRef()) {
                    outcome.body.addStatement("scriptInput.put($S, this.$L)", param.zhName(), param.field());
                }
            }
            outcome.body.addStatement("$T scriptOutput = world().invokeExternalScriptWithIo($S, $S, scriptInput)",
                    java.util.Map.class, translated.extractedPythonScriptName(), proc.def().entityId());
            int writeSeq = 0;
            for (ParamSlot param : proc.params()) {
                if (!param.writes() || param.isRef()) {
                    continue;
                }
                String var = "scriptValue" + (writeSeq++);
                outcome.body.addStatement("$T $L = scriptOutput.get($S)", Object.class, var, param.zhName());
                if (param.type() == TypeName.BOOLEAN) {
                    outcome.body.addStatement("this.$L($T.TRUE.equals($L))", param.setter(), Boolean.class, var);
                } else if (PlanModel.STRING_TYPE.equals(param.type())) {
                    outcome.body.beginControlFlow("if ($L != null)", var);
                    outcome.body.addStatement("this.$L($T.valueOf($L))", param.setter(), String.class, var);
                    outcome.body.endControlFlow();
                } else {
                    // 数值形参：脚本 JSON 输出统一转 double 回写
                    outcome.body.beginControlFlow("if ($L instanceof $T num$L)", var, Number.class, var);
                    outcome.body.addStatement("this.$L(num$L.doubleValue())", param.setter(), var);
                    outcome.body.endControlFlow();
                }
            }
            return outcome;
        }
        String code = translated.javaMethodBody().toString();
        String unsupported;
        try {
            unsupported = unsupportedMemberAccess(model, proc, code);
        } catch (RuntimeException ex) {
            // 画布代码框内容转译后仍不是可解析的 Java（如全角标点/残余 Python 语句，模型 2 实证）：
            // 按既有语义降级为运行期失败分支并保留原始代码注释，不让整条流水线崩掉
            unsupported = "静态校验无法解析（" + String.valueOf(ex.getMessage()).lines().findFirst().orElse("") + "）";
        }
        if (unsupported != null) {
            model.diagnostic("过程 " + proc.zhName() + " 的手写代码" + unsupported + "，已降级为运行期失败分支");
            outcome.body.addStatement("// 原始手写代码: $S", oneLine(raw));
            outcome.body.addStatement("fail($S)", "手写代码" + unsupported);
            return outcome;
        }
        outcome.body.add("// 以下代码由画布过程代码框转译（中文引用已无损重写为英文访问）\n");
        outcome.body.add(code.endsWith("\n") ? code.replace("$", "$$") : code.replace("$", "$$") + "\n");
        return outcome;
    }

    private static Outcome failed(Outcome outcome, String reason) {
        outcome.body.addStatement("fail($S)", reason);
        return outcome;
    }

    private static String oneLine(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", " ").replace("\r", " ");
    }

    // ------------------------------------------------------------------ 静态校验

    /**
     * 检查转译后的代码是否调用了接收者类型上不存在的方法；返回问题描述，全部可解析时返回 null
     */
    private static String unsupportedMemberAccess(PlanModel model, ProcInfo proc, String code) {
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        ParseResult<BlockStmt> parsed = parser.parseBlock("{\n" + code + "\n}");
        if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
            return "不是合法的 Java 语句";
        }
        BlockStmt block = parsed.getResult().get();
        List<MethodCallExpr> calls = new ArrayList<>(block.findAll(MethodCallExpr.class));
        for (int i = calls.size() - 1; i >= 0; i--) {
            MethodCallExpr call = calls.get(i);
            if (call.getScope().isEmpty()) {
                continue;
            }
            TypeName receiver = receiverType(model, proc, call.getScope().get());
            if (receiver == null) {
                continue;
            }
            if (!hasMethod(model, receiver, call.getNameAsString(), call.getArguments().size())) {
                return "引用了类型 " + receiver + " 上不存在的方法 " + call.getNameAsString() + "()";
            }
        }
        return null;
    }

    /** 推导接收者表达式的静态类型（仅识别转译器会生成的几种形态） */
    private static TypeName receiverType(PlanModel model, ProcInfo proc,
                                         com.github.javaparser.ast.expr.Expression scope) {
        if (scope instanceof ThisExpr) {
            return proc.taskCn();
        }
        if (scope instanceof NameExpr name) {
            if ("self".equals(name.getNameAsString()) && proc.owner() != null) {
                return proc.owner().entity();
            }
            return null;
        }
        if (scope instanceof com.github.javaparser.ast.expr.FieldAccessExpr field) {
            if (field.getScope() instanceof ThisExpr) {
                for (ParamSlot param : proc.params()) {
                    if (param.field().equals(field.getNameAsString())) {
                        return param.type();
                    }
                }
            }
            if (field.getScope() instanceof NameExpr name && "frame".equals(name.getNameAsString())) {
                for (LocalSlot local : proc.locals()) {
                    if (local.field().equals(field.getNameAsString())) {
                        return local.type();
                    }
                }
            }
            return null;
        }
        if (scope instanceof MethodCallExpr call) {
            // 形如 frame.getX() / self.getX() / this.p.getX() / <表达式>.getX()
            TypeName parent = receiverType(model, proc, call.getScope().get());
            if (parent == null || call.getArguments().size() != 0) {
                return null;
            }
            TypeInfo info = model.typeOf(parent);
            if (info == null || !call.getNameAsString().startsWith("get")) {
                return null;
            }
            for (MemberInfo member : info.allMembers()) {
                if (member.getter().equals(call.getNameAsString())) {
                    return member.type();
                }
            }
        }
        return null;
    }

    private static boolean hasMethod(PlanModel model, TypeName receiver, String method, int argCount) {
        if (receiver instanceof ParameterizedTypeName) {
            String text = receiver.toString();
            Class<?> erased = text.startsWith("java.util.Map") ? Map.class
                    : text.startsWith("java.util.Set") ? java.util.Set.class : List.class;
            return hasMethod(model, ClassName.get(erased), method, argCount);
        }
        if (!(receiver instanceof ClassName cn)) {
            return true;
        }
        TypeInfo info = model.typeOf(receiver);
        if (info != null) {
            for (MemberInfo member : info.allMembers()) {
                if (member.getter().equals(method) && argCount == 0) {
                    return true;
                }
                if (member.setterCtx().equals(method) && (argCount == 1 || argCount == 2)) {
                    return true;
                }
                if (member.setterPlain() != null && member.setterPlain().equals(method) && argCount == 1) {
                    return true;
                }
            }
            // 基类实例方法之外，实体/只读视图类自身没有其他成员方法可调（块 24 收割口径）
            return hasMethod(model, PlanModel.BASE_ENTITY, method, argCount);
        }
        if (model.enumOf(receiver) != null) {
            return argCount == 0 && ("getZhLabel".equals(method) || "name".equals(method) || "ordinal".equals(method));
        }
        // 生成模型自身的类（运行帧 BO / 只读视图 / 服务 / 过程任务等）不在生成器类路径上，
        // Class.forName 必然失败——按生成包前缀放行，杜绝“误杀合法手写代码”整段降级（块 24 收割）
        if (cn.canonicalName().startsWith(model.basePkg)) {
            return true;
        }
        try {
            Class<?> clazz = Class.forName(cn.canonicalName(), false,
                    LeafBodyEmitter.class.getClassLoader());
            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == argCount) {
                    return true;
                }
            }
            // 基本类型的包装类上对应着原生运算，方法名匹配不到也按不支持处理
            return false;
        } catch (ClassNotFoundException ex) {
            return true;
        }
    }
}

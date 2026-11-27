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
            bindings.put(local.zhName(), new ProcessCodeAstTranslator.VarBinding(
                    local.zhName(), "frame.get" + local.cap(), "frame.set" + local.cap()));
        }
        if (proc.owner() != null) {
            for (MemberInfo member : proc.owner().allMembers()) {
                bindings.put(member.zhName(), new ProcessCodeAstTranslator.VarBinding(
                        member.zhName(), "self." + member.getter(), "self." + member.setterCtx()));
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
            outcome.body.addStatement("world().invokeExternalScript($S, $S)", translated.extractedPythonScriptName(),
                    proc.def().entityId());
            return outcome;
        }
        String code = translated.javaMethodBody().toString();
        String unsupported = unsupportedMemberAccess(model, proc, code);
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
            return hasMethod(model, PlanModel.BASE_ENTITY, method, argCount);
        }
        if (model.enumOf(receiver) != null) {
            return argCount == 0 && ("getZhLabel".equals(method) || "name".equals(method) || "ordinal".equals(method));
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

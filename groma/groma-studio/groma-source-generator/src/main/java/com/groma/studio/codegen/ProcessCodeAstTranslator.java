package com.groma.studio.codegen;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.palantir.javapoet.CodeBlock;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 过程手写代码 AST 语义等价转译器与异构脚本（Python）安全降级器（落实 BE-CMP-005 & BE-EXT-004）
 */
public final class ProcessCodeAstTranslator {

    private static final Pattern PYTHON_FINGERPRINT = Pattern.compile(
            "(?m)^\\s*(def\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\(|from\\s+[A-Za-z0-9_.]+\\s+import\\s+|#\\s*-\\*-\\s*coding)"
    );

    public record TranslationResult(
            CodeBlock javaMethodBody,
            String extractedPythonScriptName,
            String extractedPythonContent,
            String diagnosticWarning
    ) {}

    private final JavaParser javaParser;

    public ProcessCodeAstTranslator() {
        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.javaParser = new JavaParser(cfg);
    }

    /**
     * @param processEntityId   过程实体 ID
     * @param processCodeSlug   过程英文标识（用于脚本落盘命名）
     * @param rawCode           模型 JSON 中的原始 javaCode 字符串
     * @param zhToEnVarBindings 中文参数/成员名 -> 规范英文 Getter/Setter 目标映射表
     */
    public TranslationResult translate(
            String processEntityId,
            String processCodeSlug,
            String rawCode,
            Map<String, VarBinding> zhToEnVarBindings) {

        if (rawCode == null || rawCode.isBlank()) {
            return new TranslationResult(CodeBlock.builder().build(), null, null, null);
        }

        // 1. 嗅探是否为非 Java 的 Python 建模脚本（如 scene-a.json 中的航路规划算法）。
        //    方法体交由 LeafBodyEmitter 生成双向数据桥（IN/IN_OUT 打包 → invokeExternalScriptWithIo → OUT 回写），
        //    这里只负责剥离脚本内容与产出告警，不再生成调用语句（历史版本曾生成不存在的
        //    ctx.invokeSandboxedPythonScript 调用，属死代码已移除）。
        if (PYTHON_FINGERPRINT.matcher(rawCode).find()) {
            String scriptFileName = processCodeSlug + "_" + processEntityId.substring(0, Math.min(8, processEntityId.length())) + ".py";
            return new TranslationResult(
                    CodeBlock.builder().build(),
                    scriptFileName,
                    rawCode,
                    "WARN [VAL-CODE-002] 过程 [" + processEntityId + "] 包含 Python 脚本，已转为受控外部脚本连接器调用"
            );
        }

        // 2. 尝试作为 Java 语句块解析并执行中文引用 AST 重写
        String wrappedBlock = "{\n" + rawCode + "\n}";
        ParseResult<BlockStmt> parsed = javaParser.parseBlock(wrappedBlock);
        if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
            int errLine = parsed.getProblems().isEmpty()
                    ? 1
                    : parsed.getProblems().get(0).getLocation()
                        .flatMap(l -> l.getBegin().getRange().map(r -> r.begin.line - 1))
                        .orElse(1);
            throw new IllegalArgumentException(
                    "[CMP-ERR-005] 过程 [" + processEntityId + "] 代码语法解析失败（原始行号: " + Math.max(1, errLine) + "）: "
                            + parsed.getProblems());
        }

        BlockStmt block = parsed.getResult().get();

        // 2.0 存量集合序列化习惯 `X.itemsToNJson()` → BuiltinTools.itemsToNJson(X)：
        //     必须先于绑定改写单独一遍处理——外层 setValue 整节点替换时内层调用会随原 AST 一起被换掉而漏改
        block.findAll(MethodCallExpr.class).forEach(call -> {
            if (!"itemsToNJson".equals(call.getNameAsString()) || !call.getArguments().isEmpty()) {
                return;
            }
            String scopeCode = call.getScope().map(java.util.Objects::toString).orElse("");
            if (call.getScope().orElse(null) instanceof NameExpr scopeName) {
                VarBinding scopeBinding =
                        zhToEnVarBindings.get(stripLeadingUnderscore(scopeName.getNameAsString()));
                if (scopeBinding != null) {
                    scopeCode = scopeBinding.getterExpr();
                }
            }
            if (!scopeCode.isBlank()) {
                call.replace(StaticExprFactory.parseExpr(
                        "com.groma.base.engine.BuiltinTools.itemsToNJson(" + scopeCode + ")"));
            }
        });

        // 2.1 改写存量平台习惯的 `中文对象.getValue()` 与 `中文对象.setValue(val)` / `setKeyInstOrValue(val)`
        block.findAll(MethodCallExpr.class).forEach(call -> {
            call.getScope().ifPresent(scope -> {
                if (scope instanceof NameExpr nameExpr) {
                    String rawName = stripLeadingUnderscore(nameExpr.getNameAsString());
                    VarBinding binding = zhToEnVarBindings.get(rawName);
                    if (binding != null) {
                        String mName = call.getNameAsString();
                        if ("getValue".equals(mName) && call.getArguments().isEmpty()) {
                            call.replace(StaticExprFactory.parseExpr(binding.getterExpr()));
                        } else if ("setKeyInstOrValue".equals(mName) && call.getArguments().size() == 1) {
                            // 存量映射集“键实例或值”开关：切换即清空键值集合（与存量 MapClazz.setKeyInstOrValue 一致）
                            call.replace(StaticExprFactory.parseExpr(
                                    "com.groma.base.engine.BuiltinTools.clearCollection(" + binding.getterExpr() + ")"));
                        } else if ("setValue".equals(mName) && call.getArguments().size() == 1) {
                            String argCode = call.getArgument(0).toString();
                            call.replace(StaticExprFactory.parseExpr(binding.setterPrefix() + "(" + argCode + ")"));
                        }
                    }
                }
            });
        });

        // 2.2 改写直接中文变量赋值 `_剩余电量 = _剩余电量 - 5.0` 及纯读取引用
        block.findAll(AssignExpr.class).forEach(assign -> {
            if (assign.getTarget() instanceof NameExpr targetName) {
                String rawName = stripLeadingUnderscore(targetName.getNameAsString());
                VarBinding binding = zhToEnVarBindings.get(rawName);
                if (binding != null && assign.getOperator() == AssignExpr.Operator.ASSIGN) {
                    // 先改写右侧表达式中的中文变量
                    rewriteNameExprsInSubtree(assign.getValue(), zhToEnVarBindings);
                    String rhs = assign.getValue().toString();
                    assign.replace(StaticExprFactory.parseExpr(binding.setterPrefix() + "(" + rhs + ")"));
                }
            }
        });

        rewriteNameExprsInSubtree(block, zhToEnVarBindings);

        // 输出转译后的干净语句（附带原始行语义保留注释）
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.add("// --- 编译转译自画布过程代码 (原始行数: $L) ---\n", rawCode.split("\n").length);
        block.getStatements().forEach(stmt -> cb.add("$L\n", stmt.toString()));
        return new TranslationResult(cb.build(), null, null, null);
    }

    private void rewriteNameExprsInSubtree(com.github.javaparser.ast.Node root, Map<String, VarBinding> bindings) {
        root.findAll(NameExpr.class).forEach(ne -> {
            String key = stripLeadingUnderscore(ne.getNameAsString());
            VarBinding b = bindings.get(key);
            if (b != null) {
                ne.replace(StaticExprFactory.parseExpr(b.getterExpr()));
            }
        });
    }

    private static String stripLeadingUnderscore(String s) {
        return (s.startsWith("_") && s.length() > 1) ? s.substring(1) : s;
    }

    public record VarBinding(String zhName, String getterExpr, String setterPrefix) {}

    private static final class StaticExprFactory {
        static com.github.javaparser.ast.expr.Expression parseExpr(String code) {
            return com.github.javaparser.StaticJavaParser.parseExpression(code);
        }
    }
}

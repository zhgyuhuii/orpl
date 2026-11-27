package com.groma.base.connector;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 受控算术/比较表达式求值沙盒（落实 BE-SEC-002：严格拒绝 Java 互操作指令，空结果安全处理）
 */
public final class ExpressionSandboxEvaluator {

    private static final Pattern FORBIDDEN_INTEROP = Pattern.compile(
            "\\b(Java\\.type|Packages|importClass|importPackage|load|loadWithNewGlobal|Runtime|ProcessBuilder|System|Class\\.forName)\\b"
    );

    private final boolean enabled;

    public ExpressionSandboxEvaluator(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean evaluateBoolean(String expression, Map<String, Object> contextVars) {
        if (!enabled) {
            throw new SecurityException("[SEC-ERR-002] 表达式执行能力已被管理员配置关闭");
        }
        if (expression == null || expression.isBlank()) {
            return false; // CK-BE-SEC-002-2：空表达式或空结果安全返回 false，不抛空指针
        }
        if (FORBIDDEN_INTEROP.matcher(expression).find()) {
            throw new SecurityException("[SEC-ERR-002] 检测到禁止的 Java 互操作调用，拒绝执行表达式");
        }
        String trimmed = expression.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        // 支持简单比较 var >= literal
        for (String op : new String[]{">=", "<=", "==", "!=", ">", "<"}) {
            int idx = trimmed.indexOf(op);
            if (idx > 0) {
                String leftKey = trimmed.substring(0, idx).trim();
                String rightLit = trimmed.substring(idx + op.length()).trim();
                Object actual = contextVars == null ? null : contextVars.get(leftKey);
                if (actual == null) {
                    return false;
                }
                if (actual instanceof Number num) {
                    double lv = num.doubleValue();
                    double rv = Double.parseDouble(rightLit);
                    return switch (op) {
                        case ">=" -> lv >= rv;
                        case "<=" -> lv <= rv;
                        case ">" -> lv > rv;
                        case "<" -> lv < rv;
                        case "==" -> Double.compare(lv, rv) == 0;
                        case "!=" -> Double.compare(lv, rv) != 0;
                        default -> false;
                    };
                }
            }
        }
        return false;
    }
}

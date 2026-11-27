package com.groma.studio.naming;

import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 阿里巴巴 Java 开发手册标识符格式化与作用域冲突守卫（规避 67 个关键字、Object 方法及 POJO 布尔 is 前缀陷阱）
 */
public final class AlibabaScopeSymbolGuard {

    private static final Set<String> OBJECT_METHOD_NAMES = Set.of(
            "getclass", "hashcode", "equals", "clone", "tostring", "notify", "notifyall", "wait", "finalize"
    );

    private AlibabaScopeSymbolGuard() {
    }

    public static String formatUpperCamelClass(List<String> words, String trailingNum, String requiredSuffix) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        if (trailingNum != null && !trailingNum.isEmpty()) {
            sb.append(trailingNum);
        }
        if (sb.isEmpty() || Character.isDigit(sb.charAt(0))) {
            sb.insert(0, "Groma");
        }
        String suffix = requiredSuffix == null ? "" : requiredSuffix;
        if (!suffix.isEmpty() && !sb.toString().endsWith(suffix)) {
            sb.append(suffix);
        }
        return guardKeyword(sb.toString(), "Model");
    }

    public static String formatLowerCamelMember(List<String> rawWords, String trailingNum, boolean isPojoBooleanField) {
        List<String> words = new ArrayList<>(rawWords);
        // 阿里巴巴 Java 手册强制规定：POJO 布尔类型变量严禁加 is 前缀！
        if (isPojoBooleanField && words.size() > 1 && "is".equalsIgnoreCase(words.get(0))) {
            words.remove(0);
        }
        if (words.isEmpty()) {
            words.add("value");
        }
        StringBuilder sb = new StringBuilder(words.get(0).toLowerCase(Locale.ROOT));
        for (int i = 1; i < words.size(); i++) {
            String w = words.get(i);
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        if (trailingNum != null && !trailingNum.isEmpty()) {
            sb.append(trailingNum);
        }
        if (Character.isDigit(sb.charAt(0))) {
            sb.insert(0, "field");
        }
        return guardKeyword(sb.toString(), "Val");
    }

    public static String formatUpperSnakeConstant(List<String> words, String trailingNum) {
        String joined = String.join("_", words).toUpperCase(Locale.ROOT);
        if (trailingNum != null && !trailingNum.isEmpty()) {
            joined = joined + "_" + trailingNum;
        }
        if (joined.isEmpty() || Character.isDigit(joined.charAt(0))) {
            joined = "CONST_" + joined;
        }
        return guardKeyword(joined, "_CONST");
    }

    private static String guardKeyword(String candidate, String safeSuffix) {
        if (!SourceVersion.isIdentifier(candidate)
                || SourceVersion.isKeyword(candidate)
                || OBJECT_METHOD_NAMES.contains(candidate.toLowerCase(Locale.ROOT))) {
            return candidate + safeSuffix;
        }
        return candidate;
    }
}

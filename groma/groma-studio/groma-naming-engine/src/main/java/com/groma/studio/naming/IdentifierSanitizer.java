package com.groma.studio.naming;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 符号清洗与物理量纲剥离预处理器（提取括号内单位存入 @GromaMeta，将前置数字移至末尾防语法非法）
 */
public final class IdentifierSanitizer {

    private static final Pattern PAREN_UNIT_RX = Pattern.compile("[（(]([^）)]{1,16})[）)]\\s*$");
    private static final Pattern LEADING_NUM_RX = Pattern.compile("^(\\d+)\\s*号?\\s*(.+)$");
    private static final Pattern AUTO_HEX_CONST_RX = Pattern.compile("^(定值|dz)[0-9a-fA-F]{16,}$");

    private IdentifierSanitizer() {
    }

    public static SanitizedLabel sanitize(String rawLabel, String aliasIfAny) {
        String candidate = (rawLabel == null || rawLabel.isBlank()) ? "unnamed" : rawLabel.trim();
        // 针对存量脏数据如 label="定值185eafb0..." 且 alias="目标下行有效"，优先采用人类可读的 alias！
        if (AUTO_HEX_CONST_RX.matcher(candidate).matches() && aliasIfAny != null && !aliasIfAny.isBlank()) {
            candidate = aliasIfAny.trim();
        }

        String unit = "";
        Matcher um = PAREN_UNIT_RX.matcher(candidate);
        if (um.find()) {
            String inside = um.group(1).trim();
            if (looksLikeUnit(inside)) {
                unit = inside;
                candidate = candidate.substring(0, um.start()).trim();
            }
        }

        String trailingNum = "";
        Matcher nm = LEADING_NUM_RX.matcher(candidate);
        if (nm.matches()) {
            trailingNum = nm.group(1);
            candidate = nm.group(2).trim();
        }

        String cleaned = candidate.replaceAll("[\\s·—\\-/\\\\:：,，。！？\"'`~@#$%^&*+=<>?|\\[\\]{}()（）]+", " ").trim();
        return new SanitizedLabel(cleaned.isEmpty() ? "item" : cleaned, unit, trailingNum);
    }

    private static boolean looksLikeUnit(String text) {
        return text.matches("^[%a-zA-Z0-9/°℃㎡³²]+$")
                || text.equals("秒") || text.equals("分") || text.equals("小时") || text.equals("米");
    }
}

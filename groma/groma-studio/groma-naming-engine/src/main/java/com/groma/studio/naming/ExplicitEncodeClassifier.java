package com.groma.studio.naming;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 零硬编码显式英文编码鉴别器（收割自 code5.html 3.5，闭环 12.1 E5b / 12.4 挂账的安全接线件）：
 * 精准区分建模人员手写的规范英文词元（{@code floodWall}、{@code uav_patrol}、{@code CruiseSpeed}）
 * 与旧平台自动生成的拼音首字母缩写码（{@code dxlx1}、{@code gccy1}、{@code znsczzjhznt1}、{@code wb12}）。
 * <p>
 * 12.2 H4 的教训：语料 encode 100% 是平台自动码且无来源标记，直接放行 Tier 1 必然产出垃圾类名——
 * 本分类器以"词形启发式"作为唯一安全闸门：自动码全量回落 Tier 2~4 分词管线，
 * 手写英文全量放行；对现有双模型语料须保持 100% 回落（回归裁判 = 三语料出码逐字节不变）。
 * 判定为纯函数、零状态、可单测。
 * </p>
 */
public final class ExplicitEncodeClassifier {

    /** 合法 ASCII 标识形态：字母开头，长度 2~64（排除单字母与空串） */
    private static final Pattern VALID_ASCII_IDENT = Pattern.compile("^[A-Za-z][A-Za-z0-9_\\-]{1,63}$");

    /** 平台自动编号典型形态：纯小写短字母串（≤4）+ 数字后缀，如 wb1、dxlx1、lgc12 */
    private static final Pattern TRAILING_DIGITS_SHORT = Pattern.compile("^[a-z]{1,4}\\d+$");

    private static final Set<Character> VOWELS = Set.of('a', 'e', 'i', 'o', 'u');

    /** ≤3 字母的合法英文技术缩写白名单（无元音但属常用标识，不判为拼音码） */
    private static final Set<String> TECH_ABBREVIATIONS = Set.of(
            "id", "src", "dst", "mgr", "ctx", "msg", "sql", "mcp", "ws", "dto", "vo", "bo", "ui", "os", "io");

    private ExplicitEncodeClassifier() {
    }

    /**
     * 返回 true 时命名引擎 Tier 1 直接采纳该 explicitEncode；
     * 返回 false 时判定为平台自动缩写码或非法码，回落 Tier 2~4 分词与词典/拼音管线。
     */
    public static boolean isGenuineUserEnglishEncode(String explicitEncode) {
        if (explicitEncode == null || !VALID_ASCII_IDENT.matcher(explicitEncode).matches()) {
            return false;
        }
        // 1. 排除 wb1 / dxlx1 / lgc12 形态的纯小写短字母+数字自动编号
        if (TRAILING_DIGITS_SHORT.matcher(explicitEncode).matches()) {
            return false;
        }
        // 2. 按驼峰边界与下划线/连字符切词，逐词检查拼音码特征
        String[] tokens = explicitEncode
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT)
                .split("[_\\-]+");
        for (String token : tokens) {
            String lettersOnly = token.replaceAll("\\d+$", "");
            if (lettersOnly.isEmpty()) {
                continue;
            }
            if (lettersOnly.length() <= 3 && TECH_ABBREVIATIONS.contains(lettersOnly)) {
                continue;
            }
            // 拼音首字母串（znsczzjhznt / gldx）的典型特征：连续辅音 ≥5 或零元音
            if (maxConsecutiveConsonants(lettersOnly) >= 5 || countVowels(lettersOnly) == 0) {
                return false;
            }
        }
        return true;
    }

    private static int maxConsecutiveConsonants(String letters) {
        int max = 0;
        int current = 0;
        for (int i = 0; i < letters.length(); i++) {
            char c = letters.charAt(i);
            if (Character.isLetter(c) && !VOWELS.contains(c)) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    private static int countVowels(String letters) {
        int count = 0;
        for (int i = 0; i < letters.length(); i++) {
            if (VOWELS.contains(letters.charAt(i))) {
                count++;
            }
        }
        return count;
    }
}

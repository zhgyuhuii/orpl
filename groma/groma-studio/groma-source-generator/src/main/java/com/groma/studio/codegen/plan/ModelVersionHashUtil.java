package com.groma.studio.codegen.plan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 模型语料内容确定性短哈希（多版本并存收口）：同一宿主内，同源不同版本的模型
 * （如 v647 与 v825）派生不同 modelCode，热挂载槽互不顶替；同内容重建幂等复用同一编码。
 * <p>
 * 编码形态 = {@code {基础码}-{8位小写十六进制}}（SHA-256(A || ':' || B) 前 8 位）。
 * 基础码已自带后缀时不再重复追加（幂等）。模块目录名与 Java 包名随之版本化，
 * Maven artifactId 则保持 {@link #artifactIdOf(String)} 的稳定基础名（同源仅最新版本入仓合包）。
 */
public final class ModelVersionHashUtil {

    /** 版本后缀形态：{@code -} + 恰好 8 位小写十六进制结尾 */
    private static final Pattern HASH_SUFFIX = Pattern.compile("^(.+)-[0-9a-f]{8}$");

    private ModelVersionHashUtil() {
    }

    /**
     * 双包语料内容确定性短哈希：SHA-256(partA || ':' || partB) 前 8 位小写十六进制。
     * 任一分包为 null 时按空数组参与摘要（与解析器的空包语义一致）。
     */
    public static String shortCorpusHash(byte[] partA, byte[] partB) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(partA != null ? partA : new byte[0]);
            md.update((byte) ':');
            md.update(partB != null ? partB : new byte[0]);
            return HexFormat.of().formatHex(md.digest()).substring(0, 8);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 摘要算法不可用", ex);
        }
    }

    /**
     * 组装版本化模型编码：{@code {基础码}-{短哈希}}；基础码已带同形后缀或短哈希为空时原样返回（幂等）。
     */
    public static String composeVersionedModelCode(String baseSlug, String shortHash) {
        String normalized = (baseSlug == null || baseSlug.isBlank()) ? "groma-model" : baseSlug.trim();
        if (shortHash == null || shortHash.isBlank()) {
            return normalized;
        }
        String suffix = "-" + shortHash.toLowerCase(java.util.Locale.ROOT);
        return normalized.endsWith(suffix) ? normalized : normalized + suffix;
    }

    /**
     * 稳定 Maven artifactId：剥离版本短哈希后缀（{@code model-emergency-deduction-a1b2c3d4}
     * → {@code model-emergency-deduction}）。同源多版本在仓内只保留最新一份，目录名仍带哈希互不覆盖。
     */
    public static String artifactIdOf(String versionedModelCode) {
        String code = versionedModelCode == null ? "" : versionedModelCode.trim();
        var matcher = HASH_SUFFIX.matcher(code);
        return matcher.matches() ? matcher.group(1) : code;
    }
}

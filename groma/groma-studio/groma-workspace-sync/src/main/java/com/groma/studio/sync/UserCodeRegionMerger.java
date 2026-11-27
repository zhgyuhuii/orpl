package com.groma.studio.sync;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @GromaUserCode 手写保护区无损提取、合并与保护区外手工修改检测器（落实 NP-HOT-003）
 * 全平台唯一的保护区合并器：支持越界篡改检测、孤儿手写区识别、结构化告警与 Java 21 AST 语法校验。
 */
public final class UserCodeRegionMerger {

    private static final Pattern REGION_PATTERN = Pattern.compile(
            "(?s)(//\\s*@GromaUserCode-START\\s*\\[([^\\]]+)\\]\\R)(.*?)(^\\s*//\\s*@GromaUserCode-END\\s*\\[\\2\\])",
            Pattern.MULTILINE
    );

    public record MergeResult(
            String mergedContent,
            String skeletonSha256,
            boolean outsideRegionModified,
            List<String> preservedRegionIds,
            List<String> orphanedRegionIds,
            List<String> warnings
    ) {}

    private final JavaParser validatorParser;

    public UserCodeRegionMerger() {
        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.validatorParser = new JavaParser(cfg);
    }

    /**
     * 将磁盘已有 Java 文件中的手写保护区代码合并入新生成的源码，并校验保护区外是否被越界篡改
     *
     * @param relativePath             文件相对路径（用于告警定位与 .java 语法校验判定）
     * @param newlyGeneratedSource     本次由生成器产出的全新源码
     * @param existingDiskSource       磁盘上当前存在的文件内容（首次生成为 null）
     * @param previousSkeletonSha256   上一次同步记录的“剥离保护区后骨架哈希”（首次或未知为 null）
     */
    public MergeResult mergeJavaFile(
            String relativePath,
            String newlyGeneratedSource,
            String existingDiskSource,
            String previousSkeletonSha256) {

        String normalizedNew = normalizeLineEndings(newlyGeneratedSource);
        String newSkeletonHash = sha256Hex(stripRegionBodies(normalizedNew));
        List<String> warnings = new ArrayList<>();

        if (existingDiskSource == null || existingDiskSource.isEmpty()) {
            return new MergeResult(normalizedNew, newSkeletonHash, false, List.of(), List.of(), List.of());
        }

        String normalizedDisk = normalizeLineEndings(existingDiskSource);

        // 1. 检测磁盘文件是否在 @GromaUserCode 保护区之外被人工改动过（CK-NP-HOT-003-3）
        boolean outsideModified = false;
        if (previousSkeletonSha256 != null && !previousSkeletonSha256.isBlank()) {
            String currentDiskSkeletonHash = sha256Hex(stripRegionBodies(normalizedDisk));
            outsideModified = !previousSkeletonSha256.equals(currentDiskSkeletonHash);
            if (outsideModified) {
                warnings.add("[SYNC-WARN-OUTSIDE-EDIT] 文件 " + relativePath
                        + " 检测到 @GromaUserCode 保护区之外的自动生成代码被手工修改，重新生成已按最新模型覆盖骨架，请将自定义逻辑放入保护区内！");
            }
        }

        // 2. 提取磁盘文件中的所有保护区代码体
        Map<String, String> diskRegions = extractRegions(normalizedDisk);
        if (diskRegions.isEmpty()) {
            return new MergeResult(normalizedNew, newSkeletonHash, outsideModified, List.of(), List.of(), List.copyOf(warnings));
        }

        // 3. 将保护区代码无损回填到新生成的源码对应槽位
        List<String> preservedIds = new ArrayList<>();
        Matcher matcher = REGION_PATTERN.matcher(normalizedNew);
        StringBuilder mergedBuf = new StringBuilder(normalizedNew.length() + 512);

        while (matcher.find()) {
            String startTag = matcher.group(1);
            String regionId = matcher.group(2).trim();
            String defaultBody = matcher.group(3);
            String endTag = matcher.group(4);

            String replacementBody = defaultBody;
            if (diskRegions.containsKey(regionId)) {
                String userBody = diskRegions.remove(regionId);
                if (!userBody.equals(defaultBody)) {
                    preservedIds.add(regionId);
                }
                replacementBody = userBody;
            }
            String assembled = startTag + replacementBody + endTag;
            matcher.appendReplacement(mergedBuf, Matcher.quoteReplacement(assembled));
        }
        matcher.appendTail(mergedBuf);

        List<String> orphanedIds = List.copyOf(diskRegions.keySet());
        if (!orphanedIds.isEmpty()) {
            warnings.add("[SYNC-WARN-ORPHAN-REGION] 文件 " + relativePath
                    + " 原模型节点已删除，以下手写区未在新源码中找到对应锚点: " + orphanedIds);
        }

        String finalSource = mergedBuf.toString();

        // 4. 若回填了手写代码，执行 Java 21 AST 语法自检，确保手写区未破坏外层类括号结构
        if (!preservedIds.isEmpty() && relativePath != null && relativePath.endsWith(".java")) {
            ParseResult<CompilationUnit> res = validatorParser.parse(finalSource);
            if (!res.isSuccessful()) {
                throw new IllegalArgumentException(
                        "[SYNC-ERR-001] 文件 " + relativePath + " 的 @GromaUserCode 手写代码区存在语法错误，阻止覆盖写盘: "
                                + res.getProblems());
            }
        }

        return new MergeResult(finalSource, newSkeletonHash, outsideModified, List.copyOf(preservedIds), orphanedIds, List.copyOf(warnings));
    }

    public Map<String, String> extractRegions(String source) {
        Map<String, String> map = new LinkedHashMap<>();
        if (source == null || source.isBlank()) {
            return map;
        }
        Matcher m = REGION_PATTERN.matcher(source);
        while (m.find()) {
            map.put(m.group(2).trim(), m.group(3));
        }
        return map;
    }

    /**
     * 剥离所有保护区内部实现，仅保留自动生成的骨架代码以计算基线哈希
     */
    public String stripRegionBodies(String source) {
        String normalized = normalizeLineEndings(source);
        Matcher m = REGION_PATTERN.matcher(normalized);
        StringBuilder sb = new StringBuilder(normalized.length());
        while (m.find()) {
            String emptyAnchor = m.group(1) + m.group(4);
            m.appendReplacement(sb, Matcher.quoteReplacement(emptyAnchor));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 计算剥离保护区后的纯骨架 SHA-256（供磁盘落盘器在无实例状态下直接调用）
     */
    public static String computeSkeletonSha256(String javaSource) {
        return sha256Hex(stripRegionBodiesStatic(javaSource));
    }

    private static String stripRegionBodiesStatic(String source) {
        String normalized = source == null ? "" : source.replace("\r\n", "\n");
        Matcher m = REGION_PATTERN.matcher(normalized);
        StringBuilder sb = new StringBuilder(normalized.length());
        while (m.find()) {
            String emptyAnchor = m.group(1) + m.group(4);
            m.appendReplacement(sb, Matcher.quoteReplacement(emptyAnchor));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeLineEndings(String text) {
        return text == null ? "" : text.replace("\r\n", "\n");
    }

    static String sha256Hex(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 摘要计算异常", ex);
        }
    }
}

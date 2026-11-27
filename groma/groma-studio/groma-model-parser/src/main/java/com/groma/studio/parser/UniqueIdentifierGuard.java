package com.groma.studio.parser;

import javax.lang.model.SourceVersion;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 显式自定义编码合法性校验与确定性去重器（落实 BE-MDL-003，规避 BL-19）
 */
public final class UniqueIdentifierGuard {

    private static final Pattern VALID_ENCODE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final Set<String> allocated = new LinkedHashSet<>();
    private int autoSeq = 0;

    /**
     * 校验用户手工填写的严格编码；非法时直接拒绝并给出修改建议（CK-BE-MDL-003-1）
     */
    public static void validateExplicitEncodeOrThrow(String encode) {
        String problem = findExplicitEncodeProblem(encode);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
    }

    /**
     * 非抛出版编码校验（供 StaticModelValidator 收集 VAL-007 问题用）：
     * 返回问题描述，合法时返回 null。
     */
    public static String findExplicitEncodeProblem(String encode) {
        if (encode == null || !VALID_ENCODE.matcher(encode).matches()) {
            return "[MDL-CODE-001] 非法标识符编码 '" + encode + "'：仅允许字母/数字/下划线且不能以数字或中文开头，建议改为合法驼峰英文";
        }
        if (SourceVersion.isKeyword(encode) || !SourceVersion.isName(encode)) {
            return "[MDL-CODE-002] 编码 '" + encode + "' 与 Java 21 关键字冲突，建议追加业务后缀如 '" + encode + "Item'";
        }
        return null;
    }

    /**
     * 导入存量模型时对空编码或重复编码（如大量重名的 wb1）分配稳定且唯一的合法编码（CK-BE-MDL-003-2 & 3）
     */
    public String allocateDeterministicUnique(String candidateOrEmpty, String fallbackPrefix) {
        String base = (candidateOrEmpty == null || candidateOrEmpty.isBlank() || !VALID_ENCODE.matcher(candidateOrEmpty).matches())
                ? fallbackPrefix + "_" + (++autoSeq)
                : candidateOrEmpty;
        if (SourceVersion.isKeyword(base)) {
            base = base + "_val";
        }
        String unique = base;
        int dupIdx = 2;
        while (!allocated.add(unique)) {
            unique = base + "_" + dupIdx++;
        }
        return unique;
    }
}

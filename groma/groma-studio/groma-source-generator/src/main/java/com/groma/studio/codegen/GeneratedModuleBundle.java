package com.groma.studio.codegen;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 单个模型子模块完整生成产物（使用 TreeMap 按相对路径严格字典序排列，确保多次生成逐字节哈希一致）
 */
public final class GeneratedModuleBundle {

    private final String moduleDirName; // 例如 "model-emergency-deduction"
    private final String basePackage;   // 例如 "com.groma.models.emergencydeduction"
    private final TreeMap<String, String> files = new TreeMap<>();
    private final AstSourceMap sourceMap;

    public GeneratedModuleBundle(String moduleDirName, String basePackage, AstSourceMap sourceMap) {
        this.moduleDirName = moduleDirName;
        this.basePackage = basePackage;
        this.sourceMap = sourceMap;
    }

    public void putFile(String relativePathUnderModule, String content) {
        String normalizedPath = relativePathUnderModule.replace('\\', '/');
        String normalizedLfContent = content.replace("\r\n", "\n");
        files.put(normalizedPath, normalizedLfContent);
    }

    public String getModuleDirName() {
        return moduleDirName;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public Map<String, String> getFiles() {
        return Collections.unmodifiableMap(files);
    }

    public AstSourceMap getSourceMap() {
        return sourceMap;
    }

    /**
     * 计算整包所有文件路径与内容的联合 SHA-256 摘要（用于 CK-NP-GEN-001-1 确定性断言）
     */
    public String computeBundleSha256() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, String> entry : files.entrySet()) {
                md.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 计算失败", ex);
        }
    }
}

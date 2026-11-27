package com.groma.studio.sync;

import com.groma.studio.codegen.GeneratedModuleBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模块工作区增量同步器（落实 NP-HOT-003 & CK-NP-GEN-003-2）
 * 1. 仅写盘 SHA-256 发生变化的文件（避免破坏磁盘 mtime 与触发 IDE 全量索引）；
 * 2. 完整保留 @GromaUserCode 保护区手写代码，并重新索引合并后的 AstSourceMap 行号；
 * 3. 检测并警告保护区外的意外修改。
 */
public final class WorkspaceSyncService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSyncService.class);
    private static final String BASELINE_META_FILE = ".groma-sync-baseline.json";

    private final UserCodeRegionMerger regionMerger = new UserCodeRegionMerger();
    private final ParentPomModuleRegistrar pomRegistrar = new ParentPomModuleRegistrar();
    private final ObjectMapper jsonMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    /**
     * 将内存生成的模块源码包增量同步至 <workspaceRoot>/groma-models/<model-code>/
     */
    public SyncReport syncModuleToWorkspace(Path workspaceRoot, GeneratedModuleBundle bundle) throws IOException {
        Path modelsRoot = workspaceRoot.resolve("groma-models");
        Path moduleDir = modelsRoot.resolve(bundle.getModuleDirName());
        Files.createDirectories(moduleDir);

        Map<String, String> previousSkeletonHashes = loadBaselineSkeletonHashes(moduleDir);
        Map<String, String> updatedSkeletonHashes = new LinkedHashMap<>();

        List<String> written = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> preservedRegions = new ArrayList<>();
        List<String> overwriteWarnings = new ArrayList<>();

        for (Map.Entry<String, String> entry : bundle.getFiles().entrySet()) {
            String relPath = entry.getKey();
            String generatedContent = entry.getValue();
            Path targetFile = moduleDir.resolve(relPath).normalize();
            if (!targetFile.startsWith(moduleDir.normalize())) {
                throw new SecurityException("非法的文件相对路径越界: " + relPath);
            }

            String existingDiskContent = Files.exists(targetFile)
                    ? Files.readString(targetFile, StandardCharsets.UTF_8)
                    : null;

            String finalToPersist = generatedContent;
            if (relPath.endsWith(".java")) {
                UserCodeRegionMerger.MergeResult mergeRes = regionMerger.mergeJavaFile(
                        relPath,
                        generatedContent,
                        existingDiskContent,
                        previousSkeletonHashes.get(relPath)
                );
                finalToPersist = mergeRes.mergedContent();
                updatedSkeletonHashes.put(relPath, mergeRes.skeletonSha256());
                preservedRegions.addAll(mergeRes.preservedRegionIds());

                if (mergeRes.outsideRegionModified()) {
                    String warn = "WARN [SYNC-OUTSIDE-OVERWRITE] 文件 " + relPath
                            + " 在 @GromaUserCode 保护区之外存在手工改动，已被模型重生成覆盖";
                    overwriteWarnings.add(warn);
                    log.warn(warn);
                }
                // 关键：若合并入了多行手写保护区代码，同步刷新双向 AstSourceMap 行号（满足 CK-NP-GEN-003-2）
                bundle.getSourceMap().indexJavaSource(relPath, finalToPersist);
            }

            if (existingDiskContent != null) {
                String diskHash = UserCodeRegionMerger.sha256Hex(existingDiskContent.replace("\r\n", "\n"));
                String targetHash = UserCodeRegionMerger.sha256Hex(finalToPersist);
                if (diskHash.equals(targetHash)) {
                    skipped.add(relPath);
                    continue;
                }
            }

            ParentPomModuleRegistrar.atomicWriteString(targetFile, finalToPersist);
            written.add(relPath);
        }

        // 保存最新骨架基线并自动在 groma-models/pom.xml 中登记子模块
        saveBaselineSkeletonHashes(moduleDir, updatedSkeletonHashes);
        pomRegistrar.ensureModuleRegistered(modelsRoot, bundle.getModuleDirName());

        log.info("[WorkspaceSync] 模块 {} 同步完成：写入 {} 个文件，跳过未变更 {} 个，保留手写块 {} 处，越界覆盖告警 {} 处",
                bundle.getModuleDirName(), written.size(), skipped.size(), preservedRegions.size(), overwriteWarnings.size());

        return new SyncReport(
                bundle.getModuleDirName(),
                List.copyOf(written),
                List.copyOf(skipped),
                List.copyOf(preservedRegions),
                List.copyOf(overwriteWarnings)
        );
    }

    private Map<String, String> loadBaselineSkeletonHashes(Path moduleDir) {
        Path meta = moduleDir.resolve(BASELINE_META_FILE);
        if (!Files.exists(meta)) {
            return Map.of();
        }
        try {
            byte[] raw = Files.readAllBytes(meta);
            return jsonMapper.readValue(raw, new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            log.warn("读取同步基线元数据失败，按首次同步处理: {}", ex.getMessage());
            return Map.of();
        }
    }

    private void saveBaselineSkeletonHashes(Path moduleDir, Map<String, String> hashes) throws IOException {
        Path meta = moduleDir.resolve(BASELINE_META_FILE);
        String json = jsonMapper.writeValueAsString(hashes);
        ParentPomModuleRegistrar.atomicWriteString(meta, json);
    }
}

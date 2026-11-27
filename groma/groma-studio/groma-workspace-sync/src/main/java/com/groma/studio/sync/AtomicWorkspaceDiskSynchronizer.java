package com.groma.studio.sync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作区增量原子落盘与 groma-models/pom.xml <modules> 自动维护器
 */
public final class AtomicWorkspaceDiskSynchronizer {

    private static final Pattern MODULES_BLOCK = Pattern.compile("(?s)(<modules>)(.*?)(</modules>)");

    public record DiskSyncReport(
            int writtenFileCount,
            int skippedIdenticalCount,
            List<String> warnings
    ) {}

    private final Path workspaceRoot;
    private final UserCodeRegionMerger regionMerger = new UserCodeRegionMerger();
    private final ConcurrentHashMap<String, String> recordedSkeletonHashes = new ConcurrentHashMap<>();

    public AtomicWorkspaceDiskSynchronizer(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    public DiskSyncReport syncModelModuleToDisk(String moduleArtifactId, Map<String, String> generatedRelFiles)
            throws IOException {
        Path modelsDir = workspaceRoot.resolve("groma-models").normalize();
        Path moduleDir = modelsDir.resolve(moduleArtifactId).normalize();
        if (!moduleDir.startsWith(modelsDir)) {
            throw new SecurityException("非法模型子模块目录名: " + moduleArtifactId);
        }
        Files.createDirectories(moduleDir);

        int written = 0;
        int skipped = 0;
        List<String> allWarnings = new ArrayList<>();

        for (Map.Entry<String, String> entry : generatedRelFiles.entrySet()) {
            String relPath = entry.getKey().replace('\\', '/');
            Path targetFile = moduleDir.resolve(relPath).normalize();
            if (!targetFile.startsWith(moduleDir)) {
                throw new SecurityException("非法子模块内文件路径越界: " + relPath);
            }
            Files.createDirectories(targetFile.getParent());

            String finalContent = entry.getValue();
            String hashKey = moduleArtifactId + ":" + relPath;

            if (relPath.endsWith(".java") && Files.exists(targetFile)) {
                String diskOld = Files.readString(targetFile, StandardCharsets.UTF_8);
                UserCodeRegionMerger.MergeResult mergeRes = regionMerger.mergeJavaFile(
                        relPath, finalContent, diskOld, recordedSkeletonHashes.get(hashKey));
                finalContent = mergeRes.mergedContent();
                recordedSkeletonHashes.put(hashKey, mergeRes.skeletonSha256());
                allWarnings.addAll(mergeRes.warnings());
            } else if (relPath.endsWith(".java")) {
                recordedSkeletonHashes.put(hashKey, UserCodeRegionMerger.computeSkeletonSha256(finalContent));
            }

            if (Files.exists(targetFile)) {
                String existingSha = InMemoryWorkspaceVfs.sha256Hex(Files.readString(targetFile, StandardCharsets.UTF_8));
                String incomingSha = InMemoryWorkspaceVfs.sha256Hex(finalContent);
                if (existingSha.equals(incomingSha)) {
                    skipped++;
                    continue;
                }
            }

            atomicWriteString(targetFile, finalContent);
            written++;
        }

        ensureModuleRegisteredInParentPom(modelsDir.resolve("pom.xml"), moduleArtifactId);
        return new DiskSyncReport(written, skipped, List.copyOf(allWarnings));
    }

    private void ensureModuleRegisteredInParentPom(Path modelsPomPath, String moduleArtifactId) throws IOException {
        if (!Files.exists(modelsPomPath)) {
            return;
        }
        String pomXml = Files.readString(modelsPomPath, StandardCharsets.UTF_8);
        String moduleTag = "<module>" + moduleArtifactId + "</module>";
        if (pomXml.contains(moduleTag)) {
            return;
        }
        Matcher matcher = MODULES_BLOCK.matcher(pomXml);
        if (matcher.find()) {
            String inner = matcher.group(2);
            String updatedInner = inner + "        " + moduleTag + "\n    ";
            String updatedPom = matcher.replaceFirst(
                    Matcher.quoteReplacement(matcher.group(1) + updatedInner + matcher.group(3)));
            atomicWriteString(modelsPomPath, updatedPom);
        }
    }

    private static void atomicWriteString(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp." + System.nanoTime());
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}

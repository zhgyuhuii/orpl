package com.groma.studio.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 亚 10ms 多模块内存虚拟文件系统（支持前端实时浏览五层源码树与差量变更计算）
 */
public final class InMemoryWorkspaceVfs {

    public record VfsFileNode(
            String moduleArtifactId,
            String relativePath,
            String content,
            String sha256,
            long updatedNano
    ) {}

    public record VfsDiffSummary(
            String moduleArtifactId,
            List<String> addedPaths,
            List<String> modifiedPaths,
            List<String> deletedPaths,
            long elapsedMicros
    ) {}

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, VfsFileNode>> moduleTrees = new ConcurrentHashMap<>();

    /**
     * 刷新单个模型子模块的内存文件树并返回精确到文件级的增删改差量清单
     */
    public VfsDiffSummary refreshModuleFiles(String moduleArtifactId, Map<String, String> generatedFiles) {
        long startNano = System.nanoTime();
        ConcurrentHashMap<String, VfsFileNode> currentMap =
                moduleTrees.computeIfAbsent(moduleArtifactId, k -> new ConcurrentHashMap<>());

        List<String> added = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        Set<String> incomingPaths = generatedFiles.keySet();
        for (String existingPath : List.copyOf(currentMap.keySet())) {
            if (!incomingPaths.contains(existingPath)) {
                currentMap.remove(existingPath);
                deleted.add(existingPath);
            }
        }

        long nowNano = System.nanoTime();
        for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
            String relPath = normalizeRelPath(entry.getKey());
            String content = entry.getValue();
            String newSha = sha256Hex(content);
            VfsFileNode oldNode = currentMap.get(relPath);
            if (oldNode == null) {
                currentMap.put(relPath, new VfsFileNode(moduleArtifactId, relPath, content, newSha, nowNano));
                added.add(relPath);
            } else if (!oldNode.sha256().equals(newSha)) {
                currentMap.put(relPath, new VfsFileNode(moduleArtifactId, relPath, content, newSha, nowNano));
                modified.add(relPath);
            }
        }

        added.sort(Comparator.naturalOrder());
        modified.sort(Comparator.naturalOrder());
        deleted.sort(Comparator.naturalOrder());
        long micros = (System.nanoTime() - startNano) / 1_000L;
        return new VfsDiffSummary(moduleArtifactId, List.copyOf(added), List.copyOf(modified), List.copyOf(deleted), micros);
    }

    public List<String> listModuleFilePaths(String moduleArtifactId) {
        ConcurrentHashMap<String, VfsFileNode> map = moduleTrees.get(moduleArtifactId);
        if (map == null) {
            return List.of();
        }
        return map.keySet().stream().sorted().toList();
    }

    public Optional<VfsFileNode> readFile(String moduleArtifactId, String relativePath) {
        ConcurrentHashMap<String, VfsFileNode> map = moduleTrees.get(moduleArtifactId);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(normalizeRelPath(relativePath)));
    }

    private static String normalizeRelPath(String path) {
        return path.replace('\\', '/').replaceAll("^/+", "");
    }

    public static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

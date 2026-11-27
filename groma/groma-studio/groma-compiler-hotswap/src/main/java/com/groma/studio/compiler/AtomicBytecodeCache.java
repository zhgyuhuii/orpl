package com.groma.studio.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * 原子字节码缓存管理器（落实 BE-CMP-004：三元缓存键 + 临时目录原子切换 + 完整性摘要校验）
 */
public final class AtomicBytecodeCache {

    private static final Logger log = LoggerFactory.getLogger(AtomicBytecodeCache.class);
    private static final String MANIFEST_NAME = "cache-manifest.json";

    private final Path cacheRootDir;
    private final String compilerVersion;
    private final String engineBuildVersion;
    private final ConcurrentHashMap<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public AtomicBytecodeCache(Path cacheRootDir, String compilerVersion, String engineBuildVersion) {
        this.cacheRootDir = cacheRootDir;
        this.compilerVersion = compilerVersion;
        this.engineBuildVersion = engineBuildVersion;
    }

    /**
     * 缓存键三要素：模型源码摘要 + 编译器版本 + 引擎构建版本（CK-BE-CMP-004-2 可见）
     */
    public String computeCacheKey(String moduleDirName, String sourceBundleSha256) {
        String raw = moduleDirName + "|" + sourceBundleSha256 + "|" + compilerVersion + "|" + engineBuildVersion;
        return moduleDirName + "_" + sha256Bytes(raw.getBytes(StandardCharsets.UTF_8)).substring(0, 24);
    }

    public ReentrantLock lockForKey(String cacheKey) {
        return keyLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
    }

    public Optional<Map<String, byte[]>> tryLoadVerified(String cacheKey) {
        Path entryDir = cacheRootDir.resolve(cacheKey);
        Path manifestFile = entryDir.resolve(MANIFEST_NAME);
        if (!Files.isDirectory(entryDir) || !Files.exists(manifestFile)) {
            return Optional.empty();
        }
        try {
            Map<String, String> expectedDigests = mapper.readValue(
                    Files.readAllBytes(manifestFile),
                    new TypeReference<Map<String, String>>() {}
            );
            Map<String, byte[]> loaded = new TreeMap<>();
            for (Map.Entry<String, String> item : expectedDigests.entrySet()) {
                String fqcn = item.getKey();
                Path classFile = entryDir.resolve("classes").resolve(fqcn.replace('.', '/') + ".class");
                if (!Files.exists(classFile)) {
                    log.warn("[BytecodeCache] 缓存项 {} 缺失类文件 {}，判定失效重新编译", cacheKey, fqcn);
                    return Optional.empty();
                }
                byte[] bytes = Files.readAllBytes(classFile);
                if (!item.getValue().equals(sha256Bytes(bytes))) {
                    log.warn("[BytecodeCache] 缓存项 {} 类文件 {} 摘要不一致，判定失效重新编译", cacheKey, fqcn);
                    return Optional.empty();
                }
                loaded.put(fqcn, bytes);
            }
            return Optional.of(Map.copyOf(loaded));
        } catch (Exception ex) {
            log.warn("[BytecodeCache] 读取缓存项 {} 异常，回退重新编译: {}", cacheKey, ex.getMessage());
            return Optional.empty();
        }
    }

    public void putAtomically(String cacheKey, Map<String, byte[]> classBytecodes) throws IOException {
        Files.createDirectories(cacheRootDir);
        Path stagingDir = cacheRootDir.resolve(cacheKey + ".staging." + System.nanoTime());
        Path classesDir = stagingDir.resolve("classes");
        Files.createDirectories(classesDir);

        Map<String, String> digests = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : new TreeMap<>(classBytecodes).entrySet()) {
            String fqcn = entry.getKey();
            byte[] bytes = entry.getValue();
            Path targetClass = classesDir.resolve(fqcn.replace('.', '/') + ".class");
            Files.createDirectories(targetClass.getParent());
            Files.write(targetClass, bytes);
            digests.put(fqcn, sha256Bytes(bytes));
        }

        Files.writeString(stagingDir.resolve(MANIFEST_NAME), mapper.writeValueAsString(digests), StandardCharsets.UTF_8);

        Path finalDir = cacheRootDir.resolve(cacheKey);
        if (Files.exists(finalDir)) {
            deleteRecursively(finalDir);
        }
        Files.move(stagingDir, finalDir, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public void evictByModulePrefix(String moduleDirName) throws IOException {
        if (!Files.isDirectory(cacheRootDir)) {
            return;
        }
        try (Stream<Path> children = Files.list(cacheRootDir)) {
            for (Path p : children.toList()) {
                if (p.getFileName().toString().startsWith(moduleDirName + "_")) {
                    deleteRecursively(p);
                }
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static String sha256Bytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 计算失败", ex);
        }
    }
}

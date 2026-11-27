package com.groma.studio.importer;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 SHA-256 内容寻址的不可变模型语料存储库（落实 NP-IMP-002 & NP-IMP-004）
 */
public final class ImmutableModelStore {

    private static final String MANIFEST_FILE = "corpus-manifest.json";
    private static final String BLOBS_DIR = "blobs";

    public record StoreOutcome(
            boolean newlyStored,
            CorpusManifestEntry entry
    ) {}

    private final Path storeRoot;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public ImmutableModelStore(Path storeRoot) {
        this.storeRoot = storeRoot;
    }

    public StoreOutcome putIfChanged(
            String modelId,
            String modelName,
            int declaredVersion,
            byte[] partABytes,
            byte[] partBBytes,
            String sourceOrigin) throws IOException {

        String shaA = sha256Hex(partABytes);
        String shaB = sha256Hex(partBBytes);
        String combined = shaA + ":" + shaB;

        writeLock.lock();
        try {
            Files.createDirectories(storeRoot.resolve(BLOBS_DIR));
            List<CorpusManifestEntry> entries = new ArrayList<>(loadAllEntriesInternal());

            // 1. 若同一个 modelId 已有完全一致的联合内容摘要，直接跳过并返回已有记录（CK-NP-IMP-004-1）
            Optional<CorpusManifestEntry> identical = entries.stream()
                    .filter(e -> e.modelId().equals(modelId) && e.combinedDigest().equals(combined))
                    .findFirst();
            if (identical.isPresent()) {
                return new StoreOutcome(false, identical.get());
            }

            // 2. 若内容变化但上游版本号未递增，自动分配不冲突的递增版本号，保留旧版本并存
            int effectiveVersion = declaredVersion;
            int maxExistingVer = entries.stream()
                    .filter(e -> e.modelId().equals(modelId))
                    .mapToInt(CorpusManifestEntry::version)
                    .max()
                    .orElse(0);
            if (effectiveVersion <= maxExistingVer) {
                effectiveVersion = maxExistingVer + 1;
            }

            String relA = BLOBS_DIR + "/" + shaA + "-a.json";
            String relB = BLOBS_DIR + "/" + shaB + "-b.json";
            writeImmutableBlobIfAbsent(storeRoot.resolve(relA), partABytes);
            writeImmutableBlobIfAbsent(storeRoot.resolve(relB), partBBytes);

            CorpusManifestEntry created = new CorpusManifestEntry(
                    modelId,
                    modelName,
                    effectiveVersion,
                    shaA,
                    shaB,
                    combined,
                    partABytes.length,
                    partBBytes.length,
                    relA,
                    relB,
                    sourceOrigin,
                    Instant.now().toString()
            );
            entries.add(created);
            entries.sort(Comparator.comparing(CorpusManifestEntry::modelId)
                    .thenComparingInt(CorpusManifestEntry::version));

            atomicWriteManifest(entries);
            return new StoreOutcome(true, created);
        } finally {
            writeLock.unlock();
        }
    }

    public List<CorpusManifestEntry> listAllEntries() throws IOException {
        writeLock.lock();
        try {
            return List.copyOf(loadAllEntriesInternal());
        } finally {
            writeLock.unlock();
        }
    }

    public Optional<CorpusManifestEntry> findByModelAndVersion(String modelId, int version) throws IOException {
        return listAllEntries().stream()
                .filter(e -> e.modelId().equals(modelId) && e.version() == version)
                .findFirst();
    }

    public Optional<CorpusManifestEntry> findLatestByModel(String modelId) throws IOException {
        return listAllEntries().stream()
                .filter(e -> e.modelId().equals(modelId))
                .max(Comparator.comparingInt(CorpusManifestEntry::version));
    }

    public Path resolvePartAPath(CorpusManifestEntry entry) {
        return storeRoot.resolve(entry.blobRelPathA());
    }

    public Path resolvePartBPath(CorpusManifestEntry entry) {
        return storeRoot.resolve(entry.blobRelPathB());
    }

    private void writeImmutableBlobIfAbsent(Path blobPath, byte[] content) throws IOException {
        if (Files.exists(blobPath)) {
            return;
        }
        Path tmp = blobPath.resolveSibling(blobPath.getFileName().toString() + ".tmp." + System.nanoTime());
        Files.write(tmp, content);
        Files.move(tmp, blobPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        blobPath.toFile().setReadOnly();
    }

    private List<CorpusManifestEntry> loadAllEntriesInternal() throws IOException {
        Path mf = storeRoot.resolve(MANIFEST_FILE);
        if (!Files.exists(mf)) {
            return List.of();
        }
        return mapper.readValue(Files.readAllBytes(mf), new TypeReference<List<CorpusManifestEntry>>() {});
    }

    private void atomicWriteManifest(List<CorpusManifestEntry> entries) throws IOException {
        Path mf = storeRoot.resolve(MANIFEST_FILE);
        Path tmp = mf.resolveSibling(MANIFEST_FILE + ".tmp." + System.nanoTime());
        Files.writeString(tmp, mapper.writeValueAsString(entries), StandardCharsets.UTF_8);
        Files.move(tmp, mf, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 计算失败", ex);
        }
    }
}

package com.groma.studio.importer;

/**
 * 语料库单版本不可变登记项（落实 NP-IMP-002 清单格式要求）
 */
public record CorpusManifestEntry(
        String modelId,
        String modelName,
        int version,
        String digestPartA,
        String digestPartB,
        String combinedDigest,
        long sizeBytesA,
        long sizeBytesB,
        String blobRelPathA,
        String blobRelPathB,
        String sourceOrigin,
        String importedAtIso
) {}

package com.groma.studio.importer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 严格对应 NP-IMP-002 检查项（CK-NP-IMP-002-1 & 2）
 */
class ModelStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void sameContentSameDigestNoDuplicate() throws Exception {
        ImmutableModelStore store = new ImmutableModelStore(tempDir);
        byte[] a = "{\"obj\":{\"id\":\"m_demo\",\"label\":\"样例\",\"version\":1,\"entities\":[]}}".getBytes(StandardCharsets.UTF_8);
        byte[] b = "{\"obj\":{\"cellMap\":{}}}".getBytes(StandardCharsets.UTF_8);

        ImmutableModelStore.StoreOutcome first = store.putIfChanged("m_demo", "样例", 1, a, b, "unit-test");
        ImmutableModelStore.StoreOutcome second = store.putIfChanged("m_demo", "样例", 1, a, b, "unit-test");

        Assertions.assertTrue(first.newlyStored());
        Assertions.assertFalse(second.newlyStored());
        Assertions.assertEquals(first.entry().combinedDigest(), second.entry().combinedDigest());
        Assertions.assertEquals(1, store.listAllEntries().size());

        try (Stream<Path> blobs = Files.list(tempDir.resolve("blobs"))) {
            Assertions.assertEquals(2L, blobs.count(), "相同内容的 A/B 文件在 blobs 池中只应各存一份");
        }
    }

    @Test
    void artifactsReferenceSourceDigest() throws Exception {
        ImmutableModelStore store = new ImmutableModelStore(tempDir);
        byte[] a = "{\"obj\":{\"id\":\"m_trace\",\"label\":\"溯源测试\",\"version\":5,\"entities\":[]}}".getBytes(StandardCharsets.UTF_8);
        byte[] b = "{\"obj\":{\"cellMap\":{\"v1\":[]}}}".getBytes(StandardCharsets.UTF_8);

        CorpusManifestEntry saved = store.putIfChanged("m_trace", "溯源测试", 5, a, b, "local").entry();
        Assertions.assertEquals(
                ImmutableModelStore.sha256Hex(a) + ":" + ImmutableModelStore.sha256Hex(b),
                saved.combinedDigest()
        );
        Assertions.assertTrue(Files.exists(store.resolvePartAPath(saved)));
        Assertions.assertTrue(Files.exists(store.resolvePartBPath(saved)));
    }
}

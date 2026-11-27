package com.groma.studio.importer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 严格对应 NP-IMP-004 检查项（CK-NP-IMP-004-1 三个测试方法）
 */
class ModelImportTest {

    @TempDir
    Path tempDir;

    @Test
    void unchangedModelIsSkipped() throws Exception {
        Path storeDir = tempDir.resolve("store");
        IncrementalModelImporter importer = new IncrementalModelImporter(null, new ImmutableModelStore(storeDir));

        Path fA = tempDir.resolve("demo-a.json");
        Path fB = tempDir.resolve("demo-b.json");
        Files.writeString(fA, "{\"obj\":{\"id\":\"m_inc\",\"label\":\"增量模型\",\"version\":10}}", StandardCharsets.UTF_8);
        Files.writeString(fB, "{\"obj\":{\"cellMap\":{\"v1\":[]}}}", StandardCharsets.UTF_8);

        Assertions.assertTrue(importer.importLocalPair(fA, fB).newlyStored());
        Assertions.assertFalse(importer.importLocalPair(fA, fB).newlyStored());
    }

    @Test
    void changedModelBecomesNewVersion() throws Exception {
        ImmutableModelStore store = new ImmutableModelStore(tempDir.resolve("store2"));
        IncrementalModelImporter importer = new IncrementalModelImporter(null, store);

        Path fA = tempDir.resolve("demo2-a.json");
        Path fB = tempDir.resolve("demo2-b.json");
        Files.writeString(fA, "{\"obj\":{\"id\":\"m_ver\",\"label\":\"多版本模型\",\"version\":1}}", StandardCharsets.UTF_8);
        Files.writeString(fB, "{\"obj\":{\"cellMap\":{\"v1\":[]}}}", StandardCharsets.UTF_8);
        CorpusManifestEntry v1 = importer.importLocalPair(fA, fB).entry();

        // 修改画布内容再次导入
        Files.writeString(fB, "{\"obj\":{\"cellMap\":{\"v1\":[{\"id\":\"c1\",\"shape\":\"node-process\"}]}}}", StandardCharsets.UTF_8);
        CorpusManifestEntry v2 = importer.importLocalPair(fA, fB).entry();

        Assertions.assertEquals(1, v1.version());
        Assertions.assertEquals(2, v2.version());
        Assertions.assertEquals(2, store.listAllEntries().size());
    }

    @Test
    void importSpecificVersion() throws Exception {
        ImmutableModelStore store = new ImmutableModelStore(tempDir.resolve("store3"));
        IncrementalModelImporter importer = new IncrementalModelImporter(null, store);

        Path fA = tempDir.resolve("demo3-a.json");
        Path fB = tempDir.resolve("demo3-b.json");
        Files.writeString(fA, "{\"obj\":{\"id\":\"m_pick\",\"label\":\"指定版本\",\"version\":1}}", StandardCharsets.UTF_8);
        Files.writeString(fB, "{\"obj\":{\"cellMap\":{\"step\":1}}}", StandardCharsets.UTF_8);
        importer.importLocalPair(fA, fB);

        Files.writeString(fA, "{\"obj\":{\"id\":\"m_pick\",\"label\":\"指定版本\",\"version\":2}}", StandardCharsets.UTF_8);
        Files.writeString(fB, "{\"obj\":{\"cellMap\":{\"step\":2}}}", StandardCharsets.UTF_8);
        importer.importLocalPair(fA, fB);

        CorpusManifestEntry pickedV1 = importer.loadSpecificVersionOrThrow("m_pick", 1);
        CorpusManifestEntry pickedV2 = importer.loadSpecificVersionOrThrow("m_pick", 2);
        Assertions.assertNotEquals(pickedV1.digestPartB(), pickedV2.digestPartB());
    }
}

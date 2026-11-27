package com.groma.studio.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 覆盖 VFS <10ms 刷新、@GromaUserCode 无损保留、保护区外篡改检测告警及父 POM 自动注册
 */
class WorkspaceSyncStandaloneTest {

    @TempDir
    Path tempWorkspace;

    @Test
    void vfsDiffUnderTenMillisAndPreservesUserCodeAcrossRegeneration() throws Exception {
        InMemoryWorkspaceVfs vfs = new InMemoryWorkspaceVfs();
        String initialJava = """
                package com.groma.models.uav.service.impl;
                public class UavServiceImpl {
                    public double calcStep(double battery) {
                        // @GromaUserCode-START [proc_calc_step]
                        return battery - 1.0D;
                        // @GromaUserCode-END [proc_calc_step]
                    }
                }
                """;
        InMemoryWorkspaceVfs.VfsDiffSummary diff = vfs.refreshModuleFiles(
                "model-uav", Map.of("src/main/java/com/groma/models/uav/service/impl/UavServiceImpl.java", initialJava));
        Assertions.assertEquals(1, diff.addedPaths().size());
        Assertions.assertTrue(diff.elapsedMicros() < 10_000L, "VFS 内存刷新耗时应小于 10ms");

        // 初始化父聚合 POM
        Path modelsDir = Files.createDirectories(tempWorkspace.resolve("groma-models"));
        Files.writeString(modelsDir.resolve("pom.xml"), """
                <project>
                    <modules>
                    </modules>
                </project>
                """, StandardCharsets.UTF_8);

        AtomicWorkspaceDiskSynchronizer sync = new AtomicWorkspaceDiskSynchronizer(tempWorkspace);
        String relJava = "src/main/java/com/groma/models/uav/service/impl/UavServiceImpl.java";
        sync.syncModelModuleToDisk("model-uav", Map.of(relJava, initialJava));

        // 模拟工程师在 @GromaUserCode 保护区内编写二次开发业务代码，同时意外改动了保护区外注释/方法签名
        Path javaFileOnDisk = modelsDir.resolve("model-uav").resolve(relJava);
        String userEdited = """
                package com.groma.models.uav.service.impl;
                public class UavServiceImpl {
                    // 工程师在保护区外误加了一行手工注释
                    public double calcStep(double battery) {
                        // @GromaUserCode-START [proc_calc_step]
                        double windFactor = 1.25D;
                        return battery - (2.0D * windFactor);
                        // @GromaUserCode-END [proc_calc_step]
                    }
                }
                """;
        Files.writeString(javaFileOnDisk, userEdited, StandardCharsets.UTF_8);

        // 画布新增第二个生成方法后再次同步落盘
        String regeneratedJava = """
                package com.groma.models.uav.service.impl;
                public class UavServiceImpl {
                    public double calcStep(double battery) {
                        // @GromaUserCode-START [proc_calc_step]
                        return battery - 1.0D;
                        // @GromaUserCode-END [proc_calc_step]
                    }
                    public boolean isLow(double battery) {
                        return battery < 20.0D;
                    }
                }
                """;
        AtomicWorkspaceDiskSynchronizer.DiskSyncReport report =
                sync.syncModelModuleToDisk("model-uav", Map.of(relJava, regeneratedJava));

        String mergedOnDisk = Files.readString(javaFileOnDisk, StandardCharsets.UTF_8);
        Assertions.assertTrue(mergedOnDisk.contains("double windFactor = 1.25D;"), "手写保护区逻辑必须 100% 保留");
        Assertions.assertTrue(mergedOnDisk.contains("public boolean isLow"), "新生成的骨架方法必须同步写入");
        Assertions.assertFalse(report.warnings().isEmpty(), "保护区外手工修改必须触发明确告警");
        Assertions.assertTrue(Files.readString(modelsDir.resolve("pom.xml")).contains("<module>model-uav</module>"));
    }
}

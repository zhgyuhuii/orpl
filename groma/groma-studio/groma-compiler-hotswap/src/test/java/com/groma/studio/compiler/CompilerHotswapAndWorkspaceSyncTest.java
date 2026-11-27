package com.groma.studio.compiler;

import com.groma.studio.codegen.AstSourceMap;
import com.groma.studio.codegen.GeneratedModuleBundle;
import com.groma.studio.sync.SyncReport;
import com.groma.studio.sync.WorkspaceSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 覆盖 NP-BLD-001/004、BE-CMP-003/004、NP-HOT-001/002/003 的完整集成单元测试
 */
class CompilerHotswapAndWorkspaceSyncTest {

    @TempDir
    Path tempDir;

    @Test
    void ecjAndJavacCompileInnerAnonymousClassesAndMapErrorsToCanvasElement() throws Exception {
        String pkg = "com.groma.models.demo";
        String relPath = "src/main/java/com/groma/models/demo/service/impl/DemoCalcServiceImpl.java";
        String codeWithInnerAndAnonymous = """
                package com.groma.models.demo.service.impl;

                import com.groma.base.common.annotation.GromaMeta;
                import java.util.concurrent.Callable;

                @GromaMeta(id = "type_demo_calc", zhName = "推演计算器")
                public class DemoCalcServiceImpl {

                    private static final class StaticFactor {
                        static double scale() { return 2.5D; }
                    }

                    @GromaMeta(id = "proc_inner_test", zhName = "含匿名类与内部类的过程")
                    public double compute(double input) throws Exception {
                        class LocalAdder {
                            double add(double x) { return x + 10.0D; }
                        }
                        Callable<Double> anonTask = new Callable<Double>() {
                            @Override
                            public Double call() {
                                return new LocalAdder().add(input) * StaticFactor.scale();
                            }
                        };
                        return anonTask.call();
                    }
                }
                """;

        AstSourceMap sourceMap = new AstSourceMap();
        sourceMap.indexJavaSource(relPath, codeWithInnerAndAnonymous);

        EcjInMemoryCompiler ecj = new EcjInMemoryCompiler();
        CompilationResult ecjRes = ecj.compile("model-demo", Map.of(relPath, codeWithInnerAndAnonymous), sourceMap, getClass().getClassLoader());
        Assertions.assertTrue(ecjRes.success(), () -> "ECJ 编译应当通过: " + ecjRes.diagnostics());
        // 验证内部类 StaticFactor、LocalAdder 与匿名类 $1 均被产出并可由 ModelModuleClassLoader 加载执行（根治 BE-14）
        Assertions.assertTrue(ecjRes.classBytecodes().size() >= 4);

        try (ModelModuleClassLoader cl = new ModelModuleClassLoader("demo", "1", pkg, ecjRes.classBytecodes(), getClass().getClassLoader())) {
            Class<?> implCls = cl.loadClass(pkg + ".service.impl.DemoCalcServiceImpl");
            Object instance = implCls.getDeclaredConstructor().newInstance();
            Method m = implCls.getMethod("compute", double.class);
            double val = (double) m.invoke(instance, 6.0D);
            Assertions.assertEquals(40.0D, val, 1e-6D);
        }

        // 若当前测试环境包含系统 javac，同步执行交叉编译一致性验证（CK-NP-BLD-001-2）
        JavacParityVerifier javacVerifier = new JavacParityVerifier();
        if (javacVerifier.isSystemJavacAvailable()) {
            CompilationResult javacRes = javacVerifier.verifyWithJavac("model-demo", Map.of(relPath, codeWithInnerAndAnonymous), sourceMap);
            Assertions.assertTrue(javacRes.success(), () -> "Javac 交叉验证应当一致通过: " + javacRes.diagnostics());
        }

        // 注入语法错误，断言编译诊断能通过 AstSourceMap 精确定位回画布过程 ID proc_inner_test（CK-NP-BLD-001-3）
        String brokenCode = codeWithInnerAndAnonymous.replace("return anonTask.call();", "int bad = \"typeMismatch\"; return 0.0D;");
        CompilationResult brokenRes = ecj.compile("model-demo", Map.of(relPath, brokenCode), sourceMap, getClass().getClassLoader());
        Assertions.assertFalse(brokenRes.success());
        Assertions.assertTrue(
                brokenRes.diagnostics().stream().anyMatch(d -> "proc_inner_test".equals(d.mappedElementId())),
                "编译错误必须精确映射回画布过程 ID proc_inner_test"
        );
    }

    @Test
    void versionPinnedSessionKeepsRunningSessionOnV1WhileNewSessionUsesV2AndCollectsOldClassLoader() throws Exception {
        Path cacheDir = tempDir.resolve("bytecode-cache");
        AtomicBytecodeCache cache = new AtomicBytecodeCache(cacheDir, EcjInMemoryCompiler.ECJ_COMPILER_TAG, "1.0.0");
        VersionPinnedHotSwapManager manager = new VersionPinnedHotSwapManager(
                new EcjInMemoryCompiler(), cache, getClass().getClassLoader());

        GeneratedModuleBundle v1Bundle = createVersionedBundle("100");
        VersionPinnedHotSwapManager.HotSwapReport r1 = manager.compileAndMountIncremental("uav", "1", v1Bundle);
        Assertions.assertTrue(r1.recompiled());

        // 同一份源码重复挂载：应跳过重编（CK-NP-HOT-001-1）
        VersionPinnedHotSwapManager.HotSwapReport r1Repeat = manager.compileAndMountIncremental("uav", "1", v1Bundle);
        Assertions.assertFalse(r1Repeat.recompiled());

        // 旧会话 A 启动并持有 v1 租约
        VersionPinnedHotSwapManager.SessionModelLease sessionALease = manager.acquireLatestSessionLease("uav");
        WeakReference<ClassLoader> v1LoaderWeakRef = extractLoaderWeakRef(
                () -> {
                    try {
                        return sessionALease.loadClass("com.groma.models.uav.VersionProbe").getClassLoader();
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );

        // 热更新发布 v2 版本（返回值从 100 变为 200）
        GeneratedModuleBundle v2Bundle = createVersionedBundle("200");
        VersionPinnedHotSwapManager.HotSwapReport r2 = manager.compileAndMountIncremental("uav", "2", v2Bundle);
        Assertions.assertTrue(r2.recompiled());

        // 新会话 B 获取租约：立即拿到 v2 版本（返回 200），而运行中的旧会话 A 不受影响仍返回 100（CK-NP-HOT-002-1 & 2）
        try (VersionPinnedHotSwapManager.SessionModelLease sessionBLease = manager.acquireLatestSessionLease("uav")) {
            Assertions.assertEquals("2", sessionBLease.modelVersion());
            Assertions.assertEquals(200, invokeProbeVersion(sessionBLease));
            Assertions.assertEquals("1", sessionALease.modelVersion());
            Assertions.assertEquals(100, invokeProbeVersion(sessionALease));
        }

        // 旧会话 A 结束关闭租约 -> 触发旧版本 ClassLoader 释放并验证可被 GC 回收（CK-NP-HOT-002-3 & CK-BE-CMP-003-2）
        sessionALease.close();
        for (int i = 0; i < 10 && v1LoaderWeakRef.get() != null; i++) {
            System.gc();
            Thread.sleep(30L);
        }
        Assertions.assertNull(v1LoaderWeakRef.get(), "旧会话结束后，退役版本的 ModelModuleClassLoader 必须可被 GC 回收");
    }

    @Test
    void workspaceSyncSkipsUnchangedPreservesUserCodeAndFlagsOutsideOverwrite() throws Exception {
        Path wsRoot = tempDir.resolve("workspace");
        WorkspaceSyncService syncService = new WorkspaceSyncService();

        String relFile = "src/main/java/com/groma/models/flood/service/impl/PumpServiceImpl.java";
        String initialGenerated = """
                package com.groma.models.flood.service.impl;

                import com.groma.base.common.annotation.GromaMeta;

                @GromaMeta(id = "svc_pump", zhName = "排水泵服务")
                public class PumpServiceImpl {
                    public double calcFlow(double base) {
                        double factor = 1.0D;
                        // @GromaUserCode-START [op_pump_flow]
                        // 默认空保护区
                        // @GromaUserCode-END [op_pump_flow]
                        return base * factor;
                    }
                }
                """;

        AstSourceMap map1 = new AstSourceMap();
        GeneratedModuleBundle b1 = new GeneratedModuleBundle("model-flood", "com.groma.models.flood", map1);
        b1.putFile(relFile, initialGenerated);

        // 1. 首次同步：写入文件并自动登记 groma-models/pom.xml
        SyncReport rep1 = syncService.syncModuleToWorkspace(wsRoot, b1);
        Assertions.assertEquals(1, rep1.writtenFiles().size());
        Assertions.assertTrue(Files.readString(wsRoot.resolve("groma-models/pom.xml")).contains("<module>model-flood</module>"));

        // 2. 无变更二次同步：100% 跳过写盘（CK-NP-HOT-003-1）
        SyncReport rep2 = syncService.syncModuleToWorkspace(wsRoot, b1);
        Assertions.assertTrue(rep2.writtenFiles().isEmpty());
        Assertions.assertEquals(1, rep2.skippedUnchangedFiles().size());

        // 3. 模拟工程师在 @GromaUserCode 保护区内编写自定义逻辑，同时误改了保护区外的 factor = 999.0D
        Path diskJava = wsRoot.resolve("groma-models/model-flood").resolve(relFile);
        String editedOnDisk = initialGenerated
                .replace("double factor = 1.0D;", "double factor = 999.0D;")
                .replace("// 默认空保护区", "base = base + 50.0D;");
        Files.writeString(diskJava, editedOnDisk, StandardCharsets.UTF_8);

        // 4. 模型重新生成（factor 升级为 2.0D）：验证手写保护区保留 + 保护区外越界修改被覆盖且产出告警（CK-NP-HOT-003-2 & 3）
        String regeneratedV2 = initialGenerated.replace("double factor = 1.0D;", "double factor = 2.0D;");
        GeneratedModuleBundle b2 = new GeneratedModuleBundle("model-flood", "com.groma.models.flood", new AstSourceMap());
        b2.putFile(relFile, regeneratedV2);

        SyncReport rep3 = syncService.syncModuleToWorkspace(wsRoot, b2);
        Assertions.assertEquals(List.of("op_pump_flow"), rep3.preservedUserCodeRegionIds());
        Assertions.assertEquals(1, rep3.outsideOverwriteWarnings().size());

        String mergedDiskText = Files.readString(diskJava, StandardCharsets.UTF_8);
        Assertions.assertTrue(mergedDiskText.contains("base = base + 50.0D;"), "手写保护区内的代码必须完整保留");
        Assertions.assertTrue(mergedDiskText.contains("double factor = 2.0D;"), "保护区外的自动生成区应更新为最新模型逻辑");
    }

    private static WeakReference<ClassLoader> extractLoaderWeakRef(Supplier<ClassLoader> supplier) {
        return new WeakReference<>(supplier.get());
    }

    private static int invokeProbeVersion(VersionPinnedHotSwapManager.SessionModelLease lease) throws Exception {
        Class<?> cls = lease.loadClass("com.groma.models.uav.VersionProbe");
        Method m = cls.getMethod("versionNum");
        return (int) m.invoke(null);
    }

    private static GeneratedModuleBundle createVersionedBundle(String returnIntLiteral) {
        GeneratedModuleBundle b = new GeneratedModuleBundle("model-uav", "com.groma.models.uav", new AstSourceMap());
        b.putFile("src/main/java/com/groma/models/uav/VersionProbe.java", """
                package com.groma.models.uav;
                public final class VersionProbe {
                    private VersionProbe() {}
                    public static int versionNum() { return %s; }
                }
                """.formatted(returnIntLiteral));
        return b;
    }
}

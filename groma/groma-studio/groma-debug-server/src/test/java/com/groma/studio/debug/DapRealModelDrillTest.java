package com.groma.studio.debug;

import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.runtime.GromaSessionManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import com.groma.base.engine.WorldSnapshot;
import com.groma.studio.codegen.AstSourceMap;
import com.groma.studio.codegen.GeneratedModuleBundle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 第六章 6.3 DAP 演练（真实生成模型）：画布图元 → 生成源码行断点 → 单写者线程挂起（携带画布图元 id）→
 * 在线改值 → continue → 推演恢复。探针接线：生成任务 fireLineProbe → 调试会话 onLineProbe。
 */
class DapRealModelDrillTest {

    private static final AtomicReference<DapProtocolModels.DapStoppedEvent> STOPPED =
            new AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicLong probeCalls =
            new java.util.concurrent.atomic.AtomicLong();
    private static final AtomicReference<String> lastProbe = new AtomicReference<>("");
    private static final AtomicReference<String> firstProbe = new AtomicReference<>("");

    @Test
    void dapBreakpointSuspendsRealModelTaskAndResumeContinues() throws Exception {
        GeneratedModuleBundle bundle = generateRealModule();
        Map<String, byte[]> bytecodes = compileRealModule(bundle);
        GromaModuleRuntime runtime = mountRealModule(bundle, bytecodes, resourcesOf(bundle));
        AstSourceMap sourceMap = bundle.getSourceMap();

        // 画布图元 → 生成源码行映射登记（断点判定链路：图元 id → 行号）
        String taskFile = "src/main/java/com/groma/models/emergencydeduction/engine/process/RainfallRainfallProcessTask.java";
        int bpLine = sourceMap.firstProbeLineOfFile(taskFile);
        Assertions.assertTrue(bpLine > 0, "生成任务文件应有探针行（fireLineProbe 映射）");

        VirtualThreadDebugSession debug = new VirtualThreadDebugSession("dbg_real", sourceMap, System.nanoTime());
        DapWebSocketRouter router = new DapWebSocketRouter();
        router.registerDebugSession("dbg_real", debug);
        router.handleDapRequest("dbg_real", new DapProtocolModels.DapRequest(1, "setBreakpoints", Map.of(
                "file", taskFile,
                "breakpoints", List.of(Map.of("line", bpLine, "condition", "", "logMessage", "")))));

        GromaSessionManager manager = new GromaSessionManager(null, 4);
        try {
            String scene = runtime.scenes().get(0).sceneCode();
            AtomicReference<GromaProcessTask> rainfallTask = new AtomicReference<>();
            String sessionId = manager.startSession(runtime, scene, Map.of(), 42L, true, ctx -> {
                var rain = ctx.spawn(runtime.newEntity("Rainfall", "dap-rain-1"));
                var task = runtime.startManually("dv:designView_fjlFgY29HmzqjbmHxYC", ctx, rain,
                        Map.of("雨量序列", buildSeries(ctx)));
                Assertions.assertNotNull(task, "手动启动降雨任务应成功");
                // 探针接线：画布图元 id → 生成源码行 → 调试会话（断点判定/挂起）
                task.setDebugProbe(elementId -> {
                    try {
                        int line = sourceMap.lineOf(elementId);
                        probeCalls.incrementAndGet();
                        lastProbe.set(elementId + "@" + line);
                        firstProbe.compareAndSet("", elementId + "@" + line);
                        if (line < 0) {
                            return;
                        }
                        debug.onLineProbe(taskFile, line, "execNode", ctx.getTickIndex(),
                                "dap-rain-1", Map.of("tick", ctx.getTickIndex()), STOPPED::set);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                ctx.launch(task);
                rainfallTask.set(task);
            });
            Assertions.assertNotNull(rainfallTask.get(), "降雨任务应已创建");

            // 推步触发探针；控制线程在线改值并 continue（挂起发生在单写者/调用线程）
            // 控制线程：3 秒后无条件在线改值并 continue（无论是否挂起，resume 均安全）
            Thread control = new Thread(() -> {
                try {
                    Thread.sleep(3_000L);
                    router.handleDapRequest("dbg_real", new DapProtocolModels.DapRequest(
                            2, "setVariable", Map.of("name", "tick", "value", 999)));
                    router.handleDapRequest("dbg_real", new DapProtocolModels.DapRequest(3, "continue", Map.of()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            control.start();
            WorldSnapshot snapshot = manager.stepSession(sessionId, 5);
            control.join(5_000L);

            Assertions.assertTrue(probeCalls.get() > 0, "探针应被调用（次=" + probeCalls.get()
                    + "，最后=" + lastProbe.get() + "）");
            Assertions.assertTrue(probeCalls.get() > 0, "探针计数 " + probeCalls.get() + " 最后=" + lastProbe.get());
            Assertions.assertTrue(debug.suspendCount() > 0 || STOPPED.get() != null,
                    "调试挂起 " + debug.suspendCount() + " 次；断点键=" + debug.activeBreakpointKeys()
                            + "；最后探针=" + lastProbe.get() + "；首个探针=" + firstProbe.get());
            Assertions.assertNotNull(STOPPED.get(), "应在生成任务源码行命中断点并产生 stopped 事件");
            Assertions.assertFalse(STOPPED.get().highlightedCanvasElementId().isEmpty(),
                    "挂起事件应携带真实画布图元 id");
            Assertions.assertTrue(snapshot.tickIndex() >= 1, "恢复后推演应继续推进");
            manager.stopSession(sessionId);
        } finally {
            manager.close();
        }
    }

    private static Map<String, Object> buildSeries(com.groma.base.engine.GromaWorldContext ctx) {
        long start = ctx.currentWorldTimeMs();
        Map<String, Object> series = new java.util.LinkedHashMap<>();
        for (long ms = start - 5_000L; ms <= start + 700_000L; ms += 1_000L) {
            series.put(String.valueOf(ms), 2.0D);
        }
        return series;
    }

    // ---- compiler-hotswap 测试工具的同构实现（自包含，跨模块不依赖其测试类）----

    private static java.util.Optional<java.nio.file.Path> corpusDir() {
        String configured = System.getProperty("groma.corpus");
        if (configured != null && !configured.isBlank()) {
            return java.util.Optional.of(java.nio.file.Path.of(configured));
        }
        for (java.nio.file.Path p = java.nio.file.Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            java.nio.file.Path corpus = p.resolve("docs/model");
            if (java.nio.file.Files.isRegularFile(corpus.resolve("data-a.json"))) {
                return java.util.Optional.of(corpus);
            }
        }
        return java.util.Optional.empty();
    }

    private static GeneratedModuleBundle generateRealModule() {
        java.nio.file.Path dir = corpusDir().orElseThrow(
                () -> new IllegalStateException("未找到语料目录 docs/model（可用 -Dgroma.corpus=<目录> 指定）"));
        com.groma.studio.ir.GromaModuleIR ir = new com.groma.studio.parser.AbModelMergerAndParser()
                .parse(dir.resolve("data-a.json"), dir.resolve("data-b.json"));
        GeneratedModuleBundle bundle = new com.groma.studio.codegen.GromaModuleSourceGeneratorFacade()
                .generateModule(ir, Map.of());
        java.nio.file.Path out = java.nio.file.Path.of("target", "generated-" + bundle.getModuleDirName());
        bundle.getFiles().forEach((rel, content) -> {
            try {
                java.nio.file.Path file = out.resolve(rel);
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.writeString(file, content, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        return bundle;
    }

    private static Map<String, byte[]> compileRealModule(GeneratedModuleBundle bundle) {
        com.groma.studio.compiler.CompilationResult result = new com.groma.studio.compiler.EcjInMemoryCompiler().compile(
                bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap(),
                DapRealModelDrillTest.class.getClassLoader());
        org.junit.jupiter.api.Assertions.assertTrue(result.success(), "ECJ 编译应成功");
        return Map.copyOf(result.classBytecodes());
    }

    private static Map<String, byte[]> resourcesOf(GeneratedModuleBundle bundle) {
        return com.groma.studio.compiler.EcjInMemoryCompiler.extractResources(bundle.getFiles());
    }

    private static GromaModuleRuntime mountRealModule(GeneratedModuleBundle bundle, Map<String, byte[]> bytecodes,
                                                      Map<String, byte[]> resources) {
        com.groma.studio.compiler.ModelModuleClassLoader loader = new com.groma.studio.compiler.ModelModuleClassLoader(
                bundle.getModuleDirName(), "real", bundle.getBasePackage(), bytecodes, resources,
                DapRealModelDrillTest.class.getClassLoader());
        java.util.ServiceLoader<GromaModuleRuntime> services =
                java.util.ServiceLoader.load(GromaModuleRuntime.class, loader);
        java.util.List<GromaModuleRuntime> runtimes = new java.util.ArrayList<>();
        services.forEach(runtimes::add);
        Assertions.assertEquals(1, runtimes.size(), "生成的模块应登记且仅登记一个运行时入口");
        return runtimes.get(0);
    }
}
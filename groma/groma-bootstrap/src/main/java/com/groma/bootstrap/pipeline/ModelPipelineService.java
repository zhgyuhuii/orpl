package com.groma.bootstrap.pipeline;

import com.groma.base.engine.GromaModuleRuntime;
import com.groma.studio.codegen.GeneratedModuleBundle;
import com.groma.studio.codegen.GromaModuleSourceGeneratorFacade;
import com.groma.studio.compiler.CanvasCompileDiagnostic;
import com.groma.studio.compiler.CompilationResult;
import com.groma.studio.compiler.JavacParityVerifier;
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.parser.AbModelMergerAndParser;
import com.groma.studio.sync.SyncReport;
import com.groma.studio.sync.WorkspaceSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 宿主内模型构建流水线（阶段 2-C3）：语料文件 → 解析 → 校验 → 五层出码 → 工程落盘 →
 * ECJ 编译热挂载 → javac 一致性对拍。
 * <p>
 * 落盘目标是独立工作区目录（{@code groma.pipeline.workspace-root}，默认 ./groma-workspace），
 * 与手写示例模型模块隔离（E1 决策：示例模块保持不动，生成模块落独立工作区）。
 * </p>
 */
@Service
public class ModelPipelineService {
    private static final Logger LOG = LoggerFactory.getLogger(ModelPipelineService.class);

    private final VersionPinnedHotSwapManager hotSwap;
    private final WorkspaceSyncService workspaceSync;
    private final JavacParityVerifier javacVerifier;
    private final Path workspaceRoot;
    private final ObjectMapper mapper = new ObjectMapper();

    public ModelPipelineService(VersionPinnedHotSwapManager hotSwap,
                                WorkspaceSyncService workspaceSync,
                                JavacParityVerifier javacVerifier,
                                @org.springframework.beans.factory.annotation.Value(
                                        "${groma.pipeline.workspace-root:groma-workspace}") String workspaceRoot) {
        this.hotSwap = hotSwap;
        this.workspaceSync = workspaceSync;
        this.javacVerifier = javacVerifier;
        this.workspaceRoot = Path.of(workspaceRoot);
    }

    /** 构建结果报告（REST 返回体，全部为可序列化字段） */
    public record BuildReport(
            String modelCode,
            String modelVersion,
            String moduleDirName,
            int sourceFileCount,
            List<String> warnings,
            boolean recompiled,
            boolean cacheHit,
            long compileMillis,
            boolean javacParity,
            String javacNote,
            String workspaceModuleDir,
            List<String> problems
    ) {}

    /**
     * 六步流水线：解析 → 校验 → 出码 → 工程落盘 → 编译热挂载 → javac 对拍
     */
    public BuildReport buildFromCorpus(Path dataA, Path dataB) {
        List<String> problems = new ArrayList<>();
        // 1. 解析（A+B 合并 → 编译计划）
        GromaModuleIR ir = new AbModelMergerAndParser().parse(dataA, dataB);
        CompilePlanIR plan = ir.compilePlan();
        List<String> warnings = plan.warnings();
        // 2. 出码
        GeneratedModuleBundle bundle = new GromaModuleSourceGeneratorFacade().generateModule(ir, Map.of());
        String modelCode = runtimeCodeOf(bundle);
        // 3. 工程落盘（原子同步 + 父 POM 模块登记 + 手写区合并）；布局 <workspace>/groma-models/<module>/
        String workspaceModuleDir;
        try {
            SyncReport syncReport = workspaceSync.syncModuleToWorkspace(workspaceRoot, bundle);
            workspaceModuleDir = workspaceRoot.resolve("groma-models").resolve(bundle.getModuleDirName()).toString();
            if (syncReport != null) {
                problems.addAll(syncReport.outsideOverwriteWarnings());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("工程落盘失败: " + ex.getMessage(), ex);
        }
        // 4. ECJ 编译并热挂载（同摘要 O(1) 跳过）
        VersionPinnedHotSwapManager.HotSwapReport hotSwapReport;
        try {
            hotSwapReport = hotSwap.compileAndMountIncremental(modelCode, ir.modelVersion() + "", bundle);
        } catch (IOException ex) {
            throw new UncheckedIOException("编译挂载失败: " + ex.getMessage(), ex);
        }
        for (CanvasCompileDiagnostic d : hotSwapReport.diagnostics()) {
            if (d.error()) {
                problems.add(d.relativeJavaFile() + ":" + d.javaLine() + " " + d.message());
            }
        }
        hotMounted.removeIf(m -> m.modelCode().equals(modelCode));
        hotMounted.add(new MountedModelView(modelCode, ir.modelVersion() + "", "热挂载",
                ir.compilePlan().types().size(), ir.compilePlan().processDefs().size(), ir.scenes().size(), "hotswap"));
        // 5. javac 一致性对拍（JDK 可用时）
        boolean javacParity = false;
        String javacNote;
        if (javacVerifier.isSystemJavacAvailable()) {
            try {
                CompilationResult javacResult = javacVerifier.verifyWithJavac(
                        bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap());
                javacParity = javacResult.success();
                javacNote = "javac 对拍完成，类数量 " + javacResult.classBytecodes().size();
            } catch (RuntimeException ex) {
                javacNote = "javac 对拍失败: " + ex.getMessage();
                problems.add(javacNote);
            }
        } else {
            javacNote = "当前 JRE 无系统 javac，跳过对拍（ECJ 编译已通过）";
        }
        BuildReport report = new BuildReport(modelCode, ir.modelVersion() + "", bundle.getModuleDirName(),
                bundle.getFiles().size(), warnings, hotSwapReport.recompiled(), hotSwapReport.cacheHit(),
                hotSwapReport.compileMillis(), javacParity, javacNote, workspaceModuleDir,
                List.copyOf(problems));
        LOG.info("[Pipeline] 模型 {} v{} 构建完成：源文件 {}，告警 {}，重编 {}，javac 对拍 {}",
                modelCode, ir.modelVersion(), bundle.getFiles().size(), warnings.size(),
                hotSwapReport.recompiled(), javacParity);
        return report;
    }

    /** 当前可见模型：合包经 ServiceLoader；热挂载模型以构建记录登记（完整注册中心在 T3 GromaModelRegistry） */
    public List<MountedModelView> mountedModels() {
        List<MountedModelView> out = new ArrayList<>();
        for (GromaModuleRuntime runtime : java.util.ServiceLoader.load(GromaModuleRuntime.class,
                ModelPipelineService.class.getClassLoader())) {
            out.add(new MountedModelView(runtime.modelCode(), runtime.modelVersion(), runtime.modelLabel(),
                    runtime.types().size(), runtime.processes().size(), runtime.scenes().size(), "bundled"));
        }
        for (MountedModelView built : hotMounted) {
            if (out.stream().noneMatch(m -> m.modelCode().equals(built.modelCode()))) {
                out.add(built);
            }
        }
        return out;
    }

    /** 本服务构建并热挂载过的模型（构建时登记，重建注册中心时由 GromaModelRegistry 接管） */
    private final List<MountedModelView> hotMounted = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 已挂载模型视图 */
    public record MountedModelView(
            String modelCode,
            String modelVersion,
            String modelLabel,
            int typeCount,
            int processCount,
            int sceneCount,
            String mountKind
    ) {}

    /** 从生成包读运行时的模型编码（运行时入口常量，避免重复解析） */
    private String runtimeCodeOf(GeneratedModuleBundle bundle) {
        return bundle.getModuleDirName().startsWith("model-")
                ? bundle.getModuleDirName().substring("model-".length())
                : bundle.getModuleDirName();
    }

    /** 语料 JSON 合法性预检（供 REST 上传前校验） */
    public void assertReadableCorpus(Path dataA, Path dataB) {
        try {
            mapper.readTree(Files.readAllBytes(dataA));
            mapper.readTree(Files.readAllBytes(dataB));
        } catch (IOException ex) {
            throw new IllegalArgumentException("语料文件不可读: " + ex.getMessage(), ex);
        }
    }
}

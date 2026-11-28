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
import com.groma.studio.parser.StaticModelValidator;
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
    private final GromaModelRegistry modelRegistry;
    private final Path workspaceRoot;
    private final ObjectMapper mapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public ModelPipelineService(VersionPinnedHotSwapManager hotSwap,
                                WorkspaceSyncService workspaceSync,
                                JavacParityVerifier javacVerifier,
                                GromaModelRegistry modelRegistry,
                                RepoModelsSyncService repoModelsSync,
                                @org.springframework.beans.factory.annotation.Value(
                                        "${groma.pipeline.workspace-root:groma-workspace}") String workspaceRoot) {
        this(hotSwap, workspaceSync, javacVerifier, modelRegistry, workspaceRoot, repoModelsSync);
    }

    /** 单测便捷构造：仓内合包同步未启用（E2 收割后集成测试默认不落仓） */
    public ModelPipelineService(VersionPinnedHotSwapManager hotSwap,
                                WorkspaceSyncService workspaceSync,
                                JavacParityVerifier javacVerifier,
                                GromaModelRegistry modelRegistry,
                                String workspaceRoot) {
        this(hotSwap, workspaceSync, javacVerifier, modelRegistry, workspaceRoot, null);
    }

    private ModelPipelineService(VersionPinnedHotSwapManager hotSwap,
                                 WorkspaceSyncService workspaceSync,
                                 JavacParityVerifier javacVerifier,
                                 GromaModelRegistry modelRegistry,
                                 String workspaceRoot,
                                 RepoModelsSyncService repoModelsSync) {
        this.hotSwap = hotSwap;
        this.workspaceSync = workspaceSync;
        this.javacVerifier = javacVerifier;
        this.modelRegistry = modelRegistry;
        this.workspaceRoot = Path.of(workspaceRoot);
        this.repoModelsSync = repoModelsSync == null
                ? new RepoModelsSyncService("") : repoModelsSync;
    }

    private final RepoModelsSyncService repoModelsSync;

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
        // 1. 解析（A+B 合并 → 编译计划）；语料短哈希收口 → 版本化模型编码（多版本并存、同内容幂等）
        byte[] bytesA;
        byte[] bytesB;
        try {
            bytesA = Files.readAllBytes(dataA);
            bytesB = Files.readAllBytes(dataB);
        } catch (IOException ex) {
            throw new UncheckedIOException("语料读取失败: " + ex.getMessage(), ex);
        }
        GromaModuleIR ir = new AbModelMergerAndParser().parse(bytesA, bytesB);
        // 语料短哈希（多版本并存收口）：在生成器内于基础码解析完成后追加，同内容幂等、异版本互不顶替
        String corpusHash = com.groma.studio.codegen.plan.ModelVersionHashUtil.shortCorpusHash(bytesA, bytesB);
        CompilePlanIR plan = ir.compilePlan();
        List<String> warnings = new ArrayList<>(plan.warnings());
        // 1.5 静态校验（VAL-001~010，修复原实现漏调校验器的断层）：
        //     ERROR 阻断出码并写入报告，WARNING 仅记录
        StaticModelValidator.ValidationSummary validation =
                new StaticModelValidator().validate(ir);
        for (StaticModelValidator.ValidationIssue issue : validation.issues()) {
            String line = "[" + issue.ruleId() + "] " + issue.elementName() + "（" + issue.elementId() + "）: "
                    + issue.message();
            if (issue.severity() == StaticModelValidator.Severity.ERROR) {
                problems.add(line);
            } else {
                warnings.add(line);
            }
        }
        if (!validation.passedWithoutErrors()) {
            throw new IllegalArgumentException(
                    "MODEL_VALIDATION_FAILED: 静态校验存在 " + problems.size() + " 条 ERROR（见 problems 明细），已阻断出码");
        }
        // 2. 出码（带语料短哈希 → 版本化模型编码）
        GeneratedModuleBundle bundle = new GromaModuleSourceGeneratorFacade().generateModule(ir, Map.of(), corpusHash);
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
        // 通知注册中心：刷新 MCP 动态工具清单（ModelToolRegistrar.onModelMountedAsync）
        modelRegistry.onModelMounted(modelCode);
        // 4.5 仓内合包同步（code4.html E2 收割）：repo-models-root 已配置时镜像进仓库 groma-models
        //      并重写聚合 pom 模块清单，兑现"下载即 mvn clean package 合包运行"（说明书 1.3 模式一）。
        //      同步失败不阻断流水线（workspace 热挂载路径不受影响），记入 problems 供部署侧感知。
        if (repoModelsSync.enabled()) {
            try {
                repoModelsSync.syncModule(bundle.getModuleDirName(),
                        Path.of(workspaceModuleDir));
            } catch (IOException ex) {
                problems.add("仓内合包同步失败: " + ex.getMessage());
            }
        }
        // 5. javac 一致性对拍（JDK 可用时）
        boolean javacParity = false;
        String javacNote;
        if (javacVerifier.isSystemJavacAvailable()) {
            try {
                CompilationResult javacResult = javacVerifier.verifyWithJavac(
                        bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap());
                javacParity = javacResult.success();
                javacNote = javacParity
                        ? "javac 对拍完成，类数量 " + javacResult.classBytecodes().size()
                        : "javac 对拍不一致（错误 " + javacResult.diagnostics().size() + " 条；"
                                + "fat jar 运行态嵌套依赖不在 java.class.path，javac 无法解析 groma-base——"
                                + "已知限制：运行态编译门禁为 ECJ，javac 对拍门禁在源码树/CI 生效）";
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

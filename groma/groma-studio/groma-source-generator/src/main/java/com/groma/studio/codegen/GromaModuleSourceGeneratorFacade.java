package com.groma.studio.codegen;

import com.groma.studio.codegen.plan.DtoWsRepositoryLayerEmitter;
import com.groma.studio.codegen.plan.LegacyPlanAdapter;
import com.groma.studio.codegen.plan.ManifestAndIndexEmitter;
import com.groma.studio.codegen.plan.ModelLayerEmitter;
import com.groma.studio.codegen.plan.ModuleRuntimeEmitter;
import com.groma.studio.codegen.plan.PlanModel;
import com.groma.studio.codegen.plan.ProcessTaskEmitter;
import com.groma.studio.codegen.plan.RelationLayerEmitter;
import com.groma.studio.codegen.plan.ServiceLayerEmitter;
import com.groma.studio.codegen.plan.WorldContextEmitter;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.GromaModuleIR;
import com.palantir.javapoet.JavaFile;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * 五层源码与 MCP 工具清单统一生成门面
 * <p>
 * 输入：GromaModuleIR（真实语料解析出的编译计划；旧版 12 分量 IR 自动适配）+ 已有符号锁 Map（首次导入传空 Map）<br>
 * 输出：GeneratedModuleBundle（controller / service / engine / model 五层 .java、pom.xml、groma-symbols.lock.json、
 * GROMA_MODEL_INDEX.md、manifest.json、运行时入口登记及双向 AstSourceMap）。
 * </p>
 * 同一份输入连续生成，整包 SHA-256 逐字节一致（CK-NP-GEN-001-1）。
 */
public final class GromaModuleSourceGeneratorFacade {

    private final ObjectMapper lockMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    public GeneratedModuleBundle generateModule(GromaModuleIR ir, Map<String, String> existingSymbolLocks) {
        return generateModule(ir, existingSymbolLocks, null);
    }

    /**
     * 带语料短哈希的出码入口（多版本并存收口）：版本化编码在基础码解析完成之后追加
     * （基础码可能来自 IR 显式编码，也可能由命名引擎按模型中文名转译），模块目录、Java 包名、
     * 注册编码随之版本化；Maven artifactId 保持稳定基础名。
     *
     * @param versionHash 语料内容 SHA-256 前 8 位十六进制；null/空 表示不版本化（既有测试与手写 IR 路径）
     */
    public GeneratedModuleBundle generateModule(GromaModuleIR ir, Map<String, String> existingSymbolLocks,
                                                String versionHash) {
        CompilePlanIR plan = (ir.compilePlan() == null || CompilePlanIR.EMPTY.equals(ir.compilePlan()))
                ? LegacyPlanAdapter.adapt(ir) : ir.compilePlan();
        PlanModel model = PlanModel.of(ir, plan, existingSymbolLocks, versionHash);
        ProcessCodeAstTranslator codeTranslator = new ProcessCodeAstTranslator();

        AstSourceMap sourceMap = new AstSourceMap();
        GeneratedModuleBundle bundle = new GeneratedModuleBundle(model.moduleDir, model.basePkg, sourceMap);

        // 1. 第 5 层：枚举、定值、领域实体、只读契约、VO、转换器
        ModelLayerEmitter modelEmitter = new ModelLayerEmitter(model);
        for (JavaFile file : modelEmitter.emitEnums()) {
            putJava(bundle, file);
        }
        putJava(bundle, modelEmitter.emitConstants());
        for (PlanModel.TypeInfo type : model.types.values()) {
            putJava(bundle, modelEmitter.emitEntity(type));
            putJava(bundle, modelEmitter.emitReadOnly(type));
            putJava(bundle, modelEmitter.emitVo(type));
        }
        putJava(bundle, modelEmitter.emitConvertUtil());

        // 2. 第 3 层：过程状态机与运行帧、场景单写者沙盒
        ProcessTaskEmitter taskEmitter = new ProcessTaskEmitter();
        for (PlanModel.ProcInfo proc : model.procList) {
            ProcessTaskEmitter.Result result = taskEmitter.emit(model, proc, codeTranslator);
            for (String[] script : result.scripts()) {
                bundle.putFile("src/main/resources/scripts/" + script[0], script[1]);
            }
            putJava(bundle, result.task());
            if (result.runtimeBo() != null) {
                putJava(bundle, result.runtimeBo());
            }
        }
        WorldContextEmitter worldEmitter = new WorldContextEmitter(model);
        for (PlanModel.SceneInfo scene : model.scenes) {
            putJava(bundle, worldEmitter.emit(scene));
        }

        // 3. 第 2 层与第 1 层：Service 接口与实现、REST 控制器、模块装配
        ServiceLayerEmitter serviceEmitter = new ServiceLayerEmitter(model);
        for (PlanModel.TypeInfo type : model.types.values()) {
            for (JavaFile file : serviceEmitter.emitService(type)) {
                putJava(bundle, file);
            }
        }
        for (JavaFile file : serviceEmitter.emitIndependentService()) {
            putJava(bundle, file);
        }
        // R07~R10 关联管理器（收割外部设计稿方案 B）：associationTypes → engine/relation/*RelationManager
        RelationLayerEmitter relationEmitter = new RelationLayerEmitter(model);
        for (JavaFile relFile : relationEmitter.emitAll()) {
            putJava(bundle, relFile);
        }
        for (PlanModel.SceneInfo scene : model.scenes) {
            putJava(bundle, serviceEmitter.emitController(scene));
        }
        // 标准分层契约补齐（code4.html M6 收割）：model/dto + ws + repository 三层逐场景产物
        DtoWsRepositoryLayerEmitter dtoWsEmitter = new DtoWsRepositoryLayerEmitter(model);
        for (JavaFile dtoWsFile : dtoWsEmitter.emitAll()) {
            putJava(bundle, dtoWsFile);
        }
        if (!model.scenes.isEmpty()) {
            putJava(bundle, serviceEmitter.emitConfiguration());
        }

        // 4. 运行时入口、过程目录、线上判断工具
        ModuleRuntimeEmitter runtimeEmitter = new ModuleRuntimeEmitter(model);
        putJava(bundle, runtimeEmitter.emitRuntime());
        putJava(bundle, runtimeEmitter.emitCatalog());
        putJava(bundle, runtimeEmitter.emitJudgeUtil());

        // 5. 模块级产物：pom、MCP 清单、双语索引、ServiceLoader 登记
        ManifestAndIndexEmitter manifestEmitter = new ManifestAndIndexEmitter(model);
        bundle.putFile("pom.xml", manifestEmitter.modulePom());
        bundle.putFile("src/main/resources/META-INF/groma/mcp/manifest.json", manifestEmitter.manifestJson());
        String index = manifestEmitter.indexMarkdown();
        bundle.putFile("GROMA_MODEL_INDEX.md", index);
        bundle.putFile("src/main/resources/META-INF/groma/GROMA_MODEL_INDEX.md", index);
        bundle.putFile("src/main/resources/META-INF/services/com.groma.base.engine.GromaModuleRuntime",
                manifestEmitter.serviceRegistration());
        bundle.putFile("groma-symbols.lock.json", lockMapper.writeValueAsString(model.lockTable.snapshot()));

        // 6. 扫描全部生成的 .java，构建精确到行列的双向 AstSourceMap（画布实体 id ↔ 源码文件与起止行）
        for (Map.Entry<String, String> entry : bundle.getFiles().entrySet()) {
            if (entry.getKey().endsWith(".java")) {
                sourceMap.indexJavaSource(entry.getKey(), entry.getValue());
            }
        }
        // 未索引文件进诊断流（plan.warnings() 是不可变拷贝，走 PlanModel.diagnostic 汇聚）
        for (String unindexed : sourceMap.unindexedFiles()) {
            model.diagnostic("[SOURCEMAP] " + unindexed);
        }
        return bundle;
    }

    private static void putJava(GeneratedModuleBundle bundle, JavaFile file) {
        bundle.putFile("src/main/java/" + file.packageName().replace('.', '/') + "/" + file.typeSpec().name() + ".java",
                file.toString());
    }

    /** 模型编码推导（供流水线在解析之后、落盘之前统一使用） */
    public static String deriveModelCode(GromaModuleIR ir) {
        CompilePlanIR plan = (ir.compilePlan() == null || CompilePlanIR.EMPTY.equals(ir.compilePlan()))
                ? LegacyPlanAdapter.adapt(ir) : ir.compilePlan();
        return PlanModel.of(ir, plan, Map.of()).modelCode;
    }

    /** 生成期的解析 / 编译诊断（不阻断生成，随同步报告一并输出） */
    public static List<String> diagnosticsOf(GromaModuleIR ir) {
        CompilePlanIR plan = (ir.compilePlan() == null || CompilePlanIR.EMPTY.equals(ir.compilePlan()))
                ? LegacyPlanAdapter.adapt(ir) : ir.compilePlan();
        return PlanModel.of(ir, plan, Map.of()).diagnostics;
    }
}

package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.PlanModel.ParamSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.codegen.plan.PlanModel.SceneInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 第 1 层（Controller REST 接入层）与第 2 层（Service 业务接口 + 实现层）生成器。
 * <p>
 * Service：对象类型的每个自有过程对应一个接口方法——耗时过程启动后返回过程任务，瞬时算子同步执行返回是否成功；
 * 实现方法体内留出 {@code @GromaUserCode} 保护区，供工程师二次开发（重生成源码时无损保留）。
 * Controller：每个运行场景一个控制器，统一挂载 /api/v1/models/{modelCode}/scenes/{sceneCode}，会话交给宿主的统一会话管理器。
 * </p>
 * 铁律：Spring Bean 不写硬编码名前缀，由宿主的 FullyQualifiedAnnotationBeanNameGenerator 自动隔离。
 */
public final class ServiceLayerEmitter {

    private static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");
    private static final ClassName REST_CONTROLLER = ClassName.get("org.springframework.web.bind.annotation", "RestController");
    private static final ClassName REQUEST_MAPPING = ClassName.get("org.springframework.web.bind.annotation", "RequestMapping");
    private static final ClassName POST_MAPPING = ClassName.get("org.springframework.web.bind.annotation", "PostMapping");
    private static final ClassName GET_MAPPING = ClassName.get("org.springframework.web.bind.annotation", "GetMapping");
    private static final ClassName DELETE_MAPPING = ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping");
    private static final ClassName PATH_VARIABLE = ClassName.get("org.springframework.web.bind.annotation", "PathVariable");
    private static final ClassName REQUEST_PARAM = ClassName.get("org.springframework.web.bind.annotation", "RequestParam");
    private static final ClassName REQUEST_BODY = ClassName.get("org.springframework.web.bind.annotation", "RequestBody");
    private static final ClassName CONFIGURATION = ClassName.get("org.springframework.context.annotation", "Configuration");
    private static final ClassName BEAN = ClassName.get("org.springframework.context.annotation", "Bean");

    private final PlanModel model;

    public ServiceLayerEmitter(PlanModel model) {
        this.model = model;
    }

    // ------------------------------------------------------------------ Service

    /** @return [接口, 实现]；该类型没有自有过程时返回空列表 */
    public List<JavaFile> emitService(TypeInfo type) {
        List<ProcInfo> owned = new ArrayList<>();
        for (ProcInfo proc : model.procList) {
            if (proc.owner() == type && !"INDEPENDENT".equals(proc.ownerKind())) {
                owned.add(proc);
            }
        }
        if (owned.isEmpty()) {
            return List.of();
        }
        TypeSpec.Builder ib = TypeSpec.interfaceBuilder(type.service()).addModifiers(Modifier.PUBLIC)
                .addJavadoc("$L 领域算子与过程服务接口（过程实现位于 engine 层的 *ProcessTask 状态机）\n", type.zhName());
        TypeSpec.Builder cb = TypeSpec.classBuilder(type.serviceImpl()).addModifiers(Modifier.PUBLIC)
                .addSuperinterface(type.service()).addAnnotation(SERVICE)
                .addJavadoc("$L 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留\n",
                        type.zhName());
        for (ProcInfo proc : owned) {
            boolean instant = !proc.timeConsuming();
            TypeName returnType = instant ? TypeName.BOOLEAN : PlanModel.PROCESS_TASK;
            String summary = instant ? "瞬时算子：同步执行完毕" : "耗时过程：挂入步进队列并返回过程任务";
            MethodSpec.Builder sig = MethodSpec.methodBuilder(proc.methodName()).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("$L（方圆过程原名：$L）—— $L\n", proc.owner().zhName(), proc.zhName(), summary)
                    .returns(returnType).addParameter(PlanModel.WORLD_CTX, "ctx").addParameter(type.entity(), "self");
            MethodSpec.Builder impl = MethodSpec.methodBuilder(proc.methodName()).addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC).returns(returnType)
                    .addParameter(PlanModel.WORLD_CTX, "ctx").addParameter(type.entity(), "self");
            for (ParamSlot param : proc.params()) {
                sig.addParameter(param.type(), param.field());
                impl.addParameter(param.type(), param.field());
            }
            CodeBlock.Builder call = CodeBlock.builder().add("new $T(ctx, self", proc.taskCn());
            for (ParamSlot param : proc.params()) {
                call.add(", $N", param.field());
            }
            call.add(")");
            impl.addCode(userCodeRegion(proc));
            if (instant) {
                impl.addStatement("return $L.startTopLevel()", call.build());
            } else {
                impl.addStatement("$T task = $L", PlanModel.PROCESS_TASK, call.build());
                impl.addStatement("ctx.launch(task)");
                impl.addStatement("return task");
            }
            ib.addMethod(sig.build());
            cb.addMethod(impl.build());
        }
        return List.of(
                JavaFile.builder(type.service().packageName(), ib.build()).indent("    ").build(),
                JavaFile.builder(type.serviceImpl().packageName(), cb.build()).indent("    ").build());
    }

    /**
     * 独立过程服务（说明书 3.2 节契约）：INDEPENDENT 过程（无宿主对象）统一生成
     * {@code IIndependentProcessService} + Impl 门面；瞬时算子同步执行，耗时过程挂入步进队列。
     * 该类型不存在独立过程时返回空列表。修复独立过程在 service 层完全缺失的缺陷。
     */
    public List<JavaFile> emitIndependentService() {
        List<ProcInfo> independents = new ArrayList<>();
        for (ProcInfo proc : model.procList) {
            if (proc.owner() == null && "INDEPENDENT".equals(proc.ownerKind())) {
                independents.add(proc);
            }
        }
        if (independents.isEmpty()) {
            return List.of();
        }
        ClassName svc = ClassName.get(model.basePkg + ".service", "IIndependentProcessService");
        ClassName impl = ClassName.get(model.basePkg + ".service.impl", "IndependentProcessServiceImpl");
        TypeSpec.Builder ib = TypeSpec.interfaceBuilder(svc).addModifiers(Modifier.PUBLIC)
                .addJavadoc("独立过程服务接口：模型内共 $L 个独立过程（无宿主对象，经由 engine 层 *ProcessTask 状态机执行）\n",
                        independents.size());
        TypeSpec.Builder cb = TypeSpec.classBuilder(impl).addModifiers(Modifier.PUBLIC)
                .addSuperinterface(svc).addAnnotation(SERVICE)
                .addJavadoc("独立过程服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留\n");
        for (ProcInfo proc : independents) {
            boolean instant = !proc.timeConsuming();
            TypeName returnType = instant ? TypeName.BOOLEAN : PlanModel.PROCESS_TASK;
            String summary = instant ? "独立瞬时算子：同步执行完毕" : "独立耗时过程：挂入步进队列并返回过程任务";
            MethodSpec.Builder sig = MethodSpec.methodBuilder(proc.methodName()).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("$L（方圆独立过程原名：$L）—— $L\n", proc.zhName(), proc.zhName(), summary)
                    .returns(returnType).addParameter(PlanModel.WORLD_CTX, "ctx");
            MethodSpec.Builder method = MethodSpec.methodBuilder(proc.methodName()).addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC).returns(returnType)
                    .addParameter(PlanModel.WORLD_CTX, "ctx");
            for (ParamSlot param : proc.params()) {
                sig.addParameter(param.type(), param.field());
                method.addParameter(param.type(), param.field());
            }
            CodeBlock.Builder call = CodeBlock.builder().add("new $T(ctx", proc.taskCn());
            for (ParamSlot param : proc.params()) {
                call.add(", $N", param.field());
            }
            call.add(")");
            method.addCode(userCodeRegion(proc));
            if (instant) {
                method.addStatement("return $L.startTopLevel()", call.build());
            } else {
                method.addStatement("$T task = $L", PlanModel.PROCESS_TASK, call.build());
                method.addStatement("ctx.launch(task)");
                method.addStatement("return task");
            }
            ib.addMethod(sig.build());
            cb.addMethod(method.build());
        }
        return List.of(
                JavaFile.builder(svc.packageName(), ib.build()).indent("    ").build(),
                JavaFile.builder(impl.packageName(), cb.build()).indent("    ").build());
    }

    /** 二次开发保护区（工作区同步器按 id 无损合并） */
    static CodeBlock userCodeRegion(ProcInfo proc) {
        return CodeBlock.of("""
                // @GromaUserCode-START [$S]
                // 过程「$L」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
                // @GromaUserCode-END [$S]

                """, proc.def().entityId(), proc.zhName(), proc.def().entityId());
    }

    // ------------------------------------------------------------------ Controller

    public JavaFile emitController(SceneInfo scene) {
        ClassName summaryCn = PlanModel.SESSION_MANAGER.nestedClass("SessionSummary");
        TypeSpec.Builder cb = TypeSpec.classBuilder(scene.controllerCn()).addModifiers(Modifier.PUBLIC)
                .addJavadoc("场景 REST 控制器：$L（统一挂载 /api/v1/models/$L/scenes/$L；无认证直接可达，"
                        + "后期由微服务网关按交接清单统一管控）\n", scene.zhName(), model.modelCode, scene.sceneCode())
                .addAnnotation(AnnotationSpec.builder(PlanModel.TAG)
                        .addMember("name", "$S", "[模型:" + model.modelLabel + "] " + scene.zhName()).build())
                .addAnnotation(REST_CONTROLLER)
                .addAnnotation(AnnotationSpec.builder(REQUEST_MAPPING)
                        .addMember("value", "$S", "/api/v1/models/" + model.modelCode + "/scenes/" + scene.sceneCode()).build())
                .addAnnotation(ProcessTaskEmitter.meta(scene.type().typeId(), scene.zhName() + "控制器", "CONTROLLER"))
                .addField(PlanModel.MODULE_RUNTIME, "moduleRuntime", Modifier.PRIVATE, Modifier.FINAL)
                .addField(PlanModel.SESSION_MANAGER, "sessionManager", Modifier.PRIVATE, Modifier.FINAL);
        cb.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addParameter(PlanModel.MODULE_RUNTIME, "moduleRuntime").addParameter(PlanModel.SESSION_MANAGER, "sessionManager")
                .addStatement("this.moduleRuntime = moduleRuntime").addStatement("this.sessionManager = sessionManager").build());

        // POST /sessions
        cb.addMethod(operation("startSession", "启动场景推演会话（startPaused=true 时创建为暂停态，仅响应逐步推进）")
                .addAnnotation(AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", "/sessions").build())
                .addParameter(ParameterSpec.builder(PlanModel.mapOfObjects(), "startParams")
                        .addAnnotation(AnnotationSpec.builder(REQUEST_BODY).addMember("required", "false").build()).build())
                .returns(PlanModel.resultOf(PlanModel.STRING_TYPE))
                .addCode("""
                        boolean startPaused = startParams != null && Boolean.TRUE.equals(startParams.get("startPaused"));
                        Long seed = startParams == null || startParams.get("seed") == null ? null
                                : $T.toLong(startParams.get("seed"));
                        return $T.ok(sessionManager.startSession(moduleRuntime, $S, startParams, seed, startPaused));
                        """, PlanModel.VALUES, PlanModel.RESULT, scene.sceneCode()).build());
        // GET /sessions
        cb.addMethod(operation("listSessions", "列出本场景在途会话摘要")
                .addAnnotation(AnnotationSpec.builder(GET_MAPPING).addMember("value", "$S", "/sessions").build())
                .returns(ParameterizedTypeName.get(PlanModel.RESULT, PlanModel.listOf(summaryCn)))
                .addStatement("return $T.ok(sessionManager.listSessions(moduleRuntime.modelCode()).stream()"
                                + ".filter(s -> $S.equals(s.sceneCode())).toList())", PlanModel.RESULT, scene.sceneCode()).build());
        // POST /sessions/{id}/step
        cb.addMethod(operation("stepSession", "精确推进 N 个仿真步进并停在步末，返回步进边界不可变快照")
                .addAnnotation(AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", "/sessions/{sessionId}/step").build())
                .addParameter(ParameterSpec.builder(String.class, "sessionId").addAnnotation(PATH_VARIABLE).build())
                .addParameter(ParameterSpec.builder(TypeName.INT, "steps")
                        .addAnnotation(AnnotationSpec.builder(REQUEST_PARAM).addMember("defaultValue", "$S", "1").build()).build())
                .returns(PlanModel.resultOf(PlanModel.SNAPSHOT))
                .addStatement("return $T.ok(sessionManager.stepSession(sessionId, steps))", PlanModel.RESULT).build());
        // GET snapshot
        cb.addMethod(operation("getSnapshot", "获取步进边界一致性快照（实例、在途过程、近期事件）")
                .addAnnotation(AnnotationSpec.builder(GET_MAPPING).addMember("value", "$S", "/sessions/{sessionId}/snapshot").build())
                .addParameter(ParameterSpec.builder(String.class, "sessionId").addAnnotation(PATH_VARIABLE).build())
                .returns(PlanModel.resultOf(PlanModel.SNAPSHOT))
                .addStatement("return $T.ok(sessionManager.getSnapshot(sessionId))", PlanModel.RESULT).build());
        // speed / pause / resume / stop
        cb.addMethod(operation("setSpeed", "设置期望倍速（孪生过程运行期间由底座强制锁 1x）")
                .addAnnotation(AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", "/sessions/{sessionId}/speed/{speed}").build())
                .addParameter(ParameterSpec.builder(String.class, "sessionId").addAnnotation(PATH_VARIABLE).build())
                .addParameter(ParameterSpec.builder(double.class, "speed").addAnnotation(PATH_VARIABLE).build())
                .returns(PlanModel.resultOf(ClassName.get(Void.class)))
                .addStatement("sessionManager.setSpeed(sessionId, speed)").addStatement("return $T.ok(null)", PlanModel.RESULT).build());
        cb.addMethod(operation("pause", "暂停推演")
                .addAnnotation(AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", "/sessions/{sessionId}/pause").build())
                .addParameter(ParameterSpec.builder(String.class, "sessionId").addAnnotation(PATH_VARIABLE).build())
                .returns(PlanModel.resultOf(ClassName.get(Void.class)))
                .addStatement("sessionManager.pause(sessionId)").addStatement("return $T.ok(null)", PlanModel.RESULT).build());
        cb.addMethod(operation("resume", "恢复实时推进")
                .addAnnotation(AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", "/sessions/{sessionId}/resume").build())
                .addParameter(ParameterSpec.builder(String.class, "sessionId").addAnnotation(PATH_VARIABLE).build())
                .returns(PlanModel.resultOf(ClassName.get(Void.class)))
                .addStatement("sessionManager.resume(sessionId)").addStatement("return $T.ok(null)", PlanModel.RESULT).build());
        cb.addMethod(operation("stopSession", "结束并移除推演会话")
                .addAnnotation(AnnotationSpec.builder(DELETE_MAPPING).addMember("value", "$S", "/sessions/{sessionId}").build())
                .addParameter(ParameterSpec.builder(String.class, "sessionId").addAnnotation(PATH_VARIABLE).build())
                .returns(PlanModel.resultOf(ClassName.get(Void.class)))
                .addStatement("sessionManager.stopSession(sessionId)").addStatement("return $T.ok(null)", PlanModel.RESULT).build());
        return JavaFile.builder(scene.controllerCn().packageName(), cb.build()).indent("    ").build();
    }

    private static MethodSpec.Builder operation(String name, String summary) {
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(PlanModel.OPERATION).addMember("summary", "$S", summary).build());
    }

    // ------------------------------------------------------------------ 模块装配

    /** 把本模型的运行时入口登记为 Spring Bean（控制器据此拿到同一个运行时实例） */
    public JavaFile emitConfiguration() {
        ClassName cn = ClassName.get(model.basePkg + ".config", model.runtimeSimple.replace("ModuleRuntime", "") + "ModuleConfiguration");
        TypeSpec config = TypeSpec.classBuilder(cn).addModifiers(Modifier.PUBLIC).addAnnotation(CONFIGURATION)
                .addJavadoc("模型「$L」装配：登记运行时入口单例（Bean 名由宿主的全限定名生成器隔离，多模型零冲突）\n", model.modelLabel)
                .addMethod(MethodSpec.methodBuilder("moduleRuntime").addAnnotation(BEAN).addModifiers(Modifier.PUBLIC)
                        .returns(PlanModel.MODULE_RUNTIME)
                        .addStatement("return new $T()", model.runtimeCn).build())
                .build();
        return JavaFile.builder(cn.packageName(), config).indent("    ").build();
    }
}

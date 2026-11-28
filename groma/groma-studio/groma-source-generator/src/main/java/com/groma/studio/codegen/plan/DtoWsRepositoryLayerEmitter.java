package com.groma.studio.codegen.plan;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * 补齐设计说明书第 1 章标准分层契约（code4.html M6 收割）：每个场景生成
 * {@code model/dto/{Scene}StartDTO}（强类型启动参数）、{@code ws/{Scene}WsChannel}
 * （WebSocket 频道路由契约）与 {@code repository/{Scene}SnapshotRepository}
 * （会话快照内存仓储，数据库适配层预留扩展点）。
 */
public final class DtoWsRepositoryLayerEmitter {

    private final PlanModel model;

    public DtoWsRepositoryLayerEmitter(PlanModel model) {
        this.model = model;
    }

    /** {Scene}StartDTO 简名（与控制器同前缀） */
    public static String startDtoSimple(PlanModel.SceneInfo scene) {
        return scene.controllerSimple().replace("Controller", "") + "StartDTO";
    }

    /** {Scene}WsChannel 简名 */
    public static String wsChannelSimple(PlanModel.SceneInfo scene) {
        return scene.controllerSimple().replace("Controller", "") + "WsChannel";
    }

    /** {Scene}SnapshotRepository 简名 */
    public static String snapshotRepositorySimple(PlanModel.SceneInfo scene) {
        return scene.controllerSimple().replace("Controller", "") + "SnapshotRepository";
    }

    /** 全部场景的 dto / ws / repository 三层产物 */
    public List<JavaFile> emitAll() {
        java.util.List<JavaFile> files = new java.util.ArrayList<>();
        for (PlanModel.SceneInfo scene : model.scenes) {
            files.add(emitStartDto(scene));
            files.add(emitWsChannel(scene));
            files.add(emitSnapshotRepository(scene));
        }
        return files;
    }

    private JavaFile emitStartDto(PlanModel.SceneInfo scene) {
        String dtoPkg = model.basePkg + ".model.dto";
        TypeName mapOfObjects = PlanModel.mapOfObjects();
        ClassName linkedMap = ClassName.get(java.util.LinkedHashMap.class);
        TypeSpec.Builder dto = TypeSpec.classBuilder(ClassName.get(dtoPkg, startDtoSimple(scene)))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("$L 场景启动参数 DTO（设计说明书 3.1 跨层三件套收口）：强类型 seed / startPaused，\n"
                        + "动态想定覆盖键值归入 initialOverrides（键语义与 REST 启动参数完全一致）。\n", scene.zhName())
                .addAnnotation(AnnotationSpec.builder(PlanModel.GROMA_META)
                        .addMember("id", "$S", scene.type().typeId())
                        .addMember("zhName", "$S", scene.zhName() + "启动参数")
                        .addMember("kind", "$S", "SCENE_START_DTO")
                        .build());
        dto.addField(FieldSpec.builder(ClassName.get(Long.class), "seed", Modifier.PRIVATE).build());
        dto.addField(FieldSpec.builder(ClassName.get(Boolean.class), "startPaused", Modifier.PRIVATE).build());
        dto.addField(FieldSpec.builder(mapOfObjects, "initialOverrides", Modifier.PRIVATE)
                .initializer("new $T<>(16)", linkedMap).build());
        dto.addMethod(MethodSpec.methodBuilder("fromMap").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(dtoPkg, startDtoSimple(scene)))
                .addParameter(PlanModel.mapOfObjects(), "startParams")
                .addJavadoc("REST Map 启动参数 → 强类型 DTO（线格式兼容既有契约；seed/startPaused 提升为字段）\n")
                .addStatement("$T dto = new $T()", ClassName.get(dtoPkg, startDtoSimple(scene)),
                        ClassName.get(dtoPkg, startDtoSimple(scene)))
                .addStatement("dto.seed = startParams == null || startParams.get($S) == null ? null\n"
                        + ": (long) $T.toLong(startParams.get($S))", "seed", PlanModel.VALUES, "seed")
                .addStatement("dto.startPaused = startParams != null && $T.TRUE.equals(startParams.get($S))",
                        Boolean.class, "startPaused")
                .beginControlFlow("if (startParams != null)")
                .addStatement("dto.initialOverrides.putAll(startParams)")
                .endControlFlow()
                .addStatement("return dto")
                .build());
        dto.addMethod(beanGetter("getSeed", ClassName.get(Long.class), "seed"));
        dto.addMethod(beanSetter("setSeed", ClassName.get(Long.class), "seed"));
        dto.addMethod(beanGetter("getStartPaused", ClassName.get(Boolean.class), "startPaused"));
        dto.addMethod(beanSetter("setStartPaused", ClassName.get(Boolean.class), "startPaused"));
        dto.addMethod(beanGetter("getInitialOverrides", mapOfObjects, "initialOverrides"));
        dto.addMethod(beanSetter("setInitialOverrides", mapOfObjects, "initialOverrides"));
        return JavaFile.builder(dtoPkg, dto.build()).indent("    ").build();
    }

    private JavaFile emitWsChannel(PlanModel.SceneInfo scene) {
        String wsPkg = model.basePkg + ".ws";
        String pathPrefix = "/ws/groma/" + model.modelCode + "/" + scene.sceneCode() + "/";
        TypeSpec channel = TypeSpec.classBuilder(ClassName.get(wsPkg, wsChannelSimple(scene)))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("$L 场景 WebSocket 频道路由契约（设计说明书 1.x：groma-base-websocket 统一路由\n"
                        + "/ws/groma/{modelCode}/{sceneCode}/{sessionId}，本类为该场景的静态路由元数据）。\n", scene.zhName())
                .addField(FieldSpec.builder(String.class, "MODEL_CODE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", model.modelCode).build())
                .addField(FieldSpec.builder(String.class, "SCENE_CODE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", scene.sceneCode()).build())
                .addField(FieldSpec.builder(String.class, "WS_PATH_TEMPLATE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", pathPrefix + "{sessionId}").build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addMethod(MethodSpec.methodBuilder("channelPath").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(String.class).addParameter(String.class, "sessionId")
                        .addJavadoc("该会话的实时态势频道路由（前端 Three.js/PixiJS 视口直连地址）\n")
                        .addStatement("return $S + sessionId", pathPrefix).build())
                .build();
        return JavaFile.builder(wsPkg, channel).indent("    ").build();
    }

    private JavaFile emitSnapshotRepository(PlanModel.SceneInfo scene) {
        String repoPkg = model.basePkg + ".repository";
        TypeName snapshotMap = ParameterizedTypeName.get(ClassName.get(java.util.Map.class),
                ClassName.get(String.class), PlanModel.SNAPSHOT);
        TypeSpec repo = TypeSpec.classBuilder(ClassName.get(repoPkg, snapshotRepositorySimple(scene)))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("$L 场景会话快照内存仓储（设计说明书标准五层 repository 落点）：\n"
                        + "步进边界快照的进程内存取；数据库五层逆向生成（说明书 M5~M6）时以同签名 JdbcClient\n"
                        + "实现替换本内存版，控制器与业务代码零改动。\n", scene.zhName())
                .addField(FieldSpec.builder(snapshotMap, "snapshots", Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get(java.util.concurrent.ConcurrentHashMap.class)).build())
                .addMethod(MethodSpec.methodBuilder("save").addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "sessionId").addParameter(PlanModel.SNAPSHOT, "snapshot")
                        .addStatement("snapshots.put(sessionId, snapshot)").build())
                .addMethod(MethodSpec.methodBuilder("find").addModifiers(Modifier.PUBLIC)
                        .returns(PlanModel.SNAPSHOT).addParameter(String.class, "sessionId")
                        .addStatement("return snapshots.get(sessionId)").build())
                .addMethod(MethodSpec.methodBuilder("remove").addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "sessionId")
                        .addStatement("snapshots.remove(sessionId)").build())
                .build();
        return JavaFile.builder(repoPkg, repo).indent("    ").build();
    }

    private static MethodSpec beanGetter(String name, TypeName type, String field) {
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC).returns(type)
                .addStatement("return this." + field).build();
    }

    private static MethodSpec beanSetter(String name, TypeName type, String field) {
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC).addParameter(type, field)
                .addStatement("this." + field + " = " + field).build();
    }
}

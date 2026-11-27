package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.PlanModel.InstanceInfo;
import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.SceneInfo;
import com.groma.studio.codegen.plan.PlanModel.SceneMember;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.ValueSpec;
import com.groma.studio.ir.GromaTypes;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 第 3 层（Engine 场景单写者沙盒层）生成器：每个运行场景生成一个 {@code *WorldContext}。
 * 初始世界由“对象实体”命名实例对象图组成：实例在首次被引用时构建并登记（成环受保护），
 * 场景对象的成员按画布绑定指向命名实例；对象登记后由底座启动其类型的默认启动过程。
 */
public final class WorldContextEmitter {

    private final PlanModel model;
    /** 需要多条语句的内联对象 / 映射的私有构建方法，全部收集后在类末尾统一追加 */
    private final List<MethodSpec> helpers = new ArrayList<>();
    private int inlineSeq;

    public WorldContextEmitter(PlanModel model) {
        this.model = model;
    }

    public JavaFile emit(SceneInfo scene) {
        TypeInfo type = scene.type();
        TypeSpec.Builder cb = TypeSpec.classBuilder(scene.contextCn())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(PlanModel.WORLD_CTX)
                .addJavadoc("场景单写者推演沙盒：$L（方圆场景原名：$L）\n<p>\n严格绑定单线程无锁推进：世界初始化按画布绑定构建命名实例对象图，\n"
                        + "随后由底座按事件接线表派发对象事件、逐拍推进耗时过程。成员的每次写入都会经底座发出标准事件码。\n</p>\n",
                        scene.zhName(), scene.zhName())
                .addAnnotation(ProcessTaskEmitter.meta(type.typeId(), scene.zhName(), "SCENE_CONTEXT"));
        cb.addField(FieldSpec.builder(type.entity(), "self", Modifier.PRIVATE).build());

        cb.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addJavadoc("@param initialOverrides 启动参数：{@code 实例ID.成员名} 定点覆盖；{@code 成员名} 覆盖场景根对象成员"
                        + "（初始化静默期内应用，不产生值变化事件）\n")
                .addParameter(PlanModel.MODULE_RUNTIME, "runtime").addParameter(String.class, "sessionId")
                .addParameter(long.class, "startEpochMs").addParameter(long.class, "finalStepMs")
                .addParameter(long.class, "seed").addParameter(PlanModel.mapOfObjects(), "initialOverrides")
                .addStatement("super(runtime, sessionId, $S, startEpochMs, finalStepMs, seed)", scene.sceneCode())
                .addStatement("setInitialOverrides(initialOverrides)")
                .build());

        // 场景根对象：裸成员名覆盖键的作用目标（基类 applyInitialOverrides 读取）
        cb.addMethod(MethodSpec.methodBuilder("sceneRootEntity").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(PlanModel.BASE_ENTITY)
                .addStatement("return self")
                .build());

        // buildInitialWorld：场景对象 + 按画布绑定的成员取值
        MethodSpec.Builder init = MethodSpec.methodBuilder("buildInitialWorld").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);
        init.addStatement("this.self = new $T($S)", type.entity(), "scene:" + type.typeId());
        for (SceneMember member : scene.members()) {
            CodeBlock value = value(member.member().placeholder(), member.value());
            init.addCode("// $L = $L\n", member.member().zhName(), describe(member.value()));
            init.addStatement("this.self.$L($L, this)", member.member().setterCtx(), value);
            if (member.member().isRef() && member.value() instanceof CompilePlanIR.RefSpec ref) {
                init.addStatement("addRootMember($S, this.self.$L())", member.member().tokenId(), member.member().getter());
            }
        }
        init.addStatement("register(this.self)");
        cb.addMethod(init.build());

        // newNamedInstance：按实例标识分派到构建方法
        MethodSpec.Builder named = MethodSpec.methodBuilder("newNamedInstance").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(PlanModel.BASE_ENTITY).addParameter(String.class, "instanceId");
        List<InstanceInfo> objectInstances = new ArrayList<>();
        List<InstanceInfo> collectionInstances = new ArrayList<>();
        for (InstanceInfo instance : model.instances) {
            if (instance.spec() instanceof CompilePlanIR.ObjectSpec) {
                objectInstances.add(instance);
            } else if (instance.spec() instanceof CompilePlanIR.ListSpec || instance.spec() instanceof CompilePlanIR.MapSpec) {
                collectionInstances.add(instance);
            }
        }
        if (objectInstances.isEmpty()) {
            named.addStatement("return null");
        } else {
            named.beginControlFlow("switch (instanceId)");
            for (InstanceInfo instance : objectInstances) {
                named.beginControlFlow("case $S ->", instance.instanceId());
                named.addStatement("return $L()", instance.builderMethod());
                named.endControlFlow();
            }
            named.addCode("default -> { return null; }\n");
            named.endControlFlow();
        }
        cb.addMethod(named.build());

        // newNamedCollection：数组 / 映射实例按实例标识分派到构建方法
        MethodSpec.Builder collections = MethodSpec.methodBuilder("newNamedCollection").addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED).returns(Object.class).addParameter(String.class, "instanceId");
        if (collectionInstances.isEmpty()) {
            collections.addStatement("return null");
        } else {
            collections.beginControlFlow("switch (instanceId)");
            for (InstanceInfo instance : collectionInstances) {
                collections.beginControlFlow("case $S ->", instance.instanceId());
                collections.addStatement("return $L()", instance.builderMethod());
                collections.endControlFlow();
            }
            collections.addCode("default -> { return null; }\n");
            collections.endControlFlow();
        }
        cb.addMethod(collections.build());

        // 命名实例构建方法
        for (InstanceInfo instance : objectInstances) {
            cb.addMethod(emitInstanceBuilder(instance));
        }
        for (InstanceInfo instance : collectionInstances) {
            cb.addMethod(emitCollectionBuilder(instance));
        }
        for (MethodSpec helper : helpers) {
            cb.addMethod(helper);
        }
        return JavaFile.builder(scene.contextCn().packageName(), cb.build()).indent("    ").build();
    }

    /** 命名实例构建方法：按对象图逐成员赋值，引用其他命名实例时经 namedInstance 惰性构建 */
    private MethodSpec emitInstanceBuilder(InstanceInfo instance) {
        CompilePlanIR.ObjectSpec object = (CompilePlanIR.ObjectSpec) instance.spec();
        TypeInfo type = instance.type();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(instance.builderMethod()).addModifiers(Modifier.PRIVATE)
                .returns(type.entity())
                .addJavadoc("命名实例：$L（方圆对象实体原名：$L；实例标识 $L）\n", instance.zhName(), instance.zhName(),
                        instance.instanceId());
        builder.addStatement("$T entity = new $T($S)", type.entity(), type.entity(), instance.instanceId());
        for (MemberInfo member : type.allMembers()) {
            ValueSpec spec = object.members().get(member.tokenId());
            if (spec == null || spec instanceof CompilePlanIR.NullSpec) {
                continue;
            }
            CodeBlock value = value(member.placeholder(), spec);
            builder.addCode("// $L = $L\n", member.zhName(), describe(spec));
            if (member.setterPlain() != null) {
                builder.addStatement("entity.$L($L)", member.setterPlain(), value);
            } else {
                builder.addStatement("entity.$L($L, this)", member.setterCtx(), value);
            }
        }
        builder.addStatement("return entity");
        return builder.build();
    }

    /** 命名集合实例构建方法：数组按元素序号、映射按键，元素为引用其他命名实例或内联值 */
    private MethodSpec emitCollectionBuilder(InstanceInfo instance) {
        TypeInfo type = instance.type();
        boolean map = instance.spec() instanceof CompilePlanIR.MapSpec;
        String placeholder = map
                ? GromaTypes.map(GromaTypes.STRING, GromaTypes.entity(type.typeId()))
                : GromaTypes.list(GromaTypes.entity(type.typeId()));
        TypeName javaType = model.resolve(placeholder);
        MethodSpec.Builder builder = MethodSpec.methodBuilder(instance.builderMethod()).addModifiers(Modifier.PRIVATE)
                .returns(javaType)
                .addJavadoc("命名集合实例：$L（方圆对象实体原名：$L；实例标识 $L）\n", instance.zhName(), instance.zhName(),
                        instance.instanceId());
        builder.addStatement("return $L", value(placeholder, instance.spec()));
        return builder.build();
    }

    // ------------------------------------------------------------------ 取值

    private String describe(ValueSpec spec) {
        if (spec == null || spec instanceof CompilePlanIR.NullSpec) {
            return "未赋值";
        }
        if (spec instanceof CompilePlanIR.ScalarSpec scalar) {
            return scalar.rawValue();
        }
        if (spec instanceof CompilePlanIR.RefSpec ref) {
            for (InstanceInfo instance : model.instances) {
                if (instance.instanceId().equals(ref.instanceId())) {
                    return "引用命名实例 " + instance.zhName();
                }
            }
            return "引用命名实例 " + ref.instanceId();
        }
        if (spec instanceof CompilePlanIR.ListSpec list) {
            return "集合，" + list.items().size() + " 个元素";
        }
        if (spec instanceof CompilePlanIR.MapSpec map) {
            return "映射，" + map.entries().size() + " 个键值对";
        }
        return "内联对象";
    }

    private static String kindOf(String placeholder) {
        if (GromaTypes.isMap(placeholder)) {
            return PlanModel.KIND_MAP;
        }
        if (GromaTypes.isList(placeholder)) {
            return PlanModel.KIND_LIST;
        }
        if (GromaTypes.isEnum(placeholder)) {
            return PlanModel.KIND_ENUM;
        }
        if (GromaTypes.isEntity(placeholder)) {
            return PlanModel.KIND_REF;
        }
        return PlanModel.KIND_BASE;
    }

    private static String mapKeyPlaceholder(String placeholder) {
        String body = placeholder.substring(GromaTypes.PREFIX_MAP.length());
        int sep = body.indexOf(':');
        return sep < 0 ? GromaTypes.STRING : body.substring(0, sep);
    }

    /** 实例成员取值 → 表达式（需要多条语句的内联对象 / 非空映射落到私有构建方法） */
    private CodeBlock value(String placeholder, ValueSpec spec) {
        TypeName type = model.resolve(placeholder);
        String kind = kindOf(placeholder);
        if (spec == null || spec instanceof CompilePlanIR.NullSpec) {
            return CodeBlock.of(model.defaultLiteral(type, kind));
        }
        if (spec instanceof CompilePlanIR.ScalarSpec scalar) {
            return model.literalCode(type, kind, scalar.rawValue());
        }
        if (spec instanceof CompilePlanIR.RefSpec ref) {
            TypeInfo target = model.types.get(ref.typeId());
            TypeName cast = PlanModel.isEntity(type) ? type : (target == null ? type : target.entity());
            return CodeBlock.of("($T) namedInstance($S)", cast, ref.instanceId());
        }
        if (spec instanceof CompilePlanIR.ListSpec list) {
            if (list.items().isEmpty()) {
                return CodeBlock.of(model.defaultLiteral(type, kind));
            }
            String element = GromaTypes.elementTypeOf(placeholder);
            CodeBlock.Builder items = CodeBlock.builder();
            for (int i = 0; i < list.items().size(); i++) {
                items.add(i == 0 ? "" : ", ").add(value(element, list.items().get(i)));
            }
            return CodeBlock.of("new java.util.ArrayList<>(java.util.List.of($L))", items.build());
        }
        if (spec instanceof CompilePlanIR.MapSpec map && map.entries().isEmpty()) {
            return CodeBlock.of(model.defaultLiteral(type, kind));
        }
        // 非空映射与内联对象：交给私有构建方法
        String helperName = "newInline" + (inlineSeq++);
        MethodSpec.Builder helper = MethodSpec.methodBuilder(helperName).addModifiers(Modifier.PRIVATE).returns(type);
        if (spec instanceof CompilePlanIR.MapSpec map) {
            String key = mapKeyPlaceholder(placeholder);
            String valueType = GromaTypes.elementTypeOf(placeholder);
            helper.addStatement("$T value = new java.util.LinkedHashMap<>()", type);
            for (Map.Entry<String, ValueSpec> entry : map.entries().entrySet()) {
                helper.addStatement("value.put($L, $L)", value(key, new CompilePlanIR.ScalarSpec(entry.getKey(), "string")),
                        value(valueType, entry.getValue()));
            }
        } else if (spec instanceof CompilePlanIR.ObjectSpec object) {
            TypeInfo info = model.types.get(object.typeId());
            TypeName targetType = info == null ? type : info.entity();
            helper.addStatement("$T value = new $T(nextInstanceId($S))", targetType, targetType,
                    info == null ? "Inline" : info.code());
            if (info != null) {
                for (MemberInfo member : info.allMembers()) {
                    ValueSpec memberSpec = object.members().get(member.tokenId());
                    if (memberSpec == null || memberSpec instanceof CompilePlanIR.NullSpec) {
                        continue;
                    }
                    CodeBlock memberValue = value(member.placeholder(), memberSpec);
                    if (member.setterPlain() != null) {
                        helper.addStatement("value.$L($L)", member.setterPlain(), memberValue);
                    } else {
                        helper.addStatement("value.$L($L, this)", member.setterCtx(), memberValue);
                    }
                }
            }
            helper.addStatement("register(value)");
        }
        helper.addStatement("return value");
        helpers.add(helper.build());
        return CodeBlock.of("$L()", helperName);
    }
}

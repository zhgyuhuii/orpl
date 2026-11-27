package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.PlanModel.EnumInfo;
import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.TriggerIR;
import com.groma.studio.ir.GromaModuleIR;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 第 5 层（Model 数据模型层）生成器：枚举、定值常量池、领域实体、只读输入契约、展示 VO 与零反射转换器。
 * <p>
 * 三级中文语义无损保留（设计书 4.4 节）在这里落实：
 * 类与成员的 Javadoc 写明方圆中文原名、单位、取值范围、默认值、初始状态与事件触发关系；
 * 类、字段上挂 {@code @GromaMeta}（RUNTIME 保留，编译进 .class）与 OpenAPI {@code @Schema}。
 * </p>
 */
public final class ModelLayerEmitter {

    private final PlanModel model;

    public ModelLayerEmitter(PlanModel model) {
        this.model = model;
    }

    // ------------------------------------------------------------------ 枚举

    public List<JavaFile> emitEnums() {
        List<JavaFile> files = new ArrayList<>();
        for (EnumInfo info : model.enums.values()) {
            TypeSpec.Builder eb = TypeSpec.enumBuilder(info.cn())
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("方圆枚举类型：$L（状态顺序与画布一致；默认初始状态：$L）\n", info.zhName(),
                            nvl(info.initialLabel()))
                    .addAnnotation(ProcessTaskEmitter.meta(info.enumId(), info.zhName(), "ENUM"))
                    .addField(String.class, "zhLabel", Modifier.PRIVATE, Modifier.FINAL);
            eb.addMethod(MethodSpec.constructorBuilder().addParameter(String.class, "zhLabel")
                    .addStatement("this.zhLabel = zhLabel").build());
            eb.addMethod(MethodSpec.methodBuilder("getZhLabel").addModifiers(Modifier.PUBLIC)
                    .addJavadoc("状态中文原名\n").returns(String.class).addStatement("return this.zhLabel").build());
            for (var entry : info.constants().entrySet()) {
                eb.addEnumConstant(entry.getValue(), TypeSpec.anonymousClassBuilder("$S", entry.getKey())
                        .addJavadoc("$L", entry.getKey()).build());
            }
            files.add(JavaFile.builder(info.cn().packageName(), eb.build()).indent("    ").build());
        }
        return files;
    }

    // ------------------------------------------------------------------ 定值常量

    public JavaFile emitConstants() {
        ClassName cn = ClassName.get(model.basePkg + ".model.enums", "ModelConstants");
        TypeSpec.Builder cb = TypeSpec.classBuilder(cn).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("模型全局定值常量池（消除魔法值；每个常量保留方圆原名）\n")
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        Set<String> used = new HashSet<>();
        for (GromaModuleIR.ConstantIR constant : model.ir.constants()) {
            String base = model.upperSnake(constant.zhName());
            String name = base;
            int seq = 2;
            while (!used.add(name)) {
                name = base + "_" + seq++;
            }
            String placeholder = PlanModel.placeholderOf(constant.fieldType());
            TypeName type = model.resolve(placeholder);
            String kind = PlanModel.KIND_BASE;
            FieldSpec.Builder field = FieldSpec.builder(type, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addJavadoc("定值（方圆原名：$L）$L\n", constant.zhName(),
                            constant.unit().isEmpty() ? "" : " 单位：" + constant.unit())
                    .addAnnotation(ProcessTaskEmitter.meta(constant.entityId(), constant.zhName(), "CONSTANT"))
                    .initializer(model.literalCode(type, kind, constant.defaultValue()));
            cb.addField(field.build());
        }
        return JavaFile.builder(cn.packageName(), cb.build()).indent("    ").build();
    }

    // ------------------------------------------------------------------ 领域实体

    public JavaFile emitEntity(TypeInfo type) {
        ClassName cn = type.entity();
        TypeSpec.Builder tb = TypeSpec.classBuilder(cn).addModifiers(Modifier.PUBLIC)
                .addJavadoc(entityJavadoc(type))
                .addAnnotation(AnnotationSpec.builder(PlanModel.SCHEMA)
                        .addMember("title", "$S", type.zhName())
                        .addMember("description", "$S", "方圆对象类型：" + type.zhName()).build())
                .addAnnotation(ProcessTaskEmitter.meta(type.typeId(), type.zhName(), "TYPE"));
        if (type.parent() != null) {
            tb.superclass(type.parent().entity());
        } else {
            tb.superclass(PlanModel.BASE_ENTITY);
        }
        tb.addSuperinterface(type.readOnly());

        // 成员元数据表（零反射成员读写、快照与外部干预都依赖它）
        CodeBlock.Builder metas = CodeBlock.builder().add("$T.of(", List.class);
        boolean firstMeta = true;
        for (MemberInfo member : type.allMembers()) {
            metas.add(firstMeta ? "" : ", ").add("new $T($S, $S, $S, $S, $S, $S)", PlanModel.MEMBER_META,
                    member.tokenId(), member.zhName(), member.field(), member.kind(), member.type().toString(),
                    nvl(member.unit()));
            firstMeta = false;
        }
        // R01 副父类成员也进元数据表（setInstanceValue / 快照导出按成员标识寻址走这张表）
        Map<String, String> mixinRoutes = mixinTokenRoutes(model, type);
        for (Map.Entry<String, String> route : mixinRoutes.entrySet()) {
            MemberInfo mixinOwned = mixinMember(model, type, route.getKey());
            if (mixinOwned == null) {
                continue;
            }
            metas.add(firstMeta ? "" : ", ").add("new $T($S, $S, $S, $S, $S, $S)", PlanModel.MEMBER_META,
                    mixinOwned.tokenId(), mixinOwned.zhName(), mixinOwned.field(), mixinOwned.kind(),
                    mixinOwned.type().toString(), nvl(mixinOwned.unit()));
            firstMeta = false;
        }
        metas.add(")");
        tb.addField(FieldSpec.builder(PlanModel.listOf(PlanModel.MEMBER_META), "MEMBERS",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer(metas.build()).build());

        for (MemberInfo member : type.ownMembers()) {
            // 有子类的实体字段放宽为 protected：子类 getMemberValue/setMemberValue 的开关表按 allMembers
            // （含继承成员）直接以 this.字段 访问，private 父字段对子类不可见（模型 2 继承实证）
            Modifier fieldVisibility = hasSubclass(type) ? Modifier.PROTECTED : Modifier.PRIVATE;
            tb.addField(FieldSpec.builder(member.type(), member.field(), fieldVisibility)
                    .addJavadoc(memberJavadoc(type, member))
                    .addAnnotation(AnnotationSpec.builder(PlanModel.SCHEMA).addMember("title", "$S", member.zhName()).build())
                    .addAnnotation(memberMeta(member))
                    .initializer(fieldInitializer(member)).build());
        }

        tb.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addJavadoc("@param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）\n")
                .addParameter(String.class, "instanceId")
                .addStatement("super(instanceId)")
                .addCode(ctorTail(model, type))
                .build());

        // R06 结构控制（收割）：类型带控制规则时生成 refreshControlMask 覆写（门点求值见基类/任务基类）
        emitRefreshControlMask(model, type, tb);

        tb.addMethod(MethodSpec.methodBuilder("getTypeCode").addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(String.class).addStatement("return $S", type.code()).build());
        tb.addMethod(MethodSpec.methodBuilder("getTypeZhName").addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(String.class).addStatement("return $S", type.zhName()).build());
        tb.addMethod(MethodSpec.methodBuilder("memberMetas").addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(PlanModel.listOf(PlanModel.MEMBER_META)).addStatement("return MEMBERS").build());

        for (MemberInfo member : type.ownMembers()) {
            emitAccessors(tb, type, member);
        }
        // R01 副父类（mixin）组合委托（收割）：副父类无法多继承，生成委托实例与桥接读写
        emitMixinDelegation(model, type, tb);
        emitMemberSwitches(tb, type);
        emitCopy(tb, type);
        return JavaFile.builder(cn.packageName(), tb.build()).indent("    ").build();
    }

    /** 委托字段名（R01 副父类组合委托） */
    private static String mixinFieldName(TypeInfo mixin) {
        return "mixin" + mixin.code() + "Entity";
    }

    /**
     * 构造器尾部（R01 副父类委托实例初始化）：委托实体复用宿主实例标识，
     * 使其成员事件在链路上与宿主同源（实例标识一致，快照/WS 按 id 寻址不分裂）。
     */
    private CodeBlock ctorTail(PlanModel model, TypeInfo type) {
        CodeBlock.Builder cb = CodeBlock.builder();
        for (String mixinId : type.mixinIds()) {
            TypeInfo mixin = model.types.get(mixinId);
            if (mixin == null) {
                continue;
            }
            cb.addStatement("this.$L = new $T(instanceId)", mixinFieldName(mixin), mixin.entity());
        }
        return cb.build();
    }

    /**
     * R01 副父类组合委托（收割自 code3.html G1 方案，按真实生成器结构落地）：
     * ReadOnly 接口多继承补齐契约；实体生成副父类实例（含其主继承链），
     * 副父类子树中与主继承链不重叠的成员生成委托 Getter/Setter 桥接——
     * Getter 直转副父类实例，Setter 经副父类带 ctx 写入（事件源实例标识与宿主一致）。
     */
    private void emitMixinDelegation(PlanModel model, TypeInfo type, TypeSpec.Builder tb) {
        if (type.mixinIds().isEmpty()) {
            return;
        }
        java.util.Set<String> primaryTokens = new java.util.HashSet<>();
        for (MemberInfo member : type.allMembers()) {
            primaryTokens.add(member.tokenId());
        }
        for (String mixinId : type.mixinIds()) {
            TypeInfo mixin = model.types.get(mixinId);
            if (mixin == null) {
                continue;
            }
            tb.addSuperinterface(mixin.readOnly());
            tb.addField(FieldSpec.builder(mixin.entity(), mixinFieldName(mixin), Modifier.PRIVATE)
                    .addJavadoc("R01 副父类【$L】组合委托实例（复用宿主实例标识）\n", mixin.zhName())
                    .build());
            for (MemberInfo member : mixin.allMembers()) {
                if (primaryTokens.contains(member.tokenId())) {
                    continue; // 主继承链同名成员优先，避免桥接重复
                }
                primaryTokens.add(member.tokenId());
                tb.addMethod(MethodSpec.methodBuilder(member.getter()).addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC).returns(member.type())
                        .addJavadoc("R01 副父类成员：$L（委托至 $L）\n", member.zhName(), mixin.zhName())
                        .addStatement("return this.$L.$L()", mixinFieldName(mixin), member.getter())
                        .build());
                tb.addMethod(MethodSpec.methodBuilder(member.setterCtx()).addModifiers(Modifier.PUBLIC)
                        .addJavadoc("R01 副父类成员写入：$L（委托至 $L，事件经其带 ctx 写入发出）\n",
                                member.zhName(), mixin.zhName())
                        .addParameter(member.type(), "value").addParameter(PlanModel.WORLD_CTX, "ctx")
                        .addStatement("this.$L.$L(value, ctx)", mixinFieldName(mixin), member.setterCtx())
                        .build());
            }
        }
    }

    /**
     * R06 结构控制（收割自 code3.html G2 方案，按门点求值落地）：按当前枚举成员状态重算
     * 成员/过程使能掩码并写回缓存；过程使能位由 GromaProcessTask.runProcess 推进前消费。
     */
    private void emitRefreshControlMask(PlanModel model, TypeInfo type, TypeSpec.Builder tb) {
        if (type.controls().isEmpty()) {
            return;
        }
        MethodSpec.Builder mb = MethodSpec.methodBuilder("refreshControlMask")
                .addAnnotation(Override.class).addModifiers(Modifier.PROTECTED)
                .addParameter(PlanModel.WORLD_CTX, "ctx")
                .addJavadoc("R06 结构控制掩码求值（门点调用：任务推进前 / 受控成员写入前）\n");
        mb.addStatement("long memberMask = -1L");
        mb.addStatement("long processMask = -1L");
        for (CompilePlanIR.ControlRuleIR rule : type.controls()) {
            MemberInfo source = type.member(rule.sourceEnumMemberId());
            if (source == null || rule.activeStateLabels().isEmpty()) {
                continue;
            }
            String labels = String.join("、", rule.activeStateLabels());
            mb.beginControlFlow("if (!(\"$L\".equals($T.enumZhLabel((Enum<?>) (this.$L())))))",
                    labels, PlanModel.BASE_ENTITY, source.getter());
            if ("PROCESS".equals(rule.targetKind())) {
                mb.addStatement("processMask &= ~(1L << $L)", rule.bitIndex());
            } else {
                mb.addStatement("memberMask &= ~(1L << $L)", rule.bitIndex());
            }
            mb.endControlFlow();
        }
        mb.addStatement("applyControlMasks(memberMask, processMask)");
        tb.addMethod(mb.build());
    }

    /** 该类型是否被其他类型继承（主父类指到它）——决定实体字段可见性 */
    private boolean hasSubclass(TypeInfo type) {
        for (TypeInfo info : model.types.values()) {
            if (info.parent() == type) {
                return true;
            }
        }
        return false;
    }

    private String entityJavadoc(TypeInfo type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.zhName()).append("实体（方圆对象类型原名：").append(type.zhName()).append(type.parent() == null
                ? "" : "；继承自 " + type.parent().zhName()).append("）\n<p>\n");
        sb.append("成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，\n");
        sb.append("并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。\n</p>\n");
        sb.append("成员清单：\n<ul>\n");
        for (MemberInfo member : type.allMembers()) {
            sb.append("<li>").append(member.zhName()).append("（").append(member.field()).append("，")
                    .append(member.kind()).append(nvl2("，单位 ", member.unit())).append("）</li>\n");
        }
        sb.append("</ul>\n");
        List<TriggerIR> triggers = triggersOf(type.typeId());
        if (!triggers.isEmpty()) {
            sb.append("事件接线（类型设计视图）：\n<ul>\n");
            for (TriggerIR trigger : triggers) {
                sb.append("<li>").append(describeTrigger(trigger)).append("</li>\n");
            }
            sb.append("</ul>\n");
        }
        // G9 收割（code3.html）：R27 备注连线采集的建模备注写入类头 Javadoc
        // （parser collectNotes 的文档化意图"出码时写入 Javadoc"此前未落地）
        if (type.notes() != null && !type.notes().isEmpty()) {
            sb.append("画布建模备注（R27）：\n<ul>\n");
            for (String note : type.notes()) {
                sb.append("<li>").append(note.replace("*/", "* /").replace("$", "＄")
                        .replaceAll("\\s*\\r?\\n\\s*", " ")).append("</li>\n");
            }
            sb.append("</ul>\n");
        }
        return sb.toString();
    }

    private String memberJavadoc(TypeInfo type, MemberInfo member) {
        StringBuilder sb = new StringBuilder();
        sb.append(member.zhName()).append("（方圆").append(PlanModel.KIND_REF.equals(member.kind()) ? "对象成员"
                : PlanModel.KIND_ENUM.equals(member.kind()) ? "状态成员" : "属性成员").append("原名：")
                .append(member.zhName()).append("）");
        if (!member.unit().isEmpty()) {
            sb.append(" 单位：").append(member.unit());
        }
        if (!member.defaultValue().isEmpty()) {
            sb.append(" 默认值：").append(member.defaultValue());
        }
        if (PlanModel.KIND_ENUM.equals(member.kind()) && member.initialState() != null && !member.initialState().isEmpty()) {
            sb.append(" 初始状态：").append(member.initialState());
        }
        sb.append("\n");
        for (TriggerIR trigger : triggersOf(type.typeId())) {
            if (trigger.memberToken().equals(member.tokenId())) {
                sb.append("事件触发关系：").append(describeTrigger(trigger)).append("\n");
            }
        }
        return sb.toString();
    }

    private String describeTrigger(TriggerIR trigger) {
        StringBuilder sb = new StringBuilder("当");
        sb.append(model.memberZhName(trigger.memberToken()));
        if (!trigger.stateLabel().isEmpty()) {
            sb.append(" 进入状态「").append(trigger.stateLabel()).append("」");
        }
        sb.append(" 发生【").append(ProcessEventNames.of(trigger.eventCode())).append("】");
        if (!trigger.judge().isEmpty()) {
            sb.append(" 且满足线上判断");
            for (CompilePlanIR.ConditionIR condition : trigger.judge()) {
                sb.append(" ").append(condition.op()).append(" ").append(condition.literal());
            }
        }
        sb.append(" 时，启动过程「").append(procZhName(trigger.targetDefKey())).append("」");
        return sb.toString();
    }

    private String procZhName(String defKey) {
        PlanModel.ProcInfo proc = model.procs.get(defKey);
        return proc == null ? defKey : proc.zhName();
    }

    private List<TriggerIR> triggersOf(String typeId) {
        List<TriggerIR> out = new ArrayList<>();
        for (TriggerIR trigger : model.triggers) {
            if (typeId.equals(trigger.ownerTypeId())) {
                out.add(trigger);
            }
        }
        return out;
    }

    private AnnotationSpec memberMeta(MemberInfo member) {
        AnnotationSpec.Builder ab = AnnotationSpec.builder(PlanModel.GROMA_META)
                .addMember("id", "$S", member.tokenId())
                .addMember("zhName", "$S", member.zhName())
                .addMember("kind", "$S", "FIELD");
        if (!member.unit().isEmpty()) {
            ab.addMember("unit", "$S", member.unit());
        }
        if (PlanModel.KIND_ENUM.equals(member.kind()) && member.initialState() != null && !member.initialState().isEmpty()) {
            ab.addMember("initState", "$S", member.initialState());
        }
        return ab.build();
    }

    private CodeBlock fieldInitializer(MemberInfo member) {
        if (PlanModel.KIND_LIST.equals(member.kind())) {
            // 方案 C：集合字段从创建起就是受观察视图（null 汇 = 创建期静默），ctx 写入方法再接上事件汇
            return CodeBlock.of("new $T<>(new $T<>(16), null)",
                    PlanModel.OBSERVABLE_LIST, java.util.ArrayList.class);
        }
        if (PlanModel.KIND_REF.equals(member.kind()) || PlanModel.KIND_MAP.equals(member.kind())) {
            return CodeBlock.of(model.defaultLiteral(member.type(), member.kind()));
        }
        String raw = PlanModel.KIND_ENUM.equals(member.kind()) ? member.initialState() : member.defaultValue();
        return model.literalCode(member.type(), member.kind(), raw);
    }

    private void emitAccessors(TypeSpec.Builder tb, TypeInfo type, MemberInfo member) {
        // R05 集合代理：代理本类型集合成员的 PROXY 图元实体 id（集合事件汇同步发代理事件）
        List<String> proxyIds = model.proxiesByType.getOrDefault(type.typeId(), List.of()).stream()
                .filter(px -> px.collectionMemberId().equals(member.tokenId()))
                .map(CompilePlanIR.ProxyDefIR::proxyEntityId)
                .toList();
        // R06 成员控制位：指向本成员的 MEMBER 型控制规则位按位或（>0 时 setterCtx 生成门点守卫）
        long memberControlBits = 0L;
        for (CompilePlanIR.ControlRuleIR rule : type.controls()) {
            if ("MEMBER".equals(rule.targetKind()) && rule.targetEntityId().equals(member.tokenId())) {
                memberControlBits |= 1L << rule.bitIndex();
            }
        }
        boolean bool = member.type() == TypeName.BOOLEAN;
        tb.addMethod(MethodSpec.methodBuilder(member.getter()).addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC).returns(member.type())
                .addStatement("return this.$L", member.field()).build());
        // 带事件写入
        MethodSpec.Builder setter = MethodSpec.methodBuilder(member.setterCtx()).addModifiers(Modifier.PUBLIC)
                .addJavadoc("写入$L并发出对应事件（初始化静默期与对象创建期间不发事件）\n", member.zhName())
                .addParameter(member.type(), "value").addParameter(PlanModel.WORLD_CTX, "ctx");
        if (memberControlBits != 0L) {
            // R06 结构控制：成员失能时写入静默忽略（标准 DSM 语义：被切断部件的操作无效）
            setter.addStatement("refreshControlMask(ctx)");
            setter.beginControlFlow("if (!memberEnabled($L))", "0x" + Long.toHexString(memberControlBits) + "L");
            setter.addStatement("return");
            setter.endControlFlow();
        }
        if (String.class.getName().equals(member.type().toString())) {
            setter.beginControlFlow("if (java.util.Objects.equals(this.$L, value))", member.field());
        } else {
            setter.beginControlFlow("if (this.$L == value)", member.field());
        }
        setter.addStatement("return");
        setter.endControlFlow();
        setter.addStatement("$T before = this.$L", member.type(), member.field());
        if (PlanModel.KIND_LIST.equals(member.kind())) {
            // 方案 C：集合写入接入双视角事件汇（持有方 ma/mx + 元素 am/xm）；已是受观察视图则原样保留。
            // R05：本成员被 PROXY 图元代理时，事件汇同步以代理 id 派发同码事件
            if (proxyIds.isEmpty()) {
                setter.addStatement("this.$L = $T.observed(value, ctx.collectionSink(this, $S))",
                        member.field(), PlanModel.OBSERVABLE_LIST, member.tokenId());
            } else {
                setter.addStatement("this.$L = $T.observed(value, ctx.collectionSinkWithProxies(this, $S, $T.of($L)))",
                        member.field(), PlanModel.OBSERVABLE_LIST, member.tokenId(), List.class,
                        CodeBlock.join(proxyIds.stream().map(id -> CodeBlock.of("$S", id)).toList(), ", "));
            }
        } else {
            setter.addStatement("this.$L = value", member.field());
        }
        setter.addStatement("markDirty($L)", member.bit());
        setter.addStatement("ctx.onMemberChanged(this, $S, before, value)", member.tokenId());
        tb.addMethod(setter.build());
        // 无事件写入（初始化、存档回填、对象创建期）
        if (member.setterPlain() != null) {
            tb.addMethod(MethodSpec.methodBuilder(member.setterPlain()).addModifiers(Modifier.PUBLIC)
                    .addJavadoc("直接写入$L（不发事件；仅用于初始化与对象创建期）\n", member.zhName())
                    .addParameter(member.type(), "value")
                    .addStatement("this.$L = value", member.field()).build());
        }
    }

    /** R01 副父类成员 token → 委托字段名（成员读写开关表路由用） */
    private Map<String, String> mixinTokenRoutes(PlanModel model, TypeInfo type) {
        if (type.mixinIds().isEmpty()) {
            return Map.of();
        }
        java.util.Set<String> primaryTokens = new java.util.HashSet<>();
        for (MemberInfo member : type.allMembers()) {
            primaryTokens.add(member.tokenId());
        }
        Map<String, String> routes = new LinkedHashMap<>();
        for (String mixinId : type.mixinIds()) {
            TypeInfo mixin = model.types.get(mixinId);
            if (mixin == null) {
                continue;
            }
            for (MemberInfo member : mixin.allMembers()) {
                if (!primaryTokens.contains(member.tokenId())) {
                    routes.put(member.tokenId(), mixinFieldName(mixin));
                }
            }
        }
        return routes;
    }

    private void emitMemberSwitches(TypeSpec.Builder tb, TypeInfo type) {
        Map<String, String> mixinRoutes = mixinTokenRoutes(model, type);
        MethodSpec.Builder get = MethodSpec.methodBuilder("getMemberValue").addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC).returns(Object.class).addParameter(String.class, "token");
        get.beginControlFlow("switch (token)");
        for (MemberInfo member : type.allMembers()) {
            get.beginControlFlow("case $S ->", member.tokenId());
            get.addStatement("return this.$L", member.field());
            get.endControlFlow();
        }
        for (Map.Entry<String, String> route : mixinRoutes.entrySet()) {
            MemberInfo member = mixinMember(model, type, route.getKey());
            get.beginControlFlow("case $S ->", route.getKey());
            get.addStatement("return this.$L.$L()", route.getValue(), member.getter());
            get.endControlFlow();
        }
        get.addCode("default -> { return $T.MEMBER_NOT_FOUND; }\n", PlanModel.BASE_ENTITY);
        get.endControlFlow();
        tb.addMethod(get.build());

        MethodSpec.Builder set = MethodSpec.methodBuilder("setMemberValue").addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC).returns(TypeName.BOOLEAN)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
                .addParameter(String.class, "token").addParameter(Object.class, "value")
                .addParameter(PlanModel.WORLD_CTX, "ctx");
        set.beginControlFlow("switch (token)");
        for (MemberInfo member : type.allMembers()) {
            set.beginControlFlow("case $S ->", member.tokenId());
            set.addStatement("this.$L($L, ctx)", member.setterCtx(), decodeValue(member));
            set.endControlFlow();
        }
        for (Map.Entry<String, String> route : mixinRoutes.entrySet()) {
            MemberInfo member = mixinMember(model, type, route.getKey());
            set.beginControlFlow("case $S ->", route.getKey());
            set.addStatement("this.$L.$L($L, ctx)", route.getValue(), member.setterCtx(), decodeValue(member));
            set.endControlFlow();
        }
        set.addCode("default -> { return false; }\n");
        set.endControlFlow();
        if (!type.allMembers().isEmpty() || !mixinRoutes.isEmpty()) {
            set.addStatement("return true");
        }
        tb.addMethod(set.build());
    }

    /** 在副父类子树里找成员（开关表路由用；找不到返回 null，路由表与委托生成同源不会失配） */
    private MemberInfo mixinMember(PlanModel model, TypeInfo type, String tokenId) {
        for (String mixinId : type.mixinIds()) {
            TypeInfo mixin = model.types.get(mixinId);
            if (mixin == null) {
                continue;
            }
            for (MemberInfo member : mixin.allMembers()) {
                if (member.tokenId().equals(tokenId)) {
                    return member;
                }
            }
        }
        return null;
    }

    /** 外部传入的原始值 → 成员类型 */
    private CodeBlock decodeValue(MemberInfo member) {
        TypeName type = member.type();
        if (type == TypeName.DOUBLE) {
            return CodeBlock.of("$T.toDouble(value)", PlanModel.VALUES);
        }
        if (type == TypeName.LONG) {
            return CodeBlock.of("$T.toLong(value)", PlanModel.VALUES);
        }
        if (type == TypeName.BOOLEAN) {
            return CodeBlock.of("$T.toBoolean(value)", PlanModel.VALUES);
        }
        if (PlanModel.KIND_ENUM.equals(member.kind())) {
            return CodeBlock.of("value == null ? null : $T.toEnum($T.class, value)", PlanModel.VALUES, type);
        }
        if (String.class.getName().equals(type.toString())) {
            return CodeBlock.of("$T.toText(value)", PlanModel.VALUES);
        }
        return CodeBlock.of("($T) $T.decode(value, ctx)", type, PlanModel.VALUES);
    }

    private void emitCopy(TypeSpec.Builder tb, TypeInfo type) {
        MethodSpec.Builder copy = MethodSpec.methodBuilder("copy").addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC).returns(type.entity()).addParameter(PlanModel.WORLD_CTX, "ctx")
                .addJavadoc("值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界\n");
        copy.addStatement("$T copy = new $T(ctx.nextInstanceId($S))", type.entity(), type.entity(), type.code());
        copy.addStatement("copy.openCreation()");
        for (MemberInfo member : type.allMembers()) {
            if (member.setterPlain() != null) {
                copy.addStatement("copy.$L(this.$L())", member.setterPlain(), member.getter());
            } else {
                // 对象引用成员：仍需登记反向引用，但在副本的创建期内不发事件
                copy.addStatement("copy.$L(this.$L(), ctx)", member.setterCtx(), member.getter());
            }
        }
        copy.addStatement("ctx.register(copy)");
        copy.addStatement("return copy");
        tb.addMethod(copy.build());
    }

    // ------------------------------------------------------------------ 只读输入契约与 VO

    public JavaFile emitReadOnly(TypeInfo type) {
        TypeSpec.Builder ib = TypeSpec.interfaceBuilder(type.readOnly()).addModifiers(Modifier.PUBLIC)
                .addJavadoc("只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取$L，编译期禁止误调写 Setter\n",
                        type.zhName());
        if (type.parent() != null) {
            ib.addSuperinterface(type.parent().readOnly());
        } else {
            ib.addMethod(MethodSpec.methodBuilder("getInstanceId").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(String.class).build());
        }
        for (MemberInfo member : type.ownMembers()) {
            ib.addMethod(MethodSpec.methodBuilder(member.getter()).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("$L\n", member.zhName()).returns(member.type()).build());
        }
        return JavaFile.builder(type.readOnly().packageName(), ib.build()).indent("    ").build();
    }

    public JavaFile emitVo(TypeInfo type) {
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        ctor.addParameter(String.class, "instanceId");
        ctor.addParameter(boolean.class, "alive");
        for (MemberInfo member : type.allMembers()) {
            ctor.addParameter(voType(member), member.field());
        }
        TypeSpec record = TypeSpec.recordBuilder(type.vo()).addModifiers(Modifier.PUBLIC)
                .addJavadoc("对外展示不可变视图 VO（方圆对象类型原名：$L）：对象关联一律扁平化为对端实例标识，杜绝 JSON 循环引用\n",
                        type.zhName())
                .addAnnotation(AnnotationSpec.builder(PlanModel.SCHEMA).addMember("title", "$S", type.zhName()).build())
                .recordConstructor(ctor.build())
                .build();
        return JavaFile.builder(type.vo().packageName(), record).indent("    ").build();
    }

    /** VO 成员类型：对象引用与“对象列表”扁平化为实例标识文本，其余保持成员类型 */
    private TypeName voType(MemberInfo member) {
        if (PlanModel.KIND_REF.equals(member.kind())) {
            return PlanModel.STRING_TYPE;
        }
        if (PlanModel.KIND_LIST.equals(member.kind()) && PlanModel.isEntity(member.elementType())) {
            return PlanModel.listOf(PlanModel.STRING_TYPE);
        }
        return member.type();
    }

    public JavaFile emitConvertUtil() {
        ClassName cn = ClassName.get(model.basePkg + ".utils", "ModelConvertUtil");
        TypeSpec.Builder cb = TypeSpec.classBuilder(cn).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("零反射 Entity -> VO 转换器\n")
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        for (TypeInfo type : model.types.values()) {
            List<CodeBlock> args = new ArrayList<>();
            args.add(CodeBlock.of("entity.getInstanceId()"));
            args.add(CodeBlock.of("entity.isAlive()"));
            for (MemberInfo member : type.allMembers()) {
                String getter = "entity." + member.getter() + "()";
                if (PlanModel.KIND_REF.equals(member.kind())) {
                    args.add(CodeBlock.of("$L == null ? null : $L.getInstanceId()", getter, getter));
                } else if (PlanModel.KIND_LIST.equals(member.kind()) && PlanModel.isEntity(member.elementType())) {
                    args.add(CodeBlock.of("$L == null ? null : $L.stream().map(item -> item instanceof $T ref ? ref.getInstanceId() : String.valueOf(item)).toList()",
                            getter, getter, PlanModel.BASE_ENTITY));
                } else {
                    args.add(CodeBlock.of(getter));
                }
            }
            MethodSpec.Builder toVO = MethodSpec.methodBuilder("to" + type.code() + "VO")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC).returns(type.vo())
                    .addParameter(type.entity(), "entity")
                    .beginControlFlow("if (entity == null)")
                    .addStatement("return null")
                    .endControlFlow();
            CodeBlock.Builder call = CodeBlock.builder();
            for (int i = 0; i < args.size(); i++) {
                call.add(i == 0 ? "$L" : ", $L", args.get(i));
            }
            toVO.addStatement("return new $T($L)", type.vo(), call.build());
            cb.addMethod(toVO.build());
        }
        return JavaFile.builder(cn.packageName(), cb.build()).indent("    ").build();
    }

    private static String nvl(String text) {
        return text == null ? "" : text;
    }

    private static String nvl2(String prefix, String text) {
        return text == null || text.isEmpty() ? "" : prefix + text;
    }

}

package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.OperandRenderer.Value;
import com.groma.studio.codegen.plan.PlanModel.LocalSlot;
import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.ParamSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.codegen.plan.PlanModel.SceneInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.ArgBindingIR;
import com.groma.studio.ir.CompilePlanIR.DefaultStartIR;
import com.groma.studio.ir.CompilePlanIR.ManualStartIR;
import com.groma.studio.ir.CompilePlanIR.PostIR;
import com.groma.studio.ir.CompilePlanIR.TriggerIR;
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
import java.util.List;
import java.util.Map;

/**
 * 模块运行时入口生成器：每个模型子模块生成一个 {@code *ModuleRuntime}（实现底座 GromaModuleRuntime），
 * 在构造时把类型 / 场景设计视图里的全部事件接线与默认启动登记进 {@link com.groma.base.engine.GromaTriggerTable}，
 * 并对外提供场景沙盒、领域实体、过程任务的工厂。宿主的多模型注册中心只依赖这个入口，
 * 所以同一份模型代码既能 Maven 合包，也能经 ECJ 热编译后由隔离类加载器挂载。
 */
public final class ModuleRuntimeEmitter {

    private static final ClassName MODULE_RUNTIME_API = ClassName.get("com.groma.base.engine", "GromaModuleRuntime");
    private static final ClassName SCENE_DESCRIPTOR = MODULE_RUNTIME_API.nestedClass("SceneDescriptor");
    private static final TypeName TYPE_DESCRIPTOR = MODULE_RUNTIME_API.nestedClass("TypeDescriptor");
    private static final ClassName PROCESS_DESCRIPTOR = MODULE_RUNTIME_API.nestedClass("ProcessDescriptor");
    private static final TypeName MAP_OF_OBJECTS = PlanModel.mapOfObjects();

    private final PlanModel model;

    public ModuleRuntimeEmitter(PlanModel model) {
        this.model = model;
    }

    /** 接线启动器里的操作数渲染：宿主对象即 contextEntity */
    static final class StarterScope extends OperandRenderer {
        StarterScope(PlanModel model) {
            super(model, null);
        }

        /**
         * 后置动作钩子里读取刚结束的任务的形参终值（钩子形参名固定为 task）。
         * 调用括号必须写在这个嵌套代码块里：Palantir JavaPoet 对“不含括号的简单表达式”嵌套块后跟的圆括号有吞并行为。
         */
        @Override
        protected CodeBlock readCompletedParam(ProcInfo callee, ParamSlot slot) {
            return CodeBlock.of("task.$L()", slot.getter());
        }

        @Override
        protected CodeBlock ctxRef() {
            return CodeBlock.of("c");
        }

        @Override
        protected CodeBlock selfRef(TypeName selfType) {
            return CodeBlock.of("contextEntity");
        }

        @Override
        protected CodeBlock readParam(ParamSlot slot) {
            throw new IllegalStateException("事件接线上下文没有形参槽位");
        }

        @Override
        protected CodeBlock readLocal(LocalSlot slot) {
            throw new IllegalStateException("事件接线上下文没有局部量槽位");
        }

        @Override
        protected CodeBlock writeLocal(LocalSlot slot, CodeBlock value) {
            throw new IllegalStateException("事件接线上下文没有局部量槽位");
        }

        @Override
        protected CodeBlock namedInstanceRef(String instanceId) {
            return CodeBlock.of("c.namedInstance($S)", instanceId);
        }

        @Override
        protected CodeBlock namedCollectionRef(String instanceId) {
            return CodeBlock.of("c.namedCollection($S)", instanceId);
        }
    }

    /** 接线项：序号在默认启动、事件触发、手动启动与过程级异常流之间连续编号，启动器与线上判断方法按序号命名 */
    private record Wiring(int seq, String typeId, String targetDefKey, List<ArgBindingIR> args,
                          List<PostIR> postActions, boolean defaultStart, boolean manual,
                          String exceptionExitCode, String sourceDefKey) {
        /** 常规接线（默认启动 / 事件触发 / 手动启动） */
        Wiring(int seq, String typeId, String targetDefKey, List<ArgBindingIR> args,
               List<PostIR> postActions, boolean defaultStart, boolean manual) {
            this(seq, typeId, targetDefKey, args, postActions, defaultStart, manual, "", "");
        }
    }

    private List<Wiring> wirings() {
        List<Wiring> out = new ArrayList<>();
        for (DefaultStartIR start : model.defaultStarts) {
            out.add(new Wiring(out.size() + 1, start.typeId(), start.targetDefKey(), start.args(), start.postActions(),
                    true, false));
        }
        for (TriggerIR trigger : model.triggers) {
            out.add(new Wiring(out.size() + 1, trigger.ownerTypeId(), trigger.targetDefKey(), trigger.args(),
                    trigger.postActions(), false, false));
        }
        for (ManualStartIR manual : model.manualStarts) {
            out.add(new Wiring(out.size() + 1, manual.typeId(), manual.targetDefKey(), manual.args(),
                    manual.postActions(), false, true));
        }
        // R25/R26 过程级异常流（sourcePc=-1，code4.html M5 收割）：过程自身 pcd/pca 事件拉起补偿过程。
        // 引擎 emitProcessEvent 以宿主为源、过程定义键为成员令牌派发 pcd/pca，走标准触发表路由。
        for (ProcInfo parent : model.procList) {
            if (parent.def().exceptionFlows() == null || parent.owner() == null) {
                continue;
            }
            for (CompilePlanIR.ExceptionFlowIR flow : parent.def().exceptionFlows()) {
                if (flow.sourcePc() >= 0 || flow.targetDefKey().isEmpty()) {
                    continue;
                }
                String exitCode = "OVERTIME_PCD".equals(flow.eventKind()) ? "pcd" : "pca";
                out.add(new Wiring(out.size() + 1, parent.owner().typeId(), flow.targetDefKey(),
                        List.of(), List.of(), false, false, exitCode, parent.defKey()));
            }
        }
        return out;
    }

    private ManualStartIR manualOf(int seq) {
        return model.manualStarts.get(seq - model.defaultStarts.size() - model.triggers.size() - 1);
    }

    /** 单条接线的登记语句（分片方法体复用；原 ctor 循环体原样搬移） */
    private void emitWiringRegistration(MethodSpec.Builder body, Wiring wiring) {
        ProcInfo target = model.procs.get(wiring.targetDefKey());
        TypeInfo contextType = model.types.get(wiring.typeId());
        if (target == null) {
            model.diagnostic("接线目标过程未注册: " + wiring.targetDefKey());
            return;
        }
        if (contextType == null) {
            model.diagnostic("接线所属类型未知: " + wiring.typeId());
            return;
        }
        if (wiring.manual()) {
            ManualStartIR manual = manualOf(wiring.seq());
            TypeInfo manualType = model.types.get(wiring.typeId());
            body.addCode("// 手动启动点：「$L」（类型 $L；无事件触发线与默认启动，供 startManually 外部干预）\n",
                    target.zhName(), manualType == null ? wiring.typeId() : manualType.zhName());
        } else if (wiring.defaultStart()) {
            DefaultStartIR start = defaultStartOf(wiring.seq());
            body.addCode("// 默认启动：$L 创建后启动「$L」\n", contextType.zhName(), target.zhName());
            body.addStatement("table.onDefaultStart($S, $L, this::start$L, $S)", contextType.code(),
                    listOf(start.hostPath()), wiring.seq(), target.zhName());
        } else if (!wiring.exceptionExitCode().isEmpty()) {
            // R25/R26 过程级异常流（code4.html M5 收割）：监听过程自身 pcd/pca（事件源=宿主，
            // 成员令牌=父过程定义键）拉起补偿过程；节点级（sourcePc>=0）由 ProcessTaskEmitter 路由
            TypeInfo sourceType = model.types.get(wiring.typeId());
            ProcInfo parent = model.procs.get(wiring.sourceDefKey());
            if (target.owner() != null && parent != null && target.owner() != parent.owner()) {
                model.diagnostic("过程级异常流补偿过程「" + target.zhName() + "」的宿主类型与被监控过程"
                        + "「" + parent.zhName() + "」不一致，已跳过（宿主实参无法安全转型）");
                return;
            }
            body.addCode("// 过程级异常流：「$L」自身 $L → 启动补偿过程「$L」\n",
                    parent == null ? wiring.sourceDefKey() : parent.zhName(),
                    wiring.exceptionExitCode(), target.zhName());
            body.addStatement("table.onEvent($S, $S, $S, $S, null, $L, $L, this::start$L, $S)",
                    sourceType == null ? "" : sourceType.code(), wiring.sourceDefKey(),
                    wiring.exceptionExitCode(), "", List.of(), List.of(), wiring.seq(), target.zhName());
        } else {
            TriggerIR trigger = triggerOf(wiring.seq());
            TypeInfo sourceType = model.types.get(trigger.sourceTypeId());
            String sourceCode = sourceType == null ? "" : sourceType.code();
            if (sourceType == null) {
                model.diagnostic("事件接线的源对象类型未知: " + trigger.describe());
            }
            body.addCode("// $L：$L.$L [$L] →「$L」\n",
                    trigger.describe().replaceAll("\\s*\\r?\\n\\s*", " ").replace("$", "＄"),
                    sourceType == null ? "?" : sourceType.zhName(),
                    model.memberZhName(trigger.memberToken()), trigger.eventCode(), target.zhName());
            // 触发概率（标准 10.4，方案 D 收割）：0<p<1 的触发线按伯努利门控（任务级确定性随机流）；
            // 互斥概率组跨触发线的 CDF 采样需要共享随机预算，语料未出现，遇组名记诊断降级
            CodeBlock starter = CodeBlock.of("this::start$L", wiring.seq());
            if (trigger.probability() > 0.0D && trigger.probability() < 0.9999D) {
                if (!trigger.probabilityGroup().isEmpty()) {
                    model.diagnostic("触发线概率组 " + trigger.probabilityGroup() + "（" + trigger.describe()
                            + "）暂不支持跨线互斥采样，已按独立伯努利处理");
                }
                starter = CodeBlock.of("(c2, ctxE, hostE) -> {"
                        + " if (c2.randomStream().nextDouble() < $L) { start$L(c2, ctxE, hostE); } }",
                        String.valueOf(trigger.probability()), wiring.seq());
            }
            body.addStatement("table.onEvent($S, $S, $S, $S, $L, $L, $L, $L, $S)", sourceCode,
                    trigger.memberToken(), trigger.eventCode(), nvl(trigger.stateLabel()),
                    trigger.judge().isEmpty() ? CodeBlock.of("null")
                            : CodeBlock.of("$T::judge$L", model.conditionJudge, wiring.seq()),
                    listOf(trigger.contextPath()), listOf(trigger.hostPath()), starter, target.zhName());
        }
    }

    /** 分片大小（防 JVM 64KB 单方法字节码上限；每片接线/描述符/case 数量恒定） */
    private static final int SHARD_SIZE = 16;

    /** 顺序分桶：n 个元素切成 ceil(n/SHARD_SIZE) 个连续桶 */
    private static <T> List<List<T>> partition(List<T> items, int size) {
        List<List<T>> shards = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            shards.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return shards;
    }

    // ------------------------------------------------------------------ 运行时入口

    public JavaFile emitRuntime() {
        StarterScope scope = new StarterScope(model);
        List<Wiring> wirings = wirings();
        TypeSpec.Builder rb = TypeSpec.classBuilder(model.runtimeCn).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(PlanModel.MODULE_RUNTIME)
                .addJavadoc("模型「$L」运行时入口（模型编码 $L，版本 $L）：事件接线、默认启动、场景沙盒与过程工厂\n",
                        model.modelLabel, model.modelCode, model.modelVersion);
        rb.addField(FieldSpec.builder(String.class, "MODEL_CODE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", model.modelCode).build());
        rb.addField(FieldSpec.builder(String.class, "MODEL_VERSION", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", model.modelVersion).build());
        rb.addField(FieldSpec.builder(String.class, "MODEL_LABEL", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", model.modelLabel).build());
        rb.addField(FieldSpec.builder(String.class, "MODEL_ID", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", model.modelId).build());
        rb.addField(FieldSpec.builder(PlanModel.TRIGGER_TABLE, "triggerTable", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", PlanModel.TRIGGER_TABLE).build());

        // 构造：登记全部接线（code6.html 分片注册收割：巨型 ctor 在千站点模型上会逼近
        // JVM 64KB 单方法字节码上限触发 ClassFormatError——按 SHARD_SIZE 条接线一片
        // 拆为 registerWiringShard_K 私有方法，ctor 只保留逐片调用）
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addStatement("$T table = this.triggerTable", PlanModel.TRIGGER_TABLE);
        List<List<Wiring>> wiringShards = partition(wirings, SHARD_SIZE);
        for (int shard = 0; shard < wiringShards.size(); shard++) {
            MethodSpec.Builder register = MethodSpec.methodBuilder("registerWiringShard" + shard)
                    .addModifiers(Modifier.PRIVATE)
                    .addJavadoc("接线登记分片 $L（$L 条；分片注册防单方法字节码超限）\n",
                            shard, wiringShards.get(shard).size())
                    .addParameter(PlanModel.TRIGGER_TABLE, "table");
            for (Wiring wiring : wiringShards.get(shard)) {
                emitWiringRegistration(register, wiring);
            }
            rb.addMethod(register.build());
            ctor.addStatement("registerWiringShard$L(table)", shard);
        }
        rb.addMethod(ctor.build());

        for (Wiring wiring : wirings) {
            emitStarter(rb, scope, wiring);
        }

        // ---- GromaModuleRuntime 契约 ----
        rb.addMethod(override("modelCode").returns(String.class).addStatement("return MODEL_CODE").build());
        rb.addMethod(override("modelVersion").returns(String.class).addStatement("return MODEL_VERSION").build());
        rb.addMethod(override("modelLabel").returns(String.class).addStatement("return MODEL_LABEL").build());
        rb.addMethod(override("modelId").returns(String.class).addStatement("return MODEL_ID").build());
        rb.addMethod(override("defaultStepMs").returns(TypeName.LONG)
                .addStatement("return $LL", model.plan.configuredStepMs()).build());
        rb.addMethod(override("triggerTable").returns(PlanModel.TRIGGER_TABLE)
                .addStatement("return this.triggerTable").build());
        rb.addMethod(override("scenes").returns(PlanModel.listOf(SCENE_DESCRIPTOR))
                .addCode(sceneDescriptors()).build());

        MethodSpec.Builder create = override("createSession").returns(PlanModel.WORLD_CTX)
                .addParameter(String.class, "sceneCode").addParameter(String.class, "sessionId")
                .addParameter(long.class, "startEpochMs").addParameter(long.class, "finalStepMs").addParameter(long.class, "seed")
                .addParameter(MAP_OF_OBJECTS, "initialOverrides");
        create.beginControlFlow("switch (sceneCode)");
        for (SceneInfo scene : model.scenes) {
            create.beginControlFlow("case $S ->", scene.sceneCode());
            create.addStatement("return new $T(this, sessionId, startEpochMs, finalStepMs, seed, initialOverrides)",
                    scene.contextCn());
            create.endControlFlow();
        }
        create.addCode("default -> throw new IllegalArgumentException($S + sceneCode);\n", "SCENE_NOT_FOUND: 场景不存在 ");
        create.endControlFlow();
        rb.addMethod(create.build());

        MethodSpec.Builder newEntity = override("newEntity").returns(PlanModel.BASE_ENTITY)
                .addParameter(String.class, "typeCode").addParameter(String.class, "instanceId");
        newEntity.beginControlFlow("switch (typeCode)");
        for (TypeInfo type : model.types.values()) {
            newEntity.beginControlFlow("case $S ->", type.code());
            newEntity.addStatement("return new $T(instanceId)", type.entity());
            newEntity.endControlFlow();
        }
        newEntity.addCode("default -> { return null; }\n");
        newEntity.endControlFlow();
        rb.addMethod(newEntity.build());

        rb.addMethod(override("newProcess").returns(PlanModel.PROCESS_TASK)
                .addParameter(String.class, "defKey").addParameter(PlanModel.WORLD_CTX, "ctx")
                .addParameter(PlanModel.BASE_ENTITY, "host").addParameter(MAP_OF_OBJECTS, "args")
                .addStatement("return $T.newTask(defKey, ctx, host, args)", model.processCatalog).build());
        rb.addMethod(emitReattachWiringHook(wirings));
        rb.addMethod(override("types").returns(PlanModel.listOf(TYPE_DESCRIPTOR))
                .addStatement("return $T.TYPES", model.processCatalog).build());
        rb.addMethod(override("processes").returns(PlanModel.listOf(PROCESS_DESCRIPTOR))
                .addStatement("return $T.PROCESSES", model.processCatalog).build());
        rb.addMethod(override("indexMarkdown").returns(String.class).addCode("""
                try (java.io.InputStream in = com.groma.base.engine.GromaModuleRuntime.class
                        .getResourceAsStream("/META-INF/groma/GROMA_MODEL_INDEX.md")) {
                    return in == null ? "" : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (java.io.IOException ex) {
                    return "";
                }
                """).build());

        // ---- 手动启动点（外部干预入口：类型/场景视图里无事件触发线也无默认启动的调用点）----
        List<Wiring> manualWirings = new ArrayList<>();
        for (Wiring wiring : wirings) {
            if (wiring.manual()) {
                manualWirings.add(wiring);
            }
        }
        if (!manualWirings.isEmpty()) {
            rb.addMethod(override("manualStarts").returns(PlanModel.listOf(PlanModel.MANUAL_START_DESCRIPTOR))
                    .addJavadoc("全部手动启动点（供宿主 / MCP 发现与校验）\n").addCode(manualStartDescriptors(manualWirings))
                    .build());
            MethodSpec.Builder startManually = override("startManually").returns(PlanModel.PROCESS_TASK)
                    .addParameter(String.class, "defKey").addParameter(PlanModel.WORLD_CTX, "ctx")
                    .addParameter(PlanModel.BASE_ENTITY, "host").addParameter(MAP_OF_OBJECTS, "argOverrides")
                    .addJavadoc("手动启动过程：宿主按类型校验，成员路径经 resolveHost 解析；未注册的键返回 null\n");
            for (Wiring wiring : manualWirings) {
                ProcInfo target = model.procs.get(wiring.targetDefKey());
                TypeInfo contextType = model.types.get(wiring.typeId());
                if (target == null || contextType == null) {
                    continue;
                }
                ManualStartIR manual = manualOf(wiring.seq());
                // 同一过程可能挂在多个类型视图（宿主类型不同）：按 defKey + 宿主实例类型分发
                startManually.beginControlFlow("if ($S.equals(defKey) && host instanceof $T)",
                        wiring.targetDefKey(), contextType.entity());
                startManually.addStatement("$T contextEntity = ($T) host", contextType.entity(), contextType.entity());
                if (manual.hostPath().isEmpty()) {
                    startManually.addStatement("return startManual$L(ctx, contextEntity, contextEntity, argOverrides)",
                            wiring.seq());
                } else {
                    startManually.addStatement("$T hostEntity = $T.resolveHost(ctx, contextEntity, $T.of($L))",
                            PlanModel.BASE_ENTITY, PlanModel.TRIGGER_TABLE, List.class, stringListLiteral(manual.hostPath()));
                    startManually.addStatement(
                            "return hostEntity == null ? null : startManual$L(ctx, contextEntity, hostEntity, argOverrides)",
                            wiring.seq());
                }
                startManually.endControlFlow();
            }
            startManually.addStatement("return null");
            rb.addMethod(startManually.build());
        }
        return JavaFile.builder(model.runtimeCn.packageName(), rb.build()).indent("    ").build();
    }

    /** 手动启动点描述列表的字面量 */
    private CodeBlock manualStartDescriptors(List<Wiring> manualWirings) {
        CodeBlock.Builder cb = CodeBlock.builder().add("return $T.of(", List.class);
        boolean first = true;
        for (Wiring wiring : manualWirings) {
            ProcInfo target = model.procs.get(wiring.targetDefKey());
            TypeInfo contextType = model.types.get(wiring.typeId());
            if (target == null || contextType == null) {
                continue;
            }
            cb.add(first ? "" : ", ");
            cb.add("new $T($S, $S, $S, $T.of(", PlanModel.MANUAL_START_DESCRIPTOR,
                    wiring.targetDefKey(), target.zhName(), contextType.code(), List.class);
            boolean firstParam = true;
            for (ParamSlot param : target.params()) {
                if (param.reads()) {
                    cb.add(firstParam ? "" : ", ").add("$S", param.zhName());
                    firstParam = false;
                }
            }
            cb.add("))");
            first = false;
        }
        return cb.add(");\n").build();
    }

    /** 生成 List.of("a", "b") 字面量 */
    private static String stringListLiteral(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(items.get(i)).append('"');
        }
        return sb.toString();
    }

    private CodeBlock sceneDescriptors() {
        CodeBlock.Builder cb = CodeBlock.builder().add("return $T.of(", List.class);
        boolean first = true;
        for (SceneInfo scene : model.scenes) {
            cb.add(first ? "" : ", ").add("new $T($S, $S, $S, $S)", SCENE_DESCRIPTOR, scene.sceneCode(), scene.zhName(),
                    "方圆运行场景：" + scene.zhName(), scene.type().typeId());
            first = false;
        }
        return cb.add(");\n").build();
    }

    private DefaultStartIR defaultStartOf(int seq) {
        return model.defaultStarts.get(seq - 1);
    }

    private TriggerIR triggerOf(int seq) {
        return model.triggers.get(seq - model.defaultStarts.size() - 1);
    }

    private static MethodSpec.Builder override(String name) {
        return MethodSpec.methodBuilder(name).addAnnotation(Override.class).addModifiers(Modifier.PUBLIC);
    }

    // ------------------------------------------------------------------ 启动器与后置动作

    /**
     * 存档续跑重建在途顶层任务后，按存档记录的接线上下文重挂输出影响回写钩子（afterN 是接线侧闭包，
     * 任务本身不带；不重挂则续跑后的过程成功不回写成员——T1.5 分叉根因二「在途任务对齐」）。
     * 键 = 过程定义键，与 startN 一一对应；无输出影响的接线（无 afterN）天然不在此列。
     */
    private MethodSpec emitReattachWiringHook(List<Wiring> wirings) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder("reattachWiringHook")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "defKey").addParameter(PlanModel.WORLD_CTX, "ctx")
                .addParameter(PlanModel.BASE_ENTITY, "context").addParameter(PlanModel.BASE_ENTITY, "host")
                .addParameter(PlanModel.PROCESS_TASK, "task")
                .addJavadoc("按定义键与接线上下文类型重挂输出影响回写钩子（存档续跑专用）\n");
        boolean any = false;
        java.util.Set<String> reattachedKeys = new java.util.HashSet<>();
        for (Wiring wiring : wirings) {
            if (wiring.postActions().isEmpty()) {
                continue;
            }
            ProcInfo target = model.procs.get(wiring.targetDefKey());
            TypeInfo contextType = model.types.get(wiring.typeId());
            if (target == null || contextType == null) {
                continue;
            }
            // 手动接线同样要重挂（续跑侧无法区分手动/触发，统一按 defKey + 上下文类型分发）
            if (!reattachedKeys.add(wiring.targetDefKey() + "@" + contextType.code())) {
                model.diagnostic("过程 " + target.zhName() + " 在类型 " + contextType.zhName()
                        + " 上有多条带输出影响的接线：存档续跑统一重挂第一条（after" + wiring.seq()
                        + "）的回写钩子，其余接线若回写目标不同需拆分过程定义");
                continue;
            }
            any = true;
            mb.beginControlFlow("if ($S.equals(defKey) && context instanceof $T)",
                    wiring.targetDefKey(), contextType.entity());
            mb.addStatement("task.setCompletionHook(() -> after$L(ctx, context, host, ($T) task))",
                    wiring.seq(), target.taskCn());
            mb.addStatement("return");
            mb.endControlFlow();
        }
        mb.addComment("无输出影响接线或非接线启动（手动/子过程）的过程不需要回写钩子\n");
        if (!any) {
            // 保留空体（注释已加）：模型没有任何带输出影响的接线
        }
        return mb.build();
    }

    /** 生成 startN（绑定实参并启动目标过程）与 afterN（过程成功后的 out-change / result / consumption） */
    private void emitStarter(TypeSpec.Builder rb, StarterScope scope, Wiring wiring) {
        if (wiring.manual()) {
            emitManualStarter(rb, scope, wiring);
            return;
        }
        ProcInfo target = model.procs.get(wiring.targetDefKey());
        TypeInfo contextType = model.types.get(wiring.typeId());
        if (target == null || contextType == null) {
            return;
        }
        String startName = "start" + wiring.seq();
        String afterName = "after" + wiring.seq();
        MethodSpec.Builder start = MethodSpec.methodBuilder(startName).addModifiers(Modifier.PRIVATE)
                .addParameter(PlanModel.WORLD_CTX, "c").addParameter(PlanModel.BASE_ENTITY, "context")
                .addParameter(PlanModel.BASE_ENTITY, "host")
                .addJavadoc("接线启动器：启动过程「$L」$L\n", target.zhName(),
                        wiring.postActions().isEmpty() ? "" : "，成功后执行调用点上的后置动作");
        start.addStatement("$T contextEntity = ($T) context", contextType.entity(), contextType.entity());
        List<CodeBlock> ctorArgs = new ArrayList<>();
        if (target.owner() != null) {
            start.addStatement("$T hostEntity = ($T) host", target.owner().entity(), target.owner().entity());
            ctorArgs.add(CodeBlock.of("hostEntity"));
        }
        for (ParamSlot param : target.params()) {
            ArgBindingIR binding = find(wiring.args(), param.zhName());
            if (param.reads() && binding != null) {
                ctorArgs.add(scope.coerce(scope.render(binding.operand()), param.type(), param.kind()));
            } else {
                ctorArgs.add(CodeBlock.of(param.defaultLiteral()));
            }
        }
        CodeBlock.Builder call = CodeBlock.builder().add("new $T(c", target.taskCn());
        for (CodeBlock arg : ctorArgs) {
            call.add(", $L", arg);
        }
        call.add(")");
        if (wiring.postActions().isEmpty()) {
            start.addStatement("c.launch($L)", call.build());
        } else {
            start.addStatement("$T task = $L", target.taskCn(), call.build());
            start.addStatement("task.setWiringContext(contextEntity)");
            start.addStatement("task.setCompletionHook(() -> $L(c, context, host, task))", afterName);
            start.addStatement("c.launch(task)");
            MethodSpec.Builder after = MethodSpec.methodBuilder(afterName).addModifiers(Modifier.PRIVATE)
                    .addParameter(PlanModel.WORLD_CTX, "c").addParameter(PlanModel.BASE_ENTITY, "context")
                    .addParameter(PlanModel.BASE_ENTITY, "host").addParameter(target.taskCn(), "task")
                    .addJavadoc("过程「$L」成功后的后置动作（输出影响回写读取 task 的形参终值）\n", target.zhName());
            after.addStatement("$T contextEntity = ($T) context", contextType.entity(), contextType.entity());
            CodeBlock.Builder body = CodeBlock.builder();
            ProcessTaskEmitter.emitPostActions(model, target, scope, wiring.postActions(), body);
            after.addCode(body.build());
            rb.addMethod(after.build());
        }
        rb.addMethod(start.build());
    }

    /**
     * 手动启动器：startManualN 返回过程任务（调用方负责 c.launch）。
     * 实参优先级：overrides（中文形参名）→ 画布绑定值 → 默认字面量；成功后的后置动作复用 afterN
     */
    private void emitManualStarter(TypeSpec.Builder rb, StarterScope scope, Wiring wiring) {
        ProcInfo target = model.procs.get(wiring.targetDefKey());
        TypeInfo contextType = model.types.get(wiring.typeId());
        if (target == null || contextType == null) {
            return;
        }
        String startName = "startManual" + wiring.seq();
        String afterName = "after" + wiring.seq();
        MethodSpec.Builder start = MethodSpec.methodBuilder(startName).addModifiers(Modifier.PRIVATE)
                .addParameter(PlanModel.WORLD_CTX, "c").addParameter(PlanModel.BASE_ENTITY, "context")
                .addParameter(PlanModel.BASE_ENTITY, "host").addParameter(MAP_OF_OBJECTS, "overrides")
                .returns(PlanModel.PROCESS_TASK)
                .addJavadoc("手动启动过程「$L」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch\n",
                        target.zhName());
        start.addStatement("$T contextEntity = ($T) context", contextType.entity(), contextType.entity());
        List<CodeBlock> ctorArgs = new ArrayList<>();
        if (target.owner() != null) {
            start.addStatement("$T hostEntity = ($T) host", target.owner().entity(), target.owner().entity());
            ctorArgs.add(CodeBlock.of("hostEntity"));
        }
        for (ParamSlot param : target.params()) {
            CodeBlock overrideExpr = overrideArgExpr(param);
            CodeBlock canvas;
            if (!param.reads()) {
                canvas = CodeBlock.of(param.defaultLiteral());
            } else if (find(wiring.args(), param.zhName()) != null) {
                canvas = scope.coerce(scope.render(find(wiring.args(), param.zhName()).operand()), param.type(),
                        param.kind());
            } else {
                canvas = CodeBlock.of(param.defaultLiteral());
            }
            ctorArgs.add(overrideExpr == null ? canvas
                    : CodeBlock.of("overrides != null && overrides.get($S) != null ? $L : $L",
                            param.zhName(), overrideExpr, canvas));
        }
        CodeBlock.Builder call = CodeBlock.builder().add("new $T(c", target.taskCn());
        for (CodeBlock arg : ctorArgs) {
            call.add(", $L", arg);
        }
        call.add(")");
        if (wiring.postActions().isEmpty()) {
            start.addStatement("return $L", call.build());
        } else {
            start.addStatement("$T task = $L", target.taskCn(), call.build());
            start.addStatement("task.setWiringContext(contextEntity)");
            start.addStatement("task.setCompletionHook(() -> $L(c, context, host, task))", afterName);
            start.addStatement("return task");
            MethodSpec.Builder after = MethodSpec.methodBuilder(afterName).addModifiers(Modifier.PRIVATE)
                    .addParameter(PlanModel.WORLD_CTX, "c").addParameter(PlanModel.BASE_ENTITY, "context")
                    .addParameter(PlanModel.BASE_ENTITY, "host").addParameter(target.taskCn(), "task")
                    .addJavadoc("手动启动「$L」成功后的后置动作（输出影响回写读取 task 的形参终值）\n", target.zhName());
            after.addStatement("$T contextEntity = ($T) context", contextType.entity(), contextType.entity());
            CodeBlock.Builder body = CodeBlock.builder();
            ProcessTaskEmitter.emitPostActions(model, target, scope, wiring.postActions(), body);
            after.addCode(body.build());
            rb.addMethod(after.build());
        }
        rb.addMethod(start.build());
    }

    /** overrides 值按形参类型的转换表达式；OUT 形参不可覆盖返回 null，REF/LIST/MAP 直接强转 */
    private CodeBlock overrideArgExpr(ParamSlot param) {
        if (!param.reads()) {
            return null;
        }
        CodeBlock raw = CodeBlock.of("overrides.get($S)", param.zhName());
        if (param.type() == TypeName.DOUBLE) {
            return CodeBlock.of("$T.toDouble($L)", PlanModel.VALUES, raw);
        }
        if (param.type() == TypeName.LONG) {
            return CodeBlock.of("$T.toLong($L)", PlanModel.VALUES, raw);
        }
        if (param.type() == TypeName.BOOLEAN) {
            return CodeBlock.of("$T.toBoolean($L)", PlanModel.VALUES, raw);
        }
        if (String.class.getName().equals(param.type().toString())) {
            return CodeBlock.of("$T.toText($L)", PlanModel.VALUES, raw);
        }
        return CodeBlock.of("($T) $L", param.type(), raw);
    }

    private static ArgBindingIR find(List<ArgBindingIR> bindings, String argName) {
        for (ArgBindingIR binding : bindings) {
            if (binding.argName().equals(argName)) {
                return binding;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 过程目录

    public JavaFile emitCatalog() {
        ClassName cn = model.processCatalog;
        TypeSpec.Builder cb = TypeSpec.classBuilder(cn).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("过程目录：按过程定义键新建过程任务；形参以中文原名传值（外部干预、MCP 无会话执行、存档续跑共用）\n")
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        // 描述符分片（code6.html 收割）：巨型 <clinit> 表达式同样受 64KB 单方法字节码上限约束，
        // 按 SHARD_SIZE 条一片生成 processDescriptorShard_K/typeDescriptorShard_K，聚合方法拼接
        List<List<ProcInfo>> procShards = partition(model.procList, SHARD_SIZE);
        List<List<java.util.Map.Entry<String, TypeInfo>>> typeShards =
                partition(new ArrayList<>(model.types.entrySet()), SHARD_SIZE);
        for (int shard = 0; shard < procShards.size(); shard++) {
            cb.addMethod(descriptorShardMethod("processDescriptorShard" + shard, procShards.get(shard)));
        }
        for (int shard = 0; shard < typeShards.size(); shard++) {
            cb.addMethod(typeDescriptorShardMethod("typeDescriptorShard" + shard, typeShards.get(shard)));
        }
        cb.addMethod(aggregateMethod("buildProcesses", "processDescriptorShard", procShards.size(),
                ParameterizedTypeName.get(ClassName.get(List.class), PROCESS_DESCRIPTOR)));
        cb.addMethod(aggregateMethod("buildTypes", "typeDescriptorShard", typeShards.size(),
                ParameterizedTypeName.get(ClassName.get(List.class), TYPE_DESCRIPTOR)));
        cb.addField(FieldSpec.builder(PlanModel.listOf(PROCESS_DESCRIPTOR), "PROCESSES",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("buildProcesses()").build());
        cb.addField(FieldSpec.builder(PlanModel.listOf(TYPE_DESCRIPTOR), "TYPES",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("buildTypes()").build());

        // newTask 分片：switch 按桶拆为 newTaskShard_K，主方法顺序探测（防巨型 switch 方法超限）
        List<List<ProcInfo>> taskShards = partition(model.procList, SHARD_SIZE);
        for (int shard = 0; shard < taskShards.size(); shard++) {
            MethodSpec.Builder taskShard = MethodSpec.methodBuilder("newTaskShard" + shard)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .addJavadoc("过程任务构造分片 $L（$L 条）\n", shard, taskShards.get(shard).size())
                    .returns(PlanModel.PROCESS_TASK).addParameter(String.class, "defKey")
                    .addParameter(PlanModel.WORLD_CTX, "ctx").addParameter(PlanModel.BASE_ENTITY, "host")
                    .addParameter(MAP_OF_OBJECTS, "args");
            taskShard.beginControlFlow("switch (defKey)");
            for (ProcInfo proc : taskShards.get(shard)) {
                taskShard.beginControlFlow("case $S ->", proc.defKey());
                taskShard.addStatement("return $L", constructTask(proc));
                taskShard.endControlFlow();
            }
            taskShard.addCode("default -> { return null; }\n");
            taskShard.endControlFlow();
            cb.addMethod(taskShard.build());
        }
        MethodSpec.Builder newTask = MethodSpec.methodBuilder("newTask").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("按过程定义键新建过程任务（未注册的键返回 null）\n")
                .returns(PlanModel.PROCESS_TASK).addParameter(String.class, "defKey")
                .addParameter(PlanModel.WORLD_CTX, "ctx").addParameter(PlanModel.BASE_ENTITY, "host")
                .addParameter(MAP_OF_OBJECTS, "args");
        for (int shard = 0; shard < taskShards.size(); shard++) {
            newTask.addStatement("$T t$L = newTaskShard$L(defKey, ctx, host, args)", PlanModel.PROCESS_TASK, shard, shard);
            newTask.beginControlFlow("if (t$L != null)", shard);
            newTask.addStatement("return t$L", shard);
            newTask.endControlFlow();
        }
        newTask.addStatement("return null");
        cb.addMethod(newTask.build());
        for (MethodSpec helper : List.of(numberArg("d", TypeName.DOUBLE, "0.0D", "toDouble"),
                numberArg("l", TypeName.LONG, "0L", "toLong"))) {
            cb.addMethod(helper);
        }
        cb.addMethod(MethodSpec.methodBuilder("b").addModifiers(Modifier.PRIVATE, Modifier.STATIC).returns(TypeName.BOOLEAN)
                .addParameter(MAP_OF_OBJECTS, "args").addParameter(String.class, "zh")
                .addStatement("return args != null && args.get(zh) != null && $T.toBoolean(args.get(zh))", PlanModel.VALUES).build());
        cb.addMethod(MethodSpec.methodBuilder("s").addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ClassName.get(String.class)).addParameter(MAP_OF_OBJECTS, "args").addParameter(String.class, "zh")
                .addStatement("return args == null ? null : $T.toText(args.get(zh))", PlanModel.VALUES).build());
        return JavaFile.builder(cn.packageName(), cb.build()).indent("    ").build();
    }

    private MethodSpec numberArg(String name, TypeName type, String zero, String converter) {
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PRIVATE, Modifier.STATIC).returns(type)
                .addParameter(MAP_OF_OBJECTS, "args").addParameter(String.class, "zh")
                .addStatement("return args == null || args.get(zh) == null ? $L : $T.$L(args.get(zh))", zero, PlanModel.VALUES,
                        converter).build();
    }

    /** 构造过程任务的表达式：外部按中文形参名传值 → 强类型构造器 */
    private CodeBlock constructTask(ProcInfo proc) {
        CodeBlock.Builder call = CodeBlock.builder().add("new $T(ctx", proc.taskCn());
        if (proc.owner() != null) {
            call.add(", ($T) host", proc.owner().entity());
        }
        for (ParamSlot param : proc.params()) {
            String zh = param.zhName();
            switch (param.kind()) {
                case PlanModel.KIND_BASE -> {
                    if (param.type() == TypeName.DOUBLE) {
                        call.add(", d(args, $S)", zh);
                    } else if (param.type() == TypeName.LONG) {
                        call.add(", l(args, $S)", zh);
                    } else if (param.type() == TypeName.BOOLEAN) {
                        call.add(", b(args, $S)", zh);
                    } else if (String.class.getName().equals(param.type().toString())) {
                        call.add(", s(args, $S)", zh);
                    } else {
                        call.add(", args == null || args.get($S) == null ? null : args.get($S)", zh, zh);
                    }
                }
                case PlanModel.KIND_ENUM -> call.add(", args == null || args.get($S) == null ? null : $T.toEnum($T.class, args.get($S))",
                        zh, PlanModel.VALUES, param.type(), zh);
                default -> call.add(", ($T) (args == null || args.get($S) == null ? null : $T.decode(args.get($S), ctx))",
                        param.type(), zh, PlanModel.VALUES, zh);
            }
        }
        return call.add(")").build();
    }

    /** 过程描述符分片方法：返回本片（≤SHARD_SIZE 条）描述符列表 */
    private MethodSpec descriptorShardMethod(String name, List<ProcInfo> procs) {
        CodeBlock.Builder cb = CodeBlock.builder().add("$T.of(", List.class);
        boolean first = true;
        for (ProcInfo proc : procs) {
            String description = nvl(proc.def().description()).replace("$", "＄");
            cb.add(first ? "" : ", ").add("new $T($S, $S, $S, $S, $S, $S, $L, $S)", PROCESS_DESCRIPTOR, proc.defKey(),
                    proc.def().entityId(), proc.zhName(), proc.owner() == null ? "" : proc.owner().code(),
                    proc.owner() == null ? "独立过程" : proc.owner().zhName(),
                    proc.timeConsuming() ? "耗时过程" : "瞬时算子", proc.timeConsuming(), description);
            first = false;
        }
        cb.add(")");
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addJavadoc("过程描述符分片（$L 条）\n", procs.size())
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), PROCESS_DESCRIPTOR))
                .addStatement("return $L", cb.build()).build();
    }

    /** 类型描述符分片方法 */
    private MethodSpec typeDescriptorShardMethod(String name, List<java.util.Map.Entry<String, TypeInfo>> entries) {
        CodeBlock.Builder cb = CodeBlock.builder().add("$T.of(", List.class);
        boolean first = true;
        for (java.util.Map.Entry<String, TypeInfo> entry : entries) {
            TypeInfo type = entry.getValue();
            cb.add(first ? "" : ", ").add("new $T($S, $S, $S, $L)", TYPE_DESCRIPTOR, type.code(), type.zhName(),
                    type.typeId(), memberMetaList(type));
            first = false;
        }
        cb.add(")");
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addJavadoc("类型描述符分片（$L 条）\n", entries.size())
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), TYPE_DESCRIPTOR))
                .addStatement("return $L", cb.build()).build();
    }

    /** 分片聚合方法：顺序拼接各分片结果（分片注册防 <clinit> 与聚合方法自身超限） */
    private MethodSpec aggregateMethod(String name, String shardPrefix, int shardCount, TypeName listType) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder(name).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addJavadoc("全量描述符聚合（$L 片拼接）\n", shardCount).returns(listType);
        mb.addStatement("$T all = new $T<>()", listType, ClassName.get(ArrayList.class));
        for (int shard = 0; shard < shardCount; shard++) {
            mb.addStatement("all.addAll($L$L())", shardPrefix, shard);
        }
        mb.addStatement("return $T.copyOf(all)", List.class);
        return mb.build();
    }

    private CodeBlock memberMetaList(TypeInfo type) {
        CodeBlock.Builder cb = CodeBlock.builder().add("$T.of(", List.class);
        boolean first = true;
        for (MemberInfo member : type.allMembers()) {
            cb.add(first ? "" : ", ").add("new $T($S, $S, $S, $S, $S, $S)", PlanModel.MEMBER_META, member.tokenId(),
                    member.zhName(), member.field(), member.kind(), member.type().toString(), nvl(member.unit()));
            first = false;
        }
        return cb.add(")").build();
    }

    // ------------------------------------------------------------------ 线上判断工具

    public JavaFile emitJudgeUtil() {
        StarterScope scope = new StarterScope(model);
        ClassName cn = model.conditionJudge;
        TypeSpec.Builder jb = TypeSpec.classBuilder(cn).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("线上逻辑判断工具（类型 / 场景设计视图里连线上的线上判断，原生比较、零表达式引擎开销）\n")
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        for (Wiring wiring : wirings()) {
            if (wiring.defaultStart() || wiring.manual()) {
                continue;
            }
            TriggerIR trigger = triggerOf(wiring.seq());
            if (trigger.judge().isEmpty()) {
                continue;
            }
            TypeInfo contextType = model.types.get(trigger.ownerTypeId());
            if (contextType == null) {
                continue;
            }
            StringBuilder condition = new StringBuilder();
            for (CompilePlanIR.ConditionIR c : trigger.judge()) {
                if (!condition.isEmpty()) {
                    condition.append(" && ");
                }
                condition.append(model.memberZhName(trigger.memberToken())).append(' ').append(c.op()).append(' ').append(c.literal());
            }
            MethodSpec.Builder judge = MethodSpec.methodBuilder("judge" + wiring.seq()).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.BOOLEAN).addParameter(PlanModel.WORLD_CTX, "c").addParameter(PlanModel.BASE_ENTITY, "context")
                    .addJavadoc("线上判断：$L 上的 $L\n", contextType.zhName(), condition);
            judge.addStatement("$T contextEntity = ($T) context", contextType.entity(), contextType.entity());
            Value value = scope.render(trigger.judgeOperand());
            CompilePlanIR.NextIR next = new CompilePlanIR.NextIR(0, true, "", trigger.judge(), "", "");
            judge.addStatement("return $L", ProcessTaskEmitter.judgeCode(model, next, value));
            jb.addMethod(judge.build());
        }
        return JavaFile.builder(cn.packageName(), jb.build()).indent("    ").build();
    }

    private static String nvl(String text) {
        return text == null ? "" : text;
    }

    private static CodeBlock listOf(List<String> values) {
        if (values == null || values.isEmpty()) {
            return CodeBlock.of("java.util.List.of()");
        }
        CodeBlock.Builder cb = CodeBlock.builder().add("java.util.List.of(");
        for (int i = 0; i < values.size(); i++) {
            cb.add(i == 0 ? "$S" : ", $S", values.get(i));
        }
        return cb.add(")").build();
    }
}

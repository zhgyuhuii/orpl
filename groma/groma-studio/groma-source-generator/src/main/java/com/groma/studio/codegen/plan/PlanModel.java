package com.groma.studio.codegen.plan;

import com.groma.base.engine.GromaProcessTask;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.DefaultStartIR;
import com.groma.studio.ir.CompilePlanIR.MemberIR;
import com.groma.studio.ir.CompilePlanIR.ManualStartIR;
import com.groma.studio.ir.CompilePlanIR.NodeIR;
import com.groma.studio.ir.CompilePlanIR.PortIR;
import com.groma.studio.ir.CompilePlanIR.ProcessDefIR;
import com.groma.studio.ir.CompilePlanIR.TriggerIR;
import com.groma.studio.ir.CompilePlanIR.TypeDefIR;
import com.groma.studio.ir.CompilePlanIR.ValueSpec;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.ir.GromaTypes;
import com.groma.studio.naming.AlibabaScopeSymbolGuard;
import com.groma.studio.naming.DeterministicNamingFacade;
import com.groma.studio.naming.DomainGlossaryTokenizer;
import com.groma.studio.naming.IdentifierSanitizer;
import com.groma.studio.naming.SymbolLockTable;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 编译计划的命名与类型解析结果：把 IR 里的类型占位（E:id / EN:id / LIST:x / MAP:k:v）解析成真实 Java 类型名，
 * 并为每个成员、形参、局部量、过程节点分配符合阿里巴巴规约的英文标识符。
 * <p>
 * 解析只依赖（命名引擎 + 符号锁表 + 编译计划），与输出顺序无关，因此同一份计划多次解析得到完全相同的符号。
 * </p>
 */
public final class PlanModel {

    // ------------------------------------------------------------------ 底座类型
    public static final ClassName WORLD_CTX = ClassName.get("com.groma.base.engine", "GromaWorldContext");
    public static final ClassName BASE_ENTITY = ClassName.get("com.groma.base.engine", "BaseSimEntity");
    public static final ClassName TASK_BASE = ClassName.get("com.groma.base.engine", "GromaProcessTask");
    public static final ClassName MODULE_RUNTIME = ClassName.get("com.groma.base.engine", "GromaModuleRuntime");
    public static final ClassName MANUAL_START_DESCRIPTOR = ClassName.get("com.groma.base.engine",
            "GromaModuleRuntime.ManualStartDescriptor");
    public static final ClassName MEMBER_META = ClassName.get("com.groma.base.engine", "GromaMemberMeta");
    public static final ClassName TRIGGER_TABLE = ClassName.get("com.groma.base.engine", "GromaTriggerTable");
    public static final ClassName TRIGGER_JUDGE = ClassName.get("com.groma.base.engine", "GromaTriggerTable.Judge");
    public static final ClassName PROCESS_STARTER = ClassName.get("com.groma.base.engine", "GromaProcessStarter");
    public static final ClassName BUILTIN_TOOLS = ClassName.get("com.groma.base.engine", "BuiltinTools");
    public static final ClassName VALUES = ClassName.get("com.groma.base.engine", "GromaValues");
    public static final ClassName SNAPSHOT = ClassName.get("com.groma.base.engine", "WorldSnapshot");
    public static final ClassName PROCESS_TASK = ClassName.get("com.groma.base.engine", "GromaProcessTask");
    public static final ClassName SESSION_MANAGER = ClassName.get("com.groma.base.engine.runtime", "GromaSessionManager");
    public static final ClassName RESULT = ClassName.get("com.groma.base.common", "Result");
    /** java.lang.Object（该 JavaPoet 分支没有 OBJECT_TYPE 常量） */
    public static final ClassName OBJECT_TYPE = ClassName.get(Object.class);
    public static final ClassName STRING_TYPE = ClassName.get(String.class);
    public static final ClassName BOXED_DOUBLE = ClassName.get(Double.class);
    public static final ClassName BOXED_LONG = ClassName.get(Long.class);
    public static final ClassName BOXED_BOOLEAN = ClassName.get(Boolean.class);
    public static final ClassName GROMA_META = ClassName.get("com.groma.base.common.annotation", "GromaMeta");
    public static final ClassName SCHEMA = ClassName.get("io.swagger.v3.oas.annotations.media", "Schema");
    /** 受观察集合视图（方案 C：集合成员增删接双视角事件总线） */
    public static final ClassName OBSERVABLE_LIST = ClassName.get("com.groma.base.common.container", "ObservableEntityList");
    public static final ClassName OPERATION = ClassName.get("io.swagger.v3.oas.annotations", "Operation");
    public static final ClassName TAG = ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag");

    public static final String KIND_BASE = "BASE";
    public static final String KIND_ENUM = "ENUM";
    public static final String KIND_REF = "REF";
    public static final String KIND_LIST = "LIST";
    public static final String KIND_MAP = "MAP";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 画布时间字面量的时区：与存量引擎一致取东八区 */
    private static final ZoneOffset CANVAS_ZONE = ZoneOffset.ofHours(8);

    // ------------------------------------------------------------------ 记录

    /** 对象类型的成员 */
    public record MemberInfo(
            String tokenId,
            String zhName,
            String field,
            String getter,
            String setterCtx,
            String setterPlain,
            String kind,
            String placeholder,
            TypeName type,
            TypeName elementType,
            String containerType,
            String fieldType,
            String defaultValue,
            String initialState,
            String unit,
            int bit
    ) {
        public boolean isRef() {
            return KIND_REF.equals(kind) || KIND_LIST.equals(kind) || KIND_MAP.equals(kind);
        }

        public boolean isEnum() {
            return KIND_ENUM.equals(kind);
        }
    }

    /** 对象类型 */
    public record TypeInfo(
            String typeId,
            String zhName,
            String kind,
            String code,
            ClassName entity,
            ClassName readOnly,
            ClassName vo,
            ClassName service,
            ClassName serviceImpl,
            List<MemberInfo> ownMembers,
            List<MemberInfo> allMembers,
            TypeInfo parent,
            List<String> mixinIds,
            List<CompilePlanIR.ControlRuleIR> controls,
            List<String> notes
    ) {
        public MemberInfo member(String tokenId) {
            for (MemberInfo m : allMembers) {
                if (m.tokenId().equals(tokenId)) {
                    return m;
                }
            }
            return null;
        }

        public boolean declares(MemberInfo member) {
            return ownMembers.contains(member);
        }
    }

    /** 枚举类型 */
    public record EnumInfo(
            String enumId,
            String zhName,
            ClassName cn,
            LinkedHashMap<String, String> constants,
            String initialLabel,
            String initialConst
    ) {
        public String constantOf(String label) {
            if (label == null) {
                return null;
            }
            String hit = constants.get(label);
            if (hit != null) {
                return hit;
            }
            for (Map.Entry<String, String> e : constants.entrySet()) {
                if (e.getKey().equalsIgnoreCase(label)) {
                    return e.getValue();
                }
            }
            return null;
        }
    }

    /** 过程形参槽位 */
    public record ParamSlot(
            String entityId,
            String zhName,
            String direction,
            boolean nonnull,
            String placeholder,
            TypeName type,
            String kind,
            String field,
            String cap,
            String defaultLiteral
    ) {
        public boolean reads() {
            return "IN".equals(direction) || "IN_OUT".equals(direction) || "LOCAL".equals(direction);
        }

        public boolean writes() {
            return "OUT".equals(direction) || "IN_OUT".equals(direction) || "LOCAL".equals(direction);
        }

        public boolean isRef() {
            return KIND_REF.equals(kind) || KIND_LIST.equals(kind) || KIND_MAP.equals(kind);
        }

        public String getter() {
            return "get" + cap;
        }

        public String setter() {
            return "set" + cap;
        }
    }

    /** 过程局部量槽位 */
    public record LocalSlot(
            String entityId,
            String zhName,
            String placeholder,
            TypeName type,
            String kind,
            String field,
            String cap,
            CodeBlock initLiteral
    ) {}

    /** 过程节点槽位 */
    public record NodeSlot(
            NodeIR node,
            boolean voidTool,
            boolean storeOutput,
            String outCap,
            TypeName outType,
            boolean storeChild,
            String childCap,
            String calleeDefKey
    ) {
        public int pc() {
            return node.pc();
        }

        public boolean isCall() {
            return "CALL".equals(node.kind());
        }

    }

    /** 过程定义 */
    public record ProcInfo(
            ProcessDefIR def,
            String defKey,
            String zhName,
            String ownerKind,
            TypeInfo owner,
            ClassName taskCn,
            String taskSimple,
            ClassName boCn,
            String boSimple,
            boolean needsBo,
            String methodName,
            List<ParamSlot> params,
            List<LocalSlot> locals,
            List<NodeSlot> nodes,
            int[] entries,
            int[] slowPcs
    ) {
        public ParamSlot param(String zhName) {
            for (ParamSlot p : params) {
                if (p.zhName().equals(zhName)) {
                    return p;
                }
            }
            return null;
        }

        public NodeSlot node(int pc) {
            for (NodeSlot n : nodes) {
                if (n.pc() == pc) {
                    return n;
                }
            }
            return null;
        }

        public boolean timeConsuming() {
            return def.timeConsuming();
        }
    }

    /** 场景成员取值 */
    public record SceneMember(MemberInfo member, ValueSpec value) {}

    /** 运行场景 */
    public record SceneInfo(
            TypeInfo type,
            String sceneCode,
            String zhName,
            ClassName contextCn,
            String contextSimple,
            ClassName controllerCn,
            String controllerSimple,
            List<SceneMember> members
    ) {}

    /** 命名实例 */
    public record InstanceInfo(String instanceId, String zhName, TypeInfo type, String builderMethod, ValueSpec spec) {}

    // ------------------------------------------------------------------ 字段

    public final GromaModuleIR ir;
    public final CompilePlanIR plan;
    public final String modelCode;
    public final String moduleDir;
    /** 稳定 Maven artifactId（model-{基础码}，无语料短哈希后缀）；目录名 {@link #moduleDir} 仍带哈希 */
    public final String artifactId;
    public final String basePkg;
    /** R05 集合代理定义按类型分组（代理图元实体 id 消费见 ModelLayerEmitter 集合事件汇） */
    public Map<String, List<CompilePlanIR.ProxyDefIR>> proxiesByType;
    public final String modelId;
    public final String modelLabel;
    public final String modelVersion;
    public final String runtimeSimple;
    public final ClassName runtimeCn;

    public final SymbolLockTable lockTable;
    public final DeterministicNamingFacade naming;
    public final DomainGlossaryTokenizer tokenizer;

    public final Map<String, TypeInfo> types = new LinkedHashMap<>();
    public final Map<String, EnumInfo> enums = new LinkedHashMap<>();
    public final Map<String, ProcInfo> procs = new LinkedHashMap<>();
    public final List<ProcInfo> procList = new ArrayList<>();
    public final List<SceneInfo> scenes = new ArrayList<>();
    public final List<InstanceInfo> instances = new ArrayList<>();
    public final List<TriggerIR> triggers;
    public final List<DefaultStartIR> defaultStarts;
    public final List<ManualStartIR> manualStarts;
    public final List<String> diagnostics = new ArrayList<>();

    public final ClassName processCatalog;
    public final ClassName conditionJudge;


    private PlanModel(GromaModuleIR ir, CompilePlanIR plan, Map<String, String> existingSymbolLocks,
                      String versionHash) {
        this.ir = ir;
        this.plan = plan;
        this.lockTable = new SymbolLockTable();
        if (existingSymbolLocks != null) {
            existingSymbolLocks.forEach(this.lockTable::lockIfAbsent);
        }
        this.tokenizer = new DomainGlossaryTokenizer();
        this.naming = new DeterministicNamingFacade(lockTable, tokenizer);
        this.triggers = plan.triggers();
        this.defaultStarts = plan.defaultStarts();
        this.manualStarts = plan.manualStarts();
        for (String warning : plan.warnings()) {
            diagnostics.add(warning);
        }

        // 模块编码：显式编码优先；否则取模型中文名的首个分段做确定性转译
        String rawCode = ir.modelCode();
        if (rawCode == null || rawCode.isBlank()) {
            rawCode = naming.resolveFieldName("model", "model", "", firstSegment(ir.modelLabel()), null, false);
        }
        // 多版本并存收口：基础码解析完成后追加语料短哈希（同内容幂等，异版本互不顶替）
        this.modelCode = ModelVersionHashUtil.composeVersionedModelCode(toKebab(rawCode), versionHash);
        this.moduleDir = "model-" + modelCode;
        // Maven 坐标稳定基础名：剥离语料短哈希后缀（同源多版本只保留目录级隔离，artifactId 不随哈希漂移）
        this.artifactId = "model-" + ModelVersionHashUtil.artifactIdOf(modelCode);
        this.basePkg = "com.groma.models." + modelCode.replace("-", "");
        this.modelId = ir.modelId() == null ? "" : ir.modelId();
        this.modelLabel = ir.modelLabel() == null ? modelCode : ir.modelLabel();
        this.modelVersion = String.valueOf(ir.modelVersion());
        this.runtimeSimple = naming.resolveEntityClassName(modelId + "#runtime", "", firstSegment(modelLabel), "ModuleRuntime");
        this.runtimeCn = ClassName.get(basePkg + ".engine.runtime", runtimeSimple);
        this.processCatalog = ClassName.get(basePkg + ".engine.process", "ProcessCatalog");
        this.conditionJudge = ClassName.get(basePkg + ".utils", "ConditionJudgeUtil");
    }

    public static PlanModel of(GromaModuleIR ir, CompilePlanIR plan, Map<String, String> existingSymbolLocks) {
        return of(ir, plan, existingSymbolLocks, null);
    }

    /** 版本化入口：{@code versionHash} 非空时在基础码解析完成后追加语料短哈希后缀 */
    public static PlanModel of(GromaModuleIR ir, CompilePlanIR plan, Map<String, String> existingSymbolLocks,
                               String versionHash) {
        PlanModel model = new PlanModel(ir, plan, existingSymbolLocks, versionHash);
        model.build();
        model.linkCallees();
        return model;
    }

    /** 调用节点的被调过程（过程定义全部注册后即可解析） */
    public ProcInfo calleeOf(NodeSlot slot) {
        return (slot == null || slot.calleeDefKey() == null) ? null : procs.get(slot.calleeDefKey());
    }

    /** 解析结束后把调用节点的被调过程补齐（此时全部过程定义都已注册） */
    private void linkCallees() {
        for (ProcInfo proc : procList) {
            for (NodeSlot slot : proc.nodes()) {
                if (slot.isCall() && calleeOf(slot) == null && slot.calleeDefKey() != null) {
                    diagnostic("过程 " + proc.zhName() + " 调用了未注册的过程定义 " + slot.calleeDefKey() + "，该调用节点按空操作处理");
                }
            }
        }
    }

    // ------------------------------------------------------------------ 构建

    private void build() {
        // 1. 枚举
        for (GromaModuleIR.EnumIR enumIR : ir.enums()) {
            LinkedHashMap<String, String> constants = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            for (String label : enumIR.stateLabels()) {
                String base = constantName(label);
                String unique = base;
                int seq = 2;
                while (!used.add(unique)) {
                    unique = base + "_" + seq++;
                }
                constants.put(label, unique);
            }
            String cn = naming.resolveEntityClassName(enumIR.entityId(), "", enumIR.zhName(), "Enum");
            String initialLabel = enumIR.initialState();
            if ((initialLabel == null || !constants.containsKey(initialLabel)) && !constants.isEmpty()) {
                initialLabel = constants.keySet().iterator().next();
            }
            enums.put(enumIR.entityId(), new EnumInfo(enumIR.entityId(), enumIR.zhName(),
                    ClassName.get(basePkg + ".model.enums", cn), constants, initialLabel,
                    initialLabel == null ? null : constants.get(initialLabel)));
        }

        // 2. 类型外壳（先占位类名，再解析成员类型）
        Map<String, List<TypeInfo>> parentHolder = new LinkedHashMap<>();
        for (TypeDefIR type : plan.types()) {
            // Tier 1 接线（code5.html 3.5 收割）：类型显式编码进命名引擎，由 ExplicitEncodeClassifier 鉴别；
            // 自动码全回落（三语料出码逐字节不变的回归裁判），手写英文直采
            String code = naming.resolveEntityClassName(type.entityId(), type.explicitEncode(), type.zhName(), "");
            types.put(type.entityId(), new TypeInfo(
                    type.entityId(), type.zhName(), type.kind(), code,
                    ClassName.get(basePkg + ".model.entity", code + "Entity"),
                    ClassName.get(basePkg + ".model.entity.api", code + "ReadOnly"),
                    ClassName.get(basePkg + ".model.vo", code + "VO"),
                    ClassName.get(basePkg + ".service", "I" + code + "Service"),
                    ClassName.get(basePkg + ".service.impl", code + "ServiceImpl"),
                    List.of(), List.of(), null, List.of(), List.of(), List.of()));
            parentHolder.put(type.entityId(), List.of());
        }
        for (TypeDefIR type : plan.types()) {
            TypeInfo parent = null;
            if (type.primaryParentId() != null && !type.primaryParentId().isEmpty() && types.containsKey(type.primaryParentId())
                    && !type.primaryParentId().equals(type.entityId())) {
                parent = types.get(type.primaryParentId());
            }
            TypeInfo shell = types.get(type.entityId());
            List<MemberInfo> own = new ArrayList<>();
            int bit = parent == null ? 0 : parent.allMembers().size();
            for (MemberIR member : type.members()) {
                own.add(resolveMember(type, member, bit++));
            }
            List<MemberInfo> all = new ArrayList<>();
            if (parent != null) {
                all.addAll(parent.allMembers());
            }
            all.addAll(own);
            // R01 副父类（mixin）只保留在类型表中真实存在的 id，生成器按此生成组合委托
            List<String> mixinIds = type.mixinParentIds() == null ? List.of() : type.mixinParentIds().stream()
                    .filter(id -> !id.isBlank() && types.containsKey(id) && !id.equals(type.entityId()))
                    .toList();
            types.put(type.entityId(), new TypeInfo(shell.typeId(), shell.zhName(), shell.kind(), shell.code(),
                    shell.entity(), shell.readOnly(), shell.vo(), shell.service(), shell.serviceImpl(), own, all, parent,
                    List.copyOf(mixinIds),
                    type.controls() == null ? List.of() : type.controls(),
                    type.notes() == null ? List.of() : type.notes()));
        }

        // R05 集合代理按所属类型分组（出码消费：集合事件汇同步发代理事件）
        Map<String, List<CompilePlanIR.ProxyDefIR>> proxiesByType = new LinkedHashMap<>();
        for (TypeDefIR type : plan.types()) {
            if (type.proxies() == null || type.proxies().isEmpty()) {
                continue;
            }
            proxiesByType.put(type.entityId(), type.proxies());
        }
        this.proxiesByType = Map.copyOf(proxiesByType);

        // 3. 过程定义
        for (ProcessDefIR def : plan.processDefs()) {
            ProcInfo proc = resolveProc(def);
            procs.put(def.defKey(), proc);
            procList.add(proc);
        }

        // 4. 场景与实例
        Map<String, CompilePlanIR.ObjectSpec> sceneSpecs = new LinkedHashMap<>();
        for (CompilePlanIR.InstanceIR instance : plan.instances()) {
            if (instance.instanceId().startsWith("scene:")) {
                if (instance.spec() instanceof CompilePlanIR.ObjectSpec object) {
                    sceneSpecs.put(instance.typeId(), object);
                }
                continue;
            }
            TypeInfo type = types.get(instance.typeId());
            if (type == null) {
                diagnostics.add("命名实例 " + instance.zhName() + " 的类型不在业务模型内，已跳过");
                continue;
            }
            String builder = naming.resolveFieldName("instances", instance.instanceId(), "", instance.zhName(), null, false);
            instances.add(new InstanceInfo(instance.instanceId(), instance.zhName(), type, "build" + cap(builder), instance.spec()));
        }
        for (TypeDefIR type : plan.types()) {
            if (!"SCENE".equals(type.kind())) {
                continue;
            }
            TypeInfo info = types.get(type.entityId());
            String code = naming.resolveFieldName("scenes", type.entityId(), "", type.zhName(), null, false);
            String contextSimple = naming.resolveEntityClassName(type.entityId() + "#ctx", "", type.zhName(), "WorldContext");
            String controllerSimple = naming.resolveEntityClassName(type.entityId() + "#ctrl", "", type.zhName(), "SceneController");
            List<SceneMember> members = new ArrayList<>();
            CompilePlanIR.ObjectSpec spec = sceneSpecs.get(type.entityId());
            for (MemberInfo member : info.ownMembers()) {
                members.add(new SceneMember(member, spec == null ? null : spec.members().get(member.tokenId())));
            }
            scenes.add(new SceneInfo(info, code, type.zhName(),
                    ClassName.get(basePkg + ".engine.context", contextSimple), contextSimple,
                    ClassName.get(basePkg + ".controller", controllerSimple), controllerSimple, members));
        }
    }

    private MemberInfo resolveMember(TypeDefIR type, MemberIR member, int bit) {
        String placeholder = member.javaType() == null || member.javaType().isEmpty()
                ? GromaTypes.OBJECT : member.javaType();
        boolean bool = KIND_BASE.equals(kindOf(placeholder)) && "boolean".equals(GromaTypes.baseJavaType(member.fieldType()));
        String field = naming.resolveFieldName(type.entityId(), member.entityId(), "", member.zhName(), member.alias(), bool);
        String getter = getterName(field, bool);
        String setterCtx = "set" + cap(field);
        String kind = kindOf(placeholder);
        return new MemberInfo(
                member.entityId(), member.zhName(), field, getter, setterCtx,
                KIND_REF.equals(kind) ? null : setterCtx,
                kind, placeholder, resolve(placeholder), resolve(GromaTypes.elementTypeOf(placeholder)),
                member.containerType(), member.fieldType(), member.defaultValue(), member.initialState(), member.unit(), bit);
    }

    /**
     * 过程体写用例扫描（方案 E 只读绑定的让位条件）：返回在过程体内被当作写入目标或子过程宿主的形参 id。
     * 写入目标 = 回写实参（node.writes）与后置动作目标（postActions）里以该形参为根的操作数；
     * 宿主 = 调用节点把该形参传给带宿主对象的子过程（子过程会写 self 成员）。
     */
    private final Map<String, Set<String>> writableParamCache = new java.util.HashMap<>();
    private final Set<String> writableParamInProgress = new HashSet<>();

    /**
     * 形参“可写性”传递闭包（方案 E 只读绑定的让位条件）：返回该过程体内会被写入的形参 id 集合。
     * 直接写 = 回写实参（node.writes）与后置动作目标（postActions）里以该形参为根的操作数；
     * 传递写 = 形参经调用线实参流入可写子过程形参、或作为宿主（node.host）传入任何子过程
     * （子过程会写自己的 self 成员）。闭包按被调过程递归（备忘 + 环路退化为直接写集合），
     * 保证调用点两侧形参类型判定一致——否则调用方传 ReadOnly、被调方收实体会编译失败。
     */
    private Set<String> writableParamIds(ProcessDefIR def) {
        Set<String> memo = writableParamCache.get(def.defKey());
        if (memo != null) {
            return memo;
        }
        if (!writableParamInProgress.add(def.defKey())) {
            // 调用环：退化为只算直接写，避免无限递归（环上传入形参保持实体类型，宁保守勿错绑）
            return directWritableParams(def);
        }
        try {
            Set<String> ids = directWritableParams(def);
            Map<String, ProcessDefIR> byKey = new java.util.HashMap<>();
            for (ProcessDefIR any : plan.processDefs()) {
                byKey.put(any.defKey(), any);
            }
            for (NodeIR node : def.nodes()) {
                if (node.calleeDefKey() == null || node.calleeDefKey().isEmpty()) {
                    continue;
                }
                ProcessDefIR callee = byKey.get(node.calleeDefKey());
                if (callee == null) {
                    continue;
                }
                Set<String> calleeWritable = writableParamIds(callee);
                // 宿主形参：被调过程会写自己的 self 成员，一律视为可写
                if (node.host() != null) {
                    markRootParam(node.host(), ids);
                }
                for (CompilePlanIR.ArgBindingIR binding : node.inputs()) {
                    boolean flowsToWritable = callee.params() == null
                            ? calleeWritable.isEmpty()
                            : callee.params().stream().anyMatch(p -> p.zhName().equals(binding.argName())
                                    && calleeWritable.contains(p.entityId()));
                    if (flowsToWritable) {
                        markRootParam(binding.operand(), ids);
                    }
                }
            }
            writableParamCache.put(def.defKey(), ids);
            return ids;
        } finally {
            writableParamInProgress.remove(def.defKey());
        }
    }

    /** 操作数根若为形参则记入集合 */
    private static void markRootParam(CompilePlanIR.OperandIR op, Set<String> ids) {
        while (op instanceof CompilePlanIR.MemberPathIR path) {
            op = path.root();
        }
        if (op instanceof CompilePlanIR.ParamIR p) {
            ids.add(p.entityId());
        }
    }

    /** 直接写扫描（回写目标 / 后置动作目标落在形参上） */
    private Set<String> directWritableParams(ProcessDefIR def) {
        Set<String> ids = new HashSet<>();
        for (NodeIR node : def.nodes()) {
            for (CompilePlanIR.ArgBindingIR binding : node.writes()) {
                markRootParam(binding.operand(), ids);
            }
            for (CompilePlanIR.PostIR post : node.postActions()) {
                if (post.target() != null) {
                    markRootParam(post.target(), ids);
                }
            }
        }
        return ids;
    }

    private ProcInfo resolveProc(ProcessDefIR def) {
        TypeInfo owner = types.get(def.ownerTypeId());
        String label = switch (def.ownerKind()) {
            case "SCENE" -> (owner == null ? "场景" : owner.zhName()) + " " + def.zhName();
            case "INDEPENDENT" -> def.zhName();
            default -> (owner == null ? "" : owner.zhName() + " ") + def.zhName();
        };
        String taskSimple = naming.resolveEntityClassName(def.entityId(), "", label, "ProcessTask");
        ClassName taskCn = ClassName.get(basePkg + ".engine.process", taskSimple);
        String boSimple = naming.resolveEntityClassName(def.entityId() + "#bo", "", def.zhName(), "RuntimeBO");
        ClassName boCn = ClassName.get(basePkg + ".model.bo", boSimple);
        // 服务方法名按宿主类型去重（同一类型下可以有多个同名过程，如场景里的多个“过程1”）
        String methodName = naming.resolveFieldName("method:" + def.ownerTypeId(), "method:" + def.entityId(), "", def.zhName(), null, false);

        // R16/R17 只读契约绑定（方案 E 收割）：IN 方向的对象引用形参解析为 ReadOnly 视图。
        // 绑定前做过程体写用例扫描：回写目标 / 后置动作目标 / 子过程宿主落在该形参上时，契约让位（保持实体类型），
        // 记诊断说明未绑定——真实语料的“区间行驶”等过程经 IN 形参回写关联成员，硬绑定会破坏生成代码编译。
        Set<String> writableParams = writableParamIds(def);
        List<ParamSlot> params = new ArrayList<>();
        for (PortIR port : def.params()) {
            String placeholder = port.javaType() == null || port.javaType().isEmpty() ? GromaTypes.OBJECT : port.javaType();
            String kind = kindOf(placeholder);
            TypeName type = resolve(placeholder);
            if ("IN".equals(port.direction()) && KIND_REF.equals(kind) && !writableParams.contains(port.entityId())) {
                TypeInfo shell = typeOf(type);
                if (shell != null) {
                    type = shell.readOnly();
                }
            }
            boolean bool = KIND_BASE.equals(kind) && type == TypeName.BOOLEAN;
            String field = naming.resolveFieldName(def.defKey(), "param:" + port.entityId(), "", port.zhName(), null, bool);
            params.add(new ParamSlot(port.entityId(), port.zhName(), port.direction(), port.nonnull(), placeholder, type,
                    kind, field, cap(field), defaultLiteral(type, kind)));
        }
        List<LocalSlot> locals = new ArrayList<>();
        for (CompilePlanIR.LocalVarIR local : def.locals()) {
            String placeholder = local.javaType() == null || local.javaType().isEmpty() ? GromaTypes.OBJECT : local.javaType();
            String kind = kindOf(placeholder);
            TypeName type = resolve(placeholder);
            boolean bool = KIND_BASE.equals(kind) && type == TypeName.BOOLEAN;
            String field = naming.resolveFieldName(def.defKey(), "local:" + local.entityId(), "", local.zhName(), null, bool);
            locals.add(new LocalSlot(local.entityId(), local.zhName(), placeholder, type, kind, field, cap(field),
                    literalCode(type, kind, local.initLiteral())));
        }

        // 哪些节点的输出被下游读取
        Set<String> consumedCells = new HashSet<>();
        for (NodeIR node : def.nodes()) {
            for (CompilePlanIR.ArgBindingIR binding : concat(node.inputs(), node.writes())) {
                if (binding.operand() instanceof CompilePlanIR.NodeOutputIR out) {
                    consumedCells.add(out.cellId());
                }
            }
            for (CompilePlanIR.PostIR post : node.postActions()) {
                if (post.value() instanceof CompilePlanIR.NodeOutputIR out) {
                    consumedCells.add(out.cellId());
                }
            }
        }
        Set<String> allDefKeys = new HashSet<>();
        for (ProcessDefIR any : plan.processDefs()) {
            allDefKeys.add(any.defKey());
        }
        List<NodeSlot> nodes = new ArrayList<>();
        List<Integer> slow = new ArrayList<>();
        for (NodeIR node : def.nodes()) {
            NodeSlot slot;
            if ("TOOL".equals(node.kind())) {
                boolean voidTool = ToolSemantics.isVoidTool(node.toolKey());
                boolean conditional = node.nexts().stream().anyMatch(n -> n.conditional());
                boolean store = !voidTool && (consumedCells.contains(node.cellId()) || conditional || !node.writes().isEmpty());
                TypeName outType = node.resultJavaType() == null || node.resultJavaType().isEmpty()
                        ? OBJECT_TYPE : resolve(node.resultJavaType());
                if (node.slow()) {
                    slow.add(node.pc());
                }
                slot = new NodeSlot(node, voidTool, store, "Out" + node.pc(), outType, false, null, null);
            } else {
                boolean conditional = node.nexts().stream().anyMatch(n -> n.conditional());
                boolean storeChild = allDefKeys.contains(node.calleeDefKey())
                        && (!node.writes().isEmpty() || conditional || consumedCells.contains(node.cellId()));
                slot = new NodeSlot(node, false, false, null, null, storeChild, "Child" + node.pc(), node.calleeDefKey());
            }
            nodes.add(slot);
        }
        boolean needsBo = !locals.isEmpty() || nodes.stream().anyMatch(n -> n.storeOutput() || n.storeChild());
        int[] entries = def.entryNodes().stream().mapToInt(Integer::intValue).toArray();
        int[] slowPcs = slow.stream().mapToInt(Integer::intValue).toArray();
        return new ProcInfo(def, def.defKey(), def.zhName(), def.ownerKind(), owner, taskCn, taskSimple,
                needsBo ? boCn : null, boSimple, needsBo, methodName, params, locals, nodes, entries, slowPcs);
    }

    private static List<CompilePlanIR.ArgBindingIR> concat(List<CompilePlanIR.ArgBindingIR> a, List<CompilePlanIR.ArgBindingIR> b) {
        List<CompilePlanIR.ArgBindingIR> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    // ------------------------------------------------------------------ 工具

    public static boolean isObject(TypeName type) {
        return OBJECT_TYPE.equals(type);
    }

    /** 统一返回契约 Result<T> */
    public static ParameterizedTypeName resultOf(TypeName data) {
        return ParameterizedTypeName.get(RESULT, data);
    }

    /** Map<String, Object> */
    public static ParameterizedTypeName mapOfObjects() {
        return mapOf(STRING_TYPE, OBJECT_TYPE);
    }

    private static final ClassName LIST_RAW = ClassName.get(List.class);
    private static final ClassName MAP_RAW = ClassName.get(Map.class);
    private static final ClassName SET_RAW = ClassName.get(java.util.Set.class);

    /** Palantir JavaPoet 的参数化类型要求原始类型是 ClassName：统一走这三个辅助方法 */
    public static ParameterizedTypeName listOf(TypeName element) {
        return ParameterizedTypeName.get(LIST_RAW, element);
    }

    public static ParameterizedTypeName mapOf(TypeName key, TypeName value) {
        return ParameterizedTypeName.get(MAP_RAW, key, value);
    }

    public static ParameterizedTypeName setOf(TypeName element) {
        return ParameterizedTypeName.get(SET_RAW, element);
    }

    public static String cap(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static String getterName(String field, boolean bool) {
        return (bool ? "is" : "get") + cap(field);
    }

    static String kindOf(String placeholder) {
        if (GromaTypes.isMap(placeholder)) {
            return KIND_MAP;
        }
        if (GromaTypes.isList(placeholder)) {
            return KIND_LIST;
        }
        if (GromaTypes.isEnum(placeholder)) {
            return KIND_ENUM;
        }
        if (GromaTypes.isEntity(placeholder)) {
            return KIND_REF;
        }
        return KIND_BASE;
    }

    /** 类型占位 → Java 类型 */
    public TypeName resolve(String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return ClassName.get(Object.class);
        }
        if (GromaTypes.isMap(placeholder)) {
            String body = placeholder.substring(GromaTypes.PREFIX_MAP.length());
            int sep = body.indexOf(':');
            TypeName key = resolve(sep < 0 ? GromaTypes.STRING : body.substring(0, sep)).box();
            TypeName value = resolve(sep < 0 ? GromaTypes.OBJECT : body.substring(sep + 1)).box();
            return ParameterizedTypeName.get((ClassName) ClassName.get(Map.class), key, value);
        }
        if (GromaTypes.isList(placeholder)) {
            return ParameterizedTypeName.get((ClassName) ClassName.get(List.class),
                    resolve(GromaTypes.elementTypeOf(placeholder)).box());
        }
        if (GromaTypes.isEnum(placeholder)) {
            EnumInfo info = enums.get(GromaTypes.enumIdOf(placeholder));
            return info == null ? ClassName.get(Object.class) : info.cn();
        }
        if (GromaTypes.isEntity(placeholder)) {
            TypeInfo info = types.get(GromaTypes.entityIdOf(placeholder));
            return info == null ? ClassName.get(Object.class) : info.entity();
        }
        return switch (placeholder) {
            case GromaTypes.DOUBLE -> TypeName.DOUBLE;
            case GromaTypes.LONG -> TypeName.LONG;
            case GromaTypes.BOOLEAN -> TypeName.BOOLEAN;
            case GromaTypes.STRING -> ClassName.get(String.class);
            default -> ClassName.get(Object.class);
        };
    }

    public static boolean isEntity(TypeName type) {
        return type instanceof ClassName cn && cn.simpleName().endsWith("Entity")
                && !"BaseSimEntity".equals(cn.simpleName());
    }

    /** 某类型对应的 TypeInfo（仅领域对象类型） */
    public TypeInfo typeOf(TypeName type) {
        if (!(type instanceof ClassName cn)) {
            return null;
        }
        for (TypeInfo info : types.values()) {
            // ReadOnly 视图（方案 E 的 IN 形参类型）也归位到类型外壳：成员寻址写目标解析依赖它
            if (info.entity().equals(cn) || info.readOnly().equals(cn)) {
                return info;
            }
        }
        return null;
    }

    public EnumInfo enumOf(TypeName type) {
        for (EnumInfo info : enums.values()) {
            if (info.cn().equals(type)) {
                return info;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 字面量

    /** 各类型的零值字面量 */
    public String defaultLiteral(TypeName type, String kind) {
        if (KIND_LIST.equals(kind)) {
            return "new java.util.ArrayList<>(16)";
        }
        if (KIND_MAP.equals(kind)) {
            return "new java.util.LinkedHashMap<>(16)";
        }
        if (type == TypeName.DOUBLE) {
            return "0.0D";
        }
        if (type == TypeName.LONG) {
            return "0L";
        }
        if (type == TypeName.BOOLEAN) {
            return "false";
        }
        if (String.class.getName().equals(type.toString())) {
            return "\"\"";
        }
        return "null";
    }

    /** 画布原始文本 → Java 字面量 */
    public String literal(TypeName type, String kind, String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultLiteral(type, kind);
        }
        String text = raw.trim();
        if (type == TypeName.DOUBLE) {
            return doubleLiteral(parseDouble(text));
        }
        if (type == TypeName.LONG) {
            Long parsed = parseLong(text);
            if (parsed == null) {
                Long epoch = parseEpochMillis(text);
                if (epoch != null) {
                    return epoch + "L";
                }
                Double d = parseDouble(text);
                return (d == null ? 0L : Math.round(d)) + "L";
            }
            return parsed + "L";
        }
        if (type == TypeName.BOOLEAN) {
            return "true".equalsIgnoreCase(text) || "1".equals(text) || "是".equals(text) ? "true" : "false";
        }
        if (KIND_ENUM.equals(kind)) {
            EnumInfo info = enumOf(type);
            String constant = info == null ? null : info.constantOf(text);
            if (constant != null) {
                return info.cn().simpleName() + "." + constant;
            }
            return "null";
        }
        return null;
    }

    public static String doubleLiteral(Double value) {
        double d = value == null || !Double.isFinite(value) ? 0.0D : value;
        return Double.toString(d) + "D";
    }

    public static Double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static Long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 画布原始文本 → 带类型导入的字面量代码块；枚举按状态中文原名 / 常量名解析为枚举常量，文本返回 null（由调用方按 $S 输出）
     */
    public CodeBlock literalCode(TypeName type, String kind, String raw) {
        if (PlanModel.KIND_ENUM.equals(kind)) {
            EnumInfo info = enumOf(type);
            if (info == null) {
                return CodeBlock.of("null");
            }
            String constant = info.constantOf(raw == null ? "" : raw.trim());
            if (constant == null && (raw == null || raw.isBlank())) {
                constant = info.initialConst();
            }
            return constant == null ? CodeBlock.of("null") : CodeBlock.of("$T.$L", info.cn(), constant);
        }
        if (PlanModel.KIND_BASE.equals(kind) && String.class.getName().equals(type.toString()) && raw != null && !raw.isBlank()) {
            return CodeBlock.of("$S", raw.trim());
        }
        String literal = literal(type, kind, raw);
        return literal == null ? CodeBlock.of(modelDefault(kind, type)) : CodeBlock.of(literal);
    }

    private String modelDefault(String kind, TypeName type) {
        return defaultLiteral(type, kind);
    }

    /** 画布时间字面量（yyyy-MM-dd HH:mm:ss 等）→ 公元纪元毫秒 */
    public static Long parseEpochMillis(String text) {
        for (DateTimeFormatter formatter : List.of(DATE_TIME, DATE_ONLY,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)) {
            try {
                return LocalDateTime.parse(text, formatter).toInstant(CANVAS_ZONE).toEpochMilli();
            } catch (RuntimeException ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    /** 中文常量名 → UPPER_SNAKE_CASE */
    public String upperSnake(String label) {
        return AlibabaScopeSymbolGuard.formatUpperSnakeConstant(
                tokenizer.tokenize(IdentifierSanitizer.sanitize(label, null).coreChineseOrEnText()), "");
    }

    /** 画布字段类型 → 类型占位 */
    public static String placeholderOf(String fieldType) {
        if (fieldType == null) {
            return GromaTypes.OBJECT;
        }
        return switch (fieldType.trim().toLowerCase(Locale.ROOT)) {
            case "double", "float", "decimal", "number" -> GromaTypes.DOUBLE;
            case "long", "int", "integer" -> GromaTypes.LONG;
            case "boolean", "bool" -> GromaTypes.BOOLEAN;
            case "string", "text", "datetime", "date", "time" -> GromaTypes.STRING;
            default -> GromaTypes.OBJECT;
        };
    }

    /** 成员 id → 中文原名 */
    public String memberZhName(String tokenId) {
        for (TypeInfo type : types.values()) {
            MemberInfo member = type.member(tokenId);
            if (member != null) {
                return member.zhName();
            }
        }
        for (ProcInfo proc : procList) {
            ParamSlot param = proc.param(tokenId);
            if (param != null) {
                return param.zhName();
            }
        }
        return tokenId;
    }

    /** 参数名 → 中文名（按形参实体 id） */
    public String zhOfParamId(String paramEntityId) {
        for (ProcInfo proc : procList) {
            for (ParamSlot param : proc.params()) {
                if (param.entityId().equals(paramEntityId)) {
                    return param.zhName();
                }
            }
        }
        return paramEntityId;
    }

    private String constantName(String label) {
        return AlibabaScopeSymbolGuard.formatUpperSnakeConstant(
                tokenizer.tokenize(IdentifierSanitizer.sanitize(label, null).coreChineseOrEnText()), "");
    }

    private static String firstSegment(String label) {
        if (label == null || label.isBlank()) {
            return "groma_model";
        }
        for (String segment : label.split("[-—–_（(\\[【:]")) {
            if (!segment.isBlank()) {
                return segment.trim();
            }
        }
        return label.trim();
    }

    /** 语料摘要短哈希（SHA-256 前 8 hex）：多版本并存命名后缀 */
    private static String shortDigest8(String digest) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(digest.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .substring(0, 8);
        } catch (java.security.NoSuchAlgorithmException never) {
            throw new IllegalStateException("SHA-256 不可用", never);
        }
    }

    static String toKebab(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && sb.charAt(sb.length() - 1) != '-') {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else if (c == '_' || c == '-') {
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '-') {
                    sb.append('-');
                }
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.isEmpty() ? "groma-model" : sb.toString();
    }

    /** 过程时长属性 → 底座 Timing 常量的构造参数文本 */
    public String timingArgs(ProcessDefIR def) {
        String durationType = def.durationParamType() == null ? "STEP" : def.durationParamType().toUpperCase(Locale.ROOT);
        double normal = def.timeNormal() > 0.0D ? def.timeNormal() : 1.0D;
        long period = def.accuracyMs() == null ? 1L
                : Math.max(1L, (def.accuracyMs() + plan.configuredStepMs() - 1L) / plan.configuredStepMs());
        return def.timeConsuming() + ", " + def.twinLocked() + ", " + def.asyncMode() + ", " + def.singletonMode()
                + ", $S, " + doubleLiteral(normal) + ", $S, " + nullableDouble(def.timeMin()) + ", "
                + nullableDouble(def.timeMax()) + ", $S, " + nullableDouble(def.distMin()) + ", "
                + nullableDouble(def.distMax()) + ", " + nullableDouble(def.distAvg()) + ", "
                + nullableDouble(def.distSd()) + ", " + nullableDouble(def.distParam()) + ", "
                + (def.accuracyMs() == null ? "null" : def.accuracyMs() + "L") + ", " + period + "L";
    }

    private static String nullableDouble(Double value) {
        return value == null ? "null" : "Double.valueOf(" + doubleLiteral(value) + ")";
    }

    public CodeBlock timingValue(ProcessDefIR def) {
        String durationType = def.durationParamType() == null ? "STEP" : def.durationParamType().toUpperCase(Locale.ROOT);
        String distribution = def.distribution() == null ? "" : def.distribution().trim().toLowerCase(Locale.ROOT);
        return CodeBlock.of("new $T.Timing(" + timingArgs(def) + ")", TASK_BASE, durationType, def.timeUnit(),
                distribution);
    }

    public void diagnostic(String message) {
        if (!diagnostics.contains(message)) {
            diagnostics.add(message);
        }
    }
}

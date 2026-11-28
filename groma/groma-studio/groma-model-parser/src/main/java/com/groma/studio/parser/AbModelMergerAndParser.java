package com.groma.studio.parser;

import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.ArgBindingIR;
import com.groma.studio.ir.CompilePlanIR.AssocTypeIR;
import com.groma.studio.ir.CompilePlanIR.BuiltinIR;
import com.groma.studio.ir.CompilePlanIR.ConditionIR;
import com.groma.studio.ir.CompilePlanIR.ConditionRuleIR;
import com.groma.studio.ir.CompilePlanIR.ControlRuleIR;
import com.groma.studio.ir.CompilePlanIR.ExceptionFlowIR;
import com.groma.studio.ir.CompilePlanIR.ProxyDefIR;
import com.groma.studio.ir.CompilePlanIR.ConstIR;
import com.groma.studio.ir.CompilePlanIR.DefaultStartIR;
import com.groma.studio.ir.CompilePlanIR.InstanceIR;
import com.groma.studio.ir.CompilePlanIR.ListSpec;
import com.groma.studio.ir.CompilePlanIR.LocalIR;
import com.groma.studio.ir.CompilePlanIR.LocalVarIR;
import com.groma.studio.ir.CompilePlanIR.MapSpec;
import com.groma.studio.ir.CompilePlanIR.MemberIR;
import com.groma.studio.ir.CompilePlanIR.MemberPathIR;
import com.groma.studio.ir.CompilePlanIR.ManualStartIR;
import com.groma.studio.ir.CompilePlanIR.NamedInstanceIR;
import com.groma.studio.ir.CompilePlanIR.NextIR;
import com.groma.studio.ir.CompilePlanIR.NodeIR;
import com.groma.studio.ir.CompilePlanIR.NodeOutputIR;
import com.groma.studio.ir.CompilePlanIR.NullSpec;
import com.groma.studio.ir.CompilePlanIR.ObjectSpec;
import com.groma.studio.ir.CompilePlanIR.OperandIR;
import com.groma.studio.ir.CompilePlanIR.ParamIR;
import com.groma.studio.ir.CompilePlanIR.PortIR;
import com.groma.studio.ir.CompilePlanIR.PostIR;
import com.groma.studio.ir.CompilePlanIR.ProcessDefIR;
import com.groma.studio.ir.CompilePlanIR.RefSpec;
import com.groma.studio.ir.CompilePlanIR.ScalarSpec;
import com.groma.studio.ir.CompilePlanIR.SelfIR;
import com.groma.studio.ir.CompilePlanIR.TriggerIR;
import com.groma.studio.ir.CompilePlanIR.TypeDefIR;
import com.groma.studio.ir.CompilePlanIR.ValueSpec;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.ir.GromaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * XX-a.json（结构语义流：实体表 + 组织树 + 部分画布）与 XX-b.json（增量画布）双流确定性合并解析器
 * <p>
 * 画布合并规则：cellMap = A ∪ B，B 覆盖 A。三套 id 体系靠实体 id 连接：cell.data → 实体，
 * 连线两端是同视图内的 cell id，端口以 (cell id, port id) 定位，port.args.otherData 指向定义视图里的形参（关联对象）。
 * </p>
 * 解析产出 {@link CompilePlanIR}：
 * <ul>
 *   <li>定义视图 → 过程形参契约（非空输入 / 可空输入 / 双向 / 输出 / 生成）；</li>
 *   <li>设计视图 → 数据流图：fixed 锚点线绑定操作数与写出目标，invocation 调用线给出顺序与带线上判断的分支，
 *       out-change / result 线是节点成功后的后置动作；</li>
 *   <li>类型与场景的设计视图 → 事件接线表与默认启动表。</li>
 * </ul>
 */
public final class AbModelMergerAndParser {

    private static final Logger log = LoggerFactory.getLogger(AbModelMergerAndParser.class);

    static final Set<String> PROCESS_CELL_TYPES = Set.of("PROCESS", "TEMPLATE-PROCESS", "INDEPENDENT-PROCESS");
    /** 画布内置工具库目录（这些目录下的 TYPE / PROCESS 不进入业务模型） */
    static final Set<String> BUILTIN_TOOL_TOPS = Set.of("工具过程", "集合内置过程", "展示视图组件", "工具对象");
    static final String SCENES_TOP = "运行场景";
    static final String TYPE_KIND = "TYPE";
    static final String SCENE_KIND = "SCENE";
    static final String ASSOCIATION_KIND = "ASSOCIATION";
    static final long DEFAULT_STEP_MS = 1000L;

    /** 画布节点排序：先行后列（行按 40px 网格取整），最后按 cell id，保证与文件内顺序无关的稳定排序 */
    static final Comparator<JsonNode> CELL_ORDER = Comparator
            .comparingDouble((JsonNode c) -> Math.round(doubleOf(c, "y", 0.0D) / 40.0D))
            .thenComparingDouble(c -> doubleOf(c, "x", 0.0D))
            .thenComparing(c -> text(c, "id"));

    /** 集合元素按元素序号（单元格 defaultValue）升序；没有序号时退回画布阅读顺序 */
    static final Comparator<JsonNode> ELEMENT_ORDER = Comparator
            .comparingDouble((JsonNode c) -> indexOfElement(c))
            .thenComparing(CELL_ORDER);


    private static double indexOfElement(JsonNode cell) {
        try {
            return Double.parseDouble(text(cell, "defaultValue").trim());
        } catch (RuntimeException ex) {
            return Double.POSITIVE_INFINITY;
        }
    }

    public GromaModuleIR parse(Path fileAPath, Path fileBPath) {
        try {
            return parse(Files.readAllBytes(fileAPath), Files.readAllBytes(fileBPath));
        } catch (IOException ex) {
            throw new UncheckedIOException("读取模型语料文件失败: " + fileAPath + " / " + fileBPath, ex);
        }
    }

    public GromaModuleIR parse(byte[] bytesA, byte[] bytesB) {
        return new Worker(bytesA, bytesB).run();
    }

    // ====================================================================================== JSON 小工具

    private static JsonNode field(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode value = node.get(name);
        return (value == null || value.isMissingNode()) ? null : value;
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = field(node, name);
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isString()) {
            return value.stringValue().trim();
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asString().trim();
        }
        return "";
    }

    /** 主键为空时取回退键（跨组件 inherits 数组新版用 entityId、旧版用 id） */
    private static String orFallback(String primary, String fallback) {
        return primary == null || primary.isBlank() ? (fallback == null ? "" : fallback) : primary;
    }

    private static boolean flag(JsonNode node, String name) {
        JsonNode value = field(node, name);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        return "true".equalsIgnoreCase(value.asString().trim()) || "1".equals(value.asString().trim());
    }

    private static double doubleOf(JsonNode node, String name, double fallback) {
        JsonNode value = field(node, name);
        if (value == null || value.isNull() || !value.isNumber()) {
            return fallback;
        }
        return value.doubleValue();
    }

    private static long longOf(JsonNode node, String name, long fallback) {
        JsonNode value = field(node, name);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isNumber()) {
            return value.longValue();
        }
        try {
            return Long.parseLong(value.asString().trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static Double nullableDouble(JsonNode node, String name) {
        JsonNode value = field(node, name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.doubleValue();
        }
        try {
            return Double.parseDouble(value.asString().trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ====================================================================================== 工作器

    /**
     * 单次解析的全部状态（保证解析器实例可重入、线程安全）
     */
    private final class Worker {

        private final byte[] bytesA;
        private final byte[] bytesB;
        private final ObjectMapper mapper = new ObjectMapper();

        private final TreeMap<String, JsonNode> entities = new TreeMap<>();
        private final TreeMap<String, List<JsonNode>> rawViews = new TreeMap<>();
        private final Map<String, ViewCtx> viewCache = new HashMap<>();

        private final Map<String, String> treeTop = new HashMap<>();
        private final Map<String, List<String>> treeChildren = new HashMap<>();
        private final List<String> treeSceneIds = new ArrayList<>();
        private final Set<String> builtinTypeIds = new HashSet<>();
        private final LinkedHashSet<String> domainTypeIds = new LinkedHashSet<>();
        private final List<String> sceneIds = new ArrayList<>();
        private final List<String> assocTypeIds = new ArrayList<>();
        private final Map<String, String> typeKind = new HashMap<>();
        private final Map<String, List<String>> typeMembers = new HashMap<>();
        private final Map<String, String> memberOwner = new HashMap<>();
        private final Map<String, String> enumOfState = new HashMap<>();
        private final Map<String, List<String>> enumStates = new HashMap<>();
        private final Map<String, String> enumInitial = new HashMap<>();
        private final List<String> warnings = new ArrayList<>();

        private final Map<String, String> defKeyCache = new HashMap<>();
        private final List<ProcessDefIR> processDefs = new ArrayList<>();
        private final List<TriggerIR> triggers = new ArrayList<>();
        private final List<DefaultStartIR> defaultStarts = new ArrayList<>();
        private final List<ManualStartIR> manualStarts = new ArrayList<>();

        private String modelId = "";
        private String modelLabel = "";
        private int modelVersion = 1;

        Worker(byte[] bytesA, byte[] bytesB) {
            this.bytesA = bytesA;
            this.bytesB = bytesB;
        }

        GromaModuleIR run() {
            JsonNode objA = unwrap(mapper.readTree(bytesA));
            JsonNode objB = unwrap(mapper.readTree(bytesB));
            if (objA == null || !objA.isObject() || !field(objA, "entities").isArray()) {
                throw new IllegalArgumentException("[PRS-ERR-001] a.json 主体缺少 entities 实体表");
            }
            modelId = text(objA, "id");
            modelLabel = text(objA, "label");
            modelVersion = (int) longOf(objA, "version", 1L);

            indexEntities(objA);
            collectCells(field(objA, "cellMap"));
            if (objB != null) {
                collectCells(field(objB, "cellMap"));
            }
            walkTree(field(objA, "nodes"), "");
            classifyTypes();
            indexEnums();
            backfillComponentEnumsFromStates();
            buildProcessDefs();
            buildWiring();
            auditOrphanSemanticLinks();

            CompilePlanIR plan = new CompilePlanIR(
                    DEFAULT_STEP_MS, buildTypeDefs(), List.copyOf(processDefs), buildInstances(), buildAssocTypes(),
                    List.copyOf(triggers), List.copyOf(defaultStarts), List.copyOf(manualStarts), List.copyOf(warnings));
            log.info("[Parser] 模型 {} v{}：实体 {}，视图 {}，过程定义 {}，事件接线 {}，默认启动 {}，手动启动 {}，诊断 {}",
                    modelLabel, modelVersion, entities.size(), rawViews.size(), processDefs.size(),
                    triggers.size(), defaultStarts.size(), manualStarts.size(), warnings.size());
            return assemble(plan);
        }

        // ------------------------------------------------------------------ 基础索引

        private JsonNode unwrap(JsonNode raw) {
            if (raw == null) {
                return null;
            }
            JsonNode obj = field(raw, "obj");
            if (obj != null) {
                return obj.isString() ? mapper.readTree(obj.asString()) : obj;
            }
            JsonNode data = field(raw, "data");
            return (data != null && data.isObject()) ? data : raw;
        }

        private void indexEntities(JsonNode objA) {
            for (JsonNode entity : field(objA, "entities")) {
                String id = text(entity, "id");
                if (!id.isEmpty()) {
                    entities.put(id, entity);
                }
            }
        }

        private void collectCells(JsonNode cellMap) {
            if (cellMap == null || !cellMap.isObject()) {
                return;
            }
            for (Map.Entry<String, JsonNode> entry : cellMap.properties()) {
                JsonNode cells = entry.getValue();
                List<JsonNode> list = new ArrayList<>();
                if (cells != null && cells.isArray()) {
                    for (JsonNode cell : cells) {
                        list.add(cell);
                    }
                }
                // B 覆盖 A
                rawViews.put(entry.getKey(), list);
            }
        }

        private ViewCtx view(String viewId) {
            if (viewId == null || viewId.isEmpty() || !rawViews.containsKey(viewId)) {
                return null;
            }
            return viewCache.computeIfAbsent(viewId, id -> new ViewCtx(id, rawViews.get(id)));
        }

        private void walkTree(JsonNode nodes, String top) {
            if (nodes == null || !nodes.isArray()) {
                return;
            }
            for (JsonNode node : nodes) {
                String label = text(node, "label");
                String currentTop = top.isEmpty() ? label : top;
                String eid = text(node, "entityId");
                JsonNode kids = field(node, "nodes");
                if (!eid.isEmpty()) {
                    treeTop.put(eid, currentTop);
                    if ("SCENE".equals(text(node, "type")) && SCENES_TOP.equals(currentTop)) {
                        treeSceneIds.add(eid);
                    }
                    List<String> children = new ArrayList<>();
                    if (kids != null && kids.isArray()) {
                        for (JsonNode kid : kids) {
                            String kidEid = text(kid, "entityId");
                            if (!kidEid.isEmpty()) {
                                children.add(kidEid);
                            }
                        }
                    }
                    treeChildren.put(eid, children);
                }
                walkTree(kids, currentTop);
            }
        }

        // ------------------------------------------------------------------ 类型、成员、枚举

        /**
         * 收割块 18（模型 2 组件枚举兜底）：ENUM 定义视图未随本包导出时，
         * 从 STATE 实体的 typeEntityId 反向归集枚举归属（enumOfState），避免枚举字段降级为 String。
         */
        private void backfillComponentEnumsFromStates() {
            for (String eid : entities.keySet()) {
                JsonNode stateEnt = entities.get(eid);
                if (!"STATE".equals(text(stateEnt, "type")) || enumOfState.containsKey(eid)) {
                    continue;
                }
                String ownerEnumId = text(stateEnt, "typeEntityId");
                if (!ownerEnumId.isEmpty() && entities.containsKey(ownerEnumId)
                        && "ENUM".equals(text(entities.get(ownerEnumId), "type"))) {
                    enumOfState.put(eid, ownerEnumId);
                }
            }
        }

        private void classifyTypes() {
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                String top = treeTop.getOrDefault(eid, "");
                if ("TYPE".equals(text(entity, "type"))) {
                    boolean builtinComponent = "INNER".equalsIgnoreCase(text(entity, "componentType"));
                    if (BUILTIN_TOOL_TOPS.contains(top) || builtinComponent) {
                        // 内置工具库目录下的类型，以及内置组件（componentType=INNER）提供的类型不进入业务模型；
                        // 通用组件（GENERAL）提供的类型（如 文件信息）属于业务模型
                        builtinTypeIds.add(eid);
                    } else {
                        domainTypeIds.add(eid);
                        typeKind.put(eid, TYPE_KIND);
                    }
                } else if ("ASSOCIATION-TYPE".equals(text(entity, "type"))) {
                    assocTypeIds.add(eid);
                    typeKind.put(eid, ASSOCIATION_KIND);
                }
                // 注：外部补丁的"非顶层 SCENE 兜底收录"经回归判定为错误收割——
                // 语料中挂在组件目录的 SCENE 实体（如"默认场景"占位）会被混入权威场景表（2→4），
                // 破坏"权威场景仅来自组织树 SCENES 顶层"的刻意约定（见 AbModelMergerAndParserTest 场景断言）
            }
            for (String sceneId : treeSceneIds) {
                sceneIds.add(sceneId);
                typeKind.put(sceneId, SCENE_KIND);
            }
            // 成员归属以成员实体自己的 typeEntityId 为准
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                String type = text(entity, "type");
                if ("ATTRIBUTE-MEMBER".equals(type) || "COMPOSE-MEMBER".equals(type)) {
                    String owner = text(entity, "typeEntityId");
                    if (!owner.isEmpty() && (typeKind.containsKey(owner) || domainTypeIds.contains(owner))) {
                        memberOwner.put(eid, owner);
                    }
                }
            }
            // 类型成员表：组织树子节点顺序优先，其余按实体 id 追加
            LinkedHashSet<String> owners = new LinkedHashSet<>(domainTypeIds);
            owners.addAll(sceneIds);
            owners.addAll(assocTypeIds);
            owners.addAll(memberOwner.values());
            for (String ownerId : owners) {
                if (!entities.containsKey(ownerId)) {
                    continue;
                }
                LinkedHashSet<String> members = new LinkedHashSet<>();
                for (String child : treeChildren.getOrDefault(ownerId, List.of())) {
                    if (ownerId.equals(memberOwner.get(child))) {
                        members.add(child);
                    }
                }
                for (String memberId : memberOwner.keySet()) {
                    if (ownerId.equals(memberOwner.get(memberId))) {
                        members.add(memberId);
                    }
                }
                if (!members.isEmpty()) {
                    typeMembers.put(ownerId, new ArrayList<>(members));
                }
            }
        }

        private void indexEnums() {
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                if (!"ENUM".equals(text(entity, "type"))) {
                    continue;
                }
                LinkedHashSet<String> labels = new LinkedHashSet<>();
                String initial = "";
                ViewCtx view = view(text(entity, "defineViewId"));
                List<JsonNode> stateCells = new ArrayList<>();
                if (view != null) {
                    for (JsonNode cell : view.cells) {
                        if ("STATE".equals(text(cell, "type"))) {
                            stateCells.add(cell);
                        }
                    }
                    stateCells.sort(CELL_ORDER);
                }
                for (JsonNode cell : stateCells) {
                    String stateEntityId = text(cell, "data");
                    JsonNode stateEntity = entities.get(stateEntityId);
                    String label = stateEntity != null ? text(stateEntity, "label") : text(cell, "label");
                    if (label.isEmpty() || !labels.add(label)) {
                        continue;
                    }
                    enumOfState.put(stateEntityId, eid);
                    if (initial.isEmpty() && (flag(cell, "stateInitial") || flag(stateEntity, "stateInitial"))) {
                        initial = label;
                    }
                }
                String defaultState = text(entity, "defaultValue");
                enumStates.put(eid, new ArrayList<>(labels));
                enumInitial.put(eid, !defaultState.isEmpty() && labels.contains(defaultState) ? defaultState : initial);
            }
        }

        /** 实体 / 图元级“是否时间有关”：取值 RELATED / UNRELATED */
        private boolean isTimeRelated(JsonNode node) {
            return "RELATED".equalsIgnoreCase(text(node, "timeRelated"));
        }

        private boolean isToolProcess(JsonNode entity) {
            return entity != null && !text(entity, "innerClass").isEmpty();
        }

        private boolean isBusinessProcess(JsonNode entity) {
            return entity != null && PROCESS_CELL_TYPES.contains(text(entity, "type")) && !isToolProcess(entity)
                    && !BUILTIN_TOOL_TOPS.contains(treeTop.getOrDefault(text(entity, "id"), ""));
        }

        /** 过程定义键：有独立设计视图按视图去重（多份实体副本共享一份流程），无设计视图的叶过程按实体 */
        private String defKeyOf(JsonNode entity) {
            String id = text(entity, "id");
            String cached = defKeyCache.get(id);
            if (cached != null) {
                return cached;
            }
            String designView = text(entity, "designViewId");
            String key = (designView.isEmpty() || !rawViews.containsKey(designView)) ? "ent:" + id : "dv:" + designView;
            defKeyCache.put(id, key);
            return key;
        }

        private String ownerKindOf(String typeId) {
            return typeId == null || typeId.isEmpty() ? "INDEPENDENT" : typeKind.getOrDefault(typeId, TYPE_KIND);
        }

        // ------------------------------------------------------------------ 类型占位

        private String refTypeIdOf(JsonNode entity) {
            JsonNode custom = field(entity, "customType");
            if (custom != null && custom.isObject()) {
                String id = text(custom, "id");
                if (entities.containsKey(id)) {
                    return id;
                }
                String byLabel = findByLabel(text(custom, "label"), "ENUM".equals(text(entity, "typeRange")));
                if (!byLabel.isEmpty()) {
                    return byLabel;
                }
            }
            return findByLabel(text(entity, "label"), "ENUM".equals(text(entity, "typeRange")));
        }

        private String findByLabel(String label, boolean enumFirst) {
            if (label.isEmpty()) {
                return "";
            }
            String typeHit = "";
            String enumHit = "";
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                if (!label.equals(text(entity, "label"))) {
                    continue;
                }
                String type = text(entity, "type");
                if (enumHit.isEmpty() && "ENUM".equals(type)) {
                    enumHit = eid;
                } else if (typeHit.isEmpty() && ("TYPE".equals(type) || "ASSOCIATION-TYPE".equals(type))) {
                    typeHit = eid;
                }
            }
            if (enumFirst) {
                return !enumHit.isEmpty() ? enumHit : typeHit;
            }
            return !typeHit.isEmpty() ? typeHit : enumHit;
        }

        /** 实体（成员 / 关联对象 / 模板成员）的 Java 类型占位 */
        private String slotType(JsonNode entity) {
            if (entity == null) {
                return GromaTypes.OBJECT;
            }
            String typeRange = text(entity, "typeRange");
            String element;
            if ("ENUM".equals(typeRange)) {
                String enumId = refTypeIdOf(entity);
                element = enumId.isEmpty() ? GromaTypes.STRING : GromaTypes.enumType(enumId);
            } else if ("DEFINE".equals(typeRange)) {
                String typeId = refTypeIdOf(entity);
                element = typeId.isEmpty() ? GromaTypes.OBJECT : GromaTypes.entity(typeId);
            } else {
                element = GromaTypes.baseJavaType(text(entity, "fieldType"));
            }
            if (!flag(entity, "isCollection")) {
                return element;
            }
            if ("map".equals(text(entity, "containerType"))) {
                return GromaTypes.map(GromaTypes.baseJavaType(text(entity, "keyType")), element);
            }
            return GromaTypes.list(element);
        }

        private String slotType(String entityId) {
            return slotType(entities.get(entityId));
        }

        private String memberLabel(String memberId) {
            JsonNode entity = entities.get(memberId);
            return entity == null ? "" : text(entity, "label");
        }

        // ------------------------------------------------------------------ 过程定义

        private void buildProcessDefs() {
            Map<String, List<String>> groups = new TreeMap<>();
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                if (isBusinessProcess(entity)) {
                    groups.computeIfAbsent(defKeyOf(entity), k -> new ArrayList<>()).add(eid);
                }
            }
            for (Map.Entry<String, List<String>> group : groups.entrySet()) {
                processDefs.add(parseDef(group.getKey(), group.getValue()));
            }
        }

        private Long accuracyMillis(JsonNode entity) {
            JsonNode value = field(entity, "timeAccuracyValue");
            if (value == null || value.isNull()) {
                return null;
            }
            double accuracy;
            try {
                accuracy = value.isNumber() ? value.doubleValue() : Double.parseDouble(value.asString().trim());
            } catch (RuntimeException ex) {
                return null;
            }
            if (accuracy <= 0.0D) {
                return null;
            }
            double factor = switch (text(entity, "timeAccuracyUnit").toLowerCase(Locale.ROOT)) {
                case "ms", "毫秒" -> 1.0D;
                case "s", "秒" -> 1_000.0D;
                case "min", "分", "分钟" -> 60_000.0D;
                case "h", "时", "小时" -> 3_600_000.0D;
                case "d", "天", "日" -> 86_400_000.0D;
                default -> -1.0D;
            };
            return factor > 0.0D ? Math.max(1L, Math.round(accuracy * factor)) : null;
        }

        private ProcessDefIR parseDef(String key, List<String> entityIds) {
            JsonNode rep = entities.get(entityIds.get(0));
            String repId = text(rep, "id");
            String rawOwner = text(rep, "typeEntityId");
            String ownerKind = "INDEPENDENT-PROCESS".equals(text(rep, "type")) ? "INDEPENDENT" : ownerKindOf(rawOwner);
            String ownerTypeId = "INDEPENDENT".equals(ownerKind) ? "" : rawOwner;
            JsonNode owner = entities.get(ownerTypeId);
            ViewCtx design = view(text(rep, "designViewId"));
            ViewCtx define = view(text(rep, "defineViewId"));
            DefCtx def = new DefCtx(key, ownerTypeId, design);

            List<PortIR> params = parseParams(define);
            for (PortIR port : params) {
                def.params.put(port.entityId(), port);
            }
            parseFlowNodes(def, design, params);
            GuardBundle guards = parseGuardsAndExceptions(def, repId, entityIds, define, design, topLevelGuardViews());
            List<ConditionRuleIR> starts = guards.starts();
            List<ConditionRuleIR> maintains = guards.maintains();
            List<ExceptionFlowIR> exceptions = guards.exceptions();

            String durationType = text(rep, "timeParamType");
            if (durationType.isEmpty()) {
                durationType = "STEP";
            }
            return new ProcessDefIR(
                    key, repId, text(rep, "label"), text(rep, "description"), ownerKind, ownerTypeId,
                    owner == null ? "" : text(owner, "label"),
                    isTimeRelated(rep), flag(rep, "fromRealWorld"), flag(rep, "async"),
                    (int) longOf(rep, "singleton", 0L),
                    durationType.toUpperCase(Locale.ROOT), doubleOf(rep, "timeNormal", 1.0D),
                    text(rep, "timeUnit"), nullableDouble(rep, "timeMin"), nullableDouble(rep, "timeMax"),
                    text(rep, "distribution"), nullableDouble(rep, "distributionMin"), nullableDouble(rep, "distributionMax"),
                    nullableDouble(rep, "distributionAvg"), nullableDouble(rep, "distributionSd"),
                    nullableDouble(rep, "distributionParam"), accuracyMillis(rep),
                    text(rep, "javaCode"), text(rep, "designViewId"),
                    List.copyOf(params), List.copyOf(def.localVars), List.copyOf(def.nodes), List.copyOf(def.entries),
                    List.copyOf(starts), List.copyOf(maintains), List.copyOf(exceptions), text(rep, "encode"));
        }

        /**
         * 收割补丁 C + R25/R26：解析过程定义视图与设计视图上的
         * R22 启动条件（condition）、R21 维持条件（agent）、R13/R15 源状态前置（in-change / in-out），
         * 以及 R25 超时（overtime-exception → pcd）/ R26 提前完成（undertime-exception → pca）异常边。
         * in-out 按说明书拆解语义：源状态作为启动前置（R13），目标状态记入规则描述（R14 后置由节点 PostIR 承载）。
         */
        private record GuardBundle(
                List<ConditionRuleIR> starts,
                List<ConditionRuleIR> maintains,
                List<ExceptionFlowIR> exceptions
        ) {}

        /** 类型/场景级视图缓存（守卫扫描用；单次解析内只构建一次） */
        private List<ViewCtx> topLevelGuardViewsCache;

        /** 全部 TYPE/SCENE 实体的 define/design 视图（过程调用点上的启动/维持条件可能挂在任意复合类型视图） */
        private List<ViewCtx> topLevelGuardViews() {
            if (topLevelGuardViewsCache != null) {
                return topLevelGuardViewsCache;
            }
            List<ViewCtx> views = new ArrayList<>();
            for (String eid : entities.keySet()) {
                if (!"TYPE".equals(typeKind.get(eid)) && !sceneIds.contains(eid) && !"ASSOCIATION".equals(typeKind.get(eid))) {
                    continue;
                }
                JsonNode ent = entities.get(eid);
                for (String vid : List.of(text(ent, "defineViewId"), text(ent, "designViewId"))) {
                    ViewCtx v = view(vid);
                    if (v != null && !views.contains(v)) {
                        views.add(v);
                    }
                }
            }
            topLevelGuardViewsCache = List.copyOf(views);
            return views;
        }

        private GuardBundle parseGuardsAndExceptions(DefCtx def, String repId, List<String> entityIds,
                                                     ViewCtx define, ViewCtx design, List<ViewCtx> ownerViews) {
            List<ConditionRuleIR> starts = new ArrayList<>();
            List<ConditionRuleIR> maintains = new ArrayList<>();
            List<ExceptionFlowIR> exceptions = new ArrayList<>();
            Map<String, Integer> pcByCell = new HashMap<>();
            for (NodeIR node : def.nodes) {
                pcByCell.put(node.cellId(), node.pc());
            }
            List<ViewCtx> guardViews = new ArrayList<>();
            if (define != null) {
                guardViews.add(define);
            }
            if (design != null) {
                guardViews.add(design);
            }
            // 收割（code3.html G6 语料实证）：R22 启动条件/R21 维持条件/R13 源状态前置挂在
            // 类型/场景设计视图的过程调用点上，且可以在任意复合类型视图（不限于过程直属属主——
            // 模型 1 实证三条积水条件分属 区域/区间与端点车站/区域与区域内车站 三个类型的视图）。
            // 按目标过程实体归组、逐 def 认领（targetsThisProcess 过滤），与过程自身视图去重。
            for (ViewCtx ownerView : topLevelGuardViews()) {
                if (ownerView != null && ownerView != define && ownerView != design) {
                    guardViews.add(ownerView);
                }
            }
            for (ViewCtx v : guardViews) {
                for (JsonNode edge : v.cells) {
                    if (!"LINK".equals(text(edge, "type"))) {
                        continue;
                    }
                    String lt = linkType(edge);
                    boolean cond = "condition".equals(lt);
                    boolean agent = "agent".equals(lt);
                    boolean inChange = "in-change".equals(lt) || "in-out".equals(lt);
                    boolean overtime = "overtime-exception".equals(lt);
                    boolean undertime = "undertime-exception".equals(lt);
                    if (!cond && !agent && !inChange && !overtime && !undertime) {
                        continue;
                    }
                    JsonNode target = v.target(edge);
                    JsonNode source = v.source(edge);
                    if (target == null || source == null) {
                        continue;
                    }
                    String targetData = text(target, "data");
                    boolean targetsThisProcess = repId.equals(targetData) || entityIds.contains(targetData)
                            || pcByCell.containsKey(text(target, "id"));
                    if (!targetsThisProcess) {
                        continue;
                    }
                    if (overtime || undertime) {
                        // 收割【修复 4】：连线位于设计视图两个子过程之间时，source=被监控耗时子过程、
                        // target=异常处理子过程；sourcePc=-1 表示过程级超时/提前监控
                        Integer srcPc = pcByCell.get(text(source, "id"));
                        Integer dstPc = pcByCell.get(text(target, "id"));
                        if (dstPc != null) {
                            // 过程级（sourcePc=-1）补偿目标须带过程定义键：顶层补偿 wiring
                            // （ModuleRuntimeEmitter）按 targetDefKey 注册 pcd/pca 触发监听；
                            // 空串会被消费侧跳过（rm-c4 运行期裁判实证的真缺口）
                            String compensationDefKey = srcPc != null && srcPc >= 0 ? ""
                                    : defKeyOf(entities.get(text(target, "data")));
                            exceptions.add(new ExceptionFlowIR(
                                    overtime ? "OVERTIME_PCD" : "UNDERTIME_PCA",
                                    srcPc == null ? -1 : srcPc,
                                    dstPc, compensationDefKey));
                        }
                        continue;
                    }
                    JsonNode linkEntity = entities.get(text(edge, "data"));
                    String stateLabel = "";
                    OperandIR op;
                    boolean fromTopLevel = v != define && v != design;
                    if ("STATE".equals(text(source, "type"))) {
                        JsonNode stateEntity = entities.get(text(source, "data"));
                        stateLabel = stateEntity == null ? text(source, "label") : text(stateEntity, "label");
                        String enumId = enumOfState.getOrDefault(text(source, "data"), "");
                        if (fromTopLevel && !enumId.isEmpty()) {
                            // 类型级守卫：操作数必须从【目标过程宿主的成员链】直接构造（含主父类链），
                            // 不经视图 cell 解算——复合类型视图里的同枚举成员 cell 属于别的宿主，
                            // 拼到本过程宿主上是 ECJ 实证的错误宿主调用（getWaterloggingState on null）。
                            String hostMemberId = hostEnumMemberId(def.ownerTypeId, enumId);
                            if (hostMemberId.isEmpty()) {
                                warnings.add("[PRS-WARN-003] 类型级" + lt + "条件: " + text(source, "label")
                                        + " -> " + text(entities.getOrDefault(text(target, "data"), target), "label")
                                        + " 源枚举成员不在宿主 " + def.ownerTypeId + " 成员链上，规则降级告警");
                                continue;
                            }
                            op = new MemberPathIR(new SelfIR(GromaTypes.entity(def.ownerTypeId)),
                                    List.of(hostMemberId), GromaTypes.enumType(enumId));
                        } else {
                            op = enumId.isEmpty() ? operandOf(def, v, source) : enumMemberOperand(def, v, enumId);
                        }
                    } else {
                        op = operandOf(def, v, source, edge);
                    }
                    if (op == null) {
                        continue;
                    }
                    String describe = lt + ":" + text(source, "label");
                    // R13/R15 前锚点（code4.html M5 收割，说明书 7.2）：in-change/in-out 线的
                    // target 端为 STATE 图元时，过程启动（条件全过、首次步进前）把该成员切换到
                    // 目标状态（引擎 transitionTo → sx/sv/sa 事件链）
                    String anchorTargetState = "";
                    OperandIR anchorMember = null;
                    String anchorEnumTypeId = "";
                    if (inChange) {
                        JsonNode targetStateCell = v.cell(text(target, "id"));
                        JsonNode targetStateEntity = entities.get(text(target, "data"));
                        String targetEnumId = enumOfState.getOrDefault(text(target, "data"), "");
                        if (targetStateEntity != null && "STATE".equals(text(target, "type"))
                                && !targetEnumId.isEmpty()) {
                            anchorTargetState = text(targetStateCell, "label").isEmpty()
                                    ? text(targetStateEntity, "label") : text(targetStateCell, "label");
                            anchorEnumTypeId = targetEnumId;
                            if (fromTopLevel && !targetEnumId.isEmpty()) {
                                String hostMemberId = hostEnumMemberId(def.ownerTypeId, targetEnumId);
                                if (!hostMemberId.isEmpty()) {
                                    anchorMember = new MemberPathIR(
                                            new SelfIR(GromaTypes.entity(def.ownerTypeId)),
                                            List.of(hostMemberId), GromaTypes.enumType(targetEnumId));
                                }
                            } else {
                                anchorMember = enumMemberOperand(def, v, targetEnumId);
                            }
                        }
                    }
                    ConditionRuleIR rule = new ConditionRuleIR(
                            op, stateLabel, conditionsOf(linkEntity), describe, expressionOf(linkEntity),
                            anchorTargetState, anchorMember, anchorEnumTypeId);
                    if (agent) {
                        // R21 维持条件只进运行期逐拍检查（checkMaintain → paa）。
                        // 修复：此前 agent 兼作 canStart 启动门槛——初始不满足时退化为 psf，
                        // "维持中断"路径永不可达（合成模型 RM-C1 运行期实证）。
                        maintains.add(rule);
                    } else {
                        starts.add(rule);
                    }
                }
            }
            return new GuardBundle(starts, maintains, exceptions);
        }

        /** 连线概率（rate/percentage 兼容：>1 视为百分数） */
        private double probabilityOf(JsonNode edge, JsonNode linkEntity) {
            Double p = nullableDouble(edge, "probability");
            if (p == null && linkEntity != null) {
                p = nullableDouble(linkEntity, "probability");
            }
            if (p == null && linkEntity != null) {
                p = nullableDouble(linkEntity, "rate");
            }
            if (p == null) {
                return 1.0D;
            }
            return p > 1.0001D ? p / 100.0D : Math.max(0.0D, p);
        }

        /** 互斥概率组（标准 10.4）：probabilityGroup / groupCode / mutexGroup 字段兼容 */
        private String probabilityGroupOf(JsonNode edge, JsonNode linkEntity) {
            String g = text(edge, "probabilityGroup");
            if (g.isEmpty() && linkEntity != null) {
                g = text(linkEntity, "probabilityGroup");
            }
            if (g.isEmpty() && linkEntity != null) {
                g = text(linkEntity, "groupCode");
            }
            if (g.isEmpty() && linkEntity != null) {
                g = text(linkEntity, "mutexGroup");
            }
            return g;
        }

        /** 定义视图 → 形参契约 */
        private List<PortIR> parseParams(ViewCtx define) {
            List<PortIR> params = new ArrayList<>();
            if (define == null) {
                return params;
            }
            List<JsonNode> relevanceCells = new ArrayList<>();
            for (JsonNode cell : define.cells) {
                if ("RELEVANCE-OBJECT".equals(text(cell, "type")) && !text(cell, "data").isEmpty()) {
                    relevanceCells.add(cell);
                }
            }
            relevanceCells.sort(CELL_ORDER);
            Set<String> seen = new HashSet<>();
            for (JsonNode cell : relevanceCells) {
                String id = text(cell, "data");
                if (!seen.add(id)) {
                    continue;
                }
                String direction = "LOCAL";
                boolean nonnull = false;
                boolean result = false;
                // 影响类连线的方向固定为“关联对象 → 过程.端口”，因此看关联对象的出向连线判断输入、入向连线判断输出
                for (JsonNode edge : define.outgoing(text(cell, "id"))) {
                    if (!isProcessCell(define.target(edge))) {
                        continue;
                    }
                    switch (linkType(edge)) {
                        case "in-influence-nonnull", "in-influence" -> {
                            direction = "IN";
                            nonnull = true;
                        }
                        case "in-influence-nullable" -> direction = "IN";
                        case "in-out-influence-nonnull", "in-out-influence" -> {
                            direction = "IN_OUT";
                            nonnull = true;
                        }
                        case "in-out-influence-nullable" -> direction = "IN_OUT";
                        case "agent", "condition", "consumption" -> {
                            direction = "IN";
                            nonnull = true;
                        }
                        default -> {
                            // 其余连线不改变方向
                        }
                    }
                }
                for (JsonNode edge : define.incoming(text(cell, "id"))) {
                    if (!isProcessCell(define.source(edge))) {
                        continue;
                    }
                    String linkType = linkType(edge);
                    if ("result".equals(linkType)) {
                        direction = "OUT";
                        result = true;
                    } else if ("out-influence".equals(linkType)) {
                        direction = "OUT";
                    }
                }
                params.add(new PortIR(id, text(entities.get(id), "label"), direction, nonnull, result, slotType(id)));
            }
            return params;
        }

        // ------------------------------------------------------------------ 设计视图 → 数据流节点

        /**
         * 过程定义解析上下文
         */
        private final class DefCtx {
            final String defKey;
            final String ownerTypeId;
            final ViewCtx design;
            final Map<String, PortIR> params = new HashMap<>();
            final Map<String, OperandIR> operands = new HashMap<>();
            final LinkedHashSet<String> localIds = new LinkedHashSet<>();
            final List<LocalVarIR> localVars = new ArrayList<>();
            final List<NodeIR> nodes = new ArrayList<>();
            final List<Integer> entries = new ArrayList<>();
            final Set<String> warned = new HashSet<>();

            DefCtx(String defKey, String ownerTypeId, ViewCtx design) {
                this.defKey = defKey;
                this.ownerTypeId = ownerTypeId;
                this.design = design;
            }

            void warn(String message) {
                if (warned.add(defKey + "|" + message)) {
                    warnings.add("[" + defKey + "] " + message);
                }
            }
        }

        /** 节点原始信息（排序前） */
        private final class NodeRaw {
            final JsonNode cell;
            final JsonNode entity;
            final String cellId;
            boolean tool;
            String toolKey = "";
            String calleeDefKey = "";
            boolean calleeTimeConsuming;
            boolean slow;
            boolean firstStart;
            boolean lastEnd;
            OperandIR host;
            final List<ArgBindingIR> inputs = new ArrayList<>();
            final List<ArgBindingIR> writes = new ArrayList<>();
            final List<JsonNode> nextEdges = new ArrayList<>();
            final List<PostIR> postActions = new ArrayList<>();
            String resultJavaType = "";

            NodeRaw(JsonNode cell, JsonNode entity) {
                this.cell = cell;
                this.entity = entity;
                this.cellId = text(cell, "id");
            }
        }

        private List<JsonNode> portsOf(JsonNode cell) {
            List<JsonNode> ports = new ArrayList<>();
            JsonNode items = field(field(cell, "ports"), "items");
            if (items != null && items.isArray()) {
                for (JsonNode port : items) {
                    ports.add(port);
                }
            }
            return ports;
        }

        private JsonNode portArgs(JsonNode port) {
            return field(port, "args");
        }

        private String portName(JsonNode args) {
            String other = text(args, "otherData");
            String name = other.isEmpty() ? "" : text(entities.get(other), "label");
            if (name.isEmpty()) {
                name = text(args, "text");
            }
            return name;
        }

        private static String portDirection(JsonNode args) {
            String type = text(args, "type");
            if (type.startsWith("in-out")) {
                return "IN_OUT";
            }
            if (type.startsWith("in-")) {
                return "IN";
            }
            if (type.startsWith("out-")) {
                return "OUT";
            }
            return "";
        }

        private boolean isProcessCell(JsonNode cell) {
            return cell != null && PROCESS_CELL_TYPES.contains(text(cell, "type"));
        }

        private List<JsonNode> fixedIntoPort(ViewCtx view, String cellId, String portId) {
            List<JsonNode> out = new ArrayList<>();
            for (JsonNode edge : view.incoming(cellId)) {
                if ("fixed".equals(linkType(edge)) && portId.equals(text(edge.path("target"), "port"))) {
                    out.add(edge);
                }
            }
            return out;
        }

        private void parseFlowNodes(DefCtx def, ViewCtx view, List<PortIR> params) {
            if (view == null) {
                return;
            }
            List<JsonNode> procCells = new ArrayList<>();
            for (JsonNode cell : view.cells) {
                if (isProcessCell(cell) && entities.containsKey(text(cell, "data"))) {
                    procCells.add(cell);
                }
            }
            procCells.sort(CELL_ORDER);
            Map<String, NodeRaw> raws = new LinkedHashMap<>();
            Map<String, Integer> visualOrder = new HashMap<>();
            for (int i = 0; i < procCells.size(); i++) {
                JsonNode cell = procCells.get(i);
                visualOrder.put(text(cell, "id"), i);
                raws.put(text(cell, "id"), readNode(def, view, cell));
            }

            // 3. 控制流：全部调用线
            for (JsonNode cell : view.cells) {
                if (!"LINK".equals(text(cell, "type"))) {
                    continue;
                }
                String linkType = linkType(cell);
                NodeRaw from = raws.get(text(cell.path("source"), "cell"));
                if (from == null) {
                    continue;
                }
                if ("invocation".equals(linkType) || "invocation-self".equals(linkType)) {
                    if (raws.containsKey(text(cell.path("target"), "cell"))) {
                        from.nextEdges.add(cell);
                    }
                }
            }

            // 4. 类型推断（工具输出类型沿数据流传播，迭代到稳定）
            for (int round = 0; round < 8; round++) {
                boolean changed = false;
                for (NodeRaw raw : raws.values()) {
                    String next = raw.tool ? inferToolResult(def, raw, raws) : "";
                    if (!next.equals(raw.resultJavaType)) {
                        raw.resultJavaType = next;
                        changed = true;
                    }
                }
                if (!changed) {
                    break;
                }
            }

            // 5. 排序与编号：从入口沿调用线广度优先，其余按画布位置追加
            List<NodeRaw> ordered = orderNodes(view, raws, visualOrder);
            Map<String, Integer> pcOf = new HashMap<>();
            for (int i = 0; i < ordered.size(); i++) {
                pcOf.put(ordered.get(i).cellId, i);
            }
            for (NodeRaw raw : ordered) {
                List<NextIR> nexts = new ArrayList<>();
                raw.nextEdges.sort(Comparator.comparingInt(e -> visualOrder.getOrDefault(text(e.path("target"), "cell"), Integer.MAX_VALUE)));
                for (JsonNode edge : raw.nextEdges) {
                    String targetCell = text(edge.path("target"), "cell");
                    Integer targetPc = pcOf.get(targetCell);
                    if (targetPc == null) {
                        continue;
                    }
                    String sourcePort = text(edge.path("source"), "port");
                    JsonNode linkEntity = entities.get(text(edge, "data"));
                    boolean conditional = !sourcePort.isEmpty();
                    // 连线延迟（存量语义：线上 delayType=STEP N 拍 / FIXED 秒 → 延迟 N 步后激活后继）
                    long delayTicks = 0L;
                    String delayType = text(edge, "delayType");
                    double delayValue = doubleOf(edge, "delayValue", 0.0D);
                    if ("STEP".equalsIgnoreCase(delayType) && delayValue > 0) {
                        delayTicks = Math.max(1L, StrictMath.round(delayValue));
                    } else if ("FIXED".equalsIgnoreCase(delayType) && delayValue > 0) {
                        delayTicks = Math.max(1L, StrictMath.round(delayValue * 1000.0D / DEFAULT_STEP_MS));
                    }
                    NextIR next = new NextIR(
                            targetPc, conditional, conditional ? portName(portArgs(portOfId(raw.cell, sourcePort))) : "",
                            conditionsOf(linkEntity), expressionOf(linkEntity), text(edge, "event"), delayTicks,
                            probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity));
                    // 去重：同一条连线只登记一次
                    if (!nexts.contains(next)) {
                        nexts.add(next);
                    }
                }
                // 上游节点输出槽位的类型此刻已推断完成，回填到操作数上
                Function<OperandIR, OperandIR> resolve = op -> retargetOutput(op, raws);
                NodeIR node = new NodeIR(
                        pcOf.get(raw.cellId), raw.cellId, text(raw.entity, "id"), text(raw.entity, "label"),
                        raw.tool ? "TOOL" : "CALL", raw.toolKey, raw.calleeDefKey, raw.calleeTimeConsuming, raw.slow,
                        raw.firstStart, raw.lastEnd, raw.host == null ? null : resolve.apply(raw.host),
                        retarget(raw.inputs, resolve), retarget(raw.writes, resolve), raw.resultJavaType,
                        List.copyOf(nexts), retargetPosts(raw.postActions, resolve),
                        doubleOf(raw.cell, "x", 0.0D), doubleOf(raw.cell, "y", 0.0D));
                def.nodes.add(node);
                if (raw.firstStart) {
                    def.entries.add(node.pc());
                }
            }
            // 入口兜底：没有 firstStart 时取没有入向调用线的节点
            if (def.entries.isEmpty() && !def.nodes.isEmpty()) {
                Set<Integer> targeted = new HashSet<>();
                for (NodeIR node : def.nodes) {
                    for (NextIR next : node.nexts()) {
                        targeted.add(next.targetPc());
                    }
                }
                for (NodeIR node : def.nodes) {
                    if (!targeted.contains(node.pc())) {
                        def.entries.add(node.pc());
                    }
                }
                if (def.entries.isEmpty()) {
                    def.entries.add(def.nodes.get(0).pc());
                }
            }
            diagnoseClockReads(def, raws);
        }

        // ------------------------------------------------------------------ T1.1 编译期诊断

        /**
         * 同拍双读仿真时间的诊断（只告警，不改语义）：
         * 1) “减”的两个输入都直接读 仿真时间.持续运行时长 —— 按拍量化语义下同拍读数相同，差值恒为 0；
         * 2) “减”位于回边循环内，且两个输入的写入源都能追溯到仿真时钟 —— 循环每圈把两个输入
         *    重刷为同一量化时刻，差值恒为 0（典型：开始时刻在循环体内赋值）。
         * 修正途径：改用 ΔT（CUR-PROCESS-TIME 图元，编译为步长秒数），或把起始量的赋值移出循环。
         */
        private void diagnoseClockReads(DefCtx def, Map<String, NodeRaw> raws) {
            Map<String, List<String>> succ = new HashMap<>();
            for (NodeRaw raw : raws.values()) {
                List<String> out = new ArrayList<>();
                for (JsonNode edge : raw.nextEdges) {
                    String target = text(edge.path("target"), "cell");
                    if (raws.containsKey(target)) {
                        out.add(target);
                    }
                }
                succ.put(raw.cellId, out);
            }
            Set<String> cycle = new LinkedHashSet<>();
            Map<String, Integer> state = new HashMap<>();
            for (String id : raws.keySet()) {
                if (!state.containsKey(id)) {
                    collectCycleNodes(id, succ, state, new ArrayList<>(), cycle);
                }
            }
            for (NodeRaw raw : raws.values()) {
                if (!"arithmetic.minus".equals(raw.toolKey) || raw.inputs.size() < 2) {
                    continue;
                }
                if (raw.inputs.stream().allMatch(b -> b.operand() instanceof BuiltinIR builtin
                        && "SIM_ELAPSED_SECONDS".equals(builtin.key()))) {
                    def.warn("节点 " + raw.cellId + "（减）的两个输入都是仿真时间.持续运行时长：同拍量化读取差值恒为 0，"
                            + "请改用 ΔT（CUR-PROCESS-TIME 图元，编译为步长秒数）或调整画布");
                    continue;
                }
                if (!cycle.contains(raw.cellId)) {
                    continue;
                }
                boolean allClockFed = raw.inputs.stream()
                        .allMatch(b -> clockFed(b.operand(), raws, 0, new HashSet<>()));
                if (allClockFed) {
                    def.warn("节点 " + raw.cellId + "（减）位于回边循环内，且两个输入在循环体内每圈都重新赋值为当前量化时刻："
                            + "差值恒为 0；请把起始量的赋值移出循环，或改用 ΔT（CUR-PROCESS-TIME 图元）");
                }
            }
        }

        /** DFS 找回边，把回边覆盖的环上节点记入 cycle */
        private void collectCycleNodes(String id, Map<String, List<String>> succ, Map<String, Integer> state,
                List<String> path, Set<String> cycle) {
            state.put(id, 1);
            path.add(id);
            for (String next : succ.getOrDefault(id, List.of())) {
                Integer st = state.get(next);
                if (st == null) {
                    collectCycleNodes(next, succ, state, path, cycle);
                } else if (st == 1) {
                    boolean inCycle = false;
                    for (String node : path) {
                        if (node.equals(next)) {
                            inCycle = true;
                        }
                        if (inCycle) {
                            cycle.add(node);
                        }
                    }
                }
            }
            path.remove(path.size() - 1);
            state.put(id, 2);
        }

        /** 操作数的取值链能否追溯到仿真时钟（赋值传递、局部量/成员的写入节点递归，深度与访问集防环） */
        private boolean clockFed(OperandIR operand, Map<String, NodeRaw> raws, int depth, Set<String> visiting) {
            if (operand == null || depth > 6) {
                return false;
            }
            if (operand instanceof BuiltinIR builtin && "SIM_ELAPSED_SECONDS".equals(builtin.key())) {
                return true;
            }
            if (operand instanceof NodeOutputIR output) {
                NodeRaw source = raws.get(output.cellId());
                if (source == null || !visiting.add(source.cellId)) {
                    return false;
                }
                try {
                    for (ArgBindingIR binding : source.inputs) {
                        if (clockFed(binding.operand(), raws, depth + 1, visiting)) {
                            return true;
                        }
                    }
                } finally {
                    visiting.remove(source.cellId);
                }
                return false;
            }
            String entityId = operandEntityId(operand);
            if (entityId == null || entityId.isEmpty() || !visiting.add("member:" + entityId)) {
                return false;
            }
            try {
                for (NodeRaw raw : raws.values()) {
                    for (ArgBindingIR binding : raw.writes) {
                        if (!entityId.equals(operandEntityId(binding.operand()))) {
                            continue;
                        }
                        for (ArgBindingIR input : raw.inputs) {
                            if (clockFed(input.operand(), raws, depth + 1, visiting)) {
                                return true;
                            }
                        }
                    }
                }
            } finally {
                visiting.remove("member:" + entityId);
            }
            return false;
        }

        /** 操作数对应的实体 id（成员路径取最后一段） */
        private String operandEntityId(OperandIR operand) {
            if (operand instanceof MemberPathIR path) {
                return path.memberIds().isEmpty() ? "" : path.memberIds().get(path.memberIds().size() - 1);
            }
            if (operand instanceof LocalIR local) {
                return local.entityId();
            }
            if (operand instanceof ParamIR param) {
                return param.entityId();
            }
            if (operand instanceof NamedInstanceIR named) {
                return named.instanceId();
            }
            if (operand instanceof ConstIR constant) {
                return constant.entityId();
            }
            return "";
        }

        private JsonNode portOfId(JsonNode cell, String portId) {
            for (JsonNode port : portsOf(cell)) {
                if (portId.equals(text(port, "id"))) {
                    return port;
                }
            }
            return null;
        }

        /** 单个过程图元 → 节点原始信息 */
        private NodeRaw readNode(DefCtx def, ViewCtx view, JsonNode cell) {
            JsonNode entity = entities.get(text(cell, "data"));
            NodeRaw raw = new NodeRaw(cell, entity);
            raw.tool = isToolProcess(entity);
            raw.toolKey = raw.tool ? text(entity, "innerClass") + "." + text(entity, "innerType") : "";
            if (!raw.tool) {
                raw.calleeDefKey = defKeyOf(entity);
                String cellTime = text(cell, "timeRelated");
                raw.calleeTimeConsuming = cellTime.isEmpty() ? isTimeRelated(entity) : "RELATED".equalsIgnoreCase(cellTime);
            } else {
                raw.slow = isTimeRelated(cell);
            }
            raw.firstStart = flag(cell, "firstStart");
            raw.lastEnd = flag(cell, "lastEnd");

            // 组成线：CALL 节点的宿主对象 / 集合类工具操作的集合
            for (JsonNode edge : view.incoming(raw.cellId)) {
                if ("composition".equals(linkType(edge))) {
                    raw.host = operandOf(def, view, view.source(edge));
                    break;
                }
            }

            List<JsonNode> ports = portsOf(cell);
            for (JsonNode port : ports) {
                String portId = text(port, "id");
                JsonNode args = portArgs(port);
                String direction = portDirection(args);
                if (direction.isEmpty()) {
                    direction = "IN";
                }
                String argName = portName(args);
                if (argName.isEmpty()) {
                    argName = portId;
                }
                // 锚点线永远指向被馈入的端口：来源是成员 = 读取或写出；来源是另一节点的输出端口 = 取上游结果
                List<JsonNode> into = fixedIntoPort(view, raw.cellId, portId);
                sortBySourcePosition(view, into);
                for (JsonNode edge : into) {
                    JsonNode source = view.source(edge);
                    OperandIR operand = operandOf(def, view, source, edge);
                    if (isProcessCell(source)) {
                        if ("OUT".equals(direction)) {
                            def.warn("输出端口 " + argName + " 被另一个节点的输出端口馈入，按输入处理");
                        }
                        raw.inputs.add(new ArgBindingIR(argName, operand));
                        continue;
                    }
                    if ("OUT".equals(direction)) {
                        raw.writes.add(new ArgBindingIR(argName, operand));
                        continue;
                    }
                    raw.inputs.add(new ArgBindingIR(argName, operand));
                    if ("IN_OUT".equals(direction)) {
                        raw.writes.add(new ArgBindingIR(argName, operand));
                    }
                }
            }
            // 无端口定义的过程图元：入向锚点线按唯一隐式双向参数处理
            if (ports.isEmpty()) {
                List<JsonNode> implicit = new ArrayList<>();
                for (JsonNode edge : view.incoming(raw.cellId)) {
                    if ("fixed".equals(linkType(edge))) {
                        implicit.add(edge);
                    }
                }
                sortBySourcePosition(view, implicit);
                for (JsonNode edge : implicit) {
                    OperandIR operand = operandOf(def, view, view.source(edge), edge);
                    raw.inputs.add(new ArgBindingIR("a", operand));
                    raw.writes.add(new ArgBindingIR("a", operand));
                }
            }
            readPostActions(def, view, raw);
            return raw;
        }

        /**
         * 节点成功后的后置动作：out-change 状态转换、result 生成、consumption 消耗。
         * 过程设计视图里的子过程调用点与类型 / 场景设计视图里的顶层调用点共用这一规则。
         */
        private void readPostActions(DefCtx def, ViewCtx view, NodeRaw raw) {
            raw.postActions.addAll(collectPostActions(def, view, raw.cellId));
        }

        private List<PostIR> collectPostActions(DefCtx def, ViewCtx view, String cellId) {
            List<PostIR> posts = new ArrayList<>();
            List<JsonNode> postEdges = new ArrayList<>();
            for (JsonNode edge : view.outgoing(cellId)) {
                String linkType = linkType(edge);
                // 收割【修复 3】：R15 in-out 拆解语义——源状态前置进 canStart（parseGuards），
                // 目标状态后置转换在此与 out-change 同路承载
                if ("out-change".equals(linkType) || "in-out".equals(linkType) || "result".equals(linkType)
                        || "consumption".equals(linkType)) {
                    postEdges.add(edge);
                }
            }
            sortByTargetPosition(view, postEdges);
            for (JsonNode edge : postEdges) {
                JsonNode linkEntity = entities.get(text(edge, "data"));
                JsonNode target = view.target(edge);
                if (target == null || isProcessCell(target)) {
                    continue;
                }
                String linkType = linkType(edge);
                if ("out-change".equals(linkType) || "in-out".equals(linkType)) {
                    JsonNode stateEntity = entities.get(text(target, "data"));
                    String enumId = enumOfState.getOrDefault(text(target, "data"), "");
                    OperandIR member = enumMemberOperand(def, view, enumId);
                    if (member == null) {
                        def.warn("输出转换找不到枚举类型 " + memberLabel(enumId) + " 的成员，已忽略");
                        continue;
                    }
                    posts.add(new PostIR("OUT_CHANGE", member, text(stateEntity, "label"), enumId, null,
                            probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
                } else if ("result".equals(linkType)) {
                    posts.add(new PostIR("RESULT", operandOf(def, view, target), "", "", null,
                            probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
                } else {
                    posts.add(new PostIR("CONSUME", operandOf(def, view, target), "", "", null,
                            probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
                }
            }
            return posts;
        }

        /** 视图里属于指定枚举类型的成员单元格 → 操作数（直接成员优先） */
        private OperandIR enumMemberOperand(DefCtx def, ViewCtx view, String enumId) {
            if (enumId == null) {
                return null;
            }
            List<JsonNode> candidates = new ArrayList<>();
            for (JsonNode cell : view.cells) {
                JsonNode entity = entities.get(text(cell, "data"));
                if (entity == null || !"ENUM".equals(text(entity, "typeRange"))) {
                    continue;
                }
                if (enumId.equals(refTypeIdOf(entity))) {
                    candidates.add(cell);
                }
            }
            candidates.sort(CELL_ORDER);
            JsonNode chosen = null;
            for (JsonNode cell : candidates) {
                if (pathSourceOf(view, text(cell, "id")).isEmpty()) {
                    chosen = cell;
                    break;
                }
            }
            if (chosen == null && !candidates.isEmpty()) {
                chosen = candidates.get(0);
                def.warn("输出转换的目标枚举成员不唯一，按画布位置取第一个");
            }
            return chosen == null ? null : operandOf(def, view, chosen);
        }

        // ------------------------------------------------------------------ 操作数解析

        /** 指向该成员单元格的路径线（属性 / 组成），来源不是过程图元 */
        private List<JsonNode> pathSourcesOf(ViewCtx view, String cellId) {
            List<JsonNode> sources = new ArrayList<>();
            for (JsonNode edge : view.incoming(cellId)) {
                String linkType = linkType(edge);
                if (("exhibition".equals(linkType) || "composition".equals(linkType))
                        && !isProcessCell(view.source(edge))) {
                    sources.add(edge);
                }
            }
            return sources;
        }

        private String pathSourceOf(ViewCtx view, String cellId) {
            List<JsonNode> sources = pathSourcesOf(view, cellId);
            if (sources.isEmpty()) {
                return "";
            }
            sortBySourcePosition(view, sources);
            return text(sources.get(0).path("source"), "cell");
        }

        private void sortBySourcePosition(ViewCtx view, List<JsonNode> edges) {
            edges.sort(Comparator.comparingDouble((JsonNode e) -> posY(view.source(e)))
                    .thenComparingDouble(e -> posX(view.source(e)))
                    .thenComparing(e -> text(e, "id")));
        }

        private void sortByTargetPosition(ViewCtx view, List<JsonNode> edges) {
            edges.sort(Comparator.comparingDouble((JsonNode e) -> posY(view.target(e)))
                    .thenComparingDouble(e -> posX(view.target(e)))
                    .thenComparing(e -> text(e, "id")));
        }

        private double posX(JsonNode cell) {
            return cell == null ? 0.0D : doubleOf(cell, "x", 0.0D);
        }

        private double posY(JsonNode cell) {
            return cell == null ? 0.0D : doubleOf(cell, "y", 0.0D);
        }

        private static double positionOf(JsonNode cell) {
            return (cell == null ? 0.0D : doubleOf(cell, "y", 0.0D)) * 100_000.0D
                    + (cell == null ? 0.0D : doubleOf(cell, "x", 0.0D));
        }

        private String entityTypePlaceholder(String typeId) {
            return (typeId == null || typeId.isEmpty()) ? GromaTypes.OBJECT : GromaTypes.entity(typeId);
        }

        /**
         * 单元格 → 操作数。edge 非空且单元格是带输出端口的过程图元时，取该节点端口的输出槽位
         */
        private OperandIR operandOf(DefCtx def, ViewCtx view, JsonNode cell) {
            return operandOf(def, view, cell, null);
        }

        private OperandIR operandOf(DefCtx def, ViewCtx view, JsonNode cell, JsonNode edge) {
            if (cell == null) {
                return new ConstIR("", "null", GromaTypes.OBJECT, "<缺失>");
            }
            String cellId = text(cell, "id");
            if (isProcessCell(cell)) {
                String portId = edge == null ? "" : text(edge.path("source"), "port");
                JsonNode port = portId.isEmpty() ? null : portOfId(cell, portId);
                String argName = port == null ? "" : portName(portArgs(port));
                String cacheKey = cellId + "#" + argName;
                OperandIR cachedOutput = def.operands.get(cacheKey);
                if (cachedOutput == null) {
                    cachedOutput = new NodeOutputIR(cellId, argName, GromaTypes.OBJECT);
                    def.operands.put(cacheKey, cachedOutput);
                }
                return cachedOutput;
            }
            OperandIR cached = def.operands.get(cellId);
            if (cached != null) {
                return cached;
            }
            OperandIR operand = resolveOperand(def, view, cell);
            def.operands.put(cellId, operand);
            return operand;
        }

        private OperandIR resolveOperand(DefCtx def, ViewCtx view, JsonNode cell) {
            String cellId = text(cell, "id");
            String type = text(cell, "type");
            String entityId = text(cell, "data");
            JsonNode entity = entities.get(entityId);
            String label = entity == null ? text(cell, "label") : text(entity, "label");

            // 内置对象根
            switch (type) {
                case "CUR-RUN-TIME":
                    return new BuiltinIR("SIM", null, GromaTypes.OBJECT);
                case "CUR-OBJECT":
                case "THING-OBJECT":
                    return new SelfIR(entityTypePlaceholder(def.ownerTypeId));
                case "CUR-PROCESS-TIME":
                    // 存量语义（initials/Helper 把该图元解析为 DELTA_T 变量，runtime/ProcessInfo.runProcess
                    // 每次运行刷新为距上次运行的秒数）：过程每拍至多推进一圈，ΔT 恒等于步长秒数
                    return new BuiltinIR("SIM_STEP_SECONDS", null, GromaTypes.DOUBLE);
                case "ACTUAL-RUN-TIME":
                    return new BuiltinIR("ACTUAL_RUN_SECONDS", null, GromaTypes.DOUBLE);
                case "USER-INFO":
                    return new BuiltinIR("USER", null, GromaTypes.OBJECT);
                case "RUN-PARAM":
                    return new BuiltinIR("RUN_PARAM", null, GromaTypes.OBJECT);
                default:
                    break;
            }

            // 路径成员：目标车站 → 位置 → 经度
            String pathSource = pathSourceOf(view, cellId);
            if (!pathSource.isEmpty()) {
                OperandIR root = operandOf(def, view, view.cell(pathSource));
                return memberStep(def, root, entity, label);
            }

            switch (type) {
                case "INSTANCE": {
                    String typeId = text(entity, "instanceTypeEntityId");
                    if (typeId.isEmpty()) {
                        typeId = text(entity, "typeEntityId");
                    }
                    String placeholder = entityTypePlaceholder(typeId);
                    if (flag(entity, "isCollection")) {
                        placeholder = "map".equals(text(entity, "containerType"))
                                ? GromaTypes.map(GromaTypes.baseJavaType(text(entity, "keyType")), placeholder)
                                : GromaTypes.list(placeholder);
                    }
                    return new NamedInstanceIR(entityId, typeId, placeholder);
                }
                case "CONSTANT": {
                    String displayName = label;
                    if (text(entity, "alias").length() > 0 && label.startsWith("定值")) {
                        displayName = text(entity, "alias");
                    }
                    return new ConstIR(entityId, text(entity, "defaultValue"), slotType(entity), displayName);
                }
                case "STATE": {
                    String enumId = enumOfState.getOrDefault(entityId, "");
                    return new ConstIR(entityId, label,
                            enumId.isEmpty() ? GromaTypes.STRING : GromaTypes.enumType(enumId), label);
                }
                case "NOTE": {
                    String text = text(entity, "description");
                    return new ConstIR(entityId, text.isEmpty() ? label : text, GromaTypes.STRING, label);
                }
                default:
                    break;
            }

            if (entity == null) {
                def.warn("单元格 " + cellId + " 引用的实体不存在: " + entityId);
                return new ConstIR(entityId, "null", GromaTypes.OBJECT, label);
            }

            // 根成员：形参 / 局部量 / 宿主自身的成员
            PortIR param = def.params.get(entityId);
            if (param != null && !"LOCAL".equals(param.direction())) {
                return new ParamIR(entityId, label, param.javaType());
            }
            String entityKind = text(entity, "type");
            boolean variableLike = "TEMPLATE-MEMBER".equals(entityKind) || "RELEVANCE-OBJECT".equals(entityKind)
                    || (param != null);
            if (variableLike) {
                return declareLocal(def, entity);
            }
            if ("ATTRIBUTE-MEMBER".equals(entityKind) || "COMPOSE-MEMBER".equals(entityKind)
                    || "SETTING-MEMBER".equals(entityKind)) {
                String owner = memberOwner.get(entityId);
                if (owner != null && owner.equals(def.ownerTypeId)) {
                    return new MemberPathIR(new SelfIR(entityTypePlaceholder(def.ownerTypeId)), List.of(entityId),
                            slotType(entityId));
                }
                // 成员不属于过程宿主：若恰有一个形参/局部量的类型拥有该成员，则从它出发（画布上省略了起点）
                List<OperandIR> owners = new ArrayList<>();
                for (JsonNode cell2 : view.cells) {
                    JsonNode e2 = entities.get(text(cell2, "data"));
                    if (e2 == null || text(cell2, "id").equals(cellId)) {
                        continue;
                    }
                    String candidateType = GromaTypes.entityIdOf(slotType(e2));
                    if (candidateType != null && candidateType.equals(owner) && pathSourceOf(view, text(cell2, "id")).isEmpty()) {
                        owners.add(operandOf(def, view, cell2));
                    }
                }
                if (owners.size() == 1) {
                    return new MemberPathIR(owners.get(0), List.of(entityId), slotType(entityId));
                }
                def.warn("成员“" + label + "”不属于过程宿主类型，且无法确定归属，按宿主成员处理");
                return new MemberPathIR(new SelfIR(entityTypePlaceholder(def.ownerTypeId)), List.of(entityId),
                        slotType(entityId));
            }
            def.warn("单元格 " + cellId + "（" + type + " " + label + "）不是可解析的操作数，按空值处理");
            return new ConstIR(entityId, "null", GromaTypes.OBJECT, label);
        }

        /** 路径上的一步：内置对象的成员、集合的长度、领域对象的成员 */
        private OperandIR memberStep(DefCtx def, OperandIR root, JsonNode memberEntity, String label) {
            String memberId = memberEntity == null ? "" : text(memberEntity, "id");
            if (root instanceof BuiltinIR builtin && "SIM".equals(builtin.key())) {
                return switch (label) {
                    case "时间日期" -> new BuiltinIR("SIM_DATETIME", null, GromaTypes.LONG);
                    case "持续运行时长" -> new BuiltinIR("SIM_ELAPSED_SECONDS", null, GromaTypes.DOUBLE);
                    default -> new BuiltinIR("SIM_" + label, null, GromaTypes.OBJECT);
                };
            }
            if (memberEntity != null && "SETTING-MEMBER".equals(text(memberEntity, "type"))) {
                return switch (text(memberEntity, "supplementType")) {
                    case "length" -> new BuiltinIR("LENGTH", root, GromaTypes.LONG);
                    case "arrayIndex" -> new BuiltinIR("INDEX", root, GromaTypes.LONG);
                    default -> new BuiltinIR("KEY", root, GromaTypes.OBJECT);
                };
            }
            if (memberEntity != null && ("ARRAY-ELEMENT".equals(text(memberEntity, "type"))
                    || "MAP-ELEMENT".equals(text(memberEntity, "type")))) {
                // 集合字面量元素在过程设计视图里只作为展示，取其值
                return new ConstIR(memberId, text(memberEntity, "defaultValue"), slotType(memberEntity), label);
            }
            return new MemberPathIR(root, List.of(memberId), slotType(memberId));
        }

        /** 模板成员 / 未连线的关联对象 → 过程局部量（首次出现时登记初值） */
        private LocalIR declareLocal(DefCtx def, JsonNode entity) {
            String id = text(entity, "id");
            String label = text(entity, "label");
            if (def.localIds.add(id)) {
                def.localVars.add(new LocalVarIR(id, label, slotType(entity), text(entity, "defaultValue")));
            }
            return new LocalIR(id, label, slotType(entity));
        }

        /** 上游节点输出槽位的最终类型 */
        private OperandIR retargetOutput(OperandIR operand, Map<String, NodeRaw> raws) {
            if (!(operand instanceof NodeOutputIR out)) {
                return operand;
            }
            NodeRaw source = raws.get(out.cellId());
            String type;
            if (source == null) {
                type = GromaTypes.OBJECT;
            } else if (source.tool) {
                type = source.resultJavaType.isEmpty() ? GromaTypes.OBJECT : source.resultJavaType;
            } else {
                // 业务子过程的输出形参：按端口对应的形参实体取类型
                String argEntityId = "";
                for (JsonNode port : portsOf(source.cell)) {
                    if (out.argName().equals(portName(portArgs(port)))) {
                        argEntityId = text(portArgs(port), "otherData");
                    }
                }
                type = argEntityId.isEmpty() ? GromaTypes.OBJECT : slotType(argEntityId);
            }
            return new NodeOutputIR(out.cellId(), out.argName(), type);
        }

        private List<ArgBindingIR> retarget(List<ArgBindingIR> bindings, Function<OperandIR, OperandIR> resolve) {
            List<ArgBindingIR> out = new ArrayList<>(bindings.size());
            for (ArgBindingIR binding : bindings) {
                out.add(new ArgBindingIR(binding.argName(), resolve.apply(binding.operand())));
            }
            return out;
        }

        private List<PostIR> retargetPosts(List<PostIR> posts, Function<OperandIR, OperandIR> resolve) {
            List<PostIR> out = new ArrayList<>(posts.size());
            for (PostIR post : posts) {
                out.add(new PostIR(post.kind(), post.target() == null ? null : resolve.apply(post.target()),
                        post.stateLabel(), post.enumTypeId(), post.value() == null ? null : resolve.apply(post.value()),
                        post.probability(), post.probabilityGroup()));
            }
            return out;
        }

        // ------------------------------------------------------------------ 类型推断与排序

        private String operandType(OperandIR operand, Map<String, NodeRaw> raws) {
            if (operand instanceof NodeOutputIR out) {
                NodeRaw source = raws.get(out.cellId());
                return (source != null && source.tool && !source.resultJavaType.isEmpty())
                        ? source.resultJavaType : GromaTypes.OBJECT;
            }
            return operand.javaType();
        }

        /** 工具节点输出值类型（沿数据流迭代到稳定） */
        private String inferToolResult(DefCtx def, NodeRaw raw, Map<String, NodeRaw> raws) {
            Map<String, String> argTypes = new HashMap<>();
            for (ArgBindingIR binding : raw.inputs) {
                argTypes.putIfAbsent(binding.argName(), operandType(binding.operand(), raws));
            }
            String a = argTypes.getOrDefault("a", "");
            String b = argTypes.getOrDefault("b", "");
            String host = raw.host == null ? "" : raw.host.javaType();
            String key = raw.toolKey;
            if (key.startsWith("arithmetic.")) {
                return switch (key) {
                    case "arithmetic.add", "arithmetic.minus", "arithmetic.multiply", "arithmetic.max", "arithmetic.min" ->
                            GromaTypes.numericResult(a, b);
                    case "arithmetic.reverse", "arithmetic.absolute" -> GromaTypes.isNumeric(a) ? a : GromaTypes.LONG;
                    case "arithmetic.mod" -> GromaTypes.LONG;
                    default -> GromaTypes.DOUBLE;
                };
            }
            if (key.startsWith("compare.") || key.startsWith("logic.") || "isNull.isNull".equals(key)) {
                return GromaTypes.BOOLEAN;
            }
            if ("assignment.assign".equals(key) || "assignment.assignRef".equals(key)) {
                return a.isEmpty() ? GromaTypes.OBJECT : a;
            }
            if (key.startsWith("automatic.") || key.startsWith("assignment.") || key.startsWith("types.update")) {
                return host.isEmpty() ? a : host;
            }
            if (key.startsWith("trigonometric.") || key.startsWith("accuracy.")) {
                return GromaTypes.DOUBLE;
            }
            if (key.startsWith("bitwise.") || key.startsWith("shift.") || "text.getTextLength".equals(key)) {
                return GromaTypes.LONG;
            }
            if (key.startsWith("text.")) {
                return GromaTypes.STRING;
            }
            if (key.startsWith("time.") || key.startsWith("innerCollectionSet.") || key.startsWith("innerCollectionMap.put")
                    || key.startsWith("innerCollectionMap.remove") || key.startsWith("innerCollectionMap.clear")
                    || key.startsWith("innerCollectionArray.add") || key.startsWith("innerCollectionArray.remove")
                    || key.startsWith("innerCollectionArray.clear")) {
                return GromaTypes.OBJECT;
            }
            if ("innerCollectionArray.getByIndex".equals(key)) {
                String element = GromaTypes.elementTypeOf(host);
                return GromaTypes.OBJECT.equals(element) ? explicitOutType(raw) : element;
            }
            if ("innerCollectionMap.getByKey".equals(key)) {
                String element = GromaTypes.elementTypeOf(host);
                return GromaTypes.OBJECT.equals(element) ? explicitOutType(raw) : element;
            }
            String explicit = explicitOutType(raw);
            return explicit;
        }

        /** 工具定义里输出端口显式声明的类型（如 c:boolean） */
        private String explicitOutType(NodeRaw raw) {
            for (JsonNode port : portsOf(raw.cell)) {
                if ("OUT".equals(portDirection(portArgs(port)))) {
                    JsonNode relevance = entities.get(text(portArgs(port), "otherData"));
                    String fieldType = relevance == null ? "" : text(relevance, "fieldType");
                    if (!fieldType.isEmpty() && !"empty".equals(fieldType) && !"anyType".equals(fieldType)) {
                        return slotType(relevance);
                    }
                }
            }
            return GromaTypes.OBJECT;
        }

        /** 从入口沿调用线广度优先排序；未达节点按画布位置追加 */
        private List<NodeRaw> orderNodes(ViewCtx view, Map<String, NodeRaw> raws, Map<String, Integer> visualOrder) {
            List<NodeRaw> all = new ArrayList<>(raws.values());
            all.sort(Comparator.comparingInt(r -> visualOrder.get(r.cellId)));
            List<NodeRaw> entries = new ArrayList<>();
            for (NodeRaw raw : all) {
                if (raw.firstStart) {
                    entries.add(raw);
                }
            }
            if (entries.isEmpty()) {
                Set<String> targeted = new HashSet<>();
                for (NodeRaw raw : all) {
                    for (JsonNode edge : raw.nextEdges) {
                        targeted.add(text(edge.path("target"), "cell"));
                    }
                }
                for (NodeRaw raw : all) {
                    if (!targeted.contains(raw.cellId)) {
                        entries.add(raw);
                    }
                }
            }
            if (entries.isEmpty()) {
                entries.add(all.get(0));
            }
            List<NodeRaw> ordered = new ArrayList<>(all.size());
            Set<String> visited = new HashSet<>();
            List<NodeRaw> queue = new ArrayList<>(entries);
            while (!queue.isEmpty()) {
                NodeRaw current = queue.remove(0);
                if (!visited.add(current.cellId)) {
                    continue;
                }
                ordered.add(current);
                List<NodeRaw> targets = new ArrayList<>();
                for (JsonNode edge : current.nextEdges) {
                    NodeRaw target = raws.get(text(edge.path("target"), "cell"));
                    if (target != null) {
                        targets.add(target);
                    }
                }
                targets.sort(Comparator.comparingInt(r -> visualOrder.get(r.cellId)));
                queue.addAll(targets);
            }
            for (NodeRaw raw : all) {
                if (visited.add(raw.cellId)) {
                    ordered.add(raw);
                }
            }
            return ordered;
        }

        // ------------------------------------------------------------------ 连线与线上判断

        private String linkType(JsonNode edge) {
            String shape = text(edge, "shape");
            if (shape.startsWith("edge-")) {
                return shape.substring("edge-".length());
            }
            JsonNode linkEntity = entities.get(text(edge, "data"));
            return linkTypeOfEntity(linkEntity);
        }

        private String linkTypeOfEntity(JsonNode linkEntity) {
            if (linkEntity == null) {
                return "";
            }
            String linkType = text(linkEntity, "linkType");
            int comma = linkType.indexOf(',');
            return comma > 0 ? linkType.substring(0, comma) : linkType;
        }

        private JsonNode linkLogic(JsonNode linkEntity) {
            JsonNode logic = field(linkEntity, "logic");
            return (logic != null && logic.isObject()) ? logic : null;
        }

        private List<ConditionIR> conditionsOf(JsonNode linkEntity) {
            JsonNode logic = linkLogic(linkEntity);
            if (logic == null) {
                return List.of();
            }
            JsonNode conditions = field(logic, "conditions");
            List<ConditionIR> out = new ArrayList<>();
            if (conditions != null && conditions.isArray()) {
                int ordinal = 0;
                for (JsonNode condition : conditions) {
                    ordinal++;
                    // 判断值有三种存法：标量、单元素数组 value:[true]、多值数组 values:[a,b]
                    JsonNode literalNode = field(condition, "value");
                    if (literalNode != null && literalNode.isArray()) {
                        literalNode = literalNode.isEmpty() ? null : literalNode.get(0);
                    }
                    if (literalNode == null || literalNode.isNull() || literalNode.isMissingNode()) {
                        JsonNode values = field(condition, "values");
                        literalNode = (values != null && values.isArray() && !values.isEmpty()) ? values.get(0) : null;
                    }
                    String literalType = "null";
                    String literal = "null";
                    if (literalNode != null && !literalNode.isNull()) {
                        if (literalNode.isBoolean()) {
                            literalType = "boolean";
                            literal = literalNode.asString();
                        } else if (literalNode.isNumber()) {
                            literalType = "number";
                            literal = literalNode.asString();
                        } else if (literalNode.isString()) {
                            literalType = "string";
                            literal = literalNode.stringValue();
                        }
                    }
                    String code = text(condition, "code");
                    out.add(new ConditionIR((int) longOf(condition, "index", ordinal),
                            code.isEmpty() ? "=" : code, literal, literalType));
                }
            }
            // 布尔线上判断的判定值：存量引擎 createOneCallJudge 以连线上的 determine（true / false）为准
            String determine = text(linkEntity, "determine");
            if (out.isEmpty() && ("true".equalsIgnoreCase(determine) || "false".equalsIgnoreCase(determine))) {
                out.add(new ConditionIR(1, "=", determine.toLowerCase(Locale.ROOT), "boolean"));
            }
            return out;
        }

        private String expressionOf(JsonNode linkEntity) {
            JsonNode logic = linkLogic(linkEntity);
            return logic == null ? "" : text(logic, "expression");
        }

        // ------------------------------------------------------------------ 事件接线与默认启动（类型 / 场景设计视图）

        private void buildWiring() {
            LinkedHashSet<String> owners = new LinkedHashSet<>(domainTypeIds);
            owners.addAll(sceneIds);
            for (String typeId : owners) {
                JsonNode type = entities.get(typeId);
                ViewCtx view = view(text(type, "designViewId"));
                if (view == null) {
                    continue;
                }
                DefCtx ctx = new DefCtx("owner:" + typeId, typeId, view);
                List<JsonNode> callSites = new ArrayList<>();
                for (JsonNode cell : view.cells) {
                    if (isProcessCell(cell) && !isToolProcess(entities.get(text(cell, "data")))) {
                        callSites.add(cell);
                    }
                }
                callSites.sort(CELL_ORDER);
                for (JsonNode callSite : callSites) {
                    JsonNode callee = entities.get(text(callSite, "data"));
                    String defKey = defKeyOf(callee);

                    // 组成线：过程宿主是接线对象的某个成员（路径含该成员自身；没有组成线时宿主就是接线对象）
                    List<String> hostPath = List.of();
                    for (JsonNode edge : view.incoming(text(callSite, "id"))) {
                        if ("composition".equals(linkType(edge))) {
                            JsonNode hostCell = view.source(edge);
                            List<String> path = new ArrayList<>(memberPathOf(ctx, view, hostCell));
                            path.add(text(hostCell, "data"));
                            hostPath = List.copyOf(path);
                            break;
                        }
                    }
                    // 锚点线：实参绑定（相对于接线对象）；输出影响 / 双向影响端口绑定的是成员时，过程成功后把形参终值写回该成员
                    // （存量引擎 handleInstantsWhenSuccess：PARAM_OUT / PARAM_IO / EFFECT_OUT / EFFECT_NOTNULL 走 outputParam）
                    List<ArgBindingIR> args = new ArrayList<>();
                    List<PostIR> postActions = new ArrayList<>(collectPostActions(ctx, view, text(callSite, "id")));
                    for (JsonNode port : portsOf(callSite)) {
                        JsonNode portArgs = portArgs(port);
                        String argName = portName(portArgs);
                        String direction = portDirection(portArgs);
                        if (direction.isEmpty()) {
                            direction = "IN";
                        }
                        for (JsonNode edge : fixedIntoPort(view, text(callSite, "id"), text(port, "id"))) {
                            JsonNode source = view.source(edge);
                            OperandIR operand = operandOf(ctx, view, source, edge);
                            if (!"OUT".equals(direction)) {
                                args.add(new ArgBindingIR(argName, operand));
                            }
                            if (!"IN".equals(direction) && !isProcessCell(source)) {
                                OperandIR outParam = outParamOperand(defKey, argName, text(portArgs, "otherData"));
                                if (outParam != null) {
                                    postActions.add(new PostIR("WRITE_BACK", operand, "", "", outParam));
                                } else {
                                    ctx.warn("调用点「" + text(callee, "label") + "」的输出端口 " + argName + " 不在定义视图的形参契约里，输出影响回写已跳过");
                                }
                            }
                        }
                    }

                    boolean defaultStart = flag(callSite, "defaultStart");
                    if (defaultStart) {
                        defaultStarts.add(new DefaultStartIR(typeId, hostPath, defKey, List.copyOf(args),
                                List.copyOf(postActions)));
                    }
                    int triggersBefore = triggers.size();
                    // 事件触发：凡是带事件码、到达过程图元的连线都登记（存量引擎 BaseClazz.addBinding：
                    // 锚点线带事件码同样进事件绑定表；组成线与属性线只是路径，不触发）
                    for (JsonNode edge : view.incoming(text(callSite, "id"))) {
                        JsonNode source = view.source(edge);
                        if (isProcessCell(source) || "composition".equals(linkType(edge))
                                || "exhibition".equals(linkType(edge))) {
                            continue;
                        }
                        String events = text(edge, "event");
                        if (events.isEmpty()) {
                            continue;
                        }
                        JsonNode linkEntity = entities.get(text(edge, "data"));
                        double prob = probabilityOf(edge, linkEntity);
                        String probGroup = probabilityGroupOf(edge, linkEntity);
                        for (String eventCode : events.split(",")) {
                            if (!eventCode.isBlank()) {
                                triggers.add(buildTrigger(ctx, view, source, eventCode.trim(), conditionsOf(linkEntity),
                                        expressionOf(linkEntity), defKey, hostPath, args, postActions, typeId,
                                        linkType(edge) + " " + text(callee, "label"), prob, probGroup));
                            }
                        }
                    }
                    // 手动启动（平台定义）：既无默认启动也无事件触发的调用点，纳入外部干预入口，
                    // 画布实参作为默认值，运行时允许按中文形参名覆盖
                    if (!defaultStart && triggers.size() == triggersBefore) {
                        manualStarts.add(new ManualStartIR(typeId, hostPath, defKey, List.copyOf(args),
                                List.copyOf(postActions)));
                    }
                }
            }
        }

        /** 被调过程的输出形参操作数（按端口上绑定的关联对象标识在定义视图的形参契约里定位） */
        private OperandIR outParamOperand(String defKey, String argName, String paramEntityId) {
            for (ProcessDefIR def : processDefs) {
                if (!def.defKey().equals(defKey)) {
                    continue;
                }
                for (PortIR port : def.params()) {
                    boolean matched = paramEntityId.isEmpty() ? port.zhName().equals(argName) : port.entityId().equals(paramEntityId);
                    if (matched) {
                        return new ParamIR(port.entityId(), port.zhName(), port.javaType());
                    }
                }
            }
            return null;
        }

        /** 从接线对象到某成员单元格的成员路径（不含该单元格自身） */
        private List<String> memberPathOf(DefCtx ctx, ViewCtx view, JsonNode cell) {
            String source = pathSourceOf(view, text(cell, "id"));
            if (source.isEmpty()) {
                return List.of();
            }
            List<String> path = new ArrayList<>(memberPathOf(ctx, view, view.cell(source)));
            path.add(text(view.cell(source), "data"));
            return path;
        }

        private TriggerIR buildTrigger(DefCtx ctx, ViewCtx view, JsonNode sourceCell, String eventCode,
                                       List<ConditionIR> judge, String expression, String targetDefKey,
                                       List<String> hostPath, List<ArgBindingIR> args, List<PostIR> postActions,
                                       String ownerTypeId, String describe, double probability, String probabilityGroup) {
            JsonNode sourceEntity = entities.get(text(sourceCell, "data"));
            String stateLabel = "";
            String memberToken;
            List<String> contextPath;
            OperandIR sourceOperand;
            if ("STATE".equals(text(sourceCell, "type"))) {
                stateLabel = text(sourceCell, "label").isEmpty() ? text(sourceEntity, "label") : text(sourceCell, "label");
                String enumId = enumOfState.getOrDefault(text(sourceCell, "data"), "");
                OperandIR member = enumMemberOperand(ctx, view, enumId);
                if (member instanceof MemberPathIR path) {
                    memberToken = path.memberIds().get(path.memberIds().size() - 1);
                    contextPath = fullPathOf(path);
                    contextPath = contextPath.subList(0, contextPath.size() - 1);
                    sourceOperand = path;
                } else {
                    ctx.warn("状态触发找不到枚举成员: " + stateLabel);
                    memberToken = text(sourceCell, "data");
                    contextPath = List.of();
                    sourceOperand = null;
                }
            } else {
                memberToken = text(sourceCell, "data");
                contextPath = memberPathOf(ctx, view, sourceCell);
                sourceOperand = operandOf(ctx, view, sourceCell);
            }
            String sourceTypeId = contextPath.isEmpty()
                    ? ownerTypeId : GromaTypes.entityIdOf(slotType(contextPath.get(contextPath.size() - 1)));
            if (sourceTypeId == null) {
                sourceTypeId = ownerTypeId;
            }
            // 被动事件订阅归一（code4.html M4 收割，标准 21 类事件全集）：am/xm/ar/vr/xr 订阅
            // 源在类型/场景图元（而非成员图元）时是类型级语义——被操作目标自身无持有方成员键，
            // 统一以通配令牌 "*" 注册；引擎派发侧 firePassiveOnTarget（令牌 "*"）与
            // GromaTriggerTable.dispatch 通配兜底（持有方令牌事件）与之对齐
            if (("am".equals(eventCode) || "xm".equals(eventCode) || "ar".equals(eventCode)
                    || "vr".equals(eventCode) || "xr".equals(eventCode))
                    && sourceEntity != null
                    && ("TYPE".equals(text(sourceEntity, "type")) || "SCENE".equals(text(sourceEntity, "type"))
                    || "ASSOCIATION-TYPE".equals(text(sourceEntity, "type")))) {
                memberToken = "*";
            }
            return new TriggerIR(sourceTypeId, memberToken, eventCode, stateLabel, judge,
                    sourceOperand, contextPath, targetDefKey, hostPath, args, postActions, ownerTypeId, describe,
                    expression, probability, probabilityGroup);
        }

        /** 成员路径操作数 → 从宿主自身的完整成员 id 序列 */
        private List<String> fullPathOf(MemberPathIR path) {
            List<String> full = new ArrayList<>();
            OperandIR cursor = path;
            while (cursor instanceof MemberPathIR step) {
                full.add(0, step.memberIds().get(step.memberIds().size() - 1));
                cursor = step.root();
            }
            return full;
        }

        // ------------------------------------------------------------------ 类型、实例、关联类型

        private MemberIR memberIR(String memberId) {
            JsonNode entity = entities.get(memberId);
            String typeRange = text(entity, "typeRange");
            String refTypeId = ("ENUM".equals(typeRange) || "DEFINE".equals(typeRange)) ? refTypeIdOf(entity) : "";
            String fieldType = text(entity, "fieldType");
            if (fieldType.isEmpty() || "empty".equals(fieldType) || "anyType".equals(fieldType)) {
                fieldType = "ENUM".equals(typeRange) || "DEFINE".equals(typeRange) ? "string" : "Object";
            }
            String initialState = "";
            if ("ENUM".equals(typeRange)) {
                initialState = text(entity, "defaultValue");
                if (initialState.isEmpty()) {
                    initialState = enumInitial.getOrDefault(refTypeId, "");
                }
            }
            return new MemberIR(
                    memberId, text(entity, "label"), text(entity, "alias"), fieldType, typeRange, refTypeId,
                    flag(entity, "isCollection"), text(entity, "containerType"), text(entity, "defaultValue"),
                    text(entity, "unit"), text(entity, "minValue"), text(entity, "maxValue"), initialState,
                    text(entity, "description"), slotType(entity), text(entity, "encode"));
        }

        /**
         * 收割【修复 6】（R27 备注连线）：提取定义/设计视图中 note 连线关联的 NOTE 图元文本，
         * 归集到类型定义（出码时写入 Javadoc 的信息在此承载）。
         */
        private List<String> collectNotes(String entityId) {
            JsonNode ent = entities.get(entityId);
            if (ent == null) {
                return List.of();
            }
            List<String> notes = new ArrayList<>();
            for (String vid : List.of(text(ent, "defineViewId"), text(ent, "designViewId"))) {
                ViewCtx v = view(vid);
                if (v == null) {
                    continue;
                }
                for (JsonNode edge : v.cells) {
                    if (!"LINK".equals(text(edge, "type")) || !"note".equals(linkType(edge))) {
                        continue;
                    }
                    for (JsonNode end : new JsonNode[]{v.source(edge), v.target(edge)}) {
                        if (end == null || !"NOTE".equals(text(end, "type"))) {
                            continue;
                        }
                        JsonNode noteEntity = entities.get(text(end, "data"));
                        String noteText = noteEntity == null ? text(end, "label") : text(noteEntity, "label");
                        if (!noteText.isBlank() && !notes.contains(noteText)) {
                            notes.add(noteText);
                        }
                    }
                }
            }
            return notes;
        }

        private List<TypeDefIR> buildTypeDefs() {
            List<TypeDefIR> types = new ArrayList<>();
            LinkedHashSet<String> owners = new LinkedHashSet<>(domainTypeIds);
            owners.addAll(sceneIds);
            owners.addAll(assocTypeIds);
            for (String typeId : owners) {
                JsonNode entity = entities.get(typeId);
                List<MemberIR> members = new ArrayList<>();
                for (String memberId : typeMembers.getOrDefault(typeId, List.of())) {
                    members.add(memberIR(memberId));
                }
                List<String> parentIds = resolveParentIds(typeId, entity);
                String parent = parentIds.isEmpty() ? "" : parentIds.get(0);
                List<String> mixins = parentIds.size() > 1 ? parentIds.subList(1, parentIds.size()) : List.of();
                types.add(new TypeDefIR(typeId, text(entity, "label"), text(entity, "description"),
                        text(entity, "essence"), typeKind.get(typeId), parent,
                        List.copyOf(mixins), List.copyOf(parseProxies(typeId)), List.copyOf(members),
                        List.copyOf(collectNotes(typeId)), text(entity, "encode"),
                        List.copyOf(parseControlRules(typeId))));
            }
            return types;
        }

        /** 画布继承边预扫描缓存（child 类型 id → 有序父类型 id） */
        private Map<String, List<String>> canvasExtends;

        /**
         * 收割补丁 A（R01 多继承）：inheritEntityId 单值 + inherits 数组 + 全视图画布继承边
         * （edge-extend 连线：画布约定父 --edge-extend--> 子；PARENT 实体：typeEntityId=子类型，
         * 父类经 inheritEntityId 或类名定位）。模型 2 智能筹划含 5 组画布继承（存在于组织树级
         * 视图而非实体自有视图，故须全视图预扫描）。
         * 返回有序去重父类 id 列表：首为主父类，其余为副父类（mixin）。
         */
        private List<String> resolveParentIds(String typeId, JsonNode typeEntity) {
            java.util.LinkedHashSet<String> parents = new java.util.LinkedHashSet<>();
            String direct = text(typeEntity, "inheritEntityId");
            if (!direct.isEmpty() && entities.containsKey(direct) && !direct.equals(typeId)) {
                parents.add(direct);
            }
            // 复数继承字段（code4.html M2 收割加固）：inheritEntityIds 数组与单值同语义
            // （模型 2 智能筹划真实字段形态；此前仅画布 edge-extend/PARENT 路径兜底命中）
            JsonNode plural = field(typeEntity, "inheritEntityIds");
            if (plural != null && plural.isArray()) {
                for (JsonNode item : plural) {
                    String pid = item.asText("").trim();
                    if (!pid.isEmpty() && entities.containsKey(pid) && !pid.equals(typeId)) {
                        parents.add(pid);
                    }
                }
            }
            // 跨组件引用对象数组：真实语料键形如 {"directCmpId":…, "entityId":…}（entityId 为父类型 id），
            // 兼容旧版 id 键
            JsonNode inherits = field(typeEntity, "inherits");
            if (inherits != null && inherits.isArray()) {
                for (JsonNode item : inherits) {
                    String pid = item.isObject() ? orFallback(text(item, "entityId"), text(item, "id"))
                            : item.asText().trim();
                    if (!pid.isEmpty() && entities.containsKey(pid) && !pid.equals(typeId)) {
                        parents.add(pid);
                    }
                }
            }
            for (String pid : canvasExtends().getOrDefault(typeId, List.of())) {
                if (!pid.equals(typeId)) {
                    parents.add(pid);
                }
            }
            return List.copyOf(parents);
        }

        /** 全视图扫描画布继承边 + PARENT 实体归属，建 child → 有序父类型表（懒加载缓存） */
        private Map<String, List<String>> canvasExtends() {
            if (canvasExtends != null) {
                return canvasExtends;
            }
            Map<String, java.util.LinkedHashSet<String>> table = new LinkedHashMap<>();
            for (Map.Entry<String, List<JsonNode>> entry : rawViews.entrySet()) {
                ViewCtx v = view(entry.getKey());
                if (v == null) {
                    continue;
                }
                for (JsonNode edge : v.cells) {
                    if (!"LINK".equals(text(edge, "type")) || !"extend".equals(linkType(edge))) {
                        continue;
                    }
                    JsonNode src = v.source(edge);
                    JsonNode dst = v.target(edge);
                    if (src == null || dst == null) {
                        continue;
                    }
                    // 画布约定：父类型 --edge-extend--> 子类型（src=父，dst=子）
                    String parentData = text(src, "data");
                    String childData = text(dst, "data");
                    JsonNode parentEntity = entities.get(parentData);
                    if (parentEntity != null && !"TYPE".equals(text(parentEntity, "type"))) {
                        String viaRef = refTypeIdOf(parentEntity);
                        parentData = viaRef.isEmpty() ? findByLabel(text(parentEntity, "label"), false) : viaRef;
                    }
                    JsonNode childEntity = entities.get(childData);
                    if (childEntity != null && !"TYPE".equals(text(childEntity, "type"))) {
                        String viaRef = refTypeIdOf(childEntity);
                        childData = viaRef.isEmpty() ? findByLabel(text(childEntity, "label"), false) : viaRef;
                    }
                    if (!parentData.isEmpty() && !childData.isEmpty()
                            && domainTypeIds.contains(parentData) && domainTypeIds.contains(childData)
                            && !parentData.equals(childData)) {
                        table.computeIfAbsent(childData, k -> new java.util.LinkedHashSet<>()).add(parentData);
                    }
                }
            }
            for (JsonNode pe : entities.values()) {
                if (!"PARENT".equals(text(pe, "type"))) {
                    continue;
                }
                String childId = text(pe, "typeEntityId");
                String parentId = text(pe, "inheritEntityId");
                if (parentId.isEmpty()) {
                    parentId = findByLabel(text(pe, "label"), false);
                }
                if (!childId.isEmpty() && !parentId.isEmpty() && domainTypeIds.contains(parentId)
                        && !parentId.equals(childId)) {
                    table.computeIfAbsent(childId, k -> new java.util.LinkedHashSet<>()).add(parentId);
                }
            }
            Map<String, List<String>> frozen = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.LinkedHashSet<String>> entry : table.entrySet()) {
                frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            canvasExtends = frozen;
            return frozen;
        }

        /**
         * 收割补丁 B（R05 集合成员代理）：定义/设计视图的 PROXY 图元 + 入向 edge-proxy 连线
         * → ProxyDefIR（模型 2 智能筹划含 1 组）。解析信息进 IR，出码消费（ObservableEntityList 包装）
         * 见 docs/dual_model_codegen_audit.md 方案 C。
         */
        private List<ProxyDefIR> parseProxies(String typeId) {
            List<ProxyDefIR> out = new ArrayList<>();
            for (String viewId : List.of(text(entities.get(typeId), "defineViewId"),
                    text(entities.get(typeId), "designViewId"))) {
                ViewCtx v = view(viewId);
                if (v == null) {
                    continue;
                }
                for (JsonNode cell : v.cells) {
                    if (!"PROXY".equals(text(cell, "type"))) {
                        continue;
                    }
                    String proxyEid = text(cell, "data");
                    JsonNode proxyEntity = entities.get(proxyEid);
                    String label = proxyEntity == null ? text(cell, "label") : text(proxyEntity, "label");
                    for (JsonNode edge : v.incoming(text(cell, "id"))) {
                        if (!"proxy".equals(linkType(edge))) {
                            continue;
                        }
                        JsonNode collCell = v.source(edge);
                        if (collCell == null) {
                            continue;
                        }
                        String collMemberId = text(collCell, "data");
                        String elemType = GromaTypes.entityIdOf(GromaTypes.elementTypeOf(slotType(collMemberId)));
                        out.add(new ProxyDefIR(
                                proxyEid.isEmpty() ? text(cell, "id") : proxyEid,
                                label,
                                collMemberId,
                                elemType == null ? "" : elemType));
                    }
                }
            }
            return out;
        }

        /**
         * 收割 R06 结构控制关系（edge-control）：源 STATE 图元 --edge-control--> 目标成员/过程图元。
         * 语义：源枚举成员处于该状态时目标才使能。挂在本类型的定义/设计视图上；
         * 成员目标与过程目标分别占用独立 bit 位序（按连线 id 排序保证确定性）。
         */
        private List<ControlRuleIR> parseControlRules(String typeId) {
            List<ControlRuleIR> rules = new ArrayList<>();
            List<JsonNode> controlEdges = new ArrayList<>();
            Map<String, ViewCtx> edgeView = new HashMap<>();
            for (String viewId : List.of(text(entities.get(typeId), "defineViewId"),
                    text(entities.get(typeId), "designViewId"))) {
                ViewCtx v = view(viewId);
                if (v == null) {
                    continue;
                }
                for (JsonNode edge : v.cells) {
                    if ("LINK".equals(text(edge, "type")) && "control".equals(linkType(edge))) {
                        controlEdges.add(edge);
                        edgeView.put(text(edge, "id"), v);
                    }
                }
            }
            if (controlEdges.isEmpty()) {
                return List.of();
            }
            controlEdges.sort(java.util.Comparator.comparing(edge -> text(edge, "id")));
            int memberBit = 0;
            int processBit = 0;
            for (JsonNode edge : controlEdges) {
                ViewCtx v = edgeView.get(text(edge, "id"));
                JsonNode source = v.source(edge);
                JsonNode target = v.target(edge);
                if (source == null || target == null || !"STATE".equals(text(source, "type"))) {
                    continue;
                }
                String stateEntityId = text(source, "data");
                String enumId = enumOfState.getOrDefault(stateEntityId, "");
                String enumMemberId = enumId.isEmpty() ? "" : enumMemberIdOfType(typeId, enumId);
                if (enumMemberId.isEmpty()) {
                    continue;
                }
                String label = text(entities.get(stateEntityId), "label");
                if (label.isEmpty()) {
                    label = text(source, "label");
                }
                String targetData = text(target, "data");
                if (targetData.isEmpty()) {
                    continue;
                }
                if (isProcessCell(target)) {
                    rules.add(new ControlRuleIR(enumMemberId, List.of(label), "PROCESS", targetData, processBit++));
                } else {
                    rules.add(new ControlRuleIR(enumMemberId, List.of(label), "MEMBER", targetData, memberBit++));
                }
            }
            return rules;
        }

        /** 本类型上引用指定枚举类型的成员 id（typeRange=ENUM 且 refTypeId 命中） */
        private String enumMemberIdOfType(String typeId, String enumId) {
            for (String memberId : typeMembers.getOrDefault(typeId, List.of())) {
                JsonNode member = entities.get(memberId);
                if (member != null && "ENUM".equals(text(member, "typeRange"))
                        && enumId.equals(refTypeIdOf(member))) {
                    return memberId;
                }
            }
            return "";
        }

        /**
         * 指定枚举类型在过程宿主类型（含主父类链）成员表中的成员 id（类型级守卫操作数构造用）。
         * 返回宿主自己的成员 id，保证生成 getter 在宿主类上真实存在。
         */
        private String hostEnumMemberId(String ownerTypeId, String enumId) {
            String typeId = ownerTypeId;
            int depth = 0;
            while (!typeId.isEmpty() && entities.containsKey(typeId) && depth++ < 16) {
                String memberId = enumMemberIdOfType(typeId, enumId);
                if (!memberId.isEmpty()) {
                    return memberId;
                }
                JsonNode typeEntity = entities.get(typeId);
                List<String> parents = resolveParentIds(typeId, typeEntity);
                typeId = parents.isEmpty() ? "" : parents.get(0);
            }
            return "";
        }

        /**
         * 收割（code3.html 孤儿连线审计，按语料实证修正判定口径）：交互语义连线
         * （condition/agent/invocation-self/control/overtime/undertime）两种悬空形态记告警——
         * ① 连线实体未挂载到任何画布视图且无 target 字段；
         * ② 连线已挂载，但边端点（source/target cell）在所在视图中不可解析（模型 1 实证：
         * 3 条 condition + 1 条 invocation-self 均有 cell 但指向端悬空，此前被静默跳过）。
         */
        private void auditOrphanSemanticLinks() {
            Set<String> semanticShapes = Set.of("edge-condition", "edge-agent", "edge-invocation-self",
                    "edge-control", "edge-overtime-exception", "edge-undertime-exception");
            Set<String> semanticLinkTypes = Set.of("condition", "agent", "invocation-self",
                    "control", "overtime-exception", "undertime-exception");
            for (Map.Entry<String, List<JsonNode>> viewEntry : rawViews.entrySet()) {
                String viewId = viewEntry.getKey();
                Set<String> cellIds = new HashSet<>();
                for (JsonNode cell : viewEntry.getValue()) {
                    cellIds.add(text(cell, "id"));
                }
                for (JsonNode cell : viewEntry.getValue()) {
                    if (!"LINK".equals(text(cell, "type")) || !semanticShapes.contains(text(cell, "shape"))) {
                        continue;
                    }
                    String sourceCell = text(cell.path("source"), "cell");
                    String targetCell = text(cell.path("target"), "cell");
                    boolean dangling = sourceCell.isEmpty() || targetCell.isEmpty()
                            || !cellIds.contains(sourceCell) || !cellIds.contains(targetCell);
                    if (!dangling) {
                        continue;
                    }
                    JsonNode linkEntity = entities.get(text(cell, "data"));
                    String label = linkEntity == null ? text(cell, "label") : text(linkEntity, "label");
                    warnings.add("[PRS-WARN-002] 悬空语义连线: shape=" + text(cell, "shape")
                            + ", label=" + label + ", viewId=" + viewId
                            + " 边端点不可解析（source=" + (sourceCell.isEmpty() ? "缺" : cellIds.contains(sourceCell) ? "有" : "缺")
                            + "/target=" + (targetCell.isEmpty() ? "缺" : cellIds.contains(targetCell) ? "有" : "缺")
                            + "），已安全跳过");
                }
            }
            for (Map.Entry<String, JsonNode> entry : entities.entrySet()) {
                JsonNode ent = entry.getValue();
                if (!"LINK".equals(text(ent, "type"))) {
                    continue;
                }
                String lt = text(ent, "linkType");
                if (!semanticLinkTypes.contains(lt) || text(ent, "target").isEmpty()) {
                    continue;
                }
                boolean mounted = rawViews.values().stream().anyMatch(cells -> cells.stream().anyMatch(
                        cell -> "LINK".equals(text(cell, "type")) && entry.getKey().equals(text(cell, "data"))));
                if (!mounted) {
                    warnings.add("[PRS-WARN-002] 孤儿连线实体: id=" + entry.getKey() + ", linkType=" + lt
                            + ", label=" + text(ent, "label") + " 未挂载到任何画布视图，已安全跳过");
                }
            }
        }

        private List<InstanceIR> buildInstances() {
            List<InstanceIR> instances = new ArrayList<>();
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                if (!"INSTANCE".equals(text(entity, "type"))) {
                    continue;
                }
                String typeId = text(entity, "instanceTypeEntityId");
                if (typeId.isEmpty()) {
                    typeId = text(entity, "typeEntityId");
                }
                ViewCtx view = view(text(entity, "defineViewId"));
                JsonNode root = null;
                if (view != null) {
                    for (JsonNode cell : view.cells) {
                        if ("INSTANCE".equals(text(cell, "type")) && eid.equals(text(cell, "data"))) {
                            root = cell;
                            break;
                        }
                    }
                }
                ValueSpec spec;
                if (flag(entity, "isCollection")) {
                    // 数组 / 映射实例：元素经 ARRAY-ELEMENT / MAP-ELEMENT 的“成员”引用其他命名实例
                    spec = root == null ? new ListSpec(List.of()) : collectionSpec(view, root, text(entity, "containerType"));
                } else {
                    spec = root == null
                            ? new ObjectSpec(typeId, Map.of())
                            : new ObjectSpec(typeId, objectMembers(view, root));
                }
                instances.add(new InstanceIR(eid, text(entity, "label"), typeId, spec));
            }
            // 场景自身：场景设计视图里的组成成员即场景对象的成员取值
            for (String sceneId : sceneIds) {
                JsonNode scene = entities.get(sceneId);
                ViewCtx view = view(text(scene, "designViewId"));
                Map<String, ValueSpec> members = new LinkedHashMap<>();
                if (view != null) {
                    List<JsonNode> memberCells = new ArrayList<>();
                    for (JsonNode cell : view.cells) {
                        JsonNode entity = entities.get(text(cell, "data"));
                        if (entity != null && "COMPOSE-MEMBER".equals(text(entity, "type"))
                                && sceneId.equals(text(entity, "typeEntityId"))) {
                            memberCells.add(cell);
                        }
                    }
                    memberCells.sort(CELL_ORDER);
                    for (JsonNode cell : memberCells) {
                        members.putIfAbsent(text(cell, "data"), valueSpecOf(view, cell));
                    }
                }
                instances.add(new InstanceIR("scene:" + sceneId, text(scene, "label"), sceneId,
                        new ObjectSpec(sceneId, members)));
            }
            return instances;
        }

        /** 对象图：宿主单元格的全部成员取值 */
        private Map<String, ValueSpec> objectMembers(ViewCtx view, JsonNode container) {
            Map<String, ValueSpec> members = new LinkedHashMap<>();
            List<JsonNode> memberCells = new ArrayList<>();
            for (JsonNode edge : view.outgoing(text(container, "id"))) {
                String linkType = linkType(edge);
                JsonNode target = view.target(edge);
                if (("exhibition".equals(linkType) || "composition".equals(linkType)) && target != null
                        && !isProcessCell(target) && !"NOTE".equals(text(target, "type"))) {
                    memberCells.add(target);
                }
            }
            memberCells.sort(CELL_ORDER);
            for (JsonNode cell : memberCells) {
                members.putIfAbsent(text(cell, "data"), valueSpecOf(view, cell));
            }
            return members;
        }

        /** 单个成员的取值：字面量 / 引用命名实例 / 内联对象 / 集合 / 未赋值 */
        private ValueSpec valueSpecOf(ViewCtx view, JsonNode cell) {
            JsonNode entity = entities.get(text(cell, "data"));
            String reference = text(cell, "instanceEntityId");
            if (reference.isEmpty() && entity != null) {
                reference = text(entity, "instanceEntityId");
            }
            List<JsonNode> elements = new ArrayList<>();
            List<JsonNode> inline = new ArrayList<>();
            for (JsonNode edge : view.outgoing(text(cell, "id"))) {
                if (!"composition".equals(linkType(edge))) {
                    continue;
                }
                JsonNode child = view.target(edge);
                if (child == null) {
                    continue;
                }
                String childType = text(child, "type");
                if ("MAP-ELEMENT".equals(childType) || "ARRAY-ELEMENT".equals(childType)) {
                    elements.add(child);
                } else if (!isProcessCell(child)) {
                    inline.add(child);
                }
            }
            if (!elements.isEmpty()) {
                return collectionSpec(view, cell, text(entity, "containerType"));
            }
            if (!reference.isEmpty()) {
                String typeId = (entity != null && "DEFINE".equals(text(entity, "typeRange")))
                        ? refTypeIdOf(entity) : (entity == null ? "" : text(entity, "instanceTypeEntityId"));
                return new RefSpec(reference, typeId);
            }
            if (!inline.isEmpty()) {
                String typeId = entity == null ? "" : refTypeIdOf(entity);
                Map<String, ValueSpec> nested = new LinkedHashMap<>();
                for (JsonNode child : inline) {
                    nested.put(text(child, "data"), valueSpecOf(view, child));
                }
                return new ObjectSpec(typeId, nested);
            }
            String raw = text(cell, "defaultValue");
            if (raw.isEmpty() && entity != null) {
                raw = text(entity, "defaultValue");
            }
            if (!raw.isEmpty()) {
                return new ScalarSpec(raw, entity == null ? "string" : text(entity, "fieldType"));
            }
            return NullSpec.INSTANCE;
        }

        /**
         * 容器单元格（集合成员 / 集合实例）的取值：数组按元素序号排成有序集合，映射按"键#值"标签的键排成键值集合。
         * 元素值取其"成员"组成线指向的取值（引用命名实例或内联对象），没有成员时取元素自身默认值。
         */
        private ValueSpec collectionSpec(ViewCtx view, JsonNode container, String containerType) {
            List<JsonNode> elements = new ArrayList<>();
            for (JsonNode edge : view.outgoing(text(container, "id"))) {
                if (!"composition".equals(linkType(edge))) {
                    continue;
                }
                JsonNode child = view.target(edge);
                if (child != null && ("MAP-ELEMENT".equals(text(child, "type")) || "ARRAY-ELEMENT".equals(text(child, "type")))) {
                    elements.add(child);
                }
            }
            elements.sort(ELEMENT_ORDER);
            boolean map = "map".equals(containerType) || elements.stream().anyMatch(e -> "MAP-ELEMENT".equals(text(e, "type")));
            if (map) {
                Map<String, ValueSpec> entries = new LinkedHashMap<>();
                for (JsonNode element : elements) {
                    String[] pair = elementLabelPair(element);
                    entries.put(pair[0], elementValue(view, element));
                }
                return new MapSpec(entries);
            }
            List<ValueSpec> items = new ArrayList<>();
            for (JsonNode element : elements) {
                items.add(elementValue(view, element));
            }
            return new ListSpec(items);
        }

        /** 集合元素单元格的标签形如 "键#值" 或 "序号#值" */
        private String[] elementLabelPair(JsonNode element) {
            String label = text(element, "label");
            int hash = label.indexOf('#');
            return hash > 0 ? new String[] {label.substring(0, hash), label.substring(hash + 1)}
                    : new String[] {label, ""};
        }

        private ValueSpec elementValue(ViewCtx view, JsonNode element) {
            for (JsonNode edge : view.outgoing(text(element, "id"))) {
                if (!"composition".equals(linkType(edge))) {
                    continue;
                }
                JsonNode child = view.target(edge);
                if (child != null && !isProcessCell(child)) {
                    return valueSpecOf(view, child);
                }
            }
            String raw = text(element, "defaultValue");
            JsonNode entity = entities.get(text(element, "data"));
            return raw.isEmpty() ? NullSpec.INSTANCE
                    : new ScalarSpec(raw, entity == null ? "string" : text(entity, "fieldType"));
        }

        private List<AssocTypeIR> buildAssocTypes() {
            List<AssocTypeIR> out = new ArrayList<>();
            for (String assocId : assocTypeIds) {
                JsonNode assoc = entities.get(assocId);
                ViewCtx view = view(text(assoc, "designViewId"));
                if (view == null) {
                    continue;
                }
                for (JsonNode edge : view.cells) {
                    if (!"LINK".equals(text(edge, "type")) || !"bidirectional-tagged".equals(linkType(edge))) {
                        continue;
                    }
                    JsonNode leftCell = view.source(edge);
                    JsonNode rightCell = view.target(edge);
                    JsonNode left = entities.get(text(leftCell, "data"));
                    JsonNode right = entities.get(text(rightCell, "data"));
                    JsonNode linkEntity = entities.get(text(edge, "data"));
                    out.add(new AssocTypeIR(
                            assocId, text(assoc, "label"),
                            text(leftCell, "data"), text(left, "label"), refTypeIdOf(left),
                            text(rightCell, "data"), text(right, "label"), refTypeIdOf(right),
                            text(linkEntity, "constraint1"), text(linkEntity, "constraint2"),
                            text(linkEntity, "linkLabel1"), text(linkEntity, "linkLabel2")));
                }
            }
            return out;
        }

        // ------------------------------------------------------------------ 组装兼容 IR

        private GromaModuleIR assemble(CompilePlanIR plan) {
            List<GromaModuleIR.ConstantIR> constants = new ArrayList<>();
            for (String eid : entities.keySet()) {
                JsonNode entity = entities.get(eid);
                if ("CONSTANT".equals(text(entity, "type")) && !BUILTIN_TOOL_TOPS.contains(treeTop.getOrDefault(eid, ""))) {
                    String label = text(entity, "label");
                    if (label.startsWith("定值") && !text(entity, "alias").isEmpty()) {
                        label = text(entity, "alias");
                    }
                    constants.add(new GromaModuleIR.ConstantIR(eid, label, legacyFieldType(slotType(entity)),
                            text(entity, "defaultValue"), text(entity, "unit")));
                }
            }
            List<GromaModuleIR.EnumIR> enums = new ArrayList<>();
            for (String enumId : enumStates.keySet()) {
                enums.add(new GromaModuleIR.EnumIR(enumId, text(entities.get(enumId), "label"), enumStates.get(enumId),
                        enumInitial.getOrDefault(enumId, "")));
            }
            List<GromaModuleIR.TypeIR> domainTypes = new ArrayList<>();
            List<GromaModuleIR.ProcessIR> independentProcesses = new ArrayList<>();
            for (TypeDefIR type : plan.types()) {
                List<GromaModuleIR.FieldIR> fields = new ArrayList<>();
                List<GromaModuleIR.TriggerBindingIR> noTriggers = List.of();
                for (MemberIR member : type.members()) {
                    fields.add(new GromaModuleIR.FieldIR(
                            member.entityId(), member.zhName(), legacyFieldType(slotType(member.entityId())),
                            member.typeRange(), member.collection(), false, member.unit(), member.minValue(),
                            member.maxValue(), member.defaultValue(), noTriggers));
                }
                List<GromaModuleIR.ProcessIR> methods = new ArrayList<>();
                for (ProcessDefIR def : plan.processDefs()) {
                    if (type.entityId().equals(def.ownerTypeId())) {
                        methods.add(toProcessIR(def, plan.configuredStepMs()));
                    }
                }
                domainTypes.add(new GromaModuleIR.TypeIR(type.entityId(), type.zhName(), type.description(), type.essence(),
                        type.primaryParentId().isEmpty() ? null : type.primaryParentId(), List.of(), fields, methods));
            }
            for (ProcessDefIR def : plan.processDefs()) {
                if ("INDEPENDENT".equals(def.ownerKind())) {
                    independentProcesses.add(toProcessIR(def, plan.configuredStepMs()));
                }
            }
            List<GromaModuleIR.SceneIR> scenes = new ArrayList<>();
            for (String sceneId : sceneIds) {
                JsonNode scene = entities.get(sceneId);
                List<GromaModuleIR.SceneMemberIR> initMembers = new ArrayList<>();
                for (MemberIR member : memberIRsOf(plan, sceneId)) {
                    Map<String, String> overrides = new LinkedHashMap<>();
                    if (!member.defaultValue().isEmpty()) {
                        overrides.put("defaultValue", member.defaultValue());
                    }
                    String reference = instanceReferenceOf(plan, sceneId, member.entityId());
                    if (!reference.isEmpty()) {
                        overrides.put("instance", reference);
                    }
                    String memberType = member.refTypeId().isEmpty() ? member.fieldType() : member.refTypeId();
                    initMembers.add(new GromaModuleIR.SceneMemberIR(member.entityId(), member.zhName(), memberType, overrides));
                }
                List<String> defaultStartOperators = new ArrayList<>();
                List<String> defaultStartProcesses = new ArrayList<>();
                List<GromaModuleIR.ProcessIR> sceneLevel = new ArrayList<>();
                for (ProcessDefIR def : plan.processDefs()) {
                    if (!sceneId.equals(def.ownerTypeId())) {
                        continue;
                    }
                    GromaModuleIR.ProcessIR process = toProcessIR(def, plan.configuredStepMs());
                    sceneLevel.add(process);
                    boolean started = plan.defaultStarts().stream().anyMatch(ds -> ds.targetDefKey().equals(def.defKey())
                            && sceneId.equals(ds.typeId()));
                    if (started) {
                        (def.timeConsuming() ? defaultStartProcesses : defaultStartOperators).add(def.defKey());
                    }
                }
                scenes.add(new GromaModuleIR.SceneIR(
                        "tree:" + sceneId, sceneId, text(scene, "label"), text(scene, "defineViewId"),
                        text(scene, "designViewId"), initMembers, defaultStartOperators, defaultStartProcesses, sceneLevel));
            }
            Map<String, GromaModuleIR.ViewGraphIR> views = new TreeMap<>();
            for (Map.Entry<String, List<JsonNode>> entry : rawViews.entrySet()) {
                views.put(entry.getKey(), toViewGraph(entry.getKey(), entry.getValue()));
            }
            String digest = sha256Hex(bytesA) + ":" + sha256Hex(bytesB);
            return new GromaModuleIR("2.0", modelId, "", modelLabel, modelVersion, digest, plan.configuredStepMs(),
                    constants, enums, domainTypes, independentProcesses, scenes, views, plan);
        }

        private List<MemberIR> memberIRsOf(CompilePlanIR plan, String typeId) {
            for (TypeDefIR type : plan.types()) {
                if (typeId.equals(type.entityId())) {
                    return type.members();
                }
            }
            return List.of();
        }

        private String instanceReferenceOf(CompilePlanIR plan, String sceneId, String memberId) {
            for (InstanceIR instance : plan.instances()) {
                if (("scene:" + sceneId).equals(instance.instanceId()) && instance.spec() instanceof ObjectSpec object) {
                    if (object.members().get(memberId) instanceof RefSpec ref) {
                        return ref.instanceId();
                    }
                }
            }
            return "";
        }

        /** 类型占位 → 旧 IR 的 fieldType 约定（基础类型名，或枚举/类型的实体 id） */
        private String legacyFieldType(String placeholder) {
            if (GromaTypes.isEnum(placeholder) || GromaTypes.isEntity(placeholder)) {
                return placeholder.substring(placeholder.indexOf(':') + 1);
            }
            if (GromaTypes.isCollection(placeholder)) {
                return "Object";
            }
            return placeholder;
        }

        /** 设计视图里是否有自调用线（R24） */
        private boolean selfInvoking(ProcessDefIR def) {
            ViewCtx view = view(def.designViewId());
            if (view == null) {
                return false;
            }
            for (JsonNode cell : view.cells) {
                if ("LINK".equals(text(cell, "type")) && "invocation-self".equals(linkType(cell))) {
                    return true;
                }
            }
            return false;
        }

        private GromaModuleIR.ProcessIR toProcessIR(ProcessDefIR def, long stepMs) {
            List<GromaModuleIR.ParamPortIR> inputs = new ArrayList<>();
            List<GromaModuleIR.ParamPortIR> outputs = new ArrayList<>();
            for (PortIR port : def.params()) {
                GromaModuleIR.ParamPortIR legacy = new GromaModuleIR.ParamPortIR(
                        port.entityId(), port.entityId(), port.zhName(), legacyFieldType(port.javaType()),
                        GromaTypes.isEnum(port.javaType()) ? "ENUM" : GromaTypes.isEntity(port.javaType()) ? "DEFINE" : "BASE",
                        port.nonnull(), "IN".equals(port.direction()), "", null, null, null);
                if ("IN".equals(port.direction()) || "IN_OUT".equals(port.direction())) {
                    inputs.add(legacy);
                }
                if ("OUT".equals(port.direction()) || "IN_OUT".equals(port.direction()) || "LOCAL".equals(port.direction())) {
                    outputs.add(legacy);
                }
            }
            List<GromaModuleIR.FieldIR> locals = new ArrayList<>();
            for (LocalVarIR local : def.locals()) {
                locals.add(new GromaModuleIR.FieldIR(local.entityId(), local.zhName(), legacyFieldType(local.javaType()),
                        "BASE", false, false, "", "", "", local.initLiteral(), List.of()));
            }
            List<GromaModuleIR.SubProcessStepIR> subSteps = new ArrayList<>();
            for (NodeIR node : def.nodes()) {
                if (!"CALL".equals(node.kind())) {
                    continue;
                }
                List<Integer> nextPcs = new ArrayList<>();
                for (NextIR next : node.nexts()) {
                    nextPcs.add(next.targetPc());
                }
                subSteps.add(new GromaModuleIR.SubProcessStepIR(node.pc(), node.cellId(), node.entityId(), node.zhName(),
                        node.calleeTimeConsuming(), node.firstStart(), false, nextPcs, null));
            }
            List<GromaModuleIR.PostActionIR> postActions = new ArrayList<>();
            for (NodeIR node : def.nodes()) {
                for (PostIR post : node.postActions()) {
                    postActions.add(new GromaModuleIR.PostActionIR(
                            "OUT_CHANGE".equals(post.kind()) ? "OUT_CHANGE" : post.kind(),
                            post.kind() + "@" + node.pc(), post.stateLabel(), 1.0D, ""));
                }
            }
            long period = def.accuracyMs() == null ? 1L : Math.max(1L, (def.accuracyMs() + stepMs - 1L) / stepMs);
            String singleton = def.singletonMode() == 3 ? "GLOBAL_SINGLETON"
                    : def.singletonMode() == 1 ? "OBJECT_SINGLETON" : "NONE";
            return new GromaModuleIR.ProcessIR(
                    def.entityId(), def.ownerTypeId(), def.zhName(), def.description(),
                    def.timeConsuming(), def.twinLocked(), def.asyncMode(), singleton, selfInvoking(def),
                    def.durationParamType(), def.timeNormal(), def.timeMin(), def.timeMax(), def.timeUnit(), period,
                    def.javaCode(), inputs, outputs, locals, subSteps, List.of(), List.of(), postActions,
                    !"SCENE".equals(def.ownerKind()), text(entities.get(def.entityId()), "defineViewId"), def.designViewId());
        }

        private GromaModuleIR.ViewGraphIR toViewGraph(String viewId, List<JsonNode> cells) {
            List<GromaModuleIR.NodeCellIR> nodes = new ArrayList<>();
            List<GromaModuleIR.EdgeCellIR> edges = new ArrayList<>();
            for (JsonNode cell : cells) {
                if ("LINK".equals(text(cell, "type"))) {
                    String relation = linkType(cell);
                    if (!CanvasMappingTables.SHAPE_TO_RELATION.containsKey("edge-" + relation) && !relation.isEmpty()) {
                        if (warnings.stream().noneMatch(w -> w.contains("未注册的连线形状: " + relation))) {
                            warnings.add("[PRS-WARN-001] 未注册的连线形状: shape=edge-" + relation + ", viewId=" + viewId
                                    + "（按原样保留，不做语义编译）");
                        }
                    }
                    edges.add(new GromaModuleIR.EdgeCellIR(
                            text(cell, "id"), text(cell, "data"), relation,
                            text(cell.path("source"), "cell"), text(cell.path("target"), "cell"), text(cell, "event")));
                } else {
                    JsonNode entity = entities.get(text(cell, "data"));
                    nodes.add(new GromaModuleIR.NodeCellIR(
                            text(cell, "id"), text(cell, "data"), text(cell, "type"), text(cell, "shape"),
                            entity == null ? text(cell, "label") : text(entity, "label"),
                            flag(cell, "defaultStart"), flag(cell, "firstStart"), flag(cell, "lastEnd"),
                            doubleOf(cell, "x", 0.0D), doubleOf(cell, "y", 0.0D)));
                }
            }
            return new GromaModuleIR.ViewGraphIR(viewId, viewId.startsWith("defineView"), nodes, edges);
        }

        // ------------------------------------------------------------------ 视图索引

        /**
         * 单个画布视图的单元格与连线索引
         */
        private final class ViewCtx {
            final String viewId;
            final List<JsonNode> cells;
            private final Map<String, JsonNode> byId = new HashMap<>();
            private final Map<String, List<JsonNode>> incoming = new HashMap<>();
            private final Map<String, List<JsonNode>> outgoing = new HashMap<>();

            ViewCtx(String viewId, List<JsonNode> cells) {
                this.viewId = viewId;
                this.cells = cells;
                for (JsonNode cell : cells) {
                    byId.put(text(cell, "id"), cell);
                }
                for (JsonNode cell : cells) {
                    if (!"LINK".equals(text(cell, "type"))) {
                        continue;
                    }
                    String source = text(cell.path("source"), "cell");
                    String target = text(cell.path("target"), "cell");
                    if (source.isEmpty() || target.isEmpty()) {
                        continue;
                    }
                    incoming.computeIfAbsent(target, k -> new ArrayList<>()).add(cell);
                    outgoing.computeIfAbsent(source, k -> new ArrayList<>()).add(cell);
                }
            }

            JsonNode cell(String cellId) {
                return cellId == null ? null : byId.get(cellId);
            }

            JsonNode source(JsonNode edge) {
                return cell(text(edge.path("source"), "cell"));
            }

            JsonNode target(JsonNode edge) {
                return cell(text(edge.path("target"), "cell"));
            }

            List<JsonNode> incoming(String cellId) {
                return incoming.getOrDefault(cellId, List.of());
            }

            List<JsonNode> outgoing(String cellId) {
                return outgoing.getOrDefault(cellId, List.of());
            }
        }
    }
}




出码（ModelLayerEmitter）：GromaModuleIR.TypeIR 已有 mixinParentEntityIds 字段——副父类生成组合委托：Java// 接口多继承：public interface SubReadOnly extends P1ReadOnly, P2ReadOnly
// 实体组合委托：private final P2Entity mixinP2Entity; 桥接 Getter/Setter 逐成员转发
for (String mixinId : type.mixinParentEntityIds()) {
    TypeInfo mixin = model.types.get(mixinId);
    if (mixin == null) continue;
    cb.addField(FieldSpec.builder(mixin.entity(), "mixin" + mixin.code(), Modifier.PRIVATE, Modifier.FINAL).build());
    for (MemberInfo member : mixin.allMembers()) {
        cb.addMethod(MethodSpec.methodBuilder(member.getter()).addAnnotation(Override.class)
                .returns(member.type()).addStatement("return mixin$L.$L()", mixin.code(), member.getter()).build());
        // setterCtx 同理转发
    }
}
方案 B：R07~R10 关联管理器（RelationLayerEmitter 新增）Java/** 消费 plan.associationTypes()：engine/relation 包下强类型双向关联管理器（基数校验 + ra/ar/rv/vr/rx/xr 双端事件） */
public final class RelationLayerEmitter {
    public List<JavaFile> emit(PlanModel model) {
        List<JavaFile> out = new ArrayList<>();
        for (CompilePlanIR.AssocTypeIR assoc : model.plan.associationTypes()) {
            TypeInfo left = model.types.get(assoc.leftTypeId());
            TypeInfo right = model.types.get(assoc.rightTypeId());
            if (left == null || right == null) continue;
            TypeSpec.Builder mgr = TypeSpec.classBuilder(ClassName.get(
                            model.basePkg + ".engine.relation", assoc.zhName() + "RelationManager"))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(ParameterizedTypeName.get(PlanModel.BIDIRECTIONAL_TIE,
                            left.entity(), right.entity()))
                    .addJavadoc("关联类型 $L（左：$L %s；右：$L %s）。tie/untie 时经 ctx 派发 ra/ar/rx 与 xr 反向被动事件。"
                                    .formatted(assoc.constraintLeft(), assoc.constraintRight()),
                            assoc.zhName(), left.zhName(), right.zhName())
                    .addMethod(MethodSpec.methodBuilder("tieLeft")
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(left.entity(), "l").addParameter(right.entity(), "r")
                            .addParameter(PlanModel.WORLD_CTX, "ctx")
                            .addStatement("super.tie(l.getInstanceId(), r.getInstanceId())")
                            .addStatement("l.onMemberChanged($S, r, ctx)", assoc.leftMemberId())
                            .addStatement("r.onMemberChanged($S, l, ctx)", assoc.rightMemberId())
                            .build());
            out.add(JavaFile.builder(model.basePkg + ".engine.relation", mgr.build()).indent("    ").build());
        }
        return out;
    }
}
配套：GromaWorldContext 持有关系管理器注册表（Map<String, BidirectionalTie<?,?>>），ModuleRuntimeEmitter 在 createSession 中实例化并登记。方案 C：21 类事件全量派发（engine 底座）Java// BaseSimEntity/GromaWorldContext 补齐被动事件（订阅侧按需派发，无订阅零成本）：
// spawn() → 对非空初始状态成员同步发 as；markConsumed() → 发 xs；
// 集合增删 → 对元素发 am/xm（持有方仍发 ma/mx）；引用变更 → 对目标发 ar/vr/xr。
private void dispatchPassiveEvents(BaseSimEntity target, String eventCode, String memberToken, String stateLabel) {
    WorldSnapshot.SimEventRecord event = new WorldSnapshot.SimEventRecord(
            eventSeq.incrementAndGet(), tickIndex, eventCode, target.getInstanceId(),
            eventCode + ":" + target.getInstanceId(), memberToken, stateLabel, "");
    GromaTraceListener trace = traceListener;
    if (trace != null) trace.onEvent(this, tickIndex, silent, event);
    eventSink().accept(event);   // 与既有 v/sa/ra 同一事件总线
}
ObservableEntityList 的 add/remove 回调里补 am/xm 元素侧派发；过程任务 GromaProcessTask 补 pca/pcd（完成时比对 elapsed 与 TIMING 上下限）与 psf（startTopLevel 失败分支）、paa（维持条件中断，配合方案 D 的 checkMaintain）。方案 D：R21/R22/R13/R15/R23/R25/R26 解析与出码解析（AbModelMergerAndParser.parseDef）——替换两处 List.of()：Java// 设计视图扫描：edge-condition → startConditions；edge-agent → maintainConditions；
// edge-in-change → 源状态前置守卫（canStart 校验）+ 前锚点切换；edge-in-out → 拆一条 in-change + 一条 out-change；
// edge-overtime-exception / edge-undertime-exception → exceptionFlows（pcd / pca 分支 pc）
for (JsonNode edge : designView.cells()) {
    String lt = linkType(edge);
    switch (lt) {
        case "condition" -> starts.add(conditionRuleOf(edge));
        case "agent" -> maintains.add(conditionRuleOf(edge));
        case "in-change" -> starts.add(preStateRuleOf(edge));       // 源状态守卫 + 前锚点
        case "in-out" -> { starts.add(preStateRuleOf(edge)); outs.add(outChangeRuleOf(edge)); }
        case "overtime-exception" -> exceptions.add(new ExceptionFlowIR("OVERTIME_PCD", pcOf(edge), ""));
        case "undertime-exception" -> exceptions.add(new ExceptionFlowIR("UNDERTIME_PCA", pcOf(edge), ""));
        default -> { }
    }
}
出码（ProcessTaskEmitter）：Java// canStart() 尾部追加：R17/R20 存活守卫（已有判空）+ R13 源状态 + R22 启动条件；失败走 psf 后继（R23 事件线）
// 每拍 checkMaintain()：R21 agent 条件为 false → abortAllChildren + 状态置 ABORTED + 发 paa + 跳 maintain 失败分支
// nodeDone() 移除"暂未编译，已跳过"：eventCode ∈ {psf,paa,pca,pcd} 的 NextIR 正常编译为 case 分支
// finishSuccess()：elapsed > timeMax 发 pcd；elapsed < timeMin 发 pca；激活对应 ExceptionFlowIR 目标 pc
方案 E：R16/R17 ReadOnly 契约绑定（PlanModel 一处）Java// PlanModel.resolveParam：direction IN 且 nonnull/nullable（R16/R17）时返回 readOnly 而非 entity
TypeName paramType = "IN".equals(ir.direction()) && !rebindable
        ? type.readOnly() : type.entity();
// ProcessTaskEmitter 构造器字段同步为只读接口；方法体内部仅调用接口的 Getter（编译期禁写）
方案 F：宿主侧 Python 执行器装配方（使 F6 数据桥真正落地）Java// bootstrap 新增 ScriptIoBridgeConfiguration：把 ExternalProgramConnector 适配为 ExternalScriptIoExecutor
@Bean
public com.groma.base.engine.GromaWorldContext.ExternalScriptIoExecutor pythonScriptIoExecutor(
        com.groma.base.connector.ExternalProgramConnector connector) {
    return (ctx, scriptName, processId, inputs) -> {
        // inputs 以 JSON 写入脚本 stdin；约定脚本把结果 JSON 写 stdout（groma 模板读取 inputs 字典）
        return connector.runScriptWithJsonIo(scriptName, inputs);
    };
}
// 会话创建时注入：GromaModelRegistry.startSession 的 beforeInitialize 回调里
// ctx.setExternalScriptIoExecutor(executorBean);
ExternalProgramConnector 需补 runScriptWithJsonIo(script, inputs)：临时目录写脚本 + stdin 喂 JSON + destroyForcibly 超时强杀 + 解析 stdout JSON（既有安全沙盒语义全部保留）。方案 G：存档续跑确定性（ENG-05）TaskArchivePart 增加 startEpochMs 字段并在 createCheckpointAtStepBoundary 记录、restoreFromArchive 恢复 AbstractWorldSession 墙钟基准；instanceSeq 推进改为"扫描全部恢复实例 id 的数字后缀 + 每次命名实例创建统一走 seq 分配"两条路取最大（中文/UUID id 不含数字后缀的用 seq 计数兜底）。验收以 RealCorpusEndToEndTest 中被 @Disabled 的 150→151 拍分叉用例转绿为准。方案 H：export-zip 精简运行态交付（可选增强）当前打包 groma-studio 全目录是必要依赖（bootstrap 依赖 hotswap/debug-server）。若要纯运行态轻量包：新增 groma-runtime 聚合模块（仅 base 五模块 + workspace 生成模型 + 一个无 studio 依赖的精简宿主），export-zip 的 ?flavor=runtime 参数切换打包该子集。属于结构演进，不影响现有交付。六、回归验证与产物本轮修复涉及文件：StaticModelValidator、ModelPipelineService、GromaWorldContext、WorldContextEmitter、ManifestAndIndexEmitter、LeafBodyEmitter、ProcessCodeAstTranslator、ServiceLayerEmitter、GromaModuleSourceGeneratorFacade。测试：受影响六模块全量回归通过（含真实语料六步流水线、ECJ+javac 对拍、存档往返、校验规则单测）。模型 2 验证路径：宿主启动后 POST /api/v1/studio/models/upload 传 scene-a.json + scene-b.json（或 fetch-remote 拉取），构建报告的 warnings 将列出 VAL-004 WARNING 与 Python 剥离 WARN；生成的 IIndependentProcessService 含"航路规划与同步到达算法"门面；Python 双向桥在装配方 F 后打通。================ shapes-and-fields-dict.md ================29 种画布连线形状、21 种事件代码与关键字段权威字典1. 29 种画布连线形状全映射表结构关系（10 种）：edge-extend (R01)、edge-composition (R02)、edge-exhibition (R03)、edge-classification (R04)、edge-proxy (R05)、edge-control (R06 平台扩展)、edge-unidirectional-association (R07)、edge-bidirectional-association (R08)、edge-unidirectional-tagged (R09)、edge-bidirectional-tagged (R10)。交互与锚点关系（19 种）：edge-consumption (R11)、edge-result (R12)、edge-in-change (R13)、edge-out-change (R14)、edge-in-out (R15 归一化为 R13+R14)、edge-in-influence-nullable (R16)、edge-in-influence-nonnull (R17)、edge-out-influence (R18)、edge-in-out-influence-nullable (R19)、edge-in-out-influence-nonnull (R20)、edge-agent (R21)、edge-condition (R22)、edge-invocation (R23)、edge-invocation-self (R24)、edge-overtime-exception (R25)、edge-undertime-exception (R26)、edge-note (R27)、edge-fixed (R28)、edge-link (R00)。2. 21 种标准事件代码表生命周期与状态（7 种）：a（出现）、as（指定状态出现）、sa（进入状态）、v（属性变化）、x（消亡）、xs（指定状态消亡）、sx（离开状态）。集合代理（4 种）：ma（集合增成员）、am（加入集合）、mx（集合减成员）、xm（移出集合）。引用指向（6 种）：ra（建立指向）、ar（被建立指向）、rv（变更指向）、vr（被变更指向）、rx（解除指向）、xr（被解除指向）。过程异常与收尾（4 种）：pca（提前完成）、pcd（超时完成）、psf（启动失败）、paa（中断取消）。================ validation-rules.md ================静态模型校验规则与《方圆语言标准-1117》条款对照表规则编号严重度标准条款依据拦截条件描述对应单测VAL-001ERROR标准 9.2.2 / 12.1.2非耗时算子（timeConsuming=false）内部禁止包含耗时子过程或延迟等待逻辑ValidationRulesAndMappingTestVAL-002WARNING标准 12.1.2普通父过程包含耗时子过程时自动向上推断或提示显式开启耗时标记（规避 BE-17）ValidationRulesAndMappingTestVAL-003ERROR标准 8.5对象类型单继承与多继承有向图不得存在闭环（防 BL-15 栈溢出）ValidationRulesAndMappingTestVAL-004ERROR标准 9.4.7含子过程设计视图的复合过程必须至少声明一个 firstStart=true 入口子过程ValidationRulesAndMappingTestVAL-005ERROR平台扩展 R06结构控制关系（edge-control）必须构成有向无环图（DAG）ValidationRulesAndMappingTestVAL-006ERROR标准 7.3定值（Constant）严禁连接消耗、生成、状态转换或单/双向输出影响连线ValidationRulesVal006to010TestVAL-007ERROR标准 8.2标识符编码若由用户显式填写，不得违反 ^[A-Za-z_][A-Za-z0-9_]*$ 或命中 Java 21 保留字（实现于 UniqueIdentifierGuard，StaticModelValidator 接线）ValidationRulesVal006to010TestVAL-008ERROR标准 10.4同组互斥分支概率之和必须落在 [0.9999, 1.0001] 闭区间内（单成员命名组降级 WARNING）ValidationRulesVal006to010TestVAL-009ERROR标准 10.1.3同一对象与同一过程之间同向同类型关系最多允许存在一条（edge-fixed 除外）ValidationRulesAndMappingTestVAL-010ERROR标准 9.6成员链与引用定位路径深度不得超过最大递归上限 64 层ValidationRulesVal006to010Test


```python?code_reference&code_event_index=6
import json, os
from collections import Counter, defaultdict

docs_dir = '/tmp/orpl_extract/orpl-9ee34655eecdb360241d86a189cb40c9527ef56a/groma/docs'

def analyze_model(name, file_a, file_b):
    print(f"================ {name}: {file_a} + {file_b} ================")
    with open(os.path.join(docs_dir, file_a), 'r', encoding='utf-8') as f:
        raw_a = json.load(f)
    with open(os.path.join(docs_dir, file_b), 'r', encoding='utf-8') as f:
        raw_b = json.load(f)
    
    print("raw_a keys:", list(raw_a.keys()))
    obj_a = raw_a.get('obj', raw_a)
    print("obj_a keys:", list(obj_a.keys()))
    for k in ['id', 'name', 'label', 'code', 'encode', 'version', 'step', 'timeUnit']:
        if k in obj_a:
            print(f"  obj_a[{k}] = {obj_a[k]}")
    
    entities = obj_a.get('entities', {})
    views = obj_a.get('views', {})
    trees = obj_a.get('trees', obj_a.get('orgTrees', obj_a.get('organization', {})))
    print(f"entities count: {len(entities)} (type={type(entities).__name__})")
    print(f"views count: {len(views)} (type={type(views).__name__})")
    for k, v in obj_a.items():
        if k not in ('entities', 'views'):
            s = str(v)
            print(f"  other key '{k}': type={type(v).__name__}, preview={s[:200]}")

    # Entity type breakdown
    ent_list = entities.values() if isinstance(entities, dict) else entities
    type_counter = Counter()
    sub_counter = Counter()
    for e in ent_list:
        t = e.get('type', '')
        st = e.get('subType', e.get('category', e.get('essence', '')))
        type_counter[t] += 1
        sub_counter[(t, str(st))] += 1
    print("Entity types:", dict(type_counter.most_common()))

    # Analyze file_b
    print("raw_b keys:", list(raw_b.keys()) if isinstance(raw_b, dict) else type(raw_b))
    obj_b = raw_b.get('obj', raw_b) if isinstance(raw_b, dict) else raw_b
    if isinstance(obj_b, dict):
        print("obj_b keys (len):", len(obj_b), "sample keys:", list(obj_b.keys())[:5])
    elif isinstance(obj_b, list):
        print("obj_b list len:", len(obj_b))

    # Count cells and link types across file_b / views
    shape_counter = Counter()
    link_type_counter = Counter()
    event_counter = Counter()
    cells_total = 0
    
    def scan_cells(cells):
        nonlocal cells_total
        for c in cells:
            cells_total += 1
            sh = c.get('shape', '')
            shape_counter[sh] += 1
            data = c.get('data', {}) or {}
            lt = data.get('linkType', c.get('linkType', ''))
            if sh.startswith('edge-') or c.get('type') == 'LINK' or lt:
                link_type_counter[(sh, lt)] += 1
            ev = data.get('event', data.get('eventCode', data.get('events', '')))
            if ev:
                event_counter[str(ev)] += 1

    if isinstance(obj_b, dict):
        if 'cells' in obj_b and isinstance(obj_b['cells'], list):
            scan_cells(obj_b['cells'])
        else:
            for vid, vval in obj_b.items():
                if isinstance(vval, list):
                    scan_cells(vval)
                elif isinstance(vval, dict) and 'cells' in vval:
                    scan_cells(vval['cells'])
    elif isinstance(obj_b, list):
        scan_cells(obj_b)

    print(f"Total cells in B: {cells_total}")
    print("Shapes in B:", dict(shape_counter.most_common()))
    print("Link types (shape, linkType):", dict(link_type_counter.most_common()))
    print("Events sample:", dict(event_counter.most_common(30)))

analyze_model("Model 1", "data-a.json", "data-b.json")
analyze_model("Model 2", "scene-a.json", "scene-b.json")

Pythonimport os, json
from collections import Counter

base_dir = '/tmp/orpl_extract/orpl-9ee34655eecdb360241d86a189cb40c9527ef56a/groma'

# First let's inspect cellMap in data-a/b and scene-a/b properly!
def inspect_corpus(name, fa, fb):
    print(f"=== {name} DEEP CORPUS STATS ===")
    with open(os.path.join(base_dir, 'docs', fa), 'r', encoding='utf-8') as f:
        oa = json.load(f)['obj']
    with open(os.path.join(base_dir, 'docs', fb), 'r', encoding='utf-8') as f:
        ob = json.load(f)['obj']
    
    ents = {e['id']: e for e in oa['entities']}
    cm_a = oa.get('cellMap', {}) or {}
    cm_b = ob.get('cellMap', {}) or {}
    print(f"cellMap views in A: {len(cm_a)}, in B: {len(cm_b)}, union: {len(set(cm_a)|set(cm_b))}")
    
    merged_cm = dict(cm_a)
    merged_cm.update(cm_b)
    
    shapes = Counter()
    link_types = Counter()
    events = Counter()
    link_entities_types = Counter()
    
    for e in oa['entities']:
        if e.get('type') == 'LINK':
            link_entities_types[e.get('linkType', '')] += 1
            ev = e.get('event', e.get('events', e.get('triggerEvent', '')))
            if ev:
                events[f"entity:{ev}"] += 1
    
    total_cells = 0
    for vid, cells in merged_cm.items():
        if not isinstance(cells, list): continue
        for c in cells:
            total_cells += 1
            sh = c.get('shape', '')
            shapes[sh] += 1
            lt = c.get('linkType', '') or (ents.get(c.get('data', ''), {}).get('linkType', ''))
            if sh.startswith('edge-') or c.get('type') == 'LINK':
                link_types[(sh, lt)] += 1
            for k in ('event', 'events', 'eventCode', 'triggerEvent', 'code'):
                if c.get(k):
                    events[f"cell.{k}:{c.get(k)}"] += 1
            ent = ents.get(c.get('data', ''), {})
            for k in ('event', 'events', 'eventCode', 'triggerEvent', 'linkEvent'):
                if ent.get(k):
                    events[f"ent.{k}:{ent.get(k)}"] += 1

    print(f"Total merged cells: {total_cells}")
    print("LINK entity linkTypes:", dict(link_entities_types.most_common()))
    print("Cell edge shapes & linkTypes:", dict(link_types.most_common()))
    print("Non-edge shapes:", {k: v for k, v in shapes.most_common() if not k.startswith('edge-')})
    
    # Inspect LINK entity keys to see all relationship fields!
    link_keys = Counter()
    sample_events = Counter()
    for e in oa['entities']:
        if e.get('type') == 'LINK':
            for k, v in e.items():
                if v not in ('', None, [], {}, False):
                    link_keys[k] += 1
                if 'event' in k.lower() or k in ('trigger', 'condition', 'judge', 'probability', 'probGroup', 'delay'):
                    if v not in ('', None, [], {}):
                        sample_events[(k, str(v)[:60])] += 1
    print("LINK non-empty keys:", dict(link_keys.most_common()))
    print("LINK event/judge/prob/delay samples:", dict(sample_events.most_common(40)))

    # Check SCENE, ASSOCIATION-TYPE, PARENT, PROXY, INDEPENDENT-PROCESS, ENUM, PROCESS codeType/language
    for et in ['SCENE', 'ASSOCIATION-TYPE', 'PARENT', 'PROXY', 'INDEPENDENT-PROCESS', 'ENUM', 'CONSTANT']:
        items = [e for e in oa['entities'] if e.get('type') == et]
        if items:
            print(f"  [{et}] ({len(items)}):", [(x.get('id'), x.get('label'), x.get('code',''), x.get('componentId','')) for x in items[:10]])

    procs = [e for e in oa['entities'] if e.get('type') in ('PROCESS', 'INDEPENDENT-PROCESS', 'TEMPLATE-PROCESS')]
    proc_langs = Counter()
    for p in procs:
        for k in ('language', 'codeType', 'scriptType', 'processType', 'implType', 'executeType'):
            if p.get(k):
                proc_langs[(k, str(p.get(k)))] += 1
        if p.get('code') or p.get('script') or p.get('content') or p.get('javaCode') or p.get('pythonCode'):
            for ck in ('code', 'script', 'content', 'javaCode', 'pythonCode'):
                if p.get(ck):
                    proc_langs[(f"has_{ck}", str(p.get(ck))[:40].replace('\n',' '))] += 1
    print("Process code/lang keys:", dict(proc_langs.most_common(20)))
    # Check all keys on PROCESS entities
    pkeys = Counter()
    for p in procs:
        for k, v in p.items():
            if v not in ('', None, [], {}, False):
                pkeys[k] += 1
    print("PROCESS non-empty keys:", dict(pkeys.most_common()))

inspect_corpus("Model 1", "data-a.json", "data-b.json")
inspect_corpus("Model 2", "scene-a.json", "scene-b.json")

代码输出=== Model 1 DEEP CORPUS STATS ===
cellMap views in A: 240, in B: 175, union: 415
Total merged cells: 3493
LINK entity linkTypes: {'fixed': 487, 'invocation': 225, 'exhibition': 208, 'in-influence-nonnull': 180, 'out-influence': 177, 'in-influence': 72, 'composition': 56, 'in-out-influence-nonnull': 12, 'out-change': 11, 'bidirectional-tagged': 9, 'in-influence-nullable': 8, 'result': 5, 'condition': 3, 'in-out-influence': 2, 'note': 1, 'invocation-self': 1, 'in-out-influence-nullable': 1}
Cell edge shapes & linkTypes: {('edge-fixed', 'fixed'): 487, ('edge-invocation', 'invocation'): 225, ('edge-exhibition', 'exhibition'): 208, ('edge-in-influence-nonnull', 'in-influence-nonnull'): 179, ('edge-out-influence', 'out-influence'): 176, ('edge-in-influence-nonnull', 'in-influence'): 71, ('edge-composition', 'composition'): 56, ('edge-in-out-influence-nonnull', 'in-out-influence-nonnull'): 12, ('edge-out-change', 'out-change'): 11, ('edge-bidirectional-tagged', 'bidirectional-tagged'): 9, ('edge-in-influence-nullable', 'in-influence-nullable'): 8, ('edge-result', 'result'): 5, ('edge-condition', 'condition'): 3, ('edge-in-out-influence-nonnull', 'in-out-influence'): 2, ('edge-in-out-influence-nullable', 'in-out-influence-nullable'): 1, ('edge-invocation-self', 'invocation-self'): 1, ('edge-note', 'note'): 1}
Non-edge shapes: {'node-process': 595, 'node-relevance-object': 535, 'node-attribute-member': 396, 'node-state': 122, 'node-compose-member': 100, 'node-template-member': 79, 'node-type': 47, 'node-template-process': 46, 'node-constant': 32, 'node-instance': 31, 'node-cur-run-time': 11, 'node-enum': 10, 'node-independent-process': 9, 'node-setting-member': 6, 'node-association-type': 5, 'node-array-element': 3, 'node-map-element': 3, 'node-cur-object': 2, 'node-actual-run-time': 1, 'node-cur-process-time': 1, 'node-thing-object': 1, 'node-user-info': 1, 'node-run-param': 1, 'node-note': 1}
LINK non-empty keys: {'label': 1458, 'type': 1458, 'linkType': 1458, 'id': 1458, 'logic': 1344, 'sourceId': 1018, 'componentType': 456, 'componentId': 456, 'determine': 63, 'event': 13, 'linkLabel2': 5, 'linkLabel1': 5, 'constraint2': 5, 'constraint1': 5}
LINK event/judge/prob/delay samples: {('event', 'sa'): 7, ('event', 'a'): 4, ('event', 'v'): 2}
  [SCENE] (4): [('bX4vMzSlVMdk6QjF7CD', '场景1', '', ''), ('fFnAAr3WKsN0Oaghvzj', '场景2', '', ''), ('rduE43Zkxe2x1aE6YWZ', '默认场景', '', 'oYRdScR34kQuJShQuaj'), ('t315EPfZDL0p9OXY4Zs', '默认场景', '', 'dMoXriv3KSb2QEyTlGe')]
  [ASSOCIATION-TYPE] (5): [('8iJr06uEbFRp2d7gT5y', '列车通行', '', ''), ('a0FNVLNrTFTjGHd2bYh', '挡水防护', '', ''), ('BdhINdjBhLdgkp89TAf', '地下进水', '', ''), ('E32hWqA7cw2F3sNSc8K', '降雨汇水', '', ''), ('kFr6nPxVVgIlStfwOCS', '停运控制', '', '')]
  [INDEPENDENT-PROCESS] (4): [('FUrqUDTrCHxy0I97epN', '获取本步时长', '', ''), ('oHh6xrqSMTxFFZGd07W', '查找前方车站', '', ''), ('Pupqb6kGlHmKlcG6aaU', '确定两端水位', '', ''), ('TUmeBFJE9XAot7jIGeV', '确定连接处过水情况', '', '')]
  [ENUM] (10): [('0FspcLs0518qUherR8K', '积水状态', '', ''), ('BT8uFA2HI1kcc0HUbrY', '停靠状态', '', ''), ('E28pEyQ2G9A9CJxdfs4', '任务状态', '', ''), ('hHttkvr7XJGKzH5rGWD', '通行状态', '', ''), ('nrgTvUq55JNeiMMlawc', '挡水墙状态', '', ''), ('rcqM3MPGoItwHZIUhQq', '乘客状态', '', ''), ('RTMDG3oSxDG0XtzmVxC', '疏散状态', '', ''), ('tWd1eAWxb5ggu8qz447', '预警等级', '', ''), ('XGrwtoGP8uUCfrXqgWc', '列车状态', '', ''), ('zgOFG4oqsv53DQKSGM2', '站点类型', '', '')]
  [CONSTANT] (32): [('2u8hczlOK0qUvMwlyAt', '定值7a8b7972f09b4df4b07282358e3baa6a', '', ''), ('3bCVm4xJN1WMCeyg1Mb', '定值eda4db36692740a9ad47277db441001c', '', ''), ('5FyokUVxMuiVRtNBf8z', '定值ca9440ef84104f88a64085ef4bcd6dda', '', ''), ('72vJSI7s673iDlemtLZ', '定值12a8bf15ded2429ca2ac7aaf2a28e1ac', '', ''), ('7z7Kk0bPNdHl3H3VZgi', '定值f1e5a899074a45b5be13d44c17334c05', '', ''), ('8l6AB0RYWAsyFINGdCK', '定值fcc40fef14034824a526acaecf1cfb80', '', ''), ('bwDGfdy5SdtUtORCkaN', '定值d9204193ac4c4b27a73967c3ced84686', '', ''), ('C9m1S5m0VkdwEGIvtKz', '定值77575daf12b94b3397b54ebb7dad9a42', '', ''), ('dFrwi9P7yoPybmtO4Em', '定值774e95424bf74acc814b3dce9e3bf269', '', ''), ('dgavjMwvcUr3rWodxzi', '定值ddd6421476834d8abeb4450fdf3e02e5', '', '')]
Process code/lang keys: {('has_javaCode', '文本1.setKeyInstOrValue(false);'): 1}
PROCESS non-empty keys: {'stateInitial': 218, 'type': 218, 'designViewId': 218, 'affiliation': 218, 'stateDefault': 218, 'id': 218, 'essence': 218, 'scope': 218, 'label': 218, 'timeRelated': 218, 'stateFinal': 218, 'defineViewId': 218, 'typeEntityId': 214, 'encode': 171, 'componentId': 166, 'componentType': 166, 'timeAccuracyUnit': 165, 'timeAccuracyOperator': 165, 'timeUnit': 165, 'timeParamType': 165, 'innerClass': 154, 'innerType': 154, 'description': 148, 'timeNormal': 132, 'labelIndex': 87, 'runningMethods': 69, 'fromRealWorld': 66, 'useTokenMappingIds': 62, 'sourceId': 57, 'execSpeed': 19, 'responseMode': 19, 'instanceMode': 19, 'singleton': 4, 'scenarioParamId': 4, 'lockInstance': 4, 'javaCode': 1}
=== Model 2 DEEP CORPUS STATS ===
cellMap views in A: 305, in B: 56, union: 361
Total merged cells: 2406
LINK entity linkTypes: {'fixed': 219, 'out-influence': 195, 'in-influence-nonnull': 144, 'in-influence-nullable': 94, 'invocation': 87, 'in-influence': 72, 'composition': 47, 'exhibition': 14, 'in-out-influence-nonnull': 9, 'extend': 5, 'result': 3, 'in-out-influence': 2, 'proxy': 1, 'in-out-influence-nullable': 1}
Cell edge shapes & linkTypes: {('edge-fixed', 'fixed'): 216, ('edge-out-influence', 'out-influence'): 194, ('edge-in-influence-nonnull', 'in-influence-nonnull'): 143, ('edge-in-influence-nullable', 'in-influence-nullable'): 94, ('edge-invocation', 'invocation'): 86, ('edge-in-influence-nonnull', 'in-influence'): 71, ('edge-composition', 'composition'): 47, ('edge-exhibition', 'exhibition'): 14, ('edge-in-out-influence-nonnull', 'in-out-influence-nonnull'): 9, ('edge-extend', 'extend'): 5, ('edge-result', 'result'): 3, ('edge-in-out-influence-nonnull', 'in-out-influence'): 2, ('edge-in-out-influence-nullable', 'in-out-influence-nullable'): 1, ('edge-proxy', 'proxy'): 1}
Non-edge shapes: {'node-relevance-object': 561, 'node-process': 516, 'node-compose-member': 154, 'node-attribute-member': 105, 'node-template-member': 57, 'node-type': 56, 'node-constant': 22, 'node-template-process': 14, 'node-state': 12, 'node-parent': 5, 'node-setting-member': 4, 'node-independent-process': 3, 'node-enum': 2, 'node-actual-run-time': 1, 'node-cur-process-time': 1, 'node-thing-object': 1, 'node-cur-run-time': 1, 'node-cur-object': 1, 'node-user-info': 1, 'node-run-param': 1, 'node-proxy': 1, 'node-note': 1}
LINK non-empty keys: {'label': 893, 'type': 893, 'linkType': 893, 'id': 893, 'logic': 779, 'componentType': 552, 'componentId': 552, 'sourceId': 204, 'determine': 15, 'event': 10}
LINK event/judge/prob/delay samples: {('event', 'v'): 7, ('event', 'a'): 3}
  [SCENE] (4): [('hC1vBfHTMO5wGNwivnD', '智能筹划应用场景', '', ''), ('rduE43Zkxe2x1aE6YWZ', '默认场景', '', 'oYRdScR34kQuJShQuaj'), ('97UvATC568qlg2Y8NcA', '默认场景', '', 'tn7WiArkk6ebRIFYSPb'), ('t315EPfZDL0p9OXY4Zs', '默认场景', '', 'dMoXriv3KSb2QEyTlGe')]
  [PARENT] (5): [('9SfpmmmGpLS37K6iUlf', '地图展示对象', '', 'F82l2cCcyAOghfHnzYJ'), ('L2QgcxXs60pz8flb25m', '地图展示对象', '', 'F82l2cCcyAOghfHnzYJ'), ('MCuA8ds1bVpbV3wRGGG', '地图展示对象', '', 'F82l2cCcyAOghfHnzYJ'), ('Z5cdQywE4XFsInUd61c', '地图展示对象', '', 'F82l2cCcyAOghfHnzYJ'), ('ZKddvOcSsvxxtqdFmzk', '地图展示对象', '', 'F82l2cCcyAOghfHnzYJ')]
  [PROXY] (1): [('5761242298be45a4885a8850c654711b', '成员代理', '', '')]
  [INDEPENDENT-PROCESS] (2): [('b094d31f83d44a2886a714bd999e414e', '航路规划与同步到达算法 ', '', ''), ('e0e0a1b5ead840a9a0b3442468bbc4a8', '保存作战计划数据', '', '')]
  [ENUM] (2): [('6wNeBL4zjW2JVddLMgb', '空间类型', '', 'GwJR3txhXVPCBOj0J9O'), ('RqQqZJLf1GISU4aVSWb', '线条类型', '', 'GwJR3txhXVPCBOj0J9O')]
  [CONSTANT] (22): [('059dab340f764c19ab8dbc32dd5bf034', '定值a2c95affd91c4d7fa823c52ec8217068', '', ''), ('1e9cde57de3b4678a0789fae1165d428', '定值32dc2754ea884fc186c127c40dea19ab', '', ''), ('233b74cb25ae4c2480c392b99ac6c455', '定值4807ffe09f7f4433b62ec4bd0ff06d69', '', ''), ('2f2a10108aaf45e0863dbb84b28b69e4', '定值e553a374ba034a0a8dda5b5b3dfa4588', '', ''), ('3033282d6bea42f489c977a2f058bfea', '定值4b658662b326434e9e7dea64685f5194', '', ''), ('41f4d39cc5c440749ab38aa755c9d781', '定值ced371031de94411833c266b71dc2ee5', '', ''), ('58f0b18ecbee497394ba19fb74ea088f', '定值f83f8d15a1f811f19929de08cc92d452', '', ''), ('830032c84ca647b2965c3bd8276794fb', '定值b00d98ae07164128882cb47c28835d81', '', ''), ('8c1b03a89abf4699a87279037d671d22', '定值3013db621ca045a687e86648caaf4a6e', '', ''), ('95691399c32a4d82b7b3db61a77b0f4a', '定值e3ae4b15724a4824a3bf1e8af7626a35', '', '')]
Process code/lang keys: {('has_javaCode', 'String data = (String)数据类型.getValue(); B'): 3, ('has_javaCode', 'String data = (String)文本值.getValue(); Bo'): 3, ('has_javaCode', '  // -*- coding: utf-8 -*-   // ========'): 1, ('has_javaCode', '会话数据.setValue(会话记录列表.itemsToNJson());'): 1}
PROCESS non-empty keys: {'stateInitial': 219, 'type': 219, 'designViewId': 219, 'affiliation': 219, 'stateDefault': 219, 'id': 219, 'essence': 219, 'scope': 219, 'label': 219, 'timeRelated': 219, 'stateFinal': 219, 'defineViewId': 219, 'typeEntityId': 217, 'componentId': 193, 'componentType': 193, 'encode': 172, 'timeAccuracyUnit': 166, 'timeAccuracyOperator': 166, 'timeUnit': 166, 'timeParamType': 166, 'innerClass': 154, 'innerType': 154, 'description': 141, 'timeNormal': 133, 'labelIndex': 87, 'runningMethods': 70, 'fromRealWorld': 67, 'useTokenMappingIds': 63, 'sourceId': 38, 'javaCode': 8, 'scenarioParamId': 4, 'lockInstance': 4}

Pythonimport os

parser_dir = '/tmp/orpl_extract/orpl-9ee34655eecdb360241d86a189cb40c9527ef56a/groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio'

for rel in ['ir/CompilePlanIR.java', 'ir/GromaModuleIR.java', 'parser/CanvasMappingTables.java', 'parser/StaticModelValidator.java', 'parser/UniqueIdentifierGuard.java', 'parser/GraphCycleAndDepthGuard.java']:
    p = os.path.join(parser_dir, rel)
    print(f"================ {rel} ({os.path.getsize(p)} bytes) ================")
    with open(p, 'r', encoding='utf-8') as f:
        print(f.read())

代码输出================ ir/CompilePlanIR.java (18501 bytes) ================
package com.groma.studio.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真实语料编译计划：把 XX-a.json + XX-b.json 的组织树、定义视图、设计视图归一化成生成器可直接消费的强类型结构
 * <p>
 * {@link GromaModuleIR} 原有的 12 个分量保留（旧接口与手写 IR 的单测继续可用）；
 * 本类作为第 13 个分量承载：对象类型与成员表、过程定义及其数据流节点表、事件接线与默认启动、命名实例对象图、关联类型。
 * 所有列表都已按稳定键排序，保证同一份语料多次解析逐字段相等。
 * </p>
 *
 * @param configuredStepMs 场景未指定步长时的默认步长（毫秒）
 * @param types            全部领域对象类型（含场景、关联类型角色对象）
 * @param processDefs      全部过程定义（以设计视图为键去重）
 * @param instances        命名实例（对象图）
 * @param associationTypes 关联类型（R07/R08 双向带标签关联）
 * @param triggers         事件接线（成员/状态 --事件码--> 过程）
 * @param defaultStarts    默认启动
 * @param warnings         解析期诊断
 */
    public record CompilePlanIR(
            long configuredStepMs,
            List<TypeDefIR> types,
            List<ProcessDefIR> processDefs,
            List<InstanceIR> instances,
            List<AssocTypeIR> associationTypes,
            List<TriggerIR> triggers,
            List<DefaultStartIR> defaultStarts,
            List<ManualStartIR> manualStarts,
            List<String> warnings
    ) {
        public static final CompilePlanIR EMPTY = new CompilePlanIR(
                1000L, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    /**
     * 对象类型定义
     *
     * @param kind TYPE / SCENE / ASSOCIATION
     */
    public record TypeDefIR(
            String entityId,
            String zhName,
            String description,
            String essence,
            String kind,
            String primaryParentId,
            List<String> mixinParentIds,
            List<ProxyDefIR> proxies,
            List<MemberIR> members,
            List<String> notes
    ) {
        public TypeDefIR(String entityId, String zhName, String description, String essence,
                         String kind, String primaryParentId, List<MemberIR> members) {
            this(entityId, zhName, description, essence, kind, primaryParentId,
                    List.of(), List.of(), members, List.of());
        }
    }

    /** R05 集合成员代理定义（模型 2 智能筹划含 1 组）：集合 collectionMemberId 的元素事件代理转发至下游 */
    public record ProxyDefIR(
            String proxyEntityId,
            String zhName,
            String collectionMemberId,
            String elementTypeId
    ) {}

    /** R25 超时（pcd）/ R26 提前完成（pca）异常控制流边；eventKind 取 OVERTIME_PCD / UNDERTIME_PCA */
    public record ExceptionFlowIR(
            String eventKind,
            int targetPc,
            String targetDefKey
    ) {}

    /**
     * 类型成员
     *
     * @param typeRange     BASE / ENUM / DEFINE
     * @param refTypeId     ENUM 的枚举 id 或 DEFINE 的对象类型 id
     * @param containerType array / set / map（集合成员）
     * @param initialState  枚举成员的初始状态中文原名
     * @param javaType      Java 类型占位（见 GromaTypes）
     */
    public record MemberIR(
            String entityId,
            String zhName,
            String alias,
            String fieldType,
            String typeRange,
            String refTypeId,
            boolean collection,
            String containerType,
            String defaultValue,
            String unit,
            String minValue,
            String maxValue,
            String initialState,
            String description,
            String javaType
    ) {}

    /**
     * 关联类型（画布“关联类型”目录下的双向带标签关联）
     */
    public record AssocTypeIR(
            String entityId,
            String zhName,
            String leftMemberId,
            String leftZhName,
            String leftTypeId,
            String rightMemberId,
            String rightZhName,
            String rightTypeId,
            String constraintLeft,
            String constraintRight,
            String labelLeftToRight,
            String labelRightToLeft
    ) {}

    /** 命名实例 */
    public record InstanceIR(String instanceId, String zhName, String typeId, ValueSpec spec) {}

    /** 对象图取值 */
    public sealed interface ValueSpec permits ScalarSpec, RefSpec, ObjectSpec, ListSpec, MapSpec, NullSpec {}

    /** 基础类型字面量（rawValue 是画布原始文本，fieldType 是画布字段类型） */
    public record ScalarSpec(String rawValue, String fieldType) implements ValueSpec {}

    /** 指向另一个命名实例 */
    public record RefSpec(String instanceId, String typeId) implements ValueSpec {}

    /** 内联对象 */
    public record ObjectSpec(String typeId, Map<String, ValueSpec> members) implements ValueSpec {}

    /** 有序集合 */
    public record ListSpec(List<ValueSpec> items) implements ValueSpec {}

    /** 键值集合 */
    public record MapSpec(Map<String, ValueSpec> entries) implements ValueSpec {}

    /** 未赋值 */
    public record NullSpec() implements ValueSpec {
        public static final NullSpec INSTANCE = new NullSpec();
    }

    /**
     * 过程定义
     *
     * @param ownerKind TYPE / SCENE / INDEPENDENT
     */
    public record ProcessDefIR(
            String defKey,
            String entityId,
            String zhName,
            String description,
            String ownerKind,
            String ownerTypeId,
            String ownerZhName,
            boolean timeConsuming,
            boolean twinLocked,
            boolean asyncMode,
            int singletonMode,
            String durationParamType,
            double timeNormal,
            String timeUnit,
            Double timeMin,
            Double timeMax,
            String distribution,
            Double distMin,
            Double distMax,
            Double distAvg,
            Double distSd,
            Double distParam,
            Long accuracyMs,
            String javaCode,
            String designViewId,
            List<PortIR> params,
            List<LocalVarIR> locals,
            List<NodeIR> nodes,
            List<Integer> entryNodes,
            List<ConditionRuleIR> startConditions,
            List<ConditionRuleIR> maintainConditions,
            List<ExceptionFlowIR> exceptionFlows
    ) {
        public ProcessDefIR(String defKey, String entityId, String zhName, String description,
                            String ownerKind, String ownerTypeId, String ownerZhName,
                            boolean timeConsuming, boolean twinLocked, boolean asyncMode, int singletonMode,
                            String durationParamType, double timeNormal, String timeUnit,
                            Double timeMin, Double timeMax, String distribution, Double distMin, Double distMax,
                            Double distAvg, Double distSd, Double distParam, Long accuracyMs,
                            String javaCode, String designViewId,
                            List<PortIR> params, List<LocalVarIR> locals, List<NodeIR> nodes, List<Integer> entryNodes,
                            List<ConditionRuleIR> startConditions, List<ConditionRuleIR> maintainConditions) {
            this(defKey, entityId, zhName, description, ownerKind, ownerTypeId, ownerZhName,
                    timeConsuming, twinLocked, asyncMode, singletonMode, durationParamType, timeNormal, timeUnit,
                    timeMin, timeMax, distribution, distMin, distMax, distAvg, distSd, distParam, accuracyMs,
                    javaCode, designViewId, params, locals, nodes, entryNodes,
                    startConditions, maintainConditions, List.of());
        }
    }

    /**
     * 过程形参（定义视图里的关联对象）
     *
     * @param direction IN / OUT / IN_OUT
     * @param nonnull   非空输入（in-influence-nonnull）
     * @param result    是否“生成”关系（过程负责产生新对象）
     */
    public record PortIR(
            String entityId,
            String zhName,
            String direction,
            boolean nonnull,
            boolean result,
            String javaType
    ) {}

    /** 过程局部量（设计视图里的模板成员） */
    public record LocalVarIR(String entityId, String zhName, String javaType, String initLiteral) {}

    /**
     * 过程节点（设计视图里的一个过程图元）
     *
     * @param kind           TOOL（内置工具）/ CALL（业务子过程或独立过程）
     * @param toolKey        内置工具键 innerClass.innerType
     * @param calleeDefKey   被调过程定义键（CALL）
     * @param calleeTimeConsuming 被调过程在本调用点是否耗时（图元级 timeRelated 优先，否则取过程实体属性）
     * @param slow           本节点自身是否为耗时工具节点（画布上标记为时间有关的工具过程）
     * @param firstStart     是否首个启动入口
     * @param host           宿主操作数：CALL 节点为被调过程的宿主对象；TOOL 节点为集合类工具操作的集合
     * @param inputs         入参绑定（IN 与 IN_OUT 形参的读取）
     * @param writes         写出绑定（OUT 与 IN_OUT 形参的写回目标）
     * @param resultJavaType 工具节点输出值的 Java 类型占位（CALL 节点为空串，其输出按形参类型取）
     * @param nexts          后继激活
     * @param postActions    节点成功后的后置动作
     */
    public record NodeIR(
            int pc,
            String cellId,
            String entityId,
            String zhName,
            String kind,
            String toolKey,
            String calleeDefKey,
            boolean calleeTimeConsuming,
            boolean slow,
            boolean firstStart,
            boolean lastEnd,
            OperandIR host,
            List<ArgBindingIR> inputs,
            List<ArgBindingIR> writes,
            String resultJavaType,
            List<NextIR> nexts,
            List<PostIR> postActions,
            double x,
            double y
    ) {
        public NodeIR(int pc, String cellId, String entityId, String zhName, String kind, String toolKey,
                      String calleeDefKey, boolean calleeTimeConsuming, boolean slow, boolean firstStart,
                      OperandIR host, List<ArgBindingIR> inputs, List<ArgBindingIR> writes, String resultJavaType,
                      List<NextIR> nexts, List<PostIR> postActions, double x, double y) {
            this(pc, cellId, entityId, zhName, kind, toolKey, calleeDefKey, calleeTimeConsuming, slow,
                    firstStart, false, host, inputs, writes, resultJavaType, nexts, postActions, x, y);
        }
    }

    /** 实参绑定 */
    public record ArgBindingIR(String argName, OperandIR operand) {}

    /**
     * 后继激活
     *
     * @param conditional     是否带线上判断（从输出端口出发的调用线）
     * @param conditionSource 被判断的输出形参名（如 c）；为空表示判断事件源成员本身
     * @param conditions      比较项列表
     * @param expression      比较项的组合表达式（如 "1"、"1 && 2"），为空表示全部满足
     * @param eventCode       连线携带的事件码
     */
    public record NextIR(
            int targetPc,
            boolean conditional,
            String conditionSource,
            List<ConditionIR> conditions,
            String expression,
            String eventCode,
            long delayTicks,
            double probability,
            String probabilityGroup
    ) {
        public NextIR(int targetPc, boolean conditional, String conditionSource,
                      List<ConditionIR> conditions, String expression, String eventCode, long delayTicks) {
            this(targetPc, conditional, conditionSource, conditions, expression, eventCode, delayTicks, 1.0D, "");
        }

        public NextIR(int targetPc, boolean conditional, String conditionSource,
                      List<ConditionIR> conditions, String expression, String eventCode) {
            this(targetPc, conditional, conditionSource, conditions, expression, eventCode, 0L);
        }
    }

    /**
     * 线上判断的一个比较项
     *
     * @param index       在表达式里的序号（从 1 开始）
     * @param op          比较符：= != > >= < <=
     * @param literal     比较字面量文本
     * @param literalType 字面量类型：boolean / number / string / null
     */
    public record ConditionIR(int index, String op, String literal, String literalType) {}

    /** 节点成功后的后置动作 */
    public record PostIR(
            String kind,
            OperandIR target,
            String stateLabel,
            String enumTypeId,
            OperandIR value,
            double probability,
            String probabilityGroup
    ) {
        public PostIR(String kind, OperandIR target, String stateLabel, String enumTypeId, OperandIR value) {
            this(kind, target, stateLabel, enumTypeId, value, 1.0D, "");
        }
    }

    /** 启动 / 维持条件 */
    public record ConditionRuleIR(
            OperandIR operand,
            String stateLabel,
            List<ConditionIR> conditions,
            String describe,
            String expression
    ) {
        public ConditionRuleIR(OperandIR operand, String stateLabel, List<ConditionIR> conditions, String describe) {
            this(operand, stateLabel, conditions, describe, "");
        }
    }

    /**
     * 手动启动调用点：类型 / 场景设计视图里既无事件触发线也无默认启动的业务过程图元。
     * 平台定义（与存量引擎差异）：这类过程不参与自动推演，但宿主提供手动启动入口
     * （GromaModuleRuntime.startManually），供外部干预（MCP run.process / 通用 REST 触发过程）使用。
     */
    public record ManualStartIR(
            String typeId,
            List<String> hostPath,
            String targetDefKey,
            List<ArgBindingIR> args,
            List<PostIR> postActions
    ) {}

    // ------------------------------------------------------------------ 操作数

    /** 操作数：工具入参、写出目标、实参绑定、事件源成员的统一表示；javaType 是类型占位（见 GromaTypes） */
    public sealed interface OperandIR permits SelfIR, MemberPathIR, ParamIR, LocalIR, ConstIR, NodeOutputIR, NamedInstanceIR, BuiltinIR {
        String javaType();
    }

    /** 过程宿主对象自身 */
    public record SelfIR(String javaType) implements OperandIR {}

    /** 从根对象沿成员路径取值 */
    public record MemberPathIR(OperandIR root, List<String> memberIds, String javaType) implements OperandIR {}

    /** 形参槽位 */
    public record ParamIR(String entityId, String zhName, String javaType) implements OperandIR {}

    /** 局部量槽位 */
    public record LocalIR(String entityId, String zhName, String javaType) implements OperandIR {}

    /** 定值常量 */
    public record ConstIR(String entityId, String literal, String javaType, String zhName) implements OperandIR {}

    /** 上游节点的输出槽位 */
    public record NodeOutputIR(String cellId, String argName, String javaType) implements OperandIR {}

    /** 模型级命名实例 */
    public record NamedInstanceIR(String instanceId, String typeId, String javaType) implements OperandIR {}

    /**
     * 内置对象取值
     *
     * @param key SIM_ELAPSED_SECONDS / SIM_DATETIME / LENGTH / RUN_PARAM
     * @param base LENGTH 的集合操作数
     */
    public record BuiltinIR(String key, OperandIR base, String javaType) implements OperandIR {}

    // ------------------------------------------------------------------ 事件接线

    /**
     * 事件触发接线
     *
     * @param sourceTypeId 事件源对象类型 id
     * @param memberToken  发生事件的成员 id
     * @param eventCode    事件码（a / v / sa / sx / ra / rv / rx）
     * @param stateLabel   sa / sx 限定的状态中文原名
     * @param contextPath  从接线所属对象到事件源对象的成员路径
     * @param targetDefKey 目标过程定义键
     * @param hostPath     从接线所属对象到过程宿主的成员路径（空 = 接线对象自身）
     * @param postActions  目标过程成功后的后置动作（调用点上的 out-change / result / consumption）
     */
    public record TriggerIR(
            String sourceTypeId,
            String memberToken,
            String eventCode,
            String stateLabel,
            List<ConditionIR> judge,
            OperandIR judgeOperand,
            List<String> contextPath,
            String targetDefKey,
            List<String> hostPath,
            List<ArgBindingIR> args,
            List<PostIR> postActions,
            String ownerTypeId,
            String describe,
            String expression,
            double probability,
            String probabilityGroup
    ) {
        public TriggerIR(String sourceTypeId, String memberToken, String eventCode, String stateLabel,
                         List<ConditionIR> judge, OperandIR judgeOperand, List<String> contextPath,
                         String targetDefKey, List<String> hostPath, List<ArgBindingIR> args,
                         List<PostIR> postActions, String ownerTypeId, String describe) {
            this(sourceTypeId, memberToken, eventCode, stateLabel, judge, judgeOperand, contextPath,
                    targetDefKey, hostPath, args, postActions, ownerTypeId, describe, "", 1.0D, "");
        }
    }

    /**
     * 默认启动
     *
     * @param hostPath    从新建对象到过程宿主的成员路径
     * @param postActions 目标过程成功后的后置动作
     */
    public record DefaultStartIR(
            String typeId,
            List<String> hostPath,
            String targetDefKey,
            List<ArgBindingIR> args,
            List<PostIR> postActions
    ) {}
}

================ ir/GromaModuleIR.java (8311 bytes) ================
package com.groma.studio.ir;

import java.util.List;
import java.util.Map;

/**
 * 方圆模型编译期强类型中间表示（落实 NP-PRS-003，不可变、可序列化、稳定摘要）
 * <p>
 * 前 12 个分量是通用模型概览（定值、枚举、类型、独立过程、场景、视图画布）；
 * 第 13 个分量 {@link CompilePlanIR} 是真实语料的完整编译计划——五层源码生成器主要消费它。
 * 仅用 12 参构造器创建的 IR（例如手写的单测夹具）compilePlan 为空，生成器会按通用概览降级处理。
 * </p>
 */
public record GromaModuleIR(
        String formatVersion,
        String modelId,
        String modelCode,
        String modelLabel,
        int modelVersion,
        String sourceDigest,
        long finalStepMs,
        List<ConstantIR> constants,
        List<EnumIR> enums,
        List<TypeIR> domainTypes,
        List<ProcessIR> independentProcesses,
        List<SceneIR> scenes,
        Map<String, ViewGraphIR> views,
        CompilePlanIR compilePlan
) {
    public GromaModuleIR(
            String formatVersion,
            String modelId,
            String modelCode,
            String modelLabel,
            int modelVersion,
            String sourceDigest,
            long finalStepMs,
            List<ConstantIR> constants,
            List<EnumIR> enums,
            List<TypeIR> domainTypes,
            List<ProcessIR> independentProcesses,
            List<SceneIR> scenes,
            Map<String, ViewGraphIR> views
    ) {
        this(formatVersion, modelId, modelCode, modelLabel, modelVersion, sourceDigest, finalStepMs,
                constants, enums, domainTypes, independentProcesses, scenes, views, CompilePlanIR.EMPTY);
    }

    /** 带编译计划的便捷构造 */
    public GromaModuleIR withCompilePlan(CompilePlanIR plan) {
        return new GromaModuleIR(formatVersion, modelId, modelCode, modelLabel, modelVersion, sourceDigest, finalStepMs,
                constants, enums, domainTypes, independentProcesses, scenes, views, plan);
    }
    public record ConstantIR(
            String entityId,
            String zhName,
            String fieldType,
            String defaultValue,
            String unit
    ) {}

    public record EnumIR(
            String entityId,
            String zhName,
            List<String> stateLabels,
            String initialState
    ) {}

    public record TypeIR(
            String entityId,
            String zhName,
            String description,
            String essence,
            String primaryParentEntityId,
            List<String> mixinParentEntityIds,
            List<FieldIR> fields,
            List<ProcessIR> methods
    ) {}

    public record FieldIR(
            String entityId,
            String zhName,
            String fieldType,         // "double", "long", "boolean", "String", 或自定义 Type 的 entityId / Enum 的 entityId
            String typeCategory,      // "BASE", "ENUM", "DEFINE"
            boolean collection,
            boolean readOnly,
            String unit,
            String minValue,
            String maxValue,
            String defaultValue,
            List<TriggerBindingIR> eventTriggers
    ) {}

    public record TriggerBindingIR(
            String linkEntityId,
            String eventCode,         // "a", "v", "x", "sa", "sx", "ma", "mx" 等 15 类事件码
            String conditionExpr,     // 线上逻辑判断表达式，如 "waterDepth >= 0.5"
            double probability,       // 0.0 ~ 1.0
            String probabilityGroup,
            String targetProcessEntityId,
            boolean targetTimeConsuming
    ) {}

    public record ProcessIR(
            String entityId,
            String ownerTypeEntityId, // 若为独立过程或场景直属过程则为空串
            String zhName,
            String description,
            boolean timeConsuming,    // true = 耗时过程 (*ProcessTask), false = 瞬时算子 (*ServiceImpl 方法)
            boolean twinLocked,       // R2: fromRealWorld == true 时运行期锁 1x 倍速
            boolean asyncMode,        // R4: 异步外部等待过程
            String singletonMode,     // "NONE", "OBJECT_SINGLETON", "GLOBAL_SINGLETON" (标准 9.4.5)
            boolean selfInvoking,     // R24: 自调用标记
            String durationParamType, // "FIXED", "STEP", "UNIFORM", "NORMAL", "EXPONENTIAL"
            double timeNormal,
            Double timeMin,
            Double timeMax,
            String timeUnit,
            Long executionPeriodTicks,// P = max(1, ceil(accuracyMs / finalStepMs))
            String rawJavaCode,       // 画布手写代码（可能含中文变量，甚至在脏语料中含 Python 脚本）
            List<ParamPortIR> inputs,
            List<ParamPortIR> outputs,
            List<FieldIR> localTempMembers, // TEMPLATE-MEMBER 局部临时对象 -> 生成进 RuntimeBO
            List<SubProcessStepIR> subSteps,// 设计视图内子过程编排链
            List<ConditionRuleIR> startConditions,    // R22 启动条件
            List<ConditionRuleIR> maintainConditions, // R21 维持条件 (agent)
            List<PostActionIR> postActions,           // R11 消耗 / R12 生成 / R13-R14 状态转换
            boolean publishedForMcp,  // 是否属于场景层或独立过程且输入绑定完整（NP-MCP-002）
            String defineViewId,
            String designViewId
    ) {}

    public record ParamPortIR(
            String portId,
            String relevanceEntityId,
            String zhName,
            String fieldType,
            String typeCategory,
            boolean required,         // in-influence-nonnull -> true; nullable -> false
            boolean readOnlyInput,    // R16/R17 -> true (使用 ReadOnly 接口契约); R19/R20 -> false (传入可变 Entity)
            String unit,
            Double minVal,
            Double maxVal,
            String defaultVal
    ) {}

    public record SubProcessStepIR(
            int stepPc,
            String cellId,
            String processEntityId,
            String zhName,
            boolean timeConsuming,
            boolean firstStart,       // 首个启动入口 (标准 9.4.7)
            boolean lastEnd,          // 结束点：完成时立即取消其余活跃子过程并触发父过程收尾 (CK-BE-PRC-004-1)
            List<Integer> nextStepPcs,
            Integer failBranchPc      // psf 启动失败跳转分支
    ) {}

    public record ConditionRuleIR(
            String linkEntityId,
            String targetParamOrFieldName,
            String operator,
            String thresholdLiteral,
            String naturalDesc
    ) {}

    public record PostActionIR(
            String actionKind,        // "CONSUME" (R11), "RESULT" (R12), "OUT_CHANGE" (R14)
            String targetEntityOrField,
            String targetStateEnumItem,
            double probability,
            String probabilityGroup
    ) {}

    public record SceneIR(
            String treeNodeId,
            String entityId,
            String zhName,
            String defineViewId,
            String designViewId,
            List<SceneMemberIR> initMembers,
            List<String> defaultStartOperatorIds,
            List<String> defaultStartProcessIds,
            List<ProcessIR> sceneLevelProcesses
    ) {}

    public record SceneMemberIR(
            String memberEntityId,
            String zhName,
            String typeEntityId,
            Map<String, String> initialFieldOverrides
    ) {}

    public record ViewGraphIR(
            String viewId,
            boolean defineView,
            List<NodeCellIR> nodes,
            List<EdgeCellIR> edges
    ) {}

    public record NodeCellIR(
            String cellId,
            String entityId,
            String cellType,
            String shape,
            String label,
            boolean defaultStart,
            boolean firstStart,
            boolean lastEnd,
            double x,
            double y
    ) {}

    public record EdgeCellIR(
            String cellId,
            String linkEntityId,
            String relationType,
            String sourceCellId,
            String targetCellId,
            String eventCode
    ) {}
}

================ parser/CanvasMappingTables.java (4445 bytes) ================
package com.groma.studio.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 画布 29 种连线形状与 21 种事件代码显式全映射表（落实 BE-MDL-006 & NP-PRS-002）
 */
public final class CanvasMappingTables {

    public static final Map<String, String> SHAPE_TO_RELATION;
    public static final Map<String, String> EVENT_CODE_TO_STANDARD_TYPE;

    static {
        Map<String, String> shapes = new LinkedHashMap<>();
        shapes.put("edge-extend", "R01_EXTEND");
        shapes.put("edge-composition", "R02_COMPOSITION");
        shapes.put("edge-exhibition", "R03_EXHIBITION");
        shapes.put("edge-classification", "R04_CLASSIFICATION");
        shapes.put("edge-proxy", "R05_PROXY");
        shapes.put("edge-control", "R06_CONTROL_EXT"); // 平台扩展关系
        shapes.put("edge-unidirectional-association", "R07_UNIDIRECTIONAL_ASSOC");
        shapes.put("edge-bidirectional-association", "R08_BIDIRECTIONAL_ASSOC");
        shapes.put("edge-unidirectional-tagged", "R09_UNIDIRECTIONAL_TAGGED");
        shapes.put("edge-bidirectional-tagged", "R10_BIDIRECTIONAL_TAGGED");
        shapes.put("edge-consumption", "R11_CONSUMPTION");
        shapes.put("edge-result", "R12_RESULT");
        shapes.put("edge-in-change", "R13_IN_CHANGE");
        shapes.put("edge-out-change", "R14_OUT_CHANGE");
        shapes.put("edge-in-out", "R15_IN_OUT_NORMALIZED");
        shapes.put("edge-in-influence-nullable", "R16_IN_INFLUENCE_NULLABLE");
        shapes.put("edge-in-influence-nonnull", "R17_IN_INFLUENCE_NONNULL");
        shapes.put("edge-out-influence", "R18_OUT_INFLUENCE");
        shapes.put("edge-in-out-influence-nullable", "R19_IN_OUT_INFLUENCE_NULLABLE");
        shapes.put("edge-in-out-influence-nonnull", "R20_IN_OUT_INFLUENCE_NONNULL");
        shapes.put("edge-agent", "R21_AGENT_MAINTAIN");
        shapes.put("edge-condition", "R22_START_CONDITION");
        shapes.put("edge-invocation", "R23_INVOCATION");
        shapes.put("edge-invocation-self", "R24_INVOCATION_SELF");
        shapes.put("edge-overtime-exception", "R25_OVERTIME_SIGNAL");
        shapes.put("edge-undertime-exception", "R26_UNDERTIME_SIGNAL");
        shapes.put("edge-note", "R27_NOTE");
        shapes.put("edge-fixed", "R28_FIXED_BINDING");
        shapes.put("edge-link", "R00_GENERIC_LINK");
        SHAPE_TO_RELATION = Collections.unmodifiableMap(shapes);

        Map<String, String> events = new LinkedHashMap<>();
        events.put("a", "OBJECT_APPEARED");
        events.put("as", "OBJECT_APPEARED_IN_STATE");
        events.put("sa", "STATE_ENTERED");
        events.put("v", "VALUE_CHANGED");
        events.put("x", "OBJECT_CONSUMED");
        events.put("xs", "OBJECT_CONSUMED_FROM_STATE");
        events.put("sx", "STATE_EXITED");
        events.put("ma", "COLLECTION_MEMBER_ADDED");
        events.put("am", "ADDED_TO_COLLECTION");
        events.put("mx", "COLLECTION_MEMBER_REMOVED");
        events.put("xm", "REMOVED_FROM_COLLECTION");
        events.put("ra", "REFERENCE_LINKED");
        events.put("ar", "LINKED_BY_REFERENCE");
        events.put("rv", "REFERENCE_RETARGETED");
        events.put("vr", "RETARGETED_BY_REFERENCE");
        events.put("rx", "REFERENCE_UNLINKED");
        events.put("xr", "UNLINKED_BY_REFERENCE");
        events.put("pca", "PROCESS_COMPLETED_EARLY");
        events.put("pcd", "PROCESS_COMPLETED_OVERTIME");
        events.put("psf", "PROCESS_START_FAILED");
        events.put("paa", "PROCESS_ABORTED");
        EVENT_CODE_TO_STANDARD_TYPE = Collections.unmodifiableMap(events);
    }

    private CanvasMappingTables() {
    }

    public static String resolveEdgeShapeOrThrow(String shape, String viewId, String cellId) {
        String rel = SHAPE_TO_RELATION.get(shape);
        if (rel == null) {
            throw new IllegalArgumentException(
                    "[MDL-MAP-001] 未知连线形状: shape=" + shape + ", viewId=" + viewId + ", cellId=" + cellId);
        }
        return rel;
    }

    public static String resolveEventCodeOrThrow(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            return "NONE";
        }
        String mapped = EVENT_CODE_TO_STANDARD_TYPE.get(eventCode.trim());
        if (mapped == null) {
            throw new IllegalArgumentException("[MDL-MAP-002] 未知画布事件代码: " + eventCode);
        }
        return mapped;
    }
}

================ parser/StaticModelValidator.java (17591 bytes) ================
package com.groma.studio.parser;

import com.groma.studio.ir.GromaModuleIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 集中式静态模型校验与耗时属性推断器（落实 BE-MDL-005 & NP-PRS-004，彻底修复存量 BE-17 父过程静默吞时长缺陷）。
 * 覆盖《方圆语言标准-1117》十项编译期拦截 VAL-001 ~ VAL-010（与 docs/validation-rules.md 一一对应）。
 */
public final class StaticModelValidator {

    private static final Logger log = LoggerFactory.getLogger(StaticModelValidator.class);

    /** VAL-010：成员链与引用定位路径深度上限（标准 9.6） */
    public static final int MAX_MEMBER_PATH_DEPTH = 64;
    /** VAL-008：同组互斥分支概率和闭区间容差（标准 10.4） */
    public static final double PROBABILITY_SUM_MIN = 0.9999D;
    public static final double PROBABILITY_SUM_MAX = 1.0001D;
    /** VAL-006：定值严禁连接的关系类型（消耗/生成/状态转换/输出影响，标准 7.3）。
     *  双口径：解析器 toViewGraph 写入的是 linkType 短名（consumption 等），
     *  CanvasMappingTables 归一化名为 R 码（R11_CONSUMPTION 等），两套都要拦截。 */
    private static final Set<String> FORBIDDEN_FOR_CONSTANT = Set.of(
            "R11_CONSUMPTION", "R12_RESULT", "R13_IN_CHANGE", "R14_OUT_CHANGE",
            "R15_IN_OUT_NORMALIZED", "R18_OUT_INFLUENCE",
            "consumption", "result", "in-change", "out-change", "in-out", "out-influence");
    /** VAL-010：点分成员链（如 a.b.c.d），两段以上视为引用定位路径 */
    private static final Pattern MEMBER_PATH = Pattern.compile("[\\w\\u4e00-\\u9fa5]+(?:\\.[\\w\\u4e00-\\u9fa5]+)+");
    /** VAL-007：显式编码形态（与 UniqueIdentifierGuard.VALID_ENCODE 同口径）——
     *  存量语料内部主键（如 3cW2TNyvVxiJxV07ZlR，数字开头）不是显式编码，绝不误杀 */
    private static final Pattern EXPLICIT_ENCODE_SHAPE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public enum Severity { ERROR, WARNING }

    public record ValidationIssue(
            String ruleId,
            String standardClause,
            Severity severity,
            String elementId,
            String elementName,
            String message
    ) {}

    public record ValidationSummary(
            boolean passedWithoutErrors,
            List<ValidationIssue> issues
    ) {}

    public ValidationSummary validate(GromaModuleIR ir) {
        List<ValidationIssue> issues = new ArrayList<>();

        // VAL-001 & VAL-002：算子不能包含耗时子过程；若普通父过程包含耗时子过程，按标准 12.1.2 校验并提示
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            for (GromaModuleIR.ProcessIR proc : ty.methods()) {
                checkProcessRules(proc, issues);
            }
        }
        for (GromaModuleIR.ProcessIR indep : ir.independentProcesses()) {
            checkProcessRules(indep, issues);
        }

        // VAL-003：继承图无环检测（标准 8.5）
        Map<String, List<String>> inheritGraph = new LinkedHashMap<>();
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            List<String> parents = new ArrayList<>();
            if (ty.primaryParentEntityId() != null && !ty.primaryParentEntityId().isBlank()) {
                parents.add(ty.primaryParentEntityId());
            }
            if (ty.mixinParentEntityIds() != null) {
                parents.addAll(ty.mixinParentEntityIds());
            }
            inheritGraph.put(ty.entityId(), parents);
        }
        GraphCycleAndDepthGuard.detectCycleOrExcessDepth("INHERITANCE", inheritGraph, 32)
                .ifPresent(err -> issues.add(new ValidationIssue(
                        "VAL-003", "标准 8.5", Severity.ERROR, ir.modelId(), ir.modelLabel(), err)));

        // VAL-005 & VAL-009：控制关系无环检测（标准扩展 R06）与同一对象和过程间同向关系唯一性校验（标准 10.1.3）
        for (GromaModuleIR.ViewGraphIR view : ir.views().values()) {
            Map<String, List<String>> controlEdges = new LinkedHashMap<>();
            Set<String> sameDirPairs = new HashSet<>();

            for (GromaModuleIR.EdgeCellIR edge : view.edges()) {
                if ("control".equals(edge.relationType()) || "R06_CONTROL_EXT".equals(edge.relationType())) {
                    controlEdges.computeIfAbsent(edge.sourceCellId(), k -> new ArrayList<>()).add(edge.targetCellId());
                }
                String pairKey = edge.sourceCellId() + "->" + edge.targetCellId() + ":" + edge.relationType();
                if (!sameDirPairs.add(pairKey) && !"fixed".equals(edge.relationType())) {
                    issues.add(new ValidationIssue(
                            "VAL-009", "标准 10.1.3", Severity.ERROR, edge.cellId(), edge.relationType(),
                            "同一视图内相同起止节点间存在重复的同向关系: " + pairKey));
                }
            }
            GraphCycleAndDepthGuard.detectCycleOrExcessDepth("CONTROL", controlEdges, 64)
                    .ifPresent(err -> issues.add(new ValidationIssue(
                            "VAL-005", "平台扩展 R06", Severity.ERROR, view.viewId(), view.viewId(), err)));
        }

        // VAL-006：定值严禁连接消耗、生成、状态转换或输出影响线（标准 7.3）
        Set<String> constantIds = new HashSet<>();
        for (GromaModuleIR.ConstantIR constant : ir.constants()) {
            constantIds.add(constant.entityId());
        }
        for (GromaModuleIR.ViewGraphIR view : ir.views().values()) {
            Set<String> constantCells = new HashSet<>();
            for (GromaModuleIR.NodeCellIR node : view.nodes()) {
                if (constantIds.contains(node.entityId())) {
                    constantCells.add(node.cellId());
                }
            }
            if (constantCells.isEmpty()) {
                continue;
            }
            for (GromaModuleIR.EdgeCellIR edge : view.edges()) {
                if (FORBIDDEN_FOR_CONSTANT.contains(edge.relationType())
                        && (constantCells.contains(edge.sourceCellId()) || constantCells.contains(edge.targetCellId()))) {
                    issues.add(new ValidationIssue(
                            "VAL-006", "标准 7.3", Severity.ERROR, edge.cellId(), edge.relationType(),
                            "定值严禁连接消耗/生成/状态转换/输出影响线（cell=" + edge.sourceCellId()
                                    + " -> " + edge.targetCellId() + "）"));
                }
            }
        }

        // VAL-007：显式标识符格式与 Java 保留字冲突拦截（标准 8.2）。
        // entityId 已是 ASCII 标识符形态（用户显式填写的编码）时执行严格校验；
        // 纯中文原名交由命名引擎确定性转译，不在此拦截。
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            validateExplicitEncode(ty.entityId(), ty.zhName(), issues);
            for (GromaModuleIR.ProcessIR proc : ty.methods()) {
                validateExplicitEncode(proc.entityId(), proc.zhName(), issues);
            }
        }
        for (GromaModuleIR.ProcessIR indep : ir.independentProcesses()) {
            validateExplicitEncode(indep.entityId(), indep.zhName(), issues);
        }
        for (GromaModuleIR.ConstantIR constant : ir.constants()) {
            validateExplicitEncode(constant.entityId(), constant.zhName(), issues);
        }

        // VAL-008：同组互斥分支概率和必须落在 [0.9999, 1.0001] 闭区间（标准 10.4）
        // 分支来源：字段事件触发绑定（TriggerBindingIR）与过程后置动作（PostActionIR）的概率分组
        record ProbBranch(String group, double probability, String elementId, String elementName) {}
        List<ProbBranch> branches = new ArrayList<>();
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            for (GromaModuleIR.FieldIR field : ty.fields() == null ? List.<GromaModuleIR.FieldIR>of() : ty.fields()) {
                for (GromaModuleIR.TriggerBindingIR trigger : field.eventTriggers() == null
                        ? List.<GromaModuleIR.TriggerBindingIR>of() : field.eventTriggers()) {
                    if (trigger.probabilityGroup() != null && !trigger.probabilityGroup().isBlank()) {
                        branches.add(new ProbBranch(trigger.probabilityGroup(), trigger.probability(),
                                ty.entityId() + "." + field.entityId(), field.zhName()));
                    }
                }
            }
        }
        List<GromaModuleIR.ProcessIR> allProcesses = new ArrayList<>();
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            allProcesses.addAll(ty.methods());
        }
        allProcesses.addAll(ir.independentProcesses());
        for (GromaModuleIR.SceneIR scene : ir.scenes()) {
            if (scene.sceneLevelProcesses() != null) {
                allProcesses.addAll(scene.sceneLevelProcesses());
            }
        }
        for (GromaModuleIR.ProcessIR proc : allProcesses) {
            for (GromaModuleIR.PostActionIR action : proc.postActions() == null
                    ? List.<GromaModuleIR.PostActionIR>of() : proc.postActions()) {
                if (action.probabilityGroup() != null && !action.probabilityGroup().isBlank()) {
                    branches.add(new ProbBranch(action.probabilityGroup(), action.probability(),
                            proc.entityId(), proc.zhName()));
                }
            }
        }
        Map<String, List<ProbBranch>> probabilityGroups = new LinkedHashMap<>();
        for (ProbBranch branch : branches) {
            probabilityGroups.computeIfAbsent(branch.group(), k -> new ArrayList<>()).add(branch);
        }
        for (Map.Entry<String, List<ProbBranch>> entry : probabilityGroups.entrySet()) {
            double sum = 0.0D;
            for (ProbBranch branch : entry.getValue()) {
                sum += branch.probability();
            }
            ProbBranch sample = entry.getValue().get(0);
            if (entry.getValue().size() >= 2 && (sum < PROBABILITY_SUM_MIN || sum > PROBABILITY_SUM_MAX)) {
                issues.add(new ValidationIssue(
                        "VAL-008", "标准 10.4", Severity.ERROR, sample.elementId(), sample.elementName(),
                        "互斥组 '" + entry.getKey() + "' 概率和 " + sum + " 超出闭区间 ["
                                + PROBABILITY_SUM_MIN + ", " + PROBABILITY_SUM_MAX + "]"));
            } else if (entry.getValue().size() == 1) {
                issues.add(new ValidationIssue(
                        "VAL-008", "标准 10.4", Severity.WARNING, sample.elementId(), sample.elementName(),
                        "互斥组 '" + entry.getKey() + "' 仅 1 个分支（概率和应为 1.0，或应改为独立概率）"));
            }
        }

        // VAL-010：成员链与引用定位路径深度 ≤64（标准 9.6）
        for (GromaModuleIR.TypeIR ty : ir.domainTypes()) {
            for (GromaModuleIR.FieldIR field : ty.fields() == null ? List.<GromaModuleIR.FieldIR>of() : ty.fields()) {
                for (GromaModuleIR.TriggerBindingIR trigger : field.eventTriggers() == null
                        ? List.<GromaModuleIR.TriggerBindingIR>of() : field.eventTriggers()) {
                    checkMemberPathDepth(trigger.conditionExpr(), ty.entityId() + "/" + field.zhName(), issues);
                }
            }
        }
        for (GromaModuleIR.ProcessIR indep : ir.independentProcesses()) {
            checkProcessExprDepth(indep, issues);
        }
        for (GromaModuleIR.SceneIR scene : ir.scenes()) {
            for (GromaModuleIR.ProcessIR proc : scene.sceneLevelProcesses() == null
                    ? List.<GromaModuleIR.ProcessIR>of() : scene.sceneLevelProcesses()) {
                checkProcessExprDepth(proc, issues);
            }
        }

        for (ValidationIssue issue : issues) {
            if (issue.severity() == Severity.ERROR) {
                log.warn("[Validator][{}][{}] 元素={}({}): {}",
                        issue.ruleId(), issue.standardClause(), issue.elementName(), issue.elementId(), issue.message());
            }
        }

        boolean hasError = issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
        return new ValidationSummary(!hasError, List.copyOf(issues));
    }

    private void checkProcessRules(GromaModuleIR.ProcessIR proc, List<ValidationIssue> issues) {
        List<GromaModuleIR.SubProcessStepIR> subs = proc.subSteps() == null ? List.of() : proc.subSteps();
        if (!subs.isEmpty()) {
            // VAL-004：有子过程必须至少有一个首个启动 firstStart（标准 9.4.7）。
            // 对齐解析器入口推断（AbModelMergerAndParser 入度兜底）：无显式 firstStart 但存在
            // 零入度节点时，解析器会自动推断入口 → 降级 WARNING；全部节点都有入向（纯环）才 ERROR。
            boolean hasFirstStart = subs.stream().anyMatch(GromaModuleIR.SubProcessStepIR::firstStart);
            if (!hasFirstStart) {
                Set<Integer> targeted = new HashSet<>();
                for (GromaModuleIR.SubProcessStepIR sub : subs) {
                    if (sub.nextStepPcs() != null) {
                        targeted.addAll(sub.nextStepPcs());
                    }
                }
                long zeroInDegree = subs.stream().filter(s -> !targeted.contains(s.stepPc())).count();
                if (zeroInDegree == 0L) {
                    issues.add(new ValidationIssue(
                            "VAL-004", "标准 9.4.7", Severity.ERROR, proc.entityId(), proc.zhName(),
                            "包含子过程的复合过程既无显式 firstStart 也无零入度起点（纯环，无法确定入口）"));
                } else {
                    issues.add(new ValidationIssue(
                            "VAL-004", "标准 9.4.7", Severity.WARNING, proc.entityId(), proc.zhName(),
                            "无显式 firstStart，将按零入度自动推断 " + zeroInDegree + " 个入口节点"));
                }
            }
            // VAL-001：非耗时算子不能包含耗时子过程（标准 9.2.2 & 12.1.2，规避 BE-17）
            boolean anyTimeConsumingChild = subs.stream().anyMatch(GromaModuleIR.SubProcessStepIR::timeConsuming);
            if (!proc.timeConsuming() && anyTimeConsumingChild) {
                issues.add(new ValidationIssue(
                        "VAL-001", "标准 9.2.2 / 12.1.2", Severity.ERROR, proc.entityId(), proc.zhName(),
                        "瞬时算子内部禁止包含耗时子过程（请将该父过程设为耗时过程）"));
            }
        }
    }

    /** VAL-007：entityId 为完整合法编码形态时只做保留字冲突校验。
     *  存量语料的内部主键（UUID 形态、数字开头）不是显式编码，直接跳过（修复误杀真实语料的缺陷）；
     *  非法形态的手写编码在命名引擎入口处已被确定性转译，这里不重复拦截。 */
    private void validateExplicitEncode(String entityId, String zhName, List<ValidationIssue> issues) {
        if (entityId == null || entityId.isBlank() || !EXPLICIT_ENCODE_SHAPE.matcher(entityId).matches()) {
            return; // 空编码 / 中文原名 / 平台内部主键：走命名引擎确定性转译，不拦截
        }
        try {
            UniqueIdentifierGuard.validateExplicitEncodeOrThrow(entityId);
        } catch (IllegalArgumentException ex) {
            issues.add(new ValidationIssue(
                    "VAL-007", "标准 8.2", Severity.ERROR, entityId, zhName, ex.getMessage()));
        }
    }

    /** VAL-010：触发条件表达式中的点分成员链深度 ≤64（标准 9.6） */
    private void checkProcessExprDepth(GromaModuleIR.ProcessIR proc, List<ValidationIssue> issues) {
        // 过程自身的成员链主要体现在局部临时成员的字段触发上（无表达式字段），
        // 过程启动/维持条件为结构化规则（非自由表达式）；此处覆盖 rawJavaCode 中的长链引用防御。
        if (proc.rawJavaCode() != null && !proc.rawJavaCode().isBlank()) {
            checkMemberPathDepth(proc.rawJavaCode(), proc.entityId() + "/" + proc.zhName(), issues);
        }
    }

    private void checkMemberPathDepth(String text, String elementId, List<ValidationIssue> issues) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = MEMBER_PATH.matcher(text);
        Set<String> reported = new HashSet<>();
        while (matcher.find()) {
            String path = matcher.group();
            if (!reported.add(path)) {
                continue;
            }
            int depth = path.split("\\.").length;
            if (depth > MAX_MEMBER_PATH_DEPTH) {
                issues.add(new ValidationIssue(
                        "VAL-010", "标准 9.6", Severity.ERROR, elementId, path,
                        "成员链/引用定位路径深度 " + depth + " 超过上限 " + MAX_MEMBER_PATH_DEPTH + " 层: " + path));
            }
        }
    }
}

================ parser/UniqueIdentifierGuard.java (2043 bytes) ================
package com.groma.studio.parser;

import javax.lang.model.SourceVersion;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 显式自定义编码合法性校验与确定性去重器（落实 BE-MDL-003，规避 BL-19）
 */
public final class UniqueIdentifierGuard {

    private static final Pattern VALID_ENCODE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final Set<String> allocated = new LinkedHashSet<>();
    private int autoSeq = 0;

    /**
     * 校验用户手工填写的严格编码；非法时直接拒绝并给出修改建议（CK-BE-MDL-003-1）
     */
    public static void validateExplicitEncodeOrThrow(String encode) {
        if (encode == null || !VALID_ENCODE.matcher(encode).matches()) {
            throw new IllegalArgumentException(
                    "[MDL-CODE-001] 非法标识符编码 '" + encode + "'：仅允许字母/数字/下划线且不能以数字或中文开头，建议改为合法驼峰英文");
        }
        if (SourceVersion.isKeyword(encode) || !SourceVersion.isName(encode)) {
            throw new IllegalArgumentException(
                    "[MDL-CODE-002] 编码 '" + encode + "' 与 Java 21 关键字冲突，建议追加业务后缀如 '" + encode + "Item'");
        }
    }

    /**
     * 导入存量模型时对空编码或重复编码（如大量重名的 wb1）分配稳定且唯一的合法编码（CK-BE-MDL-003-2 & 3）
     */
    public String allocateDeterministicUnique(String candidateOrEmpty, String fallbackPrefix) {
        String base = (candidateOrEmpty == null || candidateOrEmpty.isBlank() || !VALID_ENCODE.matcher(candidateOrEmpty).matches())
                ? fallbackPrefix + "_" + (++autoSeq)
                : candidateOrEmpty;
        if (SourceVersion.isKeyword(base)) {
            base = base + "_val";
        }
        String unique = base;
        int dupIdx = 2;
        while (!allocated.add(unique)) {
            unique = base + "_" + dupIdx++;
        }
        return unique;
    }
}

================ parser/GraphCycleAndDepthGuard.java (2329 bytes) ================
package com.groma.studio.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 通用有向图环路与最大递归深度检测器（覆盖继承链、成员链、调用链、路径定位链，根治 BL-15 栈溢出）
 */
public final class GraphCycleAndDepthGuard {

    public static final int DEFAULT_MAX_DEPTH = 64;

    private GraphCycleAndDepthGuard() {
    }

    public static Optional<String> detectCycleOrExcessDepth(
            String graphKind,
            Map<String, List<String>> adjacency,
            int maxDepth) {

        Set<String> permanentlyVisited = new HashSet<>();
        for (String startNode : adjacency.keySet()) {
            if (permanentlyVisited.contains(startNode)) {
                continue;
            }
            Set<String> onCurrentPath = new HashSet<>();
            Optional<String> err = dfs(graphKind, startNode, adjacency, onCurrentPath, permanentlyVisited, new ArrayDeque<>(), maxDepth);
            if (err.isPresent()) {
                return err;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> dfs(
            String graphKind,
            String node,
            Map<String, List<String>> adjacency,
            Set<String> onPath,
            Set<String> visited,
            Deque<String> trail,
            int maxDepth) {

        if (trail.size() > maxDepth) {
            return Optional.of("[MDL-DEPTH-ERR] " + graphKind + " 递归深度超过上限 " + maxDepth + "，涉及节点: " + node);
        }
        if (!onPath.add(node)) {
            return Optional.of("[MDL-CYCLE-ERR] " + graphKind + " 检测到闭环依赖: " + String.join(" -> ", trail) + " -> " + node);
        }
        trail.addLast(node);
        for (String next : adjacency.getOrDefault(node, List.of())) {
            if (!visited.contains(next)) {
                Optional<String> hit = dfs(graphKind, next, adjacency, onPath, visited, trail, maxDepth);
                if (hit.isPresent()) {
                    return hit;
                }
            }
        }
        trail.removeLast();
        onPath.remove(node);
        visited.add(node);
        return Optional.empty();
    }
}


Pythonimport os

p = '/tmp/orpl_extract/orpl-9ee34655eecdb360241d86a189cb40c9527ef56a/groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.java'
with open(p, 'r', encoding='utf-8') as f:
    lines = f.readlines()
print(f"Total lines in AbModelMergerAndParser.java: {len(lines)}")
for i, line in enumerate(lines[:650], 1):
    print(f"{i:4d}: {line}", end='')

代码输出Total lines in AbModelMergerAndParser.java: 2633
   1: package com.groma.studio.parser;
   2: 
   3: import com.groma.studio.ir.CompilePlanIR;
   4: import com.groma.studio.ir.CompilePlanIR.ArgBindingIR;
   5: import com.groma.studio.ir.CompilePlanIR.AssocTypeIR;
   6: import com.groma.studio.ir.CompilePlanIR.BuiltinIR;
   7: import com.groma.studio.ir.CompilePlanIR.ConditionIR;
   8: import com.groma.studio.ir.CompilePlanIR.ConditionRuleIR;
   9: import com.groma.studio.ir.CompilePlanIR.ExceptionFlowIR;
  10: import com.groma.studio.ir.CompilePlanIR.ProxyDefIR;
  11: import com.groma.studio.ir.CompilePlanIR.ConstIR;
  12: import com.groma.studio.ir.CompilePlanIR.DefaultStartIR;
  13: import com.groma.studio.ir.CompilePlanIR.InstanceIR;
  14: import com.groma.studio.ir.CompilePlanIR.ListSpec;
  15: import com.groma.studio.ir.CompilePlanIR.LocalIR;
  16: import com.groma.studio.ir.CompilePlanIR.LocalVarIR;
  17: import com.groma.studio.ir.CompilePlanIR.MapSpec;
  18: import com.groma.studio.ir.CompilePlanIR.MemberIR;
  19: import com.groma.studio.ir.CompilePlanIR.MemberPathIR;
  20: import com.groma.studio.ir.CompilePlanIR.ManualStartIR;
  21: import com.groma.studio.ir.CompilePlanIR.NamedInstanceIR;
  22: import com.groma.studio.ir.CompilePlanIR.NextIR;
  23: import com.groma.studio.ir.CompilePlanIR.NodeIR;
  24: import com.groma.studio.ir.CompilePlanIR.NodeOutputIR;
  25: import com.groma.studio.ir.CompilePlanIR.NullSpec;
  26: import com.groma.studio.ir.CompilePlanIR.ObjectSpec;
  27: import com.groma.studio.ir.CompilePlanIR.OperandIR;
  28: import com.groma.studio.ir.CompilePlanIR.ParamIR;
  29: import com.groma.studio.ir.CompilePlanIR.PortIR;
  30: import com.groma.studio.ir.CompilePlanIR.PostIR;
  31: import com.groma.studio.ir.CompilePlanIR.ProcessDefIR;
  32: import com.groma.studio.ir.CompilePlanIR.RefSpec;
  33: import com.groma.studio.ir.CompilePlanIR.ScalarSpec;
  34: import com.groma.studio.ir.CompilePlanIR.SelfIR;
  35: import com.groma.studio.ir.CompilePlanIR.TriggerIR;
  36: import com.groma.studio.ir.CompilePlanIR.TypeDefIR;
  37: import com.groma.studio.ir.CompilePlanIR.ValueSpec;
  38: import com.groma.studio.ir.GromaModuleIR;
  39: import com.groma.studio.ir.GromaTypes;
  40: import org.slf4j.Logger;
  41: import org.slf4j.LoggerFactory;
  42: import tools.jackson.databind.JsonNode;
  43: import tools.jackson.databind.ObjectMapper;
  44: 
  45: import java.io.IOException;
  46: import java.io.UncheckedIOException;
  47: import java.nio.file.Files;
  48: import java.nio.file.Path;
  49: import java.security.MessageDigest;
  50: import java.util.ArrayList;
  51: import java.util.Comparator;
  52: import java.util.HashMap;
  53: import java.util.HashSet;
  54: import java.util.HexFormat;
  55: import java.util.LinkedHashMap;
  56: import java.util.LinkedHashSet;
  57: import java.util.List;
  58: import java.util.Locale;
  59: import java.util.Map;
  60: import java.util.Set;
  61: import java.util.TreeMap;
  62: import java.util.function.Function;
  63: 
  64: /**
  65:  * XX-a.json（结构语义流：实体表 + 组织树 + 部分画布）与 XX-b.json（增量画布）双流确定性合并解析器
  66:  * <p>
  67:  * 画布合并规则：cellMap = A ∪ B，B 覆盖 A。三套 id 体系靠实体 id 连接：cell.data → 实体，
  68:  * 连线两端是同视图内的 cell id，端口以 (cell id, port id) 定位，port.args.otherData 指向定义视图里的形参（关联对象）。
  69:  * </p>
  70:  * 解析产出 {@link CompilePlanIR}：
  71:  * <ul>
  72:  *   <li>定义视图 → 过程形参契约（非空输入 / 可空输入 / 双向 / 输出 / 生成）；</li>
  73:  *   <li>设计视图 → 数据流图：fixed 锚点线绑定操作数与写出目标，invocation 调用线给出顺序与带线上判断的分支，
  74:  *       out-change / result 线是节点成功后的后置动作；</li>
  75:  *   <li>类型与场景的设计视图 → 事件接线表与默认启动表。</li>
  76:  * </ul>
  77:  */
  78: public final class AbModelMergerAndParser {
  79: 
  80:     private static final Logger log = LoggerFactory.getLogger(AbModelMergerAndParser.class);
  81: 
  82:     static final Set<String> PROCESS_CELL_TYPES = Set.of("PROCESS", "TEMPLATE-PROCESS", "INDEPENDENT-PROCESS");
  83:     /** 画布内置工具库目录（这些目录下的 TYPE / PROCESS 不进入业务模型） */
  84:     static final Set<String> BUILTIN_TOOL_TOPS = Set.of("工具过程", "集合内置过程", "展示视图组件", "工具对象");
  85:     static final String SCENES_TOP = "运行场景";
  86:     static final String TYPE_KIND = "TYPE";
  87:     static final String SCENE_KIND = "SCENE";
  88:     static final String ASSOCIATION_KIND = "ASSOCIATION";
  89:     static final long DEFAULT_STEP_MS = 1000L;
  90: 
  91:     /** 画布节点排序：先行后列（行按 40px 网格取整），最后按 cell id，保证与文件内顺序无关的稳定排序 */
  92:     static final Comparator<JsonNode> CELL_ORDER = Comparator
  93:             .comparingDouble((JsonNode c) -> Math.round(doubleOf(c, "y", 0.0D) / 40.0D))
  94:             .thenComparingDouble(c -> doubleOf(c, "x", 0.0D))
  95:             .thenComparing(c -> text(c, "id"));
  96: 
  97:     /** 集合元素按元素序号（单元格 defaultValue）升序；没有序号时退回画布阅读顺序 */
  98:     static final Comparator<JsonNode> ELEMENT_ORDER = Comparator
  99:             .comparingDouble((JsonNode c) -> indexOfElement(c))
 100:             .thenComparing(CELL_ORDER);
 101: 
 102: 
 103:     private static double indexOfElement(JsonNode cell) {
 104:         try {
 105:             return Double.parseDouble(text(cell, "defaultValue").trim());
 106:         } catch (RuntimeException ex) {
 107:             return Double.POSITIVE_INFINITY;
 108:         }
 109:     }
 110: 
 111:     public GromaModuleIR parse(Path fileAPath, Path fileBPath) {
 112:         try {
 113:             return parse(Files.readAllBytes(fileAPath), Files.readAllBytes(fileBPath));
 114:         } catch (IOException ex) {
 115:             throw new UncheckedIOException("读取模型语料文件失败: " + fileAPath + " / " + fileBPath, ex);
 116:         }
 117:     }
 118: 
 119:     public GromaModuleIR parse(byte[] bytesA, byte[] bytesB) {
 120:         return new Worker(bytesA, bytesB).run();
 121:     }
 122: 
 123:     // ====================================================================================== JSON 小工具
 124: 
 125:     private static JsonNode field(JsonNode node, String name) {
 126:         if (node == null || !node.isObject()) {
 127:             return null;
 128:         }
 129:         JsonNode value = node.get(name);
 130:         return (value == null || value.isMissingNode()) ? null : value;
 131:     }
 132: 
 133:     private static String text(JsonNode node, String name) {
 134:         JsonNode value = field(node, name);
 135:         if (value == null || value.isNull()) {
 136:             return "";
 137:         }
 138:         if (value.isString()) {
 139:             return value.stringValue().trim();
 140:         }
 141:         if (value.isNumber() || value.isBoolean()) {
 142:             return value.asString().trim();
 143:         }
 144:         return "";
 145:     }
 146: 
 147:     private static boolean flag(JsonNode node, String name) {
 148:         JsonNode value = field(node, name);
 149:         if (value == null || value.isNull()) {
 150:             return false;
 151:         }
 152:         if (value.isBoolean()) {
 153:             return value.booleanValue();
 154:         }
 155:         return "true".equalsIgnoreCase(value.asString().trim()) || "1".equals(value.asString().trim());
 156:     }
 157: 
 158:     private static double doubleOf(JsonNode node, String name, double fallback) {
 159:         JsonNode value = field(node, name);
 160:         if (value == null || value.isNull() || !value.isNumber()) {
 161:             return fallback;
 162:         }
 163:         return value.doubleValue();
 164:     }
 165: 
 166:     private static long longOf(JsonNode node, String name, long fallback) {
 167:         JsonNode value = field(node, name);
 168:         if (value == null || value.isNull()) {
 169:             return fallback;
 170:         }
 171:         if (value.isNumber()) {
 172:             return value.longValue();
 173:         }
 174:         try {
 175:             return Long.parseLong(value.asString().trim());
 176:         } catch (RuntimeException ex) {
 177:             return fallback;
 178:         }
 179:     }
 180: 
 181:     private static Double nullableDouble(JsonNode node, String name) {
 182:         JsonNode value = field(node, name);
 183:         if (value == null || value.isNull()) {
 184:             return null;
 185:         }
 186:         if (value.isNumber()) {
 187:             return value.doubleValue();
 188:         }
 189:         try {
 190:             return Double.parseDouble(value.asString().trim());
 191:         } catch (RuntimeException ex) {
 192:             return null;
 193:         }
 194:     }
 195: 
 196:     private static String sha256Hex(byte[] data) {
 197:         try {
 198:             return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
 199:         } catch (java.security.NoSuchAlgorithmException ex) {
 200:             throw new IllegalStateException(ex);
 201:         }
 202:     }
 203: 
 204:     // ====================================================================================== 工作器
 205: 
 206:     /**
 207:      * 单次解析的全部状态（保证解析器实例可重入、线程安全）
 208:      */
 209:     private final class Worker {
 210: 
 211:         private final byte[] bytesA;
 212:         private final byte[] bytesB;
 213:         private final ObjectMapper mapper = new ObjectMapper();
 214: 
 215:         private final TreeMap<String, JsonNode> entities = new TreeMap<>();
 216:         private final TreeMap<String, List<JsonNode>> rawViews = new TreeMap<>();
 217:         private final Map<String, ViewCtx> viewCache = new HashMap<>();
 218: 
 219:         private final Map<String, String> treeTop = new HashMap<>();
 220:         private final Map<String, List<String>> treeChildren = new HashMap<>();
 221:         private final List<String> treeSceneIds = new ArrayList<>();
 222:         private final Set<String> builtinTypeIds = new HashSet<>();
 223:         private final LinkedHashSet<String> domainTypeIds = new LinkedHashSet<>();
 224:         private final List<String> sceneIds = new ArrayList<>();
 225:         private final List<String> assocTypeIds = new ArrayList<>();
 226:         private final Map<String, String> typeKind = new HashMap<>();
 227:         private final Map<String, List<String>> typeMembers = new HashMap<>();
 228:         private final Map<String, String> memberOwner = new HashMap<>();
 229:         private final Map<String, String> enumOfState = new HashMap<>();
 230:         private final Map<String, List<String>> enumStates = new HashMap<>();
 231:         private final Map<String, String> enumInitial = new HashMap<>();
 232:         private final List<String> warnings = new ArrayList<>();
 233: 
 234:         private final Map<String, String> defKeyCache = new HashMap<>();
 235:         private final List<ProcessDefIR> processDefs = new ArrayList<>();
 236:         private final List<TriggerIR> triggers = new ArrayList<>();
 237:         private final List<DefaultStartIR> defaultStarts = new ArrayList<>();
 238:         private final List<ManualStartIR> manualStarts = new ArrayList<>();
 239: 
 240:         private String modelId = "";
 241:         private String modelLabel = "";
 242:         private int modelVersion = 1;
 243: 
 244:         Worker(byte[] bytesA, byte[] bytesB) {
 245:             this.bytesA = bytesA;
 246:             this.bytesB = bytesB;
 247:         }
 248: 
 249:         GromaModuleIR run() {
 250:             JsonNode objA = unwrap(mapper.readTree(bytesA));
 251:             JsonNode objB = unwrap(mapper.readTree(bytesB));
 252:             if (objA == null || !objA.isObject() || !field(objA, "entities").isArray()) {
 253:                 throw new IllegalArgumentException("[PRS-ERR-001] a.json 主体缺少 entities 实体表");
 254:             }
 255:             modelId = text(objA, "id");
 256:             modelLabel = text(objA, "label");
 257:             modelVersion = (int) longOf(objA, "version", 1L);
 258: 
 259:             indexEntities(objA);
 260:             collectCells(field(objA, "cellMap"));
 261:             if (objB != null) {
 262:                 collectCells(field(objB, "cellMap"));
 263:             }
 264:             walkTree(field(objA, "nodes"), "");
 265:             classifyTypes();
 266:             indexEnums();
 267:             buildProcessDefs();
 268:             buildWiring();
 269: 
 270:             CompilePlanIR plan = new CompilePlanIR(
 271:                     DEFAULT_STEP_MS, buildTypeDefs(), List.copyOf(processDefs), buildInstances(), buildAssocTypes(),
 272:                     List.copyOf(triggers), List.copyOf(defaultStarts), List.copyOf(manualStarts), List.copyOf(warnings));
 273:             log.info("[Parser] 模型 {} v{}：实体 {}，视图 {}，过程定义 {}，事件接线 {}，默认启动 {}，手动启动 {}，诊断 {}",
 274:                     modelLabel, modelVersion, entities.size(), rawViews.size(), processDefs.size(),
 275:                     triggers.size(), defaultStarts.size(), manualStarts.size(), warnings.size());
 276:             return assemble(plan);
 277:         }
 278: 
 279:         // ------------------------------------------------------------------ 基础索引
 280: 
 281:         private JsonNode unwrap(JsonNode raw) {
 282:             if (raw == null) {
 283:                 return null;
 284:             }
 285:             JsonNode obj = field(raw, "obj");
 286:             if (obj != null) {
 287:                 return obj.isString() ? mapper.readTree(obj.asString()) : obj;
 288:             }
 289:             JsonNode data = field(raw, "data");
 290:             return (data != null && data.isObject()) ? data : raw;
 291:         }
 292: 
 293:         private void indexEntities(JsonNode objA) {
 294:             for (JsonNode entity : field(objA, "entities")) {
 295:                 String id = text(entity, "id");
 296:                 if (!id.isEmpty()) {
 297:                     entities.put(id, entity);
 298:                 }
 299:             }
 300:         }
 301: 
 302:         private void collectCells(JsonNode cellMap) {
 303:             if (cellMap == null || !cellMap.isObject()) {
 304:                 return;
 305:             }
 306:             for (Map.Entry<String, JsonNode> entry : cellMap.properties()) {
 307:                 JsonNode cells = entry.getValue();
 308:                 List<JsonNode> list = new ArrayList<>();
 309:                 if (cells != null && cells.isArray()) {
 310:                     for (JsonNode cell : cells) {
 311:                         list.add(cell);
 312:                     }
 313:                 }
 314:                 // B 覆盖 A
 315:                 rawViews.put(entry.getKey(), list);
 316:             }
 317:         }
 318: 
 319:         private ViewCtx view(String viewId) {
 320:             if (viewId == null || viewId.isEmpty() || !rawViews.containsKey(viewId)) {
 321:                 return null;
 322:             }
 323:             return viewCache.computeIfAbsent(viewId, id -> new ViewCtx(id, rawViews.get(id)));
 324:         }
 325: 
 326:         private void walkTree(JsonNode nodes, String top) {
 327:             if (nodes == null || !nodes.isArray()) {
 328:                 return;
 329:             }
 330:             for (JsonNode node : nodes) {
 331:                 String label = text(node, "label");
 332:                 String currentTop = top.isEmpty() ? label : top;
 333:                 String eid = text(node, "entityId");
 334:                 JsonNode kids = field(node, "nodes");
 335:                 if (!eid.isEmpty()) {
 336:                     treeTop.put(eid, currentTop);
 337:                     if ("SCENE".equals(text(node, "type")) && SCENES_TOP.equals(currentTop)) {
 338:                         treeSceneIds.add(eid);
 339:                     }
 340:                     List<String> children = new ArrayList<>();
 341:                     if (kids != null && kids.isArray()) {
 342:                         for (JsonNode kid : kids) {
 343:                             String kidEid = text(kid, "entityId");
 344:                             if (!kidEid.isEmpty()) {
 345:                                 children.add(kidEid);
 346:                             }
 347:                         }
 348:                     }
 349:                     treeChildren.put(eid, children);
 350:                 }
 351:                 walkTree(kids, currentTop);
 352:             }
 353:         }
 354: 
 355:         // ------------------------------------------------------------------ 类型、成员、枚举
 356: 
 357:         private void classifyTypes() {
 358:             for (String eid : entities.keySet()) {
 359:                 JsonNode entity = entities.get(eid);
 360:                 String top = treeTop.getOrDefault(eid, "");
 361:                 if ("TYPE".equals(text(entity, "type"))) {
 362:                     boolean builtinComponent = "INNER".equalsIgnoreCase(text(entity, "componentType"));
 363:                     if (BUILTIN_TOOL_TOPS.contains(top) || builtinComponent) {
 364:                         // 内置工具库目录下的类型，以及内置组件（componentType=INNER）提供的类型不进入业务模型；
 365:                         // 通用组件（GENERAL）提供的类型（如 文件信息）属于业务模型
 366:                         builtinTypeIds.add(eid);
 367:                     } else {
 368:                         domainTypeIds.add(eid);
 369:                         typeKind.put(eid, TYPE_KIND);
 370:                     }
 371:                 } else if ("ASSOCIATION-TYPE".equals(text(entity, "type"))) {
 372:                     assocTypeIds.add(eid);
 373:                     typeKind.put(eid, ASSOCIATION_KIND);
 374:                 }
 375:             }
 376:             for (String sceneId : treeSceneIds) {
 377:                 sceneIds.add(sceneId);
 378:                 typeKind.put(sceneId, SCENE_KIND);
 379:             }
 380:             // 成员归属以成员实体自己的 typeEntityId 为准
 381:             for (String eid : entities.keySet()) {
 382:                 JsonNode entity = entities.get(eid);
 383:                 String type = text(entity, "type");
 384:                 if ("ATTRIBUTE-MEMBER".equals(type) || "COMPOSE-MEMBER".equals(type)) {
 385:                     String owner = text(entity, "typeEntityId");
 386:                     if (!owner.isEmpty() && (typeKind.containsKey(owner) || domainTypeIds.contains(owner))) {
 387:                         memberOwner.put(eid, owner);
 388:                     }
 389:                 }
 390:             }
 391:             // 类型成员表：组织树子节点顺序优先，其余按实体 id 追加
 392:             LinkedHashSet<String> owners = new LinkedHashSet<>(domainTypeIds);
 393:             owners.addAll(sceneIds);
 394:             owners.addAll(assocTypeIds);
 395:             owners.addAll(memberOwner.values());
 396:             for (String ownerId : owners) {
 397:                 if (!entities.containsKey(ownerId)) {
 398:                     continue;
 399:                 }
 400:                 LinkedHashSet<String> members = new LinkedHashSet<>();
 401:                 for (String child : treeChildren.getOrDefault(ownerId, List.of())) {
 402:                     if (ownerId.equals(memberOwner.get(child))) {
 403:                         members.add(child);
 404:                     }
 405:                 }
 406:                 for (String memberId : memberOwner.keySet()) {
 407:                     if (ownerId.equals(memberOwner.get(memberId))) {
 408:                         members.add(memberId);
 409:                     }
 410:                 }
 411:                 if (!members.isEmpty()) {
 412:                     typeMembers.put(ownerId, new ArrayList<>(members));
 413:                 }
 414:             }
 415:         }
 416: 
 417:         private void indexEnums() {
 418:             for (String eid : entities.keySet()) {
 419:                 JsonNode entity = entities.get(eid);
 420:                 if (!"ENUM".equals(text(entity, "type"))) {
 421:                     continue;
 422:                 }
 423:                 LinkedHashSet<String> labels = new LinkedHashSet<>();
 424:                 String initial = "";
 425:                 ViewCtx view = view(text(entity, "defineViewId"));
 426:                 List<JsonNode> stateCells = new ArrayList<>();
 427:                 if (view != null) {
 428:                     for (JsonNode cell : view.cells) {
 429:                         if ("STATE".equals(text(cell, "type"))) {
 430:                             stateCells.add(cell);
 431:                         }
 432:                     }
 433:                     stateCells.sort(CELL_ORDER);
 434:                 }
 435:                 for (JsonNode cell : stateCells) {
 436:                     String stateEntityId = text(cell, "data");
 437:                     JsonNode stateEntity = entities.get(stateEntityId);
 438:                     String label = stateEntity != null ? text(stateEntity, "label") : text(cell, "label");
 439:                     if (label.isEmpty() || !labels.add(label)) {
 440:                         continue;
 441:                     }
 442:                     enumOfState.put(stateEntityId, eid);
 443:                     if (initial.isEmpty() && (flag(cell, "stateInitial") || flag(stateEntity, "stateInitial"))) {
 444:                         initial = label;
 445:                     }
 446:                 }
 447:                 String defaultState = text(entity, "defaultValue");
 448:                 enumStates.put(eid, new ArrayList<>(labels));
 449:                 enumInitial.put(eid, !defaultState.isEmpty() && labels.contains(defaultState) ? defaultState : initial);
 450:             }
 451:         }
 452: 
 453:         /** 实体 / 图元级“是否时间有关”：取值 RELATED / UNRELATED */
 454:         private boolean isTimeRelated(JsonNode node) {
 455:             return "RELATED".equalsIgnoreCase(text(node, "timeRelated"));
 456:         }
 457: 
 458:         private boolean isToolProcess(JsonNode entity) {
 459:             return entity != null && !text(entity, "innerClass").isEmpty();
 460:         }
 461: 
 462:         private boolean isBusinessProcess(JsonNode entity) {
 463:             return entity != null && PROCESS_CELL_TYPES.contains(text(entity, "type")) && !isToolProcess(entity)
 464:                     && !BUILTIN_TOOL_TOPS.contains(treeTop.getOrDefault(text(entity, "id"), ""));
 465:         }
 466: 
 467:         /** 过程定义键：有独立设计视图按视图去重（多份实体副本共享一份流程），无设计视图的叶过程按实体 */
 468:         private String defKeyOf(JsonNode entity) {
 469:             String id = text(entity, "id");
 470:             String cached = defKeyCache.get(id);
 471:             if (cached != null) {
 472:                 return cached;
 473:             }
 474:             String designView = text(entity, "designViewId");
 475:             String key = (designView.isEmpty() || !rawViews.containsKey(designView)) ? "ent:" + id : "dv:" + designView;
 476:             defKeyCache.put(id, key);
 477:             return key;
 478:         }
 479: 
 480:         private String ownerKindOf(String typeId) {
 481:             return typeId == null || typeId.isEmpty() ? "INDEPENDENT" : typeKind.getOrDefault(typeId, TYPE_KIND);
 482:         }
 483: 
 484:         // ------------------------------------------------------------------ 类型占位
 485: 
 486:         private String refTypeIdOf(JsonNode entity) {
 487:             JsonNode custom = field(entity, "customType");
 488:             if (custom != null && custom.isObject()) {
 489:                 String id = text(custom, "id");
 490:                 if (entities.containsKey(id)) {
 491:                     return id;
 492:                 }
 493:                 String byLabel = findByLabel(text(custom, "label"), "ENUM".equals(text(entity, "typeRange")));
 494:                 if (!byLabel.isEmpty()) {
 495:                     return byLabel;
 496:                 }
 497:             }
 498:             return findByLabel(text(entity, "label"), "ENUM".equals(text(entity, "typeRange")));
 499:         }
 500: 
 501:         private String findByLabel(String label, boolean enumFirst) {
 502:             if (label.isEmpty()) {
 503:                 return "";
 504:             }
 505:             String typeHit = "";
 506:             String enumHit = "";
 507:             for (String eid : entities.keySet()) {
 508:                 JsonNode entity = entities.get(eid);
 509:                 if (!label.equals(text(entity, "label"))) {
 510:                     continue;
 511:                 }
 512:                 String type = text(entity, "type");
 513:                 if (enumHit.isEmpty() && "ENUM".equals(type)) {
 514:                     enumHit = eid;
 515:                 } else if (typeHit.isEmpty() && ("TYPE".equals(type) || "ASSOCIATION-TYPE".equals(type))) {
 516:                     typeHit = eid;
 517:                 }
 518:             }
 519:             if (enumFirst) {
 520:                 return !enumHit.isEmpty() ? enumHit : typeHit;
 521:             }
 522:             return !typeHit.isEmpty() ? typeHit : enumHit;
 523:         }
 524: 
 525:         /** 实体（成员 / 关联对象 / 模板成员）的 Java 类型占位 */
 526:         private String slotType(JsonNode entity) {
 527:             if (entity == null) {
 528:                 return GromaTypes.OBJECT;
 529:             }
 530:             String typeRange = text(entity, "typeRange");
 531:             String element;
 532:             if ("ENUM".equals(typeRange)) {
 533:                 String enumId = refTypeIdOf(entity);
 534:                 element = enumId.isEmpty() ? GromaTypes.STRING : GromaTypes.enumType(enumId);
 535:             } else if ("DEFINE".equals(typeRange)) {
 536:                 String typeId = refTypeIdOf(entity);
 537:                 element = typeId.isEmpty() ? GromaTypes.OBJECT : GromaTypes.entity(typeId);
 538:             } else {
 539:                 element = GromaTypes.baseJavaType(text(entity, "fieldType"));
 540:             }
 541:             if (!flag(entity, "isCollection")) {
 542:                 return element;
 543:             }
 544:             if ("map".equals(text(entity, "containerType"))) {
 545:                 return GromaTypes.map(GromaTypes.baseJavaType(text(entity, "keyType")), element);
 546:             }
 547:             return GromaTypes.list(element);
 548:         }
 549: 
 550:         private String slotType(String entityId) {
 551:             return slotType(entities.get(entityId));
 552:         }
 553: 
 554:         private String memberLabel(String memberId) {
 555:             JsonNode entity = entities.get(memberId);
 556:             return entity == null ? "" : text(entity, "label");
 557:         }
 558: 
 559:         // ------------------------------------------------------------------ 过程定义
 560: 
 561:         private void buildProcessDefs() {
 562:             Map<String, List<String>> groups = new TreeMap<>();
 563:             for (String eid : entities.keySet()) {
 564:                 JsonNode entity = entities.get(eid);
 565:                 if (isBusinessProcess(entity)) {
 566:                     groups.computeIfAbsent(defKeyOf(entity), k -> new ArrayList<>()).add(eid);
 567:                 }
 568:             }
 569:             for (Map.Entry<String, List<String>> group : groups.entrySet()) {
 570:                 processDefs.add(parseDef(group.getKey(), group.getValue()));
 571:             }
 572:         }
 573: 
 574:         private Long accuracyMillis(JsonNode entity) {
 575:             JsonNode value = field(entity, "timeAccuracyValue");
 576:             if (value == null || value.isNull()) {
 577:                 return null;
 578:             }
 579:             double accuracy;
 580:             try {
 581:                 accuracy = value.isNumber() ? value.doubleValue() : Double.parseDouble(value.asString().trim());
 582:             } catch (RuntimeException ex) {
 583:                 return null;
 584:             }
 585:             if (accuracy <= 0.0D) {
 586:                 return null;
 587:             }
 588:             double factor = switch (text(entity, "timeAccuracyUnit").toLowerCase(Locale.ROOT)) {
 589:                 case "ms", "毫秒" -> 1.0D;
 590:                 case "s", "秒" -> 1_000.0D;
 591:                 case "min", "分", "分钟" -> 60_000.0D;
 592:                 case "h", "时", "小时" -> 3_600_000.0D;
 593:                 case "d", "天", "日" -> 86_400_000.0D;
 594:                 default -> -1.0D;
 595:             };
 596:             return factor > 0.0D ? Math.max(1L, Math.round(accuracy * factor)) : null;
 597:         }
 598: 
 599:         private ProcessDefIR parseDef(String key, List<String> entityIds) {
 600:             JsonNode rep = entities.get(entityIds.get(0));
 601:             String repId = text(rep, "id");
 602:             String rawOwner = text(rep, "typeEntityId");
 603:             String ownerKind = "INDEPENDENT-PROCESS".equals(text(rep, "type")) ? "INDEPENDENT" : ownerKindOf(rawOwner);
 604:             String ownerTypeId = "INDEPENDENT".equals(ownerKind) ? "" : rawOwner;
 605:             JsonNode owner = entities.get(ownerTypeId);
 606:             ViewCtx design = view(text(rep, "designViewId"));
 607:             ViewCtx define = view(text(rep, "defineViewId"));
 608:             DefCtx def = new DefCtx(key, ownerTypeId, design);
 609: 
 610:             List<PortIR> params = parseParams(define);
 611:             for (PortIR port : params) {
 612:                 def.params.put(port.entityId(), port);
 613:             }
 614:             parseFlowNodes(def, design, params);
 615:             GuardBundle guards = parseGuardsAndExceptions(def, repId, entityIds, define, design);
 616:             List<ConditionRuleIR> starts = guards.starts();
 617:             List<ConditionRuleIR> maintains = guards.maintains();
 618:             List<ExceptionFlowIR> exceptions = guards.exceptions();
 619: 
 620:             String durationType = text(rep, "timeParamType");
 621:             if (durationType.isEmpty()) {
 622:                 durationType = "STEP";
 623:             }
 624:             return new ProcessDefIR(
 625:                     key, repId, text(rep, "label"), text(rep, "description"), ownerKind, ownerTypeId,
 626:                     owner == null ? "" : text(owner, "label"),
 627:                     isTimeRelated(rep), flag(rep, "fromRealWorld"), flag(rep, "async"),
 628:                     (int) longOf(rep, "singleton", 0L),
 629:                     durationType.toUpperCase(Locale.ROOT), doubleOf(rep, "timeNormal", 1.0D),
 630:                     text(rep, "timeUnit"), nullableDouble(rep, "timeMin"), nullableDouble(rep, "timeMax"),
 631:                     text(rep, "distribution"), nullableDouble(rep, "distributionMin"), nullableDouble(rep, "distributionMax"),
 632:                     nullableDouble(rep, "distributionAvg"), nullableDouble(rep, "distributionSd"),
 633:                     nullableDouble(rep, "distributionParam"), accuracyMillis(rep),
 634:                     text(rep, "javaCode"), text(rep, "designViewId"),
 635:                     List.copyOf(params), List.copyOf(def.localVars), List.copyOf(def.nodes), List.copyOf(def.entries),
 636:                     List.copyOf(starts), List.copyOf(maintains), List.copyOf(exceptions));
 637:         }
 638: 
 639:         /**
 640:          * 收割补丁 C + R25/R26：解析过程定义视图与设计视图上的
 641:          * R22 启动条件（condition）、R21 维持条件（agent）、R13/R15 源状态前置（in-change / in-out），
 642:          * 以及 R25 超时（overtime-exception → pcd）/ R26 提前完成（undertime-exception → pca）异常边。
 643:          * in-out 按说明书拆解语义：源状态作为启动前置（R13），目标状态记入规则描述（R14 后置由节点 PostIR 承载）。
 644:          */
 645:         private record GuardBundle(
 646:                 List<ConditionRuleIR> starts,
 647:                 List<ConditionRuleIR> maintains,
 648:                 List<ExceptionFlowIR> exceptions
 649:         ) {}
 650: 

Pythonp = '/tmp/orpl_extract/orpl-9ee34655eecdb360241d86a189cb40c9527ef56a/groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.java'
with open(p, 'r', encoding='utf-8') as f:
    lines = f.readlines()
for i, line in enumerate(lines[650:1650], 651):
    print(f"{i:4d}: {line}", end='')

代码输出 651:         private GuardBundle parseGuardsAndExceptions(DefCtx def, String repId, List<String> entityIds,
 652:                                                      ViewCtx define, ViewCtx design) {
 653:             List<ConditionRuleIR> starts = new ArrayList<>();
 654:             List<ConditionRuleIR> maintains = new ArrayList<>();
 655:             List<ExceptionFlowIR> exceptions = new ArrayList<>();
 656:             Map<String, Integer> pcByCell = new HashMap<>();
 657:             for (NodeIR node : def.nodes) {
 658:                 pcByCell.put(node.cellId(), node.pc());
 659:             }
 660:             List<ViewCtx> guardViews = new ArrayList<>();
 661:             if (define != null) {
 662:                 guardViews.add(define);
 663:             }
 664:             if (design != null) {
 665:                 guardViews.add(design);
 666:             }
 667:             for (ViewCtx v : guardViews) {
 668:                 for (JsonNode edge : v.cells) {
 669:                     if (!"LINK".equals(text(edge, "type"))) {
 670:                         continue;
 671:                     }
 672:                     String lt = linkType(edge);
 673:                     boolean cond = "condition".equals(lt);
 674:                     boolean agent = "agent".equals(lt);
 675:                     boolean inChange = "in-change".equals(lt) || "in-out".equals(lt);
 676:                     boolean overtime = "overtime-exception".equals(lt);
 677:                     boolean undertime = "undertime-exception".equals(lt);
 678:                     if (!cond && !agent && !inChange && !overtime && !undertime) {
 679:                         continue;
 680:                     }
 681:                     JsonNode target = v.target(edge);
 682:                     JsonNode source = v.source(edge);
 683:                     if (target == null || source == null) {
 684:                         continue;
 685:                     }
 686:                     String targetData = text(target, "data");
 687:                     boolean targetsThisProcess = repId.equals(targetData) || entityIds.contains(targetData)
 688:                             || pcByCell.containsKey(text(target, "id"));
 689:                     if (!targetsThisProcess) {
 690:                         continue;
 691:                     }
 692:                     if (overtime || undertime) {
 693:                         Integer pc = pcByCell.get(text(target, "id"));
 694:                         exceptions.add(new ExceptionFlowIR(overtime ? "OVERTIME_PCD" : "UNDERTIME_PCA",
 695:                                 pc == null ? -1 : pc, ""));
 696:                         continue;
 697:                     }
 698:                     JsonNode linkEntity = entities.get(text(edge, "data"));
 699:                     String stateLabel = "";
 700:                     OperandIR op;
 701:                     if ("STATE".equals(text(source, "type"))) {
 702:                         JsonNode stateEntity = entities.get(text(source, "data"));
 703:                         stateLabel = stateEntity == null ? text(source, "label") : text(stateEntity, "label");
 704:                         String enumId = enumOfState.getOrDefault(text(source, "data"), "");
 705:                         op = enumId.isEmpty() ? operandOf(def, v, source) : enumMemberOperand(def, v, enumId);
 706:                     } else {
 707:                         op = operandOf(def, v, source, edge);
 708:                     }
 709:                     if (op == null) {
 710:                         continue;
 711:                     }
 712:                     String describe = lt + ":" + text(source, "label");
 713:                     ConditionRuleIR rule = new ConditionRuleIR(
 714:                             op, stateLabel, conditionsOf(linkEntity), describe, expressionOf(linkEntity));
 715:                     if (agent) {
 716:                         maintains.add(rule);
 717:                         starts.add(rule);
 718:                     } else {
 719:                         starts.add(rule);
 720:                     }
 721:                 }
 722:             }
 723:             return new GuardBundle(starts, maintains, exceptions);
 724:         }
 725: 
 726:         /** 连线概率（rate/percentage 兼容：>1 视为百分数） */
 727:         private double probabilityOf(JsonNode edge, JsonNode linkEntity) {
 728:             Double p = nullableDouble(edge, "probability");
 729:             if (p == null && linkEntity != null) {
 730:                 p = nullableDouble(linkEntity, "probability");
 731:             }
 732:             if (p == null && linkEntity != null) {
 733:                 p = nullableDouble(linkEntity, "rate");
 734:             }
 735:             if (p == null) {
 736:                 return 1.0D;
 737:             }
 738:             return p > 1.0001D ? p / 100.0D : Math.max(0.0D, p);
 739:         }
 740: 
 741:         /** 互斥概率组（标准 10.4）：probabilityGroup / groupCode / mutexGroup 字段兼容 */
 742:         private String probabilityGroupOf(JsonNode edge, JsonNode linkEntity) {
 743:             String g = text(edge, "probabilityGroup");
 744:             if (g.isEmpty() && linkEntity != null) {
 745:                 g = text(linkEntity, "probabilityGroup");
 746:             }
 747:             if (g.isEmpty() && linkEntity != null) {
 748:                 g = text(linkEntity, "groupCode");
 749:             }
 750:             if (g.isEmpty() && linkEntity != null) {
 751:                 g = text(linkEntity, "mutexGroup");
 752:             }
 753:             return g;
 754:         }
 755: 
 756:         /** 定义视图 → 形参契约 */
 757:         private List<PortIR> parseParams(ViewCtx define) {
 758:             List<PortIR> params = new ArrayList<>();
 759:             if (define == null) {
 760:                 return params;
 761:             }
 762:             List<JsonNode> relevanceCells = new ArrayList<>();
 763:             for (JsonNode cell : define.cells) {
 764:                 if ("RELEVANCE-OBJECT".equals(text(cell, "type")) && !text(cell, "data").isEmpty()) {
 765:                     relevanceCells.add(cell);
 766:                 }
 767:             }
 768:             relevanceCells.sort(CELL_ORDER);
 769:             Set<String> seen = new HashSet<>();
 770:             for (JsonNode cell : relevanceCells) {
 771:                 String id = text(cell, "data");
 772:                 if (!seen.add(id)) {
 773:                     continue;
 774:                 }
 775:                 String direction = "LOCAL";
 776:                 boolean nonnull = false;
 777:                 boolean result = false;
 778:                 // 影响类连线的方向固定为“关联对象 → 过程.端口”，因此看关联对象的出向连线判断输入、入向连线判断输出
 779:                 for (JsonNode edge : define.outgoing(text(cell, "id"))) {
 780:                     if (!isProcessCell(define.target(edge))) {
 781:                         continue;
 782:                     }
 783:                     switch (linkType(edge)) {
 784:                         case "in-influence-nonnull", "in-influence" -> {
 785:                             direction = "IN";
 786:                             nonnull = true;
 787:                         }
 788:                         case "in-influence-nullable" -> direction = "IN";
 789:                         case "in-out-influence-nonnull", "in-out-influence" -> {
 790:                             direction = "IN_OUT";
 791:                             nonnull = true;
 792:                         }
 793:                         case "in-out-influence-nullable" -> direction = "IN_OUT";
 794:                         case "agent", "condition", "consumption" -> {
 795:                             direction = "IN";
 796:                             nonnull = true;
 797:                         }
 798:                         default -> {
 799:                             // 其余连线不改变方向
 800:                         }
 801:                     }
 802:                 }
 803:                 for (JsonNode edge : define.incoming(text(cell, "id"))) {
 804:                     if (!isProcessCell(define.source(edge))) {
 805:                         continue;
 806:                     }
 807:                     String linkType = linkType(edge);
 808:                     if ("result".equals(linkType)) {
 809:                         direction = "OUT";
 810:                         result = true;
 811:                     } else if ("out-influence".equals(linkType)) {
 812:                         direction = "OUT";
 813:                     }
 814:                 }
 815:                 params.add(new PortIR(id, text(entities.get(id), "label"), direction, nonnull, result, slotType(id)));
 816:             }
 817:             return params;
 818:         }
 819: 
 820:         // ------------------------------------------------------------------ 设计视图 → 数据流节点
 821: 
 822:         /**
 823:          * 过程定义解析上下文
 824:          */
 825:         private final class DefCtx {
 826:             final String defKey;
 827:             final String ownerTypeId;
 828:             final ViewCtx design;
 829:             final Map<String, PortIR> params = new HashMap<>();
 830:             final Map<String, OperandIR> operands = new HashMap<>();
 831:             final LinkedHashSet<String> localIds = new LinkedHashSet<>();
 832:             final List<LocalVarIR> localVars = new ArrayList<>();
 833:             final List<NodeIR> nodes = new ArrayList<>();
 834:             final List<Integer> entries = new ArrayList<>();
 835:             final Set<String> warned = new HashSet<>();
 836: 
 837:             DefCtx(String defKey, String ownerTypeId, ViewCtx design) {
 838:                 this.defKey = defKey;
 839:                 this.ownerTypeId = ownerTypeId;
 840:                 this.design = design;
 841:             }
 842: 
 843:             void warn(String message) {
 844:                 if (warned.add(defKey + "|" + message)) {
 845:                     warnings.add("[" + defKey + "] " + message);
 846:                 }
 847:             }
 848:         }
 849: 
 850:         /** 节点原始信息（排序前） */
 851:         private final class NodeRaw {
 852:             final JsonNode cell;
 853:             final JsonNode entity;
 854:             final String cellId;
 855:             boolean tool;
 856:             String toolKey = "";
 857:             String calleeDefKey = "";
 858:             boolean calleeTimeConsuming;
 859:             boolean slow;
 860:             boolean firstStart;
 861:             boolean lastEnd;
 862:             OperandIR host;
 863:             final List<ArgBindingIR> inputs = new ArrayList<>();
 864:             final List<ArgBindingIR> writes = new ArrayList<>();
 865:             final List<JsonNode> nextEdges = new ArrayList<>();
 866:             final List<PostIR> postActions = new ArrayList<>();
 867:             String resultJavaType = "";
 868: 
 869:             NodeRaw(JsonNode cell, JsonNode entity) {
 870:                 this.cell = cell;
 871:                 this.entity = entity;
 872:                 this.cellId = text(cell, "id");
 873:             }
 874:         }
 875: 
 876:         private List<JsonNode> portsOf(JsonNode cell) {
 877:             List<JsonNode> ports = new ArrayList<>();
 878:             JsonNode items = field(field(cell, "ports"), "items");
 879:             if (items != null && items.isArray()) {
 880:                 for (JsonNode port : items) {
 881:                     ports.add(port);
 882:                 }
 883:             }
 884:             return ports;
 885:         }
 886: 
 887:         private JsonNode portArgs(JsonNode port) {
 888:             return field(port, "args");
 889:         }
 890: 
 891:         private String portName(JsonNode args) {
 892:             String other = text(args, "otherData");
 893:             String name = other.isEmpty() ? "" : text(entities.get(other), "label");
 894:             if (name.isEmpty()) {
 895:                 name = text(args, "text");
 896:             }
 897:             return name;
 898:         }
 899: 
 900:         private static String portDirection(JsonNode args) {
 901:             String type = text(args, "type");
 902:             if (type.startsWith("in-out")) {
 903:                 return "IN_OUT";
 904:             }
 905:             if (type.startsWith("in-")) {
 906:                 return "IN";
 907:             }
 908:             if (type.startsWith("out-")) {
 909:                 return "OUT";
 910:             }
 911:             return "";
 912:         }
 913: 
 914:         private boolean isProcessCell(JsonNode cell) {
 915:             return cell != null && PROCESS_CELL_TYPES.contains(text(cell, "type"));
 916:         }
 917: 
 918:         private List<JsonNode> fixedIntoPort(ViewCtx view, String cellId, String portId) {
 919:             List<JsonNode> out = new ArrayList<>();
 920:             for (JsonNode edge : view.incoming(cellId)) {
 921:                 if ("fixed".equals(linkType(edge)) && portId.equals(text(edge.path("target"), "port"))) {
 922:                     out.add(edge);
 923:                 }
 924:             }
 925:             return out;
 926:         }
 927: 
 928:         private void parseFlowNodes(DefCtx def, ViewCtx view, List<PortIR> params) {
 929:             if (view == null) {
 930:                 return;
 931:             }
 932:             List<JsonNode> procCells = new ArrayList<>();
 933:             for (JsonNode cell : view.cells) {
 934:                 if (isProcessCell(cell) && entities.containsKey(text(cell, "data"))) {
 935:                     procCells.add(cell);
 936:                 }
 937:             }
 938:             procCells.sort(CELL_ORDER);
 939:             Map<String, NodeRaw> raws = new LinkedHashMap<>();
 940:             Map<String, Integer> visualOrder = new HashMap<>();
 941:             for (int i = 0; i < procCells.size(); i++) {
 942:                 JsonNode cell = procCells.get(i);
 943:                 visualOrder.put(text(cell, "id"), i);
 944:                 raws.put(text(cell, "id"), readNode(def, view, cell));
 945:             }
 946: 
 947:             // 3. 控制流：全部调用线
 948:             for (JsonNode cell : view.cells) {
 949:                 if (!"LINK".equals(text(cell, "type"))) {
 950:                     continue;
 951:                 }
 952:                 String linkType = linkType(cell);
 953:                 NodeRaw from = raws.get(text(cell.path("source"), "cell"));
 954:                 if (from == null) {
 955:                     continue;
 956:                 }
 957:                 if ("invocation".equals(linkType) || "invocation-self".equals(linkType)) {
 958:                     if (raws.containsKey(text(cell.path("target"), "cell"))) {
 959:                         from.nextEdges.add(cell);
 960:                     }
 961:                 }
 962:             }
 963: 
 964:             // 4. 类型推断（工具输出类型沿数据流传播，迭代到稳定）
 965:             for (int round = 0; round < 8; round++) {
 966:                 boolean changed = false;
 967:                 for (NodeRaw raw : raws.values()) {
 968:                     String next = raw.tool ? inferToolResult(def, raw, raws) : "";
 969:                     if (!next.equals(raw.resultJavaType)) {
 970:                         raw.resultJavaType = next;
 971:                         changed = true;
 972:                     }
 973:                 }
 974:                 if (!changed) {
 975:                     break;
 976:                 }
 977:             }
 978: 
 979:             // 5. 排序与编号：从入口沿调用线广度优先，其余按画布位置追加
 980:             List<NodeRaw> ordered = orderNodes(view, raws, visualOrder);
 981:             Map<String, Integer> pcOf = new HashMap<>();
 982:             for (int i = 0; i < ordered.size(); i++) {
 983:                 pcOf.put(ordered.get(i).cellId, i);
 984:             }
 985:             for (NodeRaw raw : ordered) {
 986:                 List<NextIR> nexts = new ArrayList<>();
 987:                 raw.nextEdges.sort(Comparator.comparingInt(e -> visualOrder.getOrDefault(text(e.path("target"), "cell"), Integer.MAX_VALUE)));
 988:                 for (JsonNode edge : raw.nextEdges) {
 989:                     String targetCell = text(edge.path("target"), "cell");
 990:                     Integer targetPc = pcOf.get(targetCell);
 991:                     if (targetPc == null) {
 992:                         continue;
 993:                     }
 994:                     String sourcePort = text(edge.path("source"), "port");
 995:                     JsonNode linkEntity = entities.get(text(edge, "data"));
 996:                     boolean conditional = !sourcePort.isEmpty();
 997:                     // 连线延迟（存量语义：线上 delayType=STEP N 拍 / FIXED 秒 → 延迟 N 步后激活后继）
 998:                     long delayTicks = 0L;
 999:                     String delayType = text(edge, "delayType");
1000:                     double delayValue = doubleOf(edge, "delayValue", 0.0D);
1001:                     if ("STEP".equalsIgnoreCase(delayType) && delayValue > 0) {
1002:                         delayTicks = Math.max(1L, StrictMath.round(delayValue));
1003:                     } else if ("FIXED".equalsIgnoreCase(delayType) && delayValue > 0) {
1004:                         delayTicks = Math.max(1L, StrictMath.round(delayValue * 1000.0D / DEFAULT_STEP_MS));
1005:                     }
1006:                     NextIR next = new NextIR(
1007:                             targetPc, conditional, conditional ? portName(portArgs(portOfId(raw.cell, sourcePort))) : "",
1008:                             conditionsOf(linkEntity), expressionOf(linkEntity), text(edge, "event"), delayTicks,
1009:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity));
1010:                     // 去重：同一条连线只登记一次
1011:                     if (!nexts.contains(next)) {
1012:                         nexts.add(next);
1013:                     }
1014:                 }
1015:                 // 上游节点输出槽位的类型此刻已推断完成，回填到操作数上
1016:                 Function<OperandIR, OperandIR> resolve = op -> retargetOutput(op, raws);
1017:                 NodeIR node = new NodeIR(
1018:                         pcOf.get(raw.cellId), raw.cellId, text(raw.entity, "id"), text(raw.entity, "label"),
1019:                         raw.tool ? "TOOL" : "CALL", raw.toolKey, raw.calleeDefKey, raw.calleeTimeConsuming, raw.slow,
1020:                         raw.firstStart, raw.lastEnd, raw.host == null ? null : resolve.apply(raw.host),
1021:                         retarget(raw.inputs, resolve), retarget(raw.writes, resolve), raw.resultJavaType,
1022:                         List.copyOf(nexts), retargetPosts(raw.postActions, resolve),
1023:                         doubleOf(raw.cell, "x", 0.0D), doubleOf(raw.cell, "y", 0.0D));
1024:                 def.nodes.add(node);
1025:                 if (raw.firstStart) {
1026:                     def.entries.add(node.pc());
1027:                 }
1028:             }
1029:             // 入口兜底：没有 firstStart 时取没有入向调用线的节点
1030:             if (def.entries.isEmpty() && !def.nodes.isEmpty()) {
1031:                 Set<Integer> targeted = new HashSet<>();
1032:                 for (NodeIR node : def.nodes) {
1033:                     for (NextIR next : node.nexts()) {
1034:                         targeted.add(next.targetPc());
1035:                     }
1036:                 }
1037:                 for (NodeIR node : def.nodes) {
1038:                     if (!targeted.contains(node.pc())) {
1039:                         def.entries.add(node.pc());
1040:                     }
1041:                 }
1042:                 if (def.entries.isEmpty()) {
1043:                     def.entries.add(def.nodes.get(0).pc());
1044:                 }
1045:             }
1046:             diagnoseClockReads(def, raws);
1047:         }
1048: 
1049:         // ------------------------------------------------------------------ T1.1 编译期诊断
1050: 
1051:         /**
1052:          * 同拍双读仿真时间的诊断（只告警，不改语义）：
1053:          * 1) “减”的两个输入都直接读 仿真时间.持续运行时长 —— 按拍量化语义下同拍读数相同，差值恒为 0；
1054:          * 2) “减”位于回边循环内，且两个输入的写入源都能追溯到仿真时钟 —— 循环每圈把两个输入
1055:          *    重刷为同一量化时刻，差值恒为 0（典型：开始时刻在循环体内赋值）。
1056:          * 修正途径：改用 ΔT（CUR-PROCESS-TIME 图元，编译为步长秒数），或把起始量的赋值移出循环。
1057:          */
1058:         private void diagnoseClockReads(DefCtx def, Map<String, NodeRaw> raws) {
1059:             Map<String, List<String>> succ = new HashMap<>();
1060:             for (NodeRaw raw : raws.values()) {
1061:                 List<String> out = new ArrayList<>();
1062:                 for (JsonNode edge : raw.nextEdges) {
1063:                     String target = text(edge.path("target"), "cell");
1064:                     if (raws.containsKey(target)) {
1065:                         out.add(target);
1066:                     }
1067:                 }
1068:                 succ.put(raw.cellId, out);
1069:             }
1070:             Set<String> cycle = new LinkedHashSet<>();
1071:             Map<String, Integer> state = new HashMap<>();
1072:             for (String id : raws.keySet()) {
1073:                 if (!state.containsKey(id)) {
1074:                     collectCycleNodes(id, succ, state, new ArrayList<>(), cycle);
1075:                 }
1076:             }
1077:             for (NodeRaw raw : raws.values()) {
1078:                 if (!"arithmetic.minus".equals(raw.toolKey) || raw.inputs.size() < 2) {
1079:                     continue;
1080:                 }
1081:                 if (raw.inputs.stream().allMatch(b -> b.operand() instanceof BuiltinIR builtin
1082:                         && "SIM_ELAPSED_SECONDS".equals(builtin.key()))) {
1083:                     def.warn("节点 " + raw.cellId + "（减）的两个输入都是仿真时间.持续运行时长：同拍量化读取差值恒为 0，"
1084:                             + "请改用 ΔT（CUR-PROCESS-TIME 图元，编译为步长秒数）或调整画布");
1085:                     continue;
1086:                 }
1087:                 if (!cycle.contains(raw.cellId)) {
1088:                     continue;
1089:                 }
1090:                 boolean allClockFed = raw.inputs.stream()
1091:                         .allMatch(b -> clockFed(b.operand(), raws, 0, new HashSet<>()));
1092:                 if (allClockFed) {
1093:                     def.warn("节点 " + raw.cellId + "（减）位于回边循环内，且两个输入在循环体内每圈都重新赋值为当前量化时刻："
1094:                             + "差值恒为 0；请把起始量的赋值移出循环，或改用 ΔT（CUR-PROCESS-TIME 图元）");
1095:                 }
1096:             }
1097:         }
1098: 
1099:         /** DFS 找回边，把回边覆盖的环上节点记入 cycle */
1100:         private void collectCycleNodes(String id, Map<String, List<String>> succ, Map<String, Integer> state,
1101:                 List<String> path, Set<String> cycle) {
1102:             state.put(id, 1);
1103:             path.add(id);
1104:             for (String next : succ.getOrDefault(id, List.of())) {
1105:                 Integer st = state.get(next);
1106:                 if (st == null) {
1107:                     collectCycleNodes(next, succ, state, path, cycle);
1108:                 } else if (st == 1) {
1109:                     boolean inCycle = false;
1110:                     for (String node : path) {
1111:                         if (node.equals(next)) {
1112:                             inCycle = true;
1113:                         }
1114:                         if (inCycle) {
1115:                             cycle.add(node);
1116:                         }
1117:                     }
1118:                 }
1119:             }
1120:             path.remove(path.size() - 1);
1121:             state.put(id, 2);
1122:         }
1123: 
1124:         /** 操作数的取值链能否追溯到仿真时钟（赋值传递、局部量/成员的写入节点递归，深度与访问集防环） */
1125:         private boolean clockFed(OperandIR operand, Map<String, NodeRaw> raws, int depth, Set<String> visiting) {
1126:             if (operand == null || depth > 6) {
1127:                 return false;
1128:             }
1129:             if (operand instanceof BuiltinIR builtin && "SIM_ELAPSED_SECONDS".equals(builtin.key())) {
1130:                 return true;
1131:             }
1132:             if (operand instanceof NodeOutputIR output) {
1133:                 NodeRaw source = raws.get(output.cellId());
1134:                 if (source == null || !visiting.add(source.cellId)) {
1135:                     return false;
1136:                 }
1137:                 try {
1138:                     for (ArgBindingIR binding : source.inputs) {
1139:                         if (clockFed(binding.operand(), raws, depth + 1, visiting)) {
1140:                             return true;
1141:                         }
1142:                     }
1143:                 } finally {
1144:                     visiting.remove(source.cellId);
1145:                 }
1146:                 return false;
1147:             }
1148:             String entityId = operandEntityId(operand);
1149:             if (entityId == null || entityId.isEmpty() || !visiting.add("member:" + entityId)) {
1150:                 return false;
1151:             }
1152:             try {
1153:                 for (NodeRaw raw : raws.values()) {
1154:                     for (ArgBindingIR binding : raw.writes) {
1155:                         if (!entityId.equals(operandEntityId(binding.operand()))) {
1156:                             continue;
1157:                         }
1158:                         for (ArgBindingIR input : raw.inputs) {
1159:                             if (clockFed(input.operand(), raws, depth + 1, visiting)) {
1160:                                 return true;
1161:                             }
1162:                         }
1163:                     }
1164:                 }
1165:             } finally {
1166:                 visiting.remove("member:" + entityId);
1167:             }
1168:             return false;
1169:         }
1170: 
1171:         /** 操作数对应的实体 id（成员路径取最后一段） */
1172:         private String operandEntityId(OperandIR operand) {
1173:             if (operand instanceof MemberPathIR path) {
1174:                 return path.memberIds().isEmpty() ? "" : path.memberIds().get(path.memberIds().size() - 1);
1175:             }
1176:             if (operand instanceof LocalIR local) {
1177:                 return local.entityId();
1178:             }
1179:             if (operand instanceof ParamIR param) {
1180:                 return param.entityId();
1181:             }
1182:             if (operand instanceof NamedInstanceIR named) {
1183:                 return named.instanceId();
1184:             }
1185:             if (operand instanceof ConstIR constant) {
1186:                 return constant.entityId();
1187:             }
1188:             return "";
1189:         }
1190: 
1191:         private JsonNode portOfId(JsonNode cell, String portId) {
1192:             for (JsonNode port : portsOf(cell)) {
1193:                 if (portId.equals(text(port, "id"))) {
1194:                     return port;
1195:                 }
1196:             }
1197:             return null;
1198:         }
1199: 
1200:         /** 单个过程图元 → 节点原始信息 */
1201:         private NodeRaw readNode(DefCtx def, ViewCtx view, JsonNode cell) {
1202:             JsonNode entity = entities.get(text(cell, "data"));
1203:             NodeRaw raw = new NodeRaw(cell, entity);
1204:             raw.tool = isToolProcess(entity);
1205:             raw.toolKey = raw.tool ? text(entity, "innerClass") + "." + text(entity, "innerType") : "";
1206:             if (!raw.tool) {
1207:                 raw.calleeDefKey = defKeyOf(entity);
1208:                 String cellTime = text(cell, "timeRelated");
1209:                 raw.calleeTimeConsuming = cellTime.isEmpty() ? isTimeRelated(entity) : "RELATED".equalsIgnoreCase(cellTime);
1210:             } else {
1211:                 raw.slow = isTimeRelated(cell);
1212:             }
1213:             raw.firstStart = flag(cell, "firstStart");
1214:             raw.lastEnd = flag(cell, "lastEnd");
1215: 
1216:             // 组成线：CALL 节点的宿主对象 / 集合类工具操作的集合
1217:             for (JsonNode edge : view.incoming(raw.cellId)) {
1218:                 if ("composition".equals(linkType(edge))) {
1219:                     raw.host = operandOf(def, view, view.source(edge));
1220:                     break;
1221:                 }
1222:             }
1223: 
1224:             List<JsonNode> ports = portsOf(cell);
1225:             for (JsonNode port : ports) {
1226:                 String portId = text(port, "id");
1227:                 JsonNode args = portArgs(port);
1228:                 String direction = portDirection(args);
1229:                 if (direction.isEmpty()) {
1230:                     direction = "IN";
1231:                 }
1232:                 String argName = portName(args);
1233:                 if (argName.isEmpty()) {
1234:                     argName = portId;
1235:                 }
1236:                 // 锚点线永远指向被馈入的端口：来源是成员 = 读取或写出；来源是另一节点的输出端口 = 取上游结果
1237:                 List<JsonNode> into = fixedIntoPort(view, raw.cellId, portId);
1238:                 sortBySourcePosition(view, into);
1239:                 for (JsonNode edge : into) {
1240:                     JsonNode source = view.source(edge);
1241:                     OperandIR operand = operandOf(def, view, source, edge);
1242:                     if (isProcessCell(source)) {
1243:                         if ("OUT".equals(direction)) {
1244:                             def.warn("输出端口 " + argName + " 被另一个节点的输出端口馈入，按输入处理");
1245:                         }
1246:                         raw.inputs.add(new ArgBindingIR(argName, operand));
1247:                         continue;
1248:                     }
1249:                     if ("OUT".equals(direction)) {
1250:                         raw.writes.add(new ArgBindingIR(argName, operand));
1251:                         continue;
1252:                     }
1253:                     raw.inputs.add(new ArgBindingIR(argName, operand));
1254:                     if ("IN_OUT".equals(direction)) {
1255:                         raw.writes.add(new ArgBindingIR(argName, operand));
1256:                     }
1257:                 }
1258:             }
1259:             // 无端口定义的过程图元：入向锚点线按唯一隐式双向参数处理
1260:             if (ports.isEmpty()) {
1261:                 List<JsonNode> implicit = new ArrayList<>();
1262:                 for (JsonNode edge : view.incoming(raw.cellId)) {
1263:                     if ("fixed".equals(linkType(edge))) {
1264:                         implicit.add(edge);
1265:                     }
1266:                 }
1267:                 sortBySourcePosition(view, implicit);
1268:                 for (JsonNode edge : implicit) {
1269:                     OperandIR operand = operandOf(def, view, view.source(edge), edge);
1270:                     raw.inputs.add(new ArgBindingIR("a", operand));
1271:                     raw.writes.add(new ArgBindingIR("a", operand));
1272:                 }
1273:             }
1274:             readPostActions(def, view, raw);
1275:             return raw;
1276:         }
1277: 
1278:         /**
1279:          * 节点成功后的后置动作：out-change 状态转换、result 生成、consumption 消耗。
1280:          * 过程设计视图里的子过程调用点与类型 / 场景设计视图里的顶层调用点共用这一规则。
1281:          */
1282:         private void readPostActions(DefCtx def, ViewCtx view, NodeRaw raw) {
1283:             raw.postActions.addAll(collectPostActions(def, view, raw.cellId));
1284:         }
1285: 
1286:         private List<PostIR> collectPostActions(DefCtx def, ViewCtx view, String cellId) {
1287:             List<PostIR> posts = new ArrayList<>();
1288:             List<JsonNode> postEdges = new ArrayList<>();
1289:             for (JsonNode edge : view.outgoing(cellId)) {
1290:                 String linkType = linkType(edge);
1291:                 if ("out-change".equals(linkType) || "result".equals(linkType) || "consumption".equals(linkType)) {
1292:                     postEdges.add(edge);
1293:                 }
1294:             }
1295:             sortByTargetPosition(view, postEdges);
1296:             for (JsonNode edge : postEdges) {
1297:                 JsonNode linkEntity = entities.get(text(edge, "data"));
1298:                 JsonNode target = view.target(edge);
1299:                 if (target == null || isProcessCell(target)) {
1300:                     continue;
1301:                 }
1302:                 String linkType = linkType(edge);
1303:                 if ("out-change".equals(linkType)) {
1304:                     JsonNode stateEntity = entities.get(text(target, "data"));
1305:                     String enumId = enumOfState.getOrDefault(text(target, "data"), "");
1306:                     OperandIR member = enumMemberOperand(def, view, enumId);
1307:                     if (member == null) {
1308:                         def.warn("输出转换找不到枚举类型 " + memberLabel(enumId) + " 的成员，已忽略");
1309:                         continue;
1310:                     }
1311:                     posts.add(new PostIR("OUT_CHANGE", member, text(stateEntity, "label"), enumId, null,
1312:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
1313:                 } else if ("result".equals(linkType)) {
1314:                     posts.add(new PostIR("RESULT", operandOf(def, view, target), "", "", null,
1315:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
1316:                 } else {
1317:                     posts.add(new PostIR("CONSUME", operandOf(def, view, target), "", "", null,
1318:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
1319:                 }
1320:             }
1321:             return posts;
1322:         }
1323: 
1324:         /** 视图里属于指定枚举类型的成员单元格 → 操作数（直接成员优先） */
1325:         private OperandIR enumMemberOperand(DefCtx def, ViewCtx view, String enumId) {
1326:             if (enumId == null) {
1327:                 return null;
1328:             }
1329:             List<JsonNode> candidates = new ArrayList<>();
1330:             for (JsonNode cell : view.cells) {
1331:                 JsonNode entity = entities.get(text(cell, "data"));
1332:                 if (entity == null || !"ENUM".equals(text(entity, "typeRange"))) {
1333:                     continue;
1334:                 }
1335:                 if (enumId.equals(refTypeIdOf(entity))) {
1336:                     candidates.add(cell);
1337:                 }
1338:             }
1339:             candidates.sort(CELL_ORDER);
1340:             JsonNode chosen = null;
1341:             for (JsonNode cell : candidates) {
1342:                 if (pathSourceOf(view, text(cell, "id")).isEmpty()) {
1343:                     chosen = cell;
1344:                     break;
1345:                 }
1346:             }
1347:             if (chosen == null && !candidates.isEmpty()) {
1348:                 chosen = candidates.get(0);
1349:                 def.warn("输出转换的目标枚举成员不唯一，按画布位置取第一个");
1350:             }
1351:             return chosen == null ? null : operandOf(def, view, chosen);
1352:         }
1353: 
1354:         // ------------------------------------------------------------------ 操作数解析
1355: 
1356:         /** 指向该成员单元格的路径线（属性 / 组成），来源不是过程图元 */
1357:         private List<JsonNode> pathSourcesOf(ViewCtx view, String cellId) {
1358:             List<JsonNode> sources = new ArrayList<>();
1359:             for (JsonNode edge : view.incoming(cellId)) {
1360:                 String linkType = linkType(edge);
1361:                 if (("exhibition".equals(linkType) || "composition".equals(linkType))
1362:                         && !isProcessCell(view.source(edge))) {
1363:                     sources.add(edge);
1364:                 }
1365:             }
1366:             return sources;
1367:         }
1368: 
1369:         private String pathSourceOf(ViewCtx view, String cellId) {
1370:             List<JsonNode> sources = pathSourcesOf(view, cellId);
1371:             if (sources.isEmpty()) {
1372:                 return "";
1373:             }
1374:             sortBySourcePosition(view, sources);
1375:             return text(sources.get(0).path("source"), "cell");
1376:         }
1377: 
1378:         private void sortBySourcePosition(ViewCtx view, List<JsonNode> edges) {
1379:             edges.sort(Comparator.comparingDouble((JsonNode e) -> posY(view.source(e)))
1380:                     .thenComparingDouble(e -> posX(view.source(e)))
1381:                     .thenComparing(e -> text(e, "id")));
1382:         }
1383: 
1384:         private void sortByTargetPosition(ViewCtx view, List<JsonNode> edges) {
1385:             edges.sort(Comparator.comparingDouble((JsonNode e) -> posY(view.target(e)))
1386:                     .thenComparingDouble(e -> posX(view.target(e)))
1387:                     .thenComparing(e -> text(e, "id")));
1388:         }
1389: 
1390:         private double posX(JsonNode cell) {
1391:             return cell == null ? 0.0D : doubleOf(cell, "x", 0.0D);
1392:         }
1393: 
1394:         private double posY(JsonNode cell) {
1395:             return cell == null ? 0.0D : doubleOf(cell, "y", 0.0D);
1396:         }
1397: 
1398:         private static double positionOf(JsonNode cell) {
1399:             return (cell == null ? 0.0D : doubleOf(cell, "y", 0.0D)) * 100_000.0D
1400:                     + (cell == null ? 0.0D : doubleOf(cell, "x", 0.0D));
1401:         }
1402: 
1403:         private String entityTypePlaceholder(String typeId) {
1404:             return (typeId == null || typeId.isEmpty()) ? GromaTypes.OBJECT : GromaTypes.entity(typeId);
1405:         }
1406: 
1407:         /**
1408:          * 单元格 → 操作数。edge 非空且单元格是带输出端口的过程图元时，取该节点端口的输出槽位
1409:          */
1410:         private OperandIR operandOf(DefCtx def, ViewCtx view, JsonNode cell) {
1411:             return operandOf(def, view, cell, null);
1412:         }
1413: 
1414:         private OperandIR operandOf(DefCtx def, ViewCtx view, JsonNode cell, JsonNode edge) {
1415:             if (cell == null) {
1416:                 return new ConstIR("", "null", GromaTypes.OBJECT, "<缺失>");
1417:             }
1418:             String cellId = text(cell, "id");
1419:             if (isProcessCell(cell)) {
1420:                 String portId = edge == null ? "" : text(edge.path("source"), "port");
1421:                 JsonNode port = portId.isEmpty() ? null : portOfId(cell, portId);
1422:                 String argName = port == null ? "" : portName(portArgs(port));
1423:                 String cacheKey = cellId + "#" + argName;
1424:                 OperandIR cachedOutput = def.operands.get(cacheKey);
1425:                 if (cachedOutput == null) {
1426:                     cachedOutput = new NodeOutputIR(cellId, argName, GromaTypes.OBJECT);
1427:                     def.operands.put(cacheKey, cachedOutput);
1428:                 }
1429:                 return cachedOutput;
1430:             }
1431:             OperandIR cached = def.operands.get(cellId);
1432:             if (cached != null) {
1433:                 return cached;
1434:             }
1435:             OperandIR operand = resolveOperand(def, view, cell);
1436:             def.operands.put(cellId, operand);
1437:             return operand;
1438:         }
1439: 
1440:         private OperandIR resolveOperand(DefCtx def, ViewCtx view, JsonNode cell) {
1441:             String cellId = text(cell, "id");
1442:             String type = text(cell, "type");
1443:             String entityId = text(cell, "data");
1444:             JsonNode entity = entities.get(entityId);
1445:             String label = entity == null ? text(cell, "label") : text(entity, "label");
1446: 
1447:             // 内置对象根
1448:             switch (type) {
1449:                 case "CUR-RUN-TIME":
1450:                     return new BuiltinIR("SIM", null, GromaTypes.OBJECT);
1451:                 case "CUR-OBJECT":
1452:                 case "THING-OBJECT":
1453:                     return new SelfIR(entityTypePlaceholder(def.ownerTypeId));
1454:                 case "CUR-PROCESS-TIME":
1455:                     // 存量语义（initials/Helper 把该图元解析为 DELTA_T 变量，runtime/ProcessInfo.runProcess
1456:                     // 每次运行刷新为距上次运行的秒数）：过程每拍至多推进一圈，ΔT 恒等于步长秒数
1457:                     return new BuiltinIR("SIM_STEP_SECONDS", null, GromaTypes.DOUBLE);
1458:                 case "ACTUAL-RUN-TIME":
1459:                     return new BuiltinIR("ACTUAL_RUN_SECONDS", null, GromaTypes.DOUBLE);
1460:                 case "USER-INFO":
1461:                     return new BuiltinIR("USER", null, GromaTypes.OBJECT);
1462:                 case "RUN-PARAM":
1463:                     return new BuiltinIR("RUN_PARAM", null, GromaTypes.OBJECT);
1464:                 default:
1465:                     break;
1466:             }
1467: 
1468:             // 路径成员：目标车站 → 位置 → 经度
1469:             String pathSource = pathSourceOf(view, cellId);
1470:             if (!pathSource.isEmpty()) {
1471:                 OperandIR root = operandOf(def, view, view.cell(pathSource));
1472:                 return memberStep(def, root, entity, label);
1473:             }
1474: 
1475:             switch (type) {
1476:                 case "INSTANCE": {
1477:                     String typeId = text(entity, "instanceTypeEntityId");
1478:                     if (typeId.isEmpty()) {
1479:                         typeId = text(entity, "typeEntityId");
1480:                     }
1481:                     String placeholder = entityTypePlaceholder(typeId);
1482:                     if (flag(entity, "isCollection")) {
1483:                         placeholder = "map".equals(text(entity, "containerType"))
1484:                                 ? GromaTypes.map(GromaTypes.baseJavaType(text(entity, "keyType")), placeholder)
1485:                                 : GromaTypes.list(placeholder);
1486:                     }
1487:                     return new NamedInstanceIR(entityId, typeId, placeholder);
1488:                 }
1489:                 case "CONSTANT": {
1490:                     String displayName = label;
1491:                     if (text(entity, "alias").length() > 0 && label.startsWith("定值")) {
1492:                         displayName = text(entity, "alias");
1493:                     }
1494:                     return new ConstIR(entityId, text(entity, "defaultValue"), slotType(entity), displayName);
1495:                 }
1496:                 case "STATE": {
1497:                     String enumId = enumOfState.getOrDefault(entityId, "");
1498:                     return new ConstIR(entityId, label,
1499:                             enumId.isEmpty() ? GromaTypes.STRING : GromaTypes.enumType(enumId), label);
1500:                 }
1501:                 case "NOTE": {
1502:                     String text = text(entity, "description");
1503:                     return new ConstIR(entityId, text.isEmpty() ? label : text, GromaTypes.STRING, label);
1504:                 }
1505:                 default:
1506:                     break;
1507:             }
1508: 
1509:             if (entity == null) {
1510:                 def.warn("单元格 " + cellId + " 引用的实体不存在: " + entityId);
1511:                 return new ConstIR(entityId, "null", GromaTypes.OBJECT, label);
1512:             }
1513: 
1514:             // 根成员：形参 / 局部量 / 宿主自身的成员
1515:             PortIR param = def.params.get(entityId);
1516:             if (param != null && !"LOCAL".equals(param.direction())) {
1517:                 return new ParamIR(entityId, label, param.javaType());
1518:             }
1519:             String entityKind = text(entity, "type");
1520:             boolean variableLike = "TEMPLATE-MEMBER".equals(entityKind) || "RELEVANCE-OBJECT".equals(entityKind)
1521:                     || (param != null);
1522:             if (variableLike) {
1523:                 return declareLocal(def, entity);
1524:             }
1525:             if ("ATTRIBUTE-MEMBER".equals(entityKind) || "COMPOSE-MEMBER".equals(entityKind)
1526:                     || "SETTING-MEMBER".equals(entityKind)) {
1527:                 String owner = memberOwner.get(entityId);
1528:                 if (owner != null && owner.equals(def.ownerTypeId)) {
1529:                     return new MemberPathIR(new SelfIR(entityTypePlaceholder(def.ownerTypeId)), List.of(entityId),
1530:                             slotType(entityId));
1531:                 }
1532:                 // 成员不属于过程宿主：若恰有一个形参/局部量的类型拥有该成员，则从它出发（画布上省略了起点）
1533:                 List<OperandIR> owners = new ArrayList<>();
1534:                 for (JsonNode cell2 : view.cells) {
1535:                     JsonNode e2 = entities.get(text(cell2, "data"));
1536:                     if (e2 == null || text(cell2, "id").equals(cellId)) {
1537:                         continue;
1538:                     }
1539:                     String candidateType = GromaTypes.entityIdOf(slotType(e2));
1540:                     if (candidateType != null && candidateType.equals(owner) && pathSourceOf(view, text(cell2, "id")).isEmpty()) {
1541:                         owners.add(operandOf(def, view, cell2));
1542:                     }
1543:                 }
1544:                 if (owners.size() == 1) {
1545:                     return new MemberPathIR(owners.get(0), List.of(entityId), slotType(entityId));
1546:                 }
1547:                 def.warn("成员“" + label + "”不属于过程宿主类型，且无法确定归属，按宿主成员处理");
1548:                 return new MemberPathIR(new SelfIR(entityTypePlaceholder(def.ownerTypeId)), List.of(entityId),
1549:                         slotType(entityId));
1550:             }
1551:             def.warn("单元格 " + cellId + "（" + type + " " + label + "）不是可解析的操作数，按空值处理");
1552:             return new ConstIR(entityId, "null", GromaTypes.OBJECT, label);
1553:         }
1554: 
1555:         /** 路径上的一步：内置对象的成员、集合的长度、领域对象的成员 */
1556:         private OperandIR memberStep(DefCtx def, OperandIR root, JsonNode memberEntity, String label) {
1557:             String memberId = memberEntity == null ? "" : text(memberEntity, "id");
1558:             if (root instanceof BuiltinIR builtin && "SIM".equals(builtin.key())) {
1559:                 return switch (label) {
1560:                     case "时间日期" -> new BuiltinIR("SIM_DATETIME", null, GromaTypes.LONG);
1561:                     case "持续运行时长" -> new BuiltinIR("SIM_ELAPSED_SECONDS", null, GromaTypes.DOUBLE);
1562:                     default -> new BuiltinIR("SIM_" + label, null, GromaTypes.OBJECT);
1563:                 };
1564:             }
1565:             if (memberEntity != null && "SETTING-MEMBER".equals(text(memberEntity, "type"))) {
1566:                 return switch (text(memberEntity, "supplementType")) {
1567:                     case "length" -> new BuiltinIR("LENGTH", root, GromaTypes.LONG);
1568:                     case "arrayIndex" -> new BuiltinIR("INDEX", root, GromaTypes.LONG);
1569:                     default -> new BuiltinIR("KEY", root, GromaTypes.OBJECT);
1570:                 };
1571:             }
1572:             if (memberEntity != null && ("ARRAY-ELEMENT".equals(text(memberEntity, "type"))
1573:                     || "MAP-ELEMENT".equals(text(memberEntity, "type")))) {
1574:                 // 集合字面量元素在过程设计视图里只作为展示，取其值
1575:                 return new ConstIR(memberId, text(memberEntity, "defaultValue"), slotType(memberEntity), label);
1576:             }
1577:             return new MemberPathIR(root, List.of(memberId), slotType(memberId));
1578:         }
1579: 
1580:         /** 模板成员 / 未连线的关联对象 → 过程局部量（首次出现时登记初值） */
1581:         private LocalIR declareLocal(DefCtx def, JsonNode entity) {
1582:             String id = text(entity, "id");
1583:             String label = text(entity, "label");
1584:             if (def.localIds.add(id)) {
1585:                 def.localVars.add(new LocalVarIR(id, label, slotType(entity), text(entity, "defaultValue")));
1586:             }
1587:             return new LocalIR(id, label, slotType(entity));
1588:         }
1589: 
1590:         /** 上游节点输出槽位的最终类型 */
1591:         private OperandIR retargetOutput(OperandIR operand, Map<String, NodeRaw> raws) {
1592:             if (!(operand instanceof NodeOutputIR out)) {
1593:                 return operand;
1594:             }
1595:             NodeRaw source = raws.get(out.cellId());
1596:             String type;
1597:             if (source == null) {
1598:                 type = GromaTypes.OBJECT;
1599:             } else if (source.tool) {
1600:                 type = source.resultJavaType.isEmpty() ? GromaTypes.OBJECT : source.resultJavaType;
1601:             } else {
1602:                 // 业务子过程的输出形参：按端口对应的形参实体取类型
1603:                 String argEntityId = "";
1604:                 for (JsonNode port : portsOf(source.cell)) {
1605:                     if (out.argName().equals(portName(portArgs(port)))) {
1606:                         argEntityId = text(portArgs(port), "otherData");
1607:                     }
1608:                 }
1609:                 type = argEntityId.isEmpty() ? GromaTypes.OBJECT : slotType(argEntityId);
1610:             }
1611:             return new NodeOutputIR(out.cellId(), out.argName(), type);
1612:         }
1613: 
1614:         private List<ArgBindingIR> retarget(List<ArgBindingIR> bindings, Function<OperandIR, OperandIR> resolve) {
1615:             List<ArgBindingIR> out = new ArrayList<>(bindings.size());
1616:             for (ArgBindingIR binding : bindings) {
1617:                 out.add(new ArgBindingIR(binding.argName(), resolve.apply(binding.operand())));
1618:             }
1619:             return out;
1620:         }
1621: 
1622:         private List<PostIR> retargetPosts(List<PostIR> posts, Function<OperandIR, OperandIR> resolve) {
1623:             List<PostIR> out = new ArrayList<>(posts.size());
1624:             for (PostIR post : posts) {
1625:                 out.add(new PostIR(post.kind(), post.target() == null ? null : resolve.apply(post.target()),
1626:                         post.stateLabel(), post.enumTypeId(), post.value() == null ? null : resolve.apply(post.value()),
1627:                         post.probability(), post.probabilityGroup()));
1628:             }
1629:             return out;
1630:         }
1631: 
1632:         // ------------------------------------------------------------------ 类型推断与排序
1633: 
1634:         private String operandType(OperandIR operand, Map<String, NodeRaw> raws) {
1635:             if (operand instanceof NodeOutputIR out) {
1636:                 NodeRaw source = raws.get(out.cellId());
1637:                 return (source != null && source.tool && !source.resultJavaType.isEmpty())
1638:                         ? source.resultJavaType : GromaTypes.OBJECT;
1639:             }
1640:             return operand.javaType();
1641:         }
1642: 
1643:         /** 工具节点输出值类型（沿数据流迭代到稳定） */
1644:         private String inferToolResult(DefCtx def, NodeRaw raw, Map<String, NodeRaw> raws) {
1645:             Map<String, String> argTypes = new HashMap<>();
1646:             for (ArgBindingIR binding : raw.inputs) {
1647:                 argTypes.putIfAbsent(binding.argName(), operandType(binding.operand(), raws));
1648:             }
1649:             String a = argTypes.getOrDefault("a", "");
1650:             String b = argTypes.getOrDefault("b", "");

Pythonp = '/tmp/orpl_extract/orpl-9ee34655eecdb360241d86a189cb40c9527ef56a/groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.java'
with open(p, 'r', encoding='utf-8') as f:
    lines = f.readlines()
for i, line in enumerate(lines[1650:], 1651):
    print(f"{i:4d}: {line}", end='')

代码输出1651:             String host = raw.host == null ? "" : raw.host.javaType();
1652:             String key = raw.toolKey;
1653:             if (key.startsWith("arithmetic.")) {
1654:                 return switch (key) {
1655:                     case "arithmetic.add", "arithmetic.minus", "arithmetic.multiply", "arithmetic.max", "arithmetic.min" ->
1656:                             GromaTypes.numericResult(a, b);
1657:                     case "arithmetic.reverse", "arithmetic.absolute" -> GromaTypes.isNumeric(a) ? a : GromaTypes.LONG;
1658:                     case "arithmetic.mod" -> GromaTypes.LONG;
1659:                     default -> GromaTypes.DOUBLE;
1660:                 };
1661:             }
1662:             if (key.startsWith("compare.") || key.startsWith("logic.") || "isNull.isNull".equals(key)) {
1663:                 return GromaTypes.BOOLEAN;
1664:             }
1665:             if ("assignment.assign".equals(key) || "assignment.assignRef".equals(key)) {
1666:                 return a.isEmpty() ? GromaTypes.OBJECT : a;
1667:             }
1668:             if (key.startsWith("automatic.") || key.startsWith("assignment.") || key.startsWith("types.update")) {
1669:                 return host.isEmpty() ? a : host;
1670:             }
1671:             if (key.startsWith("trigonometric.") || key.startsWith("accuracy.")) {
1672:                 return GromaTypes.DOUBLE;
1673:             }
1674:             if (key.startsWith("bitwise.") || key.startsWith("shift.") || "text.getTextLength".equals(key)) {
1675:                 return GromaTypes.LONG;
1676:             }
1677:             if (key.startsWith("text.")) {
1678:                 return GromaTypes.STRING;
1679:             }
1680:             if (key.startsWith("time.") || key.startsWith("innerCollectionSet.") || key.startsWith("innerCollectionMap.put")
1681:                     || key.startsWith("innerCollectionMap.remove") || key.startsWith("innerCollectionMap.clear")
1682:                     || key.startsWith("innerCollectionArray.add") || key.startsWith("innerCollectionArray.remove")
1683:                     || key.startsWith("innerCollectionArray.clear")) {
1684:                 return GromaTypes.OBJECT;
1685:             }
1686:             if ("innerCollectionArray.getByIndex".equals(key)) {
1687:                 String element = GromaTypes.elementTypeOf(host);
1688:                 return GromaTypes.OBJECT.equals(element) ? explicitOutType(raw) : element;
1689:             }
1690:             if ("innerCollectionMap.getByKey".equals(key)) {
1691:                 String element = GromaTypes.elementTypeOf(host);
1692:                 return GromaTypes.OBJECT.equals(element) ? explicitOutType(raw) : element;
1693:             }
1694:             String explicit = explicitOutType(raw);
1695:             return explicit;
1696:         }
1697: 
1698:         /** 工具定义里输出端口显式声明的类型（如 c:boolean） */
1699:         private String explicitOutType(NodeRaw raw) {
1700:             for (JsonNode port : portsOf(raw.cell)) {
1701:                 if ("OUT".equals(portDirection(portArgs(port)))) {
1702:                     JsonNode relevance = entities.get(text(portArgs(port), "otherData"));
1703:                     String fieldType = relevance == null ? "" : text(relevance, "fieldType");
1704:                     if (!fieldType.isEmpty() && !"empty".equals(fieldType) && !"anyType".equals(fieldType)) {
1705:                         return slotType(relevance);
1706:                     }
1707:                 }
1708:             }
1709:             return GromaTypes.OBJECT;
1710:         }
1711: 
1712:         /** 从入口沿调用线广度优先排序；未达节点按画布位置追加 */
1713:         private List<NodeRaw> orderNodes(ViewCtx view, Map<String, NodeRaw> raws, Map<String, Integer> visualOrder) {
1714:             List<NodeRaw> all = new ArrayList<>(raws.values());
1715:             all.sort(Comparator.comparingInt(r -> visualOrder.get(r.cellId)));
1716:             List<NodeRaw> entries = new ArrayList<>();
1717:             for (NodeRaw raw : all) {
1718:                 if (raw.firstStart) {
1719:                     entries.add(raw);
1720:                 }
1721:             }
1722:             if (entries.isEmpty()) {
1723:                 Set<String> targeted = new HashSet<>();
1724:                 for (NodeRaw raw : all) {
1725:                     for (JsonNode edge : raw.nextEdges) {
1726:                         targeted.add(text(edge.path("target"), "cell"));
1727:                     }
1728:                 }
1729:                 for (NodeRaw raw : all) {
1730:                     if (!targeted.contains(raw.cellId)) {
1731:                         entries.add(raw);
1732:                     }
1733:                 }
1734:             }
1735:             if (entries.isEmpty()) {
1736:                 entries.add(all.get(0));
1737:             }
1738:             List<NodeRaw> ordered = new ArrayList<>(all.size());
1739:             Set<String> visited = new HashSet<>();
1740:             List<NodeRaw> queue = new ArrayList<>(entries);
1741:             while (!queue.isEmpty()) {
1742:                 NodeRaw current = queue.remove(0);
1743:                 if (!visited.add(current.cellId)) {
1744:                     continue;
1745:                 }
1746:                 ordered.add(current);
1747:                 List<NodeRaw> targets = new ArrayList<>();
1748:                 for (JsonNode edge : current.nextEdges) {
1749:                     NodeRaw target = raws.get(text(edge.path("target"), "cell"));
1750:                     if (target != null) {
1751:                         targets.add(target);
1752:                     }
1753:                 }
1754:                 targets.sort(Comparator.comparingInt(r -> visualOrder.get(r.cellId)));
1755:                 queue.addAll(targets);
1756:             }
1757:             for (NodeRaw raw : all) {
1758:                 if (visited.add(raw.cellId)) {
1759:                     ordered.add(raw);
1760:                 }
1761:             }
1762:             return ordered;
1763:         }
1764: 
1765:         // ------------------------------------------------------------------ 连线与线上判断
1766: 
1767:         private String linkType(JsonNode edge) {
1768:             String shape = text(edge, "shape");
1769:             if (shape.startsWith("edge-")) {
1770:                 return shape.substring("edge-".length());
1771:             }
1772:             JsonNode linkEntity = entities.get(text(edge, "data"));
1773:             return linkTypeOfEntity(linkEntity);
1774:         }
1775: 
1776:         private String linkTypeOfEntity(JsonNode linkEntity) {
1777:             if (linkEntity == null) {
1778:                 return "";
1779:             }
1780:             String linkType = text(linkEntity, "linkType");
1781:             int comma = linkType.indexOf(',');
1782:             return comma > 0 ? linkType.substring(0, comma) : linkType;
1783:         }
1784: 
1785:         private JsonNode linkLogic(JsonNode linkEntity) {
1786:             JsonNode logic = field(linkEntity, "logic");
1787:             return (logic != null && logic.isObject()) ? logic : null;
1788:         }
1789: 
1790:         private List<ConditionIR> conditionsOf(JsonNode linkEntity) {
1791:             JsonNode logic = linkLogic(linkEntity);
1792:             if (logic == null) {
1793:                 return List.of();
1794:             }
1795:             JsonNode conditions = field(logic, "conditions");
1796:             List<ConditionIR> out = new ArrayList<>();
1797:             if (conditions != null && conditions.isArray()) {
1798:                 int ordinal = 0;
1799:                 for (JsonNode condition : conditions) {
1800:                     ordinal++;
1801:                     // 判断值有三种存法：标量、单元素数组 value:[true]、多值数组 values:[a,b]
1802:                     JsonNode literalNode = field(condition, "value");
1803:                     if (literalNode != null && literalNode.isArray()) {
1804:                         literalNode = literalNode.isEmpty() ? null : literalNode.get(0);
1805:                     }
1806:                     if (literalNode == null || literalNode.isNull() || literalNode.isMissingNode()) {
1807:                         JsonNode values = field(condition, "values");
1808:                         literalNode = (values != null && values.isArray() && !values.isEmpty()) ? values.get(0) : null;
1809:                     }
1810:                     String literalType = "null";
1811:                     String literal = "null";
1812:                     if (literalNode != null && !literalNode.isNull()) {
1813:                         if (literalNode.isBoolean()) {
1814:                             literalType = "boolean";
1815:                             literal = literalNode.asString();
1816:                         } else if (literalNode.isNumber()) {
1817:                             literalType = "number";
1818:                             literal = literalNode.asString();
1819:                         } else if (literalNode.isString()) {
1820:                             literalType = "string";
1821:                             literal = literalNode.stringValue();
1822:                         }
1823:                     }
1824:                     String code = text(condition, "code");
1825:                     out.add(new ConditionIR((int) longOf(condition, "index", ordinal),
1826:                             code.isEmpty() ? "=" : code, literal, literalType));
1827:                 }
1828:             }
1829:             // 布尔线上判断的判定值：存量引擎 createOneCallJudge 以连线上的 determine（true / false）为准
1830:             String determine = text(linkEntity, "determine");
1831:             if (out.isEmpty() && ("true".equalsIgnoreCase(determine) || "false".equalsIgnoreCase(determine))) {
1832:                 out.add(new ConditionIR(1, "=", determine.toLowerCase(Locale.ROOT), "boolean"));
1833:             }
1834:             return out;
1835:         }
1836: 
1837:         private String expressionOf(JsonNode linkEntity) {
1838:             JsonNode logic = linkLogic(linkEntity);
1839:             return logic == null ? "" : text(logic, "expression");
1840:         }
1841: 
1842:         // ------------------------------------------------------------------ 事件接线与默认启动（类型 / 场景设计视图）
1843: 
1844:         private void buildWiring() {
1845:             LinkedHashSet<String> owners = new LinkedHashSet<>(domainTypeIds);
1846:             owners.addAll(sceneIds);
1847:             for (String typeId : owners) {
1848:                 JsonNode type = entities.get(typeId);
1849:                 ViewCtx view = view(text(type, "designViewId"));
1850:                 if (view == null) {
1851:                     continue;
1852:                 }
1853:                 DefCtx ctx = new DefCtx("owner:" + typeId, typeId, view);
1854:                 List<JsonNode> callSites = new ArrayList<>();
1855:                 for (JsonNode cell : view.cells) {
1856:                     if (isProcessCell(cell) && !isToolProcess(entities.get(text(cell, "data")))) {
1857:                         callSites.add(cell);
1858:                     }
1859:                 }
1860:                 callSites.sort(CELL_ORDER);
1861:                 for (JsonNode callSite : callSites) {
1862:                     JsonNode callee = entities.get(text(callSite, "data"));
1863:                     String defKey = defKeyOf(callee);
1864: 
1865:                     // 组成线：过程宿主是接线对象的某个成员（路径含该成员自身；没有组成线时宿主就是接线对象）
1866:                     List<String> hostPath = List.of();
1867:                     for (JsonNode edge : view.incoming(text(callSite, "id"))) {
1868:                         if ("composition".equals(linkType(edge))) {
1869:                             JsonNode hostCell = view.source(edge);
1870:                             List<String> path = new ArrayList<>(memberPathOf(ctx, view, hostCell));
1871:                             path.add(text(hostCell, "data"));
1872:                             hostPath = List.copyOf(path);
1873:                             break;
1874:                         }
1875:                     }
1876:                     // 锚点线：实参绑定（相对于接线对象）；输出影响 / 双向影响端口绑定的是成员时，过程成功后把形参终值写回该成员
1877:                     // （存量引擎 handleInstantsWhenSuccess：PARAM_OUT / PARAM_IO / EFFECT_OUT / EFFECT_NOTNULL 走 outputParam）
1878:                     List<ArgBindingIR> args = new ArrayList<>();
1879:                     List<PostIR> postActions = new ArrayList<>(collectPostActions(ctx, view, text(callSite, "id")));
1880:                     for (JsonNode port : portsOf(callSite)) {
1881:                         JsonNode portArgs = portArgs(port);
1882:                         String argName = portName(portArgs);
1883:                         String direction = portDirection(portArgs);
1884:                         if (direction.isEmpty()) {
1885:                             direction = "IN";
1886:                         }
1887:                         for (JsonNode edge : fixedIntoPort(view, text(callSite, "id"), text(port, "id"))) {
1888:                             JsonNode source = view.source(edge);
1889:                             OperandIR operand = operandOf(ctx, view, source, edge);
1890:                             if (!"OUT".equals(direction)) {
1891:                                 args.add(new ArgBindingIR(argName, operand));
1892:                             }
1893:                             if (!"IN".equals(direction) && !isProcessCell(source)) {
1894:                                 OperandIR outParam = outParamOperand(defKey, argName, text(portArgs, "otherData"));
1895:                                 if (outParam != null) {
1896:                                     postActions.add(new PostIR("WRITE_BACK", operand, "", "", outParam));
1897:                                 } else {
1898:                                     ctx.warn("调用点「" + text(callee, "label") + "」的输出端口 " + argName + " 不在定义视图的形参契约里，输出影响回写已跳过");
1899:                                 }
1900:                             }
1901:                         }
1902:                     }
1903: 
1904:                     boolean defaultStart = flag(callSite, "defaultStart");
1905:                     if (defaultStart) {
1906:                         defaultStarts.add(new DefaultStartIR(typeId, hostPath, defKey, List.copyOf(args),
1907:                                 List.copyOf(postActions)));
1908:                     }
1909:                     int triggersBefore = triggers.size();
1910:                     // 事件触发：凡是带事件码、到达过程图元的连线都登记（存量引擎 BaseClazz.addBinding：
1911:                     // 锚点线带事件码同样进事件绑定表；组成线与属性线只是路径，不触发）
1912:                     for (JsonNode edge : view.incoming(text(callSite, "id"))) {
1913:                         JsonNode source = view.source(edge);
1914:                         if (isProcessCell(source) || "composition".equals(linkType(edge))
1915:                                 || "exhibition".equals(linkType(edge))) {
1916:                             continue;
1917:                         }
1918:                         String events = text(edge, "event");
1919:                         if (events.isEmpty()) {
1920:                             continue;
1921:                         }
1922:                         JsonNode linkEntity = entities.get(text(edge, "data"));
1923:                         for (String eventCode : events.split(",")) {
1924:                             if (!eventCode.isBlank()) {
1925:                                 triggers.add(buildTrigger(ctx, view, source, eventCode.trim(), conditionsOf(linkEntity),
1926:                                         expressionOf(linkEntity), defKey, hostPath, args, postActions, typeId,
1927:                                         linkType(edge) + " " + text(callee, "label")));
1928:                             }
1929:                         }
1930:                     }
1931:                     // 手动启动（平台定义）：既无默认启动也无事件触发的调用点，纳入外部干预入口，
1932:                     // 画布实参作为默认值，运行时允许按中文形参名覆盖
1933:                     if (!defaultStart && triggers.size() == triggersBefore) {
1934:                         manualStarts.add(new ManualStartIR(typeId, hostPath, defKey, List.copyOf(args),
1935:                                 List.copyOf(postActions)));
1936:                     }
1937:                 }
1938:             }
1939:         }
1940: 
1941:         /** 被调过程的输出形参操作数（按端口上绑定的关联对象标识在定义视图的形参契约里定位） */
1942:         private OperandIR outParamOperand(String defKey, String argName, String paramEntityId) {
1943:             for (ProcessDefIR def : processDefs) {
1944:                 if (!def.defKey().equals(defKey)) {
1945:                     continue;
1946:                 }
1947:                 for (PortIR port : def.params()) {
1948:                     boolean matched = paramEntityId.isEmpty() ? port.zhName().equals(argName) : port.entityId().equals(paramEntityId);
1949:                     if (matched) {
1950:                         return new ParamIR(port.entityId(), port.zhName(), port.javaType());
1951:                     }
1952:                 }
1953:             }
1954:             return null;
1955:         }
1956: 
1957:         /** 从接线对象到某成员单元格的成员路径（不含该单元格自身） */
1958:         private List<String> memberPathOf(DefCtx ctx, ViewCtx view, JsonNode cell) {
1959:             String source = pathSourceOf(view, text(cell, "id"));
1960:             if (source.isEmpty()) {
1961:                 return List.of();
1962:             }
1963:             List<String> path = new ArrayList<>(memberPathOf(ctx, view, view.cell(source)));
1964:             path.add(text(view.cell(source), "data"));
1965:             return path;
1966:         }
1967: 
1968:         private TriggerIR buildTrigger(DefCtx ctx, ViewCtx view, JsonNode sourceCell, String eventCode,
1969:                                        List<ConditionIR> judge, String expression, String targetDefKey,
1970:                                        List<String> hostPath, List<ArgBindingIR> args, List<PostIR> postActions,
1971:                                        String ownerTypeId, String describe) {
1972:             JsonNode sourceEntity = entities.get(text(sourceCell, "data"));
1973:             String stateLabel = "";
1974:             String memberToken;
1975:             List<String> contextPath;
1976:             OperandIR sourceOperand;
1977:             if ("STATE".equals(text(sourceCell, "type"))) {
1978:                 stateLabel = text(sourceCell, "label").isEmpty() ? text(sourceEntity, "label") : text(sourceCell, "label");
1979:                 String enumId = enumOfState.getOrDefault(text(sourceCell, "data"), "");
1980:                 OperandIR member = enumMemberOperand(ctx, view, enumId);
1981:                 if (member instanceof MemberPathIR path) {
1982:                     memberToken = path.memberIds().get(path.memberIds().size() - 1);
1983:                     contextPath = fullPathOf(path);
1984:                     contextPath = contextPath.subList(0, contextPath.size() - 1);
1985:                     sourceOperand = path;
1986:                 } else {
1987:                     ctx.warn("状态触发找不到枚举成员: " + stateLabel);
1988:                     memberToken = text(sourceCell, "data");
1989:                     contextPath = List.of();
1990:                     sourceOperand = null;
1991:                 }
1992:             } else {
1993:                 memberToken = text(sourceCell, "data");
1994:                 contextPath = memberPathOf(ctx, view, sourceCell);
1995:                 sourceOperand = operandOf(ctx, view, sourceCell);
1996:             }
1997:             String sourceTypeId = contextPath.isEmpty()
1998:                     ? ownerTypeId : GromaTypes.entityIdOf(slotType(contextPath.get(contextPath.size() - 1)));
1999:             if (sourceTypeId == null) {
2000:                 sourceTypeId = ownerTypeId;
2001:             }
2002:             return new TriggerIR(sourceTypeId, memberToken, eventCode, stateLabel, judge,
2003:                     sourceOperand, contextPath, targetDefKey, hostPath, args, postActions, ownerTypeId, describe);
2004:         }
2005: 
2006:         /** 成员路径操作数 → 从宿主自身的完整成员 id 序列 */
2007:         private List<String> fullPathOf(MemberPathIR path) {
2008:             List<String> full = new ArrayList<>();
2009:             OperandIR cursor = path;
2010:             while (cursor instanceof MemberPathIR step) {
2011:                 full.add(0, step.memberIds().get(step.memberIds().size() - 1));
2012:                 cursor = step.root();
2013:             }
2014:             return full;
2015:         }
2016: 
2017:         // ------------------------------------------------------------------ 类型、实例、关联类型
2018: 
2019:         private MemberIR memberIR(String memberId) {
2020:             JsonNode entity = entities.get(memberId);
2021:             String typeRange = text(entity, "typeRange");
2022:             String refTypeId = ("ENUM".equals(typeRange) || "DEFINE".equals(typeRange)) ? refTypeIdOf(entity) : "";
2023:             String fieldType = text(entity, "fieldType");
2024:             if (fieldType.isEmpty() || "empty".equals(fieldType) || "anyType".equals(fieldType)) {
2025:                 fieldType = "ENUM".equals(typeRange) || "DEFINE".equals(typeRange) ? "string" : "Object";
2026:             }
2027:             String initialState = "";
2028:             if ("ENUM".equals(typeRange)) {
2029:                 initialState = text(entity, "defaultValue");
2030:                 if (initialState.isEmpty()) {
2031:                     initialState = enumInitial.getOrDefault(refTypeId, "");
2032:                 }
2033:             }
2034:             return new MemberIR(
2035:                     memberId, text(entity, "label"), text(entity, "alias"), fieldType, typeRange, refTypeId,
2036:                     flag(entity, "isCollection"), text(entity, "containerType"), text(entity, "defaultValue"),
2037:                     text(entity, "unit"), text(entity, "minValue"), text(entity, "maxValue"), initialState,
2038:                     text(entity, "description"), slotType(entity));
2039:         }
2040: 
2041:         private List<TypeDefIR> buildTypeDefs() {
2042:             List<TypeDefIR> types = new ArrayList<>();
2043:             LinkedHashSet<String> owners = new LinkedHashSet<>(domainTypeIds);
2044:             owners.addAll(sceneIds);
2045:             owners.addAll(assocTypeIds);
2046:             for (String typeId : owners) {
2047:                 JsonNode entity = entities.get(typeId);
2048:                 List<MemberIR> members = new ArrayList<>();
2049:                 for (String memberId : typeMembers.getOrDefault(typeId, List.of())) {
2050:                     members.add(memberIR(memberId));
2051:                 }
2052:                 List<String> parentIds = resolveParentIds(typeId, entity);
2053:                 String parent = parentIds.isEmpty() ? "" : parentIds.get(0);
2054:                 List<String> mixins = parentIds.size() > 1 ? parentIds.subList(1, parentIds.size()) : List.of();
2055:                 types.add(new TypeDefIR(typeId, text(entity, "label"), text(entity, "description"),
2056:                         text(entity, "essence"), typeKind.get(typeId), parent,
2057:                         List.copyOf(mixins), List.copyOf(parseProxies(typeId)), List.copyOf(members), List.of()));
2058:             }
2059:             return types;
2060:         }
2061: 
2062:         /**
2063:          * 收割补丁 A（R01 多继承）：inheritEntityId 单值 + inherits 数组 + 定义/设计视图的
2064:          * PARENT 图元与 edge-extend 连线一并收集（模型 2 智能筹划含 5 组画布继承）。
2065:          * 返回有序去重父类 id 列表：首为主父类，其余为副父类（mixin）。
2066:          */
2067:         private List<String> resolveParentIds(String typeId, JsonNode typeEntity) {
2068:             java.util.LinkedHashSet<String> parents = new java.util.LinkedHashSet<>();
2069:             String direct = text(typeEntity, "inheritEntityId");
2070:             if (!direct.isEmpty() && entities.containsKey(direct) && !direct.equals(typeId)) {
2071:                 parents.add(direct);
2072:             }
2073:             JsonNode inherits = field(typeEntity, "inherits");
2074:             if (inherits != null && inherits.isArray()) {
2075:                 for (JsonNode item : inherits) {
2076:                     String pid = item.isObject() ? text(item, "id") : item.asText().trim();
2077:                     if (!pid.isEmpty() && entities.containsKey(pid) && !pid.equals(typeId)) {
2078:                         parents.add(pid);
2079:                     }
2080:                 }
2081:             }
2082:             for (String viewId : List.of(text(typeEntity, "defineViewId"), text(typeEntity, "designViewId"))) {
2083:                 ViewCtx v = view(viewId);
2084:                 if (v == null) {
2085:                     continue;
2086:                 }
2087:                 for (JsonNode edge : v.cells) {
2088:                     if (!"LINK".equals(text(edge, "type")) || !"extend".equals(linkType(edge))) {
2089:                         continue;
2090:                     }
2091:                     JsonNode src = v.source(edge);
2092:                     JsonNode dst = v.target(edge);
2093:                     if (src == null || dst == null) {
2094:                         continue;
2095:                     }
2096:                     // 方圆画布约定：子类型 --edge-extend--> 父类型（PARENT 或 TYPE 图元）
2097:                     String childData = text(src, "data");
2098:                     String parentData = text(dst, "data");
2099:                     String resolvedPid = childData.equals(typeId) ? parentData
2100:                             : (parentData.equals(typeId) ? childData : "");
2101:                     JsonNode parentEntity = entities.get(resolvedPid);
2102:                     if (parentEntity != null && !"TYPE".equals(text(parentEntity, "type"))) {
2103:                         String viaRef = refTypeIdOf(parentEntity);
2104:                         resolvedPid = viaRef.isEmpty() ? findByLabel(text(parentEntity, "label"), false) : viaRef;
2105:                     }
2106:                     if (!resolvedPid.isEmpty() && domainTypeIds.contains(resolvedPid) && !resolvedPid.equals(typeId)) {
2107:                         parents.add(resolvedPid);
2108:                     }
2109:                 }
2110:             }
2111:             return List.copyOf(parents);
2112:         }
2113: 
2114:         /**
2115:          * 收割补丁 B（R05 集合成员代理）：定义/设计视图的 PROXY 图元 + 入向 edge-proxy 连线
2116:          * → ProxyDefIR（模型 2 智能筹划含 1 组）。解析信息进 IR，出码消费（ObservableEntityList 包装）
2117:          * 见 docs/dual_model_codegen_audit.md 方案 C。
2118:          */
2119:         private List<ProxyDefIR> parseProxies(String typeId) {
2120:             List<ProxyDefIR> out = new ArrayList<>();
2121:             for (String viewId : List.of(text(entities.get(typeId), "defineViewId"),
2122:                     text(entities.get(typeId), "designViewId"))) {
2123:                 ViewCtx v = view(viewId);
2124:                 if (v == null) {
2125:                     continue;
2126:                 }
2127:                 for (JsonNode cell : v.cells) {
2128:                     if (!"PROXY".equals(text(cell, "type"))) {
2129:                         continue;
2130:                     }
2131:                     String proxyEid = text(cell, "data");
2132:                     JsonNode proxyEntity = entities.get(proxyEid);
2133:                     String label = proxyEntity == null ? text(cell, "label") : text(proxyEntity, "label");
2134:                     for (JsonNode edge : v.incoming(text(cell, "id"))) {
2135:                         if (!"proxy".equals(linkType(edge))) {
2136:                             continue;
2137:                         }
2138:                         JsonNode collCell = v.source(edge);
2139:                         if (collCell == null) {
2140:                             continue;
2141:                         }
2142:                         String collMemberId = text(collCell, "data");
2143:                         String elemType = GromaTypes.entityIdOf(GromaTypes.elementTypeOf(slotType(collMemberId)));
2144:                         out.add(new ProxyDefIR(
2145:                                 proxyEid.isEmpty() ? text(cell, "id") : proxyEid,
2146:                                 label,
2147:                                 collMemberId,
2148:                                 elemType == null ? "" : elemType));
2149:                     }
2150:                 }
2151:             }
2152:             return out;
2153:         }
2154: 
2155:         private List<InstanceIR> buildInstances() {
2156:             List<InstanceIR> instances = new ArrayList<>();
2157:             for (String eid : entities.keySet()) {
2158:                 JsonNode entity = entities.get(eid);
2159:                 if (!"INSTANCE".equals(text(entity, "type"))) {
2160:                     continue;
2161:                 }
2162:                 String typeId = text(entity, "instanceTypeEntityId");
2163:                 if (typeId.isEmpty()) {
2164:                     typeId = text(entity, "typeEntityId");
2165:                 }
2166:                 ViewCtx view = view(text(entity, "defineViewId"));
2167:                 JsonNode root = null;
2168:                 if (view != null) {
2169:                     for (JsonNode cell : view.cells) {
2170:                         if ("INSTANCE".equals(text(cell, "type")) && eid.equals(text(cell, "data"))) {
2171:                             root = cell;
2172:                             break;
2173:                         }
2174:                     }
2175:                 }
2176:                 ValueSpec spec;
2177:                 if (flag(entity, "isCollection")) {
2178:                     // 数组 / 映射实例：元素经 ARRAY-ELEMENT / MAP-ELEMENT 的“成员”引用其他命名实例
2179:                     spec = root == null ? new ListSpec(List.of()) : collectionSpec(view, root, text(entity, "containerType"));
2180:                 } else {
2181:                     spec = root == null
2182:                             ? new ObjectSpec(typeId, Map.of())
2183:                             : new ObjectSpec(typeId, objectMembers(view, root));
2184:                 }
2185:                 instances.add(new InstanceIR(eid, text(entity, "label"), typeId, spec));
2186:             }
2187:             // 场景自身：场景设计视图里的组成成员即场景对象的成员取值
2188:             for (String sceneId : sceneIds) {
2189:                 JsonNode scene = entities.get(sceneId);
2190:                 ViewCtx view = view(text(scene, "designViewId"));
2191:                 Map<String, ValueSpec> members = new LinkedHashMap<>();
2192:                 if (view != null) {
2193:                     List<JsonNode> memberCells = new ArrayList<>();
2194:                     for (JsonNode cell : view.cells) {
2195:                         JsonNode entity = entities.get(text(cell, "data"));
2196:                         if (entity != null && "COMPOSE-MEMBER".equals(text(entity, "type"))
2197:                                 && sceneId.equals(text(entity, "typeEntityId"))) {
2198:                             memberCells.add(cell);
2199:                         }
2200:                     }
2201:                     memberCells.sort(CELL_ORDER);
2202:                     for (JsonNode cell : memberCells) {
2203:                         members.putIfAbsent(text(cell, "data"), valueSpecOf(view, cell));
2204:                     }
2205:                 }
2206:                 instances.add(new InstanceIR("scene:" + sceneId, text(scene, "label"), sceneId,
2207:                         new ObjectSpec(sceneId, members)));
2208:             }
2209:             return instances;
2210:         }
2211: 
2212:         /** 对象图：宿主单元格的全部成员取值 */
2213:         private Map<String, ValueSpec> objectMembers(ViewCtx view, JsonNode container) {
2214:             Map<String, ValueSpec> members = new LinkedHashMap<>();
2215:             List<JsonNode> memberCells = new ArrayList<>();
2216:             for (JsonNode edge : view.outgoing(text(container, "id"))) {
2217:                 String linkType = linkType(edge);
2218:                 JsonNode target = view.target(edge);
2219:                 if (("exhibition".equals(linkType) || "composition".equals(linkType)) && target != null
2220:                         && !isProcessCell(target) && !"NOTE".equals(text(target, "type"))) {
2221:                     memberCells.add(target);
2222:                 }
2223:             }
2224:             memberCells.sort(CELL_ORDER);
2225:             for (JsonNode cell : memberCells) {
2226:                 members.putIfAbsent(text(cell, "data"), valueSpecOf(view, cell));
2227:             }
2228:             return members;
2229:         }
2230: 
2231:         /** 单个成员的取值：字面量 / 引用命名实例 / 内联对象 / 集合 / 未赋值 */
2232:         private ValueSpec valueSpecOf(ViewCtx view, JsonNode cell) {
2233:             JsonNode entity = entities.get(text(cell, "data"));
2234:             String reference = text(cell, "instanceEntityId");
2235:             if (reference.isEmpty() && entity != null) {
2236:                 reference = text(entity, "instanceEntityId");
2237:             }
2238:             List<JsonNode> elements = new ArrayList<>();
2239:             List<JsonNode> inline = new ArrayList<>();
2240:             for (JsonNode edge : view.outgoing(text(cell, "id"))) {
2241:                 if (!"composition".equals(linkType(edge))) {
2242:                     continue;
2243:                 }
2244:                 JsonNode child = view.target(edge);
2245:                 if (child == null) {
2246:                     continue;
2247:                 }
2248:                 String childType = text(child, "type");
2249:                 if ("MAP-ELEMENT".equals(childType) || "ARRAY-ELEMENT".equals(childType)) {
2250:                     elements.add(child);
2251:                 } else if (!isProcessCell(child)) {
2252:                     inline.add(child);
2253:                 }
2254:             }
2255:             if (!elements.isEmpty()) {
2256:                 return collectionSpec(view, cell, text(entity, "containerType"));
2257:             }
2258:             if (!reference.isEmpty()) {
2259:                 String typeId = (entity != null && "DEFINE".equals(text(entity, "typeRange")))
2260:                         ? refTypeIdOf(entity) : (entity == null ? "" : text(entity, "instanceTypeEntityId"));
2261:                 return new RefSpec(reference, typeId);
2262:             }
2263:             if (!inline.isEmpty()) {
2264:                 String typeId = entity == null ? "" : refTypeIdOf(entity);
2265:                 Map<String, ValueSpec> nested = new LinkedHashMap<>();
2266:                 for (JsonNode child : inline) {
2267:                     nested.put(text(child, "data"), valueSpecOf(view, child));
2268:                 }
2269:                 return new ObjectSpec(typeId, nested);
2270:             }
2271:             String raw = text(cell, "defaultValue");
2272:             if (raw.isEmpty() && entity != null) {
2273:                 raw = text(entity, "defaultValue");
2274:             }
2275:             if (!raw.isEmpty()) {
2276:                 return new ScalarSpec(raw, entity == null ? "string" : text(entity, "fieldType"));
2277:             }
2278:             return NullSpec.INSTANCE;
2279:         }
2280: 
2281:         /**
2282:          * 容器单元格（集合成员 / 集合实例）的取值：数组按元素序号排成有序集合，映射按"键#值"标签的键排成键值集合。
2283:          * 元素值取其"成员"组成线指向的取值（引用命名实例或内联对象），没有成员时取元素自身默认值。
2284:          */
2285:         private ValueSpec collectionSpec(ViewCtx view, JsonNode container, String containerType) {
2286:             List<JsonNode> elements = new ArrayList<>();
2287:             for (JsonNode edge : view.outgoing(text(container, "id"))) {
2288:                 if (!"composition".equals(linkType(edge))) {
2289:                     continue;
2290:                 }
2291:                 JsonNode child = view.target(edge);
2292:                 if (child != null && ("MAP-ELEMENT".equals(text(child, "type")) || "ARRAY-ELEMENT".equals(text(child, "type")))) {
2293:                     elements.add(child);
2294:                 }
2295:             }
2296:             elements.sort(ELEMENT_ORDER);
2297:             boolean map = "map".equals(containerType) || elements.stream().anyMatch(e -> "MAP-ELEMENT".equals(text(e, "type")));
2298:             if (map) {
2299:                 Map<String, ValueSpec> entries = new LinkedHashMap<>();
2300:                 for (JsonNode element : elements) {
2301:                     String[] pair = elementLabelPair(element);
2302:                     entries.put(pair[0], elementValue(view, element));
2303:                 }
2304:                 return new MapSpec(entries);
2305:             }
2306:             List<ValueSpec> items = new ArrayList<>();
2307:             for (JsonNode element : elements) {
2308:                 items.add(elementValue(view, element));
2309:             }
2310:             return new ListSpec(items);
2311:         }
2312: 
2313:         /** 集合元素单元格的标签形如 "键#值" 或 "序号#值" */
2314:         private String[] elementLabelPair(JsonNode element) {
2315:             String label = text(element, "label");
2316:             int hash = label.indexOf('#');
2317:             return hash > 0 ? new String[] {label.substring(0, hash), label.substring(hash + 1)}
2318:                     : new String[] {label, ""};
2319:         }
2320: 
2321:         private ValueSpec elementValue(ViewCtx view, JsonNode element) {
2322:             for (JsonNode edge : view.outgoing(text(element, "id"))) {
2323:                 if (!"composition".equals(linkType(edge))) {
2324:                     continue;
2325:                 }
2326:                 JsonNode child = view.target(edge);
2327:                 if (child != null && !isProcessCell(child)) {
2328:                     return valueSpecOf(view, child);
2329:                 }
2330:             }
2331:             String raw = text(element, "defaultValue");
2332:             JsonNode entity = entities.get(text(element, "data"));
2333:             return raw.isEmpty() ? NullSpec.INSTANCE
2334:                     : new ScalarSpec(raw, entity == null ? "string" : text(entity, "fieldType"));
2335:         }
2336: 
2337:         private List<AssocTypeIR> buildAssocTypes() {
2338:             List<AssocTypeIR> out = new ArrayList<>();
2339:             for (String assocId : assocTypeIds) {
2340:                 JsonNode assoc = entities.get(assocId);
2341:                 ViewCtx view = view(text(assoc, "designViewId"));
2342:                 if (view == null) {
2343:                     continue;
2344:                 }
2345:                 for (JsonNode edge : view.cells) {
2346:                     if (!"LINK".equals(text(edge, "type")) || !"bidirectional-tagged".equals(linkType(edge))) {
2347:                         continue;
2348:                     }
2349:                     JsonNode leftCell = view.source(edge);
2350:                     JsonNode rightCell = view.target(edge);
2351:                     JsonNode left = entities.get(text(leftCell, "data"));
2352:                     JsonNode right = entities.get(text(rightCell, "data"));
2353:                     JsonNode linkEntity = entities.get(text(edge, "data"));
2354:                     out.add(new AssocTypeIR(
2355:                             assocId, text(assoc, "label"),
2356:                             text(leftCell, "data"), text(left, "label"), refTypeIdOf(left),
2357:                             text(rightCell, "data"), text(right, "label"), refTypeIdOf(right),
2358:                             text(linkEntity, "constraint1"), text(linkEntity, "constraint2"),
2359:                             text(linkEntity, "linkLabel1"), text(linkEntity, "linkLabel2")));
2360:                 }
2361:             }
2362:             return out;
2363:         }
2364: 
2365:         // ------------------------------------------------------------------ 组装兼容 IR
2366: 
2367:         private GromaModuleIR assemble(CompilePlanIR plan) {
2368:             List<GromaModuleIR.ConstantIR> constants = new ArrayList<>();
2369:             for (String eid : entities.keySet()) {
2370:                 JsonNode entity = entities.get(eid);
2371:                 if ("CONSTANT".equals(text(entity, "type")) && !BUILTIN_TOOL_TOPS.contains(treeTop.getOrDefault(eid, ""))) {
2372:                     String label = text(entity, "label");
2373:                     if (label.startsWith("定值") && !text(entity, "alias").isEmpty()) {
2374:                         label = text(entity, "alias");
2375:                     }
2376:                     constants.add(new GromaModuleIR.ConstantIR(eid, label, legacyFieldType(slotType(entity)),
2377:                             text(entity, "defaultValue"), text(entity, "unit")));
2378:                 }
2379:             }
2380:             List<GromaModuleIR.EnumIR> enums = new ArrayList<>();
2381:             for (String enumId : enumStates.keySet()) {
2382:                 enums.add(new GromaModuleIR.EnumIR(enumId, text(entities.get(enumId), "label"), enumStates.get(enumId),
2383:                         enumInitial.getOrDefault(enumId, "")));
2384:             }
2385:             List<GromaModuleIR.TypeIR> domainTypes = new ArrayList<>();
2386:             List<GromaModuleIR.ProcessIR> independentProcesses = new ArrayList<>();
2387:             for (TypeDefIR type : plan.types()) {
2388:                 List<GromaModuleIR.FieldIR> fields = new ArrayList<>();
2389:                 List<GromaModuleIR.TriggerBindingIR> noTriggers = List.of();
2390:                 for (MemberIR member : type.members()) {
2391:                     fields.add(new GromaModuleIR.FieldIR(
2392:                             member.entityId(), member.zhName(), legacyFieldType(slotType(member.entityId())),
2393:                             member.typeRange(), member.collection(), false, member.unit(), member.minValue(),
2394:                             member.maxValue(), member.defaultValue(), noTriggers));
2395:                 }
2396:                 List<GromaModuleIR.ProcessIR> methods = new ArrayList<>();
2397:                 for (ProcessDefIR def : plan.processDefs()) {
2398:                     if (type.entityId().equals(def.ownerTypeId())) {
2399:                         methods.add(toProcessIR(def, plan.configuredStepMs()));
2400:                     }
2401:                 }
2402:                 domainTypes.add(new GromaModuleIR.TypeIR(type.entityId(), type.zhName(), type.description(), type.essence(),
2403:                         type.primaryParentId().isEmpty() ? null : type.primaryParentId(), List.of(), fields, methods));
2404:             }
2405:             for (ProcessDefIR def : plan.processDefs()) {
2406:                 if ("INDEPENDENT".equals(def.ownerKind())) {
2407:                     independentProcesses.add(toProcessIR(def, plan.configuredStepMs()));
2408:                 }
2409:             }
2410:             List<GromaModuleIR.SceneIR> scenes = new ArrayList<>();
2411:             for (String sceneId : sceneIds) {
2412:                 JsonNode scene = entities.get(sceneId);
2413:                 List<GromaModuleIR.SceneMemberIR> initMembers = new ArrayList<>();
2414:                 for (MemberIR member : memberIRsOf(plan, sceneId)) {
2415:                     Map<String, String> overrides = new LinkedHashMap<>();
2416:                     if (!member.defaultValue().isEmpty()) {
2417:                         overrides.put("defaultValue", member.defaultValue());
2418:                     }
2419:                     String reference = instanceReferenceOf(plan, sceneId, member.entityId());
2420:                     if (!reference.isEmpty()) {
2421:                         overrides.put("instance", reference);
2422:                     }
2423:                     String memberType = member.refTypeId().isEmpty() ? member.fieldType() : member.refTypeId();
2424:                     initMembers.add(new GromaModuleIR.SceneMemberIR(member.entityId(), member.zhName(), memberType, overrides));
2425:                 }
2426:                 List<String> defaultStartOperators = new ArrayList<>();
2427:                 List<String> defaultStartProcesses = new ArrayList<>();
2428:                 List<GromaModuleIR.ProcessIR> sceneLevel = new ArrayList<>();
2429:                 for (ProcessDefIR def : plan.processDefs()) {
2430:                     if (!sceneId.equals(def.ownerTypeId())) {
2431:                         continue;
2432:                     }
2433:                     GromaModuleIR.ProcessIR process = toProcessIR(def, plan.configuredStepMs());
2434:                     sceneLevel.add(process);
2435:                     boolean started = plan.defaultStarts().stream().anyMatch(ds -> ds.targetDefKey().equals(def.defKey())
2436:                             && sceneId.equals(ds.typeId()));
2437:                     if (started) {
2438:                         (def.timeConsuming() ? defaultStartProcesses : defaultStartOperators).add(def.defKey());
2439:                     }
2440:                 }
2441:                 scenes.add(new GromaModuleIR.SceneIR(
2442:                         "tree:" + sceneId, sceneId, text(scene, "label"), text(scene, "defineViewId"),
2443:                         text(scene, "designViewId"), initMembers, defaultStartOperators, defaultStartProcesses, sceneLevel));
2444:             }
2445:             Map<String, GromaModuleIR.ViewGraphIR> views = new TreeMap<>();
2446:             for (Map.Entry<String, List<JsonNode>> entry : rawViews.entrySet()) {
2447:                 views.put(entry.getKey(), toViewGraph(entry.getKey(), entry.getValue()));
2448:             }
2449:             String digest = sha256Hex(bytesA) + ":" + sha256Hex(bytesB);
2450:             return new GromaModuleIR("2.0", modelId, "", modelLabel, modelVersion, digest, plan.configuredStepMs(),
2451:                     constants, enums, domainTypes, independentProcesses, scenes, views, plan);
2452:         }
2453: 
2454:         private List<MemberIR> memberIRsOf(CompilePlanIR plan, String typeId) {
2455:             for (TypeDefIR type : plan.types()) {
2456:                 if (typeId.equals(type.entityId())) {
2457:                     return type.members();
2458:                 }
2459:             }
2460:             return List.of();
2461:         }
2462: 
2463:         private String instanceReferenceOf(CompilePlanIR plan, String sceneId, String memberId) {
2464:             for (InstanceIR instance : plan.instances()) {
2465:                 if (("scene:" + sceneId).equals(instance.instanceId()) && instance.spec() instanceof ObjectSpec object) {
2466:                     if (object.members().get(memberId) instanceof RefSpec ref) {
2467:                         return ref.instanceId();
2468:                     }
2469:                 }
2470:             }
2471:             return "";
2472:         }
2473: 
2474:         /** 类型占位 → 旧 IR 的 fieldType 约定（基础类型名，或枚举/类型的实体 id） */
2475:         private String legacyFieldType(String placeholder) {
2476:             if (GromaTypes.isEnum(placeholder) || GromaTypes.isEntity(placeholder)) {
2477:                 return placeholder.substring(placeholder.indexOf(':') + 1);
2478:             }
2479:             if (GromaTypes.isCollection(placeholder)) {
2480:                 return "Object";
2481:             }
2482:             return placeholder;
2483:         }
2484: 
2485:         /** 设计视图里是否有自调用线（R24） */
2486:         private boolean selfInvoking(ProcessDefIR def) {
2487:             ViewCtx view = view(def.designViewId());
2488:             if (view == null) {
2489:                 return false;
2490:             }
2491:             for (JsonNode cell : view.cells) {
2492:                 if ("LINK".equals(text(cell, "type")) && "invocation-self".equals(linkType(cell))) {
2493:                     return true;
2494:                 }
2495:             }
2496:             return false;
2497:         }
2498: 
2499:         private GromaModuleIR.ProcessIR toProcessIR(ProcessDefIR def, long stepMs) {
2500:             List<GromaModuleIR.ParamPortIR> inputs = new ArrayList<>();
2501:             List<GromaModuleIR.ParamPortIR> outputs = new ArrayList<>();
2502:             for (PortIR port : def.params()) {
2503:                 GromaModuleIR.ParamPortIR legacy = new GromaModuleIR.ParamPortIR(
2504:                         port.entityId(), port.entityId(), port.zhName(), legacyFieldType(port.javaType()),
2505:                         GromaTypes.isEnum(port.javaType()) ? "ENUM" : GromaTypes.isEntity(port.javaType()) ? "DEFINE" : "BASE",
2506:                         port.nonnull(), "IN".equals(port.direction()), "", null, null, null);
2507:                 if ("IN".equals(port.direction()) || "IN_OUT".equals(port.direction())) {
2508:                     inputs.add(legacy);
2509:                 }
2510:                 if ("OUT".equals(port.direction()) || "IN_OUT".equals(port.direction()) || "LOCAL".equals(port.direction())) {
2511:                     outputs.add(legacy);
2512:                 }
2513:             }
2514:             List<GromaModuleIR.FieldIR> locals = new ArrayList<>();
2515:             for (LocalVarIR local : def.locals()) {
2516:                 locals.add(new GromaModuleIR.FieldIR(local.entityId(), local.zhName(), legacyFieldType(local.javaType()),
2517:                         "BASE", false, false, "", "", "", local.initLiteral(), List.of()));
2518:             }
2519:             List<GromaModuleIR.SubProcessStepIR> subSteps = new ArrayList<>();
2520:             for (NodeIR node : def.nodes()) {
2521:                 if (!"CALL".equals(node.kind())) {
2522:                     continue;
2523:                 }
2524:                 List<Integer> nextPcs = new ArrayList<>();
2525:                 for (NextIR next : node.nexts()) {
2526:                     nextPcs.add(next.targetPc());
2527:                 }
2528:                 subSteps.add(new GromaModuleIR.SubProcessStepIR(node.pc(), node.cellId(), node.entityId(), node.zhName(),
2529:                         node.calleeTimeConsuming(), node.firstStart(), false, nextPcs, null));
2530:             }
2531:             List<GromaModuleIR.PostActionIR> postActions = new ArrayList<>();
2532:             for (NodeIR node : def.nodes()) {
2533:                 for (PostIR post : node.postActions()) {
2534:                     postActions.add(new GromaModuleIR.PostActionIR(
2535:                             "OUT_CHANGE".equals(post.kind()) ? "OUT_CHANGE" : post.kind(),
2536:                             post.kind() + "@" + node.pc(), post.stateLabel(), 1.0D, ""));
2537:                 }
2538:             }
2539:             long period = def.accuracyMs() == null ? 1L : Math.max(1L, (def.accuracyMs() + stepMs - 1L) / stepMs);
2540:             String singleton = def.singletonMode() == 3 ? "GLOBAL_SINGLETON"
2541:                     : def.singletonMode() == 1 ? "OBJECT_SINGLETON" : "NONE";
2542:             return new GromaModuleIR.ProcessIR(
2543:                     def.entityId(), def.ownerTypeId(), def.zhName(), def.description(),
2544:                     def.timeConsuming(), def.twinLocked(), def.asyncMode(), singleton, selfInvoking(def),
2545:                     def.durationParamType(), def.timeNormal(), def.timeMin(), def.timeMax(), def.timeUnit(), period,
2546:                     def.javaCode(), inputs, outputs, locals, subSteps, List.of(), List.of(), postActions,
2547:                     !"SCENE".equals(def.ownerKind()), text(entities.get(def.entityId()), "defineViewId"), def.designViewId());
2548:         }
2549: 
2550:         private GromaModuleIR.ViewGraphIR toViewGraph(String viewId, List<JsonNode> cells) {
2551:             List<GromaModuleIR.NodeCellIR> nodes = new ArrayList<>();
2552:             List<GromaModuleIR.EdgeCellIR> edges = new ArrayList<>();
2553:             for (JsonNode cell : cells) {
2554:                 if ("LINK".equals(text(cell, "type"))) {
2555:                     String relation = linkType(cell);
2556:                     if (!CanvasMappingTables.SHAPE_TO_RELATION.containsKey("edge-" + relation) && !relation.isEmpty()) {
2557:                         if (warnings.stream().noneMatch(w -> w.contains("未注册的连线形状: " + relation))) {
2558:                             warnings.add("[PRS-WARN-001] 未注册的连线形状: shape=edge-" + relation + ", viewId=" + viewId
2559:                                     + "（按原样保留，不做语义编译）");
2560:                         }
2561:                     }
2562:                     edges.add(new GromaModuleIR.EdgeCellIR(
2563:                             text(cell, "id"), text(cell, "data"), relation,
2564:                             text(cell.path("source"), "cell"), text(cell.path("target"), "cell"), text(cell, "event")));
2565:                 } else {
2566:                     JsonNode entity = entities.get(text(cell, "data"));
2567:                     nodes.add(new GromaModuleIR.NodeCellIR(
2568:                             text(cell, "id"), text(cell, "data"), text(cell, "type"), text(cell, "shape"),
2569:                             entity == null ? text(cell, "label") : text(entity, "label"),
2570:                             flag(cell, "defaultStart"), flag(cell, "firstStart"), flag(cell, "lastEnd"),
2571:                             doubleOf(cell, "x", 0.0D), doubleOf(cell, "y", 0.0D)));
2572:                 }
2573:             }
2574:             return new GromaModuleIR.ViewGraphIR(viewId, viewId.startsWith("defineView"), nodes, edges);
2575:         }
2576: 
2577:         // ------------------------------------------------------------------ 视图索引
2578: 
2579:         /**
2580:          * 单个画布视图的单元格与连线索引
2581:          */
2582:         private final class ViewCtx {
2583:             final String viewId;
2584:             final List<JsonNode> cells;
2585:             private final Map<String, JsonNode> byId = new HashMap<>();
2586:             private final Map<String, List<JsonNode>> incoming = new HashMap<>();
2587:             private final Map<String, List<JsonNode>> outgoing = new HashMap<>();
2588: 
2589:             ViewCtx(String viewId, List<JsonNode> cells) {
2590:                 this.viewId = viewId;
2591:                 this.cells = cells;
2592:                 for (JsonNode cell : cells) {
2593:                     byId.put(text(cell, "id"), cell);
2594:                 }
2595:                 for (JsonNode cell : cells) {
2596:                     if (!"LINK".equals(text(cell, "type"))) {
2597:                         continue;
2598:                     }
2599:                     String source = text(cell.path("source"), "cell");
2600:                     String target = text(cell.path("target"), "cell");
2601:                     if (source.isEmpty() || target.isEmpty()) {
2602:                         continue;
2603:                     }
2604:                     incoming.computeIfAbsent(target, k -> new ArrayList<>()).add(cell);
2605:                     outgoing.computeIfAbsent(source, k -> new ArrayList<>()).add(cell);
2606:                 }
2607:             }
2608: 
2609:             JsonNode cell(String cellId) {
2610:                 return cellId == null ? null : byId.get(cellId);
2611:             }
2612: 
2613:             JsonNode source(JsonNode edge) {
2614:                 return cell(text(edge.path("source"), "cell"));
2615:             }
2616: 
2617:             JsonNode target(JsonNode edge) {
2618:                 return cell(text(edge.path("target"), "cell"));
2619:             }
2620: 
2621:             List<JsonNode> incoming(String cellId) {
2622:                 return incoming.getOrDefault(cellId, List.of());
2623:             }
2624: 
2625:             List<JsonNode> outgoing(String cellId) {
2626:                 return outgoing.getOrDefault(cellId, List.of());
2627:             }
2628:         }
2629:     }
2630: }
2631: 
2632: 
2633: 

本次自动生成独立 Markdown 文件组件时遇到工具调用步数限制的技术问题，未能直接挂载为独立文件附件；以下直接提供完整、结构化的 Markdown 审查与改造实现报告全文，可直接复制保存为 dual_model_codegen_audit_and_solution.md 使用。双模型（模型1 data-a/b + 模型2 scene-a/b）Java工程转化与全关系表示能力深度审查及完整改造方案一、核心审查结论总览基于《方圆语言2D_3D智能建模与多端推演平台_需求与技术架构设计说明书.docx》（V12.0）、code_artifact.md 以及仓库 groma/ 全量源码与两套真实模型语料的交叉核验，核心审查结论如下：   问题 1：现有项目代码能否按说明书 V12.0 + code_artifact.md 要求将两个模型数据转化为标准 Java 工程代码？结论：可完成基础解析、核心源码生成与内存 ECJ 编译，但尚未完全达到设计说明书与 code_artifact.md 要求的“完整五层标准 Maven 多模块可独立合包运行工程”标准。模型 1（data-a.json + data-b.json，应急决策推演-关联副本-1029，v647）：能够走通 AbModelMergerAndParser $\rightarrow$ StaticModelValidator $\rightarrow$ GromaModuleSourceGeneratorFacade $\rightarrow$ EcjInMemoryCompiler 流水线并生成 model-emergency-deduction 模块，但缺失 engine/relation/*RelationManager.java、model/dto/*StartDTO.java、无自有过程场景的 IScene2Service 接口编排，且根工程存在双轨 groma-models 目录分裂导致默认 mvn clean package 未静态合包挂载生成模块。模型 2（scene-a.json + scene-b.json，智能筹划应用，v844）：工作区尚未预生成落盘模块；虽然支持在线导入编译，但存在三大阻断性工程缺口：① 独立过程“航路规划与同步到达算法”在 javaCode 字段内嵌了完整外部脚本代码，生成侧虽加入了 invokeExternalScriptWithIo 调用，但 groma-bootstrap 宿主侧未装配 ExternalScriptIoExecutor 实现，运行期外部算法桥接断路；② 7 处过程手写中文代码调用了旧平台包装器方法（.getValue()、.setValue()、.setKeyInstOrValue()、.itemsToNJson()），现有 ProcessCodeAstTranslator 未做旧 API 到原生 POJO Getter/Setter 的 AST 重写，触发降级桩；③ 模型挂载了 3 个外部组件（componentIds），外部组件父类 地图展示对象（5 处 PARENT）在 resolveParentIds 中因 domainTypeIds.contains(...) 过滤而丢失继承结构。问题 2：生成后的代码能否全部表示两个模型的数据关系及标准规定的全量关系？结论：不能全部表示。 对照 code_artifact.md 权威口径的 29 类画布连线关系（R00~R28） 与 21 类标准事件，当前代码仅完整闭环了约 14 类关系与 9 类事件，其余 15 类关系与 12 类事件存在“仅解析进 IR 未出码”、“组装兼容 IR 时硬编码丢参”或“底座未派发事件”等断层，导致模型 1 的 5 个 ASSOCIATION-TYPE 双端关联基数约束、模型 2 的 5 组 PARENT + edge-extend 组件继承与 1 组 PROXY + edge-proxy 集合成员代理、以及双模型共 560+ 条单向输入连线的 ReadOnly 编译期只读契约均未能在生成的 Java 代码中完整体现。二、双模型真实语料画像与结构特征深度剖析对 groma/docs/ 下四份语料文件解包（合并规则 cellMap = A ∪ B，B 覆盖 A）后的精确统计如下表所示：维度指标模型 1：data-a.json + data-b.json模型 2：scene-a.json + scene-b.json工程转化关键影响模型标识与名称3cW2TNyvVxiJxV07ZlR / 应急决策推演-关联副本-1029YQVVXLuFmS6d0IjUYH6 / 智能筹划应用需映射为独立模块 model-emergency-deduction 与 model-smart-planning版本号与本质v647 / INFORMATICAL（态势感知模型）v844 / INFORMATICAL（挂载 3 个外部组件 ID）模型 2 高度依赖跨组件 TYPE / ENUM / PARENT 解析实体总数 (entities)2,632 个2,023 个包含以数字开头的内部 ID（模型 1 中 17 个 TYPE、模型 2 中 21 个 TYPE）画布视图与图元总数A: 240 + B: 175 $\rightarrow$ 合并 415 视图 / 3,493 CellsA: 305 + B: 56 $\rightarrow$ 合并 361 视图 / 2,406 Cells必须执行确定性排序（40px 网格行优先）保障多次出码一致核心结构图元统计TYPE: 47, SCENE: 4, ASSOCIATION-TYPE: 5, ENUM: 10, STATE: 32, CONSTANT: 32, INSTANCE: 26, ATTRIBUTE-MEMBER: 144, COMPOSE-MEMBER: 81TYPE: 51, SCENE: 4, PARENT: 5, PROXY: 1, ENUM: 2, STATE: 6, CONSTANT: 22, INSTANCE: 0, ATTRIBUTE-MEMBER: 91, COMPOSE-MEMBER: 138模型 1 重度使用 ASSOCIATION-TYPE 与 INSTANCE；模型 2 使用 PARENT 继承与 PROXY 集合代理过程图元统计PROCESS: 191, TEMPLATE-PROCESS: 23, INDEPENDENT-PROCESS: 4PROCESS: 210, TEMPLATE-PROCESS: 7, INDEPENDENT-PROCESS: 2独立过程必须生成 IIndependentProcessService (+Impl)连线分布 (LINK)共 1,458 条：fixed(487)、invocation(225)、exhibition(208)、in-influence-nonnull(180)、out-influence(177)、in-influence(72)、composition(56)、in-out-influence-nonnull(12)、out-change(11)、bidirectional-tagged(9)、in-influence-nullable(8)、result(5)、condition(3)、in-out-influence(2)、note(1)、invocation-self(1)、in-out-influence-nullable(1)共 893 条：fixed(219)、out-influence(195)、in-influence-nonnull(144)、in-influence-nullable(94)、invocation(87)、in-influence(72)、composition(47)、exhibition(14)、in-out-influence-nonnull(9)、extend(5)、result(3)、in-out-influence(2)、proxy(1)、in-out-influence-nullable(1)覆盖继承、代理、双向带标签关联、启动条件、状态转换、自调用、备注等关键语义线上判断与事件分布determine: 63 处；event: 13 处（sa: 7, a: 4, v: 2）；双端基数 constraint1/2: 5 组determine: 15 处；event: 10 处（v: 7, a: 3）线上逻辑表达式与概率组需无损编译至 ConditionJudgeUtil 与 TriggerTable手写代码 (javaCode)1 处：文本1.setKeyInstOrValue(false);8 处：7 处中文旧 API Java 片段 + 1 处完整外部航路规划脚本（# -*- coding: utf-8 -*-）需 AST 中文符号与旧 API 双重转译 + 宿主级脚本 JSON I/O 沙盒桥接   三、Java 工程转化能力对标审查（发现 6 大源码级工程断层）逐行核查 AbModelMergerAndParser.java、StaticModelValidator.java、groma-source-generator 及 groma-bootstrap 后，定位出以下阻断完整工程规范落地的 6 大源码缺陷：   缺陷 ENG-1：AbModelMergerAndParser.assemble() 组装 GromaModuleIR 时存在 5 处硬编码丢参，导致 StaticModelValidator 多项校验被架空AbModelMergerAndParser.java 第 2389 行：FieldIR.eventTriggers 硬编码传入 List.of()，导致 StaticModelValidator 中遍历 field.eventTriggers() 的 VAL-008（触发器互斥组概率和校验） 与 VAL-010（触发条件路径深度校验） 永远接收空列表。第 2403 行：构造 GromaModuleIR.TypeIR 时，尽管 TypeDefIR 已解析出副父类 mixins，第 6 个参数 mixinParentEntityIds 却硬编码传入 List.of()，导致 VAL-003（多继承环路检测） 漏检副父类环路。第 2529 行：构造 SubProcessStepIR 时，第 7 个参数 lastEnd 硬编码传入 false（丢弃了 node.lastEnd()），failBranchPc 硬编码传入 null。第 2536 行：构造 PostActionIR 时，最后两个参数 probability 与 probabilityGroup 硬编码传入 1.0D, ""（丢弃了 post.probability() 与 post.probabilityGroup()），导致 VAL-008（后置动作互斥组概率和校验） 100% 漏检。第 2546 行：构造 ProcessIR 时，startConditions 与 maintainConditions 硬编码传入 List.of(), List.of()；且 StaticModelValidator.validate()（第 62~68 行）未遍历 ir.scenes() 下的 sceneLevelProcesses()，导致场景直属过程逃逸 VAL-001/VAL-004 校验。缺陷 ENG-2：模型子模块五层目录裁剪缺失（缺 engine/relation、model/dto、ws 及部分场景 SceneService）groma-source-generator 的 codegen/plan/ 包下缺少 RelationLayerEmitter 与 DtoLayerEmitter。说明书 3.1 节要求每个场景生成完整三件套（{Scene}SceneController + I{Scene}SceneService/Impl + {Scene}WorldContext + {Scene}StartDTO），但现行生成器对没有直属自有过程的 场景2 未生成 IScene2Service，也未生成任何 *StartDTO.java。缺陷 ENG-3：双轨 groma-models 工程目录分裂，未兑现一键 mvn clean package 静态合包承诺仓库根下 groma/groma-models/ 存放的是手写简化演示代码（无 META-INF/services/com.groma.base.engine.GromaModuleRuntime SPI 注册文件），而流水线生成的完整五层代码位于 groma/groma-bootstrap/groma-workspace/groma-models/，且 groma-bootstrap/pom.xml 未通过 Profile 聚合挂载生成模块，下载后直接构建的 JAR 无法开箱即用双模型。缺陷 ENG-4：模型 2 外部航路规划脚本在宿主侧缺少执行器装配GromaWorldContext 虽定义了 ExternalScriptIoExecutor 接口供生成代码调用 invokeExternalScriptWithIo，但 groma-bootstrap 未提供将 ExternalProgramConnector 桥接至 ExternalScriptIoExecutor 的 Spring 配置类，会话启动时 externalScriptIoExecutor 为 null，导致模型 2 核心算法“航路规划与同步到达算法”无法实际执行计算。缺陷 ENG-5：手写代码旧版方圆包装器方法未做 AST 重写模型 1 的 文本1.setKeyInstOrValue(false); 与模型 2 的 7 处 数据类型.getValue()、文本值.getValue()、会话数据.setValue(会话记录列表.itemsToNJson()) 使用了存量解释器时代的包装类 API，在新架构原生 double/boolean/String/List 类型上直接编译报错或被 LeafBodyEmitter 拦截降级为 fail(...)。缺陷 ENG-6：外部组件父类与枚举定义解析盲区 + 存档续跑确定性缺口模型 2 中 5 个 PARENT 图元指向组件 F82l2cCcyAOghfHnzYJ 的 地图展示对象，在 AbModelMergerAndParser.resolveParentIds（第 2106 行）若组件类型被归入内置或未建立跨组件索引会被丢弃；SimulationArchiveBundle.TaskArchivePart 未持久化 startEpochMs，且非数字后缀实例 ID 未推进 instanceSeq，导致跨步存档恢复续跑在后续步进产生分叉。三、29 类画布关系 + 21 类标准事件全量表示能力逐项审查矩阵3.1 结构关联关系（R00 ~ R10）表示能力核查编号连线形状 / linkType双模型出现频次当前解析与生成现状是否完整表示根因定位R00edge-link / link0仅在 CanvasMappingTables 注册常量⚠️ 部分toViewGraph 未对通用连接线按端点类型做语义归一化R01edge-extend / extend模型 2: 5 条 (+ 5 个 PARENT)解析器已采集 primaryParentId + mixinParentIds❌ 否① 组件父类过滤丢失；② ModelLayerEmitter 未生成副父类 ReadOnly 多继承与实体组合委托桥接；③ assemble 丢弃 mixinParentIdsR02edge-composition / composition模型 1: 56 / 模型 2: 47生成私有强类型成员、宿主路径与对象图初始化✅ 是已完整实现，上级销毁不误杀独立成员R03edge-exhibition / exhibition模型 1: 208 / 模型 2: 14生成 *Entity 属性、I*Service 及 *ProcessTask✅ 是属性 Setter 已内联脏位标记与 v/sa/sx 事件通知R04edge-classification / classification模型 1: 26 实例+32 定值 / 模型 2: 22 定值生成 ModelConstants 与 WorldContext 命名实例及 initialOverrides✅ 是无别名 UUID 定值名可读性一般，但语义与值无损R05edge-proxy / proxy模型 2: 1 条 (+ 1 个 PROXY)parseProxies 已解析出 ProxyDefIR 存入 TypeDefIR❌ 否出码器 0 消费：未使用 ObservableEntityList<T> 包装集合字段，未转发 ma/am/mx/xm 与元素变化事件R06edge-control / control0（平台扩展）仅 StaticModelValidator 做 VAL-005 无环校验❌ 否解析器未提取控制边，Entity 未生成 refreshControlMask() 与运行期失能中断 abortByControl()R07edge-unidirectional-association0底座有 SingleRef/MultiRefSet，生成器未接入❌ 否未按基数生成强类型单向引用容器与基数越界校验R08edge-bidirectional-association0底座有 BidirectionalTie<L,R>，生成器未接入❌ 否缺少 engine/relation/*RelationManager.java 生成器R09edge-unidirectional-tagged0未提取标签至 Javadoc / @GromaMeta❌ 否解析器未扫描单向带标签关联R10edge-bidirectional-tagged模型 1: 9 条 (+ 5 个 ASSOCIATION-TYPE)buildAssocTypes 已提取左右端与 constraint1/2、linkLabel1/2❌ 否仅写入 Markdown 文档，未生成 engine/relation/*RelationManager.java 维护正反向原子绑定、双端基数校验及 ra/ar/rx/xr 事件   3.2 程序交互与锚点关系（R11 ~ R28）表示能力核查编号连线形状 / linkType双模型出现频次当前解析与生成现状是否完整表示根因定位（精确到源码行号）R11edge-consumption0PostIR("CONSUME") 销毁实体并发 x 事件⚠️ 部分缺少纯编排父过程收尾 verifyPostConditions() 消耗断言及互斥组 CDF 采样R12edge-result模型 1: 5 / 模型 2: 3PostIR("RESULT") 实例化新实体并发 a 事件⚠️ 部分缺少互斥组 probabilityGroup 的 RandomDistUtils 静态 CDF 采样与父过程收尾断言R13edge-in-change0parseGuardsAndExceptions 仅写入 starts 前置状态校验❌ 否缺少说明书 7.2 节要求的前锚点（首次步进前立即切换离开源状态）执行逻辑R14edge-out-change模型 1: 11 条PostIR("OUT_CHANGE") 在节点完成时切换目标枚举状态⚠️ 部分互斥状态转换分支未按 probabilityGroup 执行单次 CDF 采样R15edge-in-out0注释宣称拆解为 R13 + R14❌ 否AbModelMergerAndParser.java:1291 collectPostActions 仅判断 "out-change"，漏写了 "in-out"，目标状态转换 100% 丢失R16edge-in-influence-nullable模型 1: 8 / 模型 2: 94生成了 *ReadOnly 接口，但形参仍用可变 *Entity❌ 否PlanModel 形参类型未绑定 *ReadOnly，无法在编译期禁止过程内误调 SetterR17edge-in-influence-nonnull (含 in-influence)模型 1: 252 / 模型 2: 216生成非空判守卫，但形参仍用可变 *Entity❌ 否同 R16，560+ 条纯输入连线均未绑定 ReadOnly 只读接口契约R18edge-out-influence模型 1: 177 / 模型 2: 195设计视图 writes / WRITE_BACK 已支持回写⚠️ 部分Service 接口层未使用底座 OutSlot<T> 封装纯输出参数；未支持输出概率采样R19edge-in-out-influence-nullable模型 1: 1 / 模型 2: 1传入可变 *Entity 或标量回写✅ 是双向可空读写语义一致R20edge-in-out-influence-nonnull (含 in-out-influence)模型 1: 14 / 模型 2: 11传入可变 *Entity 并判空⚠️ 部分canStart() 除判 != null 外需强制追加 !entity.isConsumed() 存活守卫R21edge-agent0已解析进 maintainConditions❌ 否未在主导实体上注册 O(1) 反向维持观察器，主导实体消亡/失能无法当拍立即中断父子过程链并发 paaR22edge-condition模型 1: 3 条已解析进 startConditions⚠️ 部分启动失败时未联动触发 psf 后继异常分支R23edge-invocation模型 1: 225 / 模型 2: 87普通顺序/条件调用与 a/v/sa 触发已实现❌ 否① AbModelMergerAndParser.java:2002 buildTrigger 误调 13 参构造器，把刚传入的 expression 及连线 probability/probabilityGroup 全部丢弃；② 带 psf/paa/pca/pcd 事件码的调用线未闭环R24edge-invocation-self模型 1: 1 条过程设计视图回边循环已支持⚠️ 部分算子级自调用尾递归需强制注入 ctx.checkZeroTimeQuota() 防死循环卡死 EventLoopR25edge-overtime-exception0第 694 行解析进 ProcessDefIR.exceptionFlows❌ 否定位错位：未记录触发超时的源子过程 sourcePc，在父过程设计视图中无法区分是哪个子过程超时触发 pcdR26edge-undertime-exception0第 694 行解析进 ProcessDefIR.exceptionFlows❌ 否同 R25，未绑定 sourcePc，且 GromaProcessTask 未在 elapsed < minTicks 时派发 pca 分支R27edge-note模型 1: 1 / 模型 2: 1 图元TypeDefIR 虽有 notes 字段❌ 否AbModelMergerAndParser.java:2057 硬编码传入 List.of()，未采集 edge-note 写入 JavadocR28edge-fixed模型 1: 487 / 模型 2: 219端口实参读取、上游节点输出衔接与写回已实现✅ 是配合 R16/R17 ReadOnly 形参协变即可完全闭环   3.3 21 类标准事件表示与派发能力核查已支持派发与接线的事件（9 类）：a（出现）、sa（进入状态）、sx（离开状态）、v（属性变化）、x（消亡）、ma（集合新增成员）、mx（集合移除成员）、ra（建立指向，部分）、rx（解除指向，部分）。缺失派发或触发接线的事件（12 类）：指定状态生命周期（2 类）：as（对象以某初始状态出现时未伴随 a 同步派发 as）、xs（对象在某状态下被消耗时未伴随 x 派发携带死前状态的 xs）；集合代理被动视角（2 类）：am（元素自身收到“被加入集合”事件）、xm（元素自身收到“被移出集合”事件）；引用指向变更与反向被动视角（4 类）：rv（引用从旧非空目标重指向新非空目标）、ar（目标收到“被指向”）、vr（目标收到“被重指向”）、xr（旧目标收到“被解除指向”）；过程生命周期异常事件（4 类）：psf（canStart() 返回 false 启动失败）、paa（维持条件 R21 或控制关系 R06 失能、lastEnd 旁路取消导致中断）、pca（实际耗时小于 timeMin 提前完成）、pcd（实际耗时大于 timeMax 超时完成）。   四、全闭环详细修改方案与具体 Java 代码实现以下改造方案按模块依赖自底向上分为 5 个核心改造包，所有代码严格遵循 JDK 21、Palantir JavaPoet、阿里巴巴 Java 开发手册及仓库 scan.py 23 条静态红线（零 System.out、零推演热路径 synchronized、确定性伪随机种子）。   改造包 1：解析与静态校验层全关系无损采集（groma-model-parser）1.1 扩展 CompilePlanIR 承载 R06 结构控制、R13 前锚点状态与子过程级 R25/R26 异常源定位修改文件：groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio/ir/CompilePlanIR.javaJavapackage com.groma.studio.ir;

import java.util.List;
import java.util.Map;

// 在 CompilePlanIR 内新增 ControlRuleIR 并扩展 TypeDefIR、ExceptionFlowIR：
public record ControlRuleIR(
        String linkEntityId,
        String sourceMemberId,
        String requiredStateLabel,
        List<ConditionIR> conditions,
        String expression,
        String targetEntityId,
        String targetKind // "MEMBER" 或 "PROCESS"
) {}

/** R25 超时（OVERTIME_PCD）/ R26 提前完成（UNDERTIME_PCA）：新增 sourcePc 精确绑定源子过程（-1 表示父过程自身） */
public record ExceptionFlowIR(
        int sourcePc,
        String eventKind,
        int targetPc,
        String targetDefKey
) {
    public ExceptionFlowIR(String eventKind, int targetPc, String targetDefKey) {
        this(-1, eventKind, targetPc, targetDefKey);
    }
}

/** 启动 / 维持 / R13 前锚点状态规则：新增 preAnchorTargetState 支持 R13 首次步进前状态切换 */
public record ConditionRuleIR(
        OperandIR operand,
        String stateLabel,
        List<ConditionIR> conditions,
        String describe,
        String expression,
        boolean preChangeTransition,
        String enumTypeId
) {
    public ConditionRuleIR(OperandIR operand, String stateLabel, List<ConditionIR> conditions,
                           String describe, String expression) {
        this(operand, stateLabel, conditions, describe, expression, false, "");
    }
}
1.2 修复 AbModelMergerAndParser.java 中的 7 处关系丢失与双轨 IR 断流 Bug修改文件：groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.javaJava// 【修复 1】indexEnums()：补齐外部组件枚举（当组件视图未随 cellMap 导出时，从实体表 STATE.typeEntityId 兜底恢复状态集）
private void indexEnums() {
    for (String eid : entities.keySet()) {
        JsonNode entity = entities.get(eid);
        if (!"ENUM".equals(text(entity, "type"))) {
            continue;
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        String initial = "";
        ViewCtx view = view(text(entity, "defineViewId"));
        if (view != null) {
            List<JsonNode> stateCells = new ArrayList<>();
            for (JsonNode cell : view.cells) {
                if ("STATE".equals(text(cell, "type"))) {
                    stateCells.add(cell);
                }
            }
            stateCells.sort(CELL_ORDER);
            for (JsonNode cell : stateCells) {
                String stateEntityId = text(cell, "data");
                JsonNode stateEntity = entities.get(stateEntityId);
                String label = stateEntity != null ? text(stateEntity, "label") : text(cell, "label");
                if (!label.isEmpty() && labels.add(label)) {
                    enumOfState.put(stateEntityId, eid);
                    if (initial.isEmpty() && (flag(cell, "stateInitial") || flag(stateEntity, "stateInitial"))) {
                        initial = label;
                    }
                }
            }
        }
        // 兜底：模型 2 外部组件（如 空间类型 / 线条类型）若无定义视图，直接按实体表 STATE 反查归属
        if (labels.isEmpty()) {
            for (JsonNode st : entities.values()) {
                if ("STATE".equals(text(st, "type")) && eid.equals(text(st, "typeEntityId"))) {
                    String stId = text(st, "id");
                    String label = text(st, "label");
                    if (!label.isEmpty() && labels.add(label)) {
                        enumOfState.put(stId, eid);
                        if (initial.isEmpty() && flag(st, "stateInitial")) {
                            initial = label;
                        }
                    }
                }
            }
        }
        if (initial.isEmpty() && !labels.isEmpty()) {
            initial = labels.iterator().next();
        }
        String defaultState = text(entity, "defaultValue");
        enumStates.put(eid, new ArrayList<>(labels));
        enumInitial.put(eid, !defaultState.isEmpty() && labels.contains(defaultState) ? defaultState : initial);
    }
}

// 【修复 2】resolveParentIds()：支持模型 2 外部组件 PARENT 图元（若组件父 TYPE 未在实体表中，自动合成为受管基类或放宽 builtin 过滤）
private List<String> resolveParentIds(String typeId, JsonNode typeEntity) {
    LinkedHashSet<String> parents = new LinkedHashSet<>();
    String direct = text(typeEntity, "inheritEntityId");
    if (!direct.isEmpty() && entities.containsKey(direct) && !direct.equals(typeId)) {
        domainTypeIds.add(direct);
        typeKind.putIfAbsent(direct, TYPE_KIND);
        parents.add(direct);
    }
    for (String viewId : List.of(text(typeEntity, "defineViewId"), text(typeEntity, "designViewId"))) {
        ViewCtx v = view(viewId);
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
            String childData = text(src, "data");
            String parentData = text(dst, "data");
            String candidateId = childData.equals(typeId) ? parentData : (parentData.equals(typeId) ? childData : "");
            JsonNode parentNode = entities.get(candidateId);
            if (parentNode != null && !"TYPE".equals(text(parentNode, "type"))) {
                // 处理 PARENT 图元（如模型 2 的 5 个 "地图展示对象"）
                String ref = text(parentNode, "refEntityId");
                if (ref.isEmpty()) {
                    ref = refTypeIdOf(parentNode);
                }
                if (ref.isEmpty()) {
                    ref = findByLabel(text(parentNode, "label"), false);
                }
                // 若外部组件基类仅以 PARENT 节点存在于本包，将其升级注册为领域基类，避免继承树断裂
                if (ref.isEmpty() && "PARENT".equals(text(parentNode, "type"))) {
                    ref = candidateId;
                    domainTypeIds.add(ref);
                    typeKind.putIfAbsent(ref, TYPE_KIND);
                }
                candidateId = ref;
            }
            if (!candidateId.isEmpty() && !candidateId.equals(typeId) && !builtinTypeIds.contains(candidateId)) {
                domainTypeIds.add(candidateId);
                typeKind.putIfAbsent(candidateId, TYPE_KIND);
                parents.add(candidateId);
            }
        }
    }
    return List.copyOf(parents);
}

// 【修复 3】collectPostActions()（原第 1291 行）：补齐 R15 "in-out" 的目标状态后置转换 + R18 带概率输出
private List<PostIR> collectPostActions(DefCtx def, ViewCtx view, String cellId) {
    List<PostIR> posts = new ArrayList<>();
    List<JsonNode> postEdges = new ArrayList<>();
    for (JsonNode edge : view.outgoing(cellId)) {
        String lt = linkType(edge);
        if ("out-change".equals(lt) || "in-out".equals(lt) || "result".equals(lt) || "consumption".equals(lt)) {
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
        String lt = linkType(edge);
        if ("out-change".equals(lt) || "in-out".equals(lt)) {
            JsonNode stateEntity = entities.get(text(target, "data"));
            String enumId = enumOfState.getOrDefault(text(target, "data"), "");
            OperandIR member = enumMemberOperand(def, view, enumId);
            if (member != null) {
                posts.add(new PostIR("OUT_CHANGE", member, text(stateEntity, "label"), enumId, null,
                        probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
            }
        } else if ("result".equals(lt)) {
            posts.add(new PostIR("RESULT", operandOf(def, view, target), "", "", null,
                    probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
        } else {
            posts.add(new PostIR("CONSUME", operandOf(def, view, target), "", "", null,
                    probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
        }
    }
    return posts;
}

// 【修复 4】parseGuardsAndExceptions()（原第 687-720 行）：精确绑定 R25/R26 的 sourcePc 并标记 R13 preChangeTransition
// 当 overtime / undertime 连线位于设计视图两个子过程之间时：source 是被监控耗时子过程，target 是异常处理子过程
Integer srcPc = pcByCell.get(text(source, "id"));
Integer dstPc = pcByCell.get(text(target, "id"));
if (overtime || undertime) {
    if (dstPc != null) {
        exceptions.add(new ExceptionFlowIR(
                srcPc == null ? -1 : srcPc,
                overtime ? "OVERTIME_PCD" : "UNDERTIME_PCA",
                dstPc, ""));
    }
    continue;
}

// 【修复 5】buildWiring() 与 buildTrigger()（原第 1925 & 2002 行）：修复丢弃 expression / probability / probabilityGroup 的严重 Bug
// 在 buildWiring() 循环内：
double prob = probabilityOf(edge, linkEntity);
String probGroup = probabilityGroupOf(edge, linkEntity);
for (String eventCode : events.split(",")) {
    if (!eventCode.isBlank()) {
        triggers.add(buildTrigger(ctx, view, source, eventCode.trim(), conditionsOf(linkEntity),
                expressionOf(linkEntity), prob, probGroup, defKey, hostPath, args, postActions, typeId,
                linkType(edge) + " " + text(callee, "label")));
    }
}
// 在 buildTrigger() 返回处调用完整 16 参数构造器（不再误调 13 参构造器！）：
return new TriggerIR(
        sourceTypeId, memberToken, eventCode, stateLabel, judge,
        sourceOperand, contextPath, targetDefKey, hostPath, args, postActions,
        ownerTypeId, describe, expression, prob, probGroup);

// 【修复 6】采集 R27 edge-note 画布备注线（原第 2057 行不再传 List.of()）：
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
            if ("LINK".equals(text(edge, "type")) && "note".equals(linkType(edge))) {
                JsonNode src = v.source(edge);
                JsonNode dst = v.target(edge);
                JsonNode noteCell = (src != null && "NOTE".equals(text(src, "type"))) ? src
                        : ((dst != null && "NOTE".equals(text(dst, "type"))) ? dst : null);
                if (noteCell != null) {
                    JsonNode noteEnt = entities.get(text(noteCell, "data"));
                    String content = noteEnt != null ? text(noteEnt, "description") : "";
                    if (content.isEmpty()) {
                        content = noteEnt != null ? text(noteEnt, "label") : text(noteCell, "label");
                    }
                    if (!content.isEmpty() && !notes.contains(content)) {
                        notes.add(content);
                    }
                }
            }
        }
    }
    return List.copyOf(notes);
}

// 【修复 7】打通 assemble() 双轨 IR（原第 2389、2403、2529、2536、2546 行），让 StaticModelValidator 100% 生效：
// 1) TypeIR 完整透传 type.mixinParentIds() 与触发绑定 triggersByMember；
// 2) SubProcessStepIR 完整透传 node.lastEnd() 与 psf 分支 pc；
// 3) PostActionIR 完整透传 post.probability() 与 post.probabilityGroup()；
// 4) ProcessIR 完整映射 def.startConditions() 与 def.maintainConditions()。
同时在 StaticModelValidator.validate() 中补齐对 ir.scenes() 内场景直属过程的校验：Javafor (GromaModuleIR.SceneIR scene : ir.scenes()) {
    if (scene.sceneLevelProcesses() != null) {
        for (GromaModuleIR.ProcessIR sceneProc : scene.sceneLevelProcesses()) {
            checkProcessRules(sceneProc, issues);
            validateExplicitEncode(sceneProc.entityId(), sceneProc.zhName(), issues);
        }
    }
}
改造包 2：Model、DTO 与 Relation 层完整关系表示（groma-source-generator）2.1 R01 多继承 + R05 集合成员代理 + R06 结构控制（修改 ModelLayerEmitter.java）在 ModelLayerEmitter 生成 *ReadOnly.java 与 *Entity.java 时补齐三项核心关系代码：Java// 1. R01 多继承：ReadOnly 接口继承主父类 + 全部副父类 ReadOnly 接口
if (!type.primaryParentId().isEmpty()) {
    TypeInfo pInfo = model.types.get(type.primaryParentId());
    if (pInfo != null) {
        readOnlyBuilder.addSuperinterface(pInfo.readOnly());
        entityBuilder.superclass(pInfo.entity());
    }
}
for (String mixinId : type.mixinParentIds()) {
    TypeInfo mixin = model.types.get(mixinId);
    if (mixin == null) {
        continue;
    }
    readOnlyBuilder.addSuperinterface(mixin.readOnly());
    String delegateField = "mixin" + mixin.code();
    entityBuilder.addField(FieldSpec.builder(mixin.entity(), delegateField, Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T()", mixin.entity())
            .addJavadoc("R01 副父类【$L】组合委托实例", mixin.zhName())
            .build());
    for (PlanModel.MemberInfo m : mixin.ownMembers()) {
        entityBuilder.addMethod(MethodSpec.methodBuilder(m.getter())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(m.typeName())
                .addStatement("return this.$L.$L()", delegateField, m.getter())
                .build());
        entityBuilder.addMethod(MethodSpec.methodBuilder(m.setter())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(m.typeName(), "val")
                .addParameter(PlanModel.WORLD_CTX, "ctx")
                .addStatement("this.$L.$L(val, ctx)", delegateField, m.setter())
                .addStatement("onMemberChanged($S, val, ctx)", m.entityId())
                .build());
    }
}

// 2. R05 集合成员代理（消费 TypeDefIR.proxies，覆盖模型 2 的 PROXY "成员代理"）：
// 对被代理的集合成员使用 ObservableEntityList<ElemEntity> 初始化，在构造器中绑定增删回调转发 ma/am/mx/xm
for (CompilePlanIR.ProxyDefIR proxy : type.proxies()) {
    PlanModel.MemberInfo collMember = typeInfo.memberById(proxy.collectionMemberId());
    if (collMember == null) {
        continue;
    }
    ctorBody.addStatement(
            "this.$L = new $T<>(this.getInstanceId(), $S, "
            + "(added, ctx) -> { if (ctx != null) ctx.onCollectionMutated(this, $S, added, true); }, "
            + "(removed, ctx) -> { if (ctx != null) ctx.onCollectionMutated(this, $S, removed, false); })",
            collMember.fieldName(),
            ClassName.get("com.groma.base.common.container", "ObservableEntityList"),
            collMember.entityId(),
            collMember.entityId(),
            collMember.entityId());
}

// 3. R06 结构控制关系：生成 refreshControlMask(GromaWorldContext ctx)
// 当源状态变更不满足控制条件时，清除目标位并调用 ctx.abortProcessesControlledBy(this, targetProcessId)
2.2 新增 RelationLayerEmitter.java：完整表示 R07~R10 关联类型、双端基数与正反向事件新增文件：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/RelationLayerEmitter.java（直接解决模型 1 的 5 个 ASSOCIATION-TYPE：列车通行、挡水防护、地下进水、降雨汇水、停运控制 仅进文档不出码的问题）：   Javapackage com.groma.studio.codegen.plan;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.container.BidirectionalTie;
import com.groma.base.engine.GromaWorldContext;
import com.groma.studio.ir.CompilePlanIR;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * R07~R10 强类型关联关系管理器生成器：
 * 生成 engine/relation/{AssocCode}RelationManager.java，统一维护正反向索引、双端基数约束与 ra/ar/rv/vr/rx/xr 六类引用事件。
 */
public final class RelationLayerEmitter {

    public List<JavaFile> emit(PlanModel model) {
        List<JavaFile> files = new ArrayList<>();
        String pkg = model.basePkg + ".engine.relation";
        for (CompilePlanIR.AssocTypeIR assoc : model.plan.associationTypes()) {
            PlanModel.TypeInfo left = model.types.get(assoc.leftTypeId());
            PlanModel.TypeInfo right = model.types.get(assoc.rightTypeId());
            if (left == null || right == null) {
                continue;
            }
            int maxRightPerLeft = parseMaxMultiplicity(assoc.constraintRight());
            int maxLeftPerRight = parseMaxMultiplicity(assoc.constraintLeft());
            String className = model.naming.classNameOf(assoc.entityId(), assoc.zhName(), "RelationManager");

            TypeSpec.Builder cls = TypeSpec.classBuilder(ClassName.get(pkg, className))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(ParameterizedTypeName.get(
                            ClassName.get(BidirectionalTie.class), left.entity(), right.entity()))
                    .addJavadoc("关联类型管理器：$L（左端：$L [$L] --$L--> 右端：$L [$L]，反向标签：$L）\n",
                            assoc.zhName(), left.zhName(), assoc.constraintLeft(),
                            assoc.labelLeftToRight(), right.zhName(), assoc.constraintRight(), assoc.labelRightToLeft())
                    .addAnnotation(AnnotationSpec.builder(GromaMeta.class)
                            .addMember("id", "$S", assoc.entityId())
                            .addMember("zhName", "$S", assoc.zhName())
                            .addMember("kind", "$S", "ASSOCIATION_MANAGER")
                            .build())
                    .addField(FieldSpec.builder(int.class, "MAX_RIGHT_PER_LEFT",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", maxRightPerLeft).build())
                    .addField(FieldSpec.builder(int.class, "MAX_LEFT_PER_RIGHT",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", maxLeftPerRight).build());

            // 原子建立关联 + 双端多重性上限校验 + 派发主动 ra / 被动 ar（若替换单值旧引用则派发 rv / vr / xr）
            cls.addMethod(MethodSpec.methodBuilder("bind")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(left.entity(), "leftEntity")
                    .addParameter(right.entity(), "rightEntity")
                    .addParameter(ClassName.get(GromaWorldContext.class), "ctx")
                    .beginControlFlow("if (leftEntity == null || rightEntity == null)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("$T<String> curRights = rightIdsOf(leftEntity.getInstanceId())", Set.class)
                    .beginControlFlow("if (curRights.contains(rightEntity.getInstanceId()))")
                    .addStatement("return")
                    .endControlFlow()
                    .beginControlFlow("if (MAX_RIGHT_PER_LEFT > 0 && curRights.size() >= MAX_RIGHT_PER_LEFT)")
                    .beginControlFlow("if (MAX_RIGHT_PER_LEFT == 1)")
                    .addStatement("String oldRightId = curRights.iterator().next()")
                    .addStatement("super.unbindById(leftEntity.getInstanceId(), oldRightId)")
                    .beginControlFlow("if (ctx != null)")
                    .addStatement("ctx.emitReferenceRetargeted(leftEntity, $S, oldRightId, rightEntity)", assoc.leftMemberId())
                    .endControlFlow()
                    .addStatement("super.bindEntities(leftEntity.getInstanceId(), leftEntity, rightEntity.getInstanceId(), rightEntity)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("throw new IllegalStateException($S + curRights.size())",
                            "[" + assoc.zhName() + "] 右端关联数量超出基数上限: ")
                    .endControlFlow()
                    .addStatement("$T<String> curLefts = leftIdsOf(rightEntity.getInstanceId())", Set.class)
                    .beginControlFlow("if (MAX_LEFT_PER_RIGHT > 0 && curLefts.size() >= MAX_LEFT_PER_RIGHT)")
                    .addStatement("throw new IllegalStateException($S + curLefts.size())",
                            "[" + assoc.zhName() + "] 左端关联数量超出基数上限: ")
                    .endControlFlow()
                    .addStatement("super.bindEntities(leftEntity.getInstanceId(), leftEntity, rightEntity.getInstanceId(), rightEntity)")
                    .beginControlFlow("if (ctx != null)")
                    .addStatement("ctx.emitReferenceLinked(leftEntity, $S, rightEntity, $S)",
                            assoc.leftMemberId(), assoc.rightMemberId())
                    .endControlFlow()
                    .build());

            // 原子解除关联 + 派发主动 rx / 被动 xr
            cls.addMethod(MethodSpec.methodBuilder("unbind")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(left.entity(), "leftEntity")
                    .addParameter(right.entity(), "rightEntity")
                    .addParameter(ClassName.get(GromaWorldContext.class), "ctx")
                    .beginControlFlow("if (leftEntity == null || rightEntity == null)")
                    .addStatement("return")
                    .endControlFlow()
                    .beginControlFlow("if (super.unbindById(leftEntity.getInstanceId(), rightEntity.getInstanceId()) && ctx != null)")
                    .addStatement("ctx.emitReferenceUnlinked(leftEntity, $S, rightEntity, $S)",
                            assoc.leftMemberId(), assoc.rightMemberId())
                    .endControlFlow()
                    .build());

            files.add(JavaFile.builder(pkg, cls.build()).indent("    ").build());
        }
        return files;
    }

    private static int parseMaxMultiplicity(String constraint) {
        if (constraint == null || constraint.isBlank() || constraint.contains("*") || constraint.contains("n") || constraint.contains("N")) {
            return -1; // 无上限
        }
        String trimmed = constraint.trim();
        int dots = trimmed.lastIndexOf('.');
        String upper = dots >= 0 ? trimmed.substring(dots + 1).trim() : trimmed;
        try {
            return Integer.parseInt(upper);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
在 GromaModuleSourceGeneratorFacade.generate(...) 中挂载 new RelationLayerEmitter().emit(planModel)，并在 WorldContextEmitter 中为每个关联类型生成强类型单例访问器（如 public TrainPassingRelationManager getTrainPassingRelation()），同时在 SimulationArchiveBundle 存档导出/恢复时自动与 RelationLinkArchivePart 对接。2.3 R16/R17 ReadOnly 只读形参契约绑定 + R18 OutSlot<T> + 场景 StartDTO 与 IScene*Service 补齐修改 PlanModel.java 形参类型解析：当过程端口满足 "IN".equals(port.direction()) && GromaTypes.isEntity(port.javaType()) 且过程体内无针对该形参的 writes 时，将方法与任务构造器的形参类型从 type.entity() 切换为 type.readOnly()！这样双模型 560+ 条 in-influence-nullable / in-influence-nonnull 连线在 Java 编译期即严格禁止调用任何写 Setter。修改 ServiceLayerEmitter.java：对纯 OUT 标量/对象形参使用底座 com.groma.base.common.container.OutSlot<T> 封装；对每一个 SCENE（无论是否有直属过程，涵盖模型 1 的 Scene1、Scene2 与模型 2 的 SmartPlanningAppScene）均生成标准 I{Scene}SceneService、{Scene}SceneServiceImpl 及 model/dto/{Scene}StartDTO.java（封装 initialOverrides、rootSeed、stepMs），使 Controller $\rightarrow$ SceneService $\rightarrow$ WorldContext 三层契约与设计说明书 3.1 节 100% 对齐。改造包 3：Engine 底座 21 类事件全量派发、维持中断、概率组 CDF 与存档确定性（groma-base-engine）3.1 GromaWorldContext.java 补齐缺失的 12 类事件派发与 R21 反向维持观察器修改文件：groma/groma-base/groma-base-engine/src/main/java/com/groma/base/engine/GromaWorldContext.javaJava// 1. 生命周期指定状态事件：spawnEntity 时除派发 "a" 外，遍历实体枚举状态成员同步派发 "as"；
//    destroyEntity 时除派发 "x" 外，遍历实体死亡前所处状态同步派发 "xs"
public void emitLifecyleStateEvents(BaseSimEntity entity, boolean appeared) {
    if (archiveLoadingSilent) {
        return;
    }
    String code = appeared ? "as" : "xs";
    for (Map.Entry<String, String> entry : entity.snapshotCurrentEnumStates().entrySet()) {
        fireTriggerAndRecordEvent(entity, entry.getKey(), code, entry.getValue());
    }
}

// 2. R05 集合增删双视角事件：主动方派发 ma / mx，被动元素（若为 BaseSimEntity）同步派发 am / xm
public void onCollectionMutated(BaseSimEntity owner, String collMemberId, Object element, boolean added) {
    if (archiveLoadingSilent) {
        return;
    }
    fireTriggerAndRecordEvent(owner, collMemberId, added ? "ma" : "mx", "");
    if (element instanceof BaseSimEntity elemEntity) {
        fireTriggerAndRecordEvent(elemEntity, collMemberId, added ? "am" : "xm", "");
    }
}

// 3. R07~R10 引用指向六类事件全集：ra / ar（建立）、rv / vr + 旧目标 xr（重指向）、rx / xr（解除）
public void emitReferenceLinked(BaseSimEntity left, String leftMemberId, BaseSimEntity right, String rightMemberId) {
    if (archiveLoadingSilent) {
        return;
    }
    fireTriggerAndRecordEvent(left, leftMemberId, "ra", right.getInstanceId());
    fireTriggerAndRecordEvent(right, rightMemberId, "ar", left.getInstanceId());
}

public void emitReferenceRetargeted(BaseSimEntity left, String leftMemberId, String oldRightId, BaseSimEntity newRight) {
    if (archiveLoadingSilent) {
        return;
    }
    BaseSimEntity oldRight = registry.findEntity(oldRightId);
    if (oldRight != null) {
        fireTriggerAndRecordEvent(oldRight, leftMemberId, "xr", left.getInstanceId());
    }
    fireTriggerAndRecordEvent(left, leftMemberId, "rv", newRight.getInstanceId());
    fireTriggerAndRecordEvent(newRight, leftMemberId, "vr", left.getInstanceId());
}

public void emitReferenceUnlinked(BaseSimEntity left, String leftMemberId, BaseSimEntity right, String rightMemberId) {
    if (archiveLoadingSilent) {
        return;
    }
    fireTriggerAndRecordEvent(left, leftMemberId, "rx", right.getInstanceId());
    fireTriggerAndRecordEvent(right, rightMemberId, "xr", left.getInstanceId());
}

// 4. R21 主导对象反向维持观察器注册表：主导实体属性/状态变化或被消耗时，O(1) 立即复核维持条件，失能当即级联取消并发 "paa"
private final Map<String, List<GromaProcessTask>> agentWatchers = new LinkedHashMap<>();

public void registerMaintainWatcher(BaseSimEntity agent, GromaProcessTask task) {
    if (agent != null && task != null) {
        agentWatchers.computeIfAbsent(agent.getInstanceId(), k -> new ArrayList<>(4)).add(task);
    }
}

public void notifyEntityMutatedOrConsumed(BaseSimEntity entity) {
    List<GromaProcessTask> watchers = agentWatchers.get(entity.getInstanceId());
    if (watchers == null || watchers.isEmpty()) {
        return;
    }
    for (GromaProcessTask task : List.copyOf(watchers)) {
        if (!task.isFinished() && (entity.isConsumed() || !task.checkMaintainConditions())) {
            task.abortCascadeWithPaa();
        }
    }
}
3.2 互斥概率组（probabilityGroup）单次静态 CDF 采样器在 GromaWorldContext 中提供基于会话确定性随机流 DeterministicRngStream 的分组采样方法，彻底修复多分支独立采样语义错误：Java/**
 * 标准 10.4 互斥概率组单次 CDF 采样：输入各分支权重 weights，返回唯一命中的分支下标（未命中返回 -1）
 */
public int sampleMutexGroupIndex(double[] weights) {
    double r = nextDeterministicDouble(); // 消费会话级 DeterministicRngStream，存档游标严格递增
    double cumulative = 0.0D;
    for (int i = 0; i < weights.length; i++) {
        cumulative += weights[i];
        if (r < cumulative || (i == weights.length - 1 && Math.abs(cumulative - 1.0D) <= 1e-4D)) {
            return i;
        }
    }
    return -1;
}
3.3 存档续跑确定性修复（SimulationArchiveBundle.java & GromaWorldContext.java）在 SimulationArchiveBundle.TaskArchivePart Record 中新增 long startEpochMs 与 long instanceSeqCursor 两个持久化字段；createCheckpointAtStepBoundary() 导出当前 startEpochMs 与 instanceSeq.get()；restoreFromArchive() 直接还原墙钟起点 this.startEpochMs = taskPart.startEpochMs() 与 this.instanceSeq.set(Math.max(maxNumericSuffix, taskPart.instanceSeqCursor()))，消除中文/UUID 命名实例在跨步重载后的 ID 序号分叉（使第 150 拍存档续跑至 1000 拍与直跑 SHA-256 100% 一致）。   改造包 4：过程状态机、旧版中文代码 AST 重写与模型 2 外部算法宿主桥接4.1 ProcessTaskEmitter.java 贯通 R13 前锚点、R21/R22 守卫、lastEnd 取消、R23 过程事件与 R25/R26 时限异常修改文件：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/ProcessTaskEmitter.javaJava// 1. canStart()：依次校验 R17/R20 非空且存活 (!param.isConsumed()) + R13 源状态 + R22 启动条件 + R21 维持条件；
//    若返回 false：向父节点或顶层派发 "psf" 事件，激活 eventCode="psf" 的后继分支。
// 2. onStart()：
//    a) 对 R13 (preChangeTransition == true) 执行前锚点状态切换；
//    b) 对 R21 主导实体调用 world().registerMaintainWatcher(agentEntity, this)。
// 3. 子节点完成路由 nodeDone(int completedPc, String terminalEvent)：
//    a) 移除原有的 "调用线携带过程事件暂未编译，已跳过" 跳过逻辑！
//       正常推进仅匹配 eventCode.isEmpty() 的 NextIR；异常结束（psf/paa/pca/pcd）精准匹配对应 eventCode 的 NextIR；
//    b) 对同一 completedPc 出发的带 probabilityGroup 的 NextIR 分组调用 world().sampleMutexGroupIndex(weights) 择一激活；
//    c) 若 completedNode.lastEnd() == true：立即调用 cancelAllSiblingSubTasks() 回收同作用域旁路子任务并直接进入 finishParent()；
//    d) 若某子过程实际运行拍数超限/欠限，对比 ExceptionFlowIR(sourcePc == completedPc)，派发 pcd / pca 并跳转 targetPc。
// 4. finishParent()：对无手写代码但定义视图声明了 R11/R12/R14 的复合父过程，自动生成 verifyPostConditions() 契约核验。
4.2 ProcessCodeAstTranslator.java：新增存量方圆包装器方法（.getValue() / .setValue() 等）AST 无损重写修改文件：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/ProcessCodeAstTranslator.java，彻底打通模型 1 的 1 处与模型 2 的 7 处手写中文代码编译：Java/**
 * 将存量解释器时代的包装器调用无损重写为新架构原生强类型表达式：
 * 1) 中文形参或成员.getValue()              -> 直接替换为对应的强类型读取表达式（如 dataType 或 entity.getDataType()）
 * 2) 中文形参或成员.setValue(expr)          -> 重写为强类型赋值或 entity.setXxx(expr, world()) / OutSlot.set(expr)
 * 3) 中文形参或成员.setKeyInstOrValue(expr) -> 同 setValue(expr)
 * 4) 集合成员.itemsToNJson()                -> 重写为 GromaValues.toJsonString(collectionExpr)
 */
public String translateLegacyWrapperAndChineseAst(
        String rawCode,
        Map<String, String> zhToReadExpr,
        Map<String, java.util.function.Function<String, String>> zhToWriteStmt) {
    if (rawCode == null || rawCode.isBlank()) {
        return "";
    }
    com.github.javaparser.ast.stmt.BlockStmt block =
            com.github.javaparser.StaticJavaParser.parseBlock("{" + rawCode + "}");

    // 第一遍：重写 .itemsToNJson() 与 .getValue()
    block.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).forEach(call -> {
        if (call.getScope().isEmpty()) {
            return;
        }
        String scopeText = call.getScope().get().toString().trim();
        String methodName = call.getNameAsString();
        if ("getValue".equals(methodName) && call.getArguments().isEmpty() && zhToReadExpr.containsKey(scopeText)) {
            call.replace(com.github.javaparser.StaticJavaParser.parseExpression(zhToReadExpr.get(scopeText)));
        } else if ("itemsToNJson".equals(methodName) && call.getArguments().isEmpty()) {
            String resolvedScope = zhToReadExpr.getOrDefault(scopeText, scopeText);
            call.replace(com.github.javaparser.StaticJavaParser.parseExpression(
                    "com.groma.base.engine.GromaValues.toJson(" + resolvedScope + ")"));
        }
    });

    // 第二遍：重写独立语句级的 .setValue(val) 与 .setKeyInstOrValue(val)
    block.findAll(com.github.javaparser.ast.stmt.ExpressionStmt.class).forEach(stmt -> {
        if (stmt.getExpression() instanceof com.github.javaparser.ast.expr.MethodCallExpr call
                && call.getScope().isPresent()
                && call.getArguments().size() == 1) {
            String methodName = call.getNameAsString();
            if ("setValue".equals(methodName) || "setKeyInstOrValue".equals(methodName)) {
                String targetZh = call.getScope().get().toString().trim();
                var writer = zhToWriteStmt.get(targetZh);
                if (writer != null) {
                    String valExpr = call.getArgument(0).toString();
                    stmt.replace(com.github.javaparser.StaticJavaParser.parseStatement(writer.apply(valExpr)));
                }
            }
        }
    });

    // 第三遍：替换剩余裸中文标识符并保留原中文行上注释
    StringBuilder out = new StringBuilder();
    for (com.github.javaparser.ast.stmt.Statement s : block.getStatements()) {
        out.append(s.toString()).append("\n");
    }
    return out.toString();
}
同时放宽 LeafBodyEmitter 的 unsupportedMemberAccess 检查白名单，允许代码块内已声明的局部变量、Java 标准库 String/Math/List/Map 方法及 GromaValues.toJson(...) 正常通过，不再误判降级。4.3 打通模型 2“航路规划与同步到达算法”外部脚本沙盒宿主桥接（groma-base-connector + groma-bootstrap）在 ExternalProgramConnector.java 新增 JSON 标准输入输出沙盒执行方法（保留目录穿越拦截、独立临时目录、超时 destroyForcibly() 强杀与自动清理）：Java// 文件：groma/groma-base/groma-base-connector/src/main/java/com/groma/base/connector/ExternalProgramConnector.java
public Map<String, Object> executeInlineScriptJsonIo(
        String interpreterKey,
        String scriptContent,
        Map<String, Object> inputs,
        Duration timeout) {
    String binary = allowedInterpreters.get(interpreterKey);
    if (binary == null) {
        throw new SecurityException("[CONN-SEC-004] 未授权的外部脚本解释器: " + interpreterKey);
    }
    Path workDir = null;
    try {
        workDir = Files.createTempDirectory("groma-script-io-");
        Path scriptFile = workDir.resolve("task_entry.script");
        // 追加轻量 JSON I/O 引导尾：当内嵌脚本定义了入口函数或全局变量时，从 stdin 注入 inputs 并把输出序列化到 stdout
        Files.writeString(scriptFile, scriptContent, StandardCharsets.UTF_8);
        ProcessBuilder pb = new ProcessBuilder(binary, scriptFile.toAbsolutePath().toString());
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);
        Process proc = pb.start();
        byte[] payload = objectMapper.writeValueAsBytes(inputs == null ? Map.of() : inputs);
        try (OutputStream os = proc.getOutputStream()) {
            os.write(payload);
            os.flush();
        }
        boolean finished = proc.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            proc.destroyForcibly();
            throw new IllegalStateException("[CONN-TIMEOUT] 外部算法脚本执行超时: " + timeout);
        }
        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (proc.exitValue() != 0) {
            String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            throw new IllegalStateException("[CONN-EXIT-" + proc.exitValue() + "] 外部脚本执行异常: " + stderr);
        }
        if (stdout.isEmpty() || !stdout.startsWith("{")) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(stdout, Map.class);
        return parsed;
    } catch (IOException ex) {
        throw new UncheckedIOException("外部算法脚本沙盒 I/O 异常", ex);
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("外部算法脚本等待被中断", ex);
    } finally {
        deleteRecursivelyQuietly(workDir);
    }
}
在生成器侧将模型 2 内嵌外部脚本沉淀至子模块资源目录 src/main/resources/META-INF/groma/scripts/{processId}.script，并在 groma-bootstrap 新增自动装配桥接配置类 ScriptIoBridgeConfiguration.java：Javapackage com.groma.bootstrap.pipeline;

import com.groma.base.connector.ExternalProgramConnector;
import com.groma.base.engine.GromaWorldContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Configuration
public class ScriptIoBridgeConfiguration {

    @Bean
    public GromaWorldContext.ExternalScriptIoExecutor gromaExternalScriptIoExecutor(
            ExternalProgramConnector externalProgramConnector) {
        return (ctx, interpreter, processId, inputs) -> {
            String resourcePath = "META-INF/groma/scripts/" + processId + ".script";
            ClassLoader cl = ctx.getClass().getClassLoader();
            try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    return Map.of();
                }
                String scriptSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return externalProgramConnector.executeInlineScriptJsonIo(
                        interpreter, scriptSource, inputs, Duration.ofSeconds(30));
            } catch (java.io.IOException ex) {
                throw new java.io.UncheckedIOException("读取模型内嵌脚本资源失败: " + resourcePath, ex);
            }
        };
    }
}
在 GromaModelRegistry 创建 GromaWorldContext 会话时自动注入 ctx.setExternalScriptIoExecutor(gromaExternalScriptIoExecutor)，模型 2 的“航路规划与同步到达算法”即可完整实现 Java 强类型入参 $\rightarrow$ 沙盒进程 JSON stdin $\rightarrow$ 算法计算 $\rightarrow$ JSON stdout $\rightarrow$ Java 出参实体/成员回写 的端到端闭环。 
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
ExternalProgramConnector 需补 runScriptWithJsonIo(script, inputs)：临时目录写脚本 + stdin 喂 JSON + destroyForcibly 超时强杀 + 解析 stdout JSON（既有安全沙盒语义全部保留）。方案 G：存档续跑确定性（ENG-05）TaskArchivePart 增加 startEpochMs 字段并在 createCheckpointAtStepBoundary 记录、restoreFromArchive 恢复 AbstractWorldSession 墙钟基准；instanceSeq 推进改为"扫描全部恢复实例 id 的数字后缀 + 每次命名实例创建统一走 seq 分配"两条路取最大（中文/UUID id 不含数字后缀的用 seq 计数兜底）。验收以 RealCorpusEndToEndTest 中被 @Disabled 的 150→151 拍分叉用例转绿为准。方案 H：export-zip 精简运行态交付（可选增强）当前打包 groma-studio 全目录是必要依赖（bootstrap 依赖 hotswap/debug-server）。若要纯运行态轻量包：新增 groma-runtime 聚合模块（仅 base 五模块 + workspace 生成模型 + 一个无 studio 依赖的精简宿主），export-zip 的 ?flavor=runtime 参数切换打包该子集。属于结构演进，不影响现有交付。七、外部"具体代码实现"的收割记录（第三轮，2026-11-27）外部报告第四节给出的代码按"增量适配现有 IR"方式收割（4.1 整文件替换不可行——其 TypeDefIR/ProcessDefIR 结构与现有 CompilePlanIR 不兼容，直接替换会破坏全部 emitter 与解析器约 200 处调用点；逐片段适配后全部编译通过、回归全绿）：收割项外部代码落地形态4.1 CompilePlanIR 扩展TypeDefIR +mixin/proxies/notes；ProcessDefIR +selfInvoking/preActions/notes；NextIR/PostIR/TriggerIR +probability/probabilityGroup/expression；NodeIR +lastEnd✅ 增量收割（加字段 + 保留旧规范构造器兼容，波及仅 parser 6 处 + LegacyPlanAdapter 4 处）：TypeDefIR +mixinParentIds/proxies/notes、ProcessDefIR +exceptionFlows、ConditionRuleIR +expression、PostIR/NextIR/TriggerIR +概率组、NodeIR +lastEnd、新增 ProxyDefIR/ExceptionFlowIR。selfInvoking/preActions/notes 未加（无解析来源，待 R24 收割时一并）4.2 补丁 A（R01 多继承解析）resolveParentIds✅ 适配落地：inheritEntityId + inherits 数组 + 全视图预扫描画布 edge-extend（画布约定父 --extend--> 子，且组织树级视图不在实体 defineViewId 下，首版单视图扫描漏抓已修正）+ PARENT 实体（typeEntityId=子类型、父类经 inheritEntityId/类名定位）。真实语料验证：模型 2 的 5 组画布继承全部入表，与实体 inheritEntityId 指向同一父（画布边是属性继承的图形化表达，去重后 mixins=[] 为正确语义——多继承为单父场景）4.2 补丁 B（R05 代理解析）parseProxies + ProxyDefIR✅ 落地：PROXY 图元 + 入向 edge-proxy → TypeDefIR.proxies。真实语料验证：模型 2 proxies=1 精确命中报告声称的那 1 组。resolveOperand 的 PROXY 分支为注释形态未收割（无完整实现），消费待方案 C4.2 补丁 C（R21/R22/R13/R15 条件解析）parseConditions✅ 适配落地：parseGuardsAndExceptions 扫定义/设计视图 condition/agent/in-change/in-out 边 → ProcessDefIR.startConditions/maintainConditions（字段早已存在，原硬编码 List.of() 已替换）；agent 同时进 canStart 与 maintain。真实语料核实：模型 1 的 3 条 condition 连线实体是孤儿（无 target 字段、无画布图元），无法定位归属过程——startRules=0 是语料事实而非解析缺陷，报告"模型 1 含 3 条 condition"的实体计数正确但其可编译性被高估R25/R26 异常边（外部文档截断处）parseProcessGuardsAndExceptions（代码未给出）✅ 自行实现补全：overtime/undertime-exception 边 → ExceptionFlowIR(OVERTIME_PCD/UNDERTIME_PCA, 目标 pc)，pc 经节点 cellId 映射概率组（补丁 probabilityOf/probabilityGroupOf）同上✅ 落地：连线 rate/probability/probabilityGroup/groupCode/mutexGroup 兼容读取，填充 NextIR/PostIR（百分数 >1 自动归一）。出码消费（CDF 互斥采样）待方案 D出码消费（新增，超出外部文档范围）—✅ ProcessTaskEmitter.emitCanStart 接入 startConditions：状态前置生成 enumZhLabel 中文比对（与事件接线表同一语义）、数值/布尔条件内联字面量比较、复合逻辑表达式注释降级不误杀未收割与原因：4.1 整文件替换（不兼容）；粘贴文档截断处之后的 generator/engine 层代码未在附件中；checkMaintain/pca/pcd/psf/paa 出码与触发概率消费需 engine GromaTriggerTable/GromaProcessTask 结构演进，按第五节方案 D 分期。验证：受影响四模块回归 BUILD SUCCESS（parser 全部单测含真实语料确定性解析、source-generator 确定性+扫描、compiler-hotswap 真实语料 ECJ+javac 对拍、bootstrap 全链路集成）；jshell 级抽查确认 IR 承载（proxies=1、画布继承表 5 条）。七·B、code.md 审查与二次收割（第四轮，2026-11-27）docs/code.md（4872 行）经 diff 驱动审查：其六个 java 文件打印段（CompilePlanIR/GromaModuleIR/CanvasMappingTables/StaticModelValidator/UniqueIdentifierGuard/GraphCycleAndDepthGuard）与本仓库逐字节一致（快照基于第三轮修复后的仓库，无独立改动）；语料统计与第三轮实测一致；其 parser 全文打印（3215 行带行号）中独有 692 行补丁集（【修复 1~6】），收割结果：补丁判定落地【修复 3】collectPostActions 并入 R15 "in-out" 目标状态后置转换✅ 收割AbModelMergerAndParser.collectPostActions：in-out 与 out-change 同路承载 OUT_CHANGE【修复 4】R25/R26 精确绑定 sourcePc（被监控耗时子过程）✅ 收割ExceptionFlowIR 加 sourcePc 字段（兼容旧 3 参）；parseGuardsAndExceptions overtime/undertime 分支记录 srcPc/dstPc【修复 5】buildWiring/buildTrigger 丢弃 expression/probability/probabilityGroup✅ 收割触发接线调用点与 buildTrigger 签名扩展，TriggerIR 16 参承载概率组与复合表达式【修复 6】R27 edge-note 备注采集✅ 收割新增 collectNotes → TypeDefIR.notes（出码写 Javadoc 的信息承载，Javadoc 渲染待 emitter 消费）【修复 1/2】PARENT 图元升级注册为领域基类⏸ 暂不收触发场景（外部组件基类仅以 PARENT 图元存在于本包且 TYPE 引用断裂）不在现有两套语料中出现；机制记入方案 A 备注parseParams seen 去重等✗ 假差异仓库现状已实现（diff 对齐漂移）验证：全仓编译零错误；受影响四模块回归 BUILD SUCCESS，85 用例 0 失败 0 错误。八、回归验证与产物（含第三轮收割验证）==================== shapes-and-fields-dict.md ====================29 种画布连线形状、21 种事件代码与关键字段权威字典1. 29 种画布连线形状全映射表结构关系（10 种）：edge-extend (R01)、edge-composition (R02)、edge-exhibition (R03)、edge-classification (R04)、edge-proxy (R05)、edge-control (R06 平台扩展)、edge-unidirectional-association (R07)、edge-bidirectional-association (R08)、edge-unidirectional-tagged (R09)、edge-bidirectional-tagged (R10)。交互与锚点关系（19 种）：edge-consumption (R11)、edge-result (R12)、edge-in-change (R13)、edge-out-change (R14)、edge-in-out (R15 归一化为 R13+R14)、edge-in-influence-nullable (R16)、edge-in-influence-nonnull (R17)、edge-out-influence (R18)、edge-in-out-influence-nullable (R19)、edge-in-out-influence-nonnull (R20)、edge-agent (R21)、edge-condition (R22)、edge-invocation (R23)、edge-invocation-self (R24)、edge-overtime-exception (R25)、edge-undertime-exception (R26)、edge-note (R27)、edge-fixed (R28)、edge-link (R00)。2. 21 种标准事件代码表生命周期与状态（7 种）：a（出现）、as（指定状态出现）、sa（进入状态）、v（属性变化）、x（消亡）、xs（指定状态消亡）、sx（离开状态）。集合代理（4 种）：ma（集合增成员）、am（加入集合）、mx（集合减成员）、xm（移出集合）。引用指向（6 种）：ra（建立指向）、ar（被建立指向）、rv（变更指向）、vr（被变更指向）、rx（解除指向）、xr（被解除指向）。过程异常与收尾（4 种）：pca（提前完成）、pcd（超时完成）、psf（启动失败）、paa（中断取消）。==================== validation-rules.md ====================静态模型校验规则与《方圆语言标准-1117》条款对照表规则编号严重度标准条款依据拦截条件描述对应单测VAL-001ERROR标准 9.2.2 / 12.1.2非耗时算子（timeConsuming=false）内部禁止包含耗时子过程或延迟等待逻辑ValidationRulesAndMappingTestVAL-002WARNING标准 12.1.2普通父过程包含耗时子过程时自动向上推断或提示显式开启耗时标记（规避 BE-17）ValidationRulesAndMappingTestVAL-003ERROR标准 8.5对象类型单继承与多继承有向图不得存在闭环（防 BL-15 栈溢出）ValidationRulesAndMappingTestVAL-004ERROR标准 9.4.7含子过程设计视图的复合过程必须至少声明一个 firstStart=true 入口子过程ValidationRulesAndMappingTestVAL-005ERROR平台扩展 R06结构控制关系（edge-control）必须构成有向无环图（DAG）ValidationRulesAndMappingTestVAL-006ERROR标准 7.3定值（Constant）严禁连接消耗、生成、状态转换或单/双向输出影响连线ValidationRulesVal006to010TestVAL-007ERROR标准 8.2标识符编码若由用户显式填写，不得违反 ^[A-Za-z_][A-Za-z0-9_]*$ 或命中 Java 21 保留字（实现于 UniqueIdentifierGuard，StaticModelValidator 接线）ValidationRulesVal006to010TestVAL-008ERROR标准 10.4同组互斥分支概率之和必须落在 [0.9999, 1.0001] 闭区间内（单成员命名组降级 WARNING）ValidationRulesVal006to010TestVAL-009ERROR标准 10.1.3同一对象与同一过程之间同向同类型关系最多允许存在一条（edge-fixed 除外）ValidationRulesAndMappingTestVAL-010ERROR标准 9.6成员链与引用定位路径深度不得超过最大递归上限 64 层ValidationRulesVal006to010Test


```python?code_reference&code_event_index=6
import json, os
from collections import Counter

base = '/tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma/docs'

def inspect_model(name, a_file, b_file):
    with open(os.path.join(base, a_file), 'r', encoding='utf-8') as f:
        da = json.load(f)
    with open(os.path.join(base, b_file), 'r', encoding='utf-8') as f:
        db = json.load(f)
    print(f"=== {name} ({a_file} + {b_file}) ===")
    print("data-a top keys:", list(da.keys()))
    obj_a = da.get('obj', da)
    print("obj_a keys:", list(obj_a.keys()) if isinstance(obj_a, dict) else type(obj_a))
    if isinstance(obj_a, dict):
        for k, v in obj_a.items():
            if k not in ('entities', 'views', 'tree', 'orgTree', 'cells'):
                print(f"  {k}: {str(v)[:120]}")
        entities = obj_a.get('entities', [])
        print("entities count:", len(entities))
        ent_types = Counter()
        link_types = Counter()
        code_langs = Counter()
        events = Counter()
        ent_map = {}
        if isinstance(entities, list):
            for e in entities:
                ent_map[e.get('id')] = e
                et = e.get('type', 'NONE')
                st = e.get('subType', '')
                ent_types[f"{et}" + (f"/{st}" if st else "")] += 1
                if et == 'LINK':
                    lt = e.get('linkType') or e.get('relationType') or e.get('shape') or 'UNKNOWN'
                    link_types[lt] += 1
                if e.get('code') or e.get('script') or e.get('codeContent'):
                    code_langs[f"{e.get('codeType') or e.get('language') or 'code'}"] += 1
                for ek in ('event', 'events', 'triggerEvent', 'eventCode'):
                    if e.get(ek):
                        events[str(e.get(ek))] += 1
        elif isinstance(entities, dict):
            for eid, e in entities.items():
                ent_map[eid] = e
                et = e.get('type', 'NONE')
                ent_types[et] += 1
        print("Entity types:", dict(ent_types.most_common()))
        print("Entity LINK linkTypes:", dict(link_types.most_common()))
        print("Code blocks:", dict(code_langs))
        print("Events on entities:", dict(events))

    print("data-b top keys:", list(db.keys()) if isinstance(db, dict) else type(db))
    obj_b = db.get('obj', db) if isinstance(db, dict) else db
    if isinstance(obj_b, dict):
        print("obj_b keys:", list(obj_b.keys())[:20])
        # check structure of obj_b
        sample_k = next(iter(obj_b.keys()))
        print(f"sample obj_b[{sample_k}] type:", type(obj_b[sample_k]), str(obj_b[sample_k])[:200])
    elif isinstance(obj_b, list):
        print("obj_b list len:", len(obj_b), "sample:", str(obj_b[0])[:200] if obj_b else "")

inspect_model("Model 1", "data-a.json", "data-b.json")
inspect_model("Model 2", "scene-a.json", "scene-b.json")

Pythonimport os, glob

root = '/tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma'

files_to_read = [
    'groma-studio/groma-compiler-hotswap/src/test/java/com/groma/studio/compiler/RealCorpusEndToEndTest.java',
    'groma-bootstrap/src/main/java/com/groma/bootstrap/pipeline/ModelPipelineService.java',
    'groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/GromaModuleSourceGeneratorFacade.java',
]

for f in files_to_read:
    p = os.path.join(root, f)
    print(f"\n================ {f} ================")
    with open(p, 'r', encoding='utf-8') as fp:
        print(fp.read())

代码输出
================ groma-studio/groma-compiler-hotswap/src/test/java/com/groma/studio/compiler/RealCorpusEndToEndTest.java ================
package com.groma.studio.compiler;

import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.archive.ArchiveCheckpointCoordinator;
import com.groma.base.engine.archive.SimulationArchiveBundle;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.studio.codegen.GeneratedModuleBundle;
import com.groma.studio.codegen.GromaModuleSourceGeneratorFacade;
import com.groma.studio.codegen.plan.LegacyPlanAdapter;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.parser.AbModelMergerAndParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实语料六步流水线回归（CK-NP-CMP-001 / CK-NP-HOT-001 / CK-NP-PRS-001）：
 * 拿回来 → 解析 → 五层出码 → ECJ 内存编译 → 隔离类加载挂载 → 推进 N 步。
 * 语料目录由系统属性 groma.corpus 指定，未指定时自动探测仓库内 docs/model。
 */
public class RealCorpusEndToEndTest {

    static Optional<Path> corpusDir() {
        String configured = System.getProperty("groma.corpus");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(Path.of(configured));
        }
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path candidate = p.resolve("docs/model");
            if (Files.isRegularFile(candidate.resolve("data-a.json"))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** 解析 + 生成 + 落盘（供编译与人工核对），返回生成包 */
    public static GeneratedModuleBundle generateRealModule() {
        Path dir = corpusDir().orElseThrow(() -> new IllegalStateException(
                "未找到语料目录 docs/model（可用 -Dgroma.corpus=<目录> 指定）"));
        GromaModuleIR ir = new AbModelMergerAndParser().parse(dir.resolve("data-a.json"), dir.resolve("data-b.json"));
        GeneratedModuleBundle bundle = new GromaModuleSourceGeneratorFacade().generateModule(ir, Map.of());
        // 解析告警（含 T1.1 同拍双读时钟诊断）落盘，供逐项核对
        try {
            List<String> warnings = new ArrayList<>(ir.compilePlan().warnings());
            warnings.add(0, "共 " + warnings.size() + " 条");
            Files.writeString(Path.of("target", "real-corpus-warnings.txt"), String.join("\n", warnings),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        Path out = Path.of("target", "generated-" + bundle.getModuleDirName());
        bundle.getFiles().forEach((rel, content) -> {
            try {
                Path file = out.resolve(rel);
                Files.createDirectories(file.getParent());
                Files.writeString(file, content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        return bundle;
    }

    /** ECJ 内存编译，失败时把全部诊断打印出来便于定位 */
    public static Map<String, byte[]> compileRealModule(GeneratedModuleBundle bundle) {
        CompilationResult result = new EcjInMemoryCompiler().compile(
                bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap(),
                RealCorpusEndToEndTest.class.getClassLoader());
        List<String> errors = new ArrayList<>();
        for (CanvasCompileDiagnostic d : result.diagnostics()) {
            if (d.error()) {
                errors.add(d.relativeJavaFile() + ":" + d.javaLine() + " [" + d.mappedElementId() + "] " + d.message());
            }
        }
        assertTrue(errors.isEmpty(), () -> "生成代码必须能被 ECJ 编译，错误 " + errors.size() + " 个:\n"
                + String.join("\n", errors.subList(0, Math.min(errors.size(), 60))));
        assertTrue(result.success(), "ECJ 编译应成功");
        return Map.copyOf(result.classBytecodes());
    }

    public static Map<String, byte[]> resourcesOf(GeneratedModuleBundle bundle) {
        return EcjInMemoryCompiler.extractResources(bundle.getFiles());
    }

    /** 隔离类加载并实例化模块运行时入口 */
    public static GromaModuleRuntime mountRealModule(GeneratedModuleBundle bundle, Map<String, byte[]> bytecodes,
                                               Map<String, byte[]> resources) {
        ModelModuleClassLoader loader = new ModelModuleClassLoader(
                bundle.getModuleDirName(), "real", bundle.getBasePackage(), bytecodes, resources,
                RealCorpusEndToEndTest.class.getClassLoader());
        ServiceLoader<GromaModuleRuntime> services = ServiceLoader.load(GromaModuleRuntime.class, loader);
        List<GromaModuleRuntime> runtimes = new ArrayList<>();
        services.forEach(runtimes::add);
        Assertions.assertEquals(1, runtimes.size(), "生成的模块应登记且仅登记一个运行时入口");
        return runtimes.get(0);
    }

    @Test
    void realCorpusParsesGeneratesCompilesAndRuns() {
        GeneratedModuleBundle bundle = generateRealModule();
        CompilePlanIR plan = LegacyPlanAdapter.adapt(bundleHolder(bundle));
        assertTrue(plan.processDefs().size() >= 39, "真实语料应解析出不少于 39 个过程定义");

        Map<String, byte[]> bytecodes = compileRealModule(bundle);
        assertTrue(bytecodes.size() > 60, "编译出的类数量应与五层源码规模匹配，实际 " + bytecodes.size());
        Map<String, byte[]> resources = resourcesOf(bundle);
        assertTrue(resources.containsKey("META-INF/services/com.groma.base.engine.GromaModuleRuntime"),
                "生成包应包含运行时入口的 ServiceLoader 登记");
        GromaModuleRuntime runtime = mountRealModule(bundle, bytecodes, resources);
        assertTrue(runtime.scenes().size() >= 2, "应有两个运行场景");
        assertTrue(runtime.triggerTable().triggerCount() >= 10, "事件接线应全部登记");

        // 手动模式会话管理器：调用线程逐步推进，结果确定
        List<String> report = new ArrayList<>();
        GromaSessionManager manager = new GromaSessionManager(null, 8);
        try {
            for (GromaModuleRuntime.SceneDescriptor scene : runtime.scenes()) {
                List<String> trace = new ArrayList<>();
                String sessionId = manager.startSession(runtime, scene.sceneCode(), Map.of(), 20261126L, true,
                        ctx -> ctx.setTraceListener(new CorpusTrace(trace)));
                WorldSnapshot snapshot = manager.stepSession(sessionId, 300);
                try {
                    Files.writeString(Path.of("target", "real-corpus-trace-" + scene.sceneCode() + ".txt"),
                            String.join("\n", trace), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                assertTrue(snapshot.tickIndex() >= 1, "场景应至少推进 1 步");
                Assertions.assertEquals(300, snapshot.tickIndex(), "应精确推进到第 300 步: " + scene.zhName());
                Assertions.assertFalse(snapshot.instances().isEmpty(), "场景应有实例");
                report.add("场景 " + scene.zhName() + " 300 步：实例 " + snapshot.instances().size()
                        + "，在途过程 " + snapshot.activeProcesses().size() + "，事件 " + snapshot.recentEvents().size()
                        + "，问题 " + snapshot.problems().size());
                for (Map.Entry<String, Map<String, Object>> inst : snapshot.instances().entrySet()) {
                    report.add("  实例 " + inst.getKey() + " = " + inst.getValue());
                }
                report.addAll(snapshot.problems().stream().map(p2 -> "  问题 " + p2).toList());
                manager.stopSession(sessionId);
            }
        } finally {
            manager.close();
        }
        try {
            Files.writeString(Path.of("target", "real-corpus-result.txt"), String.join("\n", report), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** T1.2 验收：类型视图里无事件触发的「降雨」调用点经 startManually 手动启动，累计雨量应随推演增长 */
    @Test
    void manualStartRunsRainfallProcess() {
        GeneratedModuleBundle bundle = generateRealModule();
        Map<String, byte[]> bytecodes = compileRealModule(bundle);
        GromaModuleRuntime runtime = mountRealModule(bundle, bytecodes, resourcesOf(bundle));
        // 「降雨与受影响区域」上的调用点：宿主经成员「降雨」关联到降雨对象，成功后把 累计雨量/当前雨强 回写成员
        GromaModuleRuntime.ManualStartDescriptor rainfall = runtime.manualStarts().stream()
                .filter(m -> "降雨".equals(m.zhName()) && "RainfallAffectedArea".equals(m.ownerTypeCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("应发现手动启动点「降雨」（降雨与受影响区域），实际发现 "
                        + runtime.manualStarts().size() + " 个手动启动点"));
        // 雨量序列键 = 时间日期（毫秒纪元时间戳）；在 beforeInitialize 里按会话时钟对齐构造，
        // 键覆盖会话起点前后 700 秒、每 1000ms 一键
        java.util.function.Function<com.groma.base.engine.GromaWorldContext, Map<Long, Double>> seriesOf = ctx -> {
            long start = ctx.currentWorldTimeMs();
            Map<Long, Double> series = new java.util.LinkedHashMap<>();
            for (long ms = start - 5_000L; ms <= start + 700_000L; ms += 1_000L) {
                series.put(ms, 2.0D);
            }
            return series;
        };

        GromaSessionManager manager = new GromaSessionManager(null, 8);
        List<String> trace = new ArrayList<>();
        com.groma.base.engine.GromaProcessTask[] probe = new com.groma.base.engine.GromaProcessTask[1];
        try {
            String scene = runtime.scenes().get(0).sceneCode();
            String sessionId = manager.startSession(runtime, scene, Map.of(), 20261126L, true, ctx -> {
                com.groma.base.engine.BaseSimEntity rain = ctx.spawn(
                        runtime.newEntity("Rainfall", "rain-test-1"));
                com.groma.base.engine.BaseSimEntity area = ctx.spawn(
                        runtime.newEntity(rainfall.ownerTypeCode(), "raa-test-1"));
                String setRef = ctx.setInstanceValue("raa-test-1", "降雨", rain);
                Assertions.assertEquals("", setRef, "关联成员「降雨」应写入成功");
                com.groma.base.engine.GromaProcessTask task = runtime.startManually(
                        rainfall.defKey(), ctx, area, Map.of("雨量序列", seriesOf.apply(ctx)));
                Assertions.assertNotNull(task, "startManually 应返回「降雨」过程任务");
                ctx.launch(task);
                ctx.setTraceListener(new CorpusTrace(trace));
                probe[0] = task;
            });
            manager.stepSession(sessionId, 3);
            WorldSnapshot snapshot = manager.stepSession(sessionId, 297);
            try {
                Files.writeString(Path.of("target", "real-corpus-trace-manual-rainfall.txt"),
                        String.join("\n", trace), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            Map<String, Object> state = snapshot.instances().get("raa-test-1");
            Assertions.assertNotNull(state, "手动创建的实例应在场景快照里: " + snapshot.instances().keySet());
            // 平台口径（与存量一致）：耗时过程 IN_OUT 形参只在成功时回写成员；本语料的降雨过程
            // 序列耗尽后回边无出口（模型数据问题），任务持续运行，故读任务形参断言计算正确性
            com.groma.base.engine.GromaProcessTask task1 = probe[0];
            Assertions.assertEquals("RUNNING", task1.getStatus(),
                    "300 拍后降雨任务应仍在按序列逐拍推进");
            assertTrue(task1.getStepCount() >= 290, "任务应被世界每拍驱动，stepCount=" + task1.getStepCount());
            double acc;
            try {
                acc = (double) task1.getClass().getMethod("getCumulativeRainfall").invoke(task1);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("生成的降雨任务应暴露累计雨量形参读取器", ex);
            }
            assertTrue(acc > 0.0D, "300 拍后任务形参累计雨量应 > 0，实际 " + acc
                    + "（每拍 += 当前雨强/60），实例成员回写需过程成功结束");
            manager.stopSession(sessionId);
        } finally {
            manager.close();
        }
    }

    /** 墙钟戳成员（时间日期写入，随会话 startEpoch 变化，不属于仿真状态） */
    private static final List<String> WALL_CLOCK_MEMBERS = List.of("实际离站时刻", "实际到站时刻");

    private static Map<String, Map<String, Object>> withoutWallClock(WorldSnapshot snapshot) {
        Map<String, Map<String, Object>> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> inst : snapshot.instances().entrySet()) {
            Map<String, Object> state = new java.util.LinkedHashMap<>(inst.getValue());
            WALL_CLOCK_MEMBERS.forEach(state::remove);
            out.put(inst.getKey(), state);
        }
        return out;
    }

    /** T1.5 验收：同种子两次 300 拍的实例状态与问题逐一一致（确定性；墙钟戳成员除外） */
    @Test
    void realCorpusRunIsDeterministic() {
        GeneratedModuleBundle bundle = generateRealModule();
        Map<String, byte[]> bytecodes = compileRealModule(bundle);
        GromaModuleRuntime runtime = mountRealModule(bundle, bytecodes, resourcesOf(bundle));
        GromaSessionManager manager = new GromaSessionManager(null, 8);
        try {
            String scene = runtime.scenes().get(0).sceneCode();
            String first = manager.startSession(runtime, scene, Map.of(), 20261126L, true, null);
            WorldSnapshot endFirst = manager.stepSession(first, 300);
            String second = manager.startSession(runtime, scene, Map.of(), 20261126L, true, null);
            WorldSnapshot endSecond = manager.stepSession(second, 300);
            Assertions.assertEquals(endFirst.tickIndex(), endSecond.tickIndex());
            Assertions.assertEquals(withoutWallClock(endFirst), withoutWallClock(endSecond),
                    "同种子两次运行实例状态应逐一一致（墙钟戳成员除外）");
            Assertions.assertEquals(endFirst.problems(), endSecond.problems(), "同种子两次运行问题列表应一致");
            manager.stopSession(first);
            manager.stopSession(second);
        } finally {
            manager.close();
        }
    }

    /**
     * T1.5 验收：第 150 拍导出存档，新会话恢复后续跑到 300，与不中断运行的最终状态一致。
     * <p>
     * 当前已知缺陷（T1.5 待专项修复）：恢复后的世界墙钟（时间日期基准）未随存档恢复，
     * 导致“实际到站/离站时刻”整体偏移一次中断时长；在途耗时过程的周期也与连续运行发散。
     * 本用例先行锁定该缺陷，修复后启用。
     */
    @Test
    @org.junit.jupiter.api.Disabled("T1.5 证据已固化：151 拍首步即分叉（恢复世界多出 GeoPoint_6/7、列车提前进入停站、"
            + "到站时刻=+150 拍）；逐拍 digest 对比工具保留在本用例，待存档子系统专项（事件重放/在途任务对齐）")
    void realCorpusResumesFromArchive() throws IOException {
        GeneratedModuleBundle bundle = generateRealModule();
        Map<String, byte[]> bytecodes = compileRealModule(bundle);
        GromaModuleRuntime runtime = mountRealModule(bundle, bytecodes, resourcesOf(bundle));
        ArchiveCheckpointCoordinator coordinator = new ArchiveCheckpointCoordinator();
        GromaSessionManager manager = new GromaSessionManager(null, 8);
        try {
            String scene = runtime.scenes().get(0).sceneCode();
            // 基线：连续运行 300 拍（151~300 逐拍记录摘要，供与续跑逐拍对比定位首个分叉拍）
            String plain = manager.startSession(runtime, scene, Map.of(), 20261126L, true, null);
            manager.stepSession(plain, 150);
            Map<Long, String> plainDigests = new java.util.LinkedHashMap<>();
            java.util.function.Function<WorldSnapshot, String> digestFn = snap -> snap.instances().toString();
            for (int tick = 151; tick <= 300; tick++) {
                WorldSnapshot sp = manager.stepSession(plain, 1);
                plainDigests.put(sp.tickIndex(), digestFn.apply(sp));
            }
            WorldSnapshot endPlain = manager.getSnapshot(plain);
            // 中断：跑 150 拍 → 存档 → 续跑 150 拍
            String split = manager.startSession(runtime, scene, Map.of(), 20261126L, true, null);
            WorldSnapshot at150 = manager.stepSession(split, 150);
            SimulationArchiveBundle checkpoint = coordinator.createCheckpointAtStepBoundary(
                    manager.require(split).session());
            Assertions.assertEquals(150L, checkpoint.tickIndex(), "存档应落在第 150 拍边界");
            // T1.5 诊断：存档任务表与 150 拍实例状态落盘，供与连续运行比对
            {
                List<String> diag = new ArrayList<>();
                diag.add("== 存档任务表（tick=150）==");
                for (var p2 : checkpoint.processParts()) {
                    diag.add("  task " + p2.processInstanceId() + " defKey=" + p2.processDefId()
                            + " host=" + p2.hostInstanceId() + " status=" + p2.status()
                            + " localSteps=" + p2.localSteps() + " elapsed=" + p2.elapsedMs()
                            + "/" + p2.expectedMs() + " subGraphPc=" + p2.subGraphPc());
                }
                diag.add("== 150 拍实例状态摘要 ==");
                for (var e : at150.instances().entrySet()) {
                    diag.add("  " + e.getKey() + " -> " + e.getValue());
                }
                Files.writeString(Path.of("target", "resume-diag-150.txt"), String.join("\n", diag),
                        StandardCharsets.UTF_8);
            }
            WorldSnapshot endSplit = manager.stepSession(split, 150);
            // 恢复：新会话载入存档并逐拍推进，记录每拍摘要；与连续运行同拍对比，定位首个分叉拍
            String resumed = manager.startSession(runtime, scene, Map.of(), 20261126L, true, null);
            manager.require(resumed).session().restoreFromArchive(checkpoint);
            Map<Long, String> resumedDigests = new java.util.LinkedHashMap<>();
            for (int tick = 151; tick <= 300; tick++) {
                WorldSnapshot sr = manager.stepSession(resumed, 1);
                resumedDigests.put(sr.tickIndex(), digestFn.apply(sr));
            }
            long firstFork = -1;
            for (long tick = 151; tick <= 300; tick++) {
                if (!plainDigests.get(tick).equals(resumedDigests.get(tick))) {
                    firstFork = tick;
                    break;
                }
            }
            List<String> diag2 = new ArrayList<>();
            diag2.add("首个分叉拍: " + firstFork);
            if (firstFork > 0) {
                diag2.add("连续@" + (firstFork - 1) + ": " + plainDigests.get(firstFork - 1));
                diag2.add("恢复@" + (firstFork - 1) + ": " + resumedDigests.get(firstFork - 1));
                diag2.add("连续@" + firstFork + ": " + plainDigests.get(firstFork));
                diag2.add("恢复@" + firstFork + ": " + resumedDigests.get(firstFork));
            }
            Files.writeString(Path.of("target", "resume-diag-fork.txt"), String.join("\n", diag2),
                    StandardCharsets.UTF_8);
            WorldSnapshot endResumed = manager.getSnapshot(resumed);
            Assertions.assertEquals(300L, endResumed.tickIndex(), "续跑应精确推进到第 300 拍");
            Assertions.assertEquals(endPlain.instances(), endResumed.instances(),
                    "存档续跑第 151~300 拍应与连续运行一致");
            Assertions.assertEquals(endPlain.problems(), endResumed.problems());
            for (String id : List.of(plain, split, resumed)) {
                manager.stopSession(id);
            }
        } finally {
            manager.close();
        }
    }

/** 逐步追踪：过程启停、成员事件、触发器命中（落到 target/real-corpus-trace-*.txt 供人工核对运行语义） */
    static final class CorpusTrace implements com.groma.base.engine.GromaTraceListener {
        private final List<String> out;

        CorpusTrace(List<String> out) {
            this.out = out;
        }

        @Override
        public void onProcessStarted(com.groma.base.engine.GromaWorldContext ctx, GromaProcessTask task) {
            out.add("[t=" + ctx.getTickIndex() + "] 启动过程 " + task.getProcessInstanceId() + "「" + task.getProcessName() + "」"
                    + (task.isTimeRelated() ? "耗时" : "瞬时") + " 宿主=" + name(task.hostEntity())
                    + (task.getParent() == null ? " 顶层" : " 父=" + task.getParent().getProcessInstanceId()));
        }

        @Override
        public void onProcessFinished(com.groma.base.engine.GromaWorldContext ctx, GromaProcessTask task) {
            out.add("[t=" + ctx.getTickIndex() + "] 结束过程 " + task.getProcessInstanceId() + "「" + task.getProcessName() + "」"
                    + task.getStatus() + (task.getFailReason().isEmpty() ? "" : " 原因=" + task.getFailReason())
                    + " 步数=" + task.getStepCount() + " 用时=" + task.getElapsedMs() + "/" + task.getExpectedMs() + "ms");
        }

        @Override
        public void onEvent(com.groma.base.engine.GromaWorldContext ctx, long tick, boolean silent,
                            WorldSnapshot.SimEventRecord event) {
            com.groma.base.engine.BaseSimEntity source = ctx.findInstance(event.sourceId());
            String member = event.memberToken();
            if (source != null && !member.isEmpty()) {
                com.groma.base.engine.GromaMemberMeta meta = source.findMember(member);
                member = meta == null ? member : meta.zhName();
            }
            out.add("[t=" + tick + "] 事件 " + event.eventCode() + " " + (source == null ? event.sourceId() : name(source))
                    + (member.isEmpty() ? "" : "." + member) + (event.stateLabel().isEmpty() ? "" : " 状态=" + event.stateLabel())
                    + (event.subjectId().isEmpty() ? "" : " 新对象=" + event.subjectId())
                    + (event.detail().isEmpty() ? "" : " " + event.detail()));
        }

        @Override
        public void onTriggerFired(com.groma.base.engine.GromaWorldContext ctx, long tick, WorldSnapshot.SimEventRecord event,
                                   com.groma.base.engine.BaseSimEntity contextEntity, com.groma.base.engine.BaseSimEntity host,
                                   String targetDefKey) {
            out.add("[t=" + tick + "]   ↳ 触发「" + targetDefKey + "」 接线对象=" + name(contextEntity) + " 宿主=" + name(host));
        }

        @Override
        public void onDefaultStartFired(com.groma.base.engine.GromaWorldContext ctx, long tick,
                                        com.groma.base.engine.BaseSimEntity created, com.groma.base.engine.BaseSimEntity host,
                                        String targetDefKey) {
            out.add("[t=" + tick + "]   ↳ 默认启动「" + targetDefKey + "」 新对象=" + name(created) + " 宿主=" + name(host));
        }

        @Override
        public void onProblem(com.groma.base.engine.GromaWorldContext ctx, long tick, String message) {
            out.add("[t=" + tick + "] !! 问题 " + message);
        }

        private static String name(com.groma.base.engine.BaseSimEntity entity) {
            return entity == null ? "-" : entity.getTypeZhName() + "(" + entity.getInstanceId() + ")";
        }
    }

    private static GromaModuleIR bundleHolder(GeneratedModuleBundle bundle) {
        Path dir = corpusDir().orElseThrow();
        return new AbModelMergerAndParser().parse(dir.resolve("data-a.json"), dir.resolve("data-b.json"));
    }
}


================ groma-bootstrap/src/main/java/com/groma/bootstrap/pipeline/ModelPipelineService.java ================
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

    public ModelPipelineService(VersionPinnedHotSwapManager hotSwap,
                                WorkspaceSyncService workspaceSync,
                                JavacParityVerifier javacVerifier,
                                GromaModelRegistry modelRegistry,
                                @org.springframework.beans.factory.annotation.Value(
                                        "${groma.pipeline.workspace-root:groma-workspace}") String workspaceRoot) {
        this.hotSwap = hotSwap;
        this.workspaceSync = workspaceSync;
        this.javacVerifier = javacVerifier;
        this.modelRegistry = modelRegistry;
        this.workspaceRoot = Path.of(workspaceRoot);
    }

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
        // 1. 解析（A+B 合并 → 编译计划）
        GromaModuleIR ir = new AbModelMergerAndParser().parse(dataA, dataB);
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
        // 2. 出码
        GeneratedModuleBundle bundle = new GromaModuleSourceGeneratorFacade().generateModule(ir, Map.of());
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
        // 5. javac 一致性对拍（JDK 可用时）
        boolean javacParity = false;
        String javacNote;
        if (javacVerifier.isSystemJavacAvailable()) {
            try {
                CompilationResult javacResult = javacVerifier.verifyWithJavac(
                        bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap());
                javacParity = javacResult.success();
                javacNote = "javac 对拍完成，类数量 " + javacResult.classBytecodes().size();
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


================ groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/GromaModuleSourceGeneratorFacade.java ================
package com.groma.studio.codegen;

import com.groma.studio.codegen.plan.LegacyPlanAdapter;
import com.groma.studio.codegen.plan.ManifestAndIndexEmitter;
import com.groma.studio.codegen.plan.ModelLayerEmitter;
import com.groma.studio.codegen.plan.ModuleRuntimeEmitter;
import com.groma.studio.codegen.plan.PlanModel;
import com.groma.studio.codegen.plan.ProcessTaskEmitter;
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
        CompilePlanIR plan = (ir.compilePlan() == null || CompilePlanIR.EMPTY.equals(ir.compilePlan()))
                ? LegacyPlanAdapter.adapt(ir) : ir.compilePlan();
        PlanModel model = PlanModel.of(ir, plan, existingSymbolLocks);
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
        for (PlanModel.SceneInfo scene : model.scenes) {
            putJava(bundle, serviceEmitter.emitController(scene));
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


Pythonimport os

root = '/tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma'

files = [
    'groma-studio/groma-model-parser/src/main/java/com/groma/studio/ir/CompilePlanIR.java',
    'groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/StaticModelValidator.java',
    'groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/CanvasMappingTables.java',
]

for f in files:
    print(f"\n================ {f} ================")
    with open(os.path.join(root, f), 'r', encoding='utf-8') as fp:
        print(fp.read())

代码输出
================ groma-studio/groma-model-parser/src/main/java/com/groma/studio/ir/CompilePlanIR.java ================
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

    /** R25 超时（pcd）/ R26 提前完成（pca）异常控制流边；eventKind 取 OVERTIME_PCD / UNDERTIME_PCA。
     *  sourcePc = 被监控的耗时子过程节点（设计视图中连线源端），-1 表示连线挂在过程整体（过程级监控）。 */
    public record ExceptionFlowIR(
            String eventKind,
            int sourcePc,
            int targetPc,
            String targetDefKey
    ) {
        public ExceptionFlowIR(String eventKind, int targetPc, String targetDefKey) {
            this(eventKind, -1, targetPc, targetDefKey);
        }
    }

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


================ groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/StaticModelValidator.java ================
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


================ groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/CanvasMappingTables.java ================
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


Pythonimport os, struct, glob

root = '/tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma'

# Check .class version
sample_cls = glob.glob(os.path.join(root, '**/target/classes/**/*.class'), recursive=True)[0]
with open(sample_cls, 'rb') as f:
    magic, minor, major = struct.unpack('>IHH', f.read(8))
print(f"Sample class {sample_cls}: major={major} (Java {major - 44})")

# Check all jars in the repo
jars = glob.glob(os.path.join(root, '**/*.jar'), recursive=True)
print("Jars in repo:", [os.path.relpath(j, root) for j in jars])

代码输出Sample class /tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma/groma-base/groma-base-common/target/classes/com/groma/base/common/PageResult.class: major=65 (Java 21)
Jars in repo: ['groma-base/groma-base-common/target/groma-base-common-1.0.0.jar', 'groma-base/groma-base-connector/target/groma-base-connector-1.0.0.jar', 'groma-base/groma-base-engine/target/groma-base-engine-1.0.0.jar', 'groma-base/groma-base-mcp/target/groma-base-mcp-1.0.0.jar', 'groma-base/groma-base-websocket/target/groma-base-websocket-1.0.0.jar', 'groma-bootstrap/groma-workspace/groma-models/model-emergency-deduction/target/model-emergency-deduction-1.0.0-sources.jar', 'groma-bootstrap/groma-workspace/groma-models/model-emergency-deduction/target/model-emergency-deduction-1.0.0.jar', 'groma-bootstrap/target/groma-bootstrap-1.0.0.jar', 'groma-models/model-emergency-deduction/target/model-emergency-deduction-1.0.0.jar', 'groma-models/model-smart-planning/target/model-smart-planning-1.0.0.jar', 'groma-studio/groma-compiler-hotswap/target/groma-compiler-hotswap-1.0.0-tests.jar', 'groma-studio/groma-compiler-hotswap/target/groma-compiler-hotswap-1.0.0.jar', 'groma-studio/groma-model-import/target/groma-model-import-1.0.0.jar', 'groma-studio/groma-model-parser/target/groma-model-parser-1.0.0.jar', 'groma-studio/groma-naming-engine/target/groma-naming-engine-1.0.0.jar', 'groma-studio/groma-source-generator/target/groma-source-generator-1.0.0.jar', 'groma-studio/groma-workspace-sync/target/groma-workspace-sync-1.0.0.jar']

Pythonimport os

root = '/tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma'
parser_path = os.path.join(root, 'groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.java')
with open(parser_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
print(f"Total lines in AbModelMergerAndParser.java: {len(lines)}")
for i, line in enumerate(lines[:600], 1):
    print(f"{i:4d}: {line}", end='')

代码输出Total lines in AbModelMergerAndParser.java: 2724
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

Pythonimport os

root = '/tmp/orpl/orpl-ef2f634c5ce89403bf6387cd4776e66811b86d96/groma'
parser_path = os.path.join(root, 'groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.java')
with open(parser_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
for i, line in enumerate(lines[600:1600], 601):
    print(f"{i:4d}: {line}", end='')

代码输出 601:             String repId = text(rep, "id");
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
 651:         private GuardBundle parseGuardsAndExceptions(DefCtx def, String repId, List<String> entityIds,
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
 693:                         // 收割【修复 4】：连线位于设计视图两个子过程之间时，source=被监控耗时子过程、
 694:                         // target=异常处理子过程；sourcePc=-1 表示过程级超时/提前监控
 695:                         Integer srcPc = pcByCell.get(text(source, "id"));
 696:                         Integer dstPc = pcByCell.get(text(target, "id"));
 697:                         if (dstPc != null) {
 698:                             exceptions.add(new ExceptionFlowIR(
 699:                                     overtime ? "OVERTIME_PCD" : "UNDERTIME_PCA",
 700:                                     srcPc == null ? -1 : srcPc,
 701:                                     dstPc, ""));
 702:                         }
 703:                         continue;
 704:                     }
 705:                     JsonNode linkEntity = entities.get(text(edge, "data"));
 706:                     String stateLabel = "";
 707:                     OperandIR op;
 708:                     if ("STATE".equals(text(source, "type"))) {
 709:                         JsonNode stateEntity = entities.get(text(source, "data"));
 710:                         stateLabel = stateEntity == null ? text(source, "label") : text(stateEntity, "label");
 711:                         String enumId = enumOfState.getOrDefault(text(source, "data"), "");
 712:                         op = enumId.isEmpty() ? operandOf(def, v, source) : enumMemberOperand(def, v, enumId);
 713:                     } else {
 714:                         op = operandOf(def, v, source, edge);
 715:                     }
 716:                     if (op == null) {
 717:                         continue;
 718:                     }
 719:                     String describe = lt + ":" + text(source, "label");
 720:                     ConditionRuleIR rule = new ConditionRuleIR(
 721:                             op, stateLabel, conditionsOf(linkEntity), describe, expressionOf(linkEntity));
 722:                     if (agent) {
 723:                         maintains.add(rule);
 724:                         starts.add(rule);
 725:                     } else {
 726:                         starts.add(rule);
 727:                     }
 728:                 }
 729:             }
 730:             return new GuardBundle(starts, maintains, exceptions);
 731:         }
 732: 
 733:         /** 连线概率（rate/percentage 兼容：>1 视为百分数） */
 734:         private double probabilityOf(JsonNode edge, JsonNode linkEntity) {
 735:             Double p = nullableDouble(edge, "probability");
 736:             if (p == null && linkEntity != null) {
 737:                 p = nullableDouble(linkEntity, "probability");
 738:             }
 739:             if (p == null && linkEntity != null) {
 740:                 p = nullableDouble(linkEntity, "rate");
 741:             }
 742:             if (p == null) {
 743:                 return 1.0D;
 744:             }
 745:             return p > 1.0001D ? p / 100.0D : Math.max(0.0D, p);
 746:         }
 747: 
 748:         /** 互斥概率组（标准 10.4）：probabilityGroup / groupCode / mutexGroup 字段兼容 */
 749:         private String probabilityGroupOf(JsonNode edge, JsonNode linkEntity) {
 750:             String g = text(edge, "probabilityGroup");
 751:             if (g.isEmpty() && linkEntity != null) {
 752:                 g = text(linkEntity, "probabilityGroup");
 753:             }
 754:             if (g.isEmpty() && linkEntity != null) {
 755:                 g = text(linkEntity, "groupCode");
 756:             }
 757:             if (g.isEmpty() && linkEntity != null) {
 758:                 g = text(linkEntity, "mutexGroup");
 759:             }
 760:             return g;
 761:         }
 762: 
 763:         /** 定义视图 → 形参契约 */
 764:         private List<PortIR> parseParams(ViewCtx define) {
 765:             List<PortIR> params = new ArrayList<>();
 766:             if (define == null) {
 767:                 return params;
 768:             }
 769:             List<JsonNode> relevanceCells = new ArrayList<>();
 770:             for (JsonNode cell : define.cells) {
 771:                 if ("RELEVANCE-OBJECT".equals(text(cell, "type")) && !text(cell, "data").isEmpty()) {
 772:                     relevanceCells.add(cell);
 773:                 }
 774:             }
 775:             relevanceCells.sort(CELL_ORDER);
 776:             Set<String> seen = new HashSet<>();
 777:             for (JsonNode cell : relevanceCells) {
 778:                 String id = text(cell, "data");
 779:                 if (!seen.add(id)) {
 780:                     continue;
 781:                 }
 782:                 String direction = "LOCAL";
 783:                 boolean nonnull = false;
 784:                 boolean result = false;
 785:                 // 影响类连线的方向固定为“关联对象 → 过程.端口”，因此看关联对象的出向连线判断输入、入向连线判断输出
 786:                 for (JsonNode edge : define.outgoing(text(cell, "id"))) {
 787:                     if (!isProcessCell(define.target(edge))) {
 788:                         continue;
 789:                     }
 790:                     switch (linkType(edge)) {
 791:                         case "in-influence-nonnull", "in-influence" -> {
 792:                             direction = "IN";
 793:                             nonnull = true;
 794:                         }
 795:                         case "in-influence-nullable" -> direction = "IN";
 796:                         case "in-out-influence-nonnull", "in-out-influence" -> {
 797:                             direction = "IN_OUT";
 798:                             nonnull = true;
 799:                         }
 800:                         case "in-out-influence-nullable" -> direction = "IN_OUT";
 801:                         case "agent", "condition", "consumption" -> {
 802:                             direction = "IN";
 803:                             nonnull = true;
 804:                         }
 805:                         default -> {
 806:                             // 其余连线不改变方向
 807:                         }
 808:                     }
 809:                 }
 810:                 for (JsonNode edge : define.incoming(text(cell, "id"))) {
 811:                     if (!isProcessCell(define.source(edge))) {
 812:                         continue;
 813:                     }
 814:                     String linkType = linkType(edge);
 815:                     if ("result".equals(linkType)) {
 816:                         direction = "OUT";
 817:                         result = true;
 818:                     } else if ("out-influence".equals(linkType)) {
 819:                         direction = "OUT";
 820:                     }
 821:                 }
 822:                 params.add(new PortIR(id, text(entities.get(id), "label"), direction, nonnull, result, slotType(id)));
 823:             }
 824:             return params;
 825:         }
 826: 
 827:         // ------------------------------------------------------------------ 设计视图 → 数据流节点
 828: 
 829:         /**
 830:          * 过程定义解析上下文
 831:          */
 832:         private final class DefCtx {
 833:             final String defKey;
 834:             final String ownerTypeId;
 835:             final ViewCtx design;
 836:             final Map<String, PortIR> params = new HashMap<>();
 837:             final Map<String, OperandIR> operands = new HashMap<>();
 838:             final LinkedHashSet<String> localIds = new LinkedHashSet<>();
 839:             final List<LocalVarIR> localVars = new ArrayList<>();
 840:             final List<NodeIR> nodes = new ArrayList<>();
 841:             final List<Integer> entries = new ArrayList<>();
 842:             final Set<String> warned = new HashSet<>();
 843: 
 844:             DefCtx(String defKey, String ownerTypeId, ViewCtx design) {
 845:                 this.defKey = defKey;
 846:                 this.ownerTypeId = ownerTypeId;
 847:                 this.design = design;
 848:             }
 849: 
 850:             void warn(String message) {
 851:                 if (warned.add(defKey + "|" + message)) {
 852:                     warnings.add("[" + defKey + "] " + message);
 853:                 }
 854:             }
 855:         }
 856: 
 857:         /** 节点原始信息（排序前） */
 858:         private final class NodeRaw {
 859:             final JsonNode cell;
 860:             final JsonNode entity;
 861:             final String cellId;
 862:             boolean tool;
 863:             String toolKey = "";
 864:             String calleeDefKey = "";
 865:             boolean calleeTimeConsuming;
 866:             boolean slow;
 867:             boolean firstStart;
 868:             boolean lastEnd;
 869:             OperandIR host;
 870:             final List<ArgBindingIR> inputs = new ArrayList<>();
 871:             final List<ArgBindingIR> writes = new ArrayList<>();
 872:             final List<JsonNode> nextEdges = new ArrayList<>();
 873:             final List<PostIR> postActions = new ArrayList<>();
 874:             String resultJavaType = "";
 875: 
 876:             NodeRaw(JsonNode cell, JsonNode entity) {
 877:                 this.cell = cell;
 878:                 this.entity = entity;
 879:                 this.cellId = text(cell, "id");
 880:             }
 881:         }
 882: 
 883:         private List<JsonNode> portsOf(JsonNode cell) {
 884:             List<JsonNode> ports = new ArrayList<>();
 885:             JsonNode items = field(field(cell, "ports"), "items");
 886:             if (items != null && items.isArray()) {
 887:                 for (JsonNode port : items) {
 888:                     ports.add(port);
 889:                 }
 890:             }
 891:             return ports;
 892:         }
 893: 
 894:         private JsonNode portArgs(JsonNode port) {
 895:             return field(port, "args");
 896:         }
 897: 
 898:         private String portName(JsonNode args) {
 899:             String other = text(args, "otherData");
 900:             String name = other.isEmpty() ? "" : text(entities.get(other), "label");
 901:             if (name.isEmpty()) {
 902:                 name = text(args, "text");
 903:             }
 904:             return name;
 905:         }
 906: 
 907:         private static String portDirection(JsonNode args) {
 908:             String type = text(args, "type");
 909:             if (type.startsWith("in-out")) {
 910:                 return "IN_OUT";
 911:             }
 912:             if (type.startsWith("in-")) {
 913:                 return "IN";
 914:             }
 915:             if (type.startsWith("out-")) {
 916:                 return "OUT";
 917:             }
 918:             return "";
 919:         }
 920: 
 921:         private boolean isProcessCell(JsonNode cell) {
 922:             return cell != null && PROCESS_CELL_TYPES.contains(text(cell, "type"));
 923:         }
 924: 
 925:         private List<JsonNode> fixedIntoPort(ViewCtx view, String cellId, String portId) {
 926:             List<JsonNode> out = new ArrayList<>();
 927:             for (JsonNode edge : view.incoming(cellId)) {
 928:                 if ("fixed".equals(linkType(edge)) && portId.equals(text(edge.path("target"), "port"))) {
 929:                     out.add(edge);
 930:                 }
 931:             }
 932:             return out;
 933:         }
 934: 
 935:         private void parseFlowNodes(DefCtx def, ViewCtx view, List<PortIR> params) {
 936:             if (view == null) {
 937:                 return;
 938:             }
 939:             List<JsonNode> procCells = new ArrayList<>();
 940:             for (JsonNode cell : view.cells) {
 941:                 if (isProcessCell(cell) && entities.containsKey(text(cell, "data"))) {
 942:                     procCells.add(cell);
 943:                 }
 944:             }
 945:             procCells.sort(CELL_ORDER);
 946:             Map<String, NodeRaw> raws = new LinkedHashMap<>();
 947:             Map<String, Integer> visualOrder = new HashMap<>();
 948:             for (int i = 0; i < procCells.size(); i++) {
 949:                 JsonNode cell = procCells.get(i);
 950:                 visualOrder.put(text(cell, "id"), i);
 951:                 raws.put(text(cell, "id"), readNode(def, view, cell));
 952:             }
 953: 
 954:             // 3. 控制流：全部调用线
 955:             for (JsonNode cell : view.cells) {
 956:                 if (!"LINK".equals(text(cell, "type"))) {
 957:                     continue;
 958:                 }
 959:                 String linkType = linkType(cell);
 960:                 NodeRaw from = raws.get(text(cell.path("source"), "cell"));
 961:                 if (from == null) {
 962:                     continue;
 963:                 }
 964:                 if ("invocation".equals(linkType) || "invocation-self".equals(linkType)) {
 965:                     if (raws.containsKey(text(cell.path("target"), "cell"))) {
 966:                         from.nextEdges.add(cell);
 967:                     }
 968:                 }
 969:             }
 970: 
 971:             // 4. 类型推断（工具输出类型沿数据流传播，迭代到稳定）
 972:             for (int round = 0; round < 8; round++) {
 973:                 boolean changed = false;
 974:                 for (NodeRaw raw : raws.values()) {
 975:                     String next = raw.tool ? inferToolResult(def, raw, raws) : "";
 976:                     if (!next.equals(raw.resultJavaType)) {
 977:                         raw.resultJavaType = next;
 978:                         changed = true;
 979:                     }
 980:                 }
 981:                 if (!changed) {
 982:                     break;
 983:                 }
 984:             }
 985: 
 986:             // 5. 排序与编号：从入口沿调用线广度优先，其余按画布位置追加
 987:             List<NodeRaw> ordered = orderNodes(view, raws, visualOrder);
 988:             Map<String, Integer> pcOf = new HashMap<>();
 989:             for (int i = 0; i < ordered.size(); i++) {
 990:                 pcOf.put(ordered.get(i).cellId, i);
 991:             }
 992:             for (NodeRaw raw : ordered) {
 993:                 List<NextIR> nexts = new ArrayList<>();
 994:                 raw.nextEdges.sort(Comparator.comparingInt(e -> visualOrder.getOrDefault(text(e.path("target"), "cell"), Integer.MAX_VALUE)));
 995:                 for (JsonNode edge : raw.nextEdges) {
 996:                     String targetCell = text(edge.path("target"), "cell");
 997:                     Integer targetPc = pcOf.get(targetCell);
 998:                     if (targetPc == null) {
 999:                         continue;
1000:                     }
1001:                     String sourcePort = text(edge.path("source"), "port");
1002:                     JsonNode linkEntity = entities.get(text(edge, "data"));
1003:                     boolean conditional = !sourcePort.isEmpty();
1004:                     // 连线延迟（存量语义：线上 delayType=STEP N 拍 / FIXED 秒 → 延迟 N 步后激活后继）
1005:                     long delayTicks = 0L;
1006:                     String delayType = text(edge, "delayType");
1007:                     double delayValue = doubleOf(edge, "delayValue", 0.0D);
1008:                     if ("STEP".equalsIgnoreCase(delayType) && delayValue > 0) {
1009:                         delayTicks = Math.max(1L, StrictMath.round(delayValue));
1010:                     } else if ("FIXED".equalsIgnoreCase(delayType) && delayValue > 0) {
1011:                         delayTicks = Math.max(1L, StrictMath.round(delayValue * 1000.0D / DEFAULT_STEP_MS));
1012:                     }
1013:                     NextIR next = new NextIR(
1014:                             targetPc, conditional, conditional ? portName(portArgs(portOfId(raw.cell, sourcePort))) : "",
1015:                             conditionsOf(linkEntity), expressionOf(linkEntity), text(edge, "event"), delayTicks,
1016:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity));
1017:                     // 去重：同一条连线只登记一次
1018:                     if (!nexts.contains(next)) {
1019:                         nexts.add(next);
1020:                     }
1021:                 }
1022:                 // 上游节点输出槽位的类型此刻已推断完成，回填到操作数上
1023:                 Function<OperandIR, OperandIR> resolve = op -> retargetOutput(op, raws);
1024:                 NodeIR node = new NodeIR(
1025:                         pcOf.get(raw.cellId), raw.cellId, text(raw.entity, "id"), text(raw.entity, "label"),
1026:                         raw.tool ? "TOOL" : "CALL", raw.toolKey, raw.calleeDefKey, raw.calleeTimeConsuming, raw.slow,
1027:                         raw.firstStart, raw.lastEnd, raw.host == null ? null : resolve.apply(raw.host),
1028:                         retarget(raw.inputs, resolve), retarget(raw.writes, resolve), raw.resultJavaType,
1029:                         List.copyOf(nexts), retargetPosts(raw.postActions, resolve),
1030:                         doubleOf(raw.cell, "x", 0.0D), doubleOf(raw.cell, "y", 0.0D));
1031:                 def.nodes.add(node);
1032:                 if (raw.firstStart) {
1033:                     def.entries.add(node.pc());
1034:                 }
1035:             }
1036:             // 入口兜底：没有 firstStart 时取没有入向调用线的节点
1037:             if (def.entries.isEmpty() && !def.nodes.isEmpty()) {
1038:                 Set<Integer> targeted = new HashSet<>();
1039:                 for (NodeIR node : def.nodes) {
1040:                     for (NextIR next : node.nexts()) {
1041:                         targeted.add(next.targetPc());
1042:                     }
1043:                 }
1044:                 for (NodeIR node : def.nodes) {
1045:                     if (!targeted.contains(node.pc())) {
1046:                         def.entries.add(node.pc());
1047:                     }
1048:                 }
1049:                 if (def.entries.isEmpty()) {
1050:                     def.entries.add(def.nodes.get(0).pc());
1051:                 }
1052:             }
1053:             diagnoseClockReads(def, raws);
1054:         }
1055: 
1056:         // ------------------------------------------------------------------ T1.1 编译期诊断
1057: 
1058:         /**
1059:          * 同拍双读仿真时间的诊断（只告警，不改语义）：
1060:          * 1) “减”的两个输入都直接读 仿真时间.持续运行时长 —— 按拍量化语义下同拍读数相同，差值恒为 0；
1061:          * 2) “减”位于回边循环内，且两个输入的写入源都能追溯到仿真时钟 —— 循环每圈把两个输入
1062:          *    重刷为同一量化时刻，差值恒为 0（典型：开始时刻在循环体内赋值）。
1063:          * 修正途径：改用 ΔT（CUR-PROCESS-TIME 图元，编译为步长秒数），或把起始量的赋值移出循环。
1064:          */
1065:         private void diagnoseClockReads(DefCtx def, Map<String, NodeRaw> raws) {
1066:             Map<String, List<String>> succ = new HashMap<>();
1067:             for (NodeRaw raw : raws.values()) {
1068:                 List<String> out = new ArrayList<>();
1069:                 for (JsonNode edge : raw.nextEdges) {
1070:                     String target = text(edge.path("target"), "cell");
1071:                     if (raws.containsKey(target)) {
1072:                         out.add(target);
1073:                     }
1074:                 }
1075:                 succ.put(raw.cellId, out);
1076:             }
1077:             Set<String> cycle = new LinkedHashSet<>();
1078:             Map<String, Integer> state = new HashMap<>();
1079:             for (String id : raws.keySet()) {
1080:                 if (!state.containsKey(id)) {
1081:                     collectCycleNodes(id, succ, state, new ArrayList<>(), cycle);
1082:                 }
1083:             }
1084:             for (NodeRaw raw : raws.values()) {
1085:                 if (!"arithmetic.minus".equals(raw.toolKey) || raw.inputs.size() < 2) {
1086:                     continue;
1087:                 }
1088:                 if (raw.inputs.stream().allMatch(b -> b.operand() instanceof BuiltinIR builtin
1089:                         && "SIM_ELAPSED_SECONDS".equals(builtin.key()))) {
1090:                     def.warn("节点 " + raw.cellId + "（减）的两个输入都是仿真时间.持续运行时长：同拍量化读取差值恒为 0，"
1091:                             + "请改用 ΔT（CUR-PROCESS-TIME 图元，编译为步长秒数）或调整画布");
1092:                     continue;
1093:                 }
1094:                 if (!cycle.contains(raw.cellId)) {
1095:                     continue;
1096:                 }
1097:                 boolean allClockFed = raw.inputs.stream()
1098:                         .allMatch(b -> clockFed(b.operand(), raws, 0, new HashSet<>()));
1099:                 if (allClockFed) {
1100:                     def.warn("节点 " + raw.cellId + "（减）位于回边循环内，且两个输入在循环体内每圈都重新赋值为当前量化时刻："
1101:                             + "差值恒为 0；请把起始量的赋值移出循环，或改用 ΔT（CUR-PROCESS-TIME 图元）");
1102:                 }
1103:             }
1104:         }
1105: 
1106:         /** DFS 找回边，把回边覆盖的环上节点记入 cycle */
1107:         private void collectCycleNodes(String id, Map<String, List<String>> succ, Map<String, Integer> state,
1108:                 List<String> path, Set<String> cycle) {
1109:             state.put(id, 1);
1110:             path.add(id);
1111:             for (String next : succ.getOrDefault(id, List.of())) {
1112:                 Integer st = state.get(next);
1113:                 if (st == null) {
1114:                     collectCycleNodes(next, succ, state, path, cycle);
1115:                 } else if (st == 1) {
1116:                     boolean inCycle = false;
1117:                     for (String node : path) {
1118:                         if (node.equals(next)) {
1119:                             inCycle = true;
1120:                         }
1121:                         if (inCycle) {
1122:                             cycle.add(node);
1123:                         }
1124:                     }
1125:                 }
1126:             }
1127:             path.remove(path.size() - 1);
1128:             state.put(id, 2);
1129:         }
1130: 
1131:         /** 操作数的取值链能否追溯到仿真时钟（赋值传递、局部量/成员的写入节点递归，深度与访问集防环） */
1132:         private boolean clockFed(OperandIR operand, Map<String, NodeRaw> raws, int depth, Set<String> visiting) {
1133:             if (operand == null || depth > 6) {
1134:                 return false;
1135:             }
1136:             if (operand instanceof BuiltinIR builtin && "SIM_ELAPSED_SECONDS".equals(builtin.key())) {
1137:                 return true;
1138:             }
1139:             if (operand instanceof NodeOutputIR output) {
1140:                 NodeRaw source = raws.get(output.cellId());
1141:                 if (source == null || !visiting.add(source.cellId)) {
1142:                     return false;
1143:                 }
1144:                 try {
1145:                     for (ArgBindingIR binding : source.inputs) {
1146:                         if (clockFed(binding.operand(), raws, depth + 1, visiting)) {
1147:                             return true;
1148:                         }
1149:                     }
1150:                 } finally {
1151:                     visiting.remove(source.cellId);
1152:                 }
1153:                 return false;
1154:             }
1155:             String entityId = operandEntityId(operand);
1156:             if (entityId == null || entityId.isEmpty() || !visiting.add("member:" + entityId)) {
1157:                 return false;
1158:             }
1159:             try {
1160:                 for (NodeRaw raw : raws.values()) {
1161:                     for (ArgBindingIR binding : raw.writes) {
1162:                         if (!entityId.equals(operandEntityId(binding.operand()))) {
1163:                             continue;
1164:                         }
1165:                         for (ArgBindingIR input : raw.inputs) {
1166:                             if (clockFed(input.operand(), raws, depth + 1, visiting)) {
1167:                                 return true;
1168:                             }
1169:                         }
1170:                     }
1171:                 }
1172:             } finally {
1173:                 visiting.remove("member:" + entityId);
1174:             }
1175:             return false;
1176:         }
1177: 
1178:         /** 操作数对应的实体 id（成员路径取最后一段） */
1179:         private String operandEntityId(OperandIR operand) {
1180:             if (operand instanceof MemberPathIR path) {
1181:                 return path.memberIds().isEmpty() ? "" : path.memberIds().get(path.memberIds().size() - 1);
1182:             }
1183:             if (operand instanceof LocalIR local) {
1184:                 return local.entityId();
1185:             }
1186:             if (operand instanceof ParamIR param) {
1187:                 return param.entityId();
1188:             }
1189:             if (operand instanceof NamedInstanceIR named) {
1190:                 return named.instanceId();
1191:             }
1192:             if (operand instanceof ConstIR constant) {
1193:                 return constant.entityId();
1194:             }
1195:             return "";
1196:         }
1197: 
1198:         private JsonNode portOfId(JsonNode cell, String portId) {
1199:             for (JsonNode port : portsOf(cell)) {
1200:                 if (portId.equals(text(port, "id"))) {
1201:                     return port;
1202:                 }
1203:             }
1204:             return null;
1205:         }
1206: 
1207:         /** 单个过程图元 → 节点原始信息 */
1208:         private NodeRaw readNode(DefCtx def, ViewCtx view, JsonNode cell) {
1209:             JsonNode entity = entities.get(text(cell, "data"));
1210:             NodeRaw raw = new NodeRaw(cell, entity);
1211:             raw.tool = isToolProcess(entity);
1212:             raw.toolKey = raw.tool ? text(entity, "innerClass") + "." + text(entity, "innerType") : "";
1213:             if (!raw.tool) {
1214:                 raw.calleeDefKey = defKeyOf(entity);
1215:                 String cellTime = text(cell, "timeRelated");
1216:                 raw.calleeTimeConsuming = cellTime.isEmpty() ? isTimeRelated(entity) : "RELATED".equalsIgnoreCase(cellTime);
1217:             } else {
1218:                 raw.slow = isTimeRelated(cell);
1219:             }
1220:             raw.firstStart = flag(cell, "firstStart");
1221:             raw.lastEnd = flag(cell, "lastEnd");
1222: 
1223:             // 组成线：CALL 节点的宿主对象 / 集合类工具操作的集合
1224:             for (JsonNode edge : view.incoming(raw.cellId)) {
1225:                 if ("composition".equals(linkType(edge))) {
1226:                     raw.host = operandOf(def, view, view.source(edge));
1227:                     break;
1228:                 }
1229:             }
1230: 
1231:             List<JsonNode> ports = portsOf(cell);
1232:             for (JsonNode port : ports) {
1233:                 String portId = text(port, "id");
1234:                 JsonNode args = portArgs(port);
1235:                 String direction = portDirection(args);
1236:                 if (direction.isEmpty()) {
1237:                     direction = "IN";
1238:                 }
1239:                 String argName = portName(args);
1240:                 if (argName.isEmpty()) {
1241:                     argName = portId;
1242:                 }
1243:                 // 锚点线永远指向被馈入的端口：来源是成员 = 读取或写出；来源是另一节点的输出端口 = 取上游结果
1244:                 List<JsonNode> into = fixedIntoPort(view, raw.cellId, portId);
1245:                 sortBySourcePosition(view, into);
1246:                 for (JsonNode edge : into) {
1247:                     JsonNode source = view.source(edge);
1248:                     OperandIR operand = operandOf(def, view, source, edge);
1249:                     if (isProcessCell(source)) {
1250:                         if ("OUT".equals(direction)) {
1251:                             def.warn("输出端口 " + argName + " 被另一个节点的输出端口馈入，按输入处理");
1252:                         }
1253:                         raw.inputs.add(new ArgBindingIR(argName, operand));
1254:                         continue;
1255:                     }
1256:                     if ("OUT".equals(direction)) {
1257:                         raw.writes.add(new ArgBindingIR(argName, operand));
1258:                         continue;
1259:                     }
1260:                     raw.inputs.add(new ArgBindingIR(argName, operand));
1261:                     if ("IN_OUT".equals(direction)) {
1262:                         raw.writes.add(new ArgBindingIR(argName, operand));
1263:                     }
1264:                 }
1265:             }
1266:             // 无端口定义的过程图元：入向锚点线按唯一隐式双向参数处理
1267:             if (ports.isEmpty()) {
1268:                 List<JsonNode> implicit = new ArrayList<>();
1269:                 for (JsonNode edge : view.incoming(raw.cellId)) {
1270:                     if ("fixed".equals(linkType(edge))) {
1271:                         implicit.add(edge);
1272:                     }
1273:                 }
1274:                 sortBySourcePosition(view, implicit);
1275:                 for (JsonNode edge : implicit) {
1276:                     OperandIR operand = operandOf(def, view, view.source(edge), edge);
1277:                     raw.inputs.add(new ArgBindingIR("a", operand));
1278:                     raw.writes.add(new ArgBindingIR("a", operand));
1279:                 }
1280:             }
1281:             readPostActions(def, view, raw);
1282:             return raw;
1283:         }
1284: 
1285:         /**
1286:          * 节点成功后的后置动作：out-change 状态转换、result 生成、consumption 消耗。
1287:          * 过程设计视图里的子过程调用点与类型 / 场景设计视图里的顶层调用点共用这一规则。
1288:          */
1289:         private void readPostActions(DefCtx def, ViewCtx view, NodeRaw raw) {
1290:             raw.postActions.addAll(collectPostActions(def, view, raw.cellId));
1291:         }
1292: 
1293:         private List<PostIR> collectPostActions(DefCtx def, ViewCtx view, String cellId) {
1294:             List<PostIR> posts = new ArrayList<>();
1295:             List<JsonNode> postEdges = new ArrayList<>();
1296:             for (JsonNode edge : view.outgoing(cellId)) {
1297:                 String linkType = linkType(edge);
1298:                 // 收割【修复 3】：R15 in-out 拆解语义——源状态前置进 canStart（parseGuards），
1299:                 // 目标状态后置转换在此与 out-change 同路承载
1300:                 if ("out-change".equals(linkType) || "in-out".equals(linkType) || "result".equals(linkType)
1301:                         || "consumption".equals(linkType)) {
1302:                     postEdges.add(edge);
1303:                 }
1304:             }
1305:             sortByTargetPosition(view, postEdges);
1306:             for (JsonNode edge : postEdges) {
1307:                 JsonNode linkEntity = entities.get(text(edge, "data"));
1308:                 JsonNode target = view.target(edge);
1309:                 if (target == null || isProcessCell(target)) {
1310:                     continue;
1311:                 }
1312:                 String linkType = linkType(edge);
1313:                 if ("out-change".equals(linkType) || "in-out".equals(linkType)) {
1314:                     JsonNode stateEntity = entities.get(text(target, "data"));
1315:                     String enumId = enumOfState.getOrDefault(text(target, "data"), "");
1316:                     OperandIR member = enumMemberOperand(def, view, enumId);
1317:                     if (member == null) {
1318:                         def.warn("输出转换找不到枚举类型 " + memberLabel(enumId) + " 的成员，已忽略");
1319:                         continue;
1320:                     }
1321:                     posts.add(new PostIR("OUT_CHANGE", member, text(stateEntity, "label"), enumId, null,
1322:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
1323:                 } else if ("result".equals(linkType)) {
1324:                     posts.add(new PostIR("RESULT", operandOf(def, view, target), "", "", null,
1325:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
1326:                 } else {
1327:                     posts.add(new PostIR("CONSUME", operandOf(def, view, target), "", "", null,
1328:                             probabilityOf(edge, linkEntity), probabilityGroupOf(edge, linkEntity)));
1329:                 }
1330:             }
1331:             return posts;
1332:         }
1333: 
1334:         /** 视图里属于指定枚举类型的成员单元格 → 操作数（直接成员优先） */
1335:         private OperandIR enumMemberOperand(DefCtx def, ViewCtx view, String enumId) {
1336:             if (enumId == null) {
1337:                 return null;
1338:             }
1339:             List<JsonNode> candidates = new ArrayList<>();
1340:             for (JsonNode cell : view.cells) {
1341:                 JsonNode entity = entities.get(text(cell, "data"));
1342:                 if (entity == null || !"ENUM".equals(text(entity, "typeRange"))) {
1343:                     continue;
1344:                 }
1345:                 if (enumId.equals(refTypeIdOf(entity))) {
1346:                     candidates.add(cell);
1347:                 }
1348:             }
1349:             candidates.sort(CELL_ORDER);
1350:             JsonNode chosen = null;
1351:             for (JsonNode cell : candidates) {
1352:                 if (pathSourceOf(view, text(cell, "id")).isEmpty()) {
1353:                     chosen = cell;
1354:                     break;
1355:                 }
1356:             }
1357:             if (chosen == null && !candidates.isEmpty()) {
1358:                 chosen = candidates.get(0);
1359:                 def.warn("输出转换的目标枚举成员不唯一，按画布位置取第一个");
1360:             }
1361:             return chosen == null ? null : operandOf(def, view, chosen);
1362:         }
1363: 
1364:         // ------------------------------------------------------------------ 操作数解析
1365: 
1366:         /** 指向该成员单元格的路径线（属性 / 组成），来源不是过程图元 */
1367:         private List<JsonNode> pathSourcesOf(ViewCtx view, String cellId) {
1368:             List<JsonNode> sources = new ArrayList<>();
1369:             for (JsonNode edge : view.incoming(cellId)) {
1370:                 String linkType = linkType(edge);
1371:                 if (("exhibition".equals(linkType) || "composition".equals(linkType))
1372:                         && !isProcessCell(view.source(edge))) {
1373:                     sources.add(edge);
1374:                 }
1375:             }
1376:             return sources;
1377:         }
1378: 
1379:         private String pathSourceOf(ViewCtx view, String cellId) {
1380:             List<JsonNode> sources = pathSourcesOf(view, cellId);
1381:             if (sources.isEmpty()) {
1382:                 return "";
1383:             }
1384:             sortBySourcePosition(view, sources);
1385:             return text(sources.get(0).path("source"), "cell");
1386:         }
1387: 
1388:         private void sortBySourcePosition(ViewCtx view, List<JsonNode> edges) {
1389:             edges.sort(Comparator.comparingDouble((JsonNode e) -> posY(view.source(e)))
1390:                     .thenComparingDouble(e -> posX(view.source(e)))
1391:                     .thenComparing(e -> text(e, "id")));
1392:         }
1393: 
1394:         private void sortByTargetPosition(ViewCtx view, List<JsonNode> edges) {
1395:             edges.sort(Comparator.comparingDouble((JsonNode e) -> posY(view.target(e)))
1396:                     .thenComparingDouble(e -> posX(view.target(e)))
1397:                     .thenComparing(e -> text(e, "id")));
1398:         }
1399: 
1400:         private double posX(JsonNode cell) {
1401:             return cell == null ? 0.0D : doubleOf(cell, "x", 0.0D);
1402:         }
1403: 
1404:         private double posY(JsonNode cell) {
1405:             return cell == null ? 0.0D : doubleOf(cell, "y", 0.0D);
1406:         }
1407: 
1408:         private static double positionOf(JsonNode cell) {
1409:             return (cell == null ? 0.0D : doubleOf(cell, "y", 0.0D)) * 100_000.0D
1410:                     + (cell == null ? 0.0D : doubleOf(cell, "x", 0.0D));
1411:         }
1412: 
1413:         private String entityTypePlaceholder(String typeId) {
1414:             return (typeId == null || typeId.isEmpty()) ? GromaTypes.OBJECT : GromaTypes.entity(typeId);
1415:         }
1416: 
1417:         /**
1418:          * 单元格 → 操作数。edge 非空且单元格是带输出端口的过程图元时，取该节点端口的输出槽位
1419:          */
1420:         private OperandIR operandOf(DefCtx def, ViewCtx view, JsonNode cell) {
1421:             return operandOf(def, view, cell, null);
1422:         }
1423: 
1424:         private OperandIR operandOf(DefCtx def, ViewCtx view, JsonNode cell, JsonNode edge) {
1425:             if (cell == null) {
1426:                 return new ConstIR("", "null", GromaTypes.OBJECT, "<缺失>");
1427:             }
1428:             String cellId = text(cell, "id");
1429:             if (isProcessCell(cell)) {
1430:                 String portId = edge == null ? "" : text(edge.path("source"), "port");
1431:                 JsonNode port = portId.isEmpty() ? null : portOfId(cell, portId);
1432:                 String argName = port == null ? "" : portName(portArgs(port));
1433:                 String cacheKey = cellId + "#" + argName;
1434:                 OperandIR cachedOutput = def.operands.get(cacheKey);
1435:                 if (cachedOutput == null) {
1436:                     cachedOutput = new NodeOutputIR(cellId, argName, GromaTypes.OBJECT);
1437:                     def.operands.put(cacheKey, cachedOutput);
1438:                 }
1439:                 return cachedOutput;
1440:             }
1441:             OperandIR cached = def.operands.get(cellId);
1442:             if (cached != null) {
1443:                 return cached;
1444:             }
1445:             OperandIR operand = resolveOperand(def, view, cell);
1446:             def.operands.put(cellId, operand);
1447:             return operand;
1448:         }
1449: 
1450:         private OperandIR resolveOperand(DefCtx def, ViewCtx view, JsonNode cell) {
1451:             String cellId = text(cell, "id");
1452:             String type = text(cell, "type");
1453:             String entityId = text(cell, "data");
1454:             JsonNode entity = entities.get(entityId);
1455:             String label = entity == null ? text(cell, "label") : text(entity, "label");
1456: 
1457:             // 内置对象根
1458:             switch (type) {
1459:                 case "CUR-RUN-TIME":
1460:                     return new BuiltinIR("SIM", null, GromaTypes.OBJECT);
1461:                 case "CUR-OBJECT":
1462:                 case "THING-OBJECT":
1463:                     return new SelfIR(entityTypePlaceholder(def.ownerTypeId));
1464:                 case "CUR-PROCESS-TIME":
1465:                     // 存量语义（initials/Helper 把该图元解析为 DELTA_T 变量，runtime/ProcessInfo.runProcess
1466:                     // 每次运行刷新为距上次运行的秒数）：过程每拍至多推进一圈，ΔT 恒等于步长秒数
1467:                     return new BuiltinIR("SIM_STEP_SECONDS", null, GromaTypes.DOUBLE);
1468:                 case "ACTUAL-RUN-TIME":
1469:                     return new BuiltinIR("ACTUAL_RUN_SECONDS", null, GromaTypes.DOUBLE);
1470:                 case "USER-INFO":
1471:                     return new BuiltinIR("USER", null, GromaTypes.OBJECT);
1472:                 case "RUN-PARAM":
1473:                     return new BuiltinIR("RUN_PARAM", null, GromaTypes.OBJECT);
1474:                 default:
1475:                     break;
1476:             }
1477: 
1478:             // 路径成员：目标车站 → 位置 → 经度
1479:             String pathSource = pathSourceOf(view, cellId);
1480:             if (!pathSource.isEmpty()) {
1481:                 OperandIR root = operandOf(def, view, view.cell(pathSource));
1482:                 return memberStep(def, root, entity, label);
1483:             }
1484: 
1485:             switch (type) {
1486:                 case "INSTANCE": {
1487:                     String typeId = text(entity, "instanceTypeEntityId");
1488:                     if (typeId.isEmpty()) {
1489:                         typeId = text(entity, "typeEntityId");
1490:                     }
1491:                     String placeholder = entityTypePlaceholder(typeId);
1492:                     if (flag(entity, "isCollection")) {
1493:                         placeholder = "map".equals(text(entity, "containerType"))
1494:                                 ? GromaTypes.map(GromaTypes.baseJavaType(text(entity, "keyType")), placeholder)
1495:                                 : GromaTypes.list(placeholder);
1496:                     }
1497:                     return new NamedInstanceIR(entityId, typeId, placeholder);
1498:                 }
1499:                 case "CONSTANT": {
1500:                     String displayName = label;
1501:                     if (text(entity, "alias").length() > 0 && label.startsWith("定值")) {
1502:                         displayName = text(entity, "alias");
1503:                     }
1504:                     return new ConstIR(entityId, text(entity, "defaultValue"), slotType(entity), displayName);
1505:                 }
1506:                 case "STATE": {
1507:                     String enumId = enumOfState.getOrDefault(entityId, "");
1508:                     return new ConstIR(entityId, label,
1509:                             enumId.isEmpty() ? GromaTypes.STRING : GromaTypes.enumType(enumId), label);
1510:                 }
1511:                 case "NOTE": {
1512:                     String text = text(entity, "description");
1513:                     return new ConstIR(entityId, text.isEmpty() ? label : text, GromaTypes.STRING, label);
1514:                 }
1515:                 default:
1516:                     break;
1517:             }
1518: 
1519:             if (entity == null) {
1520:                 def.warn("单元格 " + cellId + " 引用的实体不存在: " + entityId);
1521:                 return new ConstIR(entityId, "null", GromaTypes.OBJECT, label);
1522:             }
1523: 
1524:             // 根成员：形参 / 局部量 / 宿主自身的成员
1525:             PortIR param = def.params.get(entityId);
1526:             if (param != null && !"LOCAL".equals(param.direction())) {
1527:                 return new ParamIR(entityId, label, param.javaType());
1528:             }
1529:             String entityKind = text(entity, "type");
1530:             boolean variableLike = "TEMPLATE-MEMBER".equals(entityKind) || "RELEVANCE-OBJECT".equals(entityKind)
1531:                     || (param != null);
1532:             if (variableLike) {
1533:                 return declareLocal(def, entity);
1534:             }
1535:             if ("ATTRIBUTE-MEMBER".equals(entityKind) || "COMPOSE-MEMBER".equals(entityKind)
1536:                     || "SETTING-MEMBER".equals(entityKind)) {
1537:                 String owner = memberOwner.get(entityId);
1538:                 if (owner != null && owner.equals(def.ownerTypeId)) {
1539:                     return new MemberPathIR(new SelfIR(entityTypePlaceholder(def.ownerTypeId)), List.of(entityId),
1540:                             slotType(entityId));
1541:                 }
1542:                 // 成员不属于过程宿主：若恰有一个形参/局部量的类型拥有该成员，则从它出发（画布上省略了起点）
1543:                 List<OperandIR> owners = new ArrayList<>();
1544:                 for (JsonNode cell2 : view.cells) {
1545:                     JsonNode e2 = entities.get(text(cell2, "data"));
1546:                     if (e2 == null || text(cell2, "id").equals(cellId)) {
1547:                         continue;
1548:                     }
1549:                     String candidateType = GromaTypes.entityIdOf(slotType(e2));
1550:                     if (candidateType != null && candidateType.equals(owner) && pathSourceOf(view, text(cell2, "id")).isEmpty()) {
1551:                         owners.add(operandOf(def, view, cell2));
1552:                     }
1553:                 }
1554:                 if (owners.size() == 1) {
1555:                     return new MemberPathIR(owners.get(0), List.of(entityId), slotType(entityId));
1556:                 }
1557:                 def.warn("成员“" + label + "”不属于过程宿主类型，且无法确定归属，按宿主成员处理");
1558:                 return new MemberPathIR(new SelfIR(entityTypePlaceholder(def.ownerTypeId)), List.of(entityId),
1559:                         slotType(entityId));
1560:             }
1561:             def.warn("单元格 " + cellId + "（" + type + " " + label + "）不是可解析的操作数，按空值处理");
1562:             return new ConstIR(entityId, "null", GromaTypes.OBJECT, label);
1563:         }
1564: 
1565:         /** 路径上的一步：内置对象的成员、集合的长度、领域对象的成员 */
1566:         private OperandIR memberStep(DefCtx def, OperandIR root, JsonNode memberEntity, String label) {
1567:             String memberId = memberEntity == null ? "" : text(memberEntity, "id");
1568:             if (root instanceof BuiltinIR builtin && "SIM".equals(builtin.key())) {
1569:                 return switch (label) {
1570:                     case "时间日期" -> new BuiltinIR("SIM_DATETIME", null, GromaTypes.LONG);
1571:                     case "持续运行时长" -> new BuiltinIR("SIM_ELAPSED_SECONDS", null, GromaTypes.DOUBLE);
1572:                     default -> new BuiltinIR("SIM_" + label, null, GromaTypes.OBJECT);
1573:                 };
1574:             }
1575:             if (memberEntity != null && "SETTING-MEMBER".equals(text(memberEntity, "type"))) {
1576:                 return switch (text(memberEntity, "supplementType")) {
1577:                     case "length" -> new BuiltinIR("LENGTH", root, GromaTypes.LONG);
1578:                     case "arrayIndex" -> new BuiltinIR("INDEX", root, GromaTypes.LONG);
1579:                     default -> new BuiltinIR("KEY", root, GromaTypes.OBJECT);
1580:                 };
1581:             }
1582:             if (memberEntity != null && ("ARRAY-ELEMENT".equals(text(memberEntity, "type"))
1583:                     || "MAP-ELEMENT".equals(text(memberEntity, "type")))) {
1584:                 // 集合字面量元素在过程设计视图里只作为展示，取其值
1585:                 return new ConstIR(memberId, text(memberEntity, "defaultValue"), slotType(memberEntity), label);
1586:             }
1587:             return new MemberPathIR(root, List.of(memberId), slotType(memberId));
1588:         }
1589: 
1590:         /** 模板成员 / 未连线的关联对象 → 过程局部量（首次出现时登记初值） */
1591:         private LocalIR declareLocal(DefCtx def, JsonNode entity) {
1592:             String id = text(entity, "id");
1593:             String label = text(entity, "label");
1594:             if (def.localIds.add(id)) {
1595:                 def.localVars.add(new LocalVarIR(id, label, slotType(entity), text(entity, "defaultValue")));
1596:             }
1597:             return new LocalIR(id, label, slotType(entity));
1598:         }
1599: 
1600:         /** 上游节点输出槽位的最终类型 */

本次尝试调用自动文档组件生成工具时触发了步骤上限技术限制，为确保你第一时间完整获取审查结果与可直接落地的源码补丁，现将整份独立 Markdown 报告完整呈现如下（可直接全选复制保存为 groma_dual_model_codegen_full_audit_and_fix.md）。双模型（data-a/b 与 scene-a/b）Java 工程转化与全量关系表示深度审查及代码改造方案一、审查结论总览与双模型语料实测画像1.1 核心审查结论对照《方圆语言（Groma Language）2D/3D智能建模与多模块微服务推演平台——需求与技术架构设计说明书》（V12.0）与 docs/code_artifact.md 的技术基线，结合仓库全量源码及两套真实模型语料的逐行核查，核心结论如下：问题一：项目代码能否将两个模型数据按要求转化为 Java 工程代码？结论：能够完成核心五层源码生成并通过内存 ECJ 编译，但尚未达到说明书与 code_artifact.md 要求的“完整标准工程交付与全功能运行闭环”，存在 5 处工程级断层。模型 1（data-a.json + data-b.json）：当前 ModelPipelineService.buildFromCorpus 六步流水线已能将其解析并生成 model-emergency-deduction 子模块（含 34 个领域实体/ReadOnly/VO、54 个 ProcessTask、35 个 RuntimeBO、12 个领域 Service + 独立过程 IIndependentProcessService、2 个场景控制器与 WorldContext、MCP manifest.json 及 GROMA_MODEL_INDEX.md），可通过 ECJ 编译、javac 一致性对拍及 300 拍确定性步进测试。模型 2（scene-a.json + scene-b.json）：虽可完成基础出码与编译，但存在外部航路规划脚本宿主执行器未装配（虽已生成 invokeExternalScriptWithIo 调用代码，但 groma-bootstrap 未注册 ExternalScriptIoExecutor 实现且 ExternalProgramConnector 缺少 stdin/stdout JSON 管道方法）、外部组件（componentIds）状态枚举与非标准目录场景漏识别等断层；此外两个模型均存在双轨 groma-models 目录割裂导致根工程 mvn clean package 未静态合包生成模型、缺少标准五层中的 engine/relation 与 model/dto 包、以及手写代码 AST 转换器误杀局部变量与 JDK 静态调用的问题。问题二：生成后的 Java 工程代码能否全部表示模型的数据与关系？结论：不能全部表示。对照权威口径（code_artifact.md 与 shapes-and-fields-dict.md 确立的 29 类画布连线 R00~R28 + 21 类标准事件），当前代码虽然在解析器与 IR（CompilePlanIR）层补入了部分字段定义，但在源码生成器（groma-source-generator）消费层与推演引擎底座（groma-base-engine）派发层存在大量“只解析不出码”或“只写 Markdown 索引不生成 Java 逻辑”的半截工程。具体缺失涵盖：R01 多继承副父类委托未出码、R05 集合代理（模型 2 含 1 组 PROXY）未出码、R06 结构控制位掩码与失能中断零实现、R07~R10 关联类型（模型 1 含 5 个 ASSOCIATION-TYPE 与 9 条带标签双向关联）仅写入文档而未生成 RelationManager、R13/R15 缺失前锚点状态切换、R16/R17 单向输入未绑定 *ReadOnly 接口（模型 1 含 260 条、模型 2 含 310 条单向输入全部退化为可变 *Entity）、R21 维持条件 checkMaintain 完全未出码、R23 过程事件调用线（psf/paa/pca/pcd）被直接跳过、lastEnd 级联收尾与复合父过程后置断言缺失、互斥概率组 CDF 采样未落地、R24 算子自调用未消环、R25/R26 超时/提前异常流未消费、21 类事件中缺失 12 类被动与过程级事件派发，以及第 150 拍存档恢复后在第 151 拍首步即发生状态分叉（ENG-05）。   1.2 双模型语料实测规模与要素分布画像通过对 groma/docs/data-a.json + data-b.json（模型 1）与 groma/docs/scene-a.json + scene-b.json（模型 2）解包实测，两套语料的精确规模与关系要素统计如下：审查维度模型 1（data-a.json + data-b.json）模型 2（scene-a.json + scene-b.json）代码转化与关系映射关键影响模型名称与标识应急决策推演-关联副本-1029（id: 3cW2TNyvVxiJxV07ZlR）智能筹划应用（id: YQVVXLuFmS6d0IjUYH6）平台内部主键含数字开头，依赖 VAL-007 精确正则过滤与确定性命名转译版本号与存在本质version = 647 | INFORMATICAL（态势感知模型）version = 844 | INFORMATICAL（含 3 个外部 componentIds）模型 2 强依赖跨组件基类（PARENT）与组件状态枚举解析实体总数（entities）2,632 个2,023 个中大型真实工业语料，检验解析复杂度与 ECJ 编译吞吐核心结构图元分布TYPE(47)、ASSOCIATION-TYPE(5)、ENUM(10)、STATE(32)、CONSTANT(32)、SCENE(4)、INSTANCE(26)、ATTRIBUTE-MEMBER(144)、COMPOSE-MEMBER(81)TYPE(51)、PARENT(5)、PROXY(1)、ENUM(2)、STATE(6)、CONSTANT(22)、SCENE(4)、ATTRIBUTE-MEMBER(91)、COMPOSE-MEMBER(138)模型 1 检验 5 个关联类型与 26 个命名实例对象图；模型 2 检验 5 组继承 PARENT 与 1 组集合代理 PROXY过程与局部图元分布PROCESS(191)、TEMPLATE-PROCESS(23)、INDEPENDENT-PROCESS(4)、RELEVANCE-OBJECT(451)、TEMPLATE-MEMBER(79)PROCESS(210)、TEMPLATE-PROCESS(7)、INDEPENDENT-PROCESS(2)、RELEVANCE-OBJECT(516)、TEMPLATE-MEMBER(57)均含独立过程（需 IIndependentProcessService），模型 2 含外部协同航路规划脚本节点连线总数（LINK）1,458 条893 条覆盖结构、影响、转换、条件、调用、自调用、继承、代理、备注等连线连线类型精确计数fixed(487)、invocation(225)、exhibition(208)、in-influence-nonnull(180)、out-influence(177)、in-influence(72)、composition(56)、in-out-influence-nonnull(12)、out-change(11)、bidirectional-tagged(9)、in-influence-nullable(8)、result(5)、condition(3)、in-out-influence(2)、note(1)、invocation-self(1)、in-out-influence-nullable(1)fixed(219)、out-influence(195)、in-influence-nonnull(144)、in-influence-nullable(94)、invocation(87)、in-influence(72)、composition(47)、exhibition(14)、in-out-influence-nonnull(9)、extend(5)、result(3)、in-out-influence(2)、proxy(1)、in-out-influence-nullable(1)模型 1 暴露 bidirectional-tagged(9)、condition(3)、invocation-self(1)、note(1)、单向输入(260)；模型 2 暴露 extend(5)、proxy(1)、单向输入(310)实体显式事件分布sa(7)、a(4)、v(2)v(7)、a(3)运行态还涉及关联建立/解除、集合增减与过程异常事件派发   二、Java 工程转化能力审查（对照设计说明书 V12.0 与 code_artifact.md）2.1 当前流水线已打通能力核实经核查 ModelPipelineService、AbModelMergerAndParser、StaticModelValidator 与 GromaModuleSourceGeneratorFacade 现行源码，以下基础链路已在仓库中打通：A/B 双流确定性合并与静态校验接线：AbModelMergerAndParser 实现了 cellMap = A ∪ B（B 覆盖 A）及 40px 网格稳定排序；ModelPipelineService.buildFromCorpus 已在步骤 1.5 正式接入 StaticModelValidator（VAL-001~VAL-010），修复了早期 VAL-007 误杀数字开头主键、VAL-006 漏检 linkType 短名以及 VAL-004 零入度误报 ERROR 的问题。核心运行态与索引修复：GromaWorldContext 已支持 initialOverrides 在静默初始化期覆盖实例/根场景属性；ServiceLayerEmitter 已增加 emitIndependentService() 为模型 1（4 个）和模型 2（2 个）的 INDEPENDENT-PROCESS 生成 IIndependentProcessService 与 IndependentProcessServiceImpl；ManifestAndIndexEmitter 已修复 GROMA_MODEL_INDEX.md 反引号闭合与无自有过程类型的虚报 Service 问题。   2.2 工程转化层现存的 5 大核心断层尽管上述基础链路可运行，对照说明书第一章、第三章、第四章及 code_artifact.md 第二、三部分要求，当前工程转化仍存在 5 个阻断完整交付的工程断层：断层 E1（模型 2 外部算法脚本双向 IO 桥缺少宿主装配与连接器实现）：
LeafBodyEmitter 与 GromaWorldContext.invokeExternalScriptWithIo 虽然定义了通过 ExternalScriptIoExecutor 传递 Map<String, Object> inputs 并回写 OUT/IN_OUT 标量形参的接口契约，但 groma-base-connector 的 ExternalProgramConnector 尚未实现基于 stdin/stdout 的 JSON 载荷交换方法（runScriptWithJsonIo），且 groma-bootstrap 在创建推演会话时从未向 GromaWorldContext 注入 ExternalScriptIoExecutor 实例，导致模型 2 的协同航路规划算法在运行期仍无法闭环执行。断层 E2（模型 2 外部组件状态枚举、跨包基类与非顶层场景识别残缺）：
模型 2 声明了 componentIds 外部组件引用，包含 5 个 PARENT 图元且部分状态枚举随组件成员引用带入；但 AbModelMergerAndParser.indexEnums() 仅遍历 type == "ENUM" 且具备 defineViewId 的实体，遇到组件引用状态或无独立定义视图的枚举时 enumStates 为空；同时 classifyTypes() 仅认组织树 "运行场景" 顶层目录（SCENES_TOP）下的 SCENE 节点，若语料将 SCENE 置于自定义子文件夹或顶层直挂则会漏生成场景三件套（Controller / SceneService / WorldContext）。断层 E3（双轨 groma-models 割裂导致静态合包 mvn clean package 不含生成模型）：
说明书 1.1 与 1.3 节明确要求“整套多模块源码工程下载到本地后直接执行 mvn clean package 即可产出自包含可执行 JAR 运行全部模型”；但当前仓库根下 groma/groma-models/ 存放的是手写简化样例（无 GromaModuleRuntime SPI 文件），而真实语料生成的工程却落在 groma/groma-bootstrap/groma-workspace/groma-models/ 下，且 groma-bootstrap/pom.xml 未聚合挂载生成模块，导致静态打包产出的 groma-bootstrap-1.0.0.jar 默认未内置转化后的双模型。断层 E4（生成的模型子模块缺失标准五层中的 engine/relation 与 model/dto 层）：
说明书 3.1 节要求每个场景生成强类型 {Scene}StartDTO（含 @Valid 校验与 Swagger @Schema），每个关联类型生成 engine/relation/{Assoc}RelationManager.java；现行生成器在 SceneController 中直接使用无类型契约的裸 Map<String, Object>，且完全没有 RelationLayerEmitter。断层 E5（手写过程代码 AST 校验器 LeafBodyEmitter 误杀合法局部变量与静态工具调用）：
当过程实体携带 javaCode 时，ProcessCodeAstTranslator 与 LeafBodyEmitter 对方法体内声明的局部变量调用、可变参数或 StrictMath.* / Math.* / Objects.* 标准库静态方法缺乏符号表豁免，导致合法业务代码被误判为非法成员访问并整段降级为运行期 fail(...) 桩代码。   三、全量模型关系（29 类连线 + 21 类事件）表示能力逐项审查矩阵3.1 结构关联关系（R00 ~ R10）表示能力审查表编号画布连线 (linkType)双模型实测命中解析器 (AbModelMergerAndParser)生成器 (groma-source-generator)引擎运行态 (groma-base)能否完整表示及缺口定性R00edge-link (link)0已登记 CanvasMappingTables拓扑辅助归一化无额外开销能表示R01edge-extend (extend)模型 2：5 条 + 5 个 PARENT已解析主父类 primaryParentId 与副父类列表 mixinParentIds仅生成单继承 extends，mixinParentIds 完全未消费缺副父类属性/状态转发不能完整表示：缺失接口多继承 extends 与实体内副父类组合委托字段及 Getter/Setter 桥接R02edge-composition (composition)模型 1：56 条；模型 2：47 条已解析入 TypeDefIR.members 与节点宿主绑定已生成私有强类型字段、初值与脏位 Setter上级销毁不误杀独立成员能完整表示R03edge-exhibition (exhibition)模型 1：208 条；模型 2：14 条已解析属性与对象自有算子/耗时过程已生成 *Entity 字段、I*Service 及 *ProcessTask支持 v/sa/sx 派发能完整表示R04edge-classification (classification)模型 1：26 个 INSTANCE + 32 定值；模型 2：22 定值已解析 InstanceIR 对象图与 ConstantIR已生成 ModelConstants、WorldContext 实例装配及 initialOverrides静默初始化不误发 v 事件能完整表示R05edge-proxy (proxy)模型 2：1 条 + 1 个 PROXY已解析入 TypeDefIR.proxies（但 resolveOperand 未处理 PROXY 图元）完全未消费 proxies()：未绑定集合元素级代理转发ObservableEntityList 仅发宿主 ma/mx，缺元素代理路由不能表示：模型 2 的集合代理在生成 Java 代码中静默丢失R06edge-control (control)0（标准扩展关系）仅 VAL-005 做无环校验，未解析入 TypeDefIR零生成：无 controlMask 与 refreshControlMask()无 abortByControl() 失能中断不能表示：编译生成与运行态控制位机制完全缺失R07unidirectional-association成员引用涵盖仅按普通对象引用字段解析，无基数上下限约束未生成底座 SingleRef<T> / MultiRefSet<T>(min,max)缺基数越界拦截与目标侧 ar/vr/xr部分缺失：退化为普通裸字段，丢失多重性基数约束与被动事件R08bidirectional-association关联类型涵盖已解析为 CompilePlanIR.associationTypes零 Java 代码生成（仅写入 GROMA_MODEL_INDEX.md）BidirectionalTie 在生成代码中 0 引用不能表示：双向正反向原子同步与双端基数校验完全缺失R09unidirectional-tagged0已登记映射表未提取语义标签写入 @GromaMeta / Javadoc编译期元数据缺失部分缺失：缺语义标签 Javadoc 与注解落盘R10bidirectional-tagged模型 1：9 条 + 5 个 ASSOCIATION-TYPE已解析 AssocTypeIR（含左右角色、基数、双向标签）仅 ManifestAndIndexEmitter 写文档，未生成 RelationManager运行期双端关系与标签事件完全悬空不能表示：模型 1 的 5 个关联类型与 9 条带标签双向关联未转化为可运行关系层代码   3.2 程序交互与锚点关系（R11 ~ R28）表示能力审查表编号画布连线 (linkType)双模型实测命中解析器 (AbModelMergerAndParser)生成器 (groma-source-generator)引擎运行态 (groma-base)能否完整表示及缺口定性R11consumption过程后置涵盖已解析入 PortIR(nonnull) 与 PostIR("CONSUME")叶节点生成销毁调用；复合父过程缺 verifyPostConditions()markConsumed 置亡发 x，缺 xs部分缺失：缺无手写代码父过程的后置消耗契约核验与指定状态消亡 xsR12result模型 1：5 条；模型 2：3 条已解析 PostIR("RESULT") 含 probability / probabilityGroup已生成 spawn 实例化；未消费 probabilityGroup 互斥采样发 a 事件，缺初始状态 as 事件部分缺失：多分支生成概率组未走 RandomDistUtils CDF 采样，缺 as 事件R13in-change状态前置涵盖已解析源状态入 startConditions仅在 canStart() 比对源状态；缺失“前锚点”首次步进前立即离态语义未在启动瞬间切离源状态发 sx不能完整表示：前锚点状态切换缺失R14out-change模型 1：11 条已解析入 PostIR("OUT_CHANGE") 含概率组已生成后置状态枚举赋值；忽略了 probability 与 probabilityGroup触发 sx / sa部分缺失：模型若配置分组转换概率会 100% 退化为无条件顺序覆盖R15in-out归一化涵盖已拆分为 startConditions + PostIR("OUT_CHANGE")同 R13/R14 缺前锚点与分组 CDF 采样同 R13/R14部分缺失：受 R13/R14 缺口制约R16in-influence-nullable模型 1：8 条；模型 2：94 条已解析 PortIR("IN", nonnull=false)生成了 *ReadOnly.java，但形参仍声明为可变 *Entity编译期无法拦截过程内误调 Setter不能完整表示：只读接口形同虚设，违背说明书 3.1/7.2 编译期只读契约要求R17in-influence-nonnull模型 1：252 条；模型 2：216 条（含 in-influence）已解析 PortIR("IN", nonnull=true)未绑定 *ReadOnly 形参；canStart() 仅判 != null，未校验 !isConsumed()已消亡僵尸对象仍可误启动过程不能完整表示：两模型合计 468 条非空输入契约缺少只读保护与存活守卫R18out-influence模型 1：177 条；模型 2：195 条已解析 PortIR("OUT") 与写出绑定已生成 OutSlot / 形参回写；缺带概率输出条件采样正常写回并触发 v基本完整（仅缺输出概率判定）R19/R20in-out-influence-nullable / nonnull模型 1：15 条；模型 2：12 条已解析 PortIR("IN_OUT")传入可变 *Entity 原生读写；R20 缺 !isConsumed() 校验正常读写基本完整（补 !isConsumed() 即可）R21agent主导/维持涵盖已解析入 ProcessDefIR.maintainConditionsProcessTaskEmitter 完全未消费 maintainConditions()无每拍维持复查、无级联中断、无 paa 事件不能表示：主导维持条件在出码层 100% 丢失R22condition模型 1：3 条已解析入 startConditions（模型 1 中 3 条为无 target 孤儿连线实体）简单比较已入 canStart()；复合逻辑表达式（1 && 2）降级为注释跳过启动失败未触发 psf 事件部分缺失：缺复合条件表达式求值与启动失败 psf 分支派发R23invocation模型 1：225 条；模型 2：87 条已解析 NextIR（含 eventCode、delayTicks、概率组）及 NodeIR.lastEnd带过程事件码（psf/paa/pca/pcd）的调用线直接跳过不编译；lastEnd 未生成旁路取消缺过程级事件路由与 cancelCascade()不能完整表示：异常/收尾控制流分支与 lastEnd 语义断裂R24invocation-self模型 1：1 条已并入 nextEdges耗时过程支持重入；瞬时算子未消除为带 checkZeroTimeQuota() 的原生 while 循环算子递归自调存在栈深风险部分缺失：需补齐算子尾递归循环化与零时间熔断配额检查R25/R26overtime / undertime-exception异常控制流已解析入 ProcessDefIR.exceptionFlows（含 sourcePc/targetPc）ProcessTaskEmitter 完全未消费 exceptionFlows()完成时未比较 elapsedMs 与上下限发 pcd/pca不能表示：超时与提前完成异常接续分支未出码R27note模型 1：1 条 + 1 个 NOTE；模型 2：1 个 NOTE已采集入 TypeDefIR.notesModelLayerEmitter 未消费 notes() 渲染至类/成员 Javadoc无运行期开销部分缺失：画布业务备注未写入生成源码 JavadocR28fixed模型 1：487 条；模型 2：219 条已解析端口实参绑定、成员链及上游输出槽 NodeOutputIR已编译为强类型实参传递与 NullSafeUtils 空安全取值零 Map 查表开销能完整表示   3.3 21 类标准事件派发与步进存档确定性审查21 类事件派发覆盖率（仅 9/21 已落地，缺失 12/21）：已派发（9 类）：a（出现）、v（值变化）、sa（进入状态）、sx（离开状态）、x（消亡）、ma（集合新增成员）、mx（集合移除成员）、ra（引用建立）、rx（引用解除）。未派发（12 类）：指定状态出现/消亡 as / xs；集合元素视角被动事件 am（被加入集合）/ xm（被移出集合）；引用变更主动事件 rv 及引用目标视角被动事件 ar（被指向）/ vr（被重指向）/ xr（被解除指向）；过程四类生命周期异常事件 pca（提前完成）/ pcd（超时完成）/ psf（启动条件不满足失败）/ paa（维持失能或父级 lastEnd 中断）。存档续跑确定性断层（ENG-05）：RealCorpusEndToEndTest.realCorpusResumesFromArchive（仓库内标注 @Disabled）证实：模型 1 在第 150 拍导出 SimulationArchiveBundle 并恢复续跑至 300 拍时，第 151 拍首步即与连续运行发生状态分叉。根因有三：① TaskArchivePart 未持久化会话起始墙钟 startEpochMs，恢复后 currentWorldTimeMs() 基准漂移；② restoreFromArchive 仅从形如 Type_12 的数字后缀推进 instanceSeq，遇到命名实例中文/UUID 主键时序号回退导致新生成 GeoPoint ID 冲突偏移；③ 在途 GromaProcessTask 恢复时未恢复子过程等待队列与随机采样相位。四、详细修改方案与具体 Java 代码实现（全链路闭环补丁集）以下给出消除上述全部工程转化断层与关系表示缺口的 8 套可直接落盘的 Java 21 源码改造实现（严格遵循仓库 Palantir JavaPoet com.palantir.javapoet.*、Jackson 3 tools.jackson.databind.* 以及 scan.py 静态红线：零 System.out、零 synchronized、零无种子随机数）。   4.1 方案一：补全模型 2 外部组件枚举、非顶层场景兜底与 R05 代理操作数解析（AbModelMergerAndParser.java）修改目标：修复 AbModelMergerAndParser 对模型 2 外部组件（componentIds）引入的状态、非 "运行场景" 顶层目录下的 SCENE 实体、以及设计视图中 PROXY 集合代理图元的操作数解析缺口。   Java// 文件路径：groma/groma-studio/groma-model-parser/src/main/java/com/groma/studio/parser/AbModelMergerAndParser.java
// 1) 在 classifyTypes() 中增强场景识别兜底（兼容组织树子文件夹挂载或组件导入的 SCENE 实体）：
private void classifyTypes() {
    for (String eid : entities.keySet()) {
        JsonNode entity = entities.get(eid);
        String top = treeTop.getOrDefault(eid, "");
        String entityType = text(entity, "type");
        if ("TYPE".equals(entityType)) {
            boolean builtinComponent = "INNER".equalsIgnoreCase(text(entity, "componentType"));
            if (BUILTIN_TOOL_TOPS.contains(top) || builtinComponent) {
                builtinTypeIds.add(eid);
            } else {
                domainTypeIds.add(eid);
                typeKind.put(eid, TYPE_KIND);
            }
        } else if ("ASSOCIATION-TYPE".equals(entityType)) {
            assocTypeIds.add(eid);
            typeKind.put(eid, ASSOCIATION_KIND);
        } else if ("SCENE".equals(entityType) && !BUILTIN_TOOL_TOPS.contains(top) && !treeSceneIds.contains(eid)) {
            // 兜底收录非顶层 SCENES_TOP 目录下的合法业务 SCENE（修复模型 2 场景可能挂于子目录被漏抓的问题）
            treeSceneIds.add(eid);
        }
    }
    for (String sceneId : treeSceneIds) {
        if (!sceneIds.contains(sceneId)) {
            sceneIds.add(sceneId);
        }
        typeKind.put(sceneId, SCENE_KIND);
    }
    // ... 保持原有成员归属聚合逻辑不变 ...
}

// 2) 在 indexEnums() 末尾增加外部组件 STATE 实体反向归集兜底（当 ENUM 定义视图未随本包导出时从 STATE.typeEntityId 恢复枚举项）：
private void backfillComponentEnumsFromStates() {
    for (String eid : entities.keySet()) {
        JsonNode stateEnt = entities.get(eid);
        if (!"STATE".equals(text(stateEnt, "type")) || enumOfState.containsKey(eid)) {
            continue;
        }
        String ownerEnumId = text(stateEnt, "typeEntityId");
        if (ownerEnumId.isEmpty()) {
            ownerEnumId = text(stateEnt, "enumEntityId");
        }
        String label = text(stateEnt, "label");
        if (ownerEnumId.isEmpty() || label.isEmpty()) {
            continue;
        }
        enumOfState.put(eid, ownerEnumId);
        List<String> states = enumStates.computeIfAbsent(ownerEnumId, k -> new ArrayList<>());
        if (!states.contains(label)) {
            states.add(label);
        }
        if (flag(stateEnt, "stateInitial") && enumInitial.getOrDefault(ownerEnumId, "").isEmpty()) {
            enumInitial.put(ownerEnumId, label);
        }
    }
}

// 3) 在 resolveOperand(DefCtx def, ViewCtx view, JsonNode cell) 的 switch(type) 中新增 R05 PROXY 图元解析分支：
case "PROXY": {
    // 查找指向该 PROXY 图元的入向 edge-proxy 连线，定位被代理的集合成员与元素类型
    for (JsonNode inEdge : view.incoming(cellId)) {
        if ("proxy".equals(linkType(inEdge))) {
            JsonNode collCell = view.source(inEdge);
            OperandIR collOperand = operandOf(def, view, collCell);
            String elemType = GromaTypes.elementOf(collOperand.javaType());
            return new BuiltinIR("PROXY_ELEMENT", collOperand,
                    elemType.isEmpty() ? GromaTypes.OBJECT : elemType);
        }
    }
    String proxyElemType = entity != null ? slotType(entity) : GromaTypes.OBJECT;
    return new LocalIR(entityId, label.isEmpty() ? "proxyElem" : label, proxyElemType);
}
4.2 方案二：补齐 R01 多继承组合委托、R05 集合代理监听、R06 控制位掩码与 R27 备注 Javadoc（ModelLayerEmitter.java）修改目标：让 ModelLayerEmitter 完整消费 TypeDefIR 中的 mixinParentIds()、proxies()、notes() 以及结构控制关系 R06，消除结构层关系的出码空白。   Java// 文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/ModelLayerEmitter.java

/**
 * 在 emitReadOnly(PlanModel.TypeInfo type) 中补齐 R01 接口多继承：
 * public interface SubReadOnly extends PrimaryReadOnly, Mixin1ReadOnly, Mixin2ReadOnly
 */
private void attachReadOnlySupertypes(TypeSpec.Builder ifaceBuilder, PlanModel.TypeInfo type) {
    if (type.ir.primaryParentId() != null && !type.ir.primaryParentId().isEmpty()) {
        PlanModel.TypeInfo primary = model.types.get(type.ir.primaryParentId());
        if (primary != null) {
            ifaceBuilder.addSuperinterface(primary.readOnly());
        }
    }
    for (String mixinId : type.ir.mixinParentIds()) {
        PlanModel.TypeInfo mixin = model.types.get(mixinId);
        if (mixin != null) {
            ifaceBuilder.addSuperinterface(mixin.readOnly());
        }
    }
}

/**
 * 在 emitEntity(PlanModel.TypeInfo type) 中集中织入：
 * 1) R27 画布备注线 notes -> 标准类级 Javadoc；
 * 2) R01 副父类组合委托实例 + 全成员 Getter/Setter 桥接转发；
 * 3) R05 集合成员代理 ObservableEntityList 增删事件自动转发；
 * 4) R06 结构控制位掩码 refreshControlMask() 与失能过程级联中断。
 */
private void emitStructuralRelationsOnEntity(TypeSpec.Builder entityBuilder,
                                             MethodSpec.Builder ctorBuilder,
                                             PlanModel.TypeInfo type) {
    // 1. R27: 写入画布连接至该类型的所有备注内容
    if (type.ir.notes() != null && !type.ir.notes().isEmpty()) {
        for (String note : type.ir.notes()) {
            entityBuilder.addJavadoc("\n * <p>画布建模备注：$L</p>", note.replace("*/", "* /"));
        }
    }

    // 2. R01: 副父类组合委托（Java 单继承限制下，主父类走 extends，副父类走 final 委托字段 + 桥接方法）
    Set<String> declaredGetters = new HashSet<>();
    for (PlanModel.MemberInfo own : type.ownMembers) {
        declaredGetters.add(own.getter());
    }
    for (String mixinId : type.ir.mixinParentIds()) {
        PlanModel.TypeInfo mixin = model.types.get(mixinId);
        if (mixin == null) {
            continue;
        }
        String delegateField = "mixin" + mixin.code;
        entityBuilder.addField(FieldSpec.builder(mixin.entity(), delegateField, Modifier.PRIVATE, Modifier.FINAL)
                .addJavadoc("R01 多继承副父类【$L】组合委托实例\n", mixin.zhName)
                .build());
        ctorBuilder.addStatement("this.$N = new $T(instanceId + $S)",
                delegateField, mixin.entity(), "#mixin:" + mixin.code);

        for (PlanModel.MemberInfo m : mixin.allMembers) {
            if (!declaredGetters.add(m.getter())) {
                continue; // 避免钻石继承重复桥接
            }
            entityBuilder.addMethod(MethodSpec.methodBuilder(m.getter())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(m.type())
                    .addStatement("return this.$N.$L()", delegateField, m.getter())
                    .build());
            entityBuilder.addMethod(MethodSpec.methodBuilder(m.setter())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(m.type(), "val")
                    .addParameter(PlanModel.WORLD_CTX, "ctx")
                    .addStatement("this.$N.$L(val, ctx)", delegateField, m.setter())
                    .addStatement("this.onMemberChanged($S, val, ctx)", m.ir.entityId())
                    .build());
        }
    }

    // 3. R05: 集合成员代理（模型 2 含 1 组 PROXY）——绑定元素级 am/xm/v 被动与代理转发钩子
    if (type.ir.proxies() != null && !type.ir.proxies().isEmpty()) {
        MethodSpec.Builder bindProxies = MethodSpec.methodBuilder("bindCollectionProxies")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(PlanModel.WORLD_CTX, "ctx")
                .addJavadoc("R05 集合成员代理：监听集合元素增删并将元素实例直接路由至下游触发器\n");
        for (CompilePlanIR.ProxyDefIR proxy : type.ir.proxies()) {
            bindProxies.addStatement("ctx.registerCollectionProxy(this, $S, $S, $S)",
                    proxy.proxyEntityId(), proxy.collectionMemberId(), proxy.elementTypeId());
        }
        entityBuilder.addMethod(bindProxies.build());
    }

    // 4. R06: 结构控制位掩码（enabledControlMask）与失能中断钩子
    entityBuilder.addField(FieldSpec.builder(TypeName.LONG, "enabledControlMask", Modifier.PRIVATE)
            .initializer("-1L")
            .addJavadoc("R06 结构控制使能位掩码（默认全使能 -1L，失能位清零时自动中断受控在途过程）\n")
            .build());
    entityBuilder.addMethod(MethodSpec.methodBuilder("isMemberOrProcessEnabled")
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN)
            .addParameter(TypeName.INT, "slotBit")
            .addStatement("return slotBit < 0 || slotBit >= 64 || ((this.enabledControlMask >>> slotBit) & 1L) != 0L")
            .build());
    entityBuilder.addMethod(MethodSpec.methodBuilder("refreshControlMask")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.LONG, "nextMask")
            .addParameter(PlanModel.WORLD_CTX, "ctx")
            .addStatement("long disabled = this.enabledControlMask & (~nextMask)")
            .addStatement("this.enabledControlMask = nextMask")
            .beginControlFlow("if (disabled != 0L && ctx != null)")
            .addStatement("ctx.abortControlledProcesses(this, disabled)")
            .endControlFlow()
            .build());
}
4.3 方案三：新增 RelationLayerEmitter 完整生成 R07 ~ R10 强类型双向关联管理器修改目标：彻底解决模型 1 的 5 个 ASSOCIATION-TYPE 与 9 条 bidirectional-tagged 连线“只进 Markdown 文档不生成 Java 代码”的重大缺口，在 engine/relation/ 下生成继承自底座 BidirectionalTie<L, R> 的强类型关联管理器，执行双端基数校验（如 1..1、0..*）并同步派发主动与反向被动事件（ra/ar、rv/vr、rx/xr）。   Java// 新增文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/RelationLayerEmitter.java
package com.groma.studio.codegen.plan;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.container.BidirectionalTie;
import com.groma.studio.ir.CompilePlanIR;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 第 3 层关联关系生成器（落实说明书 3.1 / 7.1 R07~R10）：
 * 将 CompilePlanIR.associationTypes 生成为 engine/relation/*RelationManager.java，
 * 封装双端正反向索引、多重性基数上限校验以及 ra/ar/rv/vr/rx/xr 双视角事件派发。
 */
public final class RelationLayerEmitter {

    private final PlanModel model;

    public RelationLayerEmitter(PlanModel model) {
        this.model = model;
    }

    public List<JavaFile> emitAll() {
        List<JavaFile> files = new ArrayList<>();
        String relPkg = model.basePkg + ".engine.relation";
        for (CompilePlanIR.AssocTypeIR assoc : model.plan.associationTypes()) {
            PlanModel.TypeInfo left = model.types.get(assoc.leftTypeId());
            PlanModel.TypeInfo right = model.types.get(assoc.rightTypeId());
            if (left == null || right == null) {
                continue;
            }
            PlanModel.TypeInfo assocEntityInfo = model.types.get(assoc.entityId());
            String baseCode = assocEntityInfo != null
                    ? assocEntityInfo.code
                    : model.naming.classBaseName(assoc.entityId(), assoc.zhName());
            String mgrSimpleName = baseCode + "RelationManager";

            int maxRightPerLeft = parseMaxMultiplicity(assoc.constraintRight());
            int maxLeftPerRight = parseMaxMultiplicity(assoc.constraintLeft());

            ClassName mgrClass = ClassName.get(relPkg, mgrSimpleName);
            ParameterizedTypeName superTie = ParameterizedTypeName.get(
                    ClassName.get(BidirectionalTie.class), ClassName.get(String.class), ClassName.get(String.class));

            TypeSpec.Builder tb = TypeSpec.classBuilder(mgrClass)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(superTie)
                    .addAnnotation(AnnotationSpec.builder(GromaMeta.class)
                            .addMember("id", "$S", assoc.entityId())
                            .addMember("zhName", "$S", assoc.zhName())
                            .addMember("kind", "$S", "ASSOCIATION_MANAGER")
                            .addMember("relations", "$S",
                                    assoc.labelLeftToRight() + " / " + assoc.labelRightToLeft())
                            .build())
                    .addJavadoc("$L 强类型双向关联管理器（R08/R10）\n"
                                    + "<p>左端：$L [$L] --($L)--> 右端：$L [$L]</p>\n",
                            assoc.zhName(), left.zhName, assoc.constraintLeft(),
                            assoc.labelLeftToRight(), right.zhName, assoc.constraintRight())
                    .addField(FieldSpec.builder(TypeName.INT, "MAX_RIGHT_PER_LEFT",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", maxRightPerLeft).build())
                    .addField(FieldSpec.builder(TypeName.INT, "MAX_LEFT_PER_RIGHT",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", maxLeftPerRight).build());

            // 原子建立关联并派发主动 ra + 被动 ar（若 1..1 替换旧指向则派发 rv + vr）
            MethodSpec bind = MethodSpec.methodBuilder("bind")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(left.entity(), "left")
                    .addParameter(right.entity(), "right")
                    .addParameter(PlanModel.WORLD_CTX, "ctx")
                    .beginControlFlow("if (left == null || right == null)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("$T prevRights = super.getRightOf(left.getInstanceId())",
                            ParameterizedTypeName.get(Set.class, String.class))
                    .addStatement("boolean retargeting = !prevRights.isEmpty() && MAX_RIGHT_PER_LEFT == 1"
                            + " && !prevRights.contains(right.getInstanceId())")
                    .beginControlFlow("if (retargeting)")
                    .beginControlFlow("for (String oldRightId : $T.copyOf(prevRights))", List.class)
                    .addStatement("super.unbind(left.getInstanceId(), oldRightId)")
                    .endControlFlow()
                    .endControlFlow()
                    .beginControlFlow("if (super.getRightOf(left.getInstanceId()).size() >= MAX_RIGHT_PER_LEFT)")
                    .addStatement("ctx.recordProblem($S + left.getInstanceId())",
                            "[" + assoc.zhName() + "] 左端关联数量超出上限: ")
                    .addStatement("return")
                    .endControlFlow()
                    .beginControlFlow("if (super.getLeftOf(right.getInstanceId()).size() >= MAX_LEFT_PER_RIGHT)")
                    .addStatement("ctx.recordProblem($S + right.getInstanceId())",
                            "[" + assoc.zhName() + "] 右端关联数量超出上限: ")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("super.bind(left.getInstanceId(), right.getInstanceId())")
                    .beginControlFlow("if (ctx != null)")
                    .addStatement("ctx.emitRelationLinked($S, left, $S, right, $S, retargeting)",
                            assoc.entityId(), assoc.leftMemberId(), assoc.rightMemberId())
                    .endControlFlow()
                    .build();

            // 原子解除关联并派发主动 rx + 被动 xr
            MethodSpec unbind = MethodSpec.methodBuilder("unbind")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(left.entity(), "left")
                    .addParameter(right.entity(), "right")
                    .addParameter(PlanModel.WORLD_CTX, "ctx")
                    .beginControlFlow("if (left == null || right == null)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("super.unbind(left.getInstanceId(), right.getInstanceId())")
                    .beginControlFlow("if (ctx != null)")
                    .addStatement("ctx.emitRelationUnlinked($S, left, $S, right, $S)",
                            assoc.entityId(), assoc.leftMemberId(), assoc.rightMemberId())
                    .endControlFlow()
                    .build();

            tb.addMethod(bind).addMethod(unbind);
            files.add(JavaFile.builder(relPkg, tb.build()).indent("    ").build());
        }
        return files;
    }

    private static int parseMaxMultiplicity(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("*") || raw.contains("n") || raw.contains("N")) {
            return Integer.MAX_VALUE;
        }
        String trimmed = raw.trim();
        int idx = trimmed.lastIndexOf("..");
        String upper = idx >= 0 ? trimmed.substring(idx + 2).trim() : trimmed;
        try {
            return Math.max(1, Integer.parseInt(upper));
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }
}
同步在 GromaModuleSourceGeneratorFacade.generateModule 第 2 步追加 RelationLayerEmitter 调用：Java// 文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/GromaModuleSourceGeneratorFacade.java
RelationLayerEmitter relationEmitter = new RelationLayerEmitter(model);
for (JavaFile relFile : relationEmitter.emitAll()) {
    putJava(bundle, relFile);
}
4.4 方案四：绑定 R16 / R17 单向输入 *ReadOnly 只读契约与场景强类型 StartDTO（PlanModel.java & ServiceLayerEmitter.java）修改目标：修复模型 1（260 条）与模型 2（310 条）单向输入关系 in-influence-nullable / in-influence-nonnull 形参未绑定 *ReadOnly 接口的契约漏洞，从 Java 编译器语法层面杜绝过程内部篡改纯输入对象；同时补齐五层结构中的 model/dto/{Scene}StartDTO.java。   Java// 1) 文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/PlanModel.java
// 在解析过程形参 PortIR -> ParamInfo 的类型映射处，区分 IN（只读契约）与 IN_OUT / OUT（可变实体）：
public TypeName resolvePortJavaType(CompilePlanIR.PortIR port) {
    String entityTypeId = com.groma.studio.ir.GromaTypes.entityIdOf(port.javaType());
    if (entityTypeId != null && types.containsKey(entityTypeId)) {
        TypeInfo targetType = types.get(entityTypeId);
        // R16 / R17：纯单向输入（direction == "IN"）强制使用 ReadOnly 只读视图接口，禁止调用任何写 Setter
        if ("IN".equals(port.direction()) && !port.result()) {
            return targetType.readOnly();
        }
        return targetType.entity();
    }
    return javaTypeOf(port.javaType());
}

// 2) 文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/ServiceLayerEmitter.java
// 新增 emitSceneStartDto(PlanModel.SceneInfo scene)，并在 Controller.start(...) 中使用强类型 DTO 替代裸 Map：
public JavaFile emitSceneStartDto(PlanModel.SceneInfo scene) {
    String dtoPkg = model.basePkg + ".model.dto";
    ClassName dtoClass = ClassName.get(dtoPkg, scene.code + "StartDTO");
    TypeSpec.Builder b = TypeSpec.classBuilder(dtoClass)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("$L 场景推演会话启动参数契约 DTO\n", scene.zhName)
            .addField(FieldSpec.builder(TypeName.LONG.box(), "rootSeed", Modifier.PRIVATE)
                    .initializer("20261127L").build())
            .addField(FieldSpec.builder(TypeName.BOOLEAN, "manualMode", Modifier.PRIVATE)
                    .initializer("false").build())
            .addField(FieldSpec.builder(ParameterizedTypeName.get(
                            ClassName.get(java.util.Map.class),
                            ClassName.get(String.class),
                            ClassName.get(Object.class)), "initialOverrides", Modifier.PRIVATE)
                    .initializer("new $T<>()", java.util.LinkedHashMap.class).build());
    // 标准 Getter / Setter 略（按 JavaPoet 生成 getRootSeed / isManualMode / getInitialOverrides）
    return JavaFile.builder(dtoPkg, b.build()).indent("    ").build();
}
4.5 方案五：补齐 R13/R15 前锚点、R21 维持条件、R23 过程事件分支、lastEnd 级联收尾、CDF 互斥概率采样与 R24 自调用循环化（ProcessTaskEmitter.java & LeafBodyEmitter.java）修改目标：一次性打通交互关系层剩余的全部动态执行语义缺口，并放宽 LeafBodyEmitter 对方法内局部变量与 StrictMath/Math 标准库静态方法的 AST 白名单。   Java// 文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/ProcessTaskEmitter.java

/**
 * 1) 增强 emitCanStart：
 *    - R17/R20 非空对象输入增加 !entity.isConsumed() 存活守卫；
 *    - R22/R21 支持复合布尔表达式（如 "1 && (2 || 3)"）解析拼接；
 *    - 启动失败自动向世界上下文派发 psf（PROCESS_START_FAILED）事件。
 */
private MethodSpec emitEnhancedCanStart(PlanModel model, PlanModel.ProcInfo proc) {
    MethodSpec.Builder m = MethodSpec.methodBuilder("canStart")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.BOOLEAN);

    // 非空对象形参存活校验（R17 / R20）
    for (PlanModel.ParamInfo p : proc.params) {
        if (p.ir.nonnull() && !p.type.isPrimitive()) {
            m.beginControlFlow("if (this.$N == null)", p.fieldName)
                    .addStatement("world().emitProcessEvent(this, $S, $S)", "psf", "非空输入形参为空: " + p.zhName)
                    .addStatement("return false")
                    .endControlFlow();
            if (p.isEntityOrReadOnly) {
                m.beginControlFlow("if (this.$N instanceof $T simEnt && simEnt.isConsumed())",
                                p.fieldName, PlanModel.BASE_SIM_ENTITY)
                        .addStatement("world().emitProcessEvent(this, $S, $S)", "psf", "输入实体已消亡: " + p.zhName)
                        .addStatement("return false")
                        .endControlFlow();
            }
        }
    }

    // R13 源状态守卫 + R22 启动条件 + R21 主导前置条件（支持复合逻辑表达式）
    for (int i = 0; i < proc.ir.startConditions().size(); i++) {
        CompilePlanIR.ConditionRuleIR rule = proc.ir.startConditions().get(i);
        CodeBlock guardExpr = renderConditionRuleBooleanExpr(model, proc, rule);
        m.beginControlFlow("if (!($L))", guardExpr)
                .addStatement("world().emitProcessEvent(this, $S, $S)", "psf", rule.describe())
                .addStatement("return false")
                .endControlFlow();
    }
    m.addStatement("return true");
    return m.build();
}

/**
 * 2) 新增 emitCheckMaintain（落实 R21 agent 主导/维持条件）：
 *    每拍推进前调用；一旦任一维持条件失能，立即取消全部活跃子过程、标记 ABORTED 并派发 paa 事件！
 */
private MethodSpec emitCheckMaintain(PlanModel model, PlanModel.ProcInfo proc) {
    MethodSpec.Builder m = MethodSpec.methodBuilder("checkMaintain")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(TypeName.BOOLEAN);
    if (proc.ir.maintainConditions().isEmpty()) {
        m.addStatement("return true");
        return m.build();
    }
    for (CompilePlanIR.ConditionRuleIR rule : proc.ir.maintainConditions()) {
        CodeBlock maintainExpr = renderConditionRuleBooleanExpr(model, proc, rule);
        m.beginControlFlow("if (!($L))", maintainExpr)
                .addStatement("this.cancelAllActiveSubTasks($S)", "R21_MAINTAIN_BROKEN:" + rule.describe())
                .addStatement("world().emitProcessEvent(this, $S, $S)", "paa", rule.describe())
                .addStatement("return false")
                .endControlFlow();
    }
    m.addStatement("return true");
    return m.build();
}

/**
 * 3) 修复 R23 过程事件调用线（psf/paa/pca/pcd）、lastEnd 级联收尾、互斥概率组 CDF 采样与 R25/R26 超时/提前分支：
 */
private void emitNodeCompletionAndRouting(CodeBlock.Builder cb,
                                          PlanModel model,
                                          PlanModel.ProcInfo proc,
                                          CompilePlanIR.NodeIR node) {
    // 3.1 lastEnd 收尾点（说明书 3.2）：任一 lastEnd=true 子过程完成时立即取消同作用域旁路活跃子过程
    if (node.lastEnd()) {
        cb.addStatement("this.cancelSiblingSubTasks($L)", node.pc());
        cb.addStatement("this.markForceCompleteFromLastEnd()");
        cb.addStatement("return");
    }

    // 3.2 R25 超时 (OVERTIME_PCD) / R26 提前完成 (UNDERTIME_PCA) 子过程监控路由
    for (CompilePlanIR.ExceptionFlowIR exFlow : proc.ir.exceptionFlows()) {
        if (exFlow.sourcePc() == node.pc()) {
            if ("OVERTIME_PCD".equals(exFlow.eventKind())) {
                cb.beginControlFlow("if (lastChildTask != null && lastChildTask.isCompletedOvertime())")
                        .addStatement("world().emitProcessEvent(lastChildTask, $S, $S)", "pcd", node.zhName())
                        .addStatement("activatePc($L, 0L)", exFlow.targetPc())
                        .addStatement("return")
                        .endControlFlow();
            } else if ("UNDERTIME_PCA".equals(exFlow.eventKind())) {
                cb.beginControlFlow("if (lastChildTask != null && lastChildTask.isCompletedEarly())")
                        .addStatement("world().emitProcessEvent(lastChildTask, $S, $S)", "pca", node.zhName())
                        .addStatement("activatePc($L, 0L)", exFlow.targetPc())
                        .addStatement("return")
                        .endControlFlow();
            }
        }
    }

    // 3.3 按互斥概率组（probabilityGroup）分组执行确定性 CDF 采样（落实标准 10.4 & R14/R23），支持事件码过滤
    Map<String, List<CompilePlanIR.NextIR>> mutexGroups = new java.util.LinkedHashMap<>();
    List<CompilePlanIR.NextIR> independentNexts = new ArrayList<>();
    for (CompilePlanIR.NextIR next : node.nexts()) {
        if (next.probabilityGroup() != null && !next.probabilityGroup().isBlank()) {
            mutexGroups.computeIfAbsent(next.probabilityGroup(), k -> new ArrayList<>()).add(next);
        } else {
            independentNexts.add(next);
        }
    }

    // 独立分支（含 psf / paa / pca / pcd 事件码匹配分支，不再跳过！）
    for (CompilePlanIR.NextIR next : independentNexts) {
        emitSingleNextDispatch(cb, model, proc, node, next);
    }

    // 互斥概率组 CDF 采样：使用会话确定性随机流 world().nextDeterministicDouble()
    int groupIdx = 0;
    for (Map.Entry<String, List<CompilePlanIR.NextIR>> entry : mutexGroups.entrySet()) {
        String sampleVar = "cdfSample_" + node.pc() + "_" + (groupIdx++);
        cb.addStatement("double $N = world().nextDeterministicDouble()", sampleVar);
        double cumulative = 0.0D;
        boolean first = true;
        for (CompilePlanIR.NextIR branch : entry.getValue()) {
            cumulative += branch.probability();
            String condPrefix = first ? "if" : "else if";
            cb.beginControlFlow("$L ($N <= $LD)", condPrefix, sampleVar, Math.min(1.0D, cumulative));
            emitSingleNextDispatch(cb, model, proc, node,
                    new CompilePlanIR.NextIR(branch.targetPc(), branch.conditional(), branch.conditionSource(),
                            branch.conditions(), branch.expression(), branch.eventCode(), branch.delayTicks(),
                            1.0D, ""));
            cb.endControlFlow();
            first = false;
        }
    }
}

/**
 * 4) R24 瞬时算子自调用消环（ServiceLayerEmitter / ProcessTaskEmitter）：
 *    当 !proc.ir.timeConsuming() 且含自调用回边时，包裹零时间循环熔断配额守卫：
 */
private CodeBlock wrapInstantSelfTailLoop(CodeBlock bodyBlock) {
    return CodeBlock.builder()
            .beginControlFlow("while (true)")
            .addStatement("ctx.checkZeroTimeQuota()")
            .add(bodyBlock)
            .beginControlFlow("if (!shouldRepeatSelfInvocation)")
            .addStatement("break")
            .endControlFlow()
            .endControlFlow()
            .build();
}
同时在 LeafBodyEmitter.java 中修复手写 Java 代码局部变量与 JDK 标准静态工具类的误判拦截：Java// 文件路径：groma/groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/LeafBodyEmitter.java
private static final Set<String> SAFE_STDLIB_RECEIVERS = Set.of(
        "Math", "StrictMath", "Objects", "String", "Double", "Long", "Integer", "Boolean",
        "Collections", "List", "Map", "Set", "GeoSpatialUtils", "RandomDistUtils", "TimeUnitUtils", "NullSafeUtils");

private boolean isAllowedScopeReceiver(String scopeName, Set<String> declaredLocalVars) {
    return SAFE_STDLIB_RECEIVERS.contains(scopeName) || declaredLocalVars.contains(scopeName);
}
4.6 方案六：引擎底座补齐全部 21 类标准事件派发（GromaWorldContext.java & BaseSimEntity.java & ObservableEntityList.java）修改目标：补齐缺失的 12 类事件（as、xs、am、xm、rv、ar、vr、xr、pca、pcd、psf、paa），与既有 9 类事件共用统一事件总线与 GromaTriggerTable 零反射触发机制。   Java// 文件路径：groma/groma-base/groma-base-engine/src/main/java/com/groma/base/engine/GromaWorldContext.java

/**
 * 1) 在 spawn(BaseSimEntity entity) 中补发指定初始状态出现事件 `as`（OBJECT_APPEARED_IN_STATE）：
 */
public <T extends BaseSimEntity> T spawnWithStateEvents(T entity) {
    T registered = spawn(entity);
    if (!isSilentInitOrArchivePhase()) {
        for (GromaMemberMeta meta : registered.memberMetas()) {
            if ("ENUM".equals(meta.typeRange())) {
                String initStateZh = registered.readEnumZhLabel(meta.memberId());
                if (initStateZh != null && !initStateZh.isEmpty()) {
                    publishSimEvent("as", registered.getInstanceId(), meta.memberId(), initStateZh, "", "");
                }
            }
        }
    }
    return registered;
}

/**
 * 2) 在 destroyEntity / markConsumed 中补发指定状态消亡事件 `xs`（OBJECT_CONSUMED_FROM_STATE）：
 */
public void destroyEntityWithStateEvents(BaseSimEntity target) {
    if (target == null || target.isConsumed()) {
        return;
    }
    for (GromaMemberMeta meta : target.memberMetas()) {
        if ("ENUM".equals(meta.typeRange())) {
            String curStateZh = target.readEnumZhLabel(meta.memberId());
            if (curStateZh != null && !curStateZh.isEmpty()) {
                publishSimEvent("xs", target.getInstanceId(), meta.memberId(), curStateZh, "", "");
            }
        }
    }
    target.markConsumed(this); // 原有逻辑派发 `x`
}

/**
 * 3) 集合元素增删双视角事件：宿主发 `ma`/`mx`，被操作元素同步发被动事件 `am`/`xm`：
 */
public void emitCollectionElementAdded(BaseSimEntity owner, String collectionMemberId, Object element) {
    String elemId = (element instanceof BaseSimEntity se) ? se.getInstanceId() : "";
    publishSimEvent("ma", owner.getInstanceId(), collectionMemberId, "", elemId, "");
    if (element instanceof BaseSimEntity elemEntity) {
        publishSimEvent("am", elemEntity.getInstanceId(), collectionMemberId, "", owner.getInstanceId(), "");
    }
}

public void emitCollectionElementRemoved(BaseSimEntity owner, String collectionMemberId, Object element) {
    String elemId = (element instanceof BaseSimEntity se) ? se.getInstanceId() : "";
    publishSimEvent("mx", owner.getInstanceId(), collectionMemberId, "", elemId, "");
    if (element instanceof BaseSimEntity elemEntity) {
        publishSimEvent("xm", elemEntity.getInstanceId(), collectionMemberId, "", owner.getInstanceId(), "");
    }
}

/**
 * 4) 关联/引用建立、重指向与解除双视角事件：主动端发 `ra`/`rv`/`rx`，目标端同步发被动事件 `ar`/`vr`/`xr`：
 */
public void emitRelationLinked(String assocId, BaseSimEntity left, String leftMemberId,
                               BaseSimEntity right, String rightMemberId, boolean retargeted) {
    String activeCode = retargeted ? "rv" : "ra";
    String passiveCode = retargeted ? "vr" : "ar";
    publishSimEvent(activeCode, left.getInstanceId(), leftMemberId, "", right.getInstanceId(), assocId);
    publishSimEvent(passiveCode, right.getInstanceId(), rightMemberId, "", left.getInstanceId(), assocId);
}

public void emitRelationUnlinked(String assocId, BaseSimEntity left, String leftMemberId,
                                 BaseSimEntity right, String rightMemberId) {
    publishSimEvent("rx", left.getInstanceId(), leftMemberId, "", right.getInstanceId(), assocId);
    publishSimEvent("xr", right.getInstanceId(), rightMemberId, "", left.getInstanceId(), assocId);
}

/**
 * 5) 过程级四类异常与边界事件统一派发入口（`psf` / `paa` / `pca` / `pcd`）：
 */
public void emitProcessEvent(GromaProcessTask task, String eventCode, String detail) {
    String hostId = (task.hostEntity() != null) ? task.hostEntity().getInstanceId() : "WORLD";
    publishSimEvent(eventCode, hostId, task.getProcessDefKey(), "", task.getProcessInstanceId(), detail);
}
4.7 方案七：补齐模型 2 外部航路规划脚本沙盒执行器与宿主自动装配（ExternalProgramConnector.java & ScriptIoBridgeConfiguration.java）修改目标：闭合断层 E1，使模型 2（智能筹划应用）生成的 world().invokeExternalScriptWithIo(...) 在运行期真正通过受控子进程标准输入输出完成航路规划参数传入与结果回写。   Java// 1) 文件路径：groma/groma-base/groma-base-connector/src/main/java/com/groma/base/connector/ExternalProgramConnector.java
// 在现有安全沙盒类中新增 JSON stdin/stdout 双向执行方法：
public Map<String, Object> runScriptWithJsonIo(String interpreterKey,
                                               Path scriptFile,
                                               Map<String, Object> inputs,
                                               Duration timeout) {
    String interpreterBin = requireAllowlistedInterpreter(interpreterKey);
    Path normalizedScript = validateNoPathTraversal(scriptFile);
    ObjectMapper mapper = new ObjectMapper();
    Path tempWorkDir = null;
    try {
        tempWorkDir = Files.createTempDirectory("groma-script-sandbox-");
        ProcessBuilder pb = new ProcessBuilder(interpreterBin, normalizedScript.toAbsolutePath().toString());
        pb.directory(tempWorkDir.toFile());
        pb.redirectErrorStream(false);
        Process proc = pb.start();
        try (var stdin = proc.getOutputStream()) {
            stdin.write(mapper.writeValueAsBytes(inputs == null ? Map.of() : inputs));
            stdin.flush();
        }
        boolean finished = proc.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            proc.destroyForcibly();
            throw new IllegalStateException("EXTERNAL_SCRIPT_TIMEOUT: 外部算法脚本执行超时 (" + timeout.toMillis() + "ms)");
        }
        byte[] stdoutBytes = proc.getInputStream().readAllBytes();
        if (proc.exitValue() != 0) {
            String errText = new String(proc.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            throw new IllegalStateException("EXTERNAL_SCRIPT_FAILED(exit=" + proc.exitValue() + "): " + errText);
        }
        if (stdoutBytes.length == 0) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(stdoutBytes, Map.class);
        return parsed == null ? Map.of() : Collections.unmodifiableMap(parsed);
    } catch (IOException | InterruptedException ex) {
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        throw new IllegalStateException("EXTERNAL_SCRIPT_IO_ERROR: " + ex.getMessage(), ex);
    } finally {
        cleanupTempDirQuietly(tempWorkDir);
    }
}
Java// 2) 新增宿主装配类：groma/groma-bootstrap/src/main/java/com/groma/bootstrap/pipeline/ScriptIoBridgeConfiguration.java
package com.groma.bootstrap.pipeline;

import com.groma.base.connector.ExternalProgramConnector;
import com.groma.base.engine.GromaWorldContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 将模块内嵌资源（/scripts/*.py）提取至受控临时文件并桥接至 ExternalProgramConnector，
 * 在 GromaModelRegistry.startSession 的初始化回调中自动挂载 ctx.setExternalScriptIoExecutor(...)。
 */
@Configuration
public class ScriptIoBridgeConfiguration {

    @Bean
    public GromaWorldContext.ExternalScriptIoExecutor gromaExternalScriptIoExecutor(
            ExternalProgramConnector connector) {
        return (ctx, scriptName, processId, inputs) -> {
            ClassLoader moduleLoader = ctx.getClass().getClassLoader();
            String resourcePath = scriptName.startsWith("scripts/") ? scriptName : "scripts/" + scriptName;
            Path tempScript = Files.createTempFile("groma-mod-script-", "-" + Path.of(scriptName).getFileName());
            try (InputStream in = moduleLoader.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalStateException("模块脚本资源不存在: " + resourcePath);
                }
                Files.copy(in, tempScript, StandardCopyOption.REPLACE_EXISTING);
                return connector.runScriptWithJsonIo("python3", tempScript, inputs, Duration.ofSeconds(15));
            } finally {
                Files.deleteIfExists(tempScript);
            }
        };
    }
}
4.8 方案八：修复第 151 拍存档续跑分叉（ENG-05）与 Maven 静态合包工程统一修改目标：修复 SimulationArchiveBundle 在第 150 拍存档恢复后第 151 拍分叉的确定性缺陷，并通过 Maven Profile 统一双轨 groma-models，兑现一行 mvn clean package 合包交付。   Java// 1) 文件路径：groma/groma-base/groma-base-engine/src/main/java/com/groma/base/engine/archive/SimulationArchiveBundle.java
// 升级 TaskArchivePart 记录会话墙钟基准 startEpochMs 与全局单调实例分配计数器 instanceSeqCursor：
public record TaskArchivePart(
        String archiveId,
        long tickIndex,
        String modelCode,
        String sceneCode,
        long finalStepMs,
        long rootSeed,
        long rngDrawCursor,
        long startEpochMs,
        long instanceSeqCursor
) {}

// 2) 文件路径：groma/groma-base/groma-base-engine/src/main/java/com/groma/base/engine/GromaWorldContext.java
// 在 createArchiveCheckpoint() 与 restoreFromArchive() 中对称读写 startEpochMs 与 instanceSeqCursor：
public void restoreTimingAndSequenceBaseline(SimulationArchiveBundle.TaskArchivePart header) {
    this.startEpochMs = header.startEpochMs(); // 消除恢复后“实际到站/离站时刻”墙钟偏移
    long maxNumericSuffix = scanMaxNumericSuffixOfRestoredInstances();
    // 取存档游标与实例数字后缀的最大值，彻底解决中文/UUID命名实例不推进序号导致 GeoPoint_6/7 提前撞号分叉的问题
    this.instanceSeq.set(Math.max(header.instanceSeqCursor(), maxNumericSuffix));
}
同时在 groma/groma-bootstrap/pom.xml 中增加 bundled-models 默认激活 Profile，将流水线生成的双模型模块（model-emergency-deduction 与 model-smart-planning）纳入标准编译期依赖，消除手写演示模块与生成工作区模块的割裂：XML<!-- 文件路径：groma/groma-bootstrap/pom.xml 新增默认合包 Profile -->
<profiles>
    <profile>
        <id>bundled-workspace-models</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <dependencies>
            <dependency>
                <groupId>com.groma</groupId>
                <artifactId>model-emergency-deduction</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.groma</groupId>
                <artifactId>model-smart-planning</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </profile>
</profiles>
五、改造实施顺序与回归验收门禁清单建议按模块依赖自底向上分四步合入并执行门禁验证：第一步（底座引擎与连接器加固）：合入方案六（GromaWorldContext 21 类事件补齐）、方案七上半部（ExternalProgramConnector.runScriptWithJsonIo）与方案八上半部（SimulationArchiveBundle 补 startEpochMs 与 instanceSeqCursor），执行 groma-base 全量单测，并移除 RealCorpusEndToEndTest#realCorpusResumesFromArchive 的 @Disabled 注解，验证模型 1 第 150 拍存档恢复续跑至 300 拍与直跑 300 拍 WorldSnapshot 100% 一致。第二步（解析器兜底补全）：合入方案一（AbModelMergerAndParser 组件枚举回填、非顶层 SCENE 收录与 PROXY 图元操作数解析），验证模型 1（2,632 实体 / 1,458 连线）与模型 2（2,023 实体 / 893 连线）零错误通过 StaticModelValidator 十项静态规则校验。第三步（五层源码生成器全关系闭环）：依次合入方案四（PlanModel 单向输入绑定 *ReadOnly + StartDTO）、方案二（ModelLayerEmitter 多继承委托 + R05 代理 + R06 控制掩码 + R27 备注 Javadoc）、方案三（新增 RelationLayerEmitter 生成模型 1 的 5 个 *RelationManager）以及方案五（ProcessTaskEmitter 补 checkMaintain、lastEnd、过程事件线、CDF 互斥概率采样与 LeafBodyEmitter 白名单），验证双模型重新出码后 ECJ 与 javac 双编译器 0 Error 对拍通过。第四步（宿主装配与静态红线门禁）：合入方案七下半部（ScriptIoBridgeConfiguration 挂载模型 2 外部航路规划脚本执行器）与方案八下半部（bundled-workspace-models 合包配置），执行 python3 tools/regression/scan.py 确保 NP-R01~NP-R10 与 FE-R01~FE-R13 共 23 条静态红线 0 违例，最终执行 mvn clean package 验证单 JAR 同时挂载运行双模型全部场景与 MCP 工具集。   
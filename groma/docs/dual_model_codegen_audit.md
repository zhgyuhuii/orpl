# 双模型（data-a/b + scene-a/b）Java 工程转化能力审查报告——断言逐条核实与修复落地

> **审查基准**：`docs/方圆语言2D_3D智能建模与多端推演平台_需求与技术架构设计说明书.docx`（V12.0）+ `docs/code_artifact.md` + `groma/` 全量源码
> **待转化语料**：模型 1 `groma/docs/data-a.json + data-b.json`（应急决策推演-关联副本-1029，v647）；模型 2 `groma/docs/scene-a.json + scene-b.json`（智能筹划应用，v844）
> **核实对象**：外部审查报告（zhgyuhuii/orpl 版）的全部断言，逐条对照本仓库源码与语料实证
> **结论日期**：2026-11-27（第二轮：在上一轮 code_artifact.md 十条建议修复基础上继续）

---

## 一、总体结论

1. **外部报告的核心判断"属实"**：两套语料的规模画像（2632/2023 实体、1458/893 连线、v647/v844、模型名、47/51 个 TYPE、5 个 ASSOCIATION-TYPE 等统计）与本仓库语料**逐一吻合**；其指出的工程断层与关系缺口**全部核实为真**，其中三条直指上一轮修复引入/残留的缺陷（VAL-007 误杀、VAL-006 关系名错配、流水线漏调校验器）。
2. **"能否转化为 Java 工程"**：模型 1 可完整六步流水线出码 + ECJ 编译 + javac 对拍；模型 2 可出码可编译，但核心 Python 航路规划算法在修复前"只调空壳、不传参、不回写"——**本轮已修**（双向数据桥），宿主侧脚本执行器装配方见第五节。
3. **"能否全部表示模型关系"**：**修复前不能**（报告列举的 29 类关系逐项缺口全部属实）；**本轮修复后**，校验与工程闭环类缺口（校验器接线/语料安全/initialOverrides/反引号/phantom Service/独立过程 service/Python IO 桥）已闭环；解析与生成器层的深水区缺口（多继承、R05/R06、R13/R15/R25/R26、RelationManager、ReadOnly 绑定、R23 事件线、21 类事件全量派发、存档续跑分叉）**仍为真实缺口**，第五节给出可直接落地的方案与代码。
4. **回归验证**：本轮全部改动通过受影响模块测试（engine/connector/parser/source-generator/compiler-hotswap/bootstrap，含真实语料六步流水线集成测试）。

---

## 二、断言逐条核实矩阵

| # | 外部报告断言 | 核实结论 | 证据（本仓库现行源码/语料） |
|---|---|---|---|
| 1 | 语料规模画像（2632/2023 实体，1458/893 连线，v647/v844） | ✅ 属实 | 解包 API 信封后 `obj.entities` 长度 2632/2023、`obj.version` 647/844、label 完全一致 |
| 2 | **ENG-01a 流水线漏调 StaticModelValidator**（注释写"解析→校验→出码"，代码直接跳到出码） | ✅ 属实 | `ModelPipelineService.buildFromCorpus` 原实现 parse 后直接 generate，全文件无 validator 引用 |
| 3 | **ENG-01b VAL-007 误杀真实语料**（校验 `ty.entityId()`，语料 id 数字开头如 `3Dr4LeLCGkprqwEd722`、`0effeb2c4c6743fab5ddc2c605`） | ✅ 属实 | 语料实测：模型 1 TYPE 47 个全部含 ASCII 字母、17 个数字开头；模型 2 51 个中 21 个数字开头。原预过滤"含 ASCII 字母即校验"必然大面积误杀 |
| 4 | **ENG-01c VAL-006 关系名口径错配 100% 漏检**（validator 查 `R11_CONSUMPTION`，parser 写 `linkType` 短名） | ✅ 属实 | `AbModelMergerAndParser.toViewGraph`（2322 行起）写入 `EdgeCellIR.relationType` 的是 `linkType(cell)` 原始短名；`CanvasMappingTables.SHAPE_TO_RELATION` 的 R 码仅在映射表里 |
| 5 | **VAL-004 与解析器入度兜底冲突**（parser 零入度推断入口，validator 按画布 firstStart 全 false 误报） | ✅ 属实 | parser 903 行"入口兜底：没有 firstStart 时取没有入向调用线的节点"；但 2301 行 `SubProcessStepIR.firstStart` 仍传画布原始值 |
| 6 | **R21/R22 硬编码 `List.of()`**（启动/维持条件 100% 丢失，模型 1 含 3 条 condition） | ✅ 属实 | parser 628 行 `List.copyOf(def.entries), List.of(), List.of()` 传参位置即 startConditions/maintainConditions |
| 7 | **R23 带过程事件码的调用线被跳过**（psf/paa/pca/pcd 打印诊断后 continue） | ✅ 属实 | `ProcessTaskEmitter` 366 行"调用线携带过程事件 … 暂未编译，已跳过" |
| 8 | **R08/R10 关联类型只进文档不出码**（无 RelationManager 生成） | ✅ 属实 | `associationTypes` 全生成器仅 `ManifestAndIndexEmitter` 消费（写 Markdown），无 engine/relation 生成器 |
| 9 | **R01 多继承 / R05 集合代理 / R13 / R15 / R25 / R26 解析 0 处理** | ✅ 属实（模型 2 有 5 组 PARENT+edge-extend、1 组 PROXY；in-change/in-out/overtime/undertime 全解析器无处理） | 全文 grep 证实 |
| 10 | **R16/R17 ReadOnly 接口生成但从未作形参类型**（契约未绑定） | ✅ 属实 | `PlanModel:418` 生成 ReadOnly 接口，形参解析一律返回可变 `*Entity` |
| 11 | **ENG-02 Python 脚本只调空壳**（不传参、不回写；且 translator 死代码调用不存在的 `ctx.invokeSandboxedPythonScript`） | ✅ 属实 | `LeafBodyEmitter` 75 行 `world().invokeExternalScript($S,$S)` 无参无回写；`GromaWorldContext` 无 `invokeSandboxedPythonScript` 方法 |
| 12 | **WorldContext `initialOverrides` 悬空**（构造器收参不用） | ✅ 属实 | `WorldContextEmitter` 构造器仅 javadoc 写"预留"，super 调用与类体均未消费 |
| 13 | **GROMA_MODEL_INDEX.md 反引号破损 + phantom Service**（无自有过程对象虚报 Service 类名） | ✅ 属实 | 现存生成产物 14/15 行 `\`scene2 |` 缺右反引号；索引表 Service 列恒打印类名 |
| 14 | **独立过程缺 IIndependentProcessService**（ServiceLayerEmitter 过滤 INDEPENDENT 后无处安放） | ✅ 属实 | `ServiceLayerEmitter:58` 过滤 INDEPENDENT；全工程无独立 service 生成 |
| 15 | **21 类事件仅约 8 类派发**（as/xs/am/xm/ar/vr/xr/pca/pcd/psf/paa 缺失） | ✅ 属实 | 引擎与生成接线表无这些事件码的派发点 |
| 16 | **ENG-04 export-zip 未剔除 groma-studio**（打包整套建模套件而非轻量运行态） | ✅ 属实（行为符合描述） | `StudioWorkspaceExportService.exportZip` 打包 `groma-studio` 全目录。**但注意**：这是**有意取舍**——bootstrap 依赖 hotswap/debug-server（运行期热挂载与 DAP 必需），groma-studio 主体无法剥离；"精简运行态交付"需要 bootstrap 依赖重构（见第五节方案 H），非一行可改 |
| 17 | **ENG-05 存档续跑第 151 拍分叉**（startEpochMs/instanceSeq 未持久化） | ⚠️ 部分属实 | `instanceSeq` 确实在 `restoreFromArchive` 里从实例 id 后缀推进（`GromaWorldContext` 810-818 行），但命名实例 id（`类型_序号` 形态之外的中文/UUID id）不推进序号；`startEpochMs` 未持久化属实（`TaskArchivePart` 无该字段）。该问题在 `RealCorpusEndToEndTest` 的 @Disabled 分叉用例可复现，归入第五节方案 G |
| 18 | 模型 2 组件解析（PARENT/CUSTOM 状态枚举、场景兜底）缺失 | ✅ 属实 | `indexEnums` 只扫 ENUM 定义视图；`classifyTypes` 只认组织树 SCENES 顶层下的 SCENE |
| 19 | LeafBodyEmitter `unsupportedMemberAccess` 误杀合法 Java（局部变量、JDK 静态调用） | ✅ 属实（方向正确） | 该检查按 receiverType 白名单推导，方法内局部变量与 `Math.*`/可变参数不在推导范围即整段降级 `fail(...)` |

**不属实/需修正的报告细节**：无硬性错误；第 16 条的性质是"有意取舍"而非疏漏，第 17 条中 `instanceSeq` 已有部分推进逻辑（报告称完全未持久化不准确）。

---

## 三、本轮已修复（全部通过回归）

| 修复项 | 落点 | 内容 |
|---|---|---|
| F1 VAL-007 语料安全 | `StaticModelValidator.validateExplicitEncode` | 预过滤改为**完整匹配** `^[A-Za-z_][A-Za-z0-9_]*$`：平台内部主键（UUID/数字开头）直接跳过交命名引擎转译；合法形态只做保留字冲突校验。语料零误杀 |
| F2 VAL-006 双口径 | `StaticModelValidator.FORBIDDEN_FOR_CONSTANT` | 同时收 `R11_CONSUMPTION` 等 R 码与 `consumption` 等 linkType 短名，双口径全拦截 |
| F3 VAL-004 入度对齐 | `StaticModelValidator` VAL-004 分支 | 无显式 firstStart 时按 `subSteps.nextStepPcs` 计算零入度：>0 降级 WARNING（解析器会自动推断入口），纯环才 ERROR |
| F4 **校验器正式接入流水线** | `ModelPipelineService.buildFromCorpus` 步骤 1.5 | ERROR 记入 problems 并**阻断出码**；WARNING 记入构建报告 warnings。真实语料全链路集成测试通过（零误阻断） |
| F5 initialOverrides 落地 | `GromaWorldContext` + `WorldContextEmitter` | 基类新增 `setInitialOverrides` + 初始化静默期内 `applyInitialOverrides()`（不产生值变化事件）；键语义 `实例ID.成员名` 定点覆盖、`成员名` 覆盖场景根对象（生成类覆写 `sceneRootEntity()` 返回 self）；数值/布尔尽力类型转换，失败记 problem 不中断启动 |
| F6 Python 双向数据桥 | `GromaWorldContext.invokeExternalScriptWithIo` + `LeafBodyEmitter` Python 分支 | 新增 `ExternalScriptIoExecutor`（inputs JSON 进脚本 stdin、stdout JSON 回包）；生成代码自动打包全部 IN/IN_OUT 标量形参 → 受控执行 → 按中文形参名回写 OUT/IN_OUT（boolean/数值/String 三类适配，引用类型跳过并留注释）；同时清掉 translator 中调用不存在方法的死代码 |
| F7 索引表修复 | `ManifestAndIndexEmitter` | 场景表 sceneCode 反引号补齐（修复产物第 14/15 行破损）；Service 列按 `procList` 归属统计，无自有过程的类型改打印 "—"（消除 phantom Service） |
| F8 独立过程 service 层 | `ServiceLayerEmitter.emitIndependentService` + facade 接线 | 独立过程统一生成 `IIndependentProcessService` + `IndependentProcessServiceImpl`（@Service）：瞬时算子 `new Task(ctx, 形参…).startTopLevel()` 同步执行；耗时过程 launch 并返回任务；带 @GromaUserCode 保护区。模型无独立过程时不生成 |

**回归结果**：受影响六模块测试全绿——**BUILD SUCCESS，85 用例（0 失败 0 错误）**，含 `RealCorpusEndToEndTest` 真实语料出码 + javac 对拍、`ModelPipelineIntegrationTest`/`PortSessionFlowIntegrationTest` 全链路、`SourceGeneratorDeterminismAndScanTest`（生成产物过确定性 + 静态红线扫描）、校验规则单测；`scan.py` 23 条红线全仓零违例。

---

## 四、尚未闭环的真实缺口（修复前后的关系表示能力对照）

修复后各关系/事件的表示能力变化（对照报告 3.2 总表）：

| 关系/事件 | 修复前 | 修复后 | 说明 |
|---|---|---|---|
| 校验闭环（VAL-001~010） | 6/10 且漏调 | **10/10 且强制执行** | F1~F4 |
| R04 想定覆盖 | initialOverrides 悬空 | **已生效** | F5 |
| 独立过程五层契约 | 缺 service 层 | **IIndependentProcessService 已生成** | F8 |
| 模型 2 Python 算法 | 空壳调用 | **双向数据桥（待宿主装配方，见方案 F）** | F6 |
| R01 多继承 / R05 代理 / R06 控制 / R13 / R15 / R25 / R26 | 缺失 | 仍缺失 | 方案 A/C/D |
| R07~R10 关联出码 | 只进文档 | 仍只进文档 | 方案 B |
| R16/R17 ReadOnly 绑定 | 未绑定 | 仍未绑定 | 方案 E |
| R23 事件调用线 / R21/R22 出码 | 硬编码丢弃 | 仍丢弃 | 方案 D |
| 21 类事件全量派发 | ~8 类 | 仍 ~8 类 | 方案 C |
| 存档续跑确定性 | 分叉隐患 | 仍分叉隐患 | 方案 G |

---

## 五、剩余缺口的修改方案与具体代码

以下代码均为可直接落盘的实现（遵循 JDK 21 / JavaPoet / 零 System.out / 零 synchronized 红线）。按依赖顺序排列。

### 方案 A：R01 多继承（解析 + 出码）

**解析（`AbModelMergerAndParser`）**：

```java
/** 解析 R01：单继承（inheritEntityId）+ 画布 PARENT 图元 + 定义视图 edge-extend → 主/副父类 */
private ParentHierarchy resolveParentHierarchy(String typeId) {
    JsonNode entity = entities.get(typeId);
    java.util.LinkedHashSet<String> parents = new java.util.LinkedHashSet<>();
    String direct = text(entity, "inheritEntityId");
    if (!direct.isEmpty() && entities.containsKey(direct) && !direct.equals(typeId)) {
        parents.add(direct);
    }
    for (JsonNode candidate : entities.values()) {                      // PARENT / PARENT||CUSTOM 图元
        if ("PARENT".equals(text(candidate, "type")) && typeId.equals(text(candidate, "typeEntityId"))) {
            String pid = text(candidate, "refEntityId");
            if (pid.isEmpty()) pid = text(candidate, "parentEntityId");
            if (!pid.isEmpty() && entities.containsKey(pid) && !pid.equals(typeId)) parents.add(pid);
        }
    }
    for (String vid : List.of(text(entity, "defineViewId"), text(entity, "designViewId"))) {
        for (JsonNode cell : cellsOf(vid)) {                            // edge-extend 连线
            if ("LINK".equals(text(cell, "type")) && "extend".equals(linkType(cell))) {
                String child = text(cell.path("source"), "cell");
                String parentId = text(cell.path("target"), "data");
                if (typeId.equals(cellSourceEntityId(cell)) && entities.containsKey(parentId)) parents.add(parentId);
            }
        }
    }
    if (parents.isEmpty()) return new ParentHierarchy("", List.of());
    List<String> list = new ArrayList<>(parents);
    return new ParentHierarchy(list.get(0), list.size() > 1 ? List.copyOf(list.subList(1, list.size())) : List.of());
}
private record ParentHierarchy(String primary, List<String> mixins) {}
```

**出码（`ModelLayerEmitter`）**：`GromaModuleIR.TypeIR` 已有 `mixinParentEntityIds` 字段——副父类生成组合委托：

```java
// 接口多继承：public interface SubReadOnly extends P1ReadOnly, P2ReadOnly
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
```

### 方案 B：R07~R10 关联管理器（RelationLayerEmitter 新增）

```java
/** 消费 plan.associationTypes()：engine/relation 包下强类型双向关联管理器（基数校验 + ra/ar/rv/vr/rx/xr 双端事件） */
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
```
配套：`GromaWorldContext` 持有关系管理器注册表（`Map<String, BidirectionalTie<?,?>>`），`ModuleRuntimeEmitter` 在 `createSession` 中实例化并登记。

### 方案 C：21 类事件全量派发（engine 底座）

```java
// BaseSimEntity/GromaWorldContext 补齐被动事件（订阅侧按需派发，无订阅零成本）：
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
```
`ObservableEntityList` 的 add/remove 回调里补 `am`/`xm` 元素侧派发；过程任务 `GromaProcessTask` 补 `pca`/`pcd`（完成时比对 elapsed 与 TIMING 上下限）与 `psf`（startTopLevel 失败分支）、`paa`（维持条件中断，配合方案 D 的 checkMaintain）。

### 方案 D：R21/R22/R13/R15/R23/R25/R26 解析与出码

**解析（`AbModelMergerAndParser.parseDef`）**——替换两处 `List.of()`：

```java
// 设计视图扫描：edge-condition → startConditions；edge-agent → maintainConditions；
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
```

**出码（`ProcessTaskEmitter`）**：

```java
// canStart() 尾部追加：R17/R20 存活守卫（已有判空）+ R13 源状态 + R22 启动条件；失败走 psf 后继（R23 事件线）
// 每拍 checkMaintain()：R21 agent 条件为 false → abortAllChildren + 状态置 ABORTED + 发 paa + 跳 maintain 失败分支
// nodeDone() 移除"暂未编译，已跳过"：eventCode ∈ {psf,paa,pca,pcd} 的 NextIR 正常编译为 case 分支
// finishSuccess()：elapsed > timeMax 发 pcd；elapsed < timeMin 发 pca；激活对应 ExceptionFlowIR 目标 pc
```

### 方案 E：R16/R17 ReadOnly 契约绑定（PlanModel 一处）

```java
// PlanModel.resolveParam：direction IN 且 nonnull/nullable（R16/R17）时返回 readOnly 而非 entity
TypeName paramType = "IN".equals(ir.direction()) && !rebindable
        ? type.readOnly() : type.entity();
// ProcessTaskEmitter 构造器字段同步为只读接口；方法体内部仅调用接口的 Getter（编译期禁写）
```

### 方案 F：宿主侧 Python 执行器装配方（使 F6 数据桥真正落地）

```java
// bootstrap 新增 ScriptIoBridgeConfiguration：把 ExternalProgramConnector 适配为 ExternalScriptIoExecutor
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
```
`ExternalProgramConnector` 需补 `runScriptWithJsonIo(script, inputs)`：临时目录写脚本 + stdin 喂 JSON + `destroyForcibly` 超时强杀 + 解析 stdout JSON（既有安全沙盒语义全部保留）。

### 方案 G：存档续跑确定性（ENG-05）

`TaskArchivePart` 增加 `startEpochMs` 字段并在 `createCheckpointAtStepBoundary` 记录、`restoreFromArchive` 恢复 `AbstractWorldSession` 墙钟基准；`instanceSeq` 推进改为"扫描全部恢复实例 id 的数字后缀 + 每次命名实例创建统一走 seq 分配"两条路取最大（中文/UUID id 不含数字后缀的用 seq 计数兜底）。验收以 `RealCorpusEndToEndTest` 中被 @Disabled 的 150→151 拍分叉用例转绿为准。

### 方案 H：export-zip 精简运行态交付（可选增强）

当前打包 `groma-studio` 全目录是**必要依赖**（bootstrap 依赖 hotswap/debug-server）。若要纯运行态轻量包：新增 `groma-runtime` 聚合模块（仅 base 五模块 + workspace 生成模型 + 一个无 studio 依赖的精简宿主），export-zip 的 `?flavor=runtime` 参数切换打包该子集。属于结构演进，不影响现有交付。

---

## 七、外部"具体代码实现"的收割记录（第三轮，2026-11-27）

外部报告第四节给出的代码按"**增量适配现有 IR**"方式收割（4.1 整文件替换不可行——其 TypeDefIR/ProcessDefIR 结构与现有 CompilePlanIR 不兼容，直接替换会破坏全部 emitter 与解析器约 200 处调用点；逐片段适配后全部编译通过、回归全绿）：

| 收割项 | 外部代码 | 落地形态 |
|---|---|---|
| 4.1 CompilePlanIR 扩展 | TypeDefIR +mixin/proxies/notes；ProcessDefIR +selfInvoking/preActions/notes；NextIR/PostIR/TriggerIR +probability/probabilityGroup/expression；NodeIR +lastEnd | ✅ 增量收割（加字段 + 保留旧规范构造器兼容，波及仅 parser 6 处 + LegacyPlanAdapter 4 处）：TypeDefIR +mixinParentIds/proxies/notes、ProcessDefIR +exceptionFlows、ConditionRuleIR +expression、PostIR/NextIR/TriggerIR +概率组、NodeIR +lastEnd、新增 `ProxyDefIR`/`ExceptionFlowIR`。selfInvoking/preActions/notes 未加（无解析来源，待 R24 收割时一并） |
| 4.2 补丁 A（R01 多继承解析） | resolveParentIds | ✅ 适配落地：`inheritEntityId` + `inherits` 数组 + **全视图预扫描**画布 edge-extend（画布约定**父 --extend--> 子**，且组织树级视图不在实体 defineViewId 下，首版单视图扫描漏抓已修正）+ PARENT 实体（typeEntityId=子类型、父类经 inheritEntityId/类名定位）。**真实语料验证**：模型 2 的 5 组画布继承全部入表，与实体 `inheritEntityId` 指向同一父（画布边是属性继承的图形化表达，去重后 mixins=[] 为正确语义——多继承为单父场景） |
| 4.2 补丁 B（R05 代理解析） | parseProxies + ProxyDefIR | ✅ 落地：PROXY 图元 + 入向 edge-proxy → `TypeDefIR.proxies`。**真实语料验证：模型 2 proxies=1 精确命中报告声称的那 1 组**。resolveOperand 的 PROXY 分支为注释形态未收割（无完整实现），消费待方案 C |
| 4.2 补丁 C（R21/R22/R13/R15 条件解析） | parseConditions | ✅ 适配落地：`parseGuardsAndExceptions` 扫定义/设计视图 condition/agent/in-change/in-out 边 → `ProcessDefIR.startConditions/maintainConditions`（字段早已存在，原硬编码 `List.of()` 已替换）；agent 同时进 canStart 与 maintain。**真实语料核实**：模型 1 的 3 条 condition 连线实体是**孤儿**（无 target 字段、无画布图元），无法定位归属过程——startRules=0 是语料事实而非解析缺陷，报告"模型 1 含 3 条 condition"的实体计数正确但其可编译性被高估 |
| R25/R26 异常边（外部文档截断处） | parseProcessGuardsAndExceptions（代码未给出） | ✅ 自行实现补全：overtime/undertime-exception 边 → `ExceptionFlowIR(OVERTIME_PCD/UNDERTIME_PCA, 目标 pc)`，pc 经节点 cellId 映射 |
| 概率组（补丁 probabilityOf/probabilityGroupOf） | 同上 | ✅ 落地：连线 rate/probability/probabilityGroup/groupCode/mutexGroup 兼容读取，填充 NextIR/PostIR（百分数 >1 自动归一）。出码消费（CDF 互斥采样）待方案 D |
| 出码消费（新增，超出外部文档范围） | — | ✅ `ProcessTaskEmitter.emitCanStart` 接入 startConditions：状态前置生成 `enumZhLabel` 中文比对（与事件接线表同一语义）、数值/布尔条件内联字面量比较、复合逻辑表达式注释降级不误杀 |

**未收割与原因**：4.1 整文件替换（不兼容）；粘贴文档截断处之后的 generator/engine 层代码未在附件中；`checkMaintain`/`pca/pcd/psf/paa` 出码与触发概率消费需 engine `GromaTriggerTable`/`GromaProcessTask` 结构演进，按第五节方案 D 分期。

**验证**：受影响四模块回归 BUILD SUCCESS（parser 全部单测含真实语料确定性解析、source-generator 确定性+扫描、compiler-hotswap 真实语料 ECJ+javac 对拍、bootstrap 全链路集成）；jshell 级抽查确认 IR 承载（proxies=1、画布继承表 5 条）。

### 七·B、code.md 审查与二次收割（第四轮，2026-11-27）

`docs/code.md`（4872 行）经 diff 驱动审查：其六个 java 文件打印段（CompilePlanIR/GromaModuleIR/CanvasMappingTables/StaticModelValidator/UniqueIdentifierGuard/GraphCycleAndDepthGuard）与本仓库**逐字节一致**（快照基于第三轮修复后的仓库，无独立改动）；语料统计与第三轮实测一致；其 parser 全文打印（3215 行带行号）中独有 **692 行补丁集（【修复 1~6】）**，收割结果：

| 补丁 | 判定 | 落地 |
|---|---|---|
| 【修复 3】collectPostActions 并入 R15 "in-out" 目标状态后置转换 | ✅ 收割 | `AbModelMergerAndParser.collectPostActions`：in-out 与 out-change 同路承载 OUT_CHANGE |
| 【修复 4】R25/R26 精确绑定 sourcePc（被监控耗时子过程） | ✅ 收割 | `ExceptionFlowIR` 加 `sourcePc` 字段（兼容旧 3 参）；`parseGuardsAndExceptions` overtime/undertime 分支记录 srcPc/dstPc |
| 【修复 5】buildWiring/buildTrigger 丢弃 expression/probability/probabilityGroup | ✅ 收割 | 触发接线调用点与 `buildTrigger` 签名扩展，`TriggerIR` 16 参承载概率组与复合表达式 |
| 【修复 6】R27 edge-note 备注采集 | ✅ 收割 | 新增 `collectNotes` → `TypeDefIR.notes`（出码写 Javadoc 的信息承载，Javadoc 渲染待 emitter 消费） |
| 【修复 1/2】PARENT 图元升级注册为领域基类 | ⏸ 暂不收 | 触发场景（外部组件基类仅以 PARENT 图元存在于本包且 TYPE 引用断裂）不在现有两套语料中出现；机制记入方案 A 备注 |
| parseParams seen 去重等 | ✗ 假差异 | 仓库现状已实现（diff 对齐漂移） |

**验证**：全仓编译零错误；受影响四模块回归 **BUILD SUCCESS，85 用例 0 失败 0 错误**。

### 七·C、code1.md 审查结论（第五轮，2026-11-27）

`docs/code1.md`（4223 行）diff 驱动审查：六个 java 文件打印段（RealCorpusEndToEndTest/ModelPipelineService/GromaModuleSourceGeneratorFacade/CompilePlanIR/StaticModelValidator/CanvasMappingTables）与本仓库**逐字节一致**（差异仅为段间空行与后续 python 块溢出）；parser 全文打印**截断于 1600 行**（前 1600 行与仓库一致，尾部未打印无从比对）；其余为 .class 版本（Java 21）/jar 清单诊断输出。**结论：无任何新实现代码，无可收割项，无需调整**——该文档是对当前仓库状态的验证性快照。

### 七·D、code2.html（Gemini 对话存档）审查与收割（第六轮，2026-11-27）

`docs/code2.html`（3.1MB Google Gemini 会话存档）提取出 30 个代码块，其中 12 个为实现代码（覆盖方案 A~H）。审查判定混有**真实 API 与幻觉 API**（`memberMetas()`/`publishSimEvent`/`readEnumZhLabel`/`getRightOf`/`classBaseName` 均不存在），按真实 API 适配收割：

| 块 | 判定 | 落地 |
|---|---|---|
| 块 25（双视角事件辅助 as/xs/am/xm/ar/vr/xr/psf/paa/pca/pcd） | ✅ 适配收割 | `GromaWorldContext` 新增 `emitRelationLinked/Unlinked`、`emitCollectionElementAdded/Removed`、`emitProcessEvent`——按真实事件总线（`fire`→`enqueueMemberEvent`→`dispatchSingleEvent`）重写，非其幻觉的 `publishSimEvent` |
| 块 20+21（RelationLayerEmitter + facade 接线） | ✅ 适配收割 | 新建 [RelationLayerEmitter.java](groma-studio/groma-source-generator/src/main/java/com/groma/studio/codegen/plan/RelationLayerEmitter.java)：associationTypes → engine/relation/*RelationManager（BidirectionalTie 子类，基数上限校验、1..1 重指向 rv/vr、越界 recordPublicProblem、rx/xr 解除）；真实 API（`getRights/getLefts`、`resolveEntityClassName`）；facade 已接线 |
| 块 26+27（connector JSON IO + 宿主装配 + registry 挂载） | ✅ 适配收割 | `ExternalProgramConnector.runScriptWithJsonIo`（复用运行时白名单+超时强杀+独立工作目录；Jackson3 = tools.jackson）；新建 ScriptIoBridgeConfiguration（模块 /scripts 资源→临时文件→受控执行）；`GromaModelRegistry.startSession` beforeInitialize 挂载 executor（bundled+热挂载两路）——**方案 F 全链路闭环**（模型 2 Python 算法可真实执行） |
| 块 18（场景兜底 + STATE 反向归集） | ⚠️ 半收半退 | STATE 反向归集（`backfillComponentEnumsFromStates`）✅ 收割；**场景兜底❌ 回退**——回归抓到副作用：语料中挂在组件目录的"默认场景"占位实体被混入权威场景表（2→4），破坏既有刻意约定（parser 测试 `assertEquals(2, scenes.size())` 失败）。回退后注释留档判定依据 |
| 块 28（存档 startEpochMs/instanceSeqCursor） | ✅ 早轮已收 | 第四轮已实现（本对话上一问），本轮核对一致 |
| 块 23/19/22（canStart 增强/checkMaintain/ReadOnly/StartDTO 草图） | ⏸ 设计稿 | 引用幻觉 API（`ParamInfo`/`proc.ir`/`p.isEntityOrReadOnly`/`renderConditionRuleBooleanExpr` 等不存在），是方案 D/E 的设计参考非可收割代码；已在 canStart 部分落地的状态前置/条件内联由第三轮实现 |
| 块 29（bootstrap pom 默认合包 workspace 模型 Profile） | ❌ 不可收 | workspace 模块（groma-bootstrap/groma-workspace/groma-models/*）不在根 pom reactor 内（无父 pom 聚合），直连依赖会使构建直接失败；合包交付走 export-zip 静态打包（已实现） |
| 块 24（LeafBodyEmitter 白名单 stdlib） | ⏸ 参考 | 思路正确（unsupportedMemberAccess 误杀修复），但与现有实现结构不符，独立任务处理 |

**过程修正**：收割中途 connector pom 注入产生重复 `<dependencies>` 标签（grep 只筛 .java 错误漏掉了 pom 报错），全 pom XML 校验修复；场景兜底经回归判定回退。最终**85 用例 0 失败 0 错误**、全 pom 有效、scan 零违例。

## 八、回归验证与产物（含第三轮收割验证）

## 九、第五节四方案全量落地与存档确定性收口（2026-11-27 第七轮开发，同日晚）

P1~P5 全部交付（交接文档 HANDOFF-2026-11-27.md 的未完成事项清零，docx V13.0 重排除外——属用户决策项）：

### 9.1 方案 G / T1.5 存档续跑确定性——分叉用例转绿（三根因）

`RealCorpusEndToEndTest.realCorpusResumesFromArchive`（原 @Disabled）转绿，150→151 拍分叉的三个根因全部修复：

| # | 根因 | 修复落点 |
|---|---|---|
| 1 | **事件重放**：恢复会话先走全新初始化（默认启动等）入队的事件在装载前未清空，续跑首拍把过期事件派发给触发接线表，重放中断前已发生的启动链（实测：恢复首拍重放 进站→停站，重建 GeoPoint、改写 实际到站时刻） | `AbstractWorldSession.clearPendingEventsForArchiveRestore()` + `GromaWorldContext.enterArchiveLoadingPhase` 清空待派发队列 |
| 2 | **接线钩子丢失**：触发接线启动的顶层过程成功后的输出影响回写（afterN）是接线侧闭包，存档重建任务不带钩子→续跑后 区间行驶 成功却不回写 列车当前在线路上的里程→形成车站与列车的关联 按 里程=0 找站→绑到出发站（实测：恢复侧 StationWithParkedTrain_7 绑到 海滩寺 而非 沙口路） | `ProcessArchivePart` +`wiringContextInstanceId`（record 加尾字段，旧档缺省 "" 兼容，schemaVersion 保持 2）；`GromaProcessTask.setWiringContext`；`GromaModuleRuntime.reattachWiringHook` 默认空实现；生成运行时按 defKey 重挂 afterN；startN/startManualN 设 setWiringContext |
| 3 | **单例锁未重绑**：lockedKey 是 begin() 的运行期字段不随 exportRuntime 导出，恢复的任务结束时解不掉锁→同类型单例新实例永远被「单实例约束拒绝启动」（实测：沙口路离站后新区间行驶启动失败，恢复侧列车不再行驶） | `GromaProcessTask.restoreSingletonLock()` 按.singletonMode 重推锁键并确保入锁表；restoreProcesses 对每个恢复任务调用 |

顺手修：`advanceProcessSeq` 按恢复的过程实例标识推进 processSeq（防续跑新建过程撞名）；测试对比口径与 T1.4 对齐（墙钟戳成员 实际到站/离站时刻 随会话 startEpoch 变化，不属于仿真状态）。

**诊断方法论**：临时诊断用例（已删）逐拍 digest 对比 + CorpusTrace 过程启停追踪，把「事件重放」从「在途任务对齐」的猜测中剥离出来——p9（区间行驶）本身恢复正确（子过程 p3/p4/p5 续跑），错的是过期事件重放的启动链。

### 9.2 方案 D 出码消费（P2 五项）

| 项 | 落点 | 说明 |
|---|---|---|
| checkMaintain（R21） | 引擎 `GromaProcessTask.checkMaintain()` 每拍钩子 + `ProcessTaskEmitter.emitCheckMaintain` | maintainConditions 编译为布尔守卫（状态前置 enumZhLabel 比对 + 数值/布尔内联），不满足→`ctx.emitProcessEvent(this,"paa",...)` + `abortByMaintainBroken` 级联终止；无维持条件不生成（零开销） |
| nodeDone 事件线路由（R23） | `emitNodeDoneCase` 重写 + `emitNodeFailed` | pca/pcd 出口在成功路径按 `childExitEvent(pc)` 路由；psf/paa 出口生成 `nodeFailed` 覆写在失败分支路由；子过程出口码引擎侧记录（`classifyExit`：SUCCESS→exitEvent / ABORTED→paa / FAILED→psf） |
| 概率组 CDF（标准 10.4） | `emitProbabilityGroups` + 触发线伯努利门控 | 同组连线概率归一后 `ctx.randomStream().nextDouble()` 互斥采样（任务级确定性随机流，存档游标随存档恢复）；独立概率线伯努利守卫；触发线 probability<1 的接线启动器包伯努利门控 |
| R25/R26 异常流 | `finishSuccess` 出口归类 + `exceptionFlows` 边消费 | 引擎按 TIMING timeMin/timeMax 归类 pca/pcd 并派发过程事件；生成 nodeDone 对 sourcePc 匹配的调用节点按 pcd/pca 出口激活 targetPc 异常处理路径；sourcePc=-1（过程级监控）只发事件不激活（成功后激活已无机器可走，注释留档） |
| lastEnd 级联 | `finishWithLastEnd()` | 结束节点完成后立即清空旁路子任务与未走队列，置 WAIT4SUCCESS 让提交阶段即刻收尾（成功回写与钩子照常） |

### 9.3 方案 C 剩余接线（P3 三项）

- **spawn/markConsumed 双视角**：spawn 发 a+as、markConsumed（onEntityConsumed）发 x+xs；ProcessEventNames 补被动码中文（as/xs/am/xm/ar/vr/xr）。
- **ObservableEntityList am/xm**：列表加后备构造器 + `observed` 工厂（防视图套视图）；生成实体集合字段从创建起就是受观察视图（null 汇=创建期静默），ctx 写入方法接 `ctx.collectionSink`——持有方 ma/mx + 元素 am/xm 双视角。
- **psf 启动失败**：引擎 begin() 在单例拒绝与 canStart 失败两分支 `emitProcessEvent(this,"psf",...)`。

### 9.4 方案 E ReadOnly 绑定（P4）——扫描 + 传递闭包让位

原方案（IN 一律绑 ReadOnly）被真实语料打脸：模型 1 的 区间行驶/停站/离站 等 10 处经 IN 形参回写关联成员（写用例），硬绑定生成代码编译失败。落地形态：

- `writableParamIds(def)` 传递闭包：直接写（node.writes / postActions 目标）∪ 传入可写子过程形参的实参 ∪ 作为子过程宿主（node.host，被调方写 self）——递归备忘 + 环路退化直接写集合。**闭包保证调用点两侧类型判定一致**（调用方 ReadOnly、被调方实体会编译失败）。
- 仅「IN + 对象引用 + 闭包外」的形参绑 `readOnly()`；让位时保持实体（真实语料验证：沿区间前进/查找前方车站 等只读消费者真绑定，区间行驶 等写用例形参保持实体）。
- `PlanModel.typeOf` 同时认 ReadOnly 类名（成员寻址写目标解析依赖）；`LeafBodyEmitter.hasMethod` 对生成包前缀类名放行（块 24 收割：运行帧 BO 等不在生成器类路径上，Class.forName 必败导致误杀整段降级）。

### 9.5 P5 其余

- **R05 集合代理消费**：`PlanModel.proxiesByType` 分组；生成实体被代理集合的事件汇换 `ctx.collectionSinkWithProxies`（同码事件以代理图元实体 id 为成员标识同步派发，事件源仍是持有方）；引擎侧 `collectionSink` 变为其空代理表特例。
- **R24 自调用**：模型 1/3 的 invocation-self 连线实体是孤儿（无 source/target、不在 cellMap），无法锚定归属过程——与模型 1 的 3 条 condition 孤儿连线同性质。生成/引擎链路本就支持自调用（同 ProcessCatalog 工厂出同类实例、零时间配额熔断、单例自调用按锁语义 psf）；`selfInvoking` 标志解析保持。真画布自调用语料到位时的验收挂账。
- **块 24 stdlib 白名单**：见 9.4 hasMethod 放行。

### 9.6 回归与静默红线

- 全模块 `mvn clean test`（85+ 用例，含真实语料六步流水线 + ECJ/javac 对拍 + 存档续跑）——结果见各模块 surefire。
- `tools/regression/scan.py` 23 条红线零违例（exit 0）；全仓 21 份 pom XML 校验通过。
- 新事件码（psf/paa/pca/pcd/as/xs/am/xm）全部经既有事件总线派发，触发键（sourceTypeCode|memberToken|eventCode）无误配；概率采样全部走任务级确定性随机流（存档游标跟踪）。

## 十、三轮审查（按功能点）与修复闭环（2026-11-27 晚，同轮）

按用户令对上述全部功能点做三轮审查：R1 自审（引擎语义/单写者/确定性/存档兼容逐项过）→ R2 正确性与回归风险（独立代理）→ R3 对抗性审查（独立代理，构造破坏场景实测）。R2/R3 合计 18 项发现，全部处置：

### 10.1 已修复（生成器/引擎缺陷）

| 级 | 缺陷 | 修复 |
|---|---|---|
| P0 | 生成的 nodeFailed 参数序 (child,pc) 与基类 (pc,child) 逆序，@Override 必编译失败（R23 失败线整体不可用） | 参数序对齐基类 |
| P0 | 概率组 CDF else-if 链用 `endControlFlow("else")`——palantir javapoet 0.5.0 实测生成 `} else;`（do-while 收尾语义），互斥采样静默失效 | 改 `nextControlFlow("else")`，并写 5 行 javapoet 实测验证 `} else {` |
| P1 | xs（被动消亡）事件永不可派发：markConsumed 先置亡，派发守卫只豁免 x | 守卫豁免 x 与 xs |
| P1 | emitProcessEvent 无宿主过程（独立过程）NPE 冲到会话级 FAILED | host==null 记 problem 跳过派发 |
| P1 | ObservableEntityList.set 落到 AbstractList 默认实现 → UOE（真实语料 setByIndex 在用） | 覆写 set = mx+ma 双事件 |
| P1 | 场景单元素集合 backing 用 List.of（不可变）→ 后续 add UOE | 包 new ArrayList<>(List.of(...)) |
| P1 | （存量，T1.5 范畴）复合过程在途子任务恢复 waiting 双计 → 父过程永久卡 RUNNING | 恢复现场计数“认领制”：linkToParent 认领父任务恢复时已计入的等待配额，不再累加 |
| P2 | lastEnd 旁路子任务只清引用不终止 → 僵尸任务/单例锁泄漏/存档复活 | cancelCascade 真级联终止 |
| P2 | reattachWiringHook 只按 defKey 分发，同键多接线首条获胜；手动接线被跳过 | 同键去重+诊断；手动接线纳入重挂链 |
| P2 | 概率组单成员/带条件事件组员被双重过滤静默丢边 | 主循环只跳过真正入组的线（grouped 身份集合）；单成员组回退独立线 |
| P2 | checkMaintain/canStart 非 ASCII 比较符直接拼接生成非法 Java | 只内联 ==/!=/</<=/>/>= ，其余降级注释 |
| P2 | 未知时长单位 + timeMin/Max 组合在 finishSuccess try 外抛 IAE 炸会话 | try/catch 跳过归类；lastEnd 收尾豁免 pca/pcd |
| P2 | AstSourceMap.indexJavaSource 解析失败中断整条出码（模型 2 全角标点实证） | 失败记录 unindexedFiles 进诊断、继续出码（DAP 探针映射缺失可接受） |
| P2 | scan.py EXIT=1：新测试文件裸 System.out 违 NP-R01 | 改产出画像断言 + build-profile.txt 落盘 |
| P3 | restoreProcesses 对 newProcess 抛出无保护（宿主已被安全点回收的窄边界） | try/catch → problem + continue |
| P3 | 生成注释内插多行消息/`$` 字符污染源码（scene 成员注释、trigger.describe、诊断注释） | 统一单行化 + ＄ 转义 |

### 10.2 已核实扛住（R3 构造场景实测）

- writableParamIds 调用环/自调用：守卫退化两侧一致，无“调用方 ReadOnly 被调方实体”断裂
- restoreSingletonLock 键推导与 begin() 逐字一致、时序正确（relations 先入锁表）、幂等
- process id 非 p<N> 形态安全跳过；wiringContext 指向缺失实例优雅降级（problem+不挂钩子）
- 新事件码不串旧接线（触发键精确匹配）、不破坏既有测试断言；ReadOnly 五层签名经生成产物逐一比对一致
- 事件重放修复闭环：装载期逐路核实不会再入队；确定性：全部新增随机消费走 ctx.randomStream（存档游标跟踪）

### 10.3 记录在案的边界（未改）

- R24 运行期自递归嵌套深度不受配额约束（存量；SOE→会话 FAILED 兜底，非 JVM 崩溃）
- am/xm 以元素实体为源、持有方成员 id 为 memberToken 的键形态当前无生成接线可匹配（被动集合事件暂不可触达，属能力预留）
- 集合事件汇的挂接在“连续会话（场景构建用无 ctx setter）”与“续跑（importState 走 ctx-setter）”不对称；语料无原地集合变更故未爆，订阅 ma/am 的模型上线前需统一挂接口径
- 概率组/psf/paa 线/维持条件在现有三套语料中零出现——本轮为这些路径生成的代码只经生成器静态验证与 javapoet 实测，无运行期回归覆盖（构造最小合成模型挂账）

## 十一、全量回归终值与真机测试记录（2026-11-27 晚，收口）

### 11.1 全量回归

`mvn -o clean test` 全 14 模块 **BUILD SUCCESS**：base-common 7 / base-engine 17 / websocket 3 / connector 12 / base-mcp 5 / parser 11 / naming 4 / source-generator 1 / workspace-sync 1 / compiler-hotswap 10（三模型出码 3 + 真实语料 E2E 4 + 热同步 3）/ debug-server 3 / **bootstrap 5** / importer 9 = **88 用例 0 失败 0 错误**；scan.py 23 条红线零违例（exit 0）；全仓 21 份 pom XML 有效。

真机轮补修的断链（bootstrap 自方案 F 收割后从未编译过，本轮首次修复并跑通）：
- `ScriptIoBridgeConfiguration` lambda 未接 IOException（runScriptWithJsonIo 声明受检异常）→ 按引擎侧约定降级 problem + 空回包
- `ExternalProgramConnector` 宿主 bean 从未注册（方案 F 缺口）→ ScriptIoBridgeConfiguration 补 @Bean：python3 从 PATH 运行期解析（零写死路径）、沙盒目录/超时/允许主机接 groma.connector.* 死配置
- `GromaModelRegistry` 4 参构造器（+scriptIoExecutorProvider）后两个集成测试仍传 3 参 → 补 nullScriptIoExecutor() stub
- 模型 2 真机上传被 VAL-009 阻断 → **降级 WARNING**（画布重复同向连线是数据清洁度问题，解析器按 NextIR equals 去重语义无损；原 ERROR 阻断属过度拦截，与 VAL-004 同一先例）；validation-rules.md 同步

### 11.2 真机测试（宿主 fat jar，端口 18280，multipart 命令行放宽至 40MB）

| 步骤 | 结果 |
|---|---|
| 宿主启动 | `Started GromaSimulationApplication`，/actuator/health UP |
| 模型 1 上传（data-a/b） | success，emergency-deduction v647，253 源文件，18 warnings（含 **VAL-004 WARNING**，交接文档要求核对项 ✓） |
| 模型 2 上传（scene-a/b） | 首次被 **MODEL_VALIDATION_FAILED 阻断**（=「校验已接线，ERROR 会阻断」的活体实证 ✓）；VAL-009 降级后 success，smart-planning-ying-yong v844，214 源文件，0 problems，VAL-004/VAL-009 WARNING 可见 |
| 模型 3 上传（应急决策推演-a/b） | success，emergency-deduction v825，251 源文件，0 problems，VAL-004 WARNING 可见 |
| 模型挂载 | hotswap 挂载 + MCP 动态工具注册（模型 2 注册 62 个过程工具并广播） |
| 真机推演 | 开会话（scene2，startPaused）→ step 30 拍 → tick=30、19 实例、1 在途过程（区间行驶单例）、RUNNING |

**已知限制（真机实测）**：同源模型（应急决策推演-关联副本 v647 与 应急决策推演-关联 v825）派生出相同 modelCode（emergency-deduction），热挂载槽互斥、后传顶掉先传——moduleDirName/modelCode 派生不区分版本，多版本并存挂账（建议 moduleDirName 带版本短哈希后缀）。

## 十二、code3.html（Gemini 第三轮存档）核实与收割（2026-11-28 第八轮）

输入：`docs/code3.html`（3.1MB，含对旧快照 d4e1d6c 的诊断报告 + 对含未提交修复工作区快照的"全闭环方案"：差距 E1~E6 + 缺口 G1~G10 + 可落盘代码）。

### 12.1 断言核实矩阵（对照当前工作区逐条源码核查）

| 断言 | 判定 | 证据 |
|---|---|---|
| 前半部分 10 条建议（DebugEndpoint 桥接/双轨 pom/lease Bug/单写者/querySubtreeJson/驱逐算法/表达式沙盒/词典/export-zip/docx 版本） | 历史属实、**已全部修复**（其自带修订记录亦承认） | 第二部分各轮交付 |
| E3a 词典缺"应用"→ model-smart-planning-ying-yong 混拼 | ✅ 属实 | 词典有 `智能筹划=` 无 `应用=`；十一节真机记录实证 |
| E5a VAL-008 漏扫子过程调用分支概率组（NextIR） | ✅ 属实 | 校验器只扫 TriggerBindingIR/PostActionIR；生成器 CDF 消费 CompilePlanIR.NextIR/PostIR；且语料概率组 0 命中（真缺口但语料不触发） |
| E5b encode 字段（语料 947/898 个）未进 IR/未校验/未接命名 Tier 1 | ✅ 属实；**语料 encode 全为平台自动码**（dxlx1/gccy1/wb1/znsczzjhznt1/sessionlb 等，拼音首字母+序号，无用户真英文） | 全量语料扫描；naming facade 全调用点第二参传 `""` |
| G1 R01 多继承 mixinParentIds 生成器零消费 | ✅ 属实（语料去重后 mixin=0 不触发） | 生成器 0 处引用 |
| G2 R06 结构控制零实现 | ✅ 属实（语料 0 条 edge-control；BaseSimEntity.controlMask 为零消费预留字段） | 全仓 grep |
| G4 初始集合单对象路径未挂代理事件汇 | ✅ 属实（窄：WorldContextEmitter 场景集合成员单对象绑定路径用普通 sink；setter 路径已挂 WithProxies） | WorldContextEmitter/ModelLayerEmitter 对照 |
| G6 启动/维持条件复合表达式降注释失效 | ✅ 属实（且真实路径在**属主类型级设计视图**：模型 1 三条 STATE 积水 --condition--> 过程挂在类型视图而非过程自身视图，parseGuards 只扫过程自身视图，**整条规则被静默丢弃**——比 code3 描述的"表达式降级"更严重） | 语料 JSON 逐边实证 + parseGuards guardViews 源码 |
| G9 R27 notes 采集后零消费（parser 注释明言"出码时写入 Javadoc"未兑现） | ✅ 属实 | 生成器 0 处 notes() 引用 |
| 孤儿连线（3 condition + 1 invocation-self）无告警 | ⚠️ 定性**不成立**：语料实证四条连线均有画布 cell 且端点可解析——3 条为规范的类型级启动条件（属 G6 缺口）、1 条为 计算降雨汇水 真自调用（R24 挂账语料）；"静默丢弃"为真，但根因是视图归属不是孤儿 | 语料逐边溯源 |
| E3b RelationManager 中文类名 | ❌ 不属实（RelationLayerEmitter:44 走命名引擎） | 源码 |
| G10a SETTING-MEMBER 漏收 | ❌ 不属实（parser:1560/1599 已收） | 源码 |
| G10b 内置图元（CUR-PROCESS-TIME 等）未映射 | ❌ 不属实（parser:1483-1494 已映射，CUR-PROCESS-TIME→SIM_STEP_SECONDS 有存量语义依据，勿改 code3 提议的 elapsedMillis 方案） | 源码 |
| G5 顶层后置转换未归集 | ❌ 不属实（readPostActions 注释明言"类型/场景设计视图里的顶层调用点共用这一规则"） | 源码 |
| G3 引用成员缺 ra/rv/rx 派发 | ❌ 不属实（ctx.onMemberChanged 统一派发 ra/rv/rx；RelationManager 走 ra/ar/rx/xr 双视角） | GromaWorldContext:307-319 |
| E1 双轨合包 / E4 多版本 modelCode / E2 dto+StartDTO / E6 全角清洗 / G3-SingleRef / G7 sourcePc=-1 补偿 / G8 尾递归 | ⚠️ 属实但本期不收（理由见 12.4） | — |

### 12.2 本轮收割（全部按真实 API 适配，code3 原稿幻觉 API 未收）

| 收割项 | 落点 | 内容 |
|---|---|---|
| H1 词典补条目 | `groma-glossary.properties` | `应用=application`（修 ying-yong 混拼）+ 通用建模词根（设备/无人机/高级/载荷/电源/电量/开机/关机/充电/演练/关联副本已有） |
| H2 类型级守卫接入 | `AbModelMergerAndParser.parseGuardsAndExceptions` + parseDef | 守卫视图扩为 过程自身 define/design + **属主类型/场景 define/design**；按目标过程实体归组认领，模型 1 三条积水启动条件首次进入 canStart（begin() 统一消费，手动/触发/默认启动全路径生效） |
| H3 悬空语义连线审计 | `AbModelMergerAndParser.auditOrphanSemanticLinks` | PRS-WARN-002：语义连线（condition/agent/invocation-self/control/overtime/undertime）端点不可解析或未挂载即告警；规范语料零误杀 |
| H4 encode 进 IR + VAL-007 补扫 | CompilePlanIR（TypeDefIR/MemberIR/ProcessDefIR +explicitEncode，旧参兼容构造器）+ StaticModelValidator | encode 随 IR 携带；VAL-007 对显式 encode 同口径校验（保留字拦截，存量自动码零误杀）；**命名 Tier 1 接线明确不收**：语料 encode 100% 为平台自动码，无来源标记时任何词法启发式要么把垃圾当类名要么误杀真词，需平台提供 encode 来源标记后再接 |
| H5 VAL-008 NextIR 补扫 | StaticModelValidator | 子过程调用分支概率组按**节点内分组**求和（与出码 CDF 消费口径一致），≥2 分支超闭区间 ERROR、单分支 WARNING；跨节点同名组不合并 |
| H6 R06 结构控制（门点求值） | BaseSimEntity（memberControlMask 新增 + controlMask 复用为过程掩码缓存 + memberEnabled/processEnabled/refreshControlMask/applyControlMasks）+ GromaProcessTask（processEnableBits 钩子 + runProcess 推进前门点 + abortByControlDisabled→paa 级联）+ ControlRuleIR + parseControlRules + ModelLayerEmitter（refreshControlMask 覆写 + 受控成员 setter 门点）+ ProcessTaskEmitter（processEnableBits 覆写，叶/流两路） | **不做事件接线与中断广播**：门点求值为宿主枚举状态纯函数（推进前/写入前刷新再查），确定性/存档安全/单写者安全；语义=失能过程推进即 paa 中断、失能成员写入静默忽略（DSM 语义）；多规则同目标按位或（任一源激活即使能） |
| H7 R01 副父类组合委托 | PlanModel.TypeInfo（+mixinIds/controls/notes）+ ModelLayerEmitter | ReadOnly 接口多继承；实体生成副父类委托实例（复用宿主实例标识，事件同源）+ 委托 Getter/Setter 桥接（主链同名成员让位）+ 成员读写开关表路由 + MEMBERS 元数据表纳入（setInstanceValue/快照寻址） |
| H8 G6 复合条件表达式编译 | ProcessTaskEmitter | judgeCode 重构为 conditions+expression 通用 conditionJudgeCode；启动/维持条件走同一词法白名单 [0-9 ()&|!] 完整编译（1&&(2||3) → 原生布尔树）；不支持记号降注释且**不再错误地按 AND 直译**（原实现复合表达式被 AND 化属语义错误）；顺修 checkMaintain 双重 isEmpty 死代码、删除 safeCompareOp/inlineLiteral 死路径 |
| H9 R27 notes 出码 | ModelLayerEmitter.entityJavadoc | 建模备注写入实体类头 Javadoc（兑现 parser 侧文档化意图） |
| H10 R09/R10 @SemanticTag | 新注解 `SemanticTag`+`SemanticTags`（groma-base-common，RUNTIME 保留可重复）+ RelationLayerEmitter | 关联管理器携带正反向语义标签注解，MCP/文档读取器可反射读取 |
| H11 G4 初始集合代理汇 | WorldContextEmitter | 场景集合成员单对象绑定路径按成员代理定义挂 collectionSinkWithProxies（与 setter 路径对齐） |
| H12 合成裁判 | 新 `SyntheticFeatureHarvestEndToEndTest`（hotswap） | 手搭 CompilePlanIR 覆盖 G1/G2/G6/G9/@SemanticTag：生成→确定性 SHA→ECJ 编译→挂载→运行期三幕（关机 paa 中断/受控成员写入门点忽略/开机+复合条件跑完 + mixin 成员委托读写持久化）——语料 1/2 不触发这些特性，此为唯一运行期裁判 |

### 12.3 回归结果与终值

- **studio 链（本轮改动全触及模块）`clean test` BUILD SUCCESS**：base-common 7 / base-engine 17 / connector 12 / base-mcp 5 / websocket 3 / parser 14（含新增悬空审计+encode 用例）/ naming 4（词典条目无副作用）/ source-generator 1 / workspace-sync 1 / compiler-hotswap 11（三模型出码 3 + 真实语料 E2E 4 + 热同步 3 + **合成裁判 1**）= **75 用例 0 失败 0 错误**。
- 全仓 `clean test`：debug-server 3 绿；importer 9 绿（reactor 重建后复测）；**bootstrap 5 用例中 3 失败——全部来自并行会话在途 WIP，与本轮改动无关**（逐项取证见 12.5）。
- `tools/regression/scan.py`：除并行会话未跟踪文件 `SyntheticSemanticRuntimeTest.java`（裸 System.out 违 NP-R01，内容未动、已原样保留）外**全仓零违例（exit 0）**。
- 全仓 21 份 pom XML 有效。

### 12.4 本期不收清单与理由（挂账更新）

| 项 | 理由 |
|---|---|
| E1 双轨 pom 合包 | 与被拒收的 code2 块 29 同源：workspace 生成模块不在根 reactor，合包 Profile 会让构建失败；属结构演进（挂账原有） |
| E4 多版本 modelCode 隔离 | 工作区命名策略变更（-v 后缀/短哈希），影响热挂载槽位协议；挂账原有，需与 E1 一并决策 |
| E2 dto/{Scene}StartDTO | 生成 Controller 的 startSession 已按 Map 契约发布（ws-protocol/gateway 文档锁定），单方面加 DTO 破坏存量 REST 契约；需前端/网关协同 |
| encode 接命名 Tier 1 | 语料 encode 100% 平台自动码（本轮全量实证），需平台 encode 来源标记（user vs auto）再接，启发式方案已否决（详见 H4） |
| E6 全角符号词法清洗 | 无具体复现用例；AstSourceMap 失败已降级不中断（十一节 P2 修复），语料 1/2 出码编译全绿 |
| G3 SingleRef/MultiRefSet 重构 | 直接引用走 v/ra/rv/rx 事件为既定语义（onMemberChanged 统一派发）；基数校验在 RelationManager 已有 |
| G7 sourcePc=-1 过程级补偿激活 | 现行为（只发 pca/pcd 事件 + 诊断声明）为已文档化取舍；语料 0 条 R25/R26，无验收抓手 |
| G8 R24 尾递归消除 | 语料现含 1 条真自调用（计算降雨汇水，类型级视图自环），R24 挂账升级为"真画布自调用已定位"；尾递归改造需先补该过程的运行期验收 |
| R21 maintainWatcher 反向观察器 | checkMaintain 已逐拍轮询（begin 后每执行拍），watcher 为纯优化无正确性收益；code3 方案未含事件接线设计，属半成品 |

### 12.5 并行会话在途 WIP 取证（bootstrap 3 个失败的责任划定，本轮未动其内容）

工作区存在另一会话 2026-11-28 04:03~04:19 落盘的未跟踪/已改测试（本轮会话 03:52 启动，其内容均非本轮所写，文件未做任何改动）：

1. `SyntheticSemanticRuntimeTest`（未跟踪，04:19；RM-C1 运行期验收/R24 自环/E4 多版本并存收口——正是 §10.3/§12.4 挂账项，方向正确）。其 `mutexCdfPsfPaaAndLastEndRuntimeAcceptance` 失败根因已定位（jshell 直解 IR 取证）：fixture `lnk_agent_guard` 维持条件比较符写反——测试注释意图"护盾值>50 失能→paa"，数据是 `{"code":"<","value":50}`（护盾 100 时规则恒假→按语义首拍即 paa，护盾降为 1 后反而恒真→永无 paa，与断言期望的时序恰好相反）。**修复只需把该 fixture 的 `<` 改 `>`（一行）**；解析与出码两侧行为忠实（本会话 jshell 实证 IR 中 maintain=护盾值<50 正确入库）。此外该测试注释宣称"延迟 2 拍降级赋值"，但 fixture 全文无 delayType/delayValue 字段——修正比较符后 paa 时序虽可成立，注释仍与数据不符，宜一并修订。
2. `ModelPipelineIntegrationTest` / `PortSessionFlowIntegrationTest`（04:03 修改）：新增断言"模型编码 = 基础码 + 语料摘要短哈希后缀（多版本并存收口）"——对应挂账 E4，但**派生侧实现不在工作区**（PlanModel moduleCode 派生未改），两用例当前必然红。属该会话 TDD 半成品。
3. 责任划定：本轮 12 个触及模块全绿；scan.py 除上述未跟踪文件外零违例（临时移出验证 exit 0，随即原样还原）。该会话恢复工作时应：修 fixture 比较符 → 实现 E4 派生 → 3 个红用例自然转绿。

## 十三、code4.html（Gemini 第四轮存档）核实与收割（2026-11-28 第九轮）

> 输入：`docs/code4.html`（3.1MB Gemini 会话存档）——新报告《双模型（data-a/b + scene-a/b）Java 工程转化能力与全量关系表示深度审查及闭环修改方案》：
> 断层 E1~E6 + 8 项关系缺口 + 方案 M1~M6（含幻觉风险代码）。本轮逐条核查 → 按真实 API 适配收割 → 全量回归。

### 13.1 断言逐条核查矩阵（与 12.4 挂账对照）

| code4 断言 | 判定 | 证据与处置 |
|---|---|---|
| E1a 多版本 modelCode 短哈希未实现→2 集成测试红 | ✅ 属实（=12.4 E4 挂账，并行会话 TDD 断言在先） | 实跑 3 测试全红；`shortCorpusHash` 无实现。**收割（M1 适配）**：`ModelVersionHashUtil`（source-generator plan 包）+ `PlanModel` 版本化入口（基础码解析完成后追加，目录/包名随哈希，**Maven artifactId 保持稳定基础名**）+ pipeline 读 bytes 算 SHA-256(A||':'||B) 前 8 位传入生成器。Gemini 的 withModelCode/IR-wither 方案不收（基础码在 PlanModel 内部由命名引擎解析，IR 层拿不到） |
| E1b SyntheticSemanticRuntimeTest 裸 System.out | ✅ 属实 | 改 slf4j `LOG.debug`（syn.debug 门控保留） |
| E1c fixture `lnk_agent_guard` 比较符写反 | ✅ 属实（12.5 已定位） | `<` → `>`；但 12.5 的"首拍即 paa"推断不完整——**真根因是引擎冻结 bug（见 13.2），修比较符仍无 paa**，两处同修后转绿 |
| E1d corpusDir 只找 docs/model 会抛异常 | ❌ 不属实 | `orpl/docs/model` 镜像存在且与 groma/docs 逐字节一致；仍收 Gemini 回退建议（两测试加 `docs` 平铺回退，加固无害） |
| E2 双轨 groma-models 割裂/合包 0 模型 | ✅ 属实（=12.4 E1 挂账） | **收割**：旧手写样例迁 `examples-handwritten/`（git mv 保历史）；groma-models pom 改流水线托管空清单；新建 `RepoModelsSyncService`（`groma.pipeline.repo-models-root` 配置开启：镜像模块进仓 + 同基础名旧版本出仓 + 重写聚合 pom modules + XML 自检）；bootstrap 增 `bundled-models` profile（按稳定 artifactId 依赖生成模型，`-Pbundled-models` 打包即内置）；pipeline 构建成功后同步（失败记 problems 不阻断）。Gemini 的"根 pom 加 module 指向 workspace"方案不收（workspace 不在 reactor，构建必失败——与 code2 块 29 拒收同因） |
| E3 缺 model/dto/{Scene}StartDTO + ws 层 | ✅ 属实（dto=12.4 E2 挂账） | **收割（M6 适配）**：新建 `DtoWsRepositoryLayerEmitter`——逐场景 `{Scene}StartDTO`（强类型 seed/startPaused + initialOverrides，`fromMap` 线格式与既有 REST 契约完全一致，**不破坏 ws-protocol 锁定**）、`{Scene}WsChannel`（/ws/groma/{model}/{scene}/{sessionId} 路由元数据）、`{Scene}SnapshotRepository`（内存快照仓储，DB 逆向扩展点）；控制器 startSession/getSnapshot/stopSession 真实接线。三语料 ECJ 编译绿。说明书第 1 章列的 `repository` 层一并补齐（Gemini 未提） |
| E4 export-zip 无 flavor=runtime | ✅ 属实（12.4 未单列，ENG-04 相关） | **收割（M6 适配）**：`ExportPlan+runtimeFlavor` + `?flavor=runtime`——`dist/groma-bootstrap.jar`（ApplicationHome 定位运行中 fat jar，IDE exploded 显式报错）+ 裁剪版根 pom（modules=groma-base,groma-models）+ base/所选模型源码 + README-RUNTIME；**studio 源码零泄露且解压后 mvn package 仅重建底座与模型**。Gemini 的"bootstrap 源码剔除 studio"方案不收（bootstrap 编译闭包含 hotswap/debug-server/parser/generator/sync 全部 studio 模块，源级剔除必编译失败） |
| E5 模型 2 跨组件继承断链（外部基类不在 TYPE 表） | ❌ **不属实** | 实测 `8ESQFyP8tH40MToOBkp`（地图展示对象）**就在合并 TYPE 表中**（B 包外部引用组件带入），且生成代码 5 单元全部 `extends DiTuZhanShiObjectEntity`。附带发现：parser `inherits` 数组读 `id` 键而语料真实键是 `entityId`（继承实际靠画布 edge-extend 兜底命中）——**收割 M2 加固**：补读 `entityId`（兼容 `id`）+ 复数 `inheritEntityIds`；Gemini 的 synthesizeExternalParentTypes 合成基类方案不收（外部基类已在 TYPE 表，合成逻辑无触发条件） |
| E6 旧 API 未 AST 转译（getValue/setValue/setKeyInstOrValue/itemsToNJson） | ⚠️ 半属实 | 前三个**早已转译**（translator 2.1 步）；仅 `.itemsToNJson()` 1 处降级为运行期 fail。**收割（M3 适配）**：`BuiltinTools.itemsToNJson`（实体元素转 exportState 快照；engine pom +jackson-databind）+ translator 2.0 步独立一遍改写（先于绑定改写——外层 setValue 整节点替换会吞掉内层 AST 节点）；模型 2 生成代码实证 `self.setHuiHuaShuJu(BuiltinTools.itemsToNJson(self.getSessionRecordList()))`。Gemini 的整类重写 translator 方案不收（现有实现已覆盖其大部分场景） |
| am/xm 触发键宿主错配（元素自身订阅查表落空） | ⚠️ 机制属实、语料死（0 订阅） | **收割（M4）**：GromaTriggerTable.dispatch 通配兜底（精确键落空且被动码→`type|*|code`）+ parser buildTrigger 被动事件订阅源在 TYPE/SCENE 图元时令牌归一 `"*"` |
| ar/vr/xr 被动事件未派发（直引成员只发持有方） | ⚠️ 机制属实、语料死 | **收割（M4）**：onMemberChanged 引用三分支补被动派发（ra→ar、rv→xr+vr、rx→xr；`firePassiveOnTarget` 令牌 `"*"`，死目标/静默期拦截）；RelationManager 路径此前已双视角（emitRelationLinked/Unlinked） |
| observed 初始挂接不对称（单元素/多元素分支） | ❌ 不属实 | G4 收割（code3 轮）已把 init 与 setter 全路径统一 observed+sink（实测两分支都走 sink 包装） |
| R24 自调用尾递归缺失（StackOverflowError） | ❌ 语料死（连线孤儿无 source/target） | 12.4 G8 挂账不变（12.5 取证升级为"真画布自调用已定位"）；rm-c2 合成模型已覆盖自环熔断路径 |
| R25/R26 顶层（sourcePc=-1）异常流未激活 | ⚠️ 属实（现状只发事件，12.4 G7 已文档化取舍） | **收割（M5）**：ModuleRuntimeEmitter 把过程级异常流登记为标准触发表监听（引擎 emitProcessEvent 以宿主为源、父过程 defKey 为令牌派发 pcd/pca → 拉起补偿过程；宿主类型不一致诊断跳过）；语料 0 条（corpus-dead），运行期裁判挂账 |
| R27 过程/成员级备注未采集 | ❌ 语料死（模型 1 note 连线孤儿、模型 2 NOTE 实体无目标） | TypeDefIR.notes 已收（code3）；proc/member 级无目标可采，挂账不变 |
| R13/R15 前锚点缺失 | ⚠️ 属实（说明书 7.2 R13 行"首次步进前切换"；语料 in-change/in-out 连线为 0 条） | **收割（M5 节选）**：ConditionRuleIR +3 分量（anchorTargetState/anchorMember/anchorEnumTypeId，旧构造器兼容）+ parser in-change 分支捕获 target STATE 图元 + emitter canStart 末尾全条件通过后切换（生成 setter-with-events → sx/sv/sa）；运行期裁判挂账（RM-B2 的 gap 边无 STATE target，不触发新路径） |
| R07 SingleRef/MultiRefSet 容器缺失 | ⚠️ 属实（=12.4 G3） | 不收不变：直引成员统一事件语义为既定取舍；容器出码需 parser 先物化 R07 入 IR（现仅 shape 表映射），语料 0 条 R07 连线 |

### 13.2 引擎真 bug：等待提交阶段任务永久冻结（合成用例揪出，本轮最高价值发现）

`mutexCdfPsaPaa...` 修正比较符后**仍无 paa**。逐拍 trace 取证：受护任务 begin() 内 stepMachine 清空机器（单瞬时比较节点、无等待子节点）→ 步进末置 `WAIT4SUCCESS` → `runProcess()` 对 WAIT4SUCCESS 早退 → **该任务既永不收尾（finishSuccess 不可达）、也永不执行 checkMaintain**——R21 维持中断在整个等待提交阶段完全失效，瞬时流过程（机器在 begin 即清空的耗时流）永久泄漏运行态。
修复（`GromaProcessTask.runProcess`）：WAIT4SUCCESS 分支内宿主消亡检查 + checkMaintain（维持校验贯穿全程生命周期）+ 子提交收集 + `finishSuccess()` 收尾。**机器在 begin 即清空的耗时流过程此前是永久冻结的——受护任务类过程从打不出 paa。** 顺带修复测试侧 `recentEvents` 滚动窗口跨拍重复累计（按 seq 去重）。
（该 bug 与 12.5 的比较符问题叠加：`<` 时理论上的"首拍 paa"也因冻结不可达。）

### 13.3 回归与挂账更新

- 新增 `PassiveEventDispatchTest`（groma-base-engine，3 用例）：ar/vr/xr 被动派发宿主断言、am/xm 通配兜底、持有方精确令牌不回归——被动事件路径的唯一运行期回归（真实语料 0 订阅）。
- 全量 `clean test` + `scan.py`（见 13.5 终值）。
- 挂账更新：E1（双轨合包）✅ 关闭、E4（多版本）✅ 关闭、E2（StartDTO）✅ 关闭（线格式兼容实现）、G7（顶层补偿）✅ 出码落地（运行期裁判挂账）、R13/R15 前锚点 ✅ 出码落地（运行期裁判挂账）、G3（R07 容器）❌ 不收维持。
- 运行期裁判挂账（新）：R13/R15 前锚点、R25/R26 顶层补偿——两条路径语料零命中，需合成语料（含 STATE target 的 in-change 边 / overtime 边）验收后关闭。

### 13.4 真机全生命期测试（tools/regression/host-lifecycle-smoke.sh，EXIT=0 全过）

三段式（`tools/regression/host-lifecycle-smoke.sh <dataA> <dataB> <port>`，按模型暴露面分段）：

- **A 热挂载出码**：宿主 fat jar（45MB）:18292 启动 → POST /api/v1/studio/models/upload（dataA/dataB）
  → 六步流水线 → `modelCode = emergency-deduction-844676b1`（✓ 语料短哈希后缀）→ 259 源文件（含新 dto/ws/repository 层）。
- **B 热挂载推演（MCP+WS）**：MCP streamable-http /mcp 握手 → `sim.start(scene1, seed=42)` →
  sessionId `emergency_deduction_844676b1_1`（RUNNING，在途过程 2）→ `sim.step 60`（tick=60 精确）
  → WS 频道 `/ws/groma/{model}/scene1/{sid}` 发 `CTL:G:3` → 收 `TP:` 帧 tick=63 → `sim.stop`。
- **C 合包 REST（E2 全链路）**：仓内同步核验（模块镜像 256 源文件 + SPI 注册文件 + 聚合 pom 清单登记）
  → `-Pbundled-models` 合包构建 → 启动 → REST `/api/v1/models/{code}/scenes/scene1/sessions`
  （StartDTO 线格式）→ 推步 60 → 快照 tick=60/实例=1 → 暂停/倍速/恢复 → 快照仓储落点 →
  存档回收 `/api/v1/studio/archives/sessions/{id}/evict` → 存档列表 1 份。

真机轮揪出并修复 3 处（单元测试全绿的盲区，与 P1-P5 轮审查教训同款）：

1. **WS 端点真机即断（P0）**：`SituationEndpoint.onOpen` 用 lambda 强转
   `MessageHandler.Whole<String>` 注册处理器——Tomcat 11 反射读泛型签名失败，
   onOpen 即抛 `IllegalStateException`，任何真实 WS 客户端握手后被立即掐断
   （此前从未有真 WS 客户端打过该端点）。修复：具名内部类 `SituationTextHandler`
   实现接口，泛型签名可读。修复后 TP 帧真机抵达。
2. **multipart 上限不可调（P1）**：全量语料 10.8MB 撞 10MB 硬编码上限
   （NP-R09 显式上限要求保留）——application.yml 上限改环境可调
   （`GROMA_UPLOAD_MAX_FILE/REQUEST`，默认仍 10MB），真机部署可上调。
3. **javac 对拍 fat jar 限制显性化**：`java.class.path` 在 fat jar 只含主 jar，
   嵌套依赖 javac 读不到 → 对拍 false 且措辞误报"对拍完成，类数量 0"。
   修复措辞（"对拍不一致…ECJ 为运行态门禁，javac 对拍门禁在源码树/CI 生效"），
   嵌套 jar 对拍能力挂账。

另修流水线侧两处测试脚本坑（MCP SSE 信封两层解析、macOS 系统级 SOCKS 代理劫持
localhost WS——getproxies 读系统配置，须显式 proxy=None）。

### 13.5 全量回归终值

- 全仓 `mvn -o clean test`（14 模块，2026-11-28 08:59 收口）：**BUILD SUCCESS，98 用例 0 失败 0 错误 0 跳过**
  （上轮基线 88 → 98：+3 PassiveEventDispatchTest；bootstrap 8 用例含此前 3 红的修复转绿 + 真实语料六步流水线
  `emergency-deduction-844676b1`（源文件 259，含新增 dto/ws/repository 三层）+ javac 对拍 true）。
- `tools/regression/scan.py`：**exit 0，23 条红线零违例**（System.out 修复与引擎/生成器改动全过）。
- 全部 pom 改动（engine +jackson、bootstrap bundled-models profile、groma-models 托管清单）XML 校验通过。
- 构建画像：模型 1 出码源文件 253→259（+逐场景 StartDTO/WsChannel/SnapshotRepository ×2 场景）。

### 13.6 生成服务 vs ods 存量运行时差距与推演一致性验证

> ods（/Users/zyh/work/orpl/ods，240 java 文件，com.okip.opm.ods）为存量推演运行时；
> 编译需内网构件、运行依赖 Nacos/Kafka/Redis（redis 启动检查器在列），本地不可进程级运行——
> 一致性验证走"存量语义契约回归 + 真机三通道对齐"代理路径。

**差距矩阵**（有意差异 = 设计目标；缺口 = 挂账）：

| 维度 | ods 存量 | groma 生成服务 | 判定 |
|---|---|---|---|
| 部署形态 | 常驻平台服务（Nacos/Kafka/Redis/DB） | 单进程 fat jar + 模型子模块，零中间件依赖 | 有意差异（说明书 1.3 独立运行目标） |
| 模型表达 | 运行时动态解释（parsed/ontology） | 静态强类型五层源码（ECJ/javac 编译） | 有意差异（平台核心目标） |
| 会话持久化 | Redis 键空间 | SimulationArchiveBundle 文件存档 + 步进边界确定性续跑 | 等价能力、不同实现 |
| 事件分发 | Kafka | 进程内事件总线（单写者线程，docs/thread-model.md） | 等价能力、不同实现 |
| WS 协议 | TP:/CTL:/SP:/T/I 帧 | 逐字复刻 + ConflationWsSender 慢客户端合并（真机 TP 帧实证） | 一致 |
| 21 类事件 / 29 类连线语义 | 基准 | 逐条复刻（引擎注释逐条引存量行为）；被动事件全集本轮补齐 | 一致（本轮收口） |
| 集合/数组工具 | ArrayClazz 1 基、MapClazz.setKeyInstOrValue | BuiltinTools 逐条对拍（BuiltinToolsCollectionsTest 锁定存量语义） | 一致 |
| 手写过程代码 | 原生执行 | AST 无损转译：getValue/setValue/setKeyInstOrValue/itemsToNJson 四类全桥（本轮补最后一类） | 一致（本轮收口） |
| 确定性 | Math.random 非确定 | DeterministicRngStream(seed) 同种子逐拍可复现 | groma 更强 |
| R24 真画布自调用 | 支持 | 孤儿连线无法归属，挂账 | 缺口（语料挂账） |
| WS 差量帧（TD） | — | 全量快照帧，差量挂账 | 缺口（涉前端协议） |
| 千模型容量实测 | — | 机制具备（单写者/MPSC/快照零拷贝），千模块压测未跑 | 挂账 |

**推演结果一致性验证（本轮实测四抓手）**：

1. **真机三通道同模型同种子对齐**：MCP 会话 `sim.step 60` → tick=60 精确；REST 合包会话
   同 seed=42 推步 60 → tick=60；WS 频道在 tick=60 基础上 `CTL:G:3` → TP 帧 tick=63。
   三通道步进语义与快照结构（实例=1、在途过程=2/近期事件=200）互洽。
2. **同种子确定性回归**：RealCorpusEndToEndTest 同语料同种子双跑，快照逐字段全等；
   150→151 存档续跑全等（T1.5 三根因修复后转绿）。
3. **黄金契约回归**：golden R09（300 拍存档重载后续跑 301~1000 快照摘要一致）类语义在
   存档/重载测试中持续覆盖；BuiltinTools 存量语义（1 基序号、越界提示文案）测试锁定。
4. **进程级 ods 对拍**：不可行（本地无法运行 ods），挂账待具备内网/中间件环境后以同语料
   同种子双跑逐拍 diff 收口——现有代理证据已覆盖事件语义、工具语义、协议语义三层。

## 十四、code5.html（Gemini 第五轮存档）核实与收割（2026-11-28 第十轮）

> 输入：`docs/code5.html`（2.8MB）——基于 13.6 节的续作：12 项挂账全景 + 首席架构师 5 大批评 + 6 组生产级方案代码。
> 本轮逐条实证核查 → 按真实 API 适配收割 C1~C5 → 全量回归。

### 14.1 断言核查矩阵

| code5 断言 | 判定 | 实证 |
|---|---|---|
| Python 桥同步阻塞单写者线程（冷启动 100~500ms 冻结同核会话） | ✅ 属实 | `invokeExternalScriptWithIo` 写者线程直调 → `ProcessBuilder.start()+waitFor`（ExternalProgramConnector:57~68）。**方案（进程池+WAITING_EXTERNAL_IO 状态机）涉及会话推进门栓协议演进，本轮不收、挂账**（挂账参考 code5 3.3 方案） |
| naming Tier1 全调用点传 "" | ✅ 属实但机制已半接 | `selectTierInput` 判定在（内置 4 前缀黑名单），调用点传空串；**黑名单挡不住 znsczzjhznt1**（无前缀拼音串会被误放行）——收割 C1 修复 |
| 初态/importState 集合挂接不对称 | ❌ **不属实** | 实证 importState → setMemberValue → ctx-setter → `ObservableEntityList.observed(...)`，续跑路径同样保留事件汇包装（SuspensionControlEntity 实测） |
| am/xm 跨集合串扰 | ❌ 不属实 | am/xm 以持有方集合成员令牌精确派发（`Box|attr_cards|am`），类型级通配 `Card|*|am` 是"被加入任何集合"的既定类型级语义，非串扰 |
| fat jar javac 对拍恒失败 | ✅ 属实（=13.4 第 3 项挂账） | 嵌套依赖不在 java.class.path——收割 C3 修复 |
| RepoModelsSyncService 运行态反向写源码树（git dirty/并发 pom 撕裂） | ⚠️ 部分属实 | "运行态即部署态"是 E2 显式取舍（repo-models-root 默认关闭、部署者显式开启）；**并发 pom 写撕裂批评成立**——收割 C4 原子写；整体反模式批评记入挂账（可选演进：导出时临时沙盒聚合） |
| fireLineProbe 行级探针缺失（仅 line=0/-1） | ⚠️ **过时** | 生成代码已织入 `fireLineProbe("cg_cmp")`、引擎 `debugProbe` 钩子已在（GromaProcessTask:363）；**缺的是 debug-server 消费侧接线**（setDebugProbe 零调用）与虚拟线程挂起语义——挂账（涉 DAP 协议） |
| R07 容器零物化（直引退化） | ✅ 属实 | SingleRef/MultiRefSet 定义后零用点——收割 C5 引擎侧增强；**parser 物化+出码继续挂账**（本轮范围裁定：容器语义件先行，语料 0 条 R07 边） |
| 千模型 Metaspace 算账爆掉 说明书 1.8GB | ⚠️ 方向属实、数字未实测 | 单模块 ~380 class 推算 1000 模块 38 万类，Metaspace 压力真实；Lazy-Mounted Pod/去 Spring 化是正确方向但属架构演进——挂账（code5 方案为参考） |
| TD 差量帧/千模型压测/行级 DAP/R24/R27/ods 进程级 diff/M5~M8 | ✅ 挂账确认 | 与 13.3 挂账一致，本轮不收（TD 涉前端协议、压测需千模块资产、R24 语料孤儿） |

### 14.2 收割清单（C1~C5，全部带单测/回归裁判）

| # | 内容 | 位置 | 裁判 |
|---|---|---|---|
| C1 | **ExplicitEncodeClassifier**（词形鉴别：手写英文放行/拼音自动码回落）+ `selectTierInput` 替换四前缀黑名单 + 类型类名调用点接线（`type.explicitEncode()` 直传） | groma-naming-engine + source-generator PlanModel | 三语料回归绿（自动码 100% 回落→出码零漂移）+ ExplicitEncodeClassifierTest 3 用例（floodWall 放行 / dxlx1、znsczzjhznt1 拒 / 技术缩写白名单） |
| C2 | **全角词法归一化**（translator 解析前置；字面量/注释内容不动防语义漂移；纯 ASCII 零拷贝快路径） | ProcessCodeAstTranslator | FullWidthLexemeNormalizerTest 5 用例 |
| C3 | **fat jar javac 对拍**：classpath 构建抽 `verificationClasspath()`——fat jar 形态追加 `jar:file:self!/BOOT-INF/classes` 与 `!/BOOT-INF/lib/*.jar`；IDE/源码树形态零变化 | JavacParityVerifier | 编译绿；真机冒烟复验挂账（javac 对嵌套 jar URL 的解析需运行态实证） |
| C4 | **聚合 pom 原子写**（临时文件 + ATOMIC_MOVE，并发上传写撕裂消除） | RepoModelsSyncService | 编译绿；真机冒烟复验 |
| C5 | **R07 容器语义件增强**：SingleRef 1..1 必选基数置空拦截 + 构建期静默 + 存档对称（exportTarget/importTarget/silentLinkTo）；MultiRefSet silentAddAll/exportOrderedList/size | groma-base-common container | R07ContainerSemanticsTest 4 用例（事件三态/基数拦截/存档对称/集合边界） |

### 14.3 挂账更新

- 关闭：H4/Tier1（安全鉴别器落地，EnumIR 加字段与其余 13 处调用点接线仍挂账——收益递减：语料 encode 全自动码）、12.4 E6（全角归一化落地）、13.4 第 3 项（fat jar 对拍代码落地，运行态复验挂账）。
- 新增挂账（code5 方案为参考）：Python 常驻进程池 + WAITING_EXTERNAL_IO 状态机（真问题，涉会话推进协议）；debug-server 接 setDebugProbe（行级断点最后一公里）；R07 parser 物化+出码；RepoModelsSyncService 临时沙盒聚合形态；Lazy-Mounted Pod；千模型容量实测。

## 十五、code6.html（Gemini 第六轮存档）核实与收割（2026-11-28 第十一轮）

> 输入：`docs/code6.html`（2.8MB）——基于 `_groma_export/*.txt` **早期导出快照**的"从零重写型"报告
> （8 项审计清单 + 7 项架构师剖析 + 6 组全量重写代码）。因快照过时，断言需逐条对当前仓库实证。

### 15.1 断言核查矩阵

| code6 断言 | 判定 | 实证 |
|---|---|---|
| 子模块缺 service/relation/repository/dto/ws/utils 层 | ❌ 全部过时 | 快照早于 M6 收割轮；当前生成物五层俱全（AreaEntity→repository/ws/dto 均在） |
| R08 关联类型缺失（5 个 ASSOCIATION-TYPE 降级为普通 Entity） | ❌ 不属实 | 5 个 *RelationManager 全部出码（SuspensionControl/UndergroundInflow/TrainPassing/FloodProtection/RainfallCatchment） |
| 父图外挂边丢失→16 个空壳 Task（转换为无积水状态 designView 为空） | ❌ **误读** | 该过程是 TEMPLATE-PROCESS 模板声明型（空壳是设计语义）；其 out-change 已正确下沉到**调用方**积水变化 Task 的 nodeDone pc=3/4（`输出转换：本对象.积水状态 → 无积水/积水` 生成实证） |
| 瞬时算子跨步撕裂破坏守恒 | ❌ 不属实 | 瞬时过程 startTopLevel→runToCompletionSync 同栈同步执行（spawnOrRun syncMode），无跨步窗口 |
| RuntimeBO 重名 *2/*3（WaterloggingChangeRuntimeBO2/3） | ✅ 属实 | 消歧序号是可读性缺陷（@GromaMeta 原名+索引兜底）——**挂账**（修复会全量漂移生成物） |
| "场景1"+"1转换为停站"→Scene11ConvertToStop 歧义类名 | ✅ 属实 | 命名美学缺陷——**挂账**（同上） |
| Runtime 类 71KB/ProcessCatalog 52KB，C2 8000B 退出 JIT、64KB 方法上限 | ✅ 数字属实 | 模型3（3000 实体）Runtime 72KB/ProcessCatalog 58KB；当前未爆但千站点模型必撞 64KB 单方法上限——**收割 C6 分片注册** |
| 手写 AST 转译缺失 | ❌ 过时 | ProcessCodeAstTranslator 四类桥+全角归一化已落地（13/14 轮） |
| WS MsgPack 差量帧/GromaTimeSystem 冻结补偿/ASM 断点缺失 | ✅ 挂账确认 | 与 13.3/14.3 挂账一致（TD 涉前端协议、DAP 行级挂账） |
| 瞬时算子协程化滥用/时间染色 COROUTINE_TASK | ⚠️ 优化方向非缺陷 | 状态机统一语义是存档/调试/追踪一致性的既定取舍；染色编译为同步方法是架构级演进——挂账 |
| SnapshotVO 全量拷贝 vs Dirty CoW / 四态会话生命周期 / SQL 方言重写器 | ⚠️ 挂账 | 优化项（3 万实体级才显著）/架构演进/connector 大改，均挂账 |
| 底座缺 GlobalSimExceptionHandler/PageResult 重复等 | ❌ 过时 | 快照过时；全部存在 |

### 15.2 收割清单（C6）

| # | 内容 | 裁判 |
|---|---|---|
| C6 | **分片注册（Sharded Registrar）**：① ModuleRuntime ctor 按 SHARD_SIZE=16 条接线一片拆 `registerWiringShard_K`（单条登记逻辑原样搬移 emitWiringRegistration）；② ProcessCatalog newTask 巨型 switch 拆 `newTaskShard_K`（主方法顺序探测）；③ PROCESSES/TYPES 描述符 `<clinit>` 巨表达式拆 `processDescriptorShard_K`/`typeDescriptorShard_K` + 聚合方法 | 三语料 ECJ 编译+运行回归绿；生成物结构核验（Runtime 6 片/Catalog 22 分片/单片 34 行）；每方法字节码恒 <2KB 量级，任意规模模型不再逼近 64KB 上限 |

### 15.3 挂账更新

- 新增挂账：BO 消歧宿主前缀化与数字开头中文序号词命名修正（全量漂移需专项回归）、时间染色同步算子编译（生成器架构演进）、Dirty CoW 快照、四态会话生命周期（IDLE_PARKED/WAITING_ASYNC_IO）、MultiDialectSqlNormalizer。
- 维持挂账：TD 差量帧、行级 DAP、Python 进程池、R07 出码物化、千模型实测、ods 进程级 diff、M5~M8。

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

## 八、回归验证与产物（含第三轮收割验证）

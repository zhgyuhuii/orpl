# 方圆语言（Groma Language）2D/3D智能建模与多端推演平台——设计说明书（V12.0）与源码深度对标、内容补全及架构优化报告

> **分析基准**：
> 1. 需求与架构说明书：`docs/方圆语言2D_3D智能建模与多端推演平台_需求与技术架构设计说明书.docx`（版本 V12.0，基准《方圆语言标准-1117》）
> 2. 仓库全量源码：`groma/`（含 `groma-base` 5 子模块、`groma-studio` 8 子模块、`groma-models` 双样本模块、`groma-bootstrap` 宿主、`docs/` 7 份专项规约、`tools/regression/` 门禁脚本）

---

## 第一部分：说明书现状诊断与“图-文-码”差异总览

对 V12.0 设计说明书与当前工程源码逐模块交叉核验后，发现说明书存在**结构性章节断层**、**模块清单滞后于源码演进**以及**若干工程实现细节与文档承诺不一致**三大类问题。

### 1.1 说明书自身的结构与排版缺陷
1. **章节序号断层与错乱**：
   - 第四章结束后直接跳至“**第五章** 全量 28 类关系……”，但其下属二级标题却标为 `7.1`、`7.2`、`5.3`（疑似从旧版合并时遗留的编号错位）。
   - 第六章（断点调试）之后紧接着“**第七章** 单工程并发运行……”，其下属二级标题却标为 `5.1`、`5.2`。
2. **核心需求章节缺失**：文档标题包含“需求与技术架构设计说明书”，但正文从第一章直接切入 Maven 目录结构，缺少**业务需求全景、角色用例、非功能性指标（SLO）基线、安全与合规边界**等标准需求章节。
3. **LaTeX / 符号转义残损**：第六章 6.3 节第 4 点中出现裸露断行文本 `$ ightarrow$`（原意为 $\rightarrow$ 箭头符号被错误转义截断）。

### 1.2 说明书描述 vs 真实源码实现偏差矩阵

| 对比维度 | 说明书（V12.0）描述 | 源码真实实现现状 | 差异定性与处理建议 |
| :--- | :--- | :--- | :--- |
| **`groma-base` 子模块数** | 仅列出 **4 个**子模块（common / websocket / engine / connector），将 MCP 放在 M7~M8 远期路线图 | 已完整实现 **5 个**子模块，新增 `groma-base-mcp`（含 18 个固定工具、7 类只读资源、动态工具注册与指标埋点） | **文档滞后于代码**：需在第一、二章正式补齐 `groma-base-mcp` 架构详设 |
| **`groma-studio` 子模块数** | 仅列出 **6 个**子模块 | 实际包含 **8 个**部分（7 个 Java 模块 + `groma-studio-web`），多出 `groma-model-import` 与独立的 `groma-naming-engine` | **文档滞后于代码**：需补入语料拉取不可变仓库与独立命名引擎模块说明 |
| **画布连线与事件数量** | 全文统称 **“28 类关系 + 15 类事件”** | `docs/shapes-and-fields-dict.md` 与解析器实际支持 **29 种连线形状**（含 `edge-link` R00）与 **21 种标准事件**（含反向被动事件 `as/xs/am/xm/ar/vr/xr` 及 `paa`） | **口径不一致**：需修正为 29 类连线与 21 类事件，补齐缺失的 6 类事件语义 |
| **Web 容器与 WS 协议** | 第七章称采用 **Undertow** 容器；第二章称 WS 上行采用 **JSON-RPC 2.0**、下行支持 **MessagePack** | `groma-bootstrap` 实际使用内嵌 **Tomcat** + JSR-356 原生端点；WS 实际采用轻量文本前缀协议（下行 `TP:<JSON>`，上行 `HRT:/CTL:/SP:/T/I/TREE:`） | **图文不符**：应按已落地的 `ws-protocol.md` 修正协议描述，或明确 JSON-RPC/MsgPack 为 v2 演进规划 |
| **两处 `groma-models` 双轨并存** | 称 `groma-bootstrap` 直接合包编译 `groma-models/model-*` | 根下 `groma-models/` 存放手写简化演示代码，而生成代码实际落在 `groma-bootstrap/groma-workspace/groma-models/`，且 `groma-bootstrap/pom.xml` 因类名冲突注释掉了模型静态依赖，改走运行时流水线热挂载 | **关键工程割裂**：会导致用户下载源码执行 `mvn package` 打出的 JAR 默认未静态合包生成模型，需统一治理 |
| **两级热更新（L1/L2）与字节码插桩** | 宣称支持 **ASM 零时间循环熔断插桩** + L1 步进边界原子替换算子 | `groma-compiler-hotswap` 实际采用 ECJ 全模块增量重编 + `ModelModuleClassLoader`（Child-First）按会话版本钉住（Version-Pinned Lease），未引入 ASM 字节码插桩，零时间配额由引擎层计数器实现 | **实现机制偏移**：当前版本钉住方案比 ASM 改写更稳妥，建议更新文档使其与真实类加载器租约架构对齐 |
| **DAP 在线源码断点调试** | 宣称 `/ws/debug/{sessionId}` 已完整跑通 Web CodeMirror 6 + JDI/ASM 虚拟线程软断点 | `GromaWebSocketConfiguration.DebugEndpoint` 目前仅返回 `ACK:DEBUG:ATTACHED:` 占位回执，尚未与 `DapWebSocketRouter` 完成真实 JSON/DAP 报文桥接；`WarmJdiSandboxPool` 为轻量内存模拟桩 | **代码未闭环（缺口）**：需补齐 WS 端点到 `DapWebSocketRouter` 的协议分发及真实 JDWP 进程池连接 |
| **模型子模块内部包结构** | 宣称固定生成 `controller / service / engine / repository / model / ws / utils` 七目录 | 生成器产出的 `model-emergency-deduction` 含 `config / controller / engine / model / service / utils`，缺少生成的 `repository`、`ws`、`dto`、`engine/relation`（关联实体目前生成在 `model/entity/` 下） | **生成器裁剪差异**：需在说明书中精确界定按需生成条件，避免二次开发者困惑 |

---

## 第二部分：设计说明书缺失章节完整补充内容（可直接并入原文档）

以下补充内容严格对齐工程现有代码、7 份 `docs/*.md` 规范及 CI 门禁体系，可直接作为新章节插入原说明书。

---

### 补充章节 A：业务需求边界、核心用例与非功能性指标（SLO）基线（建议作为新第 1 章）

#### A.1 核心角色与端到端业务流
1. **领域建模专家（方圆建模师）**：在 2D 画布（或从存量方圆平台 `data-a.json` + `data-b.json` 导入）构建对象类型、状态机、29 类关系与耗时/瞬时过程，保存即触发毫秒级静态规则校验（`VAL-001` ~ `VAL-010`）与标准五层 Java 源码生成。
2. **Java 二次开发工程师**：在生成的 `ServiceImpl` 或外部连接层 `@GromaUserCode` 保护区内编写复杂行业算法（如水动力学方程、多智能体协同航路规划），利用本地 IDE 或 Web DAP 调试器单步验证，且画布再次保存重生成时手写代码 100% 无损保留。
3. **演训推控员 / 数字孪生终端**：通过 REST API、WebSocket 态势频道（`/ws/groma/...`）操控场景会话启停、倍速切换、单步推演、运行期改值干预，并在 2D/3D 视口实时观测态势。
4. **大模型与决策智能体（LLM / AI Agent）**：通过标准 **MCP Streamable HTTP（`POST /mcp`）** 零会话读取 `groma://models/**` 双语知识图谱资源，调用无状态计算过程或自主创建多分支想定沙盒（What-If 推演）辅助指挥决策。

#### A.2 平台核心非功能性指标（SLO）与确定性基线
- **可复现确定性（Determinism）**：同一模型版本、同一初始覆盖参数与相同 64 位根随机种子（`rootSeed`）连续运行任意次，最终 `WorldSnapshot` 的 SHA-256 摘要必须 **100% 字节级一致**；中途任意步（如第 300 步）导出存档并重载续跑至第 1000 步，结果与直跑 1000 步 **100% 一致**。
- **并发与容量指标**：单实例默认并发活跃会话配额 `200`（可配置扩展至 `1,000+` 轻量会话），常驻计算线程数恒等于物理核数 $N_{\text{cores}}$，千模型常驻堆内存小于 `2.5 GB`，单客户端 50kbps 弱网限速下服务端零队列积压、零 OOM。
- **实时热编译延迟**：单模型增量 ECJ 内存编译耗时小于 `100 ms`，同 SHA-256 源码摘要命中 `AtomicBytecodeCache` 时实现 $O(1)$ 零重编挂载。

---

### 补充章节 B：面向 AI 智能体的 MCP 服务底座架构设计（补齐 `groma-base-mcp`）

原说明书将 MCP 列为远期规划，但源码已在 `groma-base-mcp` 与 `GromaMcpEnginePortImpl` 中完整落地。需在第二章补充如下技术规格：

#### B.1 传输协议与双层工具暴露模型
- **端点与协议**：基于 Spring AI 2.0 MCP Server Starter 暴露 `POST /mcp`（MCP Streamable HTTP，协议版本 `2025-06-18`），请求超时 `90s`，上游网关要求关闭响应缓冲（`proxy_buffering off`）并基于 `Mcp-Session-Id` 配置一致性哈希会话亲和。
- **统一响应信封**：所有工具统一返回 `McpToolEnvelope<T>`，标准错误码枚举涵盖 `MODEL_NOT_FOUND`、`SCENE_NOT_FOUND`、`SESSION_NOT_FOUND`、`INVALID_ARGUMENT`、`LIMIT_EXCEEDED`、`TIMEOUT`、`EXECUTION_ERROR`。

#### B.2 18 个宿主固定工具（`GromaFixedMcpTools`）
所有固定工具均标注 `@McpTool.McpAnnotations(readOnlyHint, destructiveHint, idempotentHint)`，分属三大职责域：
1. **会话生命周期控制（9 个）**：`sim.start`、`sim.stop`、`sim.pause`、`sim.resume`、`sim.step`、`sim.set_speed`、`sim.wait`（支持 `@McpProgressToken` 流式进度通知）、`sim.list_sessions`、`sim.describe_session`。
2. **只读态势观测与分页检索（4 个）**：`sim.query_instances`（支持按类型、状态过滤与游标分页 `PagedResult`）、`sim.get_instance`、`sim.get_processes`、`sim.get_events`。
3. **运行期干预与过程调用（5 个）**：`sim.set_value`、`sim.set_state`（支持直接传入中文状态名自动映射枚举）、`sim.run_process`、`run.process`（不带 `sessionId` 时自动开启临时沙盒无状态执行并即刻销毁）、`model.describe_process`。

#### B.3 7 类零会话只读语义资源（`GromaModelResourceProvider`）
智能体无需消耗会话配额即可直接读取模型本体论知识：
1. `groma://models/{modelCode}/index`：直接返回模块级 `GROMA_MODEL_INDEX.md` 全局中英对照与拓扑图；
2. `groma://models/{modelCode}/types` 与 `groma://models/{modelCode}/types/{typeCode}`：领域对象类型及字段元数据；
3. `groma://models/{modelCode}/processes` 与 `groma://models/{modelCode}/processes/{processRef}`：可调用过程清单与输入输出 Schema；
4. `groma://models/{modelCode}/scenes` 与 `groma://models/{modelCode}/scenes/{sceneCode}`：权威场景构成与默认参数。

#### B.4 动态模型过程工具热注册（`ModelToolRegistrar`）
当新模型通过流水线热挂载时，生成器自动产出 `/META-INF/groma/mcp/manifest.json`，经 `ManifestSchemaValidator` 校验后由虚拟线程异步向 `McpSyncServer` 动态注册 `<modelCode>__run_<processRef>` 专属工具，并主动推送 `notifyToolsListChanged()`，旧版本工具自动平滑卸载。

---

### 补充章节 C：模型语料导入、A/B 双包合并与十项静态校验引擎（补齐 `groma-model-import` & `validation-rules`）

#### C.1 远程只读导入与 SHA-256 不可变语料库（`groma-model-import`）
- **双源接入**：支持本地双文件上传（`POST /api/v1/studio/models/upload`）及存量方圆平台远程拉取（`POST /api/v1/studio/models/fetch-remote`，自动拉取 `loadModel` 作为 `data-a.json` 全量模型定义、`loadModelCell` 作为 `data-b.json` 画布图元布局）。
- **不可变存储与覆盖率度量**：`ImmutableModelStore` 基于内容 SHA-256 摘要寻址存储语料快照；`CorpusCoverageAnalyzer` 自动统计全部 **29 种连线形状**与 **21 种事件代码**的语料覆盖率。

#### C.2 29 种连线与 21 种事件的权威补全
原说明书第五章仅列出 28 种连线与 15 种事件，需补齐以下要素：
- **第 29 种连线**：`edge-link`（`R00` 通用基础连接线，由解析器按两端节点类型归一化或作拓扑辅助）。
- **补齐 6 种反向与取消事件（凑齐 21 种标准事件）**：
  - 指定状态生命周期：`as`（以指定状态出现）、`xs`（在指定状态下消亡）；
  - 集合被动视角：`am`（个体被加入集合）、`xm`（个体被移出集合）；
  - 引用被动视角：`ar`（被建立指向）、`vr`（被变更指向）、`xr`（被解除指向）；
  - 过程异常中断：`paa`（因维持条件失能或父级 `lastEnd` 级联取消导致的中断事件）。

#### C.3 静态模型校验十条铁律（`StaticModelValidator`）
在生成源码前强制执行与《方圆语言标准-1117》条款一一对应的十项编译期拦截：
- `VAL-001`（ERROR）：非耗时算子严禁包含耗时子过程或延迟等待；
- `VAL-002`（WARNING）：普通父过程含耗时子过程时自动向上传导耗时标记；
- `VAL-003`（ERROR）：对象单继承/多继承有向图环路拦截；
- `VAL-004`（ERROR）：复合过程设计视图至少声明一个 `firstStart=true` 入口；
- `VAL-005`（ERROR）：结构控制关系（`edge-control`）DAG 无环校验；
- `VAL-006`（ERROR）：定值（Constant）严禁连接消耗、生成、状态转换或输出影响线；
- `VAL-007`（ERROR）：显式标识符格式与 Java 21 保留字冲突拦截；
- `VAL-008`（ERROR）：同组互斥分支概率和必须落在 $[0.9999, 1.0001]$ 闭区间内；
- `VAL-009`（ERROR）：同一对象与同一过程间同向同类型关系唯一性校验（`edge-fixed` 除外）；
- `VAL-010`（ERROR）：成员链与引用定位路径深度上限 $\le 64$ 层。

---

### 补充章节 D：步进边界无损存档续跑与三级冷会话驱逐机制（补齐 `archive` 体系）

原说明书未展开存档格式与恢复一致性设计，需补充 `groma-base-engine/archive` 核心机制：
1. **四位一体不可变存档包（`SimulationArchiveBundle`，`schemaVersion = 2`）**：
   必须在单写者线程的**步进边界（Step Boundary）**原子导出，包含共享同一 `archiveId` 与 `tickIndex` 的四个分片：
   - `TaskArchivePart`：保存 `modelCode`、`sceneCode`、`finalStepMs`、`rootSeed` 以及**确定性伪随机数游标 `rngDrawCursor`**（保障续跑后随机采样序列无缝衔接）；
   - `InstanceArchivePart`：所有实例存活标记、当前状态与字段键值快照；
   - `RelationLinkArchivePart`：完整保存单向/双向关联正反向实例对（彻底修复旧系统重载丢失关联关系缺陷）；
   - `ProcessArchivePart`：保存活跃耗时过程的 `localSteps`、`elapsedMs`、`subGraphPc` 及无栈协程现场 `runtimeState`。
2. **装载期事件静默保护（`ArchiveCheckpointCoordinator`）**：
   执行 `restoreIntoWorld` 时严格遵循 `enterArchiveLoadingPhase() -> 恢复头/实例/关联/过程 -> finally { exitArchiveLoadingPhaseAndEnableEvents() }`，防止回填属性时误触发运行期 `v/sa/ra` 事件，又确保退出装载态后事件监听 100% 恢复激活。
3. **冷会话自动落盘休眠（`SessionArchiveService`）**：
   当并发会话达到上限时，支持挑选最冷会话执行 `archiveAndEvict` 序列化为 JSON 存档落盘并释放内存，后续随时通过 `/api/v1/studio/archives/{file}/restore` 无损唤醒续跑。

---

### 补充章节 E：外部连接器安全沙盒、网关信任边界与 Redis 键空间规范

#### E.1 四大安全连接器沙盒（`groma-base-connector`）
原说明书仅宽泛提及 JDBC 与 HTTP，源码实际构建了严密的防注入与防逃逸体系：
1. **直连表查询器（`AnyTableQueryConnector`）**：强制管理员预注册数据源，表名/列名经 `^[A-Za-z0-9_]+$` 白名单校验，过滤值 100% 走 `PreparedStatement` 参数绑定，默认开启 `conn.setReadOnly(true)`、查询超时与最大返回行数硬上限。
2. **外部脚本沙盒（`ExternalProgramConnector`）**：仅允许调用预配置解释器（Python/Node/R），强制归一化路径拦截 `../` 目录穿越，每次调用在独立临时目录启动子进程，超时 `destroyForcibly()` 强杀并自动清理临时目录；默认禁用网络脚本源。
3. **表达式沙盒（`ExpressionSandboxEvaluator`）**：正则拦截 `Runtime`、`ProcessBuilder`、`Class.forName`、`Java.type` 等互操作逃逸指令，空值安全返回 `false`。
4. **不可变批次持久化（`ImmutableBatchPersistenceHandler`）**：通过 `List.copyOf` 构造不可变批次移交虚拟线程落库，**严格推迟 Ack 回调至数据库写入成功之后**，超限失败自动转入死信队列（`deadLetterSink`），消除并发篡改与丢数风险。
5. **外部 MCP 客户端白名单（`AllowlistedMcpClientConnector`）**：强制校验 `http/https` 协议与主机域名白名单，封堵云元数据与内网 SSRF 攻击。

#### E.2 网关交接边界、治理护栏与 Redis 键规约
- **内外网信任边界**：`groma-bootstrap` 内网无状态免鉴权运行；公网/跨安全域统一由企业 API 网关鉴权并透传 `X-Request-Id`、`X-Gateway-Subject`、`X-Gateway-Tenant`、`X-Agent-Id`（由 `GatewayHeaderExtractFilter` 提取入上下文）；`TaskIdMdcSanitizingFilter` 对 `X-Session-Id` 执行正则清洗防日志伪造注入。
- **Redis 分布式键空间与强制 TTL**：统一前缀 `groma:{env}:`，包含画布协同互斥锁 `lock:model:{modelCode}`（TTL 60s，前端 `editLockManager.ts` 每 20s 续期）、会话路由表 `session:meta:{sessionId}`（TTL 3600s）、热存档缓存 `archive:snap:{sessionId}:{tick}`（TTL 86400s，Zstd 压缩）及持久化死信队列 `dlq:persist:{date}`（TTL 7 天）。

---

### 补充章节 F：五阶段 CI 自动化质量门禁与静态红线扫描体系（补齐工程保障章）

源码内置了极高水准的质量门禁（`.gitlab-ci.yml` + `tools/regression/scan.py`），强烈建议增补为说明书第九章：
1. **五阶段流水线**：`legacy-zero-diff`（存量目录零修改校验） $\rightarrow$ `build` $\rightarrow$ `unit-test`（含 R1~R14 黄金确定性回归集） $\rightarrow$ `corpus-e2e`（真实语料六步全链路验证） $\rightarrow$ `rules-and-secrets-scan`。
2. **前后端静态红线自动阻断（`scan.py`）**：
   - 后端红线（`NP-R01` ~ `NP-R12`）：严禁 `System.out` / `printStackTrace`、严禁推演热路径使用 `synchronized` 钉死虚拟线程、严禁使用无种子 `Math.random()` / `new Random()`、严禁配置文件硬编码盘符路径/内网 IP/明文密码。
   - 前端红线（`FE-R01` ~ `FE-R13`）：水位线保存追踪（`watermarkSaveTracker.ts` 防旧响应覆盖新编辑）、X6 连线前端守卫（`x6ConnectionGuard.ts`）、XSS 安全渲染（`safeHtml.ts`）。

---

## 第三部分：针对当前源码与架构的深度优化建议（10 项高价值改进）

结合源码走读中发现的潜在隐患与工程断层，提出以下按优先级排序的改进建议：

### 优先级 P0：消除功能断层与严重正确性隐患（建议立即修复）

#### 建议 1：打通 DAP 在线断点调试的“最后一公里”（修复 `DebugEndpoint` 占位桩问题）
- **现状问题**：`GromaWebSocketConfiguration.DebugEndpoint` 在收到前端消息时仅硬编码返回 `"ACK:DEBUG:ATTACHED:" + debugSessionId`，并未把请求转发给已注入的 `DAP_ROUTER`（`DapWebSocketRouter`）；且 `TickEventLoopGroup.freezeSession()` 尚未与 `DapWebSocketRouter` 的断点命中事件联动。
- **改进方案**：
  1. 在 `DebugEndpoint.onOpen` 中解析前端发来的标准 JSON/DAP 请求体（`setBreakpoints`、`continue`、`next`、`stepIn`、`evaluate`、`variables`），反序列化为 `DapProtocolModels` 请求并路由至 `DAP_ROUTER`；
  2. 将 `DAP_ROUTER` 与 `GromaSessionManager.loopGroup()` 绑定，命中断点时调用 `loopGroup.freezeSession(sessionId)` 触发 `ClockFreezeCompensator` 记录冻结起点，恢复时调用 `resumeSession(sessionId)` 平移 `nextDeadlineNano`；
  3. 短期优先打通 **“步进级断点 + 过程入口/出口软断点（基于 `GromaTraceListener`）”**，无需外部 JDWP 子进程即可在单容器内 100% 稳定实现变量检视、条件断点、在线改值与画布三向高亮联动。

#### 建议 2：统一双轨分裂的 `groma-models` 目录，兑现“一键下载 `mvn clean package` 合包运行”承诺
- **现状问题**：仓库根下 `groma/groma-models/` 存放的是手写简化版模型（不含 `GromaModuleRuntime` SPI 注册文件），而流水线生成的完整模型却写在 `groma/groma-bootstrap/groma-workspace/groma-models/` 下，且 `groma-bootstrap/pom.xml` 注释掉了模型依赖。这导致直接执行 `mvn clean package` 产出的 `groma-bootstrap-1.0.0.jar` 内部实际上**没有挂载任何模型**（`ServiceLoader` 扫不到任何内置 `GromaModuleRuntime`）。
- **改进方案**：
  1. 将手写参考样例移至 `src/test` 或独立的 `examples-handwritten/` 目录；
  2. 让 `groma-workspace-sync` 默认同步目标直接指向根工程的 `groma-models/`（或通过 Maven Profile `-Pbundled-models` 自动引入 `groma-models/*` 依赖）；
  3. 确保生成模块自带 `META-INF/services/com.groma.base.engine.GromaModuleRuntime`，使合包 JAR 启动即自动挂载全部选定模型，真正兑现说明书 1.3 节“模式一 & 模式二”的交付能力。

#### 建议 3：修复 `GromaMcpEnginePortImpl` 热挂载模型的类加载器租约与热更新缓存失效 Bug
- **现状问题**：
  1. `GromaMcpEnginePortImpl.resolveRuntime()` 中使用了 `try (SessionModelLease lease = hotSwap.acquireLatestSessionLease(code))`，`try-with-resources` 代码块结束瞬间就调用了 `lease.close()` 将引用计数归零！若此时触发旧版本卸载清理，运行中的类加载器面临被提前关闭回收的风险。
  2. `hotRuntimes.computeIfAbsent(modelCode, ...)` 一旦缓存了首次挂载的 `GromaModuleRuntime`，当用户在 Studio 修改模型再次调用 `/api/v1/studio/models/build` 热更新 v2 版本后，`hotRuntimes` 依然返回旧版 v1 实例，且未同步触发 `ModelToolRegistrar.onModelMountedAsync` 更新 MCP 动态工具！
- **改进方案**：
  1. 引入统一的 `GromaModelRegistry`（说明书 1.1 节曾规划 `bootstrap/registry/`，但目前缺失，导致 `ModelPipelineService.hotMounted`、`GromaMcpEnginePortImpl.hotRuntimes`、`SessionArchiveService.resolveRuntimeOf` 三处各自为政，且 `SessionArchiveService` 甚至只查了 `ServiceLoader`，导致**热挂载模型的落盘存档无法恢复**！）；
  2. 会话启动时为每个 `SessionHandle` 独立申请并持有 `SessionModelLease`，在 `stopSession` 时释放租约；
  3. `ModelPipelineService.buildFromCorpus` 完成编译后，统一通知 `GromaModelRegistry` 刷新最新版本指针并回调 `ModelToolRegistrar` 刷新 MCP 工具集。

---

### 优先级 P1：并发安全、协议规范与性能加固

#### 建议 4：消除 `waitForSessionCondition` 与 `runProcessAndCollect` 对实时事件循环的跨线程竞争
- **现状问题**：在 `GromaMcpEnginePortImpl` 中，`waitForSessionCondition` 和 `runProcessAndCollect` 在 HTTP 请求线程里直接循环调用 `sessions.stepSession(sessionId, 1)` 并直接调用 `ctx.setTraceListener(...)` / `ctx.launch(task)`。如果该会话处于实时运行模式（已绑定在 `TickEventLoopGroup` 线程上），HTTP 线程直接操作 `GromaWorldContext` 违反了 `docs/thread-model.md` 的**“单写者铁律”**，会产生严重的数据竞争！此外，死循环同步推进缺少节拍让步，在高耗时目标下会占满请求线程 CPU。
- **改进方案**：
  - 实时模式下，`waitForSessionCondition` 严禁在外部线程手动调 `stepSession`，而应向 `GromaWorldContext` 注册一个轻量 `CompletableFuture<WaitOutcome>` 条件观察器，由单写者 `TickWorker` 每拍结束时检查 `targetTick` 或 `task.finished()` 并异步 complete，HTTP 虚拟线程仅 `future.get(timeoutMs)` 零 CPU 挂起等待。
  - `runProcessAndCollect` 针对已存在会话调用时，必须通过 `onWriter(...)` 提交启动指令，而非直接跨线程调用 `ctx.launch(task)`。

#### 建议 5：修复 WebSocket 子树查询序列化缺陷与帧序列化重复开销
- **现状问题**：
  1. `GromaWebSocketConfiguration.PortControlGateway.querySubtreeJson` 中直接写了 `return state == null ? "{}" : state.toString();`，Java `Map.toString()` 输出的是 `{a=1, b=2}` 而非合法 JSON `{"a":1, "b":2}`，前端 `JSON.parse` 必然报错！
  2. 下行广播目前推送全量 `WorldSnapshot` JSON，当单场景包含上千个实体且每秒 20~60 拍时，JSON 文本帧体积较大。
- **改进方案**：
  1. 立即将 `state.toString()` 替换为 Jackson `ObjectMapper.writeValueAsString(state)`，并按 `maxDepth` 递归展开子实体树；
  2. 利用实体已有的脏字段追踪机制，在 `TP:v1` 全量关键帧（Keyframe，每 N 拍或新连接首帧发送）基础上，增加 `TD:v1` **差量帧（Delta Frame，仅发送本拍发生状态/属性变化的实例与新增事件）**，可降低 85% 以上的 WebSocket 带宽与前端 Three.js/Vue 响应式重绘开销。

#### 建议 6：升级冷会话淘汰算法与静态状态桥接
- **现状问题**：
  1. `SessionArchiveService.evictColdestIfOverCapacity` 当前使用 `.min(Comparator.comparingLong(SessionSummary::tickIndex))` 挑选驱逐对象——把**刚创建、tickIndex=0 的最新活跃会话**当成“最冷会话”驱逐掉了！这属于逻辑反转 Bug。
  2. `GromaWebSocketConfiguration` 使用 `static volatile ROOMS / DISPATCHER / PORT` 桥接 JSR-356 `Endpoint`，在集成测试多 Spring Context 并行启动时会互相覆盖静态变量。
- **改进方案**：
  1. 在 `SessionSummary` 中暴露 `lastAccessEpochMs`（`GromaSessionManager.lastAccess` 已维护该时间戳），驱逐策略改为按 `lastAccessEpochMs` 最小且当前非 `RUNNING`（或无 WS 订阅者）优先淘汰；
  2. 通过自定义 `ServerEndpointConfig.Configurator` 从 Spring `ApplicationContext` 获取 Bean 实例注入 `Endpoint`，彻底移除 `static volatile` 全局变量。

---

### 优先级 P2：编译器、生成代码与智能化演进建议

#### 建议 7：增强表达式沙盒与中文 AST 转译器的完备性
- **现状问题**：`ExpressionSandboxEvaluator` 目前仅用 `String.indexOf` 支持单个二元比较（如 `a >= 10`），不支持逻辑与或（`&&`、`||`）、括号优先级、算术运算（`a + b > 100`）或字符串相等比较；若 MCP `sim.wait` 未来开放 `conditionExpr` 或连接器动态过滤使用复合条件将无法求值。
- **改进方案**：由于工程已引入 `javaparser-core`（或可采用轻量级无反射 AST 求值器 / 受限 `SimpleEvaluationContext` 的 Spring SpEL），基于白名单 AST 节点（仅允许算术、比较、布尔逻辑与只读 Map 属性读取）实现完整布尔与算术表达式求值，单次解析后缓存 AST 树，兼顾安全性与表达力。

#### 建议 8：完善命名引擎（`groma-naming-engine`）的复合分词与模块归档能力
- **现状问题**：说明书 4.1 节规划了“Jieba/HanLP 分词 + 5万条词库 + Tier 3 AI 异步预译”，目前源码 `DomainGlossaryTokenizer` 主要基于内置 `groma-glossary.properties` 最长前缀匹配 + `pinyin4j` 全拼兜底。遇到词表外的行业复合词会退化为较长的拼音串。
- **改进方案**：
  1. 扩充 `groma-glossary.properties` 为按领域分片的模块化词典（基础仿真词库 + 轨道交通/应急词库 + 无人装备/筹划词库），并开放 Studio 前端词条在线维护接口直接写回 `groma-symbols.lock.json`；
  2. 利用已引入的 `spring-ai-bom` 增加可选的异步命名润色接口（Studio 节点失焦时调用本地/企业大模型生成候选英文标识符供用户一键锁定，离线环境自动无感降级至词典+拼音）。

#### 建议 9：补齐一键源码工程 ZIP 打包导出接口
- **现状问题**：说明书第一章与第八章多次强调支持“一键勾选单个或多个模型模块下载完整可独立 `mvn clean package` 的源码工程 `.zip`”，目前后端已有 `StudioBuildController`（构建）、`ModelUploadController`（上传/远程拉取）、`SessionArchiveController`（会话存档），但缺少**导出独立多模块 Maven 源码包 ZIP 的 REST 接口**。
- **改进方案**：在 `groma-bootstrap` 新增 `GET /api/v1/studio/workspace/export-zip?models=model-a,model-b` 接口，流式打包根 `pom.xml`、`groma-base/`、所选 `groma-models/model-*`（自动裁剪父 POM `<modules>`）及精简版 `groma-bootstrap/`（剔除 `groma-studio` 编译期依赖），让客户现场交付真正做到一键下载、开箱即跑。

#### 建议 10：修正说明书技术栈版本表述的严谨性
- **现状问题**：说明书与 `pom.xml` 标注 `Spring Boot 4.1.0`、`Spring AI 2.0.0` 及 `tools.jackson.databind.ObjectMapper`（Jackson 3.x 包名）。在对外交付或信创招投标评审中，评审专家往往会核查组件在主流信创 Maven 私服（如华为云/阿里镜像或内网 Nexus）的可获取性与正式 GA 状态。
- **改进方案**：在说明书“技术栈选型与部署环境说明”中补充组件兼容性矩阵与离线 Maven 仓库打包方案（提供 `maven-dependency-plugin:go-offline` 离线依赖包制作说明，并修正 `Dockerfile` 支持挂载企业内网 Nexus `settings.xml` 镜像构建），消除内网隔离环境交付障碍。

---

## 第四部分：建议重排后的《需求与技术架构设计说明书（V13.0 修订版）》标准目录大纲

为彻底解决原 V12.0 文档章节跳号与内容缺失问题，建议按以下十章结构重排定稿：

1. **第一章 业务需求全景、应用场景与非功能性指标（SLO）基线** *(新增补全)*
2. **第二章 总体多模块工程架构设计（底座五模块托举 + 多模型子模块分治 + 四重零冲突隔离）** *(修正 5 底座 + 8 Studio 模块)*
3. **第三章 公共底座模块组（`groma-base` 五大模块）详细设计** *(补齐引擎存档体系、四大安全连接器、MCP 18 工具与 7 资源)*
4. **第四章 建模导入、29连线/21事件解析校验与五层代码生成套件（`groma-studio`）详细设计** *(补齐 `groma-model-import` 与 `VAL-001~010`)*
5. **第五章 模型子模块（`model-{code}`）内部五层映射与深层运行机理** *(明确定义视图 vs 设计视图、三级启动/收尾链)*
6. **第六章 确定性中英命名转译、阿里巴巴 Java 编码规约与大模型三级双语语义体系** *(对齐 `groma-naming-engine` 与 `GROMA_MODEL_INDEX.md`)*
7. **第七章 全量 29 类关系 + 21 类事件向 Java 21 五层代码的转译规则全集** *(修复原第五章标题与 `7.1/7.2/5.3` 编号错乱)*
8. **第八章 可视化源码级断点调试架构设计（DAP 协议 + 时统冻结补偿 + 三向高亮联动）** *(修复 `$ ightarrow$` 乱码并对齐软/硬双模断点)*
9. **第九章 高并发容量保障、十大深水区陷阱防护、网关安全边界与 Redis 规约** *(整合 `thread-model.md`、`gateway-handoff.md`、`redis-keys-ttl.md`)*
10. **第十章 前端一体化工作台、五阶段 CI 黄金回归门禁与分阶段演进路线图** *(整合 `groma-studio-web`、`scan.py` 与 `tests.md`)*
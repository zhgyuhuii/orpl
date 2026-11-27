# Groma 仿真与智能体 MCP 服务网关交接清单

## 1. 信任边界与总体原则（对应决定 D-01、D-03）
- **服务内部无认证**：`groma-bootstrap` 运行在受控内网，全部 REST、WebSocket 与 `/mcp` 接口均不校验口令或登录令牌，内网直连直接可达。
- **对外暴露统一由网关接管**：公网或跨安全域调用必须经过企业微服务网关（Spring Cloud Gateway / Higress / Kong / Envoy），由网关执行身份认证、路由亲和、流量配额与高危写接口拦截。

## 2. MCP 协议端点交接要求（NP-MCP-010 六要素）

| 交接项 | 规格与配置要求 |
|---|---|
| **1. 端点路径与传输协议** | `POST /mcp`（及同路径下的流式通知通道），采用 **MCP Streamable HTTP** |
| **2. 支持的 MCP 协议版本** | `2025-06-18`（跟随 Spring AI MCP Server Starter 协商） |
| **3. 有状态 vs 无状态取舍与会话亲和** | **选定方案：多实例网关要求基于推演 `sessionId` 或 `Mcp-Session-Id` 做一致性哈希会话亲和（Session Affinity）**。理由：1) 无状态计算（`run.process` 不带 `sessionId`、只读资源 `groma://`）本身是完全无状态的，落到任意节点均可执行；2) 但有状态的交互式推演会话（`sim.start` 创建的 `*WorldContext` 内存沙盒）驻留在单个 JVM 实例的 `TickEventLoop` 内存中，后续 `sim.step` / `sim.query_instances` / `sim.stop` 必须由网关按请求头 `Mcp-Session-Id`（或按模型分片独立部署实例）路由至同一实例。 |
| **4. 网关透传请求头（预留钩子）** | 请网关在完成外部认证后向下游透传：`X-Request-Id`（全链路追踪号）、`X-Gateway-Subject`（调用人标识）、`X-Gateway-Tenant`（租户标识）、`X-Agent-Id`（智能体标识）。`GatewayHeaderExtractFilter` 已提取存入 `GatewayIdentityContext`。 |
| **5. 超时与缓冲配置建议** | 网关到 `/mcp` 的上游响应读取超时 `read-timeout` **不得低于 `95s`**（服务端 `request-timeout=90s`，无状态耗时过程墙钟上限 `60s`）；关闭网关侧响应缓冲（`proxy_buffering off`）以保障 `sim.wait` 进度流实时送达。 |
| **6. 限流建议与单实例配额** | 单实例默认最大并发推演会话配额为 `200`（超限返回业务码 `LIMIT_EXCEEDED`）；建议网关对单个 `X-Agent-Id` 限制 `sim.start` 速率不超过 `5 QPS`，只读观测接口不超过 `50 QPS`。 |

## 3. 全量接口读写级别与影响范围矩阵（NP-HOST-002 & BE-SEC-001）

| 通道 | 路径 / 工具名 | 读写级别 | 影响范围 | 网关管控建议 |
|---|---|---|---|---|
| **REST** | `POST /api/v1/models/{model}/scenes/{scene}/sessions` | **写级** | 单个模型（新建会话） | 需已登录用户或授权服务，限制创建速率 |
| **REST** | `POST /api/v1/models/{model}/scenes/{scene}/sessions/{sid}/step` | **写级** | 单个会话 | 需会话归属方或演训操作员 |
| **REST** | `GET /api/v1/models/{model}/scenes/{scene}/sessions/{sid}/snapshot` | **只读** | 单个会话 | 普通只读放行 |
| **REST** | `DELETE /api/v1/models/{model}/scenes/{scene}/sessions/{sid}` | **写级（破坏性）** | 单个会话 | 需会话创建者或管理员 |
| **WS** | `/ws/groma/{model}/{scene}/{sid}`（下行态势帧 / 查询指令） | **只读** | 单个会话 | 校验 `Origin` 白名单，限制单 IP 并发连接数 |
| **WS** | `/ws/groma/{model}/{scene}/{sid}`（上行控制帧：暂停/倍速/改值） | **写级** | 单个会话 | 仅允许具备演训控制权的角色开启写指令 |
| **MCP** | `resources/read` (`groma://models/**`)、`model.describe_process` | **只读** | 单个模型元数据 | 对所有已注册智能体开放 |
| **MCP** | `sim.list_sessions`、`sim.describe_session`、`sim.query_instances`、`sim.get_instance`、`sim.get_processes`、`sim.get_events`、`sim.wait` | **只读** | 单个会话快照 | 对已授权观察型智能体开放 |
| **MCP** | `run.process`（不传 `sessionId` 无状态调用）、`<model>__run_<ref>` | **计算级（无残留状态）** | 临时沙盒（跑完即焚） | 按智能体计算配额限流 |
| **MCP** | `sim.start`、`sim.pause`、`sim.resume`、`sim.step`、`sim.set_speed`、`sim.set_value`、`sim.set_state`、`sim.run_process` | **写级** | 单个推演会话 | 仅对干预型/推演推控智能体授权 |
| **MCP** | `sim.stop` | **写级（破坏性）** | 单个推演会话销毁 | 严格绑定会话所属智能体或管理员 |

## 4. 统一暴露端点速查
- REST 接口前缀：`/api/v1/models/{modelCode}/scenes/{sceneCode}/**`
- WebSocket 态势总线：`/ws/groma/{modelCode}/{sceneCode}/{sessionId}`（需开启 `Upgrade: websocket` 透传，空闲超时设为 120s）
- MCP Streamable HTTP 端点：`POST /mcp`（需关闭响应缓冲 `proxy_buffering off`，超时设为 90s）
- 下游 `TaskIdMdcSanitizingFilter` 自动对 `X-Session-Id` 等标识符执行 `^[A-Za-z0-9_-]+$` 白名单消毒，防日志注入。
- 只读观测类（`/snapshot`、`groma://` 资源、`HRT:`、`T/I/TREE:`）限流阈值建议 500 QPS/租户；状态写操作限流阈值建议 50 QPS/租户，请求体上限 10MB。

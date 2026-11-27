# WebSocket 实时态势与控制信令协议规范（v1）

- **握手端点**：`/ws/groma/{modelCode}/{sceneCode}/{sessionId}`，单会话默认最多允许 50 个并发订阅客户端。
- **下行帧格式**：`TP:<JSON>`，携带 `ver="v1"`、`sessionId`、`tick`、`worldTimeMs`、`speed`、`status`、`instances`、`processes`。
- **零观察者静默开关**：当会话房间 `subscriberCount == 0` 时，引擎跳过帧序列化，后台静默推演序列化开销为 0。
- **慢客户端单槽背压合并（`ConflationWsSender`）**：每个客户端连接维护容量为 1 的 `AtomicReference<String>` 最新帧槽位；当客户端网络拥塞未完成上一帧异步发送时，新产生的步进帧自动覆盖旧槽位，杜绝发送队列堆积引发 OOM。
- **上行控制指令集**：`HRT:<ts>`（心跳）、`CTL:P`（暂停）、`CTL:R`（恢复）、`CTL:S`（终止）、`CTL:G:<steps>`（单步推进 N 步）、`SP:D` / `SP:N` / `SP:H`（倍速调节）、`T/I/TREE:<rootId>:<maxDepth>`（子树查询，最大深度上限 16）。畸形报文一律返回 `ERR:<CMD>:<REASON>`。

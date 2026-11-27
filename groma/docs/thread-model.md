# 单写者无锁推演线程模型与跨通道并发边界说明

1. **计算核固定绑定（`TickEventLoopGroup`）**：全进程启动等于物理 CPU 核数（默认 N_cores）的常驻平台线程 `groma-tick-loop-{0..N-1}`。任意会话 `sessionId` 通过一致性取模固定绑定至唯一一条工作线程，会话内 `InstanceRegistry`、实体属性与过程树 100% 单线程无锁读写，从根源消除 `ConcurrentModificationException` 与数据竞争。
2. **多生产者单消费者命令收件箱（`MpscRequestBridge`）**：来自 REST `Controller`、WebSocket 控制帧、MCP 工具调用的所有写/干预操作一律封装为不可变 `Command` 投递至 `ConcurrentLinkedQueue`，在下一步边界阶段 0 顺序排空执行。
3. **跨通道只读快照（`WorldSnapshot`）**：每步阶段 3 末尾构造不可变 `WorldSnapshot` 并通过 `volatile` 原子发布引用；REST 快照接口、WebSocket 广播器与 MCP 观测工具读取同一份快照，全程零锁且绝不发生半步撕裂。
4. **外部慢 I/O 隔离（`groma-base-connector`）**：数据库直连、HTTP/MCP 客户端、外部脚本与批量持久化全部运行在 JDK 21 虚拟线程执行器上，严禁使用阻塞载体线程的原语，确保慢外部 I/O 绝不卡死同核其余推演会话。

# Redis 键空间命名规约与过期回收策略

所有键统一采用前缀 `groma:{env}:`，严禁出现无 `TTL` 的动态会话键：

| 键模板 | 数据结构 | 默认 TTL | 用途与续期策略 |
|---|---|---|---|
| `groma:{env}:lock:model:{modelCode}` | String (LeaseToken) | 60s | Studio 画布协同编辑互斥锁，前端每 20s 续期，失联自动释放 |
| `groma:{env}:session:meta:{sessionId}` | Hash | 3600s | 活跃推演会话路由元数据，随客户端 `HRT:` 心跳刷新 TTL |
| `groma:{env}:archive:snap:{sessionId}:{tick}` | Bytes (Zstd) | 86400s | 步进边界热存档缓存，超期转储对象存储或自动淘汰 |
| `groma:{env}:dlq:persist:{date}` | List | 604800s | 批量落库重试超限后的死信暂存队列（保留 7 天） |

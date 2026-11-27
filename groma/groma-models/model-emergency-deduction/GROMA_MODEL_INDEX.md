# GROMA 模型全局语义导航地图：应急决策推演-关联 (v825)

- **源模型 ID**：`gIh2MQtjW2me1gm9GZi` | **模块编码**：`emergency-deduction`
- **权威运行场景**：`三站两区间积水与列车运行` (`/api/v1/models/emergency-deduction/scenes/three-station-flood-train`)

## 中英文五层符号对照表
| 方圆中文原名 | 建模类型 | 生成五层 Java 类/方法 | 关键物理量纲与因果规则 |
|---|---|---|---|
| 区域（金水区） | TYPE | `AreaEntity` | 汇水面积(m2)、径流系数、积水深度(m) |
| 车站（海滩寺/沙口路/月季公园） | TYPE | `StationEntity` | 挡水墙顶高程(m)，区域水位超顶 0.15m 触发挡水墙 `FAILED` |
| 地铁区间（海沙/沙月区间） | TYPE | `MetroSectionEntity` | 代表性水深 >= 0.15m 自动置 `releaseAllowed=false` |
| 列车（0501次） | TYPE | `TrainEntity` | 区间禁止放行时触发维持条件中断，立即转入 `FORCED_STOP`（迫停） |
| 确定连接处过水情况 | INDEPENDENT-PROCESS | `HydrologyAndTrainOperatorServiceImpl#computeConnectionFlowM3PerSec` | 宽顶堰流计算站-区双向漫流 |

# 静态模型校验规则与《方圆语言标准-1117》条款对照表

| 规则编号 | 严重度 | 标准条款依据 | 拦截条件描述 | 对应单测 |
|---|---|---|---|---|
| `VAL-001` | ERROR | 标准 9.2.2 / 12.1.2 | 非耗时算子（`timeConsuming=false`）内部禁止包含耗时子过程或延迟等待逻辑 | `ValidationRulesAndMappingTest` |
| `VAL-002` | WARNING | 标准 12.1.2 | 普通父过程包含耗时子过程时自动向上推断或提示显式开启耗时标记（规避 `BE-17`） | `ValidationRulesAndMappingTest` |
| `VAL-003` | ERROR | 标准 8.5 | 对象类型单继承与多继承有向图不得存在闭环（防 `BL-15` 栈溢出） | `ValidationRulesAndMappingTest` |
| `VAL-004` | ERROR | 标准 9.4.7 | 含子过程设计视图的复合过程必须至少声明一个 `firstStart=true` 入口子过程 | `ValidationRulesAndMappingTest` |
| `VAL-005` | ERROR | 平台扩展 R06 | 结构控制关系（`edge-control`）必须构成有向无环图（DAG） | `ValidationRulesAndMappingTest` |
| `VAL-006` | ERROR | 标准 7.3 | 定值（Constant）严禁连接消耗、生成、状态转换或单/双向输出影响连线 | `ValidationRulesVal006to010Test` |
| `VAL-007` | ERROR | 标准 8.2 | 标识符编码若由用户显式填写，不得违反 `^[A-Za-z_][A-Za-z0-9_]*$` 或命中 Java 21 保留字（实现于 `UniqueIdentifierGuard`，`StaticModelValidator` 接线） | `ValidationRulesVal006to010Test` |
| `VAL-008` | ERROR | 标准 10.4 | 同组互斥分支概率之和必须落在 [0.9999, 1.0001] 闭区间内（单成员命名组降级 WARNING） | `ValidationRulesVal006to010Test` |
| `VAL-009` | WARNING | 标准 10.1.3 | 同一对象与同一过程之间同向同类型关系最多允许存在一条（`edge-fixed` 除外）；重复线由解析器按 NextIR 去重、语义无损（2026-11-27 模型 2 真机实证降级，原 ERROR 阻断属过度拦截） | `ValidationRulesAndMappingTest` |
| `VAL-010` | ERROR | 标准 9.6 | 成员链与引用定位路径深度不得超过最大递归上限 64 层 | `ValidationRulesVal006to010Test` |

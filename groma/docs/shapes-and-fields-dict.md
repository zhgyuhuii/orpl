# 29 种画布连线形状、21 种事件代码与关键字段权威字典

## 1. 29 种画布连线形状全映射表
- 结构关系（10 种）：`edge-extend` (R01)、`edge-composition` (R02)、`edge-exhibition` (R03)、`edge-classification` (R04)、`edge-proxy` (R05)、`edge-control` (R06 平台扩展)、`edge-unidirectional-association` (R07)、`edge-bidirectional-association` (R08)、`edge-unidirectional-tagged` (R09)、`edge-bidirectional-tagged` (R10)。
- 交互与锚点关系（19 种）：`edge-consumption` (R11)、`edge-result` (R12)、`edge-in-change` (R13)、`edge-out-change` (R14)、`edge-in-out` (R15 归一化为 R13+R14)、`edge-in-influence-nullable` (R16)、`edge-in-influence-nonnull` (R17)、`edge-out-influence` (R18)、`edge-in-out-influence-nullable` (R19)、`edge-in-out-influence-nonnull` (R20)、`edge-agent` (R21)、`edge-condition` (R22)、`edge-invocation` (R23)、`edge-invocation-self` (R24)、`edge-overtime-exception` (R25)、`edge-undertime-exception` (R26)、`edge-note` (R27)、`edge-fixed` (R28)、`edge-link` (R00)。

## 2. 21 种标准事件代码表
- 生命周期与状态（7 种）：`a`（出现）、`as`（指定状态出现）、`sa`（进入状态）、`v`（属性变化）、`x`（消亡）、`xs`（指定状态消亡）、`sx`（离开状态）。
- 集合代理（4 种）：`ma`（集合增成员）、`am`（加入集合）、`mx`（集合减成员）、`xm`（移出集合）。
- 引用指向（6 种）：`ra`（建立指向）、`ar`（被建立指向）、`rv`（变更指向）、`vr`（被变更指向）、`rx`（解除指向）、`xr`（被解除指向）。
- 过程异常与收尾（4 种）：`pca`（提前完成）、`pcd`（超时完成）、`psf`（启动失败）、`paa`（中断取消）。

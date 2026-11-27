package com.groma.studio.ir;

import java.util.List;
import java.util.Map;

/**
 * 方圆模型编译期强类型中间表示（落实 NP-PRS-003，不可变、可序列化、稳定摘要）
 * <p>
 * 前 12 个分量是通用模型概览（定值、枚举、类型、独立过程、场景、视图画布）；
 * 第 13 个分量 {@link CompilePlanIR} 是真实语料的完整编译计划——五层源码生成器主要消费它。
 * 仅用 12 参构造器创建的 IR（例如手写的单测夹具）compilePlan 为空，生成器会按通用概览降级处理。
 * </p>
 */
public record GromaModuleIR(
        String formatVersion,
        String modelId,
        String modelCode,
        String modelLabel,
        int modelVersion,
        String sourceDigest,
        long finalStepMs,
        List<ConstantIR> constants,
        List<EnumIR> enums,
        List<TypeIR> domainTypes,
        List<ProcessIR> independentProcesses,
        List<SceneIR> scenes,
        Map<String, ViewGraphIR> views,
        CompilePlanIR compilePlan
) {
    public GromaModuleIR(
            String formatVersion,
            String modelId,
            String modelCode,
            String modelLabel,
            int modelVersion,
            String sourceDigest,
            long finalStepMs,
            List<ConstantIR> constants,
            List<EnumIR> enums,
            List<TypeIR> domainTypes,
            List<ProcessIR> independentProcesses,
            List<SceneIR> scenes,
            Map<String, ViewGraphIR> views
    ) {
        this(formatVersion, modelId, modelCode, modelLabel, modelVersion, sourceDigest, finalStepMs,
                constants, enums, domainTypes, independentProcesses, scenes, views, CompilePlanIR.EMPTY);
    }

    /** 带编译计划的便捷构造 */
    public GromaModuleIR withCompilePlan(CompilePlanIR plan) {
        return new GromaModuleIR(formatVersion, modelId, modelCode, modelLabel, modelVersion, sourceDigest, finalStepMs,
                constants, enums, domainTypes, independentProcesses, scenes, views, plan);
    }
    public record ConstantIR(
            String entityId,
            String zhName,
            String fieldType,
            String defaultValue,
            String unit
    ) {}

    public record EnumIR(
            String entityId,
            String zhName,
            List<String> stateLabels,
            String initialState
    ) {}

    public record TypeIR(
            String entityId,
            String zhName,
            String description,
            String essence,
            String primaryParentEntityId,
            List<String> mixinParentEntityIds,
            List<FieldIR> fields,
            List<ProcessIR> methods
    ) {}

    public record FieldIR(
            String entityId,
            String zhName,
            String fieldType,         // "double", "long", "boolean", "String", 或自定义 Type 的 entityId / Enum 的 entityId
            String typeCategory,      // "BASE", "ENUM", "DEFINE"
            boolean collection,
            boolean readOnly,
            String unit,
            String minValue,
            String maxValue,
            String defaultValue,
            List<TriggerBindingIR> eventTriggers
    ) {}

    public record TriggerBindingIR(
            String linkEntityId,
            String eventCode,         // "a", "v", "x", "sa", "sx", "ma", "mx" 等 15 类事件码
            String conditionExpr,     // 线上逻辑判断表达式，如 "waterDepth >= 0.5"
            double probability,       // 0.0 ~ 1.0
            String probabilityGroup,
            String targetProcessEntityId,
            boolean targetTimeConsuming
    ) {}

    public record ProcessIR(
            String entityId,
            String ownerTypeEntityId, // 若为独立过程或场景直属过程则为空串
            String zhName,
            String description,
            boolean timeConsuming,    // true = 耗时过程 (*ProcessTask), false = 瞬时算子 (*ServiceImpl 方法)
            boolean twinLocked,       // R2: fromRealWorld == true 时运行期锁 1x 倍速
            boolean asyncMode,        // R4: 异步外部等待过程
            String singletonMode,     // "NONE", "OBJECT_SINGLETON", "GLOBAL_SINGLETON" (标准 9.4.5)
            boolean selfInvoking,     // R24: 自调用标记
            String durationParamType, // "FIXED", "STEP", "UNIFORM", "NORMAL", "EXPONENTIAL"
            double timeNormal,
            Double timeMin,
            Double timeMax,
            String timeUnit,
            Long executionPeriodTicks,// P = max(1, ceil(accuracyMs / finalStepMs))
            String rawJavaCode,       // 画布手写代码（可能含中文变量，甚至在脏语料中含 Python 脚本）
            List<ParamPortIR> inputs,
            List<ParamPortIR> outputs,
            List<FieldIR> localTempMembers, // TEMPLATE-MEMBER 局部临时对象 -> 生成进 RuntimeBO
            List<SubProcessStepIR> subSteps,// 设计视图内子过程编排链
            List<ConditionRuleIR> startConditions,    // R22 启动条件
            List<ConditionRuleIR> maintainConditions, // R21 维持条件 (agent)
            List<PostActionIR> postActions,           // R11 消耗 / R12 生成 / R13-R14 状态转换
            boolean publishedForMcp,  // 是否属于场景层或独立过程且输入绑定完整（NP-MCP-002）
            String defineViewId,
            String designViewId
    ) {}

    public record ParamPortIR(
            String portId,
            String relevanceEntityId,
            String zhName,
            String fieldType,
            String typeCategory,
            boolean required,         // in-influence-nonnull -> true; nullable -> false
            boolean readOnlyInput,    // R16/R17 -> true (使用 ReadOnly 接口契约); R19/R20 -> false (传入可变 Entity)
            String unit,
            Double minVal,
            Double maxVal,
            String defaultVal
    ) {}

    public record SubProcessStepIR(
            int stepPc,
            String cellId,
            String processEntityId,
            String zhName,
            boolean timeConsuming,
            boolean firstStart,       // 首个启动入口 (标准 9.4.7)
            boolean lastEnd,          // 结束点：完成时立即取消其余活跃子过程并触发父过程收尾 (CK-BE-PRC-004-1)
            List<Integer> nextStepPcs,
            Integer failBranchPc      // psf 启动失败跳转分支
    ) {}

    public record ConditionRuleIR(
            String linkEntityId,
            String targetParamOrFieldName,
            String operator,
            String thresholdLiteral,
            String naturalDesc
    ) {}

    public record PostActionIR(
            String actionKind,        // "CONSUME" (R11), "RESULT" (R12), "OUT_CHANGE" (R14)
            String targetEntityOrField,
            String targetStateEnumItem,
            double probability,
            String probabilityGroup
    ) {}

    public record SceneIR(
            String treeNodeId,
            String entityId,
            String zhName,
            String defineViewId,
            String designViewId,
            List<SceneMemberIR> initMembers,
            List<String> defaultStartOperatorIds,
            List<String> defaultStartProcessIds,
            List<ProcessIR> sceneLevelProcesses
    ) {}

    public record SceneMemberIR(
            String memberEntityId,
            String zhName,
            String typeEntityId,
            Map<String, String> initialFieldOverrides
    ) {}

    public record ViewGraphIR(
            String viewId,
            boolean defineView,
            List<NodeCellIR> nodes,
            List<EdgeCellIR> edges
    ) {}

    public record NodeCellIR(
            String cellId,
            String entityId,
            String cellType,
            String shape,
            String label,
            boolean defaultStart,
            boolean firstStart,
            boolean lastEnd,
            double x,
            double y
    ) {}

    public record EdgeCellIR(
            String cellId,
            String linkEntityId,
            String relationType,
            String sourceCellId,
            String targetCellId,
            String eventCode
    ) {}
}

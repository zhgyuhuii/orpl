package com.groma.studio.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真实语料编译计划：把 XX-a.json + XX-b.json 的组织树、定义视图、设计视图归一化成生成器可直接消费的强类型结构
 * <p>
 * {@link GromaModuleIR} 原有的 12 个分量保留（旧接口与手写 IR 的单测继续可用）；
 * 本类作为第 13 个分量承载：对象类型与成员表、过程定义及其数据流节点表、事件接线与默认启动、命名实例对象图、关联类型。
 * 所有列表都已按稳定键排序，保证同一份语料多次解析逐字段相等。
 * </p>
 *
 * @param configuredStepMs 场景未指定步长时的默认步长（毫秒）
 * @param types            全部领域对象类型（含场景、关联类型角色对象）
 * @param processDefs      全部过程定义（以设计视图为键去重）
 * @param instances        命名实例（对象图）
 * @param associationTypes 关联类型（R07/R08 双向带标签关联）
 * @param triggers         事件接线（成员/状态 --事件码--> 过程）
 * @param defaultStarts    默认启动
 * @param warnings         解析期诊断
 */
    public record CompilePlanIR(
            long configuredStepMs,
            List<TypeDefIR> types,
            List<ProcessDefIR> processDefs,
            List<InstanceIR> instances,
            List<AssocTypeIR> associationTypes,
            List<TriggerIR> triggers,
            List<DefaultStartIR> defaultStarts,
            List<ManualStartIR> manualStarts,
            List<String> warnings
    ) {
        public static final CompilePlanIR EMPTY = new CompilePlanIR(
                1000L, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    /**
     * 对象类型定义
     *
     * @param kind           TYPE / SCENE / ASSOCIATION
     * @param explicitEncode 建模面板显式填写的编码（encode 字段；平台自动码 dxlx1/gccy1 等由命名引擎 Tier 1 排除）
     * @param controls       R06 结构控制规则（edge-control：源枚举状态 → 目标成员/过程使能位）
     */
    public record TypeDefIR(
            String entityId,
            String zhName,
            String description,
            String essence,
            String kind,
            String primaryParentId,
            List<String> mixinParentIds,
            List<ProxyDefIR> proxies,
            List<MemberIR> members,
            List<String> notes,
            String explicitEncode,
            List<ControlRuleIR> controls
    ) {
        public TypeDefIR(String entityId, String zhName, String description, String essence,
                         String kind, String primaryParentId, List<MemberIR> members) {
            this(entityId, zhName, description, essence, kind, primaryParentId,
                    List.of(), List.of(), members, List.of(), "", List.of());
        }

        /** 旧 10 参兼容构造器（encode 为空、无 R06 控制规则） */
        public TypeDefIR(String entityId, String zhName, String description, String essence,
                         String kind, String primaryParentId, List<String> mixinParentIds,
                         List<ProxyDefIR> proxies, List<MemberIR> members, List<String> notes) {
            this(entityId, zhName, description, essence, kind, primaryParentId,
                    mixinParentIds, proxies, members, notes, "", List.of());
        }
    }

    /**
     * R06 结构控制规则（edge-control）：源枚举成员处于 activeStateLabels 之一时，
     * 目标成员（targetKind=MEMBER）或目标过程（targetKind=PROCESS）才处于使能态。
     * bitIndex 为目标实体上的使能位序号（成员位与过程位各自从 0 计）。
     */
    public record ControlRuleIR(
            String sourceEnumMemberId,
            List<String> activeStateLabels,
            String targetKind,
            String targetEntityId,
            int bitIndex
    ) {}

    /** R05 集合成员代理定义（模型 2 智能筹划含 1 组）：集合 collectionMemberId 的元素事件代理转发至下游 */
    public record ProxyDefIR(
            String proxyEntityId,
            String zhName,
            String collectionMemberId,
            String elementTypeId
    ) {}

    /** R25 超时（pcd）/ R26 提前完成（pca）异常控制流边；eventKind 取 OVERTIME_PCD / UNDERTIME_PCA。
     *  sourcePc = 被监控的耗时子过程节点（设计视图中连线源端），-1 表示连线挂在过程整体（过程级监控）。 */
    public record ExceptionFlowIR(
            String eventKind,
            int sourcePc,
            int targetPc,
            String targetDefKey
    ) {
        public ExceptionFlowIR(String eventKind, int targetPc, String targetDefKey) {
            this(eventKind, -1, targetPc, targetDefKey);
        }
    }

    /**
     * 类型成员
     *
     * @param typeRange      BASE / ENUM / DEFINE
     * @param refTypeId      ENUM 的枚举 id 或 DEFINE 的对象类型 id
     * @param containerType  array / set / map（集合成员）
     * @param initialState   枚举成员的初始状态中文原名
     * @param javaType       Java 类型占位（见 GromaTypes）
     * @param explicitEncode 建模面板显式填写的编码（命名 Tier 1 输入；平台自动码由命名引擎排除规则处理）
     */
    public record MemberIR(
            String entityId,
            String zhName,
            String alias,
            String fieldType,
            String typeRange,
            String refTypeId,
            boolean collection,
            String containerType,
            String defaultValue,
            String unit,
            String minValue,
            String maxValue,
            String initialState,
            String description,
            String javaType,
            String explicitEncode
    ) {
        /** 旧 15 参兼容构造器（encode 为空） */
        public MemberIR(String entityId, String zhName, String alias, String fieldType, String typeRange,
                        String refTypeId, boolean collection, String containerType, String defaultValue,
                        String unit, String minValue, String maxValue, String initialState,
                        String description, String javaType) {
            this(entityId, zhName, alias, fieldType, typeRange, refTypeId, collection, containerType,
                    defaultValue, unit, minValue, maxValue, initialState, description, javaType, "");
        }
    }

    /**
     * 关联类型（画布“关联类型”目录下的双向带标签关联）
     */
    public record AssocTypeIR(
            String entityId,
            String zhName,
            String leftMemberId,
            String leftZhName,
            String leftTypeId,
            String rightMemberId,
            String rightZhName,
            String rightTypeId,
            String constraintLeft,
            String constraintRight,
            String labelLeftToRight,
            String labelRightToLeft
    ) {}

    /** 命名实例 */
    public record InstanceIR(String instanceId, String zhName, String typeId, ValueSpec spec) {}

    /** 对象图取值 */
    public sealed interface ValueSpec permits ScalarSpec, RefSpec, ObjectSpec, ListSpec, MapSpec, NullSpec {}

    /** 基础类型字面量（rawValue 是画布原始文本，fieldType 是画布字段类型） */
    public record ScalarSpec(String rawValue, String fieldType) implements ValueSpec {}

    /** 指向另一个命名实例 */
    public record RefSpec(String instanceId, String typeId) implements ValueSpec {}

    /** 内联对象 */
    public record ObjectSpec(String typeId, Map<String, ValueSpec> members) implements ValueSpec {}

    /** 有序集合 */
    public record ListSpec(List<ValueSpec> items) implements ValueSpec {}

    /** 键值集合 */
    public record MapSpec(Map<String, ValueSpec> entries) implements ValueSpec {}

    /** 未赋值 */
    public record NullSpec() implements ValueSpec {
        public static final NullSpec INSTANCE = new NullSpec();
    }

    /**
     * 过程定义
     *
     * @param ownerKind TYPE / SCENE / INDEPENDENT
     * @param explicitEncode 建模面板显式填写的编码（命名 Tier 1 输入；平台自动码由命名引擎排除规则处理）
     */
    public record ProcessDefIR(
            String defKey,
            String entityId,
            String zhName,
            String description,
            String ownerKind,
            String ownerTypeId,
            String ownerZhName,
            boolean timeConsuming,
            boolean twinLocked,
            boolean asyncMode,
            int singletonMode,
            String durationParamType,
            double timeNormal,
            String timeUnit,
            Double timeMin,
            Double timeMax,
            String distribution,
            Double distMin,
            Double distMax,
            Double distAvg,
            Double distSd,
            Double distParam,
            Long accuracyMs,
            String javaCode,
            String designViewId,
            List<PortIR> params,
            List<LocalVarIR> locals,
            List<NodeIR> nodes,
            List<Integer> entryNodes,
            List<ConditionRuleIR> startConditions,
            List<ConditionRuleIR> maintainConditions,
            List<ExceptionFlowIR> exceptionFlows,
            String explicitEncode
    ) {
        /** 旧 32 参兼容构造器（encode 为空） */
        public ProcessDefIR(String defKey, String entityId, String zhName, String description,
                            String ownerKind, String ownerTypeId, String ownerZhName,
                            boolean timeConsuming, boolean twinLocked, boolean asyncMode, int singletonMode,
                            String durationParamType, double timeNormal, String timeUnit,
                            Double timeMin, Double timeMax, String distribution, Double distMin, Double distMax,
                            Double distAvg, Double distSd, Double distParam, Long accuracyMs,
                            String javaCode, String designViewId,
                            List<PortIR> params, List<LocalVarIR> locals, List<NodeIR> nodes, List<Integer> entryNodes,
                            List<ConditionRuleIR> startConditions, List<ConditionRuleIR> maintainConditions,
                            List<ExceptionFlowIR> exceptionFlows) {
            this(defKey, entityId, zhName, description, ownerKind, ownerTypeId, ownerZhName,
                    timeConsuming, twinLocked, asyncMode, singletonMode, durationParamType, timeNormal, timeUnit,
                    timeMin, timeMax, distribution, distMin, distMax, distAvg, distSd, distParam, accuracyMs,
                    javaCode, designViewId, params, locals, nodes, entryNodes,
                    startConditions, maintainConditions, exceptionFlows, "");
        }

        public ProcessDefIR(String defKey, String entityId, String zhName, String description,
                            String ownerKind, String ownerTypeId, String ownerZhName,
                            boolean timeConsuming, boolean twinLocked, boolean asyncMode, int singletonMode,
                            String durationParamType, double timeNormal, String timeUnit,
                            Double timeMin, Double timeMax, String distribution, Double distMin, Double distMax,
                            Double distAvg, Double distSd, Double distParam, Long accuracyMs,
                            String javaCode, String designViewId,
                            List<PortIR> params, List<LocalVarIR> locals, List<NodeIR> nodes, List<Integer> entryNodes,
                            List<ConditionRuleIR> startConditions, List<ConditionRuleIR> maintainConditions) {
            this(defKey, entityId, zhName, description, ownerKind, ownerTypeId, ownerZhName,
                    timeConsuming, twinLocked, asyncMode, singletonMode, durationParamType, timeNormal, timeUnit,
                    timeMin, timeMax, distribution, distMin, distMax, distAvg, distSd, distParam, accuracyMs,
                    javaCode, designViewId, params, locals, nodes, entryNodes,
                    startConditions, maintainConditions, List.of());
        }
    }

    /**
     * 过程形参（定义视图里的关联对象）
     *
     * @param direction IN / OUT / IN_OUT
     * @param nonnull   非空输入（in-influence-nonnull）
     * @param result    是否“生成”关系（过程负责产生新对象）
     */
    public record PortIR(
            String entityId,
            String zhName,
            String direction,
            boolean nonnull,
            boolean result,
            String javaType
    ) {}

    /** 过程局部量（设计视图里的模板成员） */
    public record LocalVarIR(String entityId, String zhName, String javaType, String initLiteral) {}

    /**
     * 过程节点（设计视图里的一个过程图元）
     *
     * @param kind           TOOL（内置工具）/ CALL（业务子过程或独立过程）
     * @param toolKey        内置工具键 innerClass.innerType
     * @param calleeDefKey   被调过程定义键（CALL）
     * @param calleeTimeConsuming 被调过程在本调用点是否耗时（图元级 timeRelated 优先，否则取过程实体属性）
     * @param slow           本节点自身是否为耗时工具节点（画布上标记为时间有关的工具过程）
     * @param firstStart     是否首个启动入口
     * @param host           宿主操作数：CALL 节点为被调过程的宿主对象；TOOL 节点为集合类工具操作的集合
     * @param inputs         入参绑定（IN 与 IN_OUT 形参的读取）
     * @param writes         写出绑定（OUT 与 IN_OUT 形参的写回目标）
     * @param resultJavaType 工具节点输出值的 Java 类型占位（CALL 节点为空串，其输出按形参类型取）
     * @param nexts          后继激活
     * @param postActions    节点成功后的后置动作
     */
    public record NodeIR(
            int pc,
            String cellId,
            String entityId,
            String zhName,
            String kind,
            String toolKey,
            String calleeDefKey,
            boolean calleeTimeConsuming,
            boolean slow,
            boolean firstStart,
            boolean lastEnd,
            OperandIR host,
            List<ArgBindingIR> inputs,
            List<ArgBindingIR> writes,
            String resultJavaType,
            List<NextIR> nexts,
            List<PostIR> postActions,
            double x,
            double y
    ) {
        public NodeIR(int pc, String cellId, String entityId, String zhName, String kind, String toolKey,
                      String calleeDefKey, boolean calleeTimeConsuming, boolean slow, boolean firstStart,
                      OperandIR host, List<ArgBindingIR> inputs, List<ArgBindingIR> writes, String resultJavaType,
                      List<NextIR> nexts, List<PostIR> postActions, double x, double y) {
            this(pc, cellId, entityId, zhName, kind, toolKey, calleeDefKey, calleeTimeConsuming, slow,
                    firstStart, false, host, inputs, writes, resultJavaType, nexts, postActions, x, y);
        }
    }

    /** 实参绑定 */
    public record ArgBindingIR(String argName, OperandIR operand) {}

    /**
     * 后继激活
     *
     * @param conditional     是否带线上判断（从输出端口出发的调用线）
     * @param conditionSource 被判断的输出形参名（如 c）；为空表示判断事件源成员本身
     * @param conditions      比较项列表
     * @param expression      比较项的组合表达式（如 "1"、"1 && 2"），为空表示全部满足
     * @param eventCode       连线携带的事件码
     */
    public record NextIR(
            int targetPc,
            boolean conditional,
            String conditionSource,
            List<ConditionIR> conditions,
            String expression,
            String eventCode,
            long delayTicks,
            double probability,
            String probabilityGroup
    ) {
        public NextIR(int targetPc, boolean conditional, String conditionSource,
                      List<ConditionIR> conditions, String expression, String eventCode, long delayTicks) {
            this(targetPc, conditional, conditionSource, conditions, expression, eventCode, delayTicks, 1.0D, "");
        }

        public NextIR(int targetPc, boolean conditional, String conditionSource,
                      List<ConditionIR> conditions, String expression, String eventCode) {
            this(targetPc, conditional, conditionSource, conditions, expression, eventCode, 0L);
        }
    }

    /**
     * 线上判断的一个比较项
     *
     * @param index       在表达式里的序号（从 1 开始）
     * @param op          比较符：= != > >= < <=
     * @param literal     比较字面量文本
     * @param literalType 字面量类型：boolean / number / string / null
     */
    public record ConditionIR(int index, String op, String literal, String literalType) {}

    /** 节点成功后的后置动作 */
    public record PostIR(
            String kind,
            OperandIR target,
            String stateLabel,
            String enumTypeId,
            OperandIR value,
            double probability,
            String probabilityGroup
    ) {
        public PostIR(String kind, OperandIR target, String stateLabel, String enumTypeId, OperandIR value) {
            this(kind, target, stateLabel, enumTypeId, value, 1.0D, "");
        }
    }

    /** 启动 / 维持条件 */
    public record ConditionRuleIR(
            OperandIR operand,
            String stateLabel,
            List<ConditionIR> conditions,
            String describe,
            String expression,
            /** R13/R15 前锚点（code4.html M5 收割，说明书 7.2）：过程启动条件全部通过后、
             *  首次步进前把本规则的成员切换到 anchorTargetState（发 sx/sv/sa）；空 = 无前锚点 */
            String anchorTargetState,
            OperandIR anchorMember,
            String anchorEnumTypeId
    ) {
        public ConditionRuleIR(OperandIR operand, String stateLabel, List<ConditionIR> conditions, String describe) {
            this(operand, stateLabel, conditions, describe, "");
        }

        public ConditionRuleIR(OperandIR operand, String stateLabel, List<ConditionIR> conditions,
                               String describe, String expression) {
            this(operand, stateLabel, conditions, describe, expression, "", null, "");
        }
    }

    /**
     * 手动启动调用点：类型 / 场景设计视图里既无事件触发线也无默认启动的业务过程图元。
     * 平台定义（与存量引擎差异）：这类过程不参与自动推演，但宿主提供手动启动入口
     * （GromaModuleRuntime.startManually），供外部干预（MCP run.process / 通用 REST 触发过程）使用。
     */
    public record ManualStartIR(
            String typeId,
            List<String> hostPath,
            String targetDefKey,
            List<ArgBindingIR> args,
            List<PostIR> postActions
    ) {}

    // ------------------------------------------------------------------ 操作数

    /** 操作数：工具入参、写出目标、实参绑定、事件源成员的统一表示；javaType 是类型占位（见 GromaTypes） */
    public sealed interface OperandIR permits SelfIR, MemberPathIR, ParamIR, LocalIR, ConstIR, NodeOutputIR, NamedInstanceIR, BuiltinIR {
        String javaType();
    }

    /** 过程宿主对象自身 */
    public record SelfIR(String javaType) implements OperandIR {}

    /** 从根对象沿成员路径取值 */
    public record MemberPathIR(OperandIR root, List<String> memberIds, String javaType) implements OperandIR {}

    /** 形参槽位 */
    public record ParamIR(String entityId, String zhName, String javaType) implements OperandIR {}

    /** 局部量槽位 */
    public record LocalIR(String entityId, String zhName, String javaType) implements OperandIR {}

    /** 定值常量 */
    public record ConstIR(String entityId, String literal, String javaType, String zhName) implements OperandIR {}

    /** 上游节点的输出槽位 */
    public record NodeOutputIR(String cellId, String argName, String javaType) implements OperandIR {}

    /** 模型级命名实例 */
    public record NamedInstanceIR(String instanceId, String typeId, String javaType) implements OperandIR {}

    /**
     * 内置对象取值
     *
     * @param key SIM_ELAPSED_SECONDS / SIM_DATETIME / LENGTH / RUN_PARAM
     * @param base LENGTH 的集合操作数
     */
    public record BuiltinIR(String key, OperandIR base, String javaType) implements OperandIR {}

    // ------------------------------------------------------------------ 事件接线

    /**
     * 事件触发接线
     *
     * @param sourceTypeId 事件源对象类型 id
     * @param memberToken  发生事件的成员 id
     * @param eventCode    事件码（a / v / sa / sx / ra / rv / rx）
     * @param stateLabel   sa / sx 限定的状态中文原名
     * @param contextPath  从接线所属对象到事件源对象的成员路径
     * @param targetDefKey 目标过程定义键
     * @param hostPath     从接线所属对象到过程宿主的成员路径（空 = 接线对象自身）
     * @param postActions  目标过程成功后的后置动作（调用点上的 out-change / result / consumption）
     */
    public record TriggerIR(
            String sourceTypeId,
            String memberToken,
            String eventCode,
            String stateLabel,
            List<ConditionIR> judge,
            OperandIR judgeOperand,
            List<String> contextPath,
            String targetDefKey,
            List<String> hostPath,
            List<ArgBindingIR> args,
            List<PostIR> postActions,
            String ownerTypeId,
            String describe,
            String expression,
            double probability,
            String probabilityGroup
    ) {
        public TriggerIR(String sourceTypeId, String memberToken, String eventCode, String stateLabel,
                         List<ConditionIR> judge, OperandIR judgeOperand, List<String> contextPath,
                         String targetDefKey, List<String> hostPath, List<ArgBindingIR> args,
                         List<PostIR> postActions, String ownerTypeId, String describe) {
            this(sourceTypeId, memberToken, eventCode, stateLabel, judge, judgeOperand, contextPath,
                    targetDefKey, hostPath, args, postActions, ownerTypeId, describe, "", 1.0D, "");
        }
    }

    /**
     * 默认启动
     *
     * @param hostPath    从新建对象到过程宿主的成员路径
     * @param postActions 目标过程成功后的后置动作
     */
    public record DefaultStartIR(
            String typeId,
            List<String> hostPath,
            String targetDefKey,
            List<ArgBindingIR> args,
            List<PostIR> postActions
    ) {}
}

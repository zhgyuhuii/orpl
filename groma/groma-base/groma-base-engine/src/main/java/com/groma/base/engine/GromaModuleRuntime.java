package com.groma.base.engine;

import java.util.List;
import java.util.Map;

/**
 * 编译产出模型模块对运行宿主暴露的唯一入口（一个模型子模块实现一份，经 ServiceLoader 装载）
 * <p>
 * 宿主的多模型注册中心、MCP 引擎端口、通用 REST 与 WebSocket 都只依赖这个接口，
 * 因此同一份模型代码既能以 Maven 子模块形式编译进合包 JAR，也能经 ECJ 热编译后由隔离类加载器动态挂载。
 * </p>
 */
public interface GromaModuleRuntime {

    /** 场景描述 */
    record SceneDescriptor(String sceneCode, String zhName, String description, String entityId) {}

    /** 对象类型描述（供只读资源目录与外部按名取值） */
    record TypeDescriptor(String typeCode, String zhName, String entityId, List<GromaMemberMeta> members) {}

    /** 模型编码（模块名去掉 model- 前缀，如 emergency-deduction） */
    String modelCode();

    /** 模型版本（画布版本号） */
    String modelVersion();

    /** 模型中文原名 */
    String modelLabel();

    /** 源模型 id */
    String modelId();

    /** 场景未指定步长时使用的默认步长（毫秒） */
    long defaultStepMs();

    /** 全部权威运行场景 */
    List<SceneDescriptor> scenes();

    /** 过程描述（供 MCP 清单校验与只读资源目录） */
    record ProcessDescriptor(
            String defKey,
            String entityId,
            String zhName,
            String ownerTypeCode,
            String ownerZhName,
            String kind,
            boolean timeConsuming,
            String description
    ) {}

    /**
     * 手动启动点描述：类型 / 场景设计视图里既无事件触发线也无默认启动的过程调用点。
     * 这类过程不参与自动推演，宿主经 {@link #startManually} 触发（外部干预）
     */
    record ManualStartDescriptor(
            String defKey,
            String zhName,
            String ownerTypeCode,
            List<String> paramNames
    ) {}

    /** 模型级事件接线表 */
    GromaTriggerTable triggerTable();

    /** 创建一个场景推演沙盒（调用方负责在单写者线程上执行 initializeOnLoop） */
    GromaWorldContext createSession(String sceneCode, String sessionId, long startEpochMs, long finalStepMs, long seed,
                                    Map<String, Object> initialOverrides);

    /** 按类型编码新建空实体（存档续跑重建用） */
    BaseSimEntity newEntity(String typeCode, String instanceId);

    /**
     * 按过程定义键新建过程任务。args 为空时按默认绑定从宿主成员读取实参；
     * 非空时按定义视图形参中文名覆盖（供 MCP 无会话执行与外部干预使用）
     */
    GromaProcessTask newProcess(String defKey, GromaWorldContext ctx, BaseSimEntity host, Map<String, Object> args);

    /** 全部对象类型 */
    List<TypeDescriptor> types();

    /** 全部过程定义 */
    List<ProcessDescriptor> processes();

    /** 大模型全局语义导航索引（GROMA_MODEL_INDEX.md 内容） */
    String indexMarkdown();

    /** 全部手动启动点（无事件触发线、无默认启动的调用点；宿主外部干预的发现目录） */
    default List<ManualStartDescriptor> manualStarts() {
        return List.of();
    }

    /**
     * 手动启动一个过程（外部干预：MCP run.process / 通用 REST 触发过程）。
     * argOverrides 按定义视图形参中文名覆盖画布绑定的默认实参；
     * 返回 null 表示该模型没有此启动点。调用方负责在单写者线程上执行
     */
    default GromaProcessTask startManually(String defKey, GromaWorldContext ctx, BaseSimEntity host,
                                           Map<String, Object> argOverrides) {
        return null;
    }

    /**
     * 存档续跑重建在途顶层任务后，按原接线上下文重挂输出影响回写钩子（T1.5 分叉根因二：在途任务对齐）。
     * 触发接线启动的过程成功后要向 context 实体回写 IN_OUT/OUT 影响成员（生成的 afterN）；
     * 该钩子是接线侧闭包，任务重建时必须由生成运行时按 defKey 重挂，否则续跑后的过程成功不回写成员。
     * context 为存档记录的接线上下文实例（无接线上下文时为 null，本方法为空实现不挂任何钩子）。
     */
    default void reattachWiringHook(String defKey, GromaWorldContext ctx, BaseSimEntity context,
                                    BaseSimEntity host, GromaProcessTask task) {
    }
}

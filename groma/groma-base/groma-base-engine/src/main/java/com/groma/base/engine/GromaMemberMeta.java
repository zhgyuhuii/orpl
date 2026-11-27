package com.groma.base.engine;

/**
 * 编译产出领域实体的成员元数据（零反射成员读写、快照输出与外部按名取值都依赖这张小表）
 *
 * @param token    成员在模型中的稳定标识（画布实体 id），事件接线与存档都用它定位成员
 * @param zhName   方圆中文原名（快照与对外接口的成员键）
 * @param enName   规范英文成员名（与 Entity 的 Getter/Setter 一致）
 * @param kind     BASE / ENUM / REF / COLLECTION
 * @param typeName 成员 Java 类型名或枚举/对象类型编码
 * @param unit     物理量纲（没有则为空串）
 */
public record GromaMemberMeta(String token, String zhName, String enName, String kind, String typeName, String unit) {

    public static final String KIND_BASE = "BASE";
    public static final String KIND_ENUM = "ENUM";
    public static final String KIND_REF = "REF";
    public static final String KIND_COLLECTION = "COLLECTION";
}

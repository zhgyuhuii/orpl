package com.groma.base.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方圆语言 R09/R10 单向/双向带标签关联语义注解（收割自 code3.html G3 方案，按真实 API 落地）。
 * <p>保留至运行期（0 字节码外开销），供关联管理器类与字段落声明正反向语义标签，
 * 供 MCP 资源/文档读取器反射读取，避免语义标签只存在于画布与 Markdown。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(SemanticTags.class)
public @interface SemanticTag {

    /** 关联类型实体 id（ASSOCIATION-TYPE / 带标签连线实体） */
    String relationId() default "";

    /** 对端类型实体 id */
    String targetType() default "";

    /** 正向语义标签（左端 → 右端） */
    String forwardLabel() default "";

    /** 反向语义标签（右端 → 左端） */
    String reverseLabel() default "";

    /** 是否双向关联（R10） */
    boolean bidirectional() default false;
}

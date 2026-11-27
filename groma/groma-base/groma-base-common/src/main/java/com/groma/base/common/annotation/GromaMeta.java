package com.groma.base.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字节码级双语元数据保留注解（RetentionPolicy.RUNTIME，落实 NP-GEN-004）
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface GromaMeta {
    String id();
    String zhName();
    String kind() default "";
    String essence() default "";
    String unit() default "";
    String min() default "";
    String max() default "";
    String initState() default "";
    String relations() default "";
}

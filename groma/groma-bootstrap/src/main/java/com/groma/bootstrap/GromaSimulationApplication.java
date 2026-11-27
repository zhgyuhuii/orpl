package com.groma.bootstrap;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 新平台统一运行宿主（同时挂载 N 个模型子模块，装配全限定名 Bean 生成器防同名冲突，落实 NP-HOST-001）
 */
@SpringBootApplication(
        scanBasePackages = {"com.groma.bootstrap", "com.groma.base", "com.groma.models"},
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class GromaSimulationApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GromaSimulationApplication.class)
                .beanNameGenerator(FullyQualifiedAnnotationBeanNameGenerator.INSTANCE)
                .run(args);
    }
}

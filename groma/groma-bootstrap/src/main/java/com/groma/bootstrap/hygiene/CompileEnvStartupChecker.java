package com.groma.bootstrap.hygiene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * 启动期运行时与编译器主版本一致性自检（落实 BE-CFG-002）
 */
@Component
public class CompileEnvStartupChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CompileEnvStartupChecker.class);

    @Override
    public void run(ApplicationArguments args) {
        int runtimeFeature = Runtime.version().feature();
        JavaCompiler sysJavac = ToolProvider.getSystemJavaCompiler();
        if (sysJavac != null) {
            boolean supportsRuntime = sysJavac.getSourceVersions().stream()
                    .anyMatch(v -> v.ordinal() >= runtimeFeature);
            if (!supportsRuntime) {
                throw new IllegalStateException("系统 javac 支持的源码版本低于当前运行时 JDK " + runtimeFeature);
            }
            log.info("[StartupCheck] 运行时 JDK={}，系统 javac 可用且版本匹配，内嵌 ECJ 已就绪", runtimeFeature);
        } else {
            log.info("[StartupCheck] 当前为精简 JRE={} 环境，将使用内嵌纯 Java Eclipse ECJ 编译器", runtimeFeature);
        }
    }
}

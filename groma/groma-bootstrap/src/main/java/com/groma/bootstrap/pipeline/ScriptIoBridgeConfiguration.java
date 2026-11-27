package com.groma.bootstrap.pipeline;

import com.groma.base.connector.ExternalProgramConnector;
import com.groma.base.engine.GromaWorldContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 收割块 27（方案 F 关键接线）：把模块内嵌算法资源（/scripts/*.py，由 LeafBodyEmitter 剥离产出）
 * 提取至受控临时文件并桥接至 ExternalProgramConnector 的 JSON stdin/stdout 受控执行，
 * 产出 {@link GromaWorldContext.ExternalScriptIoExecutor}；由 {@link GromaModelRegistry}
 * 在会话初始化回调中挂载（ctx.setExternalScriptIoExecutor），使模型 2 的
 * 航路规划等 Python 算法经双向数据桥真实运行。
 */
@Configuration
public class ScriptIoBridgeConfiguration {

    @Bean
    public GromaWorldContext.ExternalScriptIoExecutor gromaExternalScriptIoExecutor(
            ExternalProgramConnector connector) {
        return (ctx, scriptName, processId, inputs) -> {
            // 生成模型类由 ModelModuleClassLoader 加载：ctx 的类加载器即可见模块内嵌 /scripts 资源
            ClassLoader moduleLoader = ctx.getClass().getClassLoader();
            String resourcePath = scriptName.startsWith("scripts/") ? scriptName : "scripts/" + scriptName;
            Path tempScript = Files.createTempFile("groma-mod-script-",
                    "-" + Path.of(scriptName).getFileName());
            try (InputStream in = moduleLoader.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalStateException("模块脚本资源不存在: " + resourcePath);
                }
                Files.copy(in, tempScript, StandardCopyOption.REPLACE_EXISTING);
                return connector.runScriptWithJsonIo("python3", tempScript, inputs);
            } finally {
                Files.deleteIfExists(tempScript);
            }
        };
    }

    /** 默认 15s 算法超时（Duration 版本保留给未来配置化扩展） */
    @SuppressWarnings("unused")
    private static Duration defaultScriptTimeout() {
        return Duration.ofSeconds(15);
    }
}

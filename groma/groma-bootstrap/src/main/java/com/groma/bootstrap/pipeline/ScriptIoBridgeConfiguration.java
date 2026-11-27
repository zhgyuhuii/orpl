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

    /**
     * 受控外部程序连接器（方案 F 收割时缺失的宿主装配）：python3 从 PATH 运行期解析（不写死绝对路径，
     * NP-R03）；脚本沙盒目录 / 超时 / 允许主机接 application.yml 的 groma.connector.*（原死配置启用）。
     */
    @org.springframework.context.annotation.Bean
    public ExternalProgramConnector externalProgramConnector(
            @org.springframework.beans.factory.annotation.Value(
                    "${groma.connector.script-allowlist-dir:./scripts-sandbox}") String allowlistDir,
            @org.springframework.beans.factory.annotation.Value(
                    "${groma.connector.mcp-client-allowlist:localhost}") String allowHosts,
            @org.springframework.beans.factory.annotation.Value(
                    "${groma.connector.http-read-timeout-ms:60000}") long httpTimeoutMs) {
        java.util.Map<String, Path> executables = new java.util.HashMap<>();
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
                Path candidate = Path.of(dir).resolve("python3");
                if (java.nio.file.Files.isExecutable(candidate)) {
                    executables.put("python3", candidate);
                    break;
                }
            }
        }
        Path sandbox = Path.of(allowlistDir);
        try {
            java.nio.file.Files.createDirectories(sandbox);
        } catch (java.io.IOException ignored) {
            // 沙盒目录创建失败时连接器会在执行期按绝对路径校验兜底
        }
        java.util.Set<String> hosts = java.util.Set.of(allowHosts.split(","));
        return new ExternalProgramConnector(executables, sandbox, false, hosts,
                java.time.Duration.ofMillis(Math.max(1_000L, httpTimeoutMs)));
    }

    @Bean
    public GromaWorldContext.ExternalScriptIoExecutor gromaExternalScriptIoExecutor(
            ExternalProgramConnector connector) {
        return (ctx, scriptName, processId, inputs) -> {
            // 生成模型类由 ModelModuleClassLoader 加载：ctx 的类加载器即可见模块内嵌 /scripts 资源。
            // ExternalScriptIoExecutor.run 不声明受检异常：IOException 与引擎侧约定一致降级为问题记录 + 空回包
            ClassLoader moduleLoader = ctx.getClass().getClassLoader();
            String resourcePath = scriptName.startsWith("scripts/") ? scriptName : "scripts/" + scriptName;
            Path tempScript = null;
            try {
                tempScript = Files.createTempFile("groma-mod-script-",
                        "-" + Path.of(scriptName).getFileName());
                try (InputStream in = moduleLoader.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        ctx.recordPublicProblem("模块脚本资源不存在: " + resourcePath);
                        return java.util.Map.of();
                    }
                    Files.copy(in, tempScript, StandardCopyOption.REPLACE_EXISTING);
                    return connector.runScriptWithJsonIo("python3", tempScript, inputs);
                }
            } catch (java.io.IOException ioEx) {
                ctx.recordPublicProblem("脚本 " + scriptName + " 受控执行失败: " + ioEx.getMessage());
                return java.util.Map.of();
            } finally {
                if (tempScript != null) {
                    try {
                        Files.deleteIfExists(tempScript);
                    } catch (java.io.IOException cleanupFailure) {
                        // 临时脚本清理失败无碍：系统临时目录自会回收
                    }
                }
            }
        };
    }

    /** 默认 15s 算法超时（Duration 版本保留给未来配置化扩展） */
    @SuppressWarnings("unused")
    private static Duration defaultScriptTimeout() {
        return Duration.ofSeconds(15);
    }
}

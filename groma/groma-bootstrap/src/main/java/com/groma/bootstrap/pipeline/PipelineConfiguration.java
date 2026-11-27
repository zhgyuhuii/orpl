package com.groma.bootstrap.pipeline;

import com.groma.studio.compiler.AtomicBytecodeCache;
import com.groma.studio.compiler.EcjInMemoryCompiler;
import com.groma.studio.compiler.JavacParityVerifier;
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
import com.groma.studio.sync.WorkspaceSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * 流水线组件装配（阶段 2-C3）：ECJ 内存编译器、字节码缓存、版本钉住热挂载管理器、
 * 工程同步服务与 javac 对拍器；模型注册中心（T3）在热挂载管理器之上组装。
 */
@Configuration
public class PipelineConfiguration {

    @Bean(destroyMethod = "")
    public VersionPinnedHotSwapManager versionPinnedHotSwapManager(
            @Value("${groma.pipeline.bytecode-cache-dir:}") String cacheDir,
            @Value("${groma.pipeline.workspace-root:groma-workspace}") String workspaceRoot) {
        AtomicBytecodeCache cache = cacheDir == null || cacheDir.isBlank()
                ? null
                : new AtomicBytecodeCache(Path.of(cacheDir), "ecj", "base");
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        return new VersionPinnedHotSwapManager(new EcjInMemoryCompiler(), cache, parent);
    }

    @Bean
    public WorkspaceSyncService workspaceSyncService() {
        return new WorkspaceSyncService();
    }

    @Bean
    public JavacParityVerifier javacParityVerifier() {
        return new JavacParityVerifier();
    }

    /** 会话管理器：手动 Tick 模式（loopGroup 空 = 调用线程逐步推进，结果确定） */
    @Bean(destroyMethod = "close")
    public com.groma.base.engine.runtime.GromaSessionManager gromaSessionManager() {
        return new com.groma.base.engine.runtime.GromaSessionManager(null, 8);
    }

    /** Swagger 分组：模型构建 / 会话 REST 与 MCP 端点各自成组（设计书 2-E 第 5 项） */
    @Bean
    public org.springdoc.core.models.GroupedOpenApi studioApiGroup() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("groma-studio")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi mcpApiGroup() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("groma-mcp")
                .pathsToMatch("/mcp/**")
                .build();
    }
}

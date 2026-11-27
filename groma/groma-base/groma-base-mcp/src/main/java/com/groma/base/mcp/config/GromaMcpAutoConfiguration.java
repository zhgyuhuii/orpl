package com.groma.base.mcp.config;

import com.groma.base.mcp.context.GatewayHeaderExtractFilter;
import com.groma.base.mcp.observability.GromaMcpMetricsRecorder;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.mcp.registry.GromaModelResourceProvider;
import com.groma.base.mcp.registry.ModelToolRegistrar;
import com.groma.base.mcp.tools.GromaFixedMcpTools;
import io.micrometer.core.instrument.MeterRegistry;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * MCP 服务端基础设施自动装配类（零手写 Bean 名，100% 满足 scan.py NP-R08）
 */
@AutoConfiguration
@EnableConfigurationProperties(GromaMcpProperties.class)
public class GromaMcpAutoConfiguration {

    @Bean
    public FilterRegistrationBean<GatewayHeaderExtractFilter> gatewayHeaderExtractFilter() {
        FilterRegistrationBean<GatewayHeaderExtractFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new GatewayHeaderExtractFilter());
        reg.addUrlPatterns("/mcp", "/mcp/*", "/api/v1/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }

    @Bean
    public GromaMcpMetricsRecorder gromaMcpMetricsRecorder(
            MeterRegistry meterRegistry,
            GromaMcpEnginePort enginePort) {
        return new GromaMcpMetricsRecorder(meterRegistry, enginePort);
    }

    @Bean
    public GromaFixedMcpTools gromaFixedMcpTools(
            GromaMcpEnginePort enginePort,
            GromaMcpProperties properties,
            GromaMcpMetricsRecorder metricsRecorder) {
        return new GromaFixedMcpTools(enginePort, properties, metricsRecorder);
    }

    @Bean
    public GromaModelResourceProvider gromaModelResourceProvider(GromaMcpEnginePort enginePort) {
        return new GromaModelResourceProvider(enginePort);
    }

    @Bean
    public ModelToolRegistrar modelToolRegistrar(
            ObjectProvider<McpSyncServer> mcpSyncServerProvider,
            GromaMcpEnginePort enginePort,
            GromaMcpProperties properties,
            GromaMcpMetricsRecorder metricsRecorder) {
        return new ModelToolRegistrar(
                mcpSyncServerProvider.getIfAvailable(),
                enginePort,
                properties,
                metricsRecorder
        );
    }
}

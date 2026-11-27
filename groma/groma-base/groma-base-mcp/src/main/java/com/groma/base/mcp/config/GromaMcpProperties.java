package com.groma.base.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 服务端运行期可调参数（全部支持环境变量覆盖，默认值满足 03-mcp-analysis.md 要求）
 */
@ConfigurationProperties(prefix = "groma.mcp")
public class GromaMcpProperties {

    /**
     * 传输模式说明：STATELESS_EXPLICIT_SESSION 或 STATEFUL_AFFINITY（默认采用无状态传输+显式推演 sessionId，免除网关粘性会话负担）
     */
    private String transportSessionStrategy = "STATELESS_EXPLICIT_SESSION";

    /**
     * 是否默认启用“每个发布过程动态注册一个专属工具”（大模型工具过多时可设为 false，仅用通用 run.process）
     */
    private boolean perProcessToolEnabled = true;

    /**
     * 单模型独立部署时是否省略 <model_code>__ 前缀（多模型合包默认 true 强制带前缀防冲突）
     */
    private boolean prefixToolWithModelCode = true;

    /**
     * 单服务实例允许并发存活的最大推演会话上限（超出返回 LIMIT_EXCEEDED）
     */
    private int maxConcurrentSessions = 200;

    /**
     * 查询类工具默认分页大小与单页硬上限（03-mcp-analysis.md 4.6：默认最多 200 条）
     */
    private int defaultPageSize = 200;
    private int maxPageSize = 500;

    /**
     * 无状态调用耗时过程的默认墙钟超时毫秒数（默认 60,000ms，小于 Spring AI request-timeout 90s）
     */
    private long statelessProcessTimeoutMs = 60_000L;

    public String getTransportSessionStrategy() {
        return transportSessionStrategy;
    }

    public void setTransportSessionStrategy(String transportSessionStrategy) {
        this.transportSessionStrategy = transportSessionStrategy;
    }

    public boolean isPerProcessToolEnabled() {
        return perProcessToolEnabled;
    }

    public void setPerProcessToolEnabled(boolean perProcessToolEnabled) {
        this.perProcessToolEnabled = perProcessToolEnabled;
    }

    public boolean isPrefixToolWithModelCode() {
        return prefixToolWithModelCode;
    }

    public void setPrefixToolWithModelCode(boolean prefixToolWithModelCode) {
        this.prefixToolWithModelCode = prefixToolWithModelCode;
    }

    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    public void setMaxConcurrentSessions(int maxConcurrentSessions) {
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public long getStatelessProcessTimeoutMs() {
        return statelessProcessTimeoutMs;
    }

    public void setStatelessProcessTimeoutMs(long statelessProcessTimeoutMs) {
        this.statelessProcessTimeoutMs = statelessProcessTimeoutMs;
    }
}

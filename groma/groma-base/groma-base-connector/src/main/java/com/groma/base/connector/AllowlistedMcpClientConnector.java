package com.groma.base.connector;

import java.net.URI;
import java.util.Set;

/**
 * 外部 MCP 客户端连接器（落实 BE-EXT-006：强制域名/主机白名单校验，封堵云元数据与内网 SSRF）
 */
public final class AllowlistedMcpClientConnector {

    private final Set<String> allowlistedHosts;

    public AllowlistedMcpClientConnector(Set<String> allowlistedHosts) {
        this.allowlistedHosts = Set.copyOf(allowlistedHosts);
    }

    public URI validateTargetEndpoint(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("MCP 目标地址不能为空");
        }
        URI uri = URI.create(targetUrl.trim());
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new SecurityException("[EXT-MCP-001] 仅支持 http/https 协议");
        }
        if (host == null || !allowlistedHosts.contains(host.toLowerCase())) {
            throw new SecurityException("[EXT-MCP-001] 目标 MCP 地址不在管理员白名单内，拒绝连接: " + host);
        }
        return uri;
    }
}

package com.groma.base.mcp.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关请求头提取钩子过滤器（覆盖 /mcp 与 /api/v1/**，无凭据直接放行，落实 BE-SEC-001 & NP-MCP-010）
 */
public final class GatewayHeaderExtractFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Map<String, String> headerMap = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headerMap.put(name, request.getHeader(name));
            }
        }
        GatewayIdentityContext.GatewayHeaders extracted = GatewayIdentityContext.extractFromMap(headerMap);
        GatewayIdentityContext.bind(extracted);
        response.setHeader("X-Request-Id", extracted.traceId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            GatewayIdentityContext.clear();
        }
    }
}

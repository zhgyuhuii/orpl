package com.groma.bootstrap.hygiene;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 日志上下文任务标识消毒过滤器（落实 BE-SEC-004：拦截换行符伪造日志注入）
 */
@Component
public class TaskIdMdcSanitizingFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_TASK_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    public static String sanitizeTaskId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "none";
        }
        if (SAFE_TASK_ID.matcher(raw).matches()) {
            return raw;
        }
        String stripped = raw.replaceAll("[^A-Za-z0-9_-]", "_");
        return stripped.length() > 64 ? stripped.substring(0, 64) : stripped;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String candidate = request.getHeader("X-Session-Id");
        MDC.put("taskId", sanitizeTaskId(candidate));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("taskId");
        }
    }
}

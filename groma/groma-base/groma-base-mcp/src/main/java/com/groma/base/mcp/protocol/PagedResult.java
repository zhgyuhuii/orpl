package com.groma.base.mcp.protocol;

import java.util.List;

/**
 * 观测类工具统一游标分页包装结构（落实 NP-MCP-005：返回 items、nextCursor、truncated、total，保护智能体上下文窗口）
 */
public record PagedResult<E>(
        List<E> items,
        String nextCursor,
        boolean truncated,
        int total
) {
    public static <E> PagedResult<E> slice(List<E> fullList, String cursorStr, Integer requestedLimit, int defaultLimit, int maxLimit) {
        int totalSize = fullList == null ? 0 : fullList.size();
        if (totalSize == 0) {
            return new PagedResult<>(List.of(), null, false, 0);
        }
        int offset = parseCursorOffset(cursorStr);
        if (offset >= totalSize) {
            return new PagedResult<>(List.of(), null, false, totalSize);
        }
        int effectiveLimit = (requestedLimit == null || requestedLimit <= 0)
                ? defaultLimit
                : Math.min(requestedLimit, maxLimit);

        int endExclusive = Math.min(totalSize, offset + effectiveLimit);
        boolean hasMore = endExclusive < totalSize;
        String next = hasMore ? String.valueOf(endExclusive) : null;
        return new PagedResult<>(List.copyOf(fullList.subList(offset, endExclusive)), next, hasMore, totalSize);
    }

    private static int parseCursorOffset(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(cursorStr.trim()));
        } catch (NumberFormatException ex) {
            throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "非法的分页游标 cursor: " + cursorStr);
        }
    }
}

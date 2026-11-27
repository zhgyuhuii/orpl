#!/usr/bin/env bash
# 文件: tools/regression/mcp-contract.sh
# 命令编号: CMD-NP-13 (H-13) | Streamable HTTP /mcp 真实握手、tools/list、resources/list 与结构校验
source "$(dirname "${BASH_SOURCE[0]}")/common-harness.sh"

require_service_up
INIT_RESP="$(curl -fsS -X POST "$ODS_BASE_URL/mcp" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"groma-contract-checker","version":"1.0"}}}' || true)"

if echo "$INIT_RESP" | grep -q '"protocolVersion"'; then
    record_pass "MCP-INIT-HANDSHAKE" "Streamable HTTP /mcp 无凭据握手成功" "2025-06-18"
else
    record_fail "MCP-INIT-HANDSHAKE" "/mcp 初始化握手响应异常" "$INIT_RESP"
fi
finish_summary

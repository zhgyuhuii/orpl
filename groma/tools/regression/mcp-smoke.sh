#!/usr/bin/env bash
# 文件: tools/regression/mcp-smoke.sh
# 命令编号: CMD-NP-14：groma-bootstrap 客户端冒烟——起服务、等健康、POST 六步流水线构建并热挂载，
#   断言 success=true 后停服。REST/MCP 共用同一引擎端口（GromaMcpEnginePort），此处验证服务主链路。
#
# 用法: bash tools/regression/mcp-smoke.sh [--help]
# 环境变量:
#   MCP_SMOKE_PORT   服务端口（默认 18090）
#   CORPUS_DIR / JAVA_HOME / MAVEN_HOME / MAVEN_ONLINE  同 service-harness.sh
# 退出码: 0 = 通过；1 = 未通过；2 = 环境阻塞
set -euo pipefail

case "${1:-}" in -h|--help) sed -n '2,13p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac
source "$(dirname "${BASH_SOURCE[0]}")/service-harness.sh"

PORT="${MCP_SMOKE_PORT:-18090}"
LOG="$SERVICE_DIR/target/regression/mcp-smoke-$PORT.log"
mkdir -p "$(dirname "$LOG")"
trap 'rc=$?; svc_stop; exit $rc' EXIT

require_service_tools
svc_ensure_jar

if ! svc_start "$PORT" "$LOG"; then
    echo "FAIL mcp-smoke 服务未能启动或健康检查超时（日志: ${LOG}）"
    exit 1
fi

if ! svc_build_model "$PORT"; then
    echo "FAIL mcp-smoke 模型构建 success!=true（日志: ${LOG}）"
    exit 1
fi

# 附带核对当前可见模型列表（构建后应含挂载模型）
MODELS_LIST="$(curl -fsS --max-time 15 "http://127.0.0.1:$PORT/api/v1/studio/models" || true)"
echo "[mcp-smoke] 可见模型: ${MODELS_LIST:0:200}"

echo "PASS mcp-smoke 健康检查 + 模型构建热挂载全部通过（端口 ${PORT}）"
exit 0

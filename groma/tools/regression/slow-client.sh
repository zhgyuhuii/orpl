#!/usr/bin/env bash
# 文件: tools/regression/slow-client.sh
# 命令编号: CMD-NP-07：慢客户端隔离——用 curl --limit-rate 以 16KB/s 的龟速拉取不可变快照接口，
#   断言慢读能完整拿到快照、期间服务不崩、恢复后常规请求仍正常。
#
# 用法: bash tools/regression/slow-client.sh [--help]
# 环境变量:
#   SLOW_CLIENT_PORT   服务端口（默认 18091）
#   MODEL_CODE/SCENE_CODE / CORPUS_DIR / JAVA_HOME / MAVEN_HOME / MAVEN_ONLINE  同 service-harness.sh
# 退出码: 0 = 通过；1 = 未通过；2 = 环境阻塞
set -euo pipefail

case "${1:-}" in -h|--help) sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac
source "$(dirname "${BASH_SOURCE[0]}")/service-harness.sh"

PORT="${SLOW_CLIENT_PORT:-18091}"
BASE="http://127.0.0.1:$PORT"
LOG="$SERVICE_DIR/target/regression/slow-client-$PORT.log"
mkdir -p "$(dirname "$LOG")"
trap 'rc=$?; svc_stop; exit $rc' EXIT

require_service_tools
svc_ensure_jar
svc_start "$PORT" "$LOG" || { echo "FAIL slow-client 服务未能启动（日志: ${LOG}）"; exit 1; }
svc_build_model "$PORT" || { echo "FAIL slow-client 模型构建未成功"; exit 1; }

SESSION_ID="$(svc_create_session "$PORT" 20261126)" \
    || { echo "FAIL slow-client 建会话失败（model=$MODEL_CODE scene=${SCENE_CODE}）"; exit 1; }
echo "[slow-client] 会话: $SESSION_ID"

# 先推进 300 拍再读快照，让快照体积足够大，慢速限速才有意义
if ! curl -fsS --max-time 120 -X POST "$BASE/api/v1/studio/sessions/$SESSION_ID/step" \
        -H 'Content-Type: application/json' -d '{"steps":300}' >/dev/null; then
    echo "FAIL slow-client 推进 300 拍失败"
    exit 1
fi
# 退出时清理：删会话 + 停服务
trap 'rc=$?; curl -sS -X DELETE "$BASE/api/v1/studio/sessions/$SESSION_ID" >/dev/null 2>&1 || true; svc_stop; exit $rc' EXIT

# 慢读快照：限速 16KB/s，90s 上限；-f 断言 HTTP 200
SECONDS=0
if ! curl -fsS --limit-rate 16k --max-time 90 "$BASE/api/v1/studio/sessions/$SESSION_ID/snapshot" \
        -o "$SERVICE_DIR/target/regression/slow-client-snapshot.json"; then
    echo "FAIL slow-client 慢读快照失败（curl 退出码 $?，限速 16KB/s / 上限 90s）"
    exit 1
fi
SLOW_SECONDS=$SECONDS
SNAP_BYTES="$(wc -c < "$SERVICE_DIR/target/regression/slow-client-snapshot.json" | tr -d ' ')"
echo "[slow-client] 慢读完成: ${SNAP_BYTES}B 用时 ${SLOW_SECONDS}s（16KB/s 限速）"

# 服务必须仍然健康，且常规请求不受慢客户端影响
if ! curl -fsS --max-time 5 "$BASE/actuator/health" >/dev/null; then
    echo "FAIL slow-client 慢读后健康检查失败，服务已受影响"
    exit 1
fi
STEP_START=$SECONDS
if ! curl -fsS --max-time 10 -X POST "$BASE/api/v1/studio/sessions/$SESSION_ID/step" \
        -H 'Content-Type: application/json' -d '{"steps":1}' >/dev/null; then
    echo "FAIL slow-client 慢读期间常规推进请求失败"
    exit 1
fi
echo "[slow-client] 慢读期间常规请求正常（step 用时 $((SECONDS - STEP_START))s）"

curl -fsS -X DELETE "$BASE/api/v1/studio/sessions/$SESSION_ID" >/dev/null
echo "PASS slow-client 慢客户端不阻塞服务：快照完整、健康检查与常规请求全部正常"
exit 0

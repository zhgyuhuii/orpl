#!/usr/bin/env bash
# 文件: tools/regression/capacity.sh
# 命令编号: CMD-NP-09：单宿主并发容量——并发建 N 个推演会话，断言每个请求要么 success=true，
#   要么返回明确的 LIMIT_EXCEEDED（语义化限流），不允许出现 INTERNAL_ERROR 等未定义失败。
#
# 用法: bash tools/regression/capacity.sh [--help]
# 环境变量:
#   CAPACITY_SESSIONS   并发会话数（默认 16）
#   CAPACITY_PORT       自起服务端口（默认 18092）
#   GROMA_BASE_URL      已运行服务的地址；可连通时直接压测该服务，不再自起
#   MODEL_CODE/SCENE_CODE / CORPUS_DIR / JAVA_HOME / MAVEN_HOME / MAVEN_ONLINE  同 service-harness.sh
# 退出码: 0 = 通过；1 = 未通过；2 = 环境阻塞
set -euo pipefail

case "${1:-}" in -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac
source "$(dirname "${BASH_SOURCE[0]}")/service-harness.sh"

N="${CAPACITY_SESSIONS:-16}"
PORT="${CAPACITY_PORT:-18092}"
BASE="${GROMA_BASE_URL:-http://127.0.0.1:$PORT}"
SELF_STARTED=0

require_service_tools

# 目标服务不可达时自起一个
if ! curl -fsS --max-time 3 "$BASE/actuator/health" >/dev/null 2>&1; then
    svc_ensure_jar
    LOG="$SERVICE_DIR/target/regression/capacity-$PORT.log"
    mkdir -p "$(dirname "$LOG")"
    svc_start "$PORT" "$LOG" || { echo "FAIL capacity 服务未能启动（日志: ${LOG}）"; exit 1; }
    SELF_STARTED=1
    trap 'rc=$?; svc_stop; exit $rc' EXIT
fi

# 目标服务上触发模型构建（已挂载时服务端也会快速返回，重复构建幂等）
if ! curl -sS --max-time 600 -X POST "$BASE/api/v1/studio/models/build" \
        -H 'Content-Type: application/json' -d "{\"corpusDir\":\"$CORPUS_DIR\"}" \
        | python3 -I -c 'import json,sys; d=json.load(sys.stdin); sys.exit(0 if d.get("success") else 1)'; then
    echo "FAIL capacity 模型构建未成功（base=${BASE}）"
    exit 1
fi

WORKDIR="$(mktemp -d "$SERVICE_DIR/target/regression/capacity.XXXXXX")"
echo "[capacity] 并发创建 $N 个会话（model=$MODEL_CODE scene=${SCENE_CODE}，base=${BASE}）"
PIDS=""
for i in $(seq 1 "$N"); do
    (
        curl -sS --max-time 60 -X POST "$BASE/api/v1/studio/sessions" \
            -H 'Content-Type: application/json' \
            -d "{\"modelCode\":\"$MODEL_CODE\",\"sceneCode\":\"$SCENE_CODE\",\"seed\":$((20261126 + i)),\"startPaused\":true}" \
            >"$WORKDIR/resp-$i.json" 2>"$WORKDIR/err-$i.txt"
    ) &
    PIDS="$PIDS $!"
done
# 只等 curl 子进程；不能裸 wait（会把 svc_start 起的常驻 java 服务也等进去，永远不返回）
for p in $PIDS; do
    wait "$p" || true
done

CREATED=0; LIMITED=0; OTHER=0
CREATED_IDS=""
for i in $(seq 1 "$N"); do
    VERDICT="$(RESULT_FILE="$WORKDIR/resp-$i.json" python3 -I -c '
import json, os, sys
try:
    body = json.load(open(os.environ["RESULT_FILE"]))
except Exception as exc:
    print("OTHER", "响应缺失或非JSON", exc); sys.exit(0)
if body.get("success"):
    data = body.get("data") or {}
    print("CREATED", data.get("sessionId", ""), "")
elif str(body.get("code", "")).startswith("LIMIT_EXCEEDED"):
    print("LIMITED", "", "")
else:
    print("OTHER", body.get("code"), body.get("message"))
')"
    case "$VERDICT" in
        CREATED*)  CREATED=$((CREATED + 1)); CREATED_IDS="$CREATED_IDS $(echo "$VERDICT" | awk '{print $2}')" ;;
        LIMITED*)  LIMITED=$((LIMITED + 1)) ;;
        *)         OTHER=$((OTHER + 1)); echo "FAIL capacity 第 $i 个会话异常: $VERDICT" ;;
    esac
done

# 清理全部成功创建的会话
for sid in $CREATED_IDS; do
    curl -sS -X DELETE "$BASE/api/v1/studio/sessions/$sid" >/dev/null 2>&1 || true
done

echo "[capacity] 结果: 成功=$CREATED LIMIT_EXCEEDED=$LIMITED 其它异常=$OTHER / 总数=$N"
if [ "$OTHER" -ne 0 ] || [ "$CREATED" -eq 0 ]; then
    echo "FAIL capacity 并发建会话出现未定义失败（$OTHER 个）或全部失败"
    exit 1
fi
echo "PASS capacity 全部请求成功或明确 LIMIT_EXCEEDED（成功 ${CREATED}，限流 ${LIMITED}）"
exit 0

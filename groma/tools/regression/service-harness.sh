#!/usr/bin/env bash
# 文件: tools/regression/service-harness.sh
# 需要拉起 groma-bootstrap 服务的回归脚本共用底座（mcp-smoke / slow-client / capacity）。
# 提供依赖检查、jar 兜底构建、后台起服务、健康等待、模型构建、会话创建与停服清理。
#
# 环境变量:
#   JAVA_HOME / MAVEN_HOME   JDK21 与 Maven 路径（CI 可覆盖为通用路径）
#   MAVEN_ONLINE             置 1 时去掉 -o 在线解析依赖（CI 冷缓存首跑用）
#   CORPUS_DIR               语料目录，默认 <仓库根>/docs/model
#   MODEL_CODE / SCENE_CODE  会话与容量脚本使用的模型/场景编码
set -euo pipefail

SERVICE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"   # groma/ 工程根
SERVICE_REPO_ROOT="$(cd "$SERVICE_DIR/.." && pwd)"
cd "$SERVICE_DIR"

SERVICE_JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
SERVICE_MAVEN_HOME="${MAVEN_HOME:-$HOME/tools/apache-maven-3.9.9}"
export JAVA_HOME="$SERVICE_JAVA_HOME"
export PATH="$SERVICE_JAVA_HOME/bin:$SERVICE_MAVEN_HOME/bin:$PATH"
if [ "${MAVEN_ONLINE:-0}" = "1" ]; then
    SERVICE_MVN=(mvn -B)
else
    SERVICE_MVN=(mvn -B -o)
fi
CORPUS_DIR="${CORPUS_DIR:-$SERVICE_REPO_ROOT/docs/model}"
MODEL_CODE="${MODEL_CODE:-emergency-deduction}"
SCENE_CODE="${SCENE_CODE:-scene1}"
SERVICE_JAR="$SERVICE_DIR/groma-bootstrap/target/groma-bootstrap-1.0.0.jar"
SERVICE_PID=""

require_service_tools() {
    command -v java >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 java：需要 JDK21"; exit 2; }
    command -v curl >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 curl"; exit 2; }
    command -v python3 >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 python3：用于解析 Result JSON"; exit 2; }
    [ -f "$CORPUS_DIR/data-a.json" ] || { echo "[阻塞] 语料目录不存在: ${CORPUS_DIR}（可用 CORPUS_DIR=<目录> 指定）"; exit 2; }
}

# jar 不存在时先离线打包（-am 连带上游模块）
svc_ensure_jar() {
    if [ ! -f "$SERVICE_JAR" ]; then
        echo "[service-harness] jar 不存在，先执行 mvn ${SERVICE_MVN[*]} package -DskipTests -pl groma-bootstrap -am"
        "${SERVICE_MVN[@]}" package -DskipTests -pl groma-bootstrap -am
    fi
    [ -f "$SERVICE_JAR" ]
}

# 后台起服务并等待 /actuator/health 200（最多 60s）；失败时打印日志尾部并返回非 0
svc_start() {  # svc_start <端口> <日志文件>
    local port="$1" log="$2" i
    echo "[service-harness] 启动 groma-bootstrap: java -jar groma-bootstrap/target/groma-bootstrap-1.0.0.jar --server.port=$port"
    java -jar "$SERVICE_JAR" --server.port="$port" >"$log" 2>&1 &
    SERVICE_PID=$!
    for i in $(seq 1 60); do
        if curl -fsS -o /dev/null --max-time 2 "http://127.0.0.1:$port/actuator/health" 2>/dev/null; then
            echo "[service-harness] 服务就绪: http://127.0.0.1:$port/actuator/health (等待 ${i}s)"
            return 0
        fi
        if ! kill -0 "$SERVICE_PID" 2>/dev/null; then
            echo "[service-harness] 服务进程退出，日志尾部:"; tail -30 "$log" || true
            return 1
        fi
        sleep 1
    done
    echo "[service-harness] 60s 内健康检查未就绪，日志尾部:"; tail -30 "$log" || true
    return 1
}

svc_stop() {
    if [ -n "$SERVICE_PID" ]; then
        kill "$SERVICE_PID" 2>/dev/null || true
        wait "$SERVICE_PID" 2>/dev/null || true
        SERVICE_PID=""
    fi
}

# 触发六步流水线构建并热挂载模型；返回 0 当且仅当响应 success=true
svc_build_model() {  # svc_build_model <端口>
    local port="$1" resp
    echo "[service-harness] POST /api/v1/studio/models/build corpusDir=$CORPUS_DIR"
    if ! resp="$(curl -sS --max-time 600 -X POST "http://127.0.0.1:$port/api/v1/studio/models/build" \
            -H 'Content-Type: application/json' \
            -d "{\"corpusDir\":\"$CORPUS_DIR\"}")"; then
        echo "[service-harness] build 请求失败"
        return 1
    fi
    RESULT_BODY="$resp" python3 -I -c '
import json, os, sys
body = json.loads(os.environ["RESULT_BODY"])
if body.get("success"):
    report = body.get("data") or {}
    print("[service-harness] build success=true model=%s sources=%s" % (
        report.get("modelCode"), report.get("sourceFileCount")))
    sys.exit(0)
print("[service-harness] build success=false code=%s message=%s" % (body.get("code"), body.get("message")))
sys.exit(1)
'
}

# 建会话；输出 sessionId（失败时输出空串并返回非 0）
svc_create_session() {  # svc_create_session <端口> <种子>
    local port="$1" seed="${2:-20261126}" resp
    if ! resp="$(curl -sS --max-time 30 -X POST "http://127.0.0.1:$port/api/v1/studio/sessions" \
            -H 'Content-Type: application/json' \
            -d "{\"modelCode\":\"$MODEL_CODE\",\"sceneCode\":\"$SCENE_CODE\",\"seed\":$seed,\"startPaused\":true}")"; then
        return 1
    fi
    RESULT_BODY="$resp" python3 -I -c '
import json, os, sys
body = json.loads(os.environ["RESULT_BODY"])
data = body.get("data") or {}
sid = data.get("sessionId") or ""
print(sid)
sys.exit(0 if body.get("success") and sid else 1)
'
}

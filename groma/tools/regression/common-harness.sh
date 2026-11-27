#!/usr/bin/env bash
# 文件: tools/regression/common-harness.sh
# 新平台回归脚本统一输出、阻塞判定与产物目录底座
set -euo pipefail

WORKSPACE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPO_ROOT="$(cd "$WORKSPACE_DIR/.." && pwd)"
# 回归脚本统一以工程根（groma/）为工作目录，从仓库根或任意目录调用皆可执行
cd "$WORKSPACE_DIR"
ODS_BASE_URL="${ODS_BASE_URL:-http://127.0.0.1:8080}"
RUN_TS="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$WORKSPACE_DIR/target/regression/$RUN_TS"
mkdir -p "$OUT_DIR"

# 公共工具链前提：JDK21 + Maven 3.9+，均可用环境变量覆盖（CI 用通用路径，本机默认 Homebrew + ~/tools）
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
MAVEN_HOME="${MAVEN_HOME:-$HOME/tools/apache-maven-3.9.9}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
# MAVEN_ONLINE=1 时去掉 -o（CI 冷缓存首次拉依赖用）；默认离线，要求本地 ~/.m2 已预热
if [ "${MAVEN_ONLINE:-0}" = "1" ]; then
    MVN=(mvn -B)
else
    MVN=(mvn -B -o)
fi
# 真实语料目录：默认仓库根 docs/model，可用 CORPUS_DIR 覆盖
CORPUS_DIR="${CORPUS_DIR:-$REPO_ROOT/docs/model}"

PASSED_COUNT=0
FAILED_COUNT=0

block_exit() {
    echo "[阻塞] $1"
    exit 2
}

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || block_exit "环境缺少命令 $1：$2"
}

require_service_up() {
    require_cmd curl "需要 curl 访问运行中的 groma-bootstrap 服务"
    if ! curl -fsS --max-time 3 "$ODS_BASE_URL/actuator/health" >/dev/null 2>&1; then
        block_exit "目标服务 $ODS_BASE_URL 未启动或健康检查未就绪"
    fi
}

record_pass() {
    local case_id="$1" desc="$2" metric="${3:-ok}"
    echo "PASS $case_id $desc $metric" | tee -a "$OUT_DIR/results.log"
    PASSED_COUNT=$((PASSED_COUNT + 1))
}

record_fail() {
    local case_id="$1" desc="$2" metric="${3:-failed}"
    echo "FAIL $case_id $desc $metric" | tee -a "$OUT_DIR/results.log"
    FAILED_COUNT=$((FAILED_COUNT + 1))
}

finish_summary() {
    echo "SUMMARY passed=$PASSED_COUNT failed=$FAILED_COUNT" | tee -a "$OUT_DIR/results.log"
    [ "$FAILED_COUNT" -eq 0 ] && exit 0 || exit 1
}

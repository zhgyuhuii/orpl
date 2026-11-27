#!/usr/bin/env bash
# 文件: tools/regression/soak.sh
# 命令编号: CMD-NP-08：长稳浸泡——把真实语料端到端六步流水线连续跑 SOAK_ROUNDS 轮，
#   每轮计时并断言全部通过，输出总时长与单轮耗时。反复解析/出码/编译/挂载/推演可暴露
#   资源泄漏与状态残留类缺陷。
#
# 用法: bash tools/regression/soak.sh [--help]
# 环境变量:
#   SOAK_ROUNDS   浸泡轮数（默认 3）
#   CORPUS_DIR / JAVA_HOME / MAVEN_HOME / MAVEN_ONLINE  同 corpus-check.sh
# 退出码: 0 = 全部通过；1 = 有轮次失败；2 = 环境阻塞
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"   # groma/ 工程根
cd "$ROOT_DIR"
case "${1:-}" in -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac

JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
MAVEN_HOME="${MAVEN_HOME:-$HOME/tools/apache-maven-3.9.9}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
if [ "${MAVEN_ONLINE:-0}" = "1" ]; then MVN=(mvn -B); else MVN=(mvn -B -o); fi
CORPUS_DIR="${CORPUS_DIR:-$(cd "$ROOT_DIR/.." && pwd)/docs/model}"

command -v mvn >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 mvn：需要 Maven 3.9+ 与 JDK21"; exit 2; }
[ -f "$CORPUS_DIR/data-a.json" ] || { echo "[阻塞] 语料目录不存在: ${CORPUS_DIR}（可用 CORPUS_DIR=<目录> 指定）"; exit 2; }

ROUNDS="${SOAK_ROUNDS:-3}"
MODULE="groma-studio/groma-compiler-hotswap"
FAILED=0
RUN_START=$SECONDS

for r in $(seq 1 "$ROUNDS"); do
    ROUND_START=$SECONDS
    echo "[soak] 第 $r/$ROUNDS 轮开始"
    if "${MVN[@]}" test -pl "$MODULE" -am \
            -Dtest='RealCorpusEndToEndTest#realCorpusParsesGeneratesCompilesAndRuns' \
            -Dsurefire.failIfNoSpecifiedTests=false \
            "-Dgroma.corpus=$CORPUS_DIR" >/dev/null 2>&1; then
        echo "PASS soak-round-$r 用时 $((SECONDS - ROUND_START))s"
    else
        echo "FAIL soak-round-${r}（去掉 >/dev/null 重跑定位，surefire 报告在 $MODULE/target/surefire-reports）"
        FAILED=$((FAILED + 1))
    fi
done

TOTAL_SECONDS=$((SECONDS - RUN_START))
echo "SUMMARY soak rounds=$ROUNDS failed=$FAILED total=${TOTAL_SECONDS}s"
[ "$FAILED" -eq 0 ]

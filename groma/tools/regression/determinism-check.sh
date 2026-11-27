#!/usr/bin/env bash
# 文件: tools/regression/determinism-check.sh
# 命令编号: CMD-NP-10 (T1.5)：同种子两次 300 拍推演，实例状态与问题列表逐一一致的确定性门禁。
#   真跑 RealCorpusEndToEndTest#realCorpusRunIsDeterministic（真实语料，非夹具）。
#
# 用法: bash tools/regression/determinism-check.sh [--help]
# 环境变量: CORPUS_DIR(默认 <仓库根>/docs/model)、JAVA_HOME、MAVEN_HOME、MAVEN_ONLINE=1 跳过 -o 离线
# 退出码: 0 = 通过；1 = 未通过；2 = 环境阻塞
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"   # groma/ 工程根
cd "$ROOT_DIR"
case "${1:-}" in -h|--help) sed -n '2,10p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac

JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
MAVEN_HOME="${MAVEN_HOME:-$HOME/tools/apache-maven-3.9.9}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
if [ "${MAVEN_ONLINE:-0}" = "1" ]; then MVN=(mvn -B); else MVN=(mvn -B -o); fi
CORPUS_DIR="${CORPUS_DIR:-$(cd "$ROOT_DIR/.." && pwd)/docs/model}"

command -v mvn >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 mvn：需要 Maven 3.9+ 与 JDK21"; exit 2; }
[ -f "$CORPUS_DIR/data-a.json" ] || { echo "[阻塞] 语料目录不存在: ${CORPUS_DIR}（可用 CORPUS_DIR=<目录> 指定）"; exit 2; }

MODULE="groma-studio/groma-compiler-hotswap"
echo "[determinism-check] 语料目录: $CORPUS_DIR"
echo "[determinism-check] mvn ${MVN[*]} test -pl $MODULE -am -Dtest=RealCorpusEndToEndTest#realCorpusRunIsDeterministic"

if "${MVN[@]}" test -pl "$MODULE" -am \
        -Dtest='RealCorpusEndToEndTest#realCorpusRunIsDeterministic' \
        -Dsurefire.failIfNoSpecifiedTests=false \
        "-Dgroma.corpus=$CORPUS_DIR"; then
    echo "PASS [determinism-check] 同种子两次 300 拍实例状态与问题列表完全一致（墙钟戳成员除外）"
    exit 0
else
    echo "FAIL [determinism-check] 确定性回归分叉，见上方 surefire 输出"
    exit 1
fi

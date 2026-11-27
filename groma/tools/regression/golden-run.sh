#!/usr/bin/env bash
# 文件: tools/regression/golden-run.sh
# 命令编号: CMD-NP-06：黄金回归集 R1~R14。逐 case 执行真实语料端到端（解析→出码→ECJ 编译→挂载→推演），
#   每个 case 记录名称与种子并输出 PASS/FAIL 结果行。
#
# 用法: bash tools/regression/golden-run.sh [--help]
#   GOLDEN_CASES=R1,R12 只跑指定 case（逗号分隔 caseId），默认全部。
#
# 环境变量: CORPUS_DIR、JAVA_HOME、MAVEN_HOME、MAVEN_ONLINE=1 跳过 -o 离线
# 退出码: 0 = 全部通过；1 = 有 case 未通过；2 = 环境阻塞
#
# 待扩展：golden-cases.json 的 seed/targetTicks/reloadAtTick 尚未接入测试（RealCorpusEndToEndTest
#   暂不支持 -Dseed 等系统属性），当前每个 case 以默认语料与默认种子跑同一 e2e 并记录 case 名；
#   待测试补齐 -Dseed/-DtargetTicks 注入后，把下面 CASE_PROPS 处替换为逐 case 传参。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"   # groma/ 工程根
cd "$ROOT_DIR"
case "${1:-}" in -h|--help) sed -n '2,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac

JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
MAVEN_HOME="${MAVEN_HOME:-$HOME/tools/apache-maven-3.9.9}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
if [ "${MAVEN_ONLINE:-0}" = "1" ]; then MVN=(mvn -B); else MVN=(mvn -B -o); fi
CORPUS_DIR="${CORPUS_DIR:-$(cd "$ROOT_DIR/.." && pwd)/docs/model}"

command -v mvn >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 mvn：需要 Maven 3.9+ 与 JDK21"; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 python3：用于解析 golden-cases.json"; exit 2; }
[ -f "$CORPUS_DIR/data-a.json" ] || { echo "[阻塞] 语料目录不存在: ${CORPUS_DIR}（可用 CORPUS_DIR=<目录> 指定）"; exit 2; }

CASES_FILE="$SCRIPT_DIR/golden-cases.json"
[ -f "$CASES_FILE" ] || { echo "[阻塞] 缺少 $CASES_FILE"; exit 2; }
MODULE="groma-studio/groma-compiler-hotswap"
FILTER="${GOLDEN_CASES:-}"

FAILED=0
TOTAL=0
# 逐 case 输出 caseId|名称|种子，管道逐行消费（python3 -I 隔离模式防注入）
while IFS='|' read -r CASE_ID CASE_NAME CASE_SEED; do
    [ -n "$CASE_ID" ] || continue
    if [ -n "$FILTER" ]; then
        case ",$FILTER," in *",$CASE_ID,"*) ;; *) continue ;; esac
    fi
    TOTAL=$((TOTAL + 1))
    echo "[golden-run] case ${CASE_ID}「${CASE_NAME}」seed=$CASE_SEED"
    # CASE_PROPS: 待测试支持 -Dseed 后在此追加 "-Dseed=$CASE_SEED" -DtargetTicks=... 等
    if "${MVN[@]}" test -pl "$MODULE" -am \
            -Dtest='RealCorpusEndToEndTest#realCorpusParsesGeneratesCompilesAndRuns' \
            -Dsurefire.failIfNoSpecifiedTests=false \
            "-Dgroma.corpus=$CORPUS_DIR" >/dev/null 2>&1; then
        echo "PASS golden-$CASE_ID $CASE_NAME"
    else
        echo "FAIL golden-$CASE_ID ${CASE_NAME}（详见上方被折叠的 surefire 输出，去掉 >/dev/null 重跑定位）"
        FAILED=$((FAILED + 1))
    fi
done < <(python3 -I -c '
import json, sys
cases = json.load(open(sys.argv[1]))
for c in cases:
    print("|".join([str(c.get("caseId","")), str(c.get("name","")), str(c.get("seed",""))]))
' "$CASES_FILE")

echo "SUMMARY golden-run total=$TOTAL failed=$FAILED"
[ "$FAILED" -eq 0 ]

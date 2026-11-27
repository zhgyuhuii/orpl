#!/usr/bin/env bash
# 文件: tools/regression/corpus-check.sh
# 命令编号: CMD-NP-10 (H-10) 真门禁：对真实语料完整执行六步流水线
#   拿回来 → 解析 → 五层出码 → ECJ 编译 → 隔离类加载热挂载 → 推演 300 步，
#   并核验产物 target/real-corpus-warnings.txt 与 target/real-corpus-result.txt 真实落盘。
#
# 用法: bash tools/regression/corpus-check.sh [--help]
#
# 环境变量:
#   CORPUS_DIR     语料目录（含 data-a.json/data-b.json），默认 <仓库根>/docs/model
#   JAVA_HOME      JDK21 路径（默认 /opt/homebrew/opt/openjdk@21/...，CI 可覆盖）
#   MAVEN_HOME     Maven 目录（默认 $HOME/tools/apache-maven-3.9.9，CI 可覆盖）
#   MAVEN_ONLINE   置 1 时去掉 -o 在线解析依赖（CI 冷缓存首跑用）
#
# 退出码: 0 = 通过；1 = 未通过；2 = 环境阻塞
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common-harness.sh"

case "${1:-}" in
    -h|--help) sed -n '2,21p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
esac

require_cmd java "需要 JDK 21 运行语料端到端编译器"
require_cmd mvn "需要 Maven 3.9+ 执行语料端到端门禁"
[ -f "$CORPUS_DIR/data-a.json" ] && [ -f "$CORPUS_DIR/data-b.json" ] \
    || block_exit "语料目录 $CORPUS_DIR 缺少 data-a.json/data-b.json（可用 CORPUS_DIR=<目录> 指定）"

MODULE="groma-studio/groma-compiler-hotswap"
echo "[corpus-check] 语料目录: $CORPUS_DIR"
echo "[corpus-check] mvn ${MVN[*]} test -pl $MODULE -am -Dtest=RealCorpusEndToEndTest"

# -am 让反应堆内直接构建上游依赖模块（无需事先 install 到本地仓库）；
# -Dsurefire.failIfNoSpecifiedTests=false 容忍上游模块无匹配测试类。
if "${MVN[@]}" test -pl "$MODULE" -am \
        -Dtest='RealCorpusEndToEndTest' \
        -Dsurefire.failIfNoSpecifiedTests=false \
        "-Dgroma.corpus=$CORPUS_DIR"; then
    record_pass "CORPUS-E2E" "六步流水线(导入->解析->五层出码->ECJ编译->沙盒挂载->推进300步) 真跑通过" "ok"
else
    record_fail "CORPUS-E2E" "语料端到端测试失败（见上方 surefire 输出）" "error"
    finish_summary
fi

WARN_FILE="$WORKSPACE_DIR/$MODULE/target/real-corpus-warnings.txt"
RESULT_FILE="$WORKSPACE_DIR/$MODULE/target/real-corpus-result.txt"
if [ -s "$WARN_FILE" ]; then
    record_pass "CORPUS-ARTIFACT" "解析告警报告已落盘" "$WARN_FILE"
else
    record_fail "CORPUS-ARTIFACT" "缺少产物 real-corpus-warnings.txt" "$WARN_FILE"
fi
if [ -s "$RESULT_FILE" ]; then
    record_pass "CORPUS-ARTIFACT" "推演结果报告已落盘" "$RESULT_FILE"
    echo "[corpus-check] 结果摘要: $(head -1 "$RESULT_FILE")"
else
    record_fail "CORPUS-ARTIFACT" "缺少产物 real-corpus-result.txt" "$RESULT_FILE"
fi

finish_summary

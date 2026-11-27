#!/usr/bin/env bash
# 文件: tools/regression/clean-env-build.sh
# 命令编号: CMD-NP-05：洁净环境构建——删除全部 target 后全量 install（跳测试）+ 全量 test +
#   红线扫描，验证工程可从零重建（无残留产物依赖），并复核 NP-R01~R10 源码红线。
#
# 用法: bash tools/regression/clean-env-build.sh [--help]
# 环境变量: JAVA_HOME、MAVEN_HOME、MAVEN_ONLINE=1 跳过 -o 离线（CI 冷缓存首跑用）
# 退出码: 0 = 通过；1 = 未通过；2 = 环境阻塞
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"   # groma/ 工程根
cd "$ROOT_DIR"
case "${1:-}" in -h|--help) sed -n '2,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac

JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
MAVEN_HOME="${MAVEN_HOME:-$HOME/tools/apache-maven-3.9.9}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
if [ "${MAVEN_ONLINE:-0}" = "1" ]; then MVN=(mvn -B); else MVN=(mvn -B -o); fi

command -v mvn >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 mvn：需要 Maven 3.9+ 与 JDK21"; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "[阻塞] 环境缺少 python3：红线扫描需要"; exit 2; }

echo "[clean-env-build] 删除全部 target 目录"
find "$ROOT_DIR" -type d -name target -prune -exec rm -rf {} +

START=$SECONDS
echo "[clean-env-build] mvn ${MVN[*]} install -DskipTests（全量构建）"
"${MVN[@]}" install -DskipTests || { echo "FAIL clean-env-build 全量 install 失败"; exit 1; }

echo "[clean-env-build] mvn ${MVN[*]} test（全量测试）"
"${MVN[@]}" test || { echo "FAIL clean-env-build 全量 test 失败"; exit 1; }

echo "[clean-env-build] python3 -I tools/regression/scan.py all .（红线扫描）"
python3 -I tools/regression/scan.py all . || { echo "FAIL clean-env-build 红线扫描未通过"; exit 1; }

echo "PASS clean-env-build 洁净重建 + 全量测试 + 红线扫描通过，用时 $((SECONDS - START))s"
exit 0

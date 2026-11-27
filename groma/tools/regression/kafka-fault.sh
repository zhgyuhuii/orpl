#!/usr/bin/env bash
# 文件: tools/regression/kafka-fault.sh
# 命令编号: CMD-NP-11：持久层故障注入——Kafka 生产者重试、不可变批次与死信队列不丢数验证。
#
# 现状：整个 groma 工程（所有 pom.xml 与源码）不依赖 Kafka，持久层为 JDBC/文件批次落库
#   （groma-base-connector，见 ConnectorSecurityAndBatchPersistTest）。没有 Kafka 客户端
#   就没有可注入的故障面，本脚本按约定打印 SKIP 并以 0 退出；工程引入 spring-kafka 后，
#   在下方 TODO 处接入嵌入式 Kafka（Testcontainers）+ 重试/死信断言，替换 SKIP 分支。
#
# 用法: bash tools/regression/kafka-fault.sh [--help]
# 退出码: 0 = 通过或 SKIP；1 = 未通过；2 = 环境阻塞
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"   # groma/ 工程根
cd "$ROOT_DIR"
case "${1:-}" in -h|--help) sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;; esac

if grep -rqiE --include='pom.xml' '<artifactId>[^<]*kafka' "$ROOT_DIR" 2>/dev/null \
   || grep -rqiE --include='*.java' 'org\.apache\.kafka|spring-kafka' "$ROOT_DIR" 2>/dev/null; then
    # TODO: 引入 Kafka 后接入故障注入：
    #   mvn -B -o test -pl groma-base/groma-base-connector -Dtest=KafkaFaultInjectionTest
    echo "[kafka-fault] 检测到 Kafka 依赖，但故障注入用例尚未建设（KafkaFaultInjectionTest 不存在）"
    exit 2
fi

echo "SKIP kafka-fault.sh：工程无 Kafka 依赖（持久层为 JDBC/文件批次，无死信队列可注入），无故障注入对象"
exit 0

#!/usr/bin/env bash
# 文件: tools/regression/export-build.sh
# 命令编号: CMD-NP-15 (H-15) | 在仅含 JDK 21 与 Maven 的干净容器环境执行 mvn clean package（落实 NP-BLD-002）
source "$(dirname "${BASH_SOURCE[0]}")/common-harness.sh"

require_cmd docker "需要 Docker 启动干净的 JDK 21 + Maven 容器验证独立构建"
DOCKER_IMAGE="${MAVEN_JDK21_IMAGE:-maven:3.9.9-eclipse-temurin-21}"

if docker run --rm \
    -v "$WORKSPACE_DIR":/workspace:ro \
    "$DOCKER_IMAGE" \
    bash -c "cp -r /workspace /tmp/clean-build && cd /tmp/clean-build && mvn -B clean package -DskipTests" \
    >"$OUT_DIR/docker-clean-package.log" 2>&1; then
    record_pass "EXPORT-CLEAN-BUILD" "导出工程在干净 JDK 21 容器中 mvn clean package 成功" "BUILD_SUCCESS"
else
    record_fail "EXPORT-CLEAN-BUILD" "干净容器独立构建失败" "$OUT_DIR/docker-clean-package.log"
fi

finish_summary

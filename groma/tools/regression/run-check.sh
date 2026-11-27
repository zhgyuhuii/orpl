#!/usr/bin/env bash
# 文件: tools/regression/run-check.sh (对应 CMD-NP-15：总控一键验收门禁入口，缺失任一必交件返回退出码 2)
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

REQUIRED_DOCS=(
  "docs/validation-rules.md"
  "docs/shapes-and-fields-dict.md"
  "docs/thread-model.md"
  "docs/ws-protocol.md"
  "docs/redis-keys-ttl.md"
  "docs/tests.md"
  "docs/gateway-handoff.md"
)

for doc in "${REQUIRED_DOCS[@]}"; do
  if [[ ! -f "${ROOT_DIR}/${doc}" ]]; then
    echo "[BLOCKED] 缺失必交评审文档: ${doc}" >&2
    exit 2
  fi
done

python3 "${ROOT_DIR}/tools/regression/scan.py" all "${ROOT_DIR}"
bash "${ROOT_DIR}/tools/regression/golden-run.sh"
bash "${ROOT_DIR}/tools/regression/determinism-check.sh"
bash "${ROOT_DIR}/tools/regression/reload-check.sh"
bash "${ROOT_DIR}/tools/regression/slow-client.sh"
bash "${ROOT_DIR}/tools/regression/soak.sh"
bash "${ROOT_DIR}/tools/regression/kafka-fault.sh"
bash "${ROOT_DIR}/tools/regression/mcp-smoke.sh"

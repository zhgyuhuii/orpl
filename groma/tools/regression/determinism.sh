#!/usr/bin/env bash
# 文件: tools/regression/determinism.sh
# 命令编号: CMD-NP-06 (H-02) | 用法: determinism.sh <模型编码> <种子> <步数> <次数>
source "$(dirname "${BASH_SOURCE[0]}")/common-harness.sh"

MODEL_CODE="${1:-emergency-deduction}"
SEED="${2:-20261124}"
STEPS="${3:-10000}"
RUNS="${4:-3}"
SCENE_CODE="${SCENE_CODE:-flood_scene}"

require_service_up
require_cmd sha256sum "需要 sha256sum 计算每轮运行快照序列摘要"

BASE_HASH=""
for (( i=1; i<=RUNS; i++ )); do
    SID="$(curl -fsS -X POST "$ODS_BASE_URL/api/v1/models/$MODEL_CODE/scenes/$SCENE_CODE/sessions" \
        -H "Content-Type: application/json" \
        -d "{\"seed\":$SEED,\"startPaused\":true}" | python3 -c "import sys,json; print(json.load(sys.stdin)['data'])")"

    SNAP_JSON="$(curl -fsS -X POST "$ODS_BASE_URL/api/v1/models/$MODEL_CODE/scenes/$SCENE_CODE/sessions/$SID/step?steps=$STEPS")"
    echo "$SNAP_JSON" > "$OUT_DIR/run_${i}.json"
    curl -fsS -X DELETE "$ODS_BASE_URL/api/v1/models/$MODEL_CODE/scenes/$SCENE_CODE/sessions/$SID" >/dev/null

    # 提取与会话随机 ID 无关的纯世界状态计算摘要
    CUR_HASH="$(python3 -c "
import json, hashlib, sys
d = json.load(open('$OUT_DIR/run_${i}.json'))['data']
payload = {'tick': d['tickIndex'], 'worldTimeMs': d['worldTimeMs'], 'instances': d['instances']}
print(hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest())
")"
    if [ -z "$BASE_HASH" ]; then
        BASE_HASH="$CUR_HASH"
        record_pass "DET-RUN-$i" "基线运行完成 ($STEPS 步)" "$CUR_HASH"
    elif [ "$CUR_HASH" = "$BASE_HASH" ]; then
        record_pass "DET-RUN-$i" "第 $i 次运行快照摘要与基线完全一致" "$CUR_HASH"
    else
        record_fail "DET-RUN-$i" "快照摘要发生分叉漂移！" "expected=$BASE_HASH,actual=$CUR_HASH"
    fi
done

finish_summary

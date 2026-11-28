#!/usr/bin/env bash
# 文件: tools/regression/host-lifecycle-smoke.sh
# 真机全生命期冒烟（code4.html 收割轮收口验收），按模型暴露面分三段：
#   A. 热挂载出码：语料上传 → 六步流水线（解析→校验→出码→落盘→ECJ 热挂载→javac 对拍）→ 版本化 modelCode
#   B. 热挂载推演：MCP streamable-http（/mcp）sim.start/sim.step/sim.stop 全流程 + WS 频道步进帧
#   C. 合包 REST：repo-models-root 同步（E2）→ -Pbundled-models 打包 → REST 场景控制器全生命期
#      （开会话→推步→快照→暂停/倍速/恢复→存档回收→存档列表）
# 用法: host-lifecycle-smoke.sh <dataA绝对路径> <dataB绝对路径> [port]
set -euo pipefail

DATA_A="${1:?用法: host-lifecycle-smoke.sh <dataA绝对路径> <dataB绝对路径> [port]}"
DATA_B="${2:?缺 dataB}"
PORT="${3:-18282}"
BASE="http://127.0.0.1:${PORT}"
GROMA_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JAR="${GROMA_ROOT}/groma-bootstrap/target/groma-bootstrap-1.0.0.jar"
JAVA_HOME_BIN="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
JAVA_BIN="${JAVA_HOME_BIN}/bin/java"
MVN="${MVN:-$HOME/tools/apache-maven-3.9.9/bin/mvn}"
WORKDIR="$(mktemp -d /tmp/groma-host-smoke.XXXXXX)"
LOG="${WORKDIR}/host.log"
HOST_PID=""

cleanup() {
  if [ -n "${HOST_PID}" ] && kill -0 "${HOST_PID}" 2>/dev/null; then
    kill "${HOST_PID}" 2>/dev/null || true
    wait "${HOST_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

say() { printf '\n===== %s =====\n' "$*"; }
die() { echo "✗ $*"; tail -40 "${LOG}" 2>/dev/null; exit 1; }

jqres() { # jqres '<python 表达式，d 为 Result 信封>' <<< "$json"
  python3 -I -c "
import json, sys
d = json.load(sys.stdin)
if not d.get('success'):
    print('RESULT_FAIL: ' + str(d.get('code')) + ' ' + str(d.get('message'))[:300], file=sys.stderr)
    sys.exit(3)
print($1)
"
}

start_host() {
  "${JAVA_BIN}" -jar "${JAR}" > "${LOG}" 2>&1 &
  HOST_PID=$!
  for i in $(seq 1 90); do
    if curl -sf "${BASE}/actuator/health" >/dev/null 2>&1; then echo "宿主就绪 pid=${HOST_PID}"; return 0; fi
    [ "$i" = 90 ] && die "宿主 90s 未就绪"
    sleep 1
  done
}

stop_host() {
  if [ -n "${HOST_PID}" ] && kill -0 "${HOST_PID}" 2>/dev/null; then
    kill "${HOST_PID}" 2>/dev/null || true
    wait "${HOST_PID}" 2>/dev/null || true
    HOST_PID=""
  fi
  sleep 1
}

say "A1. 构建宿主 fat jar"
cd "${GROMA_ROOT}"
JAVA_HOME="${JAVA_HOME_BIN}" "${MVN}" -o -q -f pom.xml package -pl groma-bootstrap -am \
  -DskipTests -Dsurefire.failIfNoSpecifiedTests=false
[ -f "${JAR}" ] || die "fat jar 未生成"

say "A2. 启动宿主（热挂载形态；仓内合包同步 + 语料上限上调）"
GROMA_SERVER_PORT="${PORT}" \
GROMA_PIPELINE_REPO_MODELS_ROOT="${GROMA_ROOT}/groma-models" \
GROMA_UPLOAD_MAX_FILE="${GROMA_UPLOAD_MAX_FILE:-64MB}" \
GROMA_UPLOAD_MAX_REQUEST="${GROMA_UPLOAD_MAX_REQUEST:-96MB}" \
  start_host

say "A3. 上传语料 → 六步流水线 → 版本化 modelCode"
BUILD=$(curl -sf -X POST "${BASE}/api/v1/studio/models/upload" \
  -F "dataA=@${DATA_A}" -F "dataB=@${DATA_B}") || die "上传请求失败"
MODEL_CODE=$(jqres "d['data']['modelCode']" <<< "${BUILD}") || die "构建失败（见上方 Result.fail）"
JAVAC_NOTE=$(jqres "d['data']['javacNote']" <<< "${BUILD}")
echo "modelCode = ${MODEL_CODE}"
echo "javacNote = ${JAVAC_NOTE}"
case "${MODEL_CODE}" in
  *-????????) echo "✓ 编码带语料短哈希后缀（多版本并存收口）" ;;
  *) die "编码缺短哈希后缀: ${MODEL_CODE}" ;;
esac
jqres "str(d['data']['sourceFileCount']) + ' 个源文件, 重编=' + str(d['data']['recompiled'])" <<< "${BUILD}"

say "B1. MCP streamable-http 握手（/mcp）"
MCP_SESSION=$(curl -sf -D - -o /dev/null -X POST "${BASE}/mcp" \
  -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"host-smoke","version":"1.0"}}}' \
  | LC_ALL=C grep -i "^mcp-session-id:" | tr -d '\r' | awk '{print $2}')
[ -n "${MCP_SESSION}" ] || die "MCP 会话头未获取"
curl -sf -X POST "${BASE}/mcp" -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' -H "mcp-session-id: ${MCP_SESSION}" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' > /dev/null
echo "mcp-session-id = ${MCP_SESSION}"

# 从 MCP SSE 响应提取 McpToolEnvelope.data 字段：mcp_data '<表达式，d 为 envelope.data 字典>' <<< "$响应"
mcp_data() {
  python3 -I -c "
import json, sys
raw = sys.stdin.read()
line = [l[5:] for l in raw.splitlines() if l.startswith('data:')][-1] if any(l.startswith('data:') for l in raw.splitlines()) else raw
d = json.loads(line)
env_text = d['result']['content'][0]['text']
env = json.loads(env_text)
if not env.get('isError') == False or env.get('errorCode'):
    print('TOOL_FAIL: ' + str(env.get('errorCode')) + ' ' + str(env.get('message'))[:300], file=sys.stderr)
    sys.exit(3)
d = env
print($1)
"
}

mcp_call() { # mcp_call <tool> <args-json>
  curl -sf -X POST "${BASE}/mcp" \
    -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
    -H "mcp-session-id: ${MCP_SESSION}" \
    -d "{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"tools/call\",\"params\":{\"name\":\"$1\",\"arguments\":$2}}"
}

say "B2. MCP sim.start（热挂载模型，seed=42）→ sim.step 60"
STARTED=$(mcp_call "sim.start" "{\"modelCode\":\"${MODEL_CODE}\",\"sceneCode\":\"scene1\",\"seed\":42}") \
  || die "MCP sim.start 请求失败"
SID=$(mcp_data "d['data']['sessionId']" <<< "${STARTED}") || { echo "${STARTED}" | head -c 600; die "sim.start 失败"; }
echo "sessionId = ${SID}"

STEPPED=$(mcp_call "sim.step" "{\"sessionId\":\"${SID}\",\"steps\":60}") || die "MCP sim.step 请求失败"
mcp_data "'✓ MCP 推步完成: tick=' + str(d['data']['tickIndex']) + ', 实例=' + str(d['data']['instanceCount']) + ', 在途过程=' + str(d['data']['activeProcessCount'])" <<< "${STEPPED}" \
  || { echo "${STEPPED}" | head -c 600; die "sim.step 失败"; }

say "B3. WS 频道步进帧（/ws/groma/${MODEL_CODE}/scene1/${SID}）"
python3 -I - "${PORT}" "${MODEL_CODE}" "${SID}" <<'PYEOF'
import asyncio, json, os, sys
for key in [k for k in os.environ if "proxy" in k.lower()]:
    del os.environ[key]  # 本机回环直连：清掉环境代理，防 localhost 被劫持进 SOCKS
import websockets

async def main():
    port, model, sid = sys.argv[1], sys.argv[2], sys.argv[3]
    uri = f"ws://127.0.0.1:{port}/ws/groma/{model}/scene1/{sid}"
    async with websockets.connect(uri, proxy=None) as ws:  # 显式禁代理：macOS 系统级 SOCKS 会被 getproxies 拾取
        await ws.send("CTL:G:3")
        for _ in range(10):
            frame = await asyncio.wait_for(ws.recv(), timeout=10)
            if frame.startswith("TP:"):
                data = json.loads(frame[3:])
                tick = data.get("tick")
                assert tick is not None and tick >= 60, "WS 帧应携带推演后的 tick: " + str(tick)
                print(f"✓ WS 下行 TP 帧: tick={tick} instances={len(data.get('instances', []))} status={data.get('status')}")
                return
        raise AssertionError("10 帧内未收到 TP 态势帧")

asyncio.run(main())
PYEOF

say "B4. MCP sim.stop"
STOPPED=$(mcp_call "sim.stop" "{\"sessionId\":\"${SID}\"}") || die "MCP sim.stop 请求失败"
mcp_data "'✓ 会话已停止（sim.stop）'" <<< "${STOPPED}"

say "C1. 仓内合包同步核验（E2：模块镜像 + SPI + 聚合 pom 清单）"
python3 -I - "${GROMA_ROOT}" "${MODEL_CODE}" <<'PYEOF'
import os, sys
root, model_code = sys.argv[1], sys.argv[2]
gmodels = os.path.join(root, "groma-models")
dirs = sorted(d for d in os.listdir(gmodels) if d.startswith("model-") and os.path.isdir(os.path.join(gmodels, d)))
assert dirs, "仓内应有同步的生成模型目录"
target = [d for d in dirs if model_code in d]
assert target, f"应有 {model_code} 的镜像目录，实际 {dirs}"
t = os.path.join(gmodels, target[0])
assert os.path.isfile(os.path.join(t, "pom.xml")), "镜像模块应有 pom.xml"
spi = os.path.join(t, "src/main/resources/META-INF/services/com.groma.base.engine.GromaModuleRuntime")
assert os.path.isfile(spi), "镜像模块应有 SPI 注册文件（合包 ServiceLoader 依赖）"
java = sum(len(fs) for _, _, fs in os.walk(os.path.join(t, "src")))
assert java >= 200, "镜像源码规模应与生成一致: " + str(java)
pom = open(os.path.join(gmodels, "pom.xml"), encoding="utf-8").read()
assert target[0] in pom, "聚合 pom modules 应包含 " + target[0]
print(f"✓ 仓内合包同步: {target[0]}（{java} 源文件 + SPI）+ pom 清单已登记")
PYEOF

say "C2. 停热挂载宿主 → -Pbundled-models 合包构建 → 启动合包宿主"
stop_host
JAVA_HOME="${JAVA_HOME_BIN}" "${MVN}" -o -q -f pom.xml package -pl groma-bootstrap -am -Pbundled-models \
  -DskipTests -Dsurefire.failIfNoSpecifiedTests=false || die "合包构建失败"
GROMA_SERVER_PORT="${PORT}" start_host

say "C3. 合包 REST：开会话（StartDTO 线格式）→ 推步 60 → 快照画像"
SCENE_URL="${BASE}/api/v1/models/${MODEL_CODE}/scenes/scene1"
SESSION=$(curl -sf -X POST "${SCENE_URL}/sessions" -H 'Content-Type: application/json' \
  -d '{"seed": 42, "startPaused": false}') || die "REST 会话请求失败"
SID2=$(jqres "d['data']" <<< "${SESSION}") || { echo "${SESSION}" | head -c 400; die "REST 开会话失败"; }
echo "sessionId = ${SID2}"
curl -sf -X POST "${SCENE_URL}/sessions/${SID2}/step?steps=60" > /dev/null || die "REST 推步失败"
SNAP=$(curl -sf "${SCENE_URL}/sessions/${SID2}/snapshot") || die "REST 快照失败"
python3 -I - "${SNAP}" <<'PYEOF'
import json, sys
d = json.loads(sys.argv[1])
assert d["success"], d
s = d["data"]
assert s["tickIndex"] == 60, "应精确推进到 60 拍: " + str(s["tickIndex"])
assert len(s["instances"]) > 0, "快照应有实例"
print(f"✓ REST 快照: tick=60 实例={len(s['instances'])} 近期事件={len(s.get('recentEvents', []))}")
PYEOF

say "C4. 合包 REST：暂停/倍速/恢复 + 存档回收 + 存档列表"
curl -sf -X POST "${SCENE_URL}/sessions/${SID2}/pause" > /dev/null
curl -sf -X POST "${SCENE_URL}/sessions/${SID2}/speed/2.0" > /dev/null
curl -sf -X POST "${SCENE_URL}/sessions/${SID2}/resume" > /dev/null
curl -sf "${SCENE_URL}/sessions/${SID2}/snapshot" > /dev/null          # 触发 SnapshotRepository.save（快照端点为 GET）
curl -sf -X POST "${BASE}/api/v1/studio/archives/sessions/${SID2}/evict" | head -c 200; echo
curl -sf "${BASE}/api/v1/studio/archives" | jqres "'✓ 存档落盘 ' + str(len(d['data'])) + ' 份'"

say "C5. 宿主日志问题扫描 + 关停"
if LC_ALL=C grep -q "MODEL_VALIDATION_FAILED\|Exception encountered during context initialization" "${LOG}"; then
  die "宿主日志出现校验失败/上下文初始化异常"
fi
stop_host
echo
echo "✓✓ 真机全生命期冒烟通过：A 热挂载出码(版本化编码) / B MCP 推演+WS 帧 / C 合包 REST 全链路 + E2 仓内同步"

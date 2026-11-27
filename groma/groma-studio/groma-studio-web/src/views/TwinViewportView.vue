<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue';
import Twin3dViewport from '../components/Twin3dViewport.vue';
import { useTwinViewportStore } from '../stores/twinViewportStore';
import { fetchActiveSessions } from '../api/studioApi';
import { resolveWsBaseUrl } from '../utils/runtimeEnv';

const twinStore = useTwinViewportStore();

const modelCode = ref('demo-model');
const sceneCode = ref('scene1');
const sessionId = ref('session-demo-001');
const sessions = ref<ReadonlyArray<{ readonly sessionId: string; readonly tickIndex: number }>>([]);
const sessionLoadNote = ref('');

const SPEED_LADDER = [0.5, 1, 2, 4, 8] as const;

function connectSituationRoom(): void {
  twinStore.connectSituationRoom(
    resolveWsBaseUrl(),
    modelCode.value.trim() || 'demo-model',
    sceneCode.value.trim() || 'scene1',
    sessionId.value.trim() || 'session-demo-001'
  );
}

function disconnectSituationRoom(): void {
  twinStore.disconnectSituationRoom();
}

function sendControlFrame(frame: string): void {
  twinStore.sendControlFrame(frame);
}

function cycleSpeed(): void {
  const current = twinStore.latestSnapshot?.speed ?? 1;
  const next = SPEED_LADDER.find((speed) => speed > current) ?? SPEED_LADDER[0];
  twinStore.sendControlFrame(`SP:${next}`);
}

async function loadSessions(): Promise<void> {
  try {
    const summaries = await fetchActiveSessions();
    sessions.value = summaries.map((item) => ({
      sessionId: item.sessionId,
      tickIndex: item.tickIndex
    }));
    sessionLoadNote.value = `后端共 ${sessions.value.length} 个活跃会话`;
  } catch {
    sessions.value = [];
    sessionLoadNote.value = '后端会话接口未就绪（或需先创建会话）';
  }
}

const instanceCount = computed(() => Object.keys(twinStore.latestSnapshot?.instances ?? {}).length);

onUnmounted(() => {
  twinStore.disconnectSituationRoom();
});
</script>

<template>
  <div class="twin-page">
    <aside class="twin-panel">
      <h3 class="twin-panel-title">态势频道（/ws/groma）</h3>
      <label class="twin-field">
        modelCode
        <input v-model="modelCode" class="twin-input" placeholder="demo-model" />
      </label>
      <label class="twin-field">
        sceneCode
        <input v-model="sceneCode" class="twin-input" placeholder="scene1" />
      </label>
      <label class="twin-field">
        sessionId
        <input v-model="sessionId" class="twin-input" placeholder="session-demo-001" />
      </label>
      <div class="twin-actions">
        <button type="button" class="twin-btn" @click="connectSituationRoom">连接频道</button>
        <button type="button" class="twin-btn" @click="disconnectSituationRoom">断开</button>
      </div>

      <h3 class="twin-panel-title">控制指令</h3>
      <div class="twin-actions">
        <button type="button" class="twin-btn" @click="sendControlFrame('CTL:P')">CTL:P 暂停</button>
        <button type="button" class="twin-btn" @click="sendControlFrame('CTL:R')">CTL:R 恢复</button>
        <button type="button" class="twin-btn" @click="sendControlFrame('CTL:S')">CTL:S 终止</button>
        <button type="button" class="twin-btn" @click="sendControlFrame('CTL:G:1')">CTL:G:1 单步</button>
        <button type="button" class="twin-btn" @click="cycleSpeed">倍速 SP:D/N/H</button>
      </div>

      <h3 class="twin-panel-title">会话（REST /api/v1/studio/sessions）</h3>
      <button type="button" class="twin-btn" @click="loadSessions">拉取活跃会话</button>
      <p class="twin-note">{{ sessionLoadNote }}</p>
      <ul class="twin-session-list">
        <li v-for="item in sessions" :key="item.sessionId">
          {{ item.sessionId }} · 第 {{ item.tickIndex }} 拍
        </li>
      </ul>
    </aside>

    <section class="twin-main">
      <header class="twin-statusbar">
        <span>tick: {{ twinStore.latestSnapshot?.tick ?? 0 }}</span>
        <span>worldTimeMs: {{ twinStore.latestSnapshot?.worldTimeMs ?? 0 }}</span>
        <span>speed: {{ twinStore.latestSnapshot?.speed ?? 1 }}</span>
        <span>status: {{ twinStore.latestSnapshot?.status ?? 'IDLE' }}</span>
        <span>instances: {{ instanceCount }}</span>
        <span>聚焦实体: {{ twinStore.focused3dEntityId || '全局' }}</span>
      </header>
      <div class="twin-viewport-slot">
        <Twin3dViewport />
      </div>
      <p class="twin-note twin-note--foot">
        断点命中时（源码页连接 /ws/debug 后）暂停帧的 highlighted3dEntityId 会自动聚焦本视口实体并推近相机。
      </p>
    </section>
  </div>
</template>

<style scoped>
.twin-page {
  display: flex;
  height: 100%;
  min-height: 0;
}

.twin-panel {
  flex: 0 0 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-right: 1px solid #e5e6eb;
  background: #fafbfc;
}

.twin-panel-title {
  margin: 6px 0 0;
  font-size: 13px;
  color: #4e5969;
}

.twin-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #4e5969;
}

.twin-input {
  padding: 5px 8px;
  border: 1px solid #d0d5dd;
  border-radius: 4px;
  font-size: 13px;
}

.twin-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.twin-btn {
  padding: 4px 10px;
  border: 1px solid #3f5d8f;
  border-radius: 4px;
  background: #fff;
  color: #3f5d8f;
  cursor: pointer;
  font-size: 12px;
}

.twin-btn:hover {
  background: #3f5d8f;
  color: #fff;
}

.twin-note {
  margin: 0;
  font-size: 12px;
  color: #86909c;
}

.twin-note--foot {
  margin: 8px 16px;
}

.twin-session-list {
  margin: 4px 0 0;
  padding: 0 0 0 16px;
  font-size: 12px;
  color: #4e5969;
  max-height: 180px;
  overflow: auto;
}

.twin-main {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.twin-statusbar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 10px 16px;
  font-size: 13px;
  color: #1f2329;
  border-bottom: 1px solid #e5e6eb;
}

.twin-viewport-slot {
  flex: 1 1 auto;
  min-height: 360px;
  margin: 12px 16px;
}
</style>

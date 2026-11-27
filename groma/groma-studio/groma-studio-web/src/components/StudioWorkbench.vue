<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useDapDebugStore } from '../stores/dapDebugStore';
import { useTwinViewportStore } from '../stores/twinViewportStore';
import { ModelEditLockManager } from '../utils/editLockManager';

const props = defineProps<{
  modelCode: string;
  sceneCode: string;
  activeSessionId: string;
}>();

const debugStore = useDapDebugStore();
const twinStore = useTwinViewportStore();
const lockManager = new ModelEditLockManager();

const readOnlyBanner = ref(false);
const highlightedCanvasCellId = ref('');
const statusClockTimer = ref<ReturnType<typeof globalThis.setInterval> | null>(null);
const localElapsedSec = ref(0);

const activeTickDisplay = computed(() => twinStore.latestSnapshot?.tick ?? 0);

watch(
  () => debugStore.pausedFrame,
  (frame) => {
    if (frame) {
      highlightedCanvasCellId.value = frame.highlightedCanvasElementId;
      twinStore.focused3dEntityId = frame.highlighted3dEntityId;
    }
  }
);

onMounted(async () => {
  try {
    const lease = await lockManager.acquireLease(props.modelCode, () => {
      readOnlyBanner.value = true;
    });
    readOnlyBanner.value = lease.readOnlyMode;
  } catch {
    readOnlyBanner.value = true;
  }

  statusClockTimer.value = globalThis.setInterval(() => {
    localElapsedSec.value += 1;
  }, 1_000);
});

onUnmounted(() => {
  // 严守 FE-R09：组件卸载时 100% 释放全部定时器与 WebSocket 会话句柄
  if (statusClockTimer.value !== null) {
    globalThis.clearInterval(statusClockTimer.value);
    statusClockTimer.value = null;
  }
  lockManager.stopRenewal();
  debugStore.disconnectDebugChannel();
  twinStore.disconnectSituationRoom();
});
</script>

<template>
  <div class="groma-workbench">
    <header class="groma-toolbar">
      <span class="badge-model">模块: model-{{ props.modelCode }}</span>
      <span v-if="readOnlyBanner" class="badge-readonly">只读保护模式</span>
      <span class="badge-tick">当前步进刻度: {{ activeTickDisplay }}</span>

      <div class="debug-actions">
        <button type="button" @click="debugStore.sendStepCommand('next')">F8 单步跳过</button>
        <button type="button" @click="debugStore.sendStepCommand('stepIn')">F7 单步进入</button>
        <button type="button" @click="debugStore.sendStepCommand('nextSimulationTick')">下一仿真步进</button>
        <button type="button" @click="debugStore.sendStepCommand('continue')">F9 继续推演</button>
      </div>
    </header>

    <main class="groma-panes">
      <section class="pane-canvas" :data-highlight-cell="highlightedCanvasCellId">
        <h3>方圆 2D 建模画布 (高亮图元: {{ highlightedCanvasCellId || '无' }})</h3>
      </section>

      <section class="pane-source">
        <h3>五层 Java 21 源码与在线断点视图</h3>
        <p v-if="debugStore.pausedFrame">
          已挂起于 {{ debugStore.pausedFrame.relativeFile }} : 第 {{ debugStore.pausedFrame.line }} 行
        </p>
      </section>

      <section class="pane-3d-viewport" :data-focus-entity="twinStore.focused3dEntityId">
        <h3>2D/3D 数字孪生态势视口 (聚焦实体: {{ twinStore.focused3dEntityId || '全局' }})</h3>
      </section>
    </main>
  </div>
</template>

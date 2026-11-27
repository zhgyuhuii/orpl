<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { EditorState, RangeSet, StateEffect, StateField } from '@codemirror/state';
import { Decoration, EditorView, GutterMarker, gutter } from '@codemirror/view';
import { useDapDebugStore } from '../stores/dapDebugStore';
import { fetchModelSource } from '../api/studioApi';
import { resolveWsBaseUrl } from '../utils/runtimeEnv';

const DEFAULT_MODEL_CODE = 'demo-model';

const DEMO_SOURCE_FILES: ReadonlyArray<string> = [
  'controller/UavController.java',
  'service/CruiseProcessService.java',
  'repository/UavStateRepository.java',
  'model/UavEntity.java',
  'client/TelemetryClient.java'
];

const dapDebug = useDapDebugStore();

const editorHost = ref<HTMLDivElement | null>(null);
const currentFile = ref<string>(DEMO_SOURCE_FILES[0]);
const sessionIdInput = ref<string>('session-demo-001');
const loadMode = ref<'backend' | 'demo' | 'idle'>('idle');
const loading = ref(false);
const feedback = ref('点击左侧文件加载五层源码；行号左侧栏点击可切换行断点');

let editorView: EditorView | null = null;

/* ---------- CodeMirror 扩展：挂起行高亮 + 行断点 gutter ---------- */

const setActiveLine = StateEffect.define<number>();

const activeLineField = StateField.define<number>({
  create: () => 0,
  update(current, tr) {
    let next = current;
    for (const effect of tr.effects) {
      if (effect.is(setActiveLine)) {
        next = effect.value;
      }
    }
    return next;
  },
  provide: (field) =>
    EditorView.decorations.from(field, (line) =>
      line > 0
        ? Decoration.set([Decoration.line({ class: 'cm-groma-active-line' }).range(line - 1)])
        : Decoration.none
    )
});

const setBreakpointLines = StateEffect.define<ReadonlyArray<number>>();

const breakpointLinesField = StateField.define<ReadonlyArray<number>>({
  create: () => [],
  update(current, tr) {
    let next = current;
    for (const effect of tr.effects) {
      if (effect.is(setBreakpointLines)) {
        next = effect.value;
      }
    }
    return next;
  }
});

class BreakpointGutterMarker extends GutterMarker {
  toDOM(): HTMLElement {
    const dot = document.createElement('div');
    dot.className = 'cm-groma-bp-dot';
    dot.setAttribute('title', '行断点（点击切换）');
    return dot;
  }
}

const BREAKPOINT_MARKER = new BreakpointGutterMarker();

const breakpointGutter = gutter({
  class: 'cm-groma-bp-gutter',
  markers: (view) => {
    const lines = view.state.field(breakpointLinesField, false) ?? [];
    if (lines.length === 0) {
      return RangeSet.empty;
    }
    const maxLine = view.state.doc.lines;
    const ranges = [...lines]
      .sort((a, b) => a - b)
      .filter((line) => line >= 1 && line <= maxLine)
      .map((line) => BREAKPOINT_MARKER.range(line - 1));
    return RangeSet.of(ranges, true);
  },
  domEventHandlers: {
    mousedown: (_view, line) => {
      toggleBreakpointAt(line.from + 1);
      return true;
    }
  }
});

function toggleBreakpointAt(line: number): void {
  dapDebug.toggleLineBreakpoint(currentFile.value, line);
  feedback.value = `已切换断点：${currentFile.value} 第 ${line} 行（经 /ws/debug 频道下发 setBreakpoints）`;
}

/* ---------- 编辑器装载与联动 ---------- */

/**
 * 集中构造编辑器初始状态（只读视图 + 挂起行高亮 + 行断点 gutter）
 */
function editorStateFromContent(content: string): EditorState {
  return EditorState.create({
    doc: content,
    extensions: [
      EditorView.editable.of(false),
      EditorView.lineWrapping,
      activeLineField,
      breakpointLinesField,
      breakpointGutter,
      EditorView.theme({
        '&': { height: '100%', fontSize: '13px' },
        '.cm-scroller': { fontFamily: "'JetBrains Mono', Menlo, Consolas, monospace" },
        '.cm-gutter': { userSelect: 'none' }
      })
    ]
  });
}

function mountEditor(content: string): void {
  const host = editorHost.value;
  if (!host) {
    return;
  }
  editorView?.destroy();
  editorView = new EditorView({
    parent: host,
    state: editorStateFromContent(content)
  });
}

function syncBreakpointsToEditor(): void {
  if (!editorView) {
    return;
  }
  const lines = dapDebug.breakpointsByFile[currentFile.value] ?? [];
  editorView.dispatch({ effects: setBreakpointLines.of([...lines]) });
}

function syncPausedLineToEditor(): void {
  if (!editorView) {
    return;
  }
  const frame = dapDebug.pausedFrame;
  if (!frame || frame.relativeFile !== currentFile.value) {
    editorView.dispatch({ effects: setActiveLine.of(0) });
    return;
  }
  const safeLine = Math.max(1, Math.min(frame.line, editorView.state.doc.lines));
  editorView.dispatch({ effects: setActiveLine.of(safeLine) });
}

watch(
  () => dapDebug.breakpointsByFile[currentFile.value],
  () => syncBreakpointsToEditor()
);

watch(
  () => dapDebug.pausedFrame,
  () => syncPausedLineToEditor()
);

function buildDemoSource(relativeFile: string): string {
  const layerName = relativeFile.includes('/')
    ? relativeFile.slice(0, relativeFile.indexOf('/'))
    : 'root';
  const lines: string[] = [
    `// 五层映射演示源码：${relativeFile}（层：${layerName}）`,
    '// 实际内容由 groma-source-generator 从方圆语料生成，此处为前端预览降级版本。',
    `public class ${relativeFile
      .slice(relativeFile.lastIndexOf('/') + 1)
      .replace('.java', '')
      .trim()} {`,
    ''
  ];
  for (let i = 1; i <= 24; i += 1) {
    if (i % 8 === 0) {
      lines.push(`    // 断点提示：本行可挂行断点，命中后联动画布图元与 3D 实体（canvasElementId=cell-${i}）`);
    } else {
      lines.push(`    public void step${i}() { /* 第 ${i} 步仿真逻辑占位 */ }`);
    }
  }
  lines.push('}');
  return lines.join('\n');
}

async function openFile(relativeFile: string): Promise<void> {
  currentFile.value = relativeFile;
  loading.value = true;
  try {
    const dto = await fetchModelSource(DEFAULT_MODEL_CODE, relativeFile);
    mountEditor(dto.content);
    loadMode.value = 'backend';
    feedback.value = `已从后端加载 ${relativeFile}`;
  } catch {
    mountEditor(buildDemoSource(relativeFile));
    loadMode.value = 'demo';
    feedback.value = '后端源码接口未就绪，已加载演示降级内容';
  } finally {
    loading.value = false;
    syncBreakpointsToEditor();
    syncPausedLineToEditor();
  }
}

function connectDebugChannel(): void {
  dapDebug.connectDebugChannel(resolveWsBaseUrl(), sessionIdInput.value.trim() || 'session-demo-001');
  feedback.value = `DAP 调试频道已请求连接：/ws/debug/${sessionIdInput.value}`;
}

function disconnectDebugChannel(): void {
  dapDebug.disconnectDebugChannel();
  feedback.value = 'DAP 调试频道已断开';
}

onMounted(() => {
  void openFile(currentFile.value);
});

onUnmounted(() => {
  editorView?.destroy();
  editorView = null;
  dapDebug.disconnectDebugChannel();
});
</script>

<template>
  <div class="source-page">
    <aside class="source-file-list">
      <h3 class="source-pane-title">五层 Java 源码</h3>
      <button
        v-for="file in DEMO_SOURCE_FILES"
        :key="file"
        type="button"
        class="source-file-item"
        :class="{ 'source-file-item--active': file === currentFile }"
        @click="openFile(file)"
      >
        {{ file }}
      </button>
    </aside>

    <section class="source-main">
      <header class="source-toolbar">
        <span class="source-file-name">{{ currentFile }}</span>
        <span v-if="loadMode === 'demo'" class="source-tag source-tag--warn">演示降级</span>
        <span v-else-if="loadMode === 'backend'" class="source-tag">后端源码</span>
        <span class="source-conn" :class="{ 'source-conn--on': dapDebug.connected }">
          {{ dapDebug.connected ? 'DAP 已连接' : 'DAP 未连接' }}
        </span>
        <input v-model="sessionIdInput" class="source-input" placeholder="sessionId" />
        <button type="button" class="source-btn" @click="connectDebugChannel">连接调试频道</button>
        <button type="button" class="source-btn" @click="disconnectDebugChannel">断开</button>
        <button
          type="button"
          class="source-btn"
          :disabled="!dapDebug.pausedFrame"
          @click="dapDebug.sendStepCommand('next')"
        >
          F8 跳过
        </button>
        <button
          type="button"
          class="source-btn"
          :disabled="!dapDebug.pausedFrame"
          @click="dapDebug.sendStepCommand('nextSimulationTick')"
        >
          下一仿真步
        </button>
        <button
          type="button"
          class="source-btn"
          :disabled="!dapDebug.pausedFrame"
          @click="dapDebug.sendStepCommand('continue')"
        >
          F9 继续
        </button>
      </header>

      <p v-if="dapDebug.pausedFrame" class="source-paused">
        已挂起：{{ dapDebug.pausedFrame.relativeFile }} 第 {{ dapDebug.pausedFrame.line }} 行 ·
        仿真拍 {{ dapDebug.pausedFrame.simulationTick }} ·
        画布图元 {{ dapDebug.pausedFrame.highlightedCanvasElementId || '无' }} ·
        3D 实体 {{ dapDebug.pausedFrame.highlighted3dEntityId || '无' }}
      </p>
      <p class="source-feedback">{{ feedback }}{{ loading ? '（加载中…）' : '' }}</p>

      <div ref="editorHost" class="source-editor-host"></div>
    </section>
  </div>
</template>

<style scoped>
.source-page {
  display: flex;
  height: 100%;
  min-height: 0;
}

.source-file-list {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-right: 1px solid #e5e6eb;
  background: #fafbfc;
}

.source-pane-title {
  margin: 0 0 4px;
  font-size: 13px;
  color: #4e5969;
}

.source-file-item {
  text-align: left;
  padding: 6px 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: #1f2329;
  word-break: break-all;
}

.source-file-item:hover {
  background: #eef2f7;
}

.source-file-item--active {
  background: #3f5d8f;
  color: #fff;
}

.source-main {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.source-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px 0;
}

.source-file-name {
  font-weight: 600;
  font-size: 13px;
}

.source-tag,
.source-conn {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e8f3ff;
  color: #3f5d8f;
}

.source-tag--warn {
  background: #fff7e8;
  color: #b25e02;
}

.source-conn {
  background: #f2f3f5;
  color: #86909c;
}

.source-conn--on {
  background: #e8ffea;
  color: #237804;
}

.source-input {
  width: 180px;
  padding: 4px 8px;
  border: 1px solid #d0d5dd;
  border-radius: 4px;
  font-size: 13px;
}

.source-btn {
  padding: 4px 10px;
  border: 1px solid #3f5d8f;
  border-radius: 4px;
  background: #fff;
  color: #3f5d8f;
  cursor: pointer;
  font-size: 12px;
}

.source-btn:hover:not(:disabled) {
  background: #3f5d8f;
  color: #fff;
}

.source-btn:disabled {
  border-color: #d0d5dd;
  color: #b3b9c4;
  cursor: not-allowed;
}

.source-paused {
  margin: 8px 16px 0;
  padding: 6px 10px;
  border-radius: 4px;
  background: #fff3bf;
  color: #7a5b00;
  font-size: 13px;
}

.source-feedback {
  margin: 6px 16px 0;
  font-size: 12px;
  color: #86909c;
}

.source-editor-host {
  flex: 1 1 auto;
  min-height: 380px;
  margin: 10px 16px 16px;
  border: 1px solid #d0d5dd;
  border-radius: 6px;
  overflow: hidden;
}

.source-editor-host :deep(.cm-editor) {
  height: 100%;
  outline: none;
}

.source-editor-host :deep(.cm-groma-active-line) {
  background: #fff3bf;
  box-shadow: inset 3px 0 0 #f59f00;
}

.source-editor-host :deep(.cm-groma-bp-gutter) {
  width: 22px;
  background: #f1f3f5;
  border-right: 1px solid #e5e6eb;
}

.source-editor-host :deep(.cm-groma-bp-dot) {
  width: 10px;
  height: 10px;
  margin: 3px auto;
  border-radius: 50%;
  background: #e03131;
  cursor: pointer;
}
</style>

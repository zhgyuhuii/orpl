<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { Graph } from '@antv/x6';
import type { Edge } from '@antv/x6';
import {
  ALL_29_EDGE_SHAPES,
  validateCanvasConnection
} from '../graph/x6ConnectionGuard';
import type { CanvasNodeMeta, ExistingEdgeMeta, GromaNodeCategory } from '../graph/x6ConnectionGuard';
import { fetchModelCanvas } from '../api/studioApi';
import type { CanvasCellDTO } from '../api/studioApi';

const DEFAULT_MODEL_CODE = 'demo-model';

const canvasHost = ref<HTMLDivElement | null>(null);
const selectedEdgeShape = ref<string>('edge-agent');
const selectedCategory = ref<GromaNodeCategory>('OBJECT_TYPE');
const feedback = ref('拖动节点锚点即可连线；每次连线实时执行 29 形状白名单与 VAL-009 同向唯一校验');
const stats = ref({ nodes: 0, edges: 0 });

const edgeShapeOptions: ReadonlyArray<string> = Array.from(ALL_29_EDGE_SHAPES);

const CATEGORY_LABELS: Readonly<Record<GromaNodeCategory, string>> = {
  OBJECT_TYPE: '对象类型',
  STATE_ITEM: '状态项',
  OPERATOR: '算子',
  PROCESS: '过程',
  NOTE: '备注'
};

const CATEGORY_FILLS: Readonly<Record<GromaNodeCategory, string>> = {
  OBJECT_TYPE: '#e8f3ff',
  STATE_ITEM: '#fff7e8',
  OPERATOR: '#f0f5ff',
  PROCESS: '#e8ffea',
  NOTE: '#fffbe8'
};

const DEMO_CELLS: ReadonlyArray<CanvasCellDTO> = [
  { id: 'cell-uav', shape: 'node', label: '无人机对象', category: 'OBJECT_TYPE' },
  { id: 'cell-cruise', shape: 'node', label: '巡航过程（耗时）', category: 'PROCESS' },
  { id: 'cell-station', shape: 'node', label: '地面站对象', category: 'OBJECT_TYPE' },
  { id: 'cell-power', shape: 'node', label: '电量状态项', category: 'STATE_ITEM' },
  { id: 'cell-note', shape: 'node', label: '建模备注', category: 'NOTE' },
  { id: 'demo-edge-agent', shape: 'edge-agent', source: 'cell-uav', target: 'cell-cruise' },
  { id: 'demo-edge-consume', shape: 'edge-consumption', source: 'cell-cruise', target: 'cell-power' },
  { id: 'demo-edge-note', shape: 'edge-note', source: 'cell-note', target: 'cell-cruise' }
];

let graph: Graph | null = null;

interface GraphNodeData {
  category: GromaNodeCategory;
}

function registerGromaEdgeShapes(): void {
  for (const shape of ALL_29_EDGE_SHAPES) {
    Graph.registerEdge(
      shape,
      {
        inherit: 'edge',
        attrs: {
          line: { stroke: '#5b7fae', strokeWidth: 1.6, targetMarker: { name: 'block', size: 8 } }
        }
      },
      true
    );
  }
}

function asCategory(rawCategory: string | undefined): GromaNodeCategory {
  if (rawCategory && rawCategory in CATEGORY_LABELS) {
    return rawCategory as GromaNodeCategory;
  }
  return 'PROCESS';
}

function readNodeMeta(cellId: string): CanvasNodeMeta | null {
  if (!graph) {
    return null;
  }
  const cell = graph.getCellById(cellId);
  if (!cell || !cell.isNode()) {
    return null;
  }
  const data = cell.getData<GraphNodeData>();
  return { cellId, category: data && data.category ? data.category : 'PROCESS' };
}

function collectExistingEdges(excludeEdgeId: string): ReadonlyArray<ExistingEdgeMeta> {
  if (!graph) {
    return [];
  }
  return graph
    .getEdges()
    .filter((edge) => edge.id !== excludeEdgeId)
    .map((edge) => ({
      edgeId: edge.id,
      shape: edge.shape,
      sourceId: edge.getSourceCellId(),
      targetId: edge.getTargetCellId()
    }));
}

function refreshStats(): void {
  stats.value = {
    nodes: graph?.getNodes().length ?? 0,
    edges: graph?.getEdges().length ?? 0
  };
}

function handleEdgeConnected(edge: Edge): void {
  if (!graph) {
    return;
  }
  const shape = edge.shape;
  const source = readNodeMeta(edge.getSourceCellId());
  const target = readNodeMeta(edge.getTargetCellId());
  if (!source || !target) {
    graph.removeCell(edge);
    feedback.value = '已拦截：连线两端必须都是已注册的建模节点';
    return;
  }
  const verdict = validateCanvasConnection(shape, source, target, collectExistingEdges(edge.id));
  if (!verdict.allowed) {
    graph.removeCell(edge);
    feedback.value = `已拦截：${verdict.reason}`;
  } else {
    feedback.value = `已建立连线 ${shape}（29 形状白名单 + VAL-009 校验通过）`;
  }
  refreshStats();
}

function isEdgeCell(cell: CanvasCellDTO): boolean {
  return cell.source !== undefined && cell.target !== undefined && cell.shape.startsWith('edge-');
}

function renderCells(cells: ReadonlyArray<CanvasCellDTO>): void {
  if (!graph) {
    return;
  }
  graph.clearCells();
  let slot = 0;
  for (const cell of cells) {
    if (isEdgeCell(cell) && cell.source && cell.target) {
      const safeShape = ALL_29_EDGE_SHAPES.has(cell.shape) ? cell.shape : 'edge-link';
      graph.addEdge({ id: cell.id, shape: safeShape, source: cell.source, target: cell.target });
      continue;
    }
    const category = asCategory(cell.category);
    graph.addNode({
      id: cell.id,
      shape: 'rect',
      x: 60 + (slot % 3) * 220,
      y: 50 + Math.floor(slot / 3) * 130,
      width: 168,
      height: 48,
      label: cell.label ?? cell.id,
      attrs: {
        body: { fill: CATEGORY_FILLS[category], stroke: '#3f5d8f', rx: 6, ry: 6 },
        label: { fontSize: 12, fill: '#1f2329' }
      },
      data: { category }
    });
    slot += 1;
  }
  refreshStats();
}

async function loadModelCanvas(modelCode: string): Promise<void> {
  if (!graph) {
    return;
  }
  let cells: ReadonlyArray<CanvasCellDTO>;
  try {
    cells = (await fetchModelCanvas(modelCode)).cells;
    feedback.value = `已加载模型 ${modelCode} 的画布数据`;
  } catch {
    cells = DEMO_CELLS;
    feedback.value = '后端画布接口未就绪，已加载本地演示图元（连线校验逻辑不受影响）';
  }
  renderCells(cells);
}

function addNodeFromToolbar(): void {
  if (!graph) {
    return;
  }
  const category = selectedCategory.value;
  const count = graph.getNodes().length;
  const nodeId = `cell-new-${count + 1}-${Date.now().toString(36)}`;
  graph.addNode({
    id: nodeId,
    shape: 'rect',
    x: 60 + (count % 3) * 220,
    y: 50 + Math.floor(count / 3) * 130,
    width: 168,
    height: 48,
    label: `${CATEGORY_LABELS[category]}-${count + 1}`,
    attrs: {
      body: { fill: CATEGORY_FILLS[category], stroke: '#3f5d8f', rx: 6, ry: 6 },
      label: { fontSize: 12, fill: '#1f2329' }
    },
    data: { category }
  });
  refreshStats();
}

function clearCanvas(): void {
  graph?.clearCells();
  refreshStats();
  feedback.value = '画布已清空，可重新加载模型画布或添加演示节点';
}

onMounted(() => {
  const host = canvasHost.value;
  if (!host) {
    return;
  }
  registerGromaEdgeShapes();
  graph = new Graph({
    container: host,
    background: { color: '#f7f8fa' },
    grid: { visible: true, type: 'dot' },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },
    connecting: {
      router: 'manhattan',
      connector: 'rounded',
      allowBlank: false,
      allowLoop: true,
      allowMulti: false,
      createEdge: () => {
        const current = graph;
        if (!current) {
          throw new Error('画布尚未初始化，无法创建连线');
        }
        return current.createEdge({ shape: selectedEdgeShape.value });
      }
    }
  });
  graph.on('edge:connected', ({ edge }) => handleEdgeConnected(edge));
  void loadModelCanvas(DEFAULT_MODEL_CODE);
});

onUnmounted(() => {
  graph?.dispose();
  graph = null;
});
</script>

<template>
  <div class="canvas-page">
    <header class="canvas-toolbar">
      <label class="toolbar-field">
        连线形状
        <select v-model="selectedEdgeShape" class="toolbar-select">
          <option v-for="shape in edgeShapeOptions" :key="shape" :value="shape">{{ shape }}</option>
        </select>
      </label>
      <label class="toolbar-field">
        节点类别
        <select v-model="selectedCategory" class="toolbar-select">
          <option v-for="(label, key) in CATEGORY_LABELS" :key="key" :value="key">{{ label }}</option>
        </select>
      </label>
      <button type="button" class="toolbar-btn" @click="addNodeFromToolbar">添加节点</button>
      <button type="button" class="toolbar-btn" @click="loadModelCanvas(DEFAULT_MODEL_CODE)">重新加载画布</button>
      <button type="button" class="toolbar-btn" @click="clearCanvas">清空</button>
      <span class="toolbar-stats">节点 {{ stats.nodes }} / 连线 {{ stats.edges }}</span>
    </header>
    <p class="canvas-feedback">{{ feedback }}</p>
    <div ref="canvasHost" class="canvas-host"></div>
  </div>
</template>

<style scoped>
.canvas-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.canvas-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 16px 0;
}

.toolbar-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #4e5969;
}

.toolbar-select {
  max-width: 240px;
  padding: 4px 6px;
  border: 1px solid #d0d5dd;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
}

.toolbar-btn {
  padding: 5px 12px;
  border: 1px solid #3f5d8f;
  border-radius: 4px;
  background: #fff;
  color: #3f5d8f;
  cursor: pointer;
  font-size: 13px;
}

.toolbar-btn:hover {
  background: #3f5d8f;
  color: #fff;
}

.toolbar-stats {
  margin-left: auto;
  font-size: 13px;
  color: #4e5969;
}

.canvas-feedback {
  margin: 8px 16px 0;
  font-size: 13px;
  color: #6b7280;
}

.canvas-host {
  flex: 1 1 auto;
  min-height: 420px;
  margin: 10px 16px 16px;
  border: 1px solid #d0d5dd;
  border-radius: 6px;
  overflow: hidden;
}
</style>

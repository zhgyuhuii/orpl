<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import * as THREE from 'three';
import { useTwinViewportStore } from '../stores/twinViewportStore';

/**
 * 2D/3D 数字孪生态势视口（Three.js 静态可编译实现）：
 * - 数据源：twinViewportStore.latestSnapshot.instances（来自 /ws/groma 频道 TP: 帧）；
 * - 实体渲染：每个 instance 一个盒体网格，缺坐标时按槽位网格布局；
 * - 聚焦联动：twinStore.focused3dEntityId 命中时高亮并推近相机（DAP 断点命中三向联动）。
 * 差距说明：设计说明书第八章的 WebGPU 渲染器与 Rapier3D 物理引擎依赖未引入，
 * 当前为 WebGLRenderer 静态渲染版本，不包含碰撞体物理仿真。
 */

const GRID_COLUMNS = 8;
const COLUMN_GAP = 2.6;
const ROW_GAP = 3.0;
const GRID_ORIGIN_X = 9.1;
const GRID_ORIGIN_Z = 10.5;

const twinStore = useTwinViewportStore();

const viewportHost = ref<HTMLDivElement | null>(null);

let renderer: THREE.WebGLRenderer | null = null;
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let gridHelper: THREE.GridHelper | null = null;
let frameHandle = 0;
let appliedFocusId = '';

const instanceMeshes = new Map<string, THREE.Mesh>();

interface InstancePose {
  x: number;
  y: number;
  z: number;
}

function readPose(instance: unknown, slot: number): InstancePose {
  const gridX = (slot % GRID_COLUMNS) * COLUMN_GAP - GRID_ORIGIN_X;
  const gridZ = Math.floor(slot / GRID_COLUMNS) * ROW_GAP - GRID_ORIGIN_Z;
  const fallback: InstancePose = { x: gridX, y: 0.45, z: gridZ };
  if (typeof instance === 'object' && instance !== null) {
    const record = instance as Record<string, unknown>;
    return {
      x: typeof record.x === 'number' ? record.x : fallback.x,
      y: typeof record.y === 'number' ? record.y : fallback.y,
      z: typeof record.z === 'number' ? record.z : fallback.z
    };
  }
  return fallback;
}

function createInstanceMesh(): THREE.Mesh {
  const geometry = new THREE.BoxGeometry(1.6, 0.9, 2.4);
  const material = new THREE.MeshStandardMaterial({
    color: 0x4e7ea8,
    roughness: 0.45,
    metalness: 0.15
  });
  return new THREE.Mesh(geometry, material);
}

function disposeMesh(mesh: THREE.Mesh): void {
  mesh.geometry.dispose();
  (mesh.material as THREE.Material).dispose();
}

function syncInstances(): void {
  const activeScene = scene;
  if (!activeScene) {
    return;
  }
  const instances = twinStore.latestSnapshot?.instances ?? {};
  const aliveIds = new Set<string>();
  Object.entries(instances).forEach(([instanceId, rawInstance], slot) => {
    aliveIds.add(instanceId);
    let mesh = instanceMeshes.get(instanceId);
    if (!mesh) {
      mesh = createInstanceMesh();
      instanceMeshes.set(instanceId, mesh);
      activeScene.add(mesh);
    }
    const pose = readPose(rawInstance, slot);
    mesh.position.set(pose.x, pose.y, pose.z);
    mesh.rotation.y = (slot % 4) * (Math.PI / 8);
  });
  for (const [instanceId, mesh] of instanceMeshes) {
    if (!aliveIds.has(instanceId)) {
      activeScene.remove(mesh);
      disposeMesh(mesh);
      instanceMeshes.delete(instanceId);
    }
  }
}

function applyFocusHighlight(): void {
  const focusId = twinStore.focused3dEntityId;
  if (focusId === appliedFocusId) {
    return;
  }
  for (const [instanceId, mesh] of instanceMeshes) {
    const material = mesh.material as THREE.MeshStandardMaterial;
    if (focusId !== '' && instanceId === focusId) {
      material.emissive.setHex(0xff9f1c);
      material.emissiveIntensity = 0.9;
      camera?.position.set(mesh.position.x + 6, 7.5, mesh.position.z + 8);
      camera?.lookAt(mesh.position);
    } else {
      material.emissive.setHex(0x000000);
      material.emissiveIntensity = 0;
    }
  }
  appliedFocusId = focusId;
}

function handleResize(): void {
  if (!renderer || !camera || !viewportHost.value) {
    return;
  }
  const width = viewportHost.value.clientWidth;
  const height = viewportHost.value.clientHeight;
  if (width <= 0 || height <= 0) {
    return;
  }
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setSize(width, height);
}

function renderLoop(): void {
  frameHandle = requestAnimationFrame(renderLoop);
  if (renderer && scene && camera) {
    renderer.render(scene, camera);
  }
}

watch(
  () => twinStore.latestSnapshot,
  () => syncInstances()
);

watch(
  () => twinStore.focused3dEntityId,
  () => applyFocusHighlight()
);

onMounted(() => {
  const host = viewportHost.value;
  if (!host) {
    return;
  }
  const width = host.clientWidth || 640;
  const height = host.clientHeight || 360;

  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x0e1626);

  camera = new THREE.PerspectiveCamera(55, width / height, 0.1, 500);
  camera.position.set(0, 13, 17);
  camera.lookAt(0, 0, 0);

  gridHelper = new THREE.GridHelper(44, 44, 0x2f4f7f, 0x1d2e4a);
  scene.add(gridHelper);
  scene.add(new THREE.AmbientLight(0xffffff, 0.65));
  const keyLight = new THREE.DirectionalLight(0xffffff, 1.15);
  keyLight.position.set(8, 14, 6);
  scene.add(keyLight);

  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(width, height);
  host.appendChild(renderer.domElement);

  frameHandle = requestAnimationFrame(renderLoop);
  window.addEventListener('resize', handleResize);

  syncInstances();
  applyFocusHighlight();
});

onUnmounted(() => {
  cancelAnimationFrame(frameHandle);
  window.removeEventListener('resize', handleResize);
  for (const [, mesh] of instanceMeshes) {
    disposeMesh(mesh);
  }
  instanceMeshes.clear();
  gridHelper?.dispose();
  gridHelper = null;
  renderer?.dispose();
  renderer?.domElement.remove();
  renderer = null;
  scene = null;
  camera = null;
});
</script>

<template>
  <div ref="viewportHost" class="twin-viewport-host"></div>
</template>

<style scoped>
.twin-viewport-host {
  width: 100%;
  height: 100%;
  min-height: 320px;
  border-radius: 6px;
  overflow: hidden;
  background: #0e1626;
}
</style>

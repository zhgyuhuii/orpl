import { createRouter, createWebHashHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import StudioShellLayout from '../layouts/StudioShellLayout.vue';
import CanvasModelingView from '../views/CanvasModelingView.vue';
import SourceDebugView from '../views/SourceDebugView.vue';
import TwinViewportView from '../views/TwinViewportView.vue';
import StudioWorkbench from '../components/StudioWorkbench.vue';

/**
 * Studio 工作台路由（hash 模式：便于嵌入宿主框架与静态托管，规避服务端 fallback 配置）
 */
export const studioRoutes: RouteRecordRaw[] = [
  { path: '/', redirect: '/workbench' },
  {
    path: '/',
    component: StudioShellLayout,
    children: [
      { path: 'workbench', name: 'workbench', component: StudioWorkbench },
      { path: 'canvas', name: 'canvas', component: CanvasModelingView },
      { path: 'source', name: 'source', component: SourceDebugView },
      { path: 'twin', name: 'twin', component: TwinViewportView }
    ]
  }
];

export function createStudioRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: studioRoutes
  });
}

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const activeNav = computed(() => (typeof route.name === 'string' ? route.name : 'workbench'));

interface NavItem {
  readonly name: string;
  readonly path: string;
  readonly label: string;
}

const NAV_ITEMS: ReadonlyArray<NavItem> = [
  { name: 'workbench', path: '/workbench', label: '三栏工作台' },
  { name: 'canvas', path: '/canvas', label: 'X6 建模画布' },
  { name: 'source', path: '/source', label: '源码与断点' },
  { name: 'twin', path: '/twin', label: '3D 态势视口' }
];
</script>

<template>
  <div class="studio-shell">
    <header class="studio-header">
      <span class="studio-brand">Groma Studio 工作台</span>
      <nav class="studio-nav">
        <router-link
          v-for="item in NAV_ITEMS"
          :key="item.name"
          :to="item.path"
          class="studio-nav-link"
          :class="{ 'studio-nav-link--active': activeNav === item.name }"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </header>
    <main class="studio-main">
      <router-view />
    </main>
    <footer class="studio-footer">REST 前缀 /api/v1/studio · WS /ws/groma / /ws/debug</footer>
  </div>
</template>

<style scoped>
.studio-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  color: #1f2329;
}

.studio-header {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 52px;
  padding: 0 20px;
  background: #101828;
  color: #fff;
  flex: 0 0 auto;
}

.studio-brand {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.studio-nav {
  display: flex;
  gap: 12px;
}

.studio-nav-link {
  color: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 14px;
}

.studio-nav-link--active,
.studio-nav-link:hover {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.studio-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.studio-footer {
  flex: 0 0 auto;
  padding: 4px 20px;
  font-size: 12px;
  color: #86909c;
  border-top: 1px solid #e5e6eb;
}
</style>

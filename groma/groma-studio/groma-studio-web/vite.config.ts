/// <reference types="vitest" />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  build: {
    target: 'es2022',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'pinia'],
          'vendor-ui': ['element-plus'],
          'vendor-graph': ['@antv/x6'],
          'vendor-3d': ['three', 'pixi.js']
        }
      }
    }
  },
  test: {
    environment: 'jsdom'
  }
});

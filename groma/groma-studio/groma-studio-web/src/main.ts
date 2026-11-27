import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import { createStudioRouter } from './router/studioRoutes';

const app = createApp(App);
app.use(createPinia());
app.use(createStudioRouter());
app.use(ElementPlus);
app.mount('#app');

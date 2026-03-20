import { createApp } from 'vue'
import App from './App.vue'

// 引入 Element Plus 及其核心 CSS
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

// 告诉 Vue 使用 Element Plus
app.use(ElementPlus)
app.mount('#app')

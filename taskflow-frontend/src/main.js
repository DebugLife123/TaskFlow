import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'

// 引入 Element Plus 及其核心 CSS
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router' // 引入路由
// 💡 导入图标（用于登录页的 User/Lock 图标）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// ✨ 新增：Axios 全局拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    // 只要有 Token，就在请求头里加上它，后端拦截器就能收到了
    config.headers['token'] = token
  }
  return config
}, error => {
  return Promise.reject(error)
})

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 告诉 Vue 使用 Element Plus
app.use(ElementPlus)
app.use(router) // 挂载路由
app.mount('#app')

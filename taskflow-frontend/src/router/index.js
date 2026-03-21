import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Board from '../views/Board.vue'
import Register from '../views/Register.vue' // ✨ 1. 引入注册页

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/register', component: Register }, // ✨ 2. 注册页路由
  { 
    path: '/board', 
    component: Board,
    // 🛡️ 路由守卫：没登录（没Token）不准进看板
    beforeEnter: (to, from, next) => {
      const token = localStorage.getItem('token')
      if (token) next()
      else next('/login')
    }
  },
  // 在 routes 数组中添加以下对象
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue') // 懒加载引入
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
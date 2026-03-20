<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 style="text-align: center; margin: 0;">TaskFlow 注册</h2>
      </template>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入新账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="success" style="width: 100%" @click="handleRegister" :loading="loading">立即注册</el-button>
        </el-form-item>
        <div style="text-align: right; margin-top: 10px;">
          <el-link type="info" :underline="false" @click="router.push('/login')">⬅️ 已有账号？返回登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleRegister = async () => {
  if (!form.username || !form.password) return ElMessage.warning('账号和密码不能为空！')
  
  loading.value = true
  try {
    const res = await axios.post('/api/user/register', form)
    if (res.data.code === 200) {
      ElMessage.success('🎉 注册成功！请登录')
      router.push('/login') // ✨ 注册成功，自动跳转回登录页
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (err) {
    ElMessage.error('服务器连接失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  /* 1. 使用更丰富的多色线性渐变 */
  background: linear-gradient(125deg, #2c3e50, #27ae60, #2980b9, #e74c3c, #8e44ad);
  /* 2. 让背景画布比例放大，这样移动时才不会露白 */
  background-size: 400% 400%;
  /* 3. 绑定我们下面定义的动画，15秒循环一次，平滑过渡 */
  animation: gradientMove 15s ease infinite;
  overflow: hidden;
}

/* 4. 定义背景移动的动画帧 */
@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}
.login-card {
  width: 400px;
  /* 1. 设置半透明背景，白色带 0.2 的透明度 */
  background: rgba(255, 255, 255, 0.2) !important;
  /* 2. 核心：毛玻璃模糊效果 */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  /* 3. 细微的边框，模拟玻璃边缘的折射 */
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 16px;
  /* 4. 去失默认阴影，改用更柔和的深色投影 */
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
}

/* 5. 穿透修改 Element Plus 内部标题样式 */
:deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
/* 1. 深度修改输入框样式 */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  box-shadow: none !important;
  border-radius: 8px;
  transition: all 0.3s;
}

/* 输入框聚焦时的发光效果 */
:deep(.el-input__wrapper.is-focus) {
  background-color: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.2) !important;
}

/* 修改输入框文字颜色 */
:deep(.el-input__inner) {
  color: #ffffff !important;
}
:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}

/* 2. 修改图标颜色 */
:deep(.el-input__prefix-icon) {
  color: #ffffff !important;
}

/* 3. 让登录按钮更有质感 */
.el-button--primary {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  height: 40px;
  border-radius: 8px;
  font-weight: bold;
  letter-spacing: 2px;
  transition: transform 0.2s;
}

.el-button--primary:hover {
  transform: scale(1.02);
  opacity: 0.9;
}

/* 4. 底部链接颜色调整 */
.el-link {
  color: rgba(255, 255, 255, 0.8) !important;
}
.el-link:hover {
  color: #ffffff !important;
}
</style>
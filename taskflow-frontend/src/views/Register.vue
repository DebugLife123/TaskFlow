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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  border-radius: 12px;
}
</style>
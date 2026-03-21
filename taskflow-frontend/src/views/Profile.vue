<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const activeTab = ref('info') // 控制右侧选项卡

// --- 1. 用户基本信息 ---
const userForm = reactive({
  nickname: '',
  email: 'admin@taskflow.com', // 模拟数据
  role: '超级管理员'
})

// 头像上传相关
const imageUrl = ref('https://img1.baidu.com/it/u=2092066117,4107631351&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=800')
const uploadHeaders = {
  // 从 localStorage 取出 token 塞进请求头，具体 key (如 token 或 Authorization) 根据你的后端拦截器设定
  token: localStorage.getItem('token') 
}

// --- 2. 密码修改表单与验证规则 ---
const pwdFormRef = ref(null)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义验证规则：确认密码必须和新密码一致
const validateConfirmPwd = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致！'))
  } else {
    callback()
  }
}

const pwdRules = reactive({
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPwd, trigger: 'blur' }]
})

// ================= 3. 获取真实用户信息 =================
const fetchUserInfo = async () => {
  try {
    const res = await axios.get('/api/user/info', {
      headers: { token: localStorage.getItem('token') } // 带上你的身份牌
    })
    
    if (res.data.code === 200) {
      const userData = res.data.data
      
      // 1. 替换真实昵称
      userForm.nickname = userData.nickname || userData.username
      
      // 2. 替换真实头像（如果有的话）
      if (userData.avatar) {
        imageUrl.value = userData.avatar
      }
    }
  } catch (error) {
    console.error("获取个人信息失败", error)
    ElMessage.error('获取个人信息失败，请检查网络')
  }
}

// ✨ 1. 新增：存放动态计算的任务数量
const doingCount = ref(0)
const doneCount = ref(0)

// ✨ 2. 新增：拉取任务列表并统计数量的方法
const fetchUserStats = async () => {
  try {
    const res = await axios.get('/api/tasks/list', {
      headers: { token: localStorage.getItem('token') } // 别忘了带上身份牌
    })
    
    if (res.data.code === 200) {
      const tasks = res.data.data
      // 遍历数组，数一数有多少个对应状态的任务
      doingCount.value = tasks.filter(task => task.status === 1).length
      doneCount.value = tasks.filter(task => task.status === 2).length
    }
  } catch (error) {
    console.error("获取任务统计失败", error)
  }
}

// 页面一加载，就去向后端要数据！
onMounted(() => {
  fetchUserInfo()
  fetchUserStats() // ✨ 3. 新增：顺便把任务统计也查一下
})

// --- 4. 交互方法 ---
const goBack = () => {
  router.push('/board')
}

// 头像上传前的格式与大小校验
const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 修改原有的 handleAvatarSuccess 方法，接住后端传回来的真实 URL
const handleAvatarSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    imageUrl.value = response.data // 替换为后端生成的网络图片地址
    ElMessage.success('头像上传成功！')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// ✨ 提交修改基本信息（昵称）的请求
const submitUserInfo = async () => {
  // 1. 基础校验
  if (!userForm.nickname || userForm.nickname.trim() === '') {
    return ElMessage.warning('昵称不能为空！')
  }

  try {
    // 2. 向后端发送 POST 请求
    const res = await axios.post('/api/user/update-info', {
      nickname: userForm.nickname
    }, {
      headers: { token: localStorage.getItem('token') } // 必须带上身份牌
    })

    if (res.data.code === 200) {
      ElMessage.success('基本信息保存成功！')
      
      // 3. 保存成功后，重新向后端拉取一次最新数据，刷新页面上的显示
      fetchUserInfo() 
    } else {
      ElMessage.error(res.data.msg || res.data.message || '保存失败')
    }
  } catch (error) {
    console.error("保存个人信息失败", error)
    ElMessage.error('网络请求失败，请检查后端是否启动')
  }
}

// ✨ 提交修改密码的请求
const submitPassword = () => {
  pwdFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 发送 POST 请求给后端，把旧密码和新密码包在一个对象里传过去
        const res = await axios.post('/api/user/update-password', {
          oldPassword: pwdForm.oldPassword,
          newPassword: pwdForm.newPassword
        }, {
          headers: { token: localStorage.getItem('token') } // 别忘了带上身份牌
        })

        if (res.data.code === 200) {
          ElMessage.success('密码修改成功，请重新登录！')
          
          // 💡 企业级体验：修改密码后，必须清理本地的旧 Token，并强制踢回登录页
          localStorage.removeItem('token')
          setTimeout(() => {
            router.push('/login')
          }, 1500) // 延迟 1.5 秒跳转，让用户看清成功提示
          
        } else {
          // 后端如果返回 400（比如原密码错误），就会走到这里
          ElMessage.error(res.data.msg || res.data.message || '修改失败')
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('网络请求失败，请检查后端是否启动')
      }
    } else {
      ElMessage.warning('请检查表单填写是否有误')
      return false
    }
  })
}
</script>

<template>
  <div class="profile-layout">
    <div class="header">
      <el-page-header @back="goBack" title="返回看板">
        <template #content>
          <span style="font-size: 18px; font-weight: bold; color: #303133;">个人中心</span>
        </template>
      </el-page-header>
    </div>

    <div class="profile-container">
      <el-row :gutter="24">
        
        <el-col :span="8">
          <el-card shadow="hover" class="avatar-card">
            <div class="avatar-wrapper">
             <el-upload
                class="avatar-uploader"
                action="/api/user/upload-avatar" 
                :headers="uploadHeaders"
                name="file"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
              >
                <img v-if="imageUrl" :src="imageUrl" class="avatar" />
                <div v-else class="avatar-uploader-icon">
                  <el-icon><Plus /></el-icon>
                </div>
                <div class="upload-mask">
                  <el-icon><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </el-upload>
            </div>
            
            <h2 class="user-nickname">{{ userForm.nickname }}</h2>
            <p class="user-role">{{ userForm.role }}</p>
            
            <el-divider />
            
           <div class="user-stats">
              <div class="stat-item">
                <span class="stat-num">{{ doneCount }}</span>
                <span class="stat-label">完成任务</span>
              </div>
              <div class="stat-item">
                <span class="stat-num">{{ doingCount }}</span>
                <span class="stat-label">进行中</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card shadow="hover" class="form-card">
            <el-tabs v-model="activeTab" class="profile-tabs">
              
              <el-tab-pane label="基本信息" name="info">
                <el-form :model="userForm" label-width="80px" class="mt-4">
                  <el-form-item label="用户昵称">
                    <el-input v-model="userForm.nickname" size="large" />
                  </el-form-item>
                  <el-form-item label="登录邮箱">
                    <el-input v-model="userForm.email" disabled size="large" />
                    <span style="font-size: 12px; color: #909399; margin-top: 4px;">登录账号暂不支持修改</span>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" size="large" @click="submitUserInfo">保存修改</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>

              <el-tab-pane label="安全设置" name="security">
                <el-form 
                  ref="pwdFormRef" 
                  :model="pwdForm" 
                  :rules="pwdRules" 
                  label-width="100px" 
                  class="mt-4"
                >
                  <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password size="large" placeholder="请输入当前密码" />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password size="large" placeholder="请输入至少6位新密码" />
                  </el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword">
                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password size="large" placeholder="请再次输入新密码" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="danger" size="large" @click="submitPassword">更新密码</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>

            </el-tabs>
          </el-card>
        </el-col>

      </el-row>
    </div>
  </div>
</template>

<style scoped>
.profile-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background-color: #fff;
  padding: 15px 50px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
}

.profile-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
}

.mt-4 {
  margin-top: 20px;
}

/* 左侧名片样式 */
.avatar-card {
  text-align: center;
  border-radius: 12px;
  padding-bottom: 20px;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 20px auto;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border: 4px solid #fff;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 巧妙的头像悬浮上传遮罩 */
.upload-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.avatar-wrapper:hover .upload-mask {
  opacity: 1;
}

.upload-mask span {
  font-size: 12px;
  margin-top: 5px;
}

.user-nickname {
  margin: 10px 0 5px;
  color: #303133;
}

.user-role {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 15px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 右侧表单样式 */
.form-card {
  border-radius: 12px;
  min-height: 400px;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 20px;
  height: 50px;
  line-height: 50px;
}
</style>
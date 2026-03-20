<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus' // ✨ 补上了 ElMessageBox
import axios from 'axios'

// --- 1. 响应式数据定义 ---
const router = useRouter()
const nickname = ref('加载中...')
const taskList = ref([]) // ✅ 只保留这一个声明
const dialogVisible = ref(false)
const taskForm = reactive({ 
  title: '', 
  content: '' ,
  priority: 1 // 默认选中“普通”
})

// --- 2. 数据获取逻辑 ---
const fetchTasks = async () => {
  try {
    const response = await axios.get('/api/tasks/list')
    if (response.data.code === 200) {
      taskList.value = response.data.data
    }
  } catch (error) {
    ElMessage.error("获取任务列表失败")
  }
}

const fetchUserInfo = async () => {
  try {
    const res = await axios.get('/api/user/info')
    if (res.data.code === 200) {
      nickname.value = res.data.data.nickname
    }
  } catch (err) {
    console.error("获取用户信息失败")
  }
}

// --- 3. 任务操作逻辑 ---
// ✨ 替换你原本的 submitTask 函数
const submitTask = async () => {
  // 1. 标题非空校验
  if (!taskForm.title) {
    ElMessage.warning('请输入任务标题！')
    return
  }
  
  try {
    // 2. 发送请求
    const res = await axios.post('/api/tasks/add', taskForm)
    
    if (res.data.code === 200) {
      ElMessage.success('任务添加成功！')
      
      // 3. 收尾工作（注意这里的拼写，千万别拼错变量名）
      dialogVisible.value = false
      taskForm.title = ''
      taskForm.content = ''
      taskForm.priority = 1 // 恢复默认优先级为普通
      
      // 4. 刷新列表
      fetchTasks() 
    } else {
      ElMessage.error(res.data.message || '后端拒绝了添加')
    }
  } catch (error) {
    // 💡 导师黑科技：把真正的报错原因打印到浏览器的控制台
    console.error("🚨 抓到捣鬼的报错了：", error) 
    ElMessage.error('添加失败，请按 F12 查看控制台红字')
  }
}

const updateStatus = async (task, newStatus) => {
  try {
    const updatedTask = { ...task, status: newStatus }
    const res = await axios.post('/api/tasks/update', updatedTask)
    if (res.data.code === 200) {
      ElMessage.success('状态更新成功')
      fetchTasks()
    }
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const deleteTask = async (id) => {
  try {
    const res = await axios.delete(`/api/tasks/delete/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('任务已删除')
      fetchTasks()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// --- 4. 退出登录 ---
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出系统吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    ElMessage.success('已安全退出')
    router.push('/login')
  }).catch(() => {})
}

// --- 5. 生命周期钩子 ---
onMounted(() => {
  fetchUserInfo()
  fetchTasks()
})

// --- 6. 计算属性 ---
const todoTasks = computed(() => taskList.value.filter(task => task.status === 0))
const doingTasks = computed(() => taskList.value.filter(task => task.status === 1))
const doneTasks = computed(() => taskList.value.filter(task => task.status === 2))
</script>

<template>
  <div class="common-layout">
    <el-container>
      <el-header class="header">
        <div class="logo">🚀 TaskFlow Kanban</div>
        <div class="user-info">
          <span class="welcome">欢迎回来，<strong>{{ nickname }}</strong></span>
          <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="board-container">
        <div class="board-header">
          <h2 style="margin: 0;">🚀 TaskFlow 敏捷看板</h2>
          <el-button type="primary" @click="dialogVisible = true"> + 新增任务 </el-button>
        </div>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="board-column">
              <template #header><div class="column-header">待办事项 (TODO)</div></template>
              <el-card v-for="task in todoTasks" :key="task.id" class="task-card" shadow="hover">
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                <h4 style="margin: 0; color: #333;">{{ task.title }}</h4>
                <el-tag v-if="task.priority === 1" type="info" size="small" effect="plain">普通</el-tag>
                <el-tag v-else-if="task.priority === 2" type="warning" size="small" effect="dark">中等</el-tag>
                <el-tag v-else-if="task.priority === 3" type="danger" size="small" effect="dark">紧急</el-tag>
              </div>
                <p class="task-content">{{ task.content }}</p>
                <div class="card-footer">
                  <el-button size="small" type="success" plain @click="updateStatus(task, 1)">开始制作 ➡️</el-button>
                  <el-button size="small" type="danger" text @click="deleteTask(task.id)">删除</el-button>
                </div>
              </el-card>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="board-column">
              <template #header><div class="column-header" style="color: #e6a23c;">进行中 (DOING)</div></template>
              <el-card v-for="task in doingTasks" :key="task.id" class="task-card" shadow="hover">
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                <h4 style="margin: 0; color: #333;">{{ task.title }}</h4>
                <el-tag v-if="task.priority === 1" type="info" size="small" effect="plain">普通</el-tag>
                <el-tag v-else-if="task.priority === 2" type="warning" size="small" effect="dark">中等</el-tag>
                <el-tag v-else-if="task.priority === 3" type="danger" size="small" effect="dark">紧急</el-tag>
              </div>
                <p class="task-content">{{ task.content }}</p>
                <div class="card-footer">
                  <el-button size="small" type="info" plain @click="updateStatus(task, 0)">⬅️ 撤回</el-button>
                  <el-button size="small" type="success" plain @click="updateStatus(task, 2)">完成 ➡️</el-button>
                </div>
              </el-card>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="board-column">
              <template #header><div class="column-header" style="color: #67c23a;">已完成 (DONE)</div></template>
              <el-card v-for="task in doneTasks" :key="task.id" class="task-card" shadow="hover">
               <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                <h4 style="margin: 0; color: #333;">{{ task.title }}</h4>
                <el-tag v-if="task.priority === 1" type="info" size="small" effect="plain">普通</el-tag>
                <el-tag v-else-if="task.priority === 2" type="warning" size="small" effect="dark">中等</el-tag>
                <el-tag v-else-if="task.priority === 3" type="danger" size="small" effect="dark">紧急</el-tag>
              </div>
                <p class="task-content">{{ task.content }}</p>
                <div class="card-footer">
                  <el-button size="small" type="warning" plain @click="updateStatus(task, 1)">⬅️ 返工</el-button>
                  <el-button size="small" type="danger" text @click="deleteTask(task.id)">删除</el-button>
                </div>
              </el-card>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>

    <el-dialog v-model="dialogVisible" title="新增任务卡片" width="30%">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务标题">
          <el-input v-model="taskForm.title" placeholder="准备做什么？" />
        </el-form-item>

<el-form-item label="优先级">
          <el-select v-model="taskForm.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option label="🟢 普通" :value="1" />
            <el-option label="🟡 中等" :value="2" />
            <el-option label="🔴 紧急" :value="3" />
          </el-select>
        </el-form-item>



        <el-form-item label="任务详情">
          <el-input v-model="taskForm.content" type="textarea" placeholder="写点详细的备注吧..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitTask">提 交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header {
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ddd;
  padding: 0 50px;
}
.logo { font-size: 20px; font-weight: bold; color: #409eff; }
.welcome { margin-right: 20px; font-size: 14px; }
.board-header { display: flex; justify-content: center; align-items: center; margin-bottom: 30px; gap: 20px; }
.board-container { padding: 20px 50px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }
.board-column { background-color: #ebecf0; border-radius: 8px; min-height: 500px; }
.column-header { font-weight: bold; font-size: 16px; }
.task-card { margin-bottom: 15px; }
.card-footer { margin-top: 15px; text-align: right; }
.task-content { font-size: 14px; color: #666; margin-top: 8px; }
h4 { margin: 0; color: #333; }
</style>
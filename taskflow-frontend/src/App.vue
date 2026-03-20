<script setup>
import { ref, reactive,onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const taskList = ref([])

// 抓取数据的函数保持不变
const fetchTasks = async () => {
  try {
    const response = await axios.get('/api/tasks/list')
    if (response.data.code === 200) {
      taskList.value = response.data.data
    }
  } catch (error) {
    console.error("请求失败：", error)
  }
}

onMounted(() => {
  fetchTasks()
})

// ✨ 新增核心逻辑：利用计算属性，根据 status 将任务分类
const todoTasks = computed(() => taskList.value.filter(task => task.status === 0))
const doingTasks = computed(() => taskList.value.filter(task => task.status === 1))
const doneTasks = computed(() => taskList.value.filter(task => task.status === 2))

// ================= 新增逻辑区 =================

// ✨ 2. 控制弹窗是否显示的开关
const dialogVisible = ref(false)

// ✨ 3. 存放表单数据的对象
const taskForm = reactive({
  title: '',
  content: ''
})
// ✨ 4. 提交表单的函数
const submitTask = async () => {
  // 简单校验一下，标题不能为空
  if (!taskForm.title) {
    ElMessage.warning('请输入任务标题！')
    return
  }
  
  try {
    // 向我们刚才在后端写的 /add 接口发送 POST 请求，把 taskForm 传过去
    const res = await axios.post('/api/tasks/add', taskForm)
    if (res.data.code === 200) {
      ElMessage.success('任务添加成功！')
      
      // 添加成功后的收尾工作：
      dialogVisible.value = false // 关闭弹窗
      taskForm.title = ''         // 清空表单标题
      taskForm.content = ''       // 清空表单内容
      fetchTasks()                // 重新从后端拉取一次最新数据，刷新页面！
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('添加失败，请查看控制台')
  }
}

// ✨ 新增 1：更新任务状态的函数
const updateStatus = async (task, newStatus) => {
  try {
    // 组装一个新的任务对象，ID还是原来的，但状态改成我们要的目标状态
    const updatedTask = { ...task, status: newStatus }
    // 发送 POST 请求交给后端更新
    const res = await axios.post('/api/tasks/update', updatedTask)
    
    if (res.data.code === 200) {
      ElMessage.success('状态流转成功！')
      fetchTasks() // 更新成功后，重新拉取数据刷新页面
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('更新失败')
  }
}

// ✨ 新增 2：删除任务的函数
const deleteTask = async (id) => {
  try {
    // 发送 DELETE 请求，注意这里的 URL 拼接了任务的 id
    const res = await axios.delete(`/api/tasks/delete/${id}`)
    
    if (res.data.code === 200) {
      ElMessage.success('任务已删除！')
      fetchTasks() // 删除成功后，刷新页面
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('删除失败')
  }
}

</script>

<template>
  <div class="board-container">

    <div style="display: flex; justify-content: center; align-items: center; margin-bottom: 30px;">
      <h2 style="margin: 0; margin-right: 20px;">🚀 TaskFlow 敏捷看板</h2>
      <el-button type="primary" @click="dialogVisible = true"> + 新增任务 </el-button>
    </div>
    
    <el-row :gutter="20">
      
      <el-col :span="8">
        <el-card class="board-column">
          <template #header><div class="column-header">待办事项 (TODO)</div></template>
          <el-card v-for="task in todoTasks" :key="task.id" class="task-card" shadow="hover">
            <h4>{{ task.title }}</h4>
            <p class="task-content">{{ task.content }}</p>
            <div style="margin-top: 15px; text-align: right;">
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
            <h4>{{ task.title }}</h4>
            <p class="task-content">{{ task.content }}</p>
            <div style="margin-top: 15px; text-align: right;">
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
            <h4>{{ task.title }}</h4>
            <p class="task-content">{{ task.content }}</p>
            <div style="margin-top: 15px; text-align: right;">
              <el-button size="small" type="warning" plain @click="updateStatus(task, 1)">⬅️ 返工</el-button>
              <el-button size="small" type="danger" text @click="deleteTask(task.id)">删除</el-button>
            </div>
          </el-card>
        </el-card>
      </el-col>

    </el-row>
    
    <el-dialog v-model="dialogVisible" title="新增任务卡片" width="30%">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务标题">
          <el-input v-model="taskForm.title" placeholder="准备做什么？" />
        </el-form-item>
        <el-form-item label="任务详情">
          <el-input v-model="taskForm.content" type="textarea" placeholder="写点详细的备注吧..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitTask">提 交</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
/* 简单的美化样式 */
.board-container {
  padding: 20px 50px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.board-column {
  background-color: #ebecf0;
  border-radius: 8px;
  min-height: 500px;
}
.column-header {
  font-weight: bold;
  font-size: 16px;
}
.task-card {
  margin-bottom: 15px;
  cursor: pointer;
}
.task-content {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}
h4 {
  margin: 0;
  color: #333;
}
</style>
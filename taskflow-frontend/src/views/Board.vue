<script setup>
import { ref, reactive, onMounted, computed, watch, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

// ================= 1. 响应式数据定义 =================
const router = useRouter()
const nickname = ref('加载中...')
// ✨ 新增：动态存储用户头像地址，默认给一个初始头像
const userAvatarUrl = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png') 
const taskList = ref([]) 

// 搜索与过滤变量
const searchQuery = ref('')     
const filterPriority = ref('')  

// 折叠状态变量
const isTodoCollapsed = ref(false)
const isDoingCollapsed = ref(false)
const isDoneCollapsed = ref(false)

// ECharts 引用
const statusChartRef = ref(null)    
const priorityChartRef = ref(null)  
const statusChart = shallowRef(null)
const priorityChart = shallowRef(null)

// 弹窗与表单开关
const dialogVisible = ref(false)      // 新增弹窗开关
const editDrawerVisible = ref(false)  // 编辑抽屉开关

// 表单数据
const taskForm = reactive({ 
  title: '', 
  content: '',
  priority: 1,
  startDate: '', 
  endDate: ''    
})
const editTaskForm = ref({}) 


// ================= 2. ECharts 图表渲染 =================
const updateCharts = () => {
  if (!statusChart.value || !priorityChart.value) return;

  const todoCount = taskList.value.filter(t => t.status === 0).length;
  const doingCount = taskList.value.filter(t => t.status === 1).length;
  const doneCount = taskList.value.filter(t => t.status === 2).length;

  statusChart.value.setOption({
    title: { text: '任务进度概览', left: 'center', top: 10, textStyle: { color: '#606266', fontSize: 16 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 项' }, 
    grid: { left: '5%', right: '5%', bottom: '15%', top: '25%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: ['待办 (TODO)', '进行中 (DOING)', '已完成 (DONE)'], 
      axisLabel: { interval: 0, color: '#606266', fontWeight: 'bold' }, 
      axisTick: { show: false }, 
      axisLine: { lineStyle: { color: '#DCDFE6' } } 
    },
    yAxis: { 
      type: 'value', 
      minInterval: 1,
      splitLine: { lineStyle: { type: 'dashed', color: '#EBEEF5' } } 
    }, 
    series: [
      {
        type: 'bar',
        barMaxWidth: 50, 
        data: [
          { value: todoCount, itemStyle: { color: '#F56C6C', borderRadius: [4, 4, 0, 0] } },
          { value: doingCount, itemStyle: { color: '#E6A23C', borderRadius: [4, 4, 0, 0] } },
          { value: doneCount, itemStyle: { color: '#67C23A', borderRadius: [4, 4, 0, 0] } }
        ],
        label: { show: true, position: 'top', fontSize: 16, fontWeight: 'bold' }
      }
    ]
  });

  const p1Count = taskList.value.filter(t => t.priority === 1).length;
  const p2Count = taskList.value.filter(t => t.priority === 2).length;
  const p3Count = taskList.value.filter(t => t.priority === 3).length;

  priorityChart.value.setOption({
    title: { text: '优先级分布', left: 'center', textStyle: { color: '#606266' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 项 ({d}%)' },
    legend: { bottom: '0', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
      data: [
        { value: p1Count, name: '🟢 普通', itemStyle: { color: '#909399' } },
        { value: p2Count, name: '🟡 中等', itemStyle: { color: '#E6A23C' } },
        { value: p3Count, name: '🔴 紧急', itemStyle: { color: '#F56C6C' } }
      ]
    }]
  });
};


// ================= 3. 数据获取逻辑 =================
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

// ✨ 核心修复：带上 Token，并接收后端的 avatar 地址
const fetchUserInfo = async () => {
  try {
    const res = await axios.get('/api/user/info', {
      headers: { token: localStorage.getItem('token') } // 必须带上Token身份
    })
    if (res.data.code === 200) {
      nickname.value = res.data.data.nickname
      // 如果后端返回了头像地址，就更新它
      if (res.data.data.avatar) {
        userAvatarUrl.value = res.data.data.avatar
      }
    }
  } catch (err) {
    console.error("获取用户信息失败", err)
    ElMessage.error("登录状态已失效，请重新登录")
    router.push('/login')
  }
}


// ================= 4. 任务操作逻辑 (增删改查) =================
const submitTask = async () => {
  if (!taskForm.title) return ElMessage.warning('请输入任务标题！')
  try {
    const res = await axios.post('/api/tasks/add', taskForm)
    if (res.data.code === 200) {
      ElMessage.success('任务添加成功！')
      dialogVisible.value = false
      taskForm.title = ''
      taskForm.content = ''
      taskForm.priority = 1 
      taskForm.startDate = '' 
      taskForm.endDate = '' 
      fetchTasks() 
    } else {
      ElMessage.error(res.data.message || '后端拒绝了添加')
    }
  } catch (error) {
    console.error("添加失败：", error) 
    ElMessage.error('添加失败，请检查网络')
  }
}

const openEditDialog = (task) => {
  editTaskForm.value = { ...task }
  editDrawerVisible.value = true
}

const submitEditTask = async () => {
  if (!editTaskForm.value.title) return ElMessage.warning('任务标题不能为空！')
  try {
    const res = await axios.post('/api/tasks/update', editTaskForm.value)
    if (res.data.code === 200) {
      ElMessage.success('任务修改成功！')
      editDrawerVisible.value = false 
      fetchTasks() 
    } else {
      ElMessage.error(res.data.message || '修改失败')
    }
  } catch (error) {
    ElMessage.error('修改失败，请检查网络')
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


// ================= 5. 拖拽流转核心逻辑 =================
const draggedTask = ref(null)

const handleDragStart = (task) => { draggedTask.value = task }
const handleDragEnd = () => { draggedTask.value = null }
const handleDrop = async (newStatus) => {
  if (!draggedTask.value || draggedTask.value.status === newStatus) return

  const oldStatus = draggedTask.value.status
  try {
    draggedTask.value.status = newStatus 
    const res = await axios.post('/api/tasks/update', draggedTask.value)
    if (res.data.code === 200) {
      ElMessage.success('流转成功！')
    } else {
      throw new Error(res.data.message)
    }
  } catch (error) {
    draggedTask.value.status = oldStatus
    ElMessage.error('拖拽同步失败，已回滚')
  } finally {
    draggedTask.value = null
    fetchTasks() 
  }
}


// ================= 6. 退出登录 =================
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出系统吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    ElMessage.success('已安全退出')
    router.push('/login')
  }).catch(() => {})
}


// ================= 7. 生命周期与监听 =================
onMounted(() => {
  fetchUserInfo()
  fetchTasks()
  setTimeout(() => {
    if (statusChartRef.value) statusChart.value = echarts.init(statusChartRef.value)
    if (priorityChartRef.value) priorityChart.value = echarts.init(priorityChartRef.value)
    if (taskList.value.length > 0) updateCharts()
  }, 100)

  window.addEventListener('resize', () => {
    statusChart.value?.resize()
    priorityChart.value?.resize()
  })
})

watch(() => taskList.value, () => {
  updateCharts()
}, { deep: true })


// ================= 8. 数据漏斗 (计算属性) =================
const filteredTaskList = computed(() => {
  return taskList.value.filter(task => {
    const matchSearch = !searchQuery.value || 
      task.title.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
      (task.content && task.content.toLowerCase().includes(searchQuery.value.toLowerCase()));
    const matchPriority = !filterPriority.value || task.priority === filterPriority.value;
    return matchSearch && matchPriority;
  });
});

const todoTasks = computed(() => filteredTaskList.value.filter(task => task.status === 0))
const doingTasks = computed(() => filteredTaskList.value.filter(task => task.status === 1))
const doneTasks = computed(() => filteredTaskList.value.filter(task => task.status === 2))

// ================= 9. 修复折叠面板 ECharts 塌陷问题 =================
const handleCollapseChange = (activeNames) => {
  if (activeNames.includes('1')) {
    setTimeout(() => {
      statusChart.value?.resize();
      priorityChart.value?.resize();
    }, 300);
  }
}
</script>

<template>
  <div class="common-layout">
    <el-container>
      <el-header class="header">
        <div class="logo">🚀 TaskFlow Kanban</div>
        <div class="user-info">
          <div class="user-profile">
            <div class="user-details">
              <span class="user-name">{{ nickname }}</span>
              <div class="user-status">
                <span class="status-dot"></span>
                <span class="status-text">在线</span>
              </div>
            </div>
            <el-avatar 
              :size="40" 
              :src="userAvatarUrl" 
              @click="router.push('/profile')"
              style="cursor: pointer; border: 2px solid #fff; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); transition: transform 0.3s;"
              onmouseover="this.style.transform='scale(1.1)'"
              onmouseout="this.style.transform='scale(1)'"
            />
          </div>

          <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="board-container">
        <div class="board-header">
          <h2 style="margin: 0;">🚀 TaskFlow 敏捷看板</h2>
          
          <div class="filter-area" style="display: flex; gap: 15px; margin-left: auto; margin-right: 20px;">
            <el-input 
              v-model="searchQuery" 
              placeholder="搜索任务标题或内容..." 
              prefix-icon="Search"
              clearable
              style="width: 250px;"
            />
            
            <el-select v-model="filterPriority" placeholder="全部优先级" clearable style="width: 150px;">
              <el-option label="🟢 普通" :value="1" />
              <el-option label="🟡 中等" :value="2" />
              <el-option label="🔴 紧急" :value="3" />
            </el-select>
          </div>

          <el-button type="primary" @click="dialogVisible = true"> + 新增任务 </el-button>
        </div>

        <el-collapse @change="handleCollapseChange" style="margin-bottom: 20px; border-radius: 8px; overflow: hidden; border: none; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);">
          <el-collapse-item name="1">
            <template #title>
              <div style="font-size: 16px; font-weight: bold; padding-left: 10px; color: #409EFF;">
                📊 展开项目数据看板
              </div>
            </template>
            <el-row :gutter="20" style="padding: 10px 20px;">
              <el-col :span="12">
                <el-card shadow="hover" style="border-radius: 12px;">
                  <div ref="statusChartRef" style="height: 300px; width: 100%;"></div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="hover" style="border-radius: 12px;">
                  <div ref="priorityChartRef" style="height: 300px; width: 100%;"></div>
                </el-card>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
        
       <div class="kanban-flex-container">
          
          <div class="kanban-column" :class="{ 'is-collapsed': isTodoCollapsed }">
            <el-card class="board-column" @dragover.prevent @drop="handleDrop(0)">
              <template #header>
                <div class="column-header-flex">
                  <span v-show="!isTodoCollapsed" class="column-header">待办事项 (TODO)</span>
                  <el-button size="small" circle @click="isTodoCollapsed = !isTodoCollapsed">
                    <el-icon><ArrowLeft v-if="!isTodoCollapsed"/><ArrowRight v-else/></el-icon>
                  </el-button>
                </div>
              </template>
              
              <div v-show="!isTodoCollapsed">
                <el-card 
                  v-for="task in todoTasks" :key="task.id" 
                  class="task-card" shadow="hover" 
                  @click="openEditDialog(task)" 
                  style="cursor: grab;"
                  draggable="true"
                  @dragstart="handleDragStart(task)"
                  @dragend="handleDragEnd"
                  :class="{ 'is-dragging': draggedTask?.id === task.id }"
                >
                  <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                    <h4 style="margin: 0; color: #333;">{{ task.title }}</h4>
                    <el-tag v-if="task.priority === 1" type="info" size="small" effect="plain">普通</el-tag>
                    <el-tag v-else-if="task.priority === 2" type="warning" size="small" effect="dark">中等</el-tag>
                    <el-tag v-else-if="task.priority === 3" type="danger" size="small" effect="dark">紧急</el-tag>
                  </div>
                  <p class="task-content">{{ task.content }}</p>
                  <div v-if="task.startDate || task.endDate" style="font-size: 12px; color: #909399; margin-top: 10px; display: flex; align-items: center;">
                    <el-icon style="margin-right: 4px;"><Calendar /></el-icon>
                    <span>{{ task.startDate || '未定' }} 至 {{ task.endDate || '未定' }}</span>
                  </div>
                  <div class="card-footer">
                    <el-button size="small" type="success" plain @click.stop="updateStatus(task, 1)">开始制作 ➡️</el-button>
                    <el-button size="small" type="danger" text @click.stop="deleteTask(task.id)">删除</el-button>
                  </div>
                </el-card>
              </div>

              <div v-show="isTodoCollapsed" class="vertical-title">待办事项</div>
            </el-card>
          </div>

          <div class="kanban-column" :class="{ 'is-collapsed': isDoingCollapsed }">
            <el-card class="board-column" @dragover.prevent @drop="handleDrop(1)">
              <template #header>
                <div class="column-header-flex">
                  <span v-show="!isDoingCollapsed" class="column-header" style="color: #e6a23c;">进行中 (DOING)</span>
                  <el-button size="small" circle @click="isDoingCollapsed = !isDoingCollapsed">
                    <el-icon><ArrowLeft v-if="!isDoingCollapsed"/><ArrowRight v-else/></el-icon>
                  </el-button>
                </div>
              </template>
              
              <div v-show="!isDoingCollapsed">
                <el-card 
                  v-for="task in doingTasks" :key="task.id" 
                  class="task-card" shadow="hover" 
                  @click="openEditDialog(task)" 
                  style="cursor: grab;"
                  draggable="true"
                  @dragstart="handleDragStart(task)"
                  @dragend="handleDragEnd"
                  :class="{ 'is-dragging': draggedTask?.id === task.id }"
                >
                  <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                    <h4 style="margin: 0; color: #333;">{{ task.title }}</h4>
                    <el-tag v-if="task.priority === 1" type="info" size="small" effect="plain">普通</el-tag>
                    <el-tag v-else-if="task.priority === 2" type="warning" size="small" effect="dark">中等</el-tag>
                    <el-tag v-else-if="task.priority === 3" type="danger" size="small" effect="dark">紧急</el-tag>
                  </div>
                  <p class="task-content">{{ task.content }}</p>
                  <div v-if="task.startDate || task.endDate" style="font-size: 12px; color: #909399; margin-top: 10px; display: flex; align-items: center;">
                    <el-icon style="margin-right: 4px;"><Calendar /></el-icon>
                    <span>{{ task.startDate || '未定' }} 至 {{ task.endDate || '未定' }}</span>
                  </div>
                  <div class="card-footer">
                    <el-button size="small" type="info" plain @click.stop="updateStatus(task, 0)">⬅️ 撤回</el-button>
                    <el-button size="small" type="success" plain @click.stop="updateStatus(task, 2)">完成 ➡️</el-button>
                  </div>
                </el-card>
              </div>

              <div v-show="isDoingCollapsed" class="vertical-title" style="color: #e6a23c;">进行中</div>
            </el-card>
          </div>

          <div class="kanban-column" :class="{ 'is-collapsed': isDoneCollapsed }">
            <el-card class="board-column" @dragover.prevent @drop="handleDrop(2)">
              <template #header>
                <div class="column-header-flex">
                  <span v-show="!isDoneCollapsed" class="column-header" style="color: #67c23a;">已完成 (DONE)</span>
                  <el-button size="small" circle @click="isDoneCollapsed = !isDoneCollapsed">
                    <el-icon><ArrowLeft v-if="!isDoneCollapsed"/><ArrowRight v-else/></el-icon>
                  </el-button>
                </div>
              </template>
              
              <div v-show="!isDoneCollapsed">
                <el-card 
                  v-for="task in doneTasks" :key="task.id" 
                  class="task-card" shadow="hover" 
                  @click="openEditDialog(task)" 
                  style="cursor: grab;"
                  draggable="true"
                  @dragstart="handleDragStart(task)"
                  @dragend="handleDragEnd"
                  :class="{ 'is-dragging': draggedTask?.id === task.id }"
                >
                  <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                    <h4 style="margin: 0; color: #333;">{{ task.title }}</h4>
                    <el-tag v-if="task.priority === 1" type="info" size="small" effect="plain">普通</el-tag>
                    <el-tag v-else-if="task.priority === 2" type="warning" size="small" effect="dark">中等</el-tag>
                    <el-tag v-else-if="task.priority === 3" type="danger" size="small" effect="dark">紧急</el-tag>
                  </div>
                  <p class="task-content">{{ task.content }}</p>
                  <div v-if="task.startDate || task.endDate" style="font-size: 12px; color: #909399; margin-top: 10px; display: flex; align-items: center;">
                    <el-icon style="margin-right: 4px;"><Calendar /></el-icon>
                    <span>{{ task.startDate || '未定' }} 至 {{ task.endDate || '未定' }}</span>
                  </div>
                  <div class="card-footer">
                    <el-button size="small" type="warning" plain @click.stop="updateStatus(task, 1)">⬅️ 返工</el-button>
                    <el-button size="small" type="danger" text @click.stop="deleteTask(task.id)">删除</el-button>
                  </div>
                </el-card>
              </div>

              <div v-show="isDoneCollapsed" class="vertical-title" style="color: #67c23a;">已完成</div>
            </el-card>
          </div>

        </div>
      </el-main>
    </el-container>

    <el-dialog v-model="dialogVisible" title="✨ 新增任务卡片" width="30%">
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

        <el-form-item label="起止时间">
          <div style="display: flex; justify-content: space-between; width: 100%;">
            <el-date-picker
              v-model="taskForm.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 48%;"
            />
            <el-date-picker
              v-model="taskForm.endDate"
              type="date"
              placeholder="截止日期"
              value-format="YYYY-MM-DD"
              style="width: 48%;"
            />
          </div>
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

    <el-drawer 
      v-model="editDrawerVisible" 
      title="📝 编辑任务详情" 
      size="400px"
      destroy-on-close
    >
      <el-form :model="editTaskForm" label-position="top">
        <el-form-item label="任务标题">
          <el-input v-model="editTaskForm.title" placeholder="准备做什么？" size="large" />
        </el-form-item>

        <el-form-item label="优先级">
          <el-select v-model="editTaskForm.priority" placeholder="请选择优先级" style="width: 100%" size="large">
            <el-option label="🟢 普通" :value="1" />
            <el-option label="🟡 中等" :value="2" />
            <el-option label="🔴 紧急" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="起止时间">
          <div style="display: flex; justify-content: space-between; width: 100%;">
            <el-date-picker
              v-model="editTaskForm.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 48%;"
            />
            <el-date-picker
              v-model="editTaskForm.endDate"
              type="date"
              placeholder="截止日期"
              value-format="YYYY-MM-DD"
              style="width: 48%;"
            />
          </div>
        </el-form-item>

        <el-form-item label="任务详情">
          <el-input 
            v-model="editTaskForm.content" 
            type="textarea" 
            :rows="6" 
            placeholder="写点详细的备注吧..." 
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div style="flex: auto; display: flex; justify-content: flex-end; gap: 10px;">
          <el-button @click="editDrawerVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitEditTask">保存修改</el-button>
        </div>
      </template>
    </el-drawer>

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
/* ✨ 新增：用户头像与状态栏样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px; 
}

.user-details {
  display: flex;
  flex-direction: column;
  align-items: flex-end; 
}

.user-name {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.user-status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background-color: #67C23A; 
  border-radius: 50%;
  box-shadow: 0 0 5px rgba(103, 194, 58, 0.6); 
}

.status-text {
  font-size: 12px;
  color: #909399;
}
.board-header { display: flex; justify-content: center; align-items: center; margin-bottom: 30px; gap: 20px; }
.board-container { padding: 20px 50px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }
.board-column { background-color: #ebecf0; border-radius: 8px; min-height: 500px; }
.column-header { font-weight: bold; font-size: 16px; }
.task-card { margin-bottom: 15px; }
.card-footer { margin-top: 15px; text-align: right; }
.task-content { font-size: 14px; color: #666; margin-top: 8px; }
h4 { margin: 0; color: #333; }

/* 卡片拖拽特效 */
.is-dragging {
  opacity: 0.4;
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
  border: 1px dashed #409EFF !important;
}

/* Flex 伸缩布局 */
.kanban-flex-container {
  display: flex;
  gap: 20px;
  align-items: stretch;
  min-height: 500px;
}
.kanban-column {
  flex: 1; 
  transition: all 0.3s ease; 
  min-width: 0; 
}
.kanban-column.is-collapsed {
  flex: 0 0 60px; 
}
.column-header-flex {
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  width: 100%;
}
.vertical-title {
  writing-mode: vertical-lr; 
  letter-spacing: 6px;
  font-weight: bold;
  color: #909399;
  margin: 40px auto;
  text-align: center;
}
.board-column { 
  background-color: #ebecf0; 
  border-radius: 8px; 
  height: 100%; 
}
</style>
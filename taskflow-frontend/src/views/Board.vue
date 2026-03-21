<script setup>
import { ref, reactive, onMounted, computed, watch ,shallowRef} from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus' // ✨ 补上了 ElMessageBox
import axios from 'axios'
import * as echarts from 'echarts' // ✨ 新增：完整引入 ECharts

// --- 1. 响应式数据定义 ---
const router = useRouter()
const nickname = ref('加载中...')
const taskList = ref([]) // ✅ 只保留这一个声明
// --- 新增：搜索与过滤的响应式变量 ---
const searchQuery = ref('')     // 存放用户输入的搜索关键字
const filterPriority = ref('')  // 存放选中的优先级，默认空表示“看全部”
// --- 新增：ECharts 图表容器引用 ---
const statusChartRef = ref(null)    // 指向状态图表的 DOM
const priorityChartRef = ref(null)  // 指向优先级图表的 DOM
// --- ECharts 实例引用（导师黑科技：必须用 shallowRef 提升性能） ---
const statusChart = shallowRef(null)
const priorityChart = shallowRef(null)

// --- 1. 初始化并渲染图表的方法 ---
const updateCharts = () => {
  if (!statusChart.value || !priorityChart.value) return;

  // 📊 左侧图表：任务状态柱状图
  const todoCount = taskList.value.filter(t => t.status === 0).length
  const doingCount = taskList.value.filter(t => t.status === 1).length
  const doneCount = taskList.value.filter(t => t.status === 2).length

  statusChart.value.setOption({
    title: { text: '任务进度漏斗', left: 'center', textStyle: { color: '#606266' } },
    tooltip: { trigger: 'item' },
    grid: { left: '10%', right: '10%', bottom: '15%' },
    xAxis: { type: 'category', data: ['待办 (TODO)', '进行中 (DOING)', '已完成 (DONE)'] },
    yAxis: { type: 'value', minInterval: 1 }, // 保证Y轴是整数
    series: [{
      type: 'bar',
      barWidth: '40%',
      data: [
        { value: todoCount, itemStyle: { color: '#F56C6C', borderRadius: [6, 6, 0, 0] } },
        { value: doingCount, itemStyle: { color: '#E6A23C', borderRadius: [6, 6, 0, 0] } },
        { value: doneCount, itemStyle: { color: '#67C23A', borderRadius: [6, 6, 0, 0] } }
      ],
      label: { show: true, position: 'top', fontSize: 16, fontWeight: 'bold' }
    }]
  })

  // 🥧 右侧图表：优先级环形图
  const p1Count = taskList.value.filter(t => t.priority === 1).length
  const p2Count = taskList.value.filter(t => t.priority === 2).length
  const p3Count = taskList.value.filter(t => t.priority === 3).length

  priorityChart.value.setOption({
    title: { text: '优先级分布', left: 'center', textStyle: { color: '#606266' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 项 ({d}%)' },
    legend: { bottom: '0', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'], // 环形图设计更显高级
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
  })
}


const dialogVisible = ref(false)
const taskForm = reactive({ 
  title: '', 
  content: '' ,
  priority: 1 ,// 默认选中“普通”
  startDate: '', // ✨ 新增开始时间
  endDate: ''    // ✨ 新增截止时间
})
// --- 新增：编辑任务相关的状态与数据 ---
const editDialogVisible = ref(false)
// 用来存放当前正在编辑的任务数据，初始为空对象
const editTaskForm = ref({}) 

// 打开编辑弹窗的方法
const openEditDialog = (task) => {
  // 💡 导师黑科技：这里使用 {...task} 浅拷贝！
  // 为什么不直接 editTaskForm.value = task？
  // 因为如果直接赋值，你在弹窗里修改文字，背后的看板卡片文字会同步跟着变（哪怕你还没点保存）。
  // 用浅拷贝相当于创建了一个“替身”，随便改，只有点保存时才提交给后端。
  editTaskForm.value = { ...task }
  editDialogVisible.value = true
}

// 提交编辑的方法
const submitEditTask = async () => {
  if (!editTaskForm.value.title) {
    return ElMessage.warning('任务标题不能为空！')
  }
  
  try {
    // ✨ 核心业务逻辑：强制把状态改回 0（待办 TODO）
    editTaskForm.value.status = 0
    
    // 调用后端更新接口
    const res = await axios.post('/api/tasks/update', editTaskForm.value)
    if (res.data.code === 200) {
      ElMessage.success('任务修改成功，已转入待办事项！')
      editDialogVisible.value = false // 关闭弹窗
      fetchTasks() // 重新拉取最新列表，刷新页面
    } else {
      ElMessage.error(res.data.message || '修改失败')
    }
  } catch (error) {
    console.error("修改任务报错：", error)
    ElMessage.error('修改失败，请检查网络')
  }
}
// --- ✨ 新增：原生拖拽流转核心逻辑 ---
const draggedTask = ref(null) // 用来记录当前被鼠标抓在空中的是哪个任务

// 1. 拖拽开始：把当前抓起的卡片存起来
const handleDragStart = (task) => {
  draggedTask.value = task
}

// 2. 拖拽结束：松开鼠标时清理状态
const handleDragEnd = () => {
  draggedTask.value = null
}

// 3. 拖拽放下（松手）：最核心的业务逻辑
const handleDrop = async (newStatus) => {
  if (!draggedTask.value) return
  if (draggedTask.value.status === newStatus) return // 如果是在同一列原地放下，啥也不做

  // 记录老状态，为了做容错处理
  const oldStatus = draggedTask.value.status
  
  try {
    // 💡 导师黑科技：乐观更新 (Optimistic UI)
    // 先在前端瞬间把卡片状态改掉，用户体验是“零延迟”的丝滑
    draggedTask.value.status = newStatus 
    
    // 然后再悄悄发请求给后端更新数据库
    const res = await axios.post('/api/tasks/update', draggedTask.value)
    if (res.data.code === 200) {
      ElMessage.success('流转成功！')
    } else {
      throw new Error(res.data.message)
    }
  } catch (error) {
    // 如果后端报错或者断网了，把卡片状态弹回原来的那一列
    draggedTask.value.status = oldStatus
    ElMessage.error('拖拽同步失败，已回滚')
  } finally {
    draggedTask.value = null
    fetchTasks() // 保险起见，重新同步一次最新数据，图表也会跟着刷新
  }
}

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
      taskForm.startDate = '' // 慢复默认开始时间为空字符串
      taskForm.endDate = '' // 慢复默认截止时间为空字符串
      
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
  // 确保 DOM 渲染完毕后再初始化 ECharts
  setTimeout(() => {
    if (statusChartRef.value) statusChart.value = echarts.init(statusChartRef.value)
    if (priorityChartRef.value) priorityChart.value = echarts.init(priorityChartRef.value)
    
    // 如果刚进页面已经有数据了，直接画图
    if (taskList.value.length > 0) {
      updateCharts()
    }
  }, 100)
  // 监听浏览器窗口大小变化，让图表自动缩放（极其影响体验的细节！）
  window.addEventListener('resize', () => {
    statusChart.value?.resize()
    priorityChart.value?.resize()
  })
})

// --- 3. 建立响应式联动：只要任务列表数据一变，自动重新画图 ---
watch(() => taskList.value, () => {
  updateCharts()
}, { deep: true })

// --- 6. 计算属性 ---
// --- 6. 计算属性：超级数据漏斗 ---

// 第一层漏斗：先根据【搜索词】和【优先级】过滤出所有符合条件的任务
const filteredTaskList = computed(() => {
  return taskList.value.filter(task => {
    // 1. 检查搜索词：如果没输入搜索词，就算匹配成功；如果输入了，就看标题或详情里包不包含这个词
    const matchSearch = !searchQuery.value || 
      task.title.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
      (task.content && task.content.toLowerCase().includes(searchQuery.value.toLowerCase()));
      
    // 2. 检查优先级：如果没选优先级，就算匹配成功；如果选了，就必须完全等于选中的级别
    const matchPriority = !filterPriority.value || task.priority === filterPriority.value;
    
    // 必须同时满足上面两个条件，这个任务才能流到下一步
    return matchSearch && matchPriority;
  });
});

// 第二层漏斗：把刚刚过滤出来的结果，再按照 待办/进行中/已完成 分发给三列
const todoTasks = computed(() => filteredTaskList.value.filter(task => task.status === 0))
const doingTasks = computed(() => filteredTaskList.value.filter(task => task.status === 1))
const doneTasks = computed(() => filteredTaskList.value.filter(task => task.status === 2))
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
        <el-collapse style="margin-bottom: 20px; border-radius: 8px; overflow: hidden; border: none; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);">
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
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="board-column" @dragover.prevent @drop="handleDrop(0)">
              <template #header><div class="column-header">待办事项 (TODO)</div></template>
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
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="board-column" @dragover.prevent @drop="handleDrop(1)">
              <template #header><div class="column-header" style="color: #e6a23c;">进行中 (DOING)</div></template>
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
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="board-column" @dragover.prevent @drop="handleDrop(2)">
              <template #header><div class="column-header" style="color: #67c23a;">已完成 (DONE)</div></template>
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


        <el-form-item label="起止时间">
          <el-date-picker
            v-model="taskForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 48%; margin-right: 4%;"
          />
          <el-date-picker
            v-model="taskForm.endDate"
            type="date"
            placeholder="截止日期"
            value-format="YYYY-MM-DD"
            style="width: 48%;"
          />
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

    <el-dialog v-model="editDialogVisible" title="编辑任务详情" width="30%">
      <el-form :model="editTaskForm" label-width="80px">
        <el-form-item label="任务标题">
          <el-input v-model="editTaskForm.title" placeholder="准备做什么？" />
        </el-form-item>

        <el-form-item label="优先级">
          <el-select v-model="editTaskForm.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option label="🟢 普通" :value="1" />
            <el-option label="🟡 中等" :value="2" />
            <el-option label="🔴 紧急" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="起止时间">
          <el-date-picker
            v-model="editTaskForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 48%; margin-right: 4%;"
          />
          <el-date-picker
            v-model="editTaskForm.endDate"
            type="date"
            placeholder="截止日期"
            value-format="YYYY-MM-DD"
            style="width: 48%;"
          />
        </el-form-item>

        <el-form-item label="任务详情">
          <el-input v-model="editTaskForm.content" type="textarea" placeholder="写点详细的备注吧..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEditTask">保存修改</el-button>
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
/* ✨ 新增：被抓起时的卡片虚影特效 */
.is-dragging {
  opacity: 0.4;
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
  border: 1px dashed #409EFF !important;
}
</style>
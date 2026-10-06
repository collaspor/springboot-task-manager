<script setup>
import { computed, onMounted, ref } from 'vue'
import { createTask, deleteTask, listTasks, updateTask } from './api.js'

const filters = [
  { value: 'all', label: '全部任务' },
  { value: 'active', label: '进行中' },
  { value: 'completed', label: '已完成' }
]

const filter = ref('all')
const tasks = ref([])
const allTasks = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const newTitle = ref('')
const newDescription = ref('')
const editingId = ref(null)
const editTitle = ref('')
const editDescription = ref('')

const completedCount = computed(() => allTasks.value.filter(task => task.completed).length)
const activeCount = computed(() => allTasks.value.length - completedCount.value)
const progress = computed(() => allTasks.value.length === 0 ? 0 : Math.round(completedCount.value / allTasks.value.length * 100))
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

async function loadTasks() {
  loading.value = true
  error.value = ''
  try {
    if (filter.value === 'all') {
      allTasks.value = await listTasks('all')
      tasks.value = allTasks.value
    } else {
      const [visible, completeList] = await Promise.all([listTasks(filter.value), listTasks('all')])
      tasks.value = visible
      allTasks.value = completeList
    }
  } catch (cause) {
    error.value = `加载任务失败：${cause.message}`
  } finally {
    loading.value = false
  }
}

async function selectFilter(value) {
  if (filter.value === value) return
  filter.value = value
  await loadTasks()
}

async function addTask() {
  const title = newTitle.value.trim()
  if (!title || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await createTask({ title, description: newDescription.value.trim() })
    newTitle.value = ''
    newDescription.value = ''
    await loadTasks()
  } catch (cause) {
    error.value = `新增任务失败：${cause.message}`
  } finally {
    saving.value = false
  }
}

function startEditing(task) {
  editingId.value = task.id
  editTitle.value = task.title
  editDescription.value = task.description || ''
  error.value = ''
}

function cancelEditing() {
  editingId.value = null
}

async function saveEdit(task) {
  const title = editTitle.value.trim()
  if (!title || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await updateTask(task.id, { title, description: editDescription.value.trim(), completed: task.completed })
    editingId.value = null
    await loadTasks()
  } catch (cause) {
    error.value = `保存任务失败：${cause.message}`
  } finally {
    saving.value = false
  }
}

async function toggleTask(task) {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    await updateTask(task.id, { title: task.title, description: task.description, completed: !task.completed })
    await loadTasks()
  } catch (cause) {
    error.value = `更新状态失败：${cause.message}`
  } finally {
    saving.value = false
  }
}

async function removeTask(task) {
  if (saving.value || !window.confirm(`确定删除「${task.title}」吗？`)) return
  saving.value = true
  error.value = ''
  try {
    await deleteTask(task.id)
    await loadTasks()
  } catch (cause) {
    error.value = `删除任务失败：${cause.message}`
  } finally {
    saving.value = false
  }
}

onMounted(loadTasks)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">✓</span><span>日日清单<small>DAILY NOTES</small></span></div>
      <div class="sidebar-main">
        <p class="eyebrow light">把今天，过得更清楚</p>
        <h1>一点一点，<br><em>完成重要的事。</em></h1>
        <p class="sidebar-copy">把想法写下来，给每个小进展一个位置。</p>
      </div>
      <div class="progress-card">
        <div class="progress-top"><span>完成进度</span><strong>{{ progress }}%</strong></div>
        <div class="progress-track"><span :style="{ width: `${progress}%` }"></span></div>
        <p>{{ completedCount }} 项已完成 · {{ activeCount }} 项待完成</p>
      </div>
      <p class="sidebar-footer">SPRING BOOT × VUE 3<br>学习项目 · 任务管理</p>
    </aside>

    <main class="workspace">
      <header class="page-header">
        <div><p class="eyebrow">{{ today }}</p><h2>我的任务<span class="title-dot">.</span></h2></div>
        <span class="header-count">{{ allTasks.length.toString().padStart(2, '0') }} <small>个任务</small></span>
      </header>

      <section class="composer" aria-labelledby="composer-title">
        <div class="section-heading"><span class="section-number">01 /</span><h3 id="composer-title">记录新的计划</h3></div>
        <form @submit.prevent="addTask">
          <label class="sr-only" for="new-title">任务标题</label>
          <input id="new-title" v-model="newTitle" type="text" maxlength="100" required placeholder="接下来要完成什么？" :disabled="saving">
          <label class="sr-only" for="new-description">任务描述</label>
          <textarea id="new-description" v-model="newDescription" maxlength="1000" rows="2" placeholder="补充一点细节（可选）" :disabled="saving"></textarea>
          <div class="composer-bottom"><span>先写下来，再一步步完成。</span><button class="primary-button" type="submit" :disabled="saving || !newTitle.trim()">{{ saving ? '处理中…' : '＋ 添加任务' }}</button></div>
        </form>
      </section>

      <section class="task-section" aria-labelledby="task-list-title">
        <div class="section-heading task-heading"><span class="section-number">02 /</span><h3 id="task-list-title">计划列表</h3><span class="section-rule"></span></div>
        <div class="filters" role="group" aria-label="筛选任务">
          <button v-for="item in filters" :key="item.value" type="button" :class="['filter-button', { active: filter === item.value }]" :aria-pressed="filter === item.value" @click="selectFilter(item.value)">{{ item.label }}</button>
        </div>

        <div v-if="error" class="error-banner" role="alert"><span>{{ error }}</span><button type="button" @click="loadTasks">重试</button></div>
        <div v-if="loading" class="state-panel" role="status">正在整理你的任务…</div>
        <div v-else-if="tasks.length === 0" class="state-panel empty-state"><span class="empty-icon">✳</span><strong>这里还没有任务</strong><p>{{ filter === 'all' ? '写下第一件想完成的事吧。' : '换个筛选条件，或新增一个任务。' }}</p></div>
        <ul v-else class="task-list">
          <li v-for="task in tasks" :key="task.id" :class="['task-card', { done: task.completed }]">
            <template v-if="editingId === task.id">
              <form class="edit-form" @submit.prevent="saveEdit(task)">
                <label class="sr-only" :for="`edit-title-${task.id}`">编辑任务标题</label>
                <input :id="`edit-title-${task.id}`" v-model="editTitle" type="text" maxlength="100" required :disabled="saving">
                <label class="sr-only" :for="`edit-description-${task.id}`">编辑任务描述</label>
                <textarea :id="`edit-description-${task.id}`" v-model="editDescription" maxlength="1000" rows="2" :disabled="saving"></textarea>
                <div class="edit-actions"><button class="text-button" type="button" @click="cancelEditing">取消</button><button class="small-primary" type="submit" :disabled="saving || !editTitle.trim()">保存修改</button></div>
              </form>
            </template>
            <template v-else>
              <button class="complete-button" type="button" :disabled="saving" :aria-label="task.completed ? `标记 ${task.title} 为未完成` : `完成 ${task.title}`" @click="toggleTask(task)"><span v-if="task.completed">✓</span></button>
              <div class="task-content"><h4>{{ task.title }}</h4><p v-if="task.description">{{ task.description }}</p><span class="task-date">{{ new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(new Date(task.createdAt)) }} 创建</span></div>
              <div class="task-actions"><button type="button" :disabled="saving" :aria-label="`编辑 ${task.title}`" @click="startEditing(task)">编辑</button><button type="button" :disabled="saving" :aria-label="`删除 ${task.title}`" @click="removeTask(task)">删除</button></div>
            </template>
          </li>
        </ul>
      </section>
    </main>
  </div>
</template>

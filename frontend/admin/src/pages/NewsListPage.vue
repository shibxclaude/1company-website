<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api.js'

const q = reactive({ keyword: '', status: '', page: 1, size: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const r = await api.newsList(q)
    rows.value = r.list
    total.value = r.total
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function toggle(row) {
  await api.newsStatus(row.id, row.status ? 0 : 1)
  ElMessage.success(row.status ? '已撤回为草稿' : '已发布')
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await api.deleteNews(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q.keyword" placeholder="标题" clearable style="width: 200px" @keyup.enter="q.page = 1; load()" />
      <el-select v-model="q.status" placeholder="全部状态" clearable style="width: 120px" @change="q.page = 1; load()">
        <el-option label="已发布" :value="1" /><el-option label="草稿" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="q.page = 1; load()">查询</el-button>
      <span class="spacer" />
      <el-button type="primary" icon="Plus" @click="$router.push('/news/new')">发布新闻</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="标题" min-width="260">
        <template #default="{ row }"><el-tag v-if="row.top" size="small" type="danger">置顶</el-tag> {{ row.title }}</template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="170" />
      <el-table-column prop="views" label="浏览" width="80" />
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '已发布' : '草稿' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/news/${row.id}`)">编辑</el-button>
          <el-button link :type="row.status ? 'warning' : 'success'" @click="toggle(row)">{{ row.status ? '撤回' : '发布' }}</el-button>
          <el-button v-if="row.status" link tag="a" :href="`/news/${row.id}`" target="_blank">查看</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
  </div>
</template>

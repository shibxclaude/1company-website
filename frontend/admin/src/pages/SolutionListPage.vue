<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { api, money } from '../api.js'

const q = reactive({ keyword: '', categoryId: '', status: '', page: 1, size: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])

async function load() {
  loading.value = true
  try {
    const r = await api.solutions(q)
    rows.value = r.list
    total.value = r.total
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  categories.value = await api.categories()
  load()
})

async function toggle(row) {
  const to = row.status === 1 ? 0 : 1
  await api.solutionStatus(row.id, to)
  ElMessage.success(to ? '已上架，前台即时可见' : '已下架')
  load()
}
async function saveSort(row) {
  await api.solutionSort(row.id, row.sort)
  ElMessage.success('排序已保存')
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？删除后前台不再展示，历史订单不受影响。`, '删除确认', { type: 'warning' })
  await api.deleteSolution(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q.keyword" placeholder="名称 / URL 标识" clearable style="width: 200px" @keyup.enter="q.page = 1; load()" />
      <el-select v-model="q.categoryId" placeholder="全部分类" clearable style="width: 140px" @change="q.page = 1; load()">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="q.status" placeholder="全部状态" clearable style="width: 120px" @change="q.page = 1; load()">
        <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="q.page = 1; load()">查询</el-button>
      <span class="spacer" />
      <el-button type="primary" icon="Plus" @click="$router.push('/solutions/new')">新增解决方案</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="主图" width="110">
        <template #default="{ row }"><img v-if="row.mainImage" :src="row.mainImage" class="thumb" alt="" /></template>
      </el-table-column>
      <el-table-column label="解决方案" min-width="220">
        <template #default="{ row }">
          <div><b>{{ row.name }}</b><el-tag v-if="row.recommended" size="small" type="danger" class="ml">首页推荐</el-tag></div>
          <a :href="`/solutions/${row.slug}`" target="_blank" class="muted">/solutions/{{ row.slug }}</a>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column label="最低价" width="130"><template #default="{ row }"><span class="money">{{ money(row.minPrice) }}</span></template></el-table-column>
      <el-table-column label="版本/增值服务" width="120"><template #default="{ row }">{{ row.versionCount }} / {{ row.serviceCount }}</template></el-table-column>
      <el-table-column label="排序" width="130">
        <template #default="{ row }"><el-input-number v-model="row.sort" size="small" :min="0" :max="9999" controls-position="right" style="width: 100px" @change="saveSort(row)" /></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '上架' : '下架' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/solutions/${row.id}`)">编辑</el-button>
          <el-button link :type="row.status ? 'warning' : 'success'" @click="toggle(row)">{{ row.status ? '下架' : '上架' }}</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>

<style scoped>
.thumb { width: 84px; height: 54px; object-fit: cover; border-radius: 3px; }
.ml { margin-left: 6px; }
.muted { text-decoration: none; }
</style>

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
    const r = await api.users(q)
    rows.value = r.list
    total.value = r.total
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function toggle(row) {
  const to = row.status ? 0 : 1
  if (!to) await ElMessageBox.confirm(`停用后该用户（${row.phone}）将立即退出登录且无法再登录，确定吗？`, '停用用户', { type: 'warning' })
  await api.userStatus(row.id, to)
  ElMessage.success(to ? '已启用' : '已停用')
  load()
}
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q.keyword" placeholder="手机号 / 账号" clearable style="width: 200px" @keyup.enter="q.page = 1; load()" />
      <el-select v-model="q.status" placeholder="全部状态" clearable style="width: 120px" @change="q.page = 1; load()">
        <el-option label="正常" :value="1" /><el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="q.page = 1; load()">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="已设密码" width="90"><template #default="{ row }">{{ row.hasPassword ? '是' : '否' }}</template></el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column prop="lastLoginAt" label="最近登录" width="170" />
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'danger'">{{ row.status ? '正常' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push({ path: '/orders', query: { keyword: row.phone } })">订单</el-button>
          <el-button link :type="row.status ? 'danger' : 'success'" @click="toggle(row)">{{ row.status ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
  </div>
</template>

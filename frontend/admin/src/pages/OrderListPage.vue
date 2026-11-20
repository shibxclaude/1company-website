<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api, money, ORDER_STATUS } from '../api.js'

const route = useRoute()
const q = reactive({ status: route.query.status || '', keyword: route.query.keyword || '', range: null, page: 1, size: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const r = await api.orders({ status: q.status, keyword: q.keyword, from: q.range?.[0], to: q.range?.[1], page: q.page, size: q.size })
    rows.value = r.list
    total.value = r.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
function search() { q.page = 1; load() }
</script>

<template>
  <div class="page-card">
    <el-radio-group v-model="q.status" class="tabs" @change="search">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button v-for="(s, k) in ORDER_STATUS" :key="k" :value="k">{{ s.label }}</el-radio-button>
    </el-radio-group>
    <div class="toolbar">
      <el-input v-model="q.keyword" placeholder="订单号 / 手机号 / 联系人 / 企业" clearable style="width: 260px" @keyup.enter="search" />
      <el-date-picker v-model="q.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="下单开始" end-placeholder="下单结束" style="width: 260px" @change="search" />
      <el-button type="primary" plain @click="search">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe @row-dblclick="(r) => $router.push(`/orders/${r.orderNo}`)">
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="solutionName" label="解决方案" min-width="180" />
      <el-table-column label="金额" width="130"><template #default="{ row }"><span class="money">{{ money(row.totalAmount) }}</span></template></el-table-column>
      <el-table-column label="联系人" min-width="170">
        <template #default="{ row }">{{ row.contactName }} {{ row.contactPhone }}<div class="muted">{{ row.companyName }}</div></template>
      </el-table-column>
      <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="ORDER_STATUS[row.status].type">{{ row.statusLabel }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="下单时间" width="170" />
      <el-table-column prop="paidAt" label="支付时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }"><el-button link type="primary" @click="$router.push(`/orders/${row.orderNo}`)">处理</el-button></template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
  </div>
</template>

<style scoped>
.tabs { margin-bottom: 14px; }
</style>

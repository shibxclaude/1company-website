<script setup>
import { ref, onMounted } from 'vue'
import { api, money } from '../api.js'

const d = ref(null)
onMounted(async () => { d.value = await api.dashboard() })
</script>

<template>
  <div v-if="d">
    <el-row :gutter="16">
      <el-col :xs="12" :sm="6"><el-card shadow="never"><el-statistic title="待支付订单" :value="d.ordersPendingPay" /></el-card></el-col>
      <el-col :xs="12" :sm="6"><el-card shadow="never"><el-statistic title="待服务订单（已支付/服务中）" :value="d.ordersToServe" /></el-card></el-col>
      <el-col :xs="12" :sm="6"><el-card shadow="never"><el-statistic title="今日新订单" :value="d.ordersToday" /></el-card></el-col>
      <el-col :xs="12" :sm="6"><el-card shadow="never"><div class="stat"><span>累计实收（已扣退款）</span><b>{{ money(d.paidAmountTotal) }}</b></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" class="row2">
      <el-col :xs="12" :sm="8"><el-card shadow="never"><el-statistic title="上架解决方案" :value="d.solutionsOnSale"><template #suffix>/ {{ d.solutionsTotal }}</template></el-statistic></el-card></el-col>
      <el-col :xs="12" :sm="8"><el-card shadow="never"><el-statistic title="已发布新闻" :value="d.newsPublished" /></el-card></el-col>
      <el-col :xs="12" :sm="8"><el-card shadow="never"><el-statistic title="注册用户" :value="d.users" /></el-card></el-col>
    </el-row>
    <el-card shadow="never" class="row2">
      <template #header>快捷操作</template>
      <el-space wrap>
        <el-button type="primary" @click="$router.push('/solutions/new')">新增解决方案</el-button>
        <el-button @click="$router.push('/orders?status=PAID')">处理已支付订单</el-button>
        <el-button @click="$router.push('/news/new')">发布新闻</el-button>
        <el-link href="/price" target="_blank" type="primary">查看前台价格公示</el-link>
      </el-space>
    </el-card>
  </div>
</template>

<style scoped>
.row2 { margin-top: 16px; }
.el-col { margin-bottom: 16px; }
.stat { display: flex; flex-direction: column; gap: 4px; }
.stat span { font-size: 14px; color: #909399; }
.stat b { font-size: 22px; color: #e4393c; }
</style>

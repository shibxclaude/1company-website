<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, money, ORDER_STATUS } from '../api.js'

const route = useRoute()
const d = ref(null)
const form = reactive({ toStatus: '', note: '' })
const saving = ref(false)

async function load() { d.value = await api.order(route.params.orderNo) }
onMounted(load)

async function submit() {
  if (!form.toStatus && !form.note.trim()) return ElMessage.warning('请选择新状态或填写进度备注')
  if (form.toStatus === 'CANCELLED') await ElMessageBox.confirm('确定取消该待支付订单吗？', '确认', { type: 'warning' })
  saving.value = true
  try {
    d.value = await api.progress(route.params.orderNo, { toStatus: form.toStatus || null, note: form.note })
    ElMessage.success('已更新，客户可在用户中心看到最新进度')
    form.toStatus = ''
    form.note = ''
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-if="d" class="wrap">
    <el-card shadow="never">
      <template #header>
        <div class="hd">
          <span>订单 {{ d.order.orderNo }}</span>
          <el-tag :type="ORDER_STATUS[d.order.status].type" size="large">{{ d.order.statusLabel }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="解决方案">{{ d.order.solutionName }}</el-descriptions-item>
        <el-descriptions-item label="订单金额"><span class="money">{{ money(d.order.totalAmount) }}</span></el-descriptions-item>
        <el-descriptions-item label="联系人">{{ d.order.contactName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ d.order.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ d.order.contactEmail }}</el-descriptions-item>
        <el-descriptions-item label="企业名称">{{ d.order.companyName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ d.order.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="下单方式">{{ d.userId ? `注册用户（ID ${d.userId}）` : '游客' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ d.order.paidAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式 / 交易号">{{ d.order.payChannel || '—' }} {{ d.transactionId || '' }}</el-descriptions-item>
        <el-descriptions-item label="协议版本">{{ d.order.agreementVersion }}</el-descriptions-item>
        <el-descriptions-item label="支付截止">{{ d.order.payExpireAt }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ d.order.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>订单明细</template>
      <el-table :data="d.order.items" border size="small">
        <el-table-column label="类型" width="100"><template #default="{ row }">{{ row.itemType === 'VERSION' ? '版本' : '增值服务' }}</template></el-table-column>
        <el-table-column prop="itemName" label="项目" />
        <el-table-column label="单价" width="160"><template #default="{ row }">{{ money(row.unitPrice) }}/{{ row.unit }}</template></el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="小计" width="150"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>服务进度</template>
      <el-timeline>
        <el-timeline-item v-for="(l, i) in [...d.order.logs].reverse()" :key="i" :timestamp="l.createdAt" placement="top">
          <b>{{ l.toStatusLabel }}</b>
          <span class="muted"> · {{ { USER: '客户', GUEST: '游客', ADMIN: '管理员', SYSTEM: '系统' }[l.operatorType] || l.operatorType }}</span>
          <div v-if="l.note">{{ l.note }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-divider />
      <el-form label-width="90px" style="max-width: 640px">
        <el-form-item label="变更状态">
          <el-select v-model="form.toStatus" placeholder="不变更（仅记录进度）" clearable style="width: 240px">
            <el-option v-for="s in d.nextStatuses" :key="s" :label="ORDER_STATUS[s].label" :value="s" />
          </el-select>
          <span v-if="!d.nextStatuses.length" class="muted" style="margin-left: 8px">当前状态无可变更项</span>
        </el-form-item>
        <el-form-item label="进度备注">
          <el-input v-model="form.note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="如：已安排工程师远程部署；客户可在用户中心看到" />
        </el-form-item>
        <el-form-item>
          <el-button @click="$router.push('/orders')">返回列表</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.wrap { display: flex; flex-direction: column; gap: 16px; }
.hd { display: flex; justify-content: space-between; align-items: center; }
</style>

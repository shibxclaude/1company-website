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

const REFUND_STATUS = { PROCESSING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
const refundDlg = reactive({ visible: false, amount: '', reason: '', saving: false })
function openRefund() {
  Object.assign(refundDlg, { visible: true, amount: d.value.refundableAmount, reason: '' })
}
async function submitRefund() {
  const amount = Number(refundDlg.amount)
  if (!(amount > 0)) return ElMessage.warning('请填写正确的退款金额')
  if (amount > Number(d.value.refundableAmount)) return ElMessage.warning(`最多可退 ${money(d.value.refundableAmount)}`)
  if (!refundDlg.reason.trim()) return ElMessage.warning('请填写退款原因')
  await ElMessageBox.confirm(`将原路退回 ${money(amount)}，提交后无法撤回。确定退款吗？`, '确认退款', { type: 'warning' })
  refundDlg.saving = true
  try {
    d.value = await api.refund(route.params.orderNo, { amount: refundDlg.amount, reason: refundDlg.reason.trim() })
    refundDlg.visible = false
    afterRefund()
  } finally {
    refundDlg.saving = false
  }
}
async function refundStray(p) {
  const { value: reason } = await ElMessageBox.prompt(`将整笔退回交易 ${p.transactionId}（${money(p.amount)}）。请填写退款原因：`, '退回异常收款',
    { inputValue: '重复付款', inputValidator: (v) => (v && v.trim() ? true : '请填写退款原因'), type: 'warning' })
  d.value = await api.refundStray(route.params.orderNo, { transactionId: p.transactionId, reason: reason.trim() })
  afterRefund()
}
function afterRefund() {
  const latest = d.value.refunds[0]
  if (latest?.status === 'SUCCESS') ElMessage.success('退款成功，款项已原路退回')
  else if (latest?.status === 'FAILED') ElMessage.error(`退款失败：${latest.failReason || '支付平台拒绝'}`)
  else ElMessage.info('退款已提交，支付平台处理中，结果会自动更新')
}

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
        <el-descriptions-item label="订单金额">
          <span class="money">{{ money(d.order.totalAmount) }}</span>
          <span v-if="Number(d.order.refundedAmount) > 0" class="refunded">（已退 {{ money(d.order.refundedAmount) }}）</span>
        </el-descriptions-item>
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

    <el-card v-if="Number(d.refundableAmount) > 0 || d.refunds.length || d.abnormalPayments.length" shadow="never">
      <template #header>
        <div class="hd">
          <span>退款（原路退回）</span>
          <el-button v-if="Number(d.refundableAmount) > 0" type="danger" plain size="small" @click="openRefund">
            发起退款（可退 {{ money(d.refundableAmount) }}）
          </el-button>
        </div>
      </template>
      <el-alert v-if="d.abnormalPayments.some((p) => !p.refunded)" type="error" :closable="false" show-icon style="margin-bottom: 12px"
        title="存在未入账的异常收款（如重复付款），钱已扣但没有计入订单，请核实后退回" />
      <el-table v-if="d.abnormalPayments.length" :data="d.abnormalPayments" border size="small" style="margin-bottom: 12px">
        <el-table-column label="异常收款交易号" prop="transactionId" min-width="200" />
        <el-table-column label="金额" width="140"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="原因" prop="note" min-width="200" />
        <el-table-column label="时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.refunded" type="info" size="small">已退回/退回中</el-tag>
            <el-button v-else type="danger" link @click="refundStray(row)">整笔退回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-table v-if="d.refunds.length" :data="d.refunds" border size="small">
        <el-table-column label="退款单号" prop="refundNo" min-width="200" />
        <el-table-column label="类型" width="110"><template #default="{ row }">{{ row.kind === 'STRAY' ? '异常收款退回' : '订单退款' }}</template></el-table-column>
        <el-table-column label="金额" width="130"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tooltip v-if="row.failReason" :content="row.failReason"><el-tag :type="REFUND_STATUS[row.status]" size="small">{{ row.statusLabel }}</el-tag></el-tooltip>
            <el-tag v-else :type="REFUND_STATUS[row.status]" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="160" />
        <el-table-column label="操作人" prop="operatorName" width="100" />
        <el-table-column label="发起 / 完成时间" width="180">
          <template #default="{ row }">{{ row.createdAt }}<br /><span class="muted">{{ row.finishedAt || '—' }}</span></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="refundDlg.visible" title="发起退款" width="460px">
      <el-form label-width="80px">
        <el-form-item label="退款金额">
          <el-input v-model="refundDlg.amount" style="width: 200px"><template #prepend>¥</template></el-input>
          <div class="muted tip">最多 {{ money(d.refundableAmount) }}；全额退完后订单变为「已退款」</div>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundDlg.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="客户可在订单进度中看到" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDlg.visible = false">取消</el-button>
        <el-button type="danger" :loading="refundDlg.saving" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>

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
.refunded { color: var(--el-color-danger); margin-left: 6px; }
.tip { width: 100%; font-size: 12px; line-height: 1.6; }
</style>

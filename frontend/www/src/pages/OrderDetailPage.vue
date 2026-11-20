<script setup>
// 订单详情：明细、状态与服务进度、联系人、电子协议。登录用户或持有订单凭证的游客可查看。
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useHead } from '@unhead/vue'
import StateBlock from '../components/StateBlock.vue'
import { api } from '../api/http.js'
import { money } from '../utils/money.js'
import { resolveOrderToken, orderLink } from '../utils/orderTokens.js'

useHead({ title: '订单详情 - 北京一零科技有限公司', meta: [{ name: 'robots', content: 'noindex' }] })

const route = useRoute()
const orderNo = String(route.params.orderNo)
const token = resolveOrderToken(route, orderNo)

const order = ref(null)
const loading = ref(true)
const error = ref('')
const cancelling = ref(false)

const STEPS = [
  ['PENDING_PAY', '提交订单'],
  ['PAID', '支付成功'],
  ['IN_SERVICE', '服务进行中'],
  ['COMPLETED', '已完成'],
]
const stepIndex = computed(() => STEPS.findIndex(([s]) => s === order.value?.status))
const payLink = computed(() => orderLink(`/pay/${orderNo}`, token))

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await api.order(orderNo, token)
  } catch (e) {
    error.value = e.status === 404 ? '订单不存在，或您没有查看该订单的权限。请登录下单手机号对应的账号后查看。' : e.message
  } finally {
    loading.value = false
  }
}

async function cancel() {
  if (!window.confirm('确定取消该订单吗？')) return
  cancelling.value = true
  try {
    await api.cancelOrder(orderNo, token)
    await load()
  } catch (e) {
    window.alert(e.message)
  } finally {
    cancelling.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <p class="back"><RouterLink to="/user/orders">‹ 我的订单</RouterLink></p>
    <StateBlock :loading="loading" :error="error" @retry="load">
      <template v-if="order">
        <section class="card">
          <div class="head">
            <div>
              <h1>{{ order.statusLabel }}</h1>
              <p class="no">订单号 {{ order.orderNo }}　下单时间 {{ order.createdAt }}</p>
            </div>
            <div class="ops">
              <template v-if="order.status === 'PENDING_PAY'">
                <RouterLink :to="payLink" class="pay">去支付</RouterLink>
                <button type="button" class="cancel" :disabled="cancelling" @click="cancel">取消订单</button>
              </template>
            </div>
          </div>
          <ol v-if="order.status !== 'CANCELLED'" class="steps">
            <li v-for="([s, l], i) in STEPS" :key="s" :class="{ done: i <= stepIndex }">{{ l }}</li>
          </ol>
          <p v-else class="cancelled">订单已取消：{{ order.cancelReason }}（{{ order.cancelledAt }}）</p>
        </section>

        <section class="card">
          <h2>订单明细</h2>
          <p class="sol">
            <RouterLink v-if="order.solutionSlug" :to="`/solutions/${order.solutionSlug}`">{{ order.solutionName }}</RouterLink>
            <span v-else>{{ order.solutionName }}</span>
          </p>
          <table>
            <thead><tr><th>项目</th><th class="num">单价</th><th class="num">数量</th><th class="num">小计</th></tr></thead>
            <tbody>
              <tr v-for="i in order.items" :key="i.itemName">
                <td>{{ i.itemName }}<small>{{ i.itemType === 'VERSION' ? '版本' : '增值服务' }}</small></td>
                <td class="num">{{ money(i.unitPrice) }}/{{ i.unit }}</td>
                <td class="num">{{ i.quantity }}</td>
                <td class="num">{{ money(i.amount) }}</td>
              </tr>
            </tbody>
          </table>
          <p class="total">订单金额 <strong>{{ money(order.totalAmount) }}</strong></p>
          <p v-if="order.paidAt" class="paid">支付时间 {{ order.paidAt }}　支付方式 {{ order.payChannel === 'MOCK' ? '模拟支付' : '微信支付' }}</p>
        </section>

        <section class="card">
          <h2>服务进度</h2>
          <ul class="logs">
            <li v-for="(l, i) in [...order.logs].reverse()" :key="i">
              <time>{{ l.createdAt }}</time>
              <strong>{{ l.toStatusLabel }}</strong>
              <span v-if="l.note">{{ l.note }}</span>
            </li>
          </ul>
        </section>

        <section class="card two">
          <div>
            <h2>联系人</h2>
            <p>{{ order.contactName }}　{{ order.contactPhone }}</p>
            <p>{{ order.contactEmail }}</p>
            <p v-if="order.companyName">{{ order.companyName }}</p>
            <p v-if="order.remark" class="remark">备注：{{ order.remark }}</p>
          </div>
          <div>
            <h2>电子协议</h2>
            <p><RouterLink to="/agreement/purchase" target="_blank">《产品购买服务协议》</RouterLink>（{{ order.agreementVersion }}，下单时已同意）</p>
            <p><RouterLink to="/agreement/privacy" target="_blank">《隐私政策》</RouterLink></p>
          </div>
        </section>
      </template>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; padding: 110px 16px 0; }
.back { margin-bottom: 12px; font-size: 14px; }
.back a { color: var(--c-brand); }
.card { margin-bottom: 16px; padding: 20px 24px; background: #fff; border: 1px solid var(--c-line); border-radius: 8px; }
.card h2 { margin-bottom: 12px; font-size: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head h1 { font-size: 22px; }
.no { margin-top: 6px; font-size: 13px; color: #888; }
.ops { display: flex; gap: 10px; align-items: center; }
.pay { padding: 8px 22px; color: #fff; background: var(--c-brand); border-radius: var(--radius-sm); }
.cancel { padding: 8px 18px; color: #666; background: #fff; border: 1px solid #ddd; border-radius: var(--radius-sm); }
.steps { display: flex; margin-top: 22px; counter-reset: s; }
.steps li { flex: 1; position: relative; padding-top: 30px; text-align: center; font-size: 13px; color: #aaa; }
.steps li::before {
  counter-increment: s;
  content: counter(s);
  position: absolute;
  top: 0;
  left: 50%;
  width: 22px;
  height: 22px;
  margin-left: -11px;
  line-height: 22px;
  color: #fff;
  background: #ccc;
  border-radius: 50%;
  z-index: 1;
}
.steps li::after { content: ''; position: absolute; top: 10px; left: -50%; width: 100%; height: 2px; background: #e5e5e5; }
.steps li:first-child::after { display: none; }
.steps li.done { color: var(--c-brand); }
.steps li.done::before { background: var(--c-brand); }
.steps li.done::after { background: var(--c-brand); }
.cancelled { margin-top: 12px; font-size: 14px; color: #999; }
.sol { margin-bottom: 8px; font-weight: 600; }
.sol a { color: var(--c-brand); }
table { width: 100%; border-collapse: collapse; font-size: 14px; }
th, td { padding: 9px 6px; text-align: left; border-bottom: 1px solid #f0f0f0; }
th { font-weight: 500; color: #888; }
td small { margin-left: 6px; font-size: 12px; color: #999; }
.num { text-align: right; white-space: nowrap; }
.total { margin-top: 12px; text-align: right; }
.total strong { font-size: 20px; color: var(--c-price); }
.paid { margin-top: 4px; text-align: right; font-size: 13px; color: #888; }
.logs li { display: grid; grid-template-columns: 150px 100px 1fr; gap: 8px; padding: 8px 0; font-size: 14px; border-bottom: 1px dashed #eee; }
.logs time { color: #999; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.two p { font-size: 14px; line-height: 1.9; color: #555; }
.two a { color: var(--c-brand); }
.remark { color: #888 !important; }
@media (max-width: 991px) { .page { padding-top: 80px; } }
@media (max-width: 599px) {
  .card { padding: 16px; }
  .logs li { grid-template-columns: 1fr; gap: 2px; }
  .two { grid-template-columns: 1fr; }
}
</style>

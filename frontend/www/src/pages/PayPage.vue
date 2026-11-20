<script setup>
// 支付页：微信 Native 扫码。显示二维码、倒计时（以服务端时间为准）、轮询支付状态；
// 开发/演示环境（模拟支付）显示"模拟支付成功"按钮。
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useHead } from '@unhead/vue'
import StateBlock from '../components/StateBlock.vue'
import { api } from '../api/http.js'
import { money } from '../utils/money.js'
import { resolveOrderToken, orderLink } from '../utils/orderTokens.js'

useHead({ title: '订单支付 - 北京一零科技有限公司', meta: [{ name: 'robots', content: 'noindex' }] })

const route = useRoute()
const orderNo = String(route.params.orderNo)
const token = resolveOrderToken(route, orderNo)

const order = ref(null)
const pay = ref(null)
const qr = ref('')
const status = ref('')
const loading = ref(true)
const error = ref('')
const remain = ref(0) // 剩余秒数
const mockPaying = ref(false)
let skew = 0 // 服务端时间 - 本地时间
let tick = null
let poll = null

const parseTime = (s) => new Date(s.replace(' ', 'T') + '+08:00').getTime()
const mmss = computed(() => `${String(Math.floor(remain.value / 60)).padStart(2, '0')}:${String(remain.value % 60).padStart(2, '0')}`)
const detailLink = computed(() => orderLink(`/user/orders/${orderNo}`, token))

function startCountdown(expireAt, serverTime) {
  skew = parseTime(serverTime) - Date.now()
  const end = parseTime(expireAt)
  const update = () => {
    remain.value = Math.max(0, Math.floor((end - (Date.now() + skew)) / 1000))
    if (remain.value === 0) checkStatus()
  }
  update()
  tick = setInterval(update, 1000)
}

async function checkStatus() {
  try {
    const s = await api.orderStatus(orderNo, token)
    status.value = s.status
    if (s.status !== 'PENDING_PAY') stop()
  } catch { /* 网络抖动时下次再查 */ }
}

function stop() {
  clearInterval(tick)
  clearInterval(poll)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await api.order(orderNo, token)
    status.value = order.value.status
    if (status.value === 'PENDING_PAY') {
      pay.value = await api.pay(orderNo, token)
      const QRCode = (await import('qrcode')).default
      qr.value = await QRCode.toDataURL(pay.value.codeUrl, { width: 240, margin: 1, errorCorrectionLevel: 'M' })
      startCountdown(pay.value.payExpireAt, pay.value.serverTime)
      poll = setInterval(checkStatus, 3000)
    }
  } catch (e) {
    error.value = e.status === 404 ? '订单不存在，或您没有查看该订单的权限（请使用下单时的浏览器，或登录下单手机号对应的账号）' : e.message
    if (e.status === 409) await checkStatus()
  } finally {
    loading.value = false
  }
}

async function mockSucceed() {
  mockPaying.value = true
  try {
    await api.mockPay(orderNo, token)
    await checkStatus()
  } catch (e) {
    error.value = e.message
  } finally {
    mockPaying.value = false
  }
}

onMounted(load)
onBeforeUnmount(stop)
</script>

<template>
  <div class="page">
    <StateBlock :loading="loading" :error="error && !order ? error : ''" @retry="load">
      <div v-if="order" class="card">
        <header class="head">
          <div>
            <p class="label">订单号</p>
            <p class="no">{{ orderNo }}</p>
          </div>
          <div class="amount">
            <p class="label">应付金额</p>
            <p class="money">{{ money(order.totalAmount) }}</p>
          </div>
        </header>
        <p class="sol">{{ order.solutionName }}<span v-for="i in order.items" :key="i.itemName">　{{ i.itemName }} × {{ i.quantity }}{{ i.unit }}</span></p>

        <!-- 待支付 -->
        <section v-if="status === 'PENDING_PAY' && pay" class="paybox" aria-live="polite">
          <template v-if="remain > 0">
            <p class="countdown">请在 <strong>{{ mmss }}</strong> 内完成支付，超时订单将自动关闭</p>
            <div class="qr">
              <img v-if="qr" :src="qr" alt="微信支付二维码" width="240" height="240" />
            </div>
            <p class="scan"><span class="wx" aria-hidden="true" />请使用微信扫一扫完成支付</p>
            <p class="waiting">支付完成后页面将自动跳转…</p>
            <div v-if="pay.mock" class="mock">
              <p>当前为<strong>模拟支付模式</strong>（开发/演示环境，未接入真实微信支付）</p>
              <button type="button" :disabled="mockPaying" @click="mockSucceed">{{ mockPaying ? '处理中…' : '模拟支付成功' }}</button>
            </div>
          </template>
          <p v-else class="closed">支付已超时，订单正在关闭…</p>
        </section>

        <!-- 已支付 -->
        <section v-else-if="['PAID', 'IN_SERVICE', 'COMPLETED'].includes(status)" class="result ok" role="status">
          <span class="icon" aria-hidden="true">✓</span>
          <h1>支付成功</h1>
          <p>我们已通过短信和邮件向 {{ order.contactPhone }} / {{ order.contactEmail }} 发送订单确认，工作人员将在 1 个工作日内与您联系安排交付。</p>
          <div class="btns">
            <RouterLink :to="detailLink" class="primary">查看订单详情</RouterLink>
            <RouterLink to="/user/orders" class="ghost">进入用户中心</RouterLink>
          </div>
        </section>

        <!-- 已关闭 -->
        <section v-else-if="status === 'CANCELLED'" class="result" role="status">
          <span class="icon gray" aria-hidden="true">!</span>
          <h1>订单已关闭</h1>
          <p>{{ order.cancelReason || '订单已取消' }}。如需购买请重新下单。</p>
          <div class="btns">
            <RouterLink :to="order.solutionSlug ? `/solutions/${order.solutionSlug}#buy` : '/solutions'" class="primary">重新下单</RouterLink>
          </div>
        </section>
        <p v-if="error && order" class="err" role="alert">{{ error }}</p>
      </div>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 120px 16px 0; }
.card { padding: 28px 32px; background: #fff; border: 1px solid var(--c-line); border-radius: 8px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.label { font-size: 13px; color: #999; }
.no { margin-top: 4px; font-size: 16px; font-family: Consolas, monospace; letter-spacing: 0.5px; }
.amount { text-align: right; }
.money { margin-top: 2px; font-size: 28px; font-weight: 700; color: var(--c-price); }
.sol { margin-top: 12px; padding-bottom: 16px; font-size: 14px; color: #666; border-bottom: 1px solid #f0f0f0; }
.paybox { padding-top: 22px; text-align: center; }
.countdown { font-size: 15px; color: #555; }
.countdown strong { color: var(--c-price); font-size: 18px; font-variant-numeric: tabular-nums; }
.qr { display: inline-block; margin: 18px 0 10px; padding: 10px; border: 1px solid #eee; border-radius: 8px; }
.qr img { display: block; width: 240px; height: 240px; }
.scan { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 15px; }
.wx { width: 22px; height: 22px; border-radius: 50%; background: #07c160; }
.waiting { margin-top: 6px; font-size: 13px; color: #999; }
.mock { margin-top: 22px; padding: 14px; font-size: 13px; color: #8a6d3b; background: #fcf8e3; border-radius: 6px; }
.mock button { margin-top: 10px; padding: 8px 26px; font-size: 15px; color: #fff; background: #07c160; border: 0; border-radius: var(--radius-sm); }
.mock button:disabled { background: #9ad8b5; }
.closed { padding: 40px 0; color: #999; }
.result { padding: 30px 0 10px; text-align: center; }
.result .icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  font-size: 36px;
  color: #fff;
  background: #07c160;
  border-radius: 50%;
}
.result .icon.gray { background: #bbb; }
.result h1 { margin-top: 14px; font-size: 22px; }
.result p { margin: 10px auto 0; max-width: 520px; font-size: 14px; line-height: 1.8; color: #666; }
.btns { display: flex; justify-content: center; gap: 12px; margin-top: 22px; flex-wrap: wrap; }
.btns a { padding: 9px 26px; font-size: 15px; border-radius: var(--radius-sm); }
.primary { color: #fff; background: var(--c-brand); }
.ghost { color: var(--c-brand); border: 1px solid var(--c-brand); }
.err { margin-top: 14px; font-size: 14px; color: #c0392b; text-align: center; }
@media (max-width: 991px) { .page { padding-top: 84px; } }
@media (max-width: 599px) {
  .card { padding: 20px 16px; }
  .amount { text-align: left; }
}
</style>

<script setup>
// 下单确认：已选方案/版本/数量/增值服务与金额汇总 + 联系人表单。游客可直接提交，无需注册。
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHead } from '@unhead/vue'
import StateBlock from '../components/StateBlock.vue'
import QtyInput from '../components/QtyInput.vue'
import { useCatalogStore } from '../stores/catalog.js'
import { useUserStore } from '../stores/user.js'
import { api } from '../api/http.js'
import { estimateTotal, formatCents, money } from '../utils/money.js'
import { saveOrderToken } from '../utils/orderTokens.js'

useHead({ title: '确认订单 - 北京一零科技有限公司', meta: [{ name: 'robots', content: 'noindex' }] })

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()
const user = useUserStore()

const slug = String(route.query.slug || '')
const solution = computed(() => catalog.details[slug])
const loading = ref(true)
const loadError = ref('')

const versionId = ref(Number(route.query.v) || null)
const quantity = ref(Math.min(99, Math.max(1, Number(route.query.q) || 1)))
const serviceQty = ref(
  Object.fromEntries(
    String(route.query.s || '')
      .split(',')
      .filter(Boolean)
      .map((p) => p.split(':').map(Number))
      .filter(([id, n]) => id > 0 && n > 0)
      .map(([id, n]) => [id, Math.min(99, n)]),
  ),
)

const version = computed(() => solution.value?.versions.find((v) => v.id === versionId.value) || null)
const services = computed(() =>
  (solution.value?.services || []).filter((s) => serviceQty.value[s.id] > 0).map((s) => ({ ...s, quantity: serviceQty.value[s.id] })),
)
const totalCents = computed(() => estimateTotal(version.value, quantity.value, services.value))

const CONTACT_KEY = 'cn01_last_contact'
const form = reactive({ contactName: '', contactPhone: '', contactEmail: '', companyName: '', remark: '', agreed: false })
const errors = reactive({})
const submitting = ref(false)
const submitError = ref('')

onMounted(async () => {
  try {
    Object.assign(form, JSON.parse(localStorage.getItem(CONTACT_KEY) || '{}'), { agreed: false, remark: '' })
  } catch { /* ignore */ }
  if (!slug) {
    loadError.value = '缺少解决方案参数，请从解决方案页面选择后购买'
    loading.value = false
    return
  }
  try {
    await catalog.loadDetail(slug, { force: true })
    if (solution.value && !version.value) versionId.value = solution.value.versions[0]?.id ?? null
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
})
watch(() => user.user, (u) => { if (u && !form.contactPhone) form.contactPhone = u.phone }, { immediate: true })

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!form.contactName.trim()) errors.contactName = '请填写姓名'
  if (!/^1[3-9]\d{9}$/.test(form.contactPhone)) errors.contactPhone = '请填写正确的手机号'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.contactEmail.trim())) errors.contactEmail = '请填写正确的邮箱'
  if (!form.agreed) errors.agreed = '请阅读并同意《产品购买服务协议》'
  return Object.keys(errors).length === 0
}

async function submit() {
  submitError.value = ''
  if (!validate() || !version.value) return
  submitting.value = true
  try {
    const body = {
      slug,
      versionId: version.value.id,
      quantity: quantity.value,
      services: services.value.map((s) => ({ serviceId: s.id, quantity: s.quantity })),
      contactName: form.contactName.trim(),
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail.trim(),
      companyName: form.companyName.trim() || undefined,
      remark: form.remark.trim() || undefined,
      agreed: form.agreed,
    }
    const created = await api.createOrder(body)
    saveOrderToken(created.orderNo, created.accessToken)
    try {
      const { contactName, contactPhone, contactEmail, companyName } = body
      localStorage.setItem(CONTACT_KEY, JSON.stringify({ contactName, contactPhone, contactEmail, companyName: companyName || '' }))
    } catch { /* ignore */ }
    router.replace({ path: `/pay/${created.orderNo}`, query: { t: created.accessToken } })
  } catch (e) {
    submitError.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1 class="page-title">确认订单</h1>
    <StateBlock :loading="loading" :error="loadError || (solution === null ? '该解决方案已下架' : '')" @retry="$router.go(0)">
      <div v-if="solution" class="layout">
        <section class="card summary" aria-label="订单内容">
          <h2>订单内容</h2>
          <div class="sol">
            <img :src="solution.mainImage" :alt="solution.name" width="120" height="78" />
            <div>
              <RouterLink :to="`/solutions/${solution.slug}`" class="name">{{ solution.name }}</RouterLink>
              <p class="meta">交付方式：{{ solution.deliveryLabel }}　{{ solution.servicePeriod }}</p>
            </div>
          </div>
          <table class="items">
            <thead><tr><th>项目</th><th class="num">单价</th><th class="num">数量</th><th class="num">小计</th></tr></thead>
            <tbody>
              <tr v-if="version">
                <td>
                  <select v-model="versionId" aria-label="版本">
                    <option v-for="v in solution.versions" :key="v.id" :value="v.id">{{ v.name }}（{{ v.billingLabel }}）</option>
                  </select>
                </td>
                <td class="num">{{ money(version.price) }}/{{ version.unit }}</td>
                <td class="num"><QtyInput v-model="quantity" label="购买数量" /></td>
                <td class="num">¥{{ formatCents(estimateTotal(version, quantity)) }}</td>
              </tr>
              <tr v-for="s in services" :key="s.id">
                <td>{{ s.name }}<small>增值服务</small></td>
                <td class="num">{{ money(s.price) }}/{{ s.unit }}</td>
                <td class="num">
                  <QtyInput :model-value="s.quantity" :min="0" :label="`${s.name}数量`" @update:model-value="serviceQty = { ...serviceQty, [s.id]: $event }" />
                </td>
                <td class="num">¥{{ formatCents(estimateTotal({ price: s.price }, s.quantity)) }}</td>
              </tr>
            </tbody>
          </table>
          <p class="total">应付金额 <strong>¥{{ formatCents(totalCents) }}</strong></p>
          <p class="tip">金额以提交后服务端计算为准；订单提交后请在 30 分钟内完成支付。</p>
        </section>

        <form class="card form" novalidate @submit.prevent="submit">
          <h2>联系人信息</h2>
          <p v-if="!user.loggedIn" class="guest">无需注册即可下单。<RouterLink :to="{ path: '/user/login', query: { redirect: $route.fullPath } }">登录</RouterLink>后可在用户中心统一管理订单。</p>
          <label :class="{ err: errors.contactName }">
            <span>姓名<i>*</i></span>
            <input v-model="form.contactName" maxlength="50" autocomplete="name" />
            <em v-if="errors.contactName">{{ errors.contactName }}</em>
          </label>
          <label :class="{ err: errors.contactPhone }">
            <span>手机号<i>*</i></span>
            <input v-model="form.contactPhone" type="tel" maxlength="11" inputmode="numeric" autocomplete="tel" />
            <em v-if="errors.contactPhone">{{ errors.contactPhone }}</em>
          </label>
          <label :class="{ err: errors.contactEmail }">
            <span>邮箱<i>*</i></span>
            <input v-model="form.contactEmail" type="email" maxlength="100" autocomplete="email" />
            <em v-if="errors.contactEmail">{{ errors.contactEmail }}</em>
          </label>
          <label>
            <span>企业名称</span>
            <input v-model="form.companyName" maxlength="100" placeholder="选填" autocomplete="organization" />
          </label>
          <label>
            <span>备注</span>
            <textarea v-model="form.remark" maxlength="500" rows="3" placeholder="选填，如期望的部署时间、开票信息等" />
          </label>
          <label class="agree" :class="{ err: errors.agreed }">
            <input v-model="form.agreed" type="checkbox" />
            <span>我已阅读并同意 <RouterLink to="/agreement/purchase" target="_blank">《产品购买服务协议》</RouterLink> 和 <RouterLink to="/agreement/privacy" target="_blank">《隐私政策》</RouterLink></span>
          </label>
          <em v-if="errors.agreed" class="agree-err">{{ errors.agreed }}</em>
          <p v-if="submitError" class="submit-err" role="alert">{{ submitError }}</p>
          <button type="submit" class="submit" :disabled="submitting || !version">{{ submitting ? '提交中…' : `提交订单 ¥${formatCents(totalCents)}` }}</button>
        </form>
      </div>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { max-width: var(--wide-w); margin: 0 auto; padding: 120px 16px 0; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.layout { display: grid; grid-template-columns: 1.3fr 1fr; gap: 24px; align-items: start; }
.card { padding: 22px 24px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; }
.card h2 { font-size: 17px; margin-bottom: 16px; }
.sol { display: flex; gap: 14px; align-items: center; padding-bottom: 14px; border-bottom: 1px solid #f0f0f0; }
.sol img { width: 120px; height: 78px; object-fit: cover; border-radius: 4px; }
.name { font-size: 16px; font-weight: 600; }
.name:hover { color: var(--c-primary); }
.meta { margin-top: 6px; font-size: 13px; color: #888; }
.items { width: 100%; margin-top: 10px; border-collapse: collapse; font-size: 14px; }
.items th, .items td { padding: 10px 6px; border-bottom: 1px solid #f0f0f0; text-align: left; }
.items th { font-weight: 500; color: #888; }
.items small { display: block; font-size: 12px; color: #999; }
.items select { max-width: 100%; padding: 6px 8px; font-size: 14px; border: 1px solid #dcdfe6; border-radius: 4px; }
.num { text-align: right !important; white-space: nowrap; }
.total { margin-top: 16px; text-align: right; font-size: 15px; }
.total strong { font-size: 26px; color: #e4393c; }
.tip { margin-top: 6px; text-align: right; font-size: 12px; color: #999; }
.guest { margin: -6px 0 14px; font-size: 13px; color: #888; }
.guest a, .agree a { color: var(--c-primary); }
.form label { display: block; margin-bottom: 14px; }
.form label > span { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form i { margin-left: 2px; color: #e4393c; font-style: normal; }
.form input:not([type='checkbox']), .form textarea {
  width: 100%;
  padding: 9px 12px;
  font: inherit;
  font-size: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
}
.form input:focus, .form textarea:focus { border-color: #00a8ff; }
.form .err input { border-color: #e4393c; }
.form em { display: block; margin-top: 4px; font-size: 12px; font-style: normal; color: #e4393c; }
.agree { display: flex !important; gap: 8px; align-items: flex-start; font-size: 13px; color: #555; }
.agree input { margin-top: 3px; accent-color: #e4393c; }
.agree span { display: inline !important; margin: 0 !important; }
.agree-err { margin: -8px 0 10px; font-size: 12px; font-style: normal; color: #e4393c; }
.submit-err { margin-bottom: 10px; padding: 8px 12px; font-size: 14px; color: #c0392b; background: #fdecea; border-radius: 4px; }
.submit { width: 100%; height: 46px; font-size: 17px; color: #fff; background: #e4393c; border: 0; border-radius: 23px; }
.submit:disabled { background: #ccc; cursor: not-allowed; }
@media (max-width: 991px) {
  .page { padding-top: 84px; }
  .layout { grid-template-columns: 1fr; }
}
@media (max-width: 599px) {
  .card { padding: 16px; }
  .items thead { display: none; }
  .items tr { display: grid; grid-template-columns: 1fr auto; gap: 6px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
  .items td { padding: 0; border: 0; }
  .items td:nth-child(2) { text-align: right !important; }
  .items td:nth-child(3) { text-align: left !important; }
}
</style>

<script setup>
// 用户中心 - 我的订单：待支付 / 已支付 / 服务进行中 / 已完成 / 已取消
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHead } from '@unhead/vue'
import UserNav from '../components/UserNav.vue'
import StateBlock from '../components/StateBlock.vue'
import { api, getToken } from '../api/http.js'
import { useUserStore } from '../stores/user.js'
import { money } from '../utils/money.js'

useHead({ title: '我的订单 - 北京一零科技有限公司', meta: [{ name: 'robots', content: 'noindex' }] })

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const TABS = [
  ['', '全部'],
  ['PENDING_PAY', '待支付'],
  ['PAID', '已支付'],
  ['IN_SERVICE', '服务进行中'],
  ['COMPLETED', '已完成'],
  ['CANCELLED', '已取消'],
]
const status = ref(typeof route.query.status === 'string' ? route.query.status : '')
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const error = ref('')

async function load(reset = true) {
  if (reset) page.value = 1
  loading.value = true
  error.value = ''
  try {
    const r = await api.myOrders({ status: status.value, page: page.value, size: 10 })
    list.value = reset ? r.list : [...list.value, ...r.list]
    total.value = r.total
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function more() {
  page.value += 1
  load(false)
}

onMounted(() => {
  if (!getToken()) return router.replace({ path: '/user/login', query: { redirect: route.fullPath } })
  user.restore()
  load()
})
watch(status, (s) => {
  router.replace({ query: s ? { status: s } : {} })
  load()
})
</script>

<template>
  <div class="page">
    <UserNav />
    <div class="tabs" role="tablist" aria-label="订单状态">
      <button v-for="[v, l] in TABS" :key="v" type="button" role="tab" :aria-selected="status === v" :class="{ on: status === v }" @click="status = v">{{ l }}</button>
    </div>
    <StateBlock :loading="loading && !list.length" :error="!list.length ? error : ''" :empty="!loading && !list.length" empty-text="暂无订单" @retry="load()">
      <ul class="list">
        <li v-for="o in list" :key="o.orderNo" class="order">
          <div class="top">
            <span>{{ o.createdAt }}　订单号 {{ o.orderNo }}</span>
            <span class="st" :class="`st--${o.status}`">{{ o.statusLabel }}</span>
          </div>
          <div class="mid">
            <div>
              <p class="name">{{ o.solutionName }}</p>
              <p class="sum">{{ o.itemSummary }}</p>
            </div>
            <div class="right">
              <p class="money">{{ money(o.totalAmount) }}</p>
              <div class="btns">
                <RouterLink v-if="o.status === 'PENDING_PAY'" :to="`/pay/${o.orderNo}`" class="pay">去支付</RouterLink>
                <RouterLink :to="`/user/orders/${o.orderNo}`" class="view">订单详情</RouterLink>
              </div>
            </div>
          </div>
        </li>
      </ul>
      <p v-if="list.length < total" class="more"><button type="button" :disabled="loading" @click="more">{{ loading ? '加载中…' : '加载更多' }}</button></p>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; padding: 120px 16px 0; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 16px; }
.tabs button { padding: 7px 16px; font-size: 14px; color: #555; background: #fff; border: 1px solid #e4e7ed; border-radius: var(--radius-sm); }
.tabs button.on { color: #fff; background: var(--c-brand); border-color: var(--c-brand); }
.order { margin-bottom: 14px; background: #fff; border: 1px solid var(--c-line); border-radius: 8px; overflow: hidden; }
.top { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; padding: 10px 18px; font-size: 13px; color: #888; background: #fafbfc; }
.st { font-weight: 600; }
.st--PENDING_PAY { color: #e6a23c; }
.st--PAID, .st--IN_SERVICE { color: var(--c-brand); }
.st--COMPLETED { color: #07a35a; }
.st--CANCELLED { color: #aaa; }
.mid { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; padding: 16px 18px; }
.name { font-size: 16px; font-weight: 600; }
.sum { margin-top: 6px; font-size: 13px; color: #888; }
.right { text-align: right; }
.money { font-size: 18px; font-weight: 700; color: var(--c-price); }
.btns { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.btns a { padding: 5px 14px; font-size: 13px; border-radius: var(--radius-sm); }
.pay { color: #fff; background: var(--c-brand); }
.view { color: var(--c-brand); border: 1px solid var(--c-brand); }
.more { text-align: center; }
.more button { padding: 8px 30px; color: var(--c-brand); background: #fff; border: 1px solid var(--c-brand); border-radius: var(--radius-sm); }
@media (max-width: 991px) { .page { padding-top: 84px; } }
@media (max-width: 599px) { .right { text-align: left; } .btns { justify-content: flex-start; } }
</style>

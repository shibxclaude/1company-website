<script setup>
// 在线购买与会员服务未开放时，下单、支付、登录注册、用户中心页面显示此提示
import { useHead } from '@unhead/vue'
import { useSiteStore } from '../stores/site.js'

const site = useSiteStore()
useHead({ meta: [{ name: 'robots', content: 'noindex' }] })
</script>

<template>
  <section class="closed" role="status">
    <h1>在线购买暂未开放</h1>
    <p>在线下单、支付与用户中心暂未开放。如需购买或了解报价，请致电 <a :href="site.telHref">{{ site.servicePhone }}</a>，我们将为您安排。</p>
    <div class="links">
      <RouterLink to="/solutions">查看解决方案</RouterLink>
      <RouterLink to="/price">价格公示</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.closed { max-width: 640px; margin: 0 auto; padding: 160px 16px 120px; text-align: center; }
h1 { font-size: 26px; margin-bottom: 16px; }
p { color: var(--c-text-2, #555); line-height: 1.8; }
p a { color: var(--c-brand); font-weight: 600; white-space: nowrap; }
.links { margin-top: 28px; display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
.links a { padding: 8px 20px; border: 1px solid var(--c-line); border-radius: var(--radius-sm, 4px); }
</style>

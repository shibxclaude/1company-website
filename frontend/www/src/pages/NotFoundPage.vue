<script setup>
import { useHead } from '@unhead/vue'
import { company } from '../data/site.js'

useHead({ title: `页面不存在 - ${company.name}`, meta: [{ name: 'robots', content: 'noindex' }] })
</script>

<template>
  <section class="nf">
    <p class="code">404</p>
    <h1>页面不存在</h1>
    <p class="tip">您访问的页面可能已经删除或地址有误。</p>
    <div class="links">
      <RouterLink to="/">返回首页</RouterLink>
      <RouterLink to="/solutions">查看解决方案</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.nf { padding: 160px 16px 60px; text-align: center; }
.code { font-size: 72px; font-weight: 700; line-height: 1; color: var(--c-primary); }
h1 { margin-top: 16px; font-size: 24px; }
.tip { margin-top: 10px; color: #888; }
.links { display: flex; justify-content: center; gap: 16px; margin-top: 28px; }
.links a { padding: 8px 24px; color: #fff; background: var(--c-brand); border-radius: var(--radius-sm); }
.links a + a { color: var(--c-brand); background: #fff; border: 1px solid var(--c-brand); }
@media (max-width: 991px) { .nf { padding-top: 110px; } }
</style>

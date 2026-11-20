<script setup>
import PageBanner from '../components/PageBanner.vue'
import { useSeo } from '../composables/useSeo.js'
import { history } from '../data/site.js'

useSeo({
  title: '发展历程',
  description: '一零科技发展历程：2015年成立，2017年获国家高新技术企业证书，深耕公安、政务、营商环境、物联网等领域。',
})
</script>

<template>
  <PageBanner title="发展历程" subtitle="从 2015 年获得首批软件著作权，到服务多地公安与政务部门的数据平台。" :parent="{ label: '关于我们', to: '/about' }" />
  <section class="section">
    <ol class="timeline">
      <li v-for="(h, i) in history" :key="h.year" :class="i % 2 ? 'right' : 'left'">
        <div class="card">
          <time class="year">{{ h.year }}</time>
          <p v-for="(e, j) in h.events" :key="j">{{ e }}</p>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.timeline {
  position: relative;
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}
.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 4px;
  margin-left: -2px;
  border-radius: 2px;
  background: linear-gradient(var(--c-brand), var(--c-line) 85%);
}
.timeline li { position: relative; width: 50%; padding: 0 36px 28px; }
.timeline li.left { text-align: right; }
.timeline li.right { margin-left: 50%; }
.timeline li::after {
  content: '';
  position: absolute;
  top: 14px;
  width: 14px;
  height: 14px;
  border: 3px solid var(--c-signal);
  border-radius: 50%;
  background: #fff;
}
.timeline li.left::after { right: -7px; }
.timeline li.right::after { left: -7px; }
.year { display: block; font-size: 30px; font-weight: 700; line-height: 1.3; color: var(--c-heading-blue); }

.card p { font-size: 14px; line-height: 1.9; color: #555; }

@media (max-width: 767px) {
  .timeline::before { left: 24px; }
  .timeline li, .timeline li.right { width: auto; margin-left: 0; padding: 0 0 22px 36px; text-align: left; }
  .timeline li.left::after, .timeline li.right::after { left: -2px; right: auto; }
  .year { font-size: 24px; }
}
</style>

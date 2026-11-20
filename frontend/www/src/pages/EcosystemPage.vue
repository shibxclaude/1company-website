<script setup>
import PageBanner from '../components/PageBanner.vue'
import { useSeo } from '../composables/useSeo.js'

useSeo({
  title: '产品生态图总览',
  description:
    '一零科技产品生态：感知层、传输层、业务应用层、智能应用层，涵盖多维数据融合平台、物联网开发平台、低代码二次开发平台、营商环境门户平台等。',
})
</script>

<template>
  <PageBanner title="产品生态图总览" subtitle="从感知层、传输层到业务应用与智能应用的完整产品体系。" :parent="{ label: '产品中心', to: '/products' }" />
  <div class="figure">
    <img
      src="/images/chanpinshengtai.png"
      alt="产品生态图：感知层、传输层、业务应用层、智能应用层"
      width="1920"
      height="1842"
      loading="lazy"
    />
  </div>
</template>

<style scoped>
.figure { max-width: var(--wide-w); margin: 40px auto 0; padding: 0 24px; }
.figure img { display: block; width: 100%; border: 1px solid var(--c-line); border-radius: var(--radius); }
@media (max-width: 767px) { .figure { margin-top: 24px; padding: 0 16px; } }
</style>

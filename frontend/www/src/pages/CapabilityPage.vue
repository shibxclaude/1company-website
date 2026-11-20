<script setup>
import PageBanner from '../components/PageBanner.vue'
import { useSeo } from '../composables/useSeo.js'

useSeo({
  title: '信息技术应用能力',
  description:
    '一零科技信息技术应用能力：信息采集能力、数字服务能力、科技研发能力、行业定制能力、大数据模型比对分析能力，覆盖政务数据、物联网数据、互联网数据。',
})
</script>

<template>
  <PageBanner title="信息技术应用能力" subtitle="信息采集、数字服务、科技研发、行业定制与大数据模型比对分析。" :parent="{ label: '产品中心', to: '/products' }" />
  <div class="figure">
    <img
      src="/images/xinxi.png"
      alt="信息技术应用能力：信息采集能力、数字服务能力、科技研发能力、行业定制能力、大数据模型比对分析能力"
      width="1920"
      height="1385"
      loading="lazy"
    />
  </div>
</template>

<style scoped>
.figure { max-width: var(--wide-w); margin: 40px auto 0; padding: 0 24px; }
.figure img { display: block; width: 100%; border: 1px solid var(--c-line); border-radius: var(--radius); }
@media (max-width: 767px) { .figure { margin-top: 24px; padding: 0 16px; } }
</style>

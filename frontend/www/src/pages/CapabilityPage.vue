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
  <PageBanner image="/images/chanpinzhongxin.png" title="信息技术应用能力" />
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
.figure { display: flex; justify-content: center; }
.figure img { display: block; width: 100%; max-width: 1920px; }
</style>

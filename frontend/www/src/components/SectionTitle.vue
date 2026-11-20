<script setup>
// 原站区块标题："- 我们的<蓝>服务领域</蓝> -" + 灰色副标题
defineProps({
  lead: { type: String, default: '' },
  highlight: { type: String, required: true },
  tip: { type: String, default: '' },
  as: { type: String, default: 'h2' },
})
</script>

<template>
  <div class="section-title">
    <component :is="as" class="heading">
      <span class="dash" aria-hidden="true">-</span>
      <span>{{ lead }}</span><span class="hl">{{ highlight }}</span>
      <span class="dash" aria-hidden="true">-</span>
    </component>
    <p v-if="tip" class="tip">{{ tip }}</p>
  </div>
</template>

<style scoped>
.section-title { text-align: center; margin-bottom: 40px; padding: 0 16px; }
.heading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 25px;
  font-weight: 700;
  color: #222;
}
.hl { color: var(--c-primary); }
.dash { font-size: 40px; line-height: 1; color: var(--c-primary); transform: scaleX(1.6); margin: 0 6px; }
.tip { margin-top: 14px; font-size: 16px; color: var(--c-text-muted); }
@media (max-width: 767px) {
  .section-title { margin-bottom: 24px; }
  .heading { font-size: 21px; }
  .tip { font-size: 14px; }
}
</style>

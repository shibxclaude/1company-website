<script setup>
// 区块标题：左对齐，标题 + 一句说明；右侧可放一个链接（slot="aside"）
defineProps({
  lead: { type: String, default: '' },
  highlight: { type: String, required: true },
  tip: { type: String, default: '' },
  as: { type: String, default: 'h2' },
})
</script>

<template>
  <div class="section-title">
    <div>
      <component :is="as" class="heading">{{ lead }}{{ highlight }}</component>
      <p v-if="tip" class="tip">{{ tip }}</p>
    </div>
    <div v-if="$slots.aside" class="aside"><slot name="aside" /></div>
  </div>
</template>

<style scoped>
.section-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 32px;
  flex-wrap: wrap;
  max-width: var(--wide-w);
  margin: 0 auto 36px;
  padding: 0 24px;
}
.heading { font-size: var(--fs-2xl); line-height: 1.3; font-weight: 700; }
.tip { margin-top: 8px; max-width: 36em; font-size: var(--fs-md); color: var(--c-text-2); }
.aside { font-size: var(--fs-sm); }
.aside :deep(a) { color: var(--c-brand); }
.aside :deep(a:hover) { text-decoration: underline; }
@media (max-width: 767px) {
  .section-title { margin-bottom: 24px; padding: 0 16px; }
  .heading { font-size: var(--fs-xl); }
  .tip { font-size: var(--fs-sm); }
}
</style>

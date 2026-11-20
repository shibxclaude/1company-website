<script setup>
defineProps({
  title: { type: String, required: true },
  version: { type: String, required: true },
  effectiveDate: { type: String, required: true },
})
</script>

<template>
  <article class="legal">
    <h1>{{ title }}</h1>
    <p class="meta">版本：{{ version }}　生效日期：{{ effectiveDate }}</p>
    <div class="body"><slot /></div>
  </article>
</template>

<style scoped>
.legal { max-width: 900px; margin: 0 auto; padding: 120px 16px 0; }
h1 { font-size: 26px; text-align: center; }
.meta { margin-top: 10px; font-size: 13px; color: #999; text-align: center; }
.body { margin-top: 28px; font-size: 15px; line-height: 2; color: #444; }
.body :deep(h2) { margin: 26px 0 8px; font-size: 17px; color: #222; }
.body :deep(p) { margin-bottom: 8px; }
.body :deep(ol), .body :deep(ul) { margin: 0 0 8px 1.6em; }
.body :deep(ol) { list-style: decimal; }
.body :deep(ul) { list-style: disc; }
.body :deep(strong) { color: #222; }
.body :deep(a) { color: var(--c-primary); }
@media (max-width: 991px) { .legal { padding-top: 84px; } }
@media (max-width: 599px) { h1 { font-size: 21px; } .body { font-size: 14px; } }
</style>

<script setup>
// 内页页头：墨蓝底 + 淡等高线纹理 + 面包屑 + 标题。
// 原站的 banner 图片自带文字（如"当前位置：首页 > 关于我们"），改版后不再使用；image 参数保留只为兼容旧调用。
defineProps({
  image: { type: String, default: '' },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  parent: { type: Object, default: null }, // { label, to }
})
</script>

<template>
  <div class="page-hero">
    <svg class="contours" viewBox="0 0 800 260" preserveAspectRatio="xMaxYMid slice" aria-hidden="true">
      <g fill="none" stroke="currentColor">
        <ellipse v-for="i in 7" :key="i" cx="640" cy="170" :rx="60 + i * 46" :ry="34 + i * 26" />
      </g>
    </svg>
    <div class="inner">
      <nav class="crumbs" aria-label="面包屑">
        <RouterLink to="/">首页</RouterLink>
        <template v-if="parent"><span aria-hidden="true">/</span><RouterLink :to="parent.to">{{ parent.label }}</RouterLink></template>
        <span aria-hidden="true">/</span><span aria-current="page">{{ title }}</span>
      </nav>
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="sub">{{ subtitle }}</p>
    </div>
  </div>
</template>

<style scoped>
.page-hero {
  position: relative;
  overflow: hidden;
  padding: calc(var(--header-h) + 44px) 0 48px;
  color: var(--c-on-dark);
  background: linear-gradient(115deg, var(--c-ink) 40%, var(--c-ink-2));
}
.contours { position: absolute; inset: 0; width: 100%; height: 100%; color: rgba(160, 190, 255, 0.12); }
.inner { position: relative; max-width: var(--wide-w); margin: 0 auto; padding: 0 24px; }
.crumbs { display: flex; flex-wrap: wrap; gap: 8px; font-size: 13px; color: var(--c-on-dark-2); }
.crumbs a:hover { color: #fff; }
.crumbs [aria-current] { color: var(--c-on-dark); }
h1 { margin-top: 14px; font-size: var(--fs-3xl); line-height: 1.25; color: #fff; max-width: 22em; }
.sub { margin-top: 12px; max-width: 40em; font-size: var(--fs-md); color: var(--c-on-dark-2); }
@media (max-width: 767px) {
  .page-hero { padding: calc(var(--header-h) + 24px) 0 32px; }
  .inner { padding: 0 16px; }
  h1 { font-size: var(--fs-2xl); }
}
</style>

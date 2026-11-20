<script setup>
// 内页顶部 banner（1920×565 整图）+ 右下角"回到首页"
defineProps({
  image: { type: String, required: true },
  title: { type: String, required: true },
})
</script>

<template>
  <div class="page-banner">
    <img :src="image" :alt="title" width="1920" height="565" fetchpriority="high" />
    <h1 class="sr-only">{{ title }}</h1>
    <RouterLink to="/" class="back">回到首页</RouterLink>
  </div>
</template>

<style scoped>
.page-banner { position: relative; }
.page-banner img { display: block; width: 100%; height: auto; }
.back {
  position: absolute;
  right: 15%;
  bottom: 1%;
  font-size: 16px;
  color: #000;
}
.back:hover { color: var(--c-nav-active); }
@media (max-width: 767px) {
  .back { display: none; }
}
</style>

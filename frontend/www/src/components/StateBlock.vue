<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
})
defineEmits(['retry'])
</script>

<template>
  <div v-if="loading" class="state" role="status"><span class="spinner" aria-hidden="true" />加载中…</div>
  <div v-else-if="error" class="state error" role="alert">
    <p>{{ error }}</p>
    <button type="button" @click="$emit('retry')">重试</button>
  </div>
  <div v-else-if="empty" class="state">{{ emptyText }}</div>
  <slot v-else />
</template>

<style scoped>
.state { padding: 60px 16px; text-align: center; color: #999; font-size: 15px; }
.error { color: #c0392b; }
.error button { margin-top: 12px; padding: 6px 22px; color: #fff; background: #00a8ff; border: 0; border-radius: 16px; }
.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 8px;
  vertical-align: -3px;
  border: 2px solid #cfe9ff;
  border-top-color: #00a8ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>

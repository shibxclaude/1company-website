<script setup>
// 公司资质图标（替代原图中的四个图标）
defineProps({ name: { type: String, required: true } })
</script>

<template>
  <svg class="cred-icon" viewBox="0 0 64 64" width="56" height="56" aria-hidden="true">
    <template v-if="name === 'building'">
      <path d="M8 56V14l18-6v48z" fill="#0b1f3a" />
      <path d="M28 56V22h14v34z" fill="#0b1f3a" />
      <path d="M44 56V32h12v24z" fill="none" stroke="#0b1f3a" stroke-width="3" />
      <g fill="#fff"><rect x="13" y="18" width="4" height="4" /><rect x="19" y="18" width="4" height="4" /><rect x="13" y="26" width="4" height="4" /><rect x="19" y="26" width="4" height="4" /><rect x="13" y="34" width="4" height="4" /><rect x="19" y="34" width="4" height="4" /><rect x="32" y="28" width="3" height="3" /><rect x="37" y="28" width="3" height="3" /><rect x="32" y="35" width="3" height="3" /><rect x="37" y="35" width="3" height="3" /></g>
    </template>
    <template v-else-if="name === 'certificate'">
      <rect x="8" y="6" width="40" height="48" rx="4" fill="none" stroke="#ffaa2c" stroke-width="4" />
      <path d="M16 18h24M16 27h16M16 36h10" stroke="#1d5bff" stroke-width="4" stroke-linecap="round" />
      <circle cx="44" cy="42" r="9" fill="#fff" stroke="#1d5bff" stroke-width="4" />
      <path d="M39 50l-3 10 8-4 8 4-3-10" fill="none" stroke="#1d5bff" stroke-width="3" />
    </template>
    <template v-else-if="name === 'stack'">
      <path d="M32 6l24 12-24 12L8 18z" fill="none" stroke="#0b1f3a" stroke-width="4" stroke-linejoin="round" />
      <path d="M8 30l24 12 24-12M8 42l24 12 24-12" fill="none" stroke="#0b1f3a" stroke-width="4" stroke-linejoin="round" />
      <path d="M26 16l12 4" stroke="#ffaa2c" stroke-width="3" />
    </template>
    <template v-else>
      <circle cx="32" cy="26" r="20" fill="#1d5bff" />
      <path d="M32 14l4 9 9 1-7 6 2 9-8-5-8 5 2-9-7-6 9-1z" fill="#fff" />
      <path d="M18 40l-6 18 10-4 5 8 5-14M46 40l6 18-10-4-5 8-5-14" fill="#1d5bff" />
    </template>
  </svg>
</template>

<style scoped>
.cred-icon { display: block; }
</style>

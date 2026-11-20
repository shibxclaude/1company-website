<script setup>
import { computed } from 'vue'
import { money } from '../utils/money.js'

const props = defineProps({
  price: { type: [Number, String], default: null },
  originalPrice: { type: [Number, String], default: null },
  unit: { type: String, default: '' },
  from: { type: Boolean, default: false },
  size: { type: String, default: 'md' },
})
const unitText = computed(() => (props.unit === '年' ? '/年' : props.unit ? `/${props.unit}` : ''))
</script>

<template>
  <span class="price-tag" :class="`price-tag--${size}`">
    <template v-if="price !== null && price !== undefined">
      <strong class="now">{{ money(price) }}</strong><span class="unit">{{ unitText }}{{ from ? ' 起' : '' }}</span>
      <del v-if="originalPrice" class="orig">{{ money(originalPrice) }}</del>
    </template>
    <span v-else class="tbd">价格以咨询为准</span>
  </span>
</template>

<style scoped>
.price-tag { display: inline-flex; align-items: baseline; flex-wrap: wrap; gap: 2px 8px; }
.now { color: var(--c-price); font-weight: 700; font-size: 20px; }
.unit { font-size: 13px; color: var(--c-price); }
.orig { font-size: 13px; color: #999; }
.tbd { font-size: 14px; color: #999; }
.price-tag--lg .now { font-size: 30px; }
.price-tag--lg .unit { font-size: 15px; }
.price-tag--sm .now { font-size: 16px; }
</style>

<script setup>
const model = defineModel({ type: Number, default: 1 })
const props = defineProps({
  min: { type: Number, default: 1 },
  max: { type: Number, default: 99 },
  label: { type: String, default: '数量' },
  disabled: { type: Boolean, default: false },
})

function set(v) {
  const n = Number.parseInt(v, 10)
  model.value = Number.isNaN(n) ? props.min : Math.min(props.max, Math.max(props.min, n))
}
</script>

<template>
  <span class="qty" :class="{ disabled }">
    <button type="button" :disabled="disabled || model <= min" :aria-label="`减少${label}`" @click="set(model - 1)">−</button>
    <input
      type="number"
      inputmode="numeric"
      :min="min"
      :max="max"
      :value="model"
      :disabled="disabled"
      :aria-label="label"
      @change="set($event.target.value)"
    />
    <button type="button" :disabled="disabled || model >= max" :aria-label="`增加${label}`" @click="set(model + 1)">+</button>
  </span>
</template>

<style scoped>
.qty { display: inline-flex; align-items: stretch; height: 34px; border: 1px solid #dcdfe6; border-radius: 4px; overflow: hidden; }
.qty button { width: 34px; font-size: 18px; color: #606266; background: #f5f7fa; border: 0; }
.qty button:disabled { color: #c0c4cc; cursor: not-allowed; }
.qty input {
  width: 52px;
  text-align: center;
  font-size: 15px;
  border: 0;
  border-left: 1px solid #dcdfe6;
  border-right: 1px solid #dcdfe6;
  -moz-appearance: textfield;
}
.qty input::-webkit-outer-spin-button, .qty input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
.qty.disabled { opacity: 0.5; }
</style>

<script setup>
defineProps({ title: { type: String, required: true } })
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>{{ title }}</h1>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.auth-page { padding: 130px 16px 0; }
.auth-card {
  max-width: 420px;
  margin: 0 auto;
  padding: 30px 32px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.05);
}
h1 { margin-bottom: 20px; font-size: 22px; text-align: center; }
:slotted(.field) { display: block; margin-bottom: 14px; }
:slotted(.field > span) { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
:slotted(.field input) {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  font-size: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
}
:slotted(.field input:focus) { border-color: #00a8ff; }
:slotted(.code-row) { display: flex; gap: 10px; }
:slotted(.code-row button) {
  flex: none;
  width: 120px;
  font-size: 14px;
  color: #00a8ff;
  background: #fff;
  border: 1px solid #00a8ff;
  border-radius: 4px;
}
:slotted(.code-row button:disabled) { color: #aaa; border-color: #ddd; cursor: not-allowed; }
:slotted(.submit) { width: 100%; height: 44px; margin-top: 6px; font-size: 16px; color: #fff; background: #00a8ff; border: 0; border-radius: 22px; }
:slotted(.submit:disabled) { background: #9fd8f7; }
:slotted(.err) { margin-bottom: 10px; padding: 8px 12px; font-size: 13px; color: #c0392b; background: #fdecea; border-radius: 4px; }
:slotted(.mock-hint) { margin: -6px 0 12px; font-size: 12px; color: #8a6d3b; }
:slotted(.links) { display: flex; justify-content: space-between; margin-top: 16px; font-size: 14px; }
:slotted(.links a) { color: #00a8ff; }
@media (max-width: 991px) { .auth-page { padding-top: 90px; } }
@media (max-width: 480px) { .auth-card { padding: 24px 18px; } }
</style>

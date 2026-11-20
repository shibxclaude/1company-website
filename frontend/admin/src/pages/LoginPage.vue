<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth } from '../api.js'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const formRef = ref()
const loading = ref(false)
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const r = await api.login(form)
    auth.set(r.token)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <el-card class="card" shadow="always">
      <h1>一零科技 · 管理后台</h1>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
        <el-form-item prop="username"><el-input v-model="form.username" placeholder="账号" prefix-icon="User" autocomplete="username" /></el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password autocomplete="current-password" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="btn">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login { display: flex; align-items: center; justify-content: center; min-height: 100%; padding: 16px; box-sizing: border-box; background: linear-gradient(135deg, #0b1f3a, #12305e); }
.card { width: 100%; max-width: 380px; }
h1 { margin: 6px 0 24px; font-size: 20px; text-align: center; color: #303133; }
.btn { width: 100%; }
</style>

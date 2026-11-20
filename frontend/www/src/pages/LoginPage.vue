<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHead } from '@unhead/vue'
import AuthCard from '../components/AuthCard.vue'
import { api } from '../api/http.js'
import { useUserStore } from '../stores/user.js'
import { useSmsCode } from '../composables/useSmsCode.js'

useHead({ title: '登录 - 北京一零科技有限公司', meta: [{ name: 'robots', content: 'noindex' }] })

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const mode = ref('sms')
const form = reactive({ phone: '', code: '', account: '', password: '' })
const error = ref('')
const loading = ref(false)
const sms = useSmsCode('LOGIN')
const PHONE = /^1[3-9]\d{9}$/

async function sendCode() {
  error.value = ''
  if (!PHONE.test(form.phone)) return (error.value = '请输入正确的手机号')
  try {
    await sms.send(form.phone)
  } catch (e) {
    error.value = e.message
  }
}

async function submit() {
  error.value = ''
  if (mode.value === 'sms') {
    if (!PHONE.test(form.phone)) return (error.value = '请输入正确的手机号')
    if (!/^\d{6}$/.test(form.code)) return (error.value = '请输入 6 位验证码')
  } else if (!form.account.trim() || !form.password) {
    return (error.value = '请输入账号和密码')
  }
  loading.value = true
  try {
    const r =
      mode.value === 'sms'
        ? await api.loginSms({ phone: form.phone, code: form.code })
        : await api.loginPassword({ account: form.account.trim(), password: form.password })
    user.loginWith(r)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/user/orders'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthCard title="登录">
    <div class="tabs" role="tablist">
      <button type="button" role="tab" :aria-selected="mode === 'sms'" :class="{ on: mode === 'sms' }" @click="mode = 'sms'">验证码登录</button>
      <button type="button" role="tab" :aria-selected="mode === 'pwd'" :class="{ on: mode === 'pwd' }" @click="mode = 'pwd'">密码登录</button>
    </div>
    <form novalidate @submit.prevent="submit">
      <template v-if="mode === 'sms'">
        <label class="field"><span>手机号</span><input v-model="form.phone" type="tel" maxlength="11" inputmode="numeric" autocomplete="tel" /></label>
        <label class="field">
          <span>验证码</span>
          <span class="code-row">
            <input v-model="form.code" maxlength="6" inputmode="numeric" autocomplete="one-time-code" />
            <button type="button" :disabled="sms.countdown.value > 0 || sms.sending.value" @click="sendCode">
              {{ sms.countdown.value > 0 ? `${sms.countdown.value} 秒后重发` : '获取验证码' }}
            </button>
          </span>
        </label>
        <p v-if="sms.mockCode.value" class="mock-hint">模拟短信模式，验证码：{{ sms.mockCode.value }}</p>
        <p class="tip">未注册的手机号验证后将自动创建账号</p>
      </template>
      <template v-else>
        <label class="field"><span>手机号 / 账号</span><input v-model="form.account" maxlength="50" autocomplete="username" /></label>
        <label class="field"><span>密码</span><input v-model="form.password" type="password" maxlength="64" autocomplete="current-password" /></label>
      </template>
      <p v-if="error" class="err" role="alert">{{ error }}</p>
      <button type="submit" class="submit" :disabled="loading">{{ loading ? '登录中…' : '登录' }}</button>
    </form>
    <div class="links">
      <RouterLink :to="{ path: '/user/register', query: $route.query }">注册账号</RouterLink>
      <RouterLink to="/solutions">先去逛逛</RouterLink>
    </div>
  </AuthCard>
</template>

<style scoped>
.tabs { display: flex; margin-bottom: 18px; border-bottom: 1px solid #eee; }
.tabs button { flex: 1; padding: 10px 0; font-size: 15px; color: #666; background: none; border: 0; border-bottom: 2px solid transparent; }
.tabs button.on { color: var(--c-brand); border-bottom-color: var(--c-brand); }
.tip { margin: -6px 0 12px; font-size: 12px; color: #999; }
</style>

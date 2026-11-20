<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHead } from '@unhead/vue'
import AuthCard from '../components/AuthCard.vue'
import { api } from '../api/http.js'
import { useUserStore } from '../stores/user.js'
import { useSmsCode } from '../composables/useSmsCode.js'

useHead({ title: '注册 - 北京一零科技有限公司', meta: [{ name: 'robots', content: 'noindex' }] })

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const form = reactive({ phone: '', code: '', password: '', password2: '', username: '', agreed: false })
const error = ref('')
const loading = ref(false)
const sms = useSmsCode('REGISTER')
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
  if (!PHONE.test(form.phone)) return (error.value = '请输入正确的手机号')
  if (!/^\d{6}$/.test(form.code)) return (error.value = '请输入 6 位验证码')
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(form.password)) return (error.value = '密码 8-32 位，需同时包含字母和数字')
  if (form.password !== form.password2) return (error.value = '两次输入的密码不一致')
  if (form.username && !/^[A-Za-z][A-Za-z0-9_]{3,19}$/.test(form.username)) return (error.value = '账号以字母开头，4-20 位字母、数字或下划线')
  if (!form.agreed) return (error.value = '请阅读并同意《隐私政策》')
  loading.value = true
  try {
    const r = await api.register({ phone: form.phone, code: form.code, password: form.password, username: form.username || '' })
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
  <AuthCard title="注册账号">
    <form novalidate @submit.prevent="submit">
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
      <label class="field"><span>设置密码</span><input v-model="form.password" type="password" maxlength="32" autocomplete="new-password" placeholder="8-32 位，含字母和数字" /></label>
      <label class="field"><span>确认密码</span><input v-model="form.password2" type="password" maxlength="32" autocomplete="new-password" /></label>
      <label class="field"><span>账号（选填，可用于密码登录）</span><input v-model="form.username" maxlength="20" autocomplete="username" /></label>
      <label class="agree"><input v-model="form.agreed" type="checkbox" /> 我已阅读并同意 <RouterLink to="/agreement/privacy" target="_blank">《隐私政策》</RouterLink></label>
      <p v-if="error" class="err" role="alert">{{ error }}</p>
      <button type="submit" class="submit" :disabled="loading">{{ loading ? '提交中…' : '注册并登录' }}</button>
    </form>
    <div class="links">
      <RouterLink :to="{ path: '/user/login', query: $route.query }">已有账号，去登录</RouterLink>
    </div>
  </AuthCard>
</template>

<style scoped>
.agree { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; font-size: 13px; color: #555; }
.agree a { color: #00a8ff; }
</style>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user.js'

const user = useUserStore()
const router = useRouter()
function logout() {
  user.logout()
  router.replace('/')
}
</script>

<template>
  <div class="user-nav">
    <span class="hello">{{ user.user ? `您好，${user.user.nickname || user.user.phone}` : '用户中心' }}</span>
    <nav>
      <RouterLink to="/user/orders">我的订单</RouterLink>
      <button v-if="user.loggedIn" type="button" @click="logout">退出登录</button>
    </nav>
  </div>
</template>

<style scoped>
.user-nav { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.hello { font-size: 18px; font-weight: 600; }
nav { display: flex; gap: 16px; align-items: center; font-size: 14px; }
nav a { color: #00a8ff; }
nav button { padding: 0; font-size: 14px; color: #999; background: none; border: 0; }
nav button:hover { color: #e4393c; }
</style>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth } from '../api.js'
import { menus } from '../router.js'

const route = useRoute()
const router = useRouter()
const me = ref(null)
const collapsed = ref(window.innerWidth < 768)
const active = computed(() => route.meta.menu || '/' + route.path.split('/')[1])

onMounted(async () => {
  try {
    me.value = await api.me()
  } catch {
    auth.set(null)
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
  }
})

function onCommand(cmd) {
  if (cmd === 'password') router.push('/password')
  if (cmd === 'logout') {
    auth.set(null)
    router.replace('/login')
  }
}
</script>

<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '208px'" class="aside">
      <div class="brand">{{ collapsed ? '一零' : '一零科技 · 管理后台' }}</div>
      <el-menu :default-active="active" :collapse="collapsed" router background-color="#001529" text-color="#bfcbd9" active-text-color="#fff">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-button text @click="collapsed = !collapsed"><el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon></el-button>
        <span class="title">{{ route.meta.title }}</span>
        <span class="spacer" />
        <a href="/" target="_blank" class="site">访问官网</a>
        <el-dropdown @command="onCommand">
          <span class="user">{{ me?.realName || me?.username || '…' }}<el-icon><ArrowDown /></el-icon></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { height: 100%; }
.aside { background: #001529; transition: width 0.2s; overflow-x: hidden; }
.aside .el-menu { border-right: 0; }
.brand { height: 56px; line-height: 56px; text-align: center; font-weight: 600; color: #fff; white-space: nowrap; overflow: hidden; }
.header { display: flex; align-items: center; gap: 8px; height: 56px; background: #fff; box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08); }
.title { font-size: 16px; font-weight: 600; }
.spacer { flex: 1; }
.site { margin-right: 16px; font-size: 14px; color: #409eff; text-decoration: none; }
.user { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }
.main { padding: 16px; }
@media (max-width: 767px) { .site { display: none; } }
</style>

<script setup>
import { onMounted, onServerPrefetch } from 'vue'
import { useHead } from '@unhead/vue'
import { useRouter } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { useCatalogStore } from './stores/catalog.js'
import { useUserStore } from './stores/user.js'
import { setUnauthorizedHandler } from './api/http.js'

useHead({ htmlAttrs: { lang: 'zh-CN' } })

const catalog = useCatalogStore()
const user = useUserStore()
const router = useRouter()

// 导航下拉、首页、页脚都要用到方案列表：预渲染时取一次，浏览器里再刷新到最新
onServerPrefetch(() => catalog.loadList())
onMounted(() => {
  catalog.loadList({ force: true })
  user.restore()
  setUnauthorizedHandler(() => {
    user.logout()
    const path = router.currentRoute.value
    if (path.path.startsWith('/user')) router.replace({ path: '/user/login', query: { redirect: path.fullPath } })
  })
})
</script>

<template>
  <SiteHeader />
  <main id="main">
    <RouterView />
  </main>
  <SiteFooter />
</template>

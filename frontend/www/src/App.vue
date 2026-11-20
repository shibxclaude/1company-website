<script setup>
import { onMounted, onServerPrefetch } from 'vue'
import { useHead } from '@unhead/vue'
import { useRouter, useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { useCatalogStore } from './stores/catalog.js'
import { useUserStore } from './stores/user.js'
import { useSiteStore } from './stores/site.js'
import ShopClosed from './components/ShopClosed.vue'
import { setUnauthorizedHandler } from './api/http.js'

useHead({ htmlAttrs: { lang: 'zh-CN' } })

const catalog = useCatalogStore()
const user = useUserStore()
const router = useRouter()
const route = useRoute()
const site = useSiteStore()

// 导航下拉、首页、页脚都要用到方案列表：预渲染时取一次，浏览器里再刷新到最新
onServerPrefetch(() => Promise.all([catalog.loadList(), site.load()]))
onMounted(() => {
  catalog.loadList({ force: true })
  site.load().then(() => site.shopEnabled && user.restore())
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
    <!-- 在线购买未开放：下单、支付、登录、用户中心页面显示提示；配置取回之前不渲染，避免闪现 -->
    <template v-if="route.meta.shop && !site.shopEnabled">
      <ShopClosed v-if="site.loaded" />
    </template>
    <RouterView v-else />
  </main>
  <SiteFooter />
</template>

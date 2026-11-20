<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { nav as baseNav } from '../data/site.js'
import { useCatalogStore } from '../stores/catalog.js'
import { useUserStore } from '../stores/user.js'

const catalog = useCatalogStore()
const user = useUserStore()
const nav = computed(() =>
  baseNav.map((item) =>
    item.dynamic === 'solutions' ? { ...item, children: [...catalog.navSolutions, ...item.children] } : item,
  ),
)
const route = useRoute()
const activeKey = computed(() => route.meta.nav)
const scrolled = ref(false)
const menuOpen = ref(false)

function onScroll() {
  scrolled.value = window.scrollY > 24
}
onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

watch(() => route.fullPath, () => { menuOpen.value = false })
watch(menuOpen, (open) => {
  if (typeof document !== 'undefined') document.body.style.overflow = open ? 'hidden' : ''
})
</script>

<template>
  <header class="site-header" :class="{ 'is-scrolled': scrolled, 'is-solid': route.meta.solidHeader }">
    <div class="bar">
      <RouterLink to="/" class="logo" aria-label="一零科技首页">
        <img src="/images/topLeft.png" alt="一零科技" width="120" height="60" />
      </RouterLink>

      <nav class="pc-nav" aria-label="主导航">
        <ul>
          <li v-for="item in nav" :key="item.key" class="pc-item" :class="{ 'has-children': item.children }">
            <RouterLink :to="item.to" class="pc-link" :class="{ active: activeKey === item.key }">
              {{ item.label }}
              <svg v-if="item.children" class="caret" viewBox="0 0 10 6" aria-hidden="true"><path d="M1 1l4 4 4-4" /></svg>
            </RouterLink>
            <div v-if="item.children" class="dropdown">
              <RouterLink v-for="c in item.children" :key="c.to" :to="c.to">{{ c.label }}</RouterLink>
            </div>
          </li>
        </ul>
      </nav>

      <div class="right">
        <RouterLink to="/price" class="price-link">价格公示</RouterLink>
        <RouterLink v-if="user.loggedIn" to="/user/orders" class="user-link">我的订单</RouterLink>
        <RouterLink v-else to="/user/login" class="user-link">登录 / 注册</RouterLink>
      </div>

      <button
        class="menu-btn"
        type="button"
        aria-label="打开菜单"
        :aria-expanded="menuOpen"
        aria-controls="mobile-menu"
        @click="menuOpen = true"
      >
        <span /><span /><span />
      </button>
    </div>

    <div id="mobile-menu" class="mobile-menu" :class="{ open: menuOpen }" :aria-hidden="!menuOpen">
      <div class="m-top">
        <img src="/images/topLeft.png" alt="一零科技" width="96" height="48" />
        <button class="close-btn" type="button" aria-label="关闭菜单" @click="menuOpen = false">
          <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M4 4l12 12M16 4L4 16" /></svg>
        </button>
      </div>
      <ul>
        <li v-for="item in nav" :key="item.key">
          <RouterLink :to="item.to" :class="{ active: activeKey === item.key }" :tabindex="menuOpen ? 0 : -1">{{ item.label }}</RouterLink>
        </li>
        <li><RouterLink to="/price" :tabindex="menuOpen ? 0 : -1">价格公示</RouterLink></li>
        <li>
          <RouterLink :to="user.loggedIn ? '/user/orders' : '/user/login'" :class="{ active: activeKey === 'user' }" :tabindex="menuOpen ? 0 : -1">
            {{ user.loggedIn ? '我的订单' : '登录 / 注册' }}
          </RouterLink>
        </li>
      </ul>
      <a class="m-phone" href="tel:13699285133" :tabindex="menuOpen ? 0 : -1">咨询电话 <span class="num">136 9928 5133</span></a>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 100;
  height: var(--header-h);
  color: var(--c-on-dark);
  border-bottom: 1px solid transparent;
  transition: background-color 0.25s, border-color 0.25s;
}
.site-header.is-scrolled,
.site-header.is-solid {
  background: rgba(11, 31, 58, 0.94);
  border-bottom-color: var(--c-line-dark);
  backdrop-filter: saturate(160%) blur(10px);
}

.bar {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 0 24px;
}
.logo img { display: block; width: 104px; height: 52px; }

.pc-nav { flex: 1; }
.pc-nav ul { display: flex; justify-content: center; gap: 4px; }
.pc-item { position: relative; }
.pc-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: var(--header-h);
  padding: 0 14px;
  font-size: 15px;
  color: var(--c-on-dark-2);
  transition: color 0.2s;
}
.pc-link:hover, .pc-item:focus-within .pc-link { color: #fff; }
.pc-link.active { color: #fff; box-shadow: inset 0 -2px 0 var(--c-signal); }
.caret { width: 9px; height: 6px; fill: none; stroke: currentColor; stroke-width: 1.5; opacity: 0.7; }

.dropdown {
  position: absolute;
  top: calc(100% - 6px);
  left: 50%;
  min-width: 220px;
  padding: 8px;
  background: #102a50;
  border: 1px solid var(--c-line-dark);
  border-radius: var(--radius);
  box-shadow: 0 18px 40px rgba(3, 12, 30, 0.45);
  opacity: 0;
  visibility: hidden;
  transform: translate(-50%, 6px);
  transition: opacity 0.18s, transform 0.18s, visibility 0.18s;
}
.pc-item:hover .dropdown, .pc-item:focus-within .dropdown { opacity: 1; visibility: visible; transform: translate(-50%, 0); }
.dropdown a {
  display: block;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--c-on-dark-2);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}
.dropdown a:hover, .dropdown a.router-link-exact-active { color: #fff; background: rgba(255, 255, 255, 0.07); }

.right { display: flex; align-items: center; gap: 18px; }
.price-link { font-size: 14px; color: var(--c-on-dark-2); }
.price-link:hover { color: #fff; }
.user-link {
  padding: 7px 16px;
  font-size: 14px;
  color: #fff;
  background: var(--c-brand);
  border-radius: var(--radius-sm);
  white-space: nowrap;
  transition: background-color 0.2s;
}
.user-link:hover { background: var(--c-brand-strong); }

.menu-btn {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 40px;
  height: 40px;
  margin-left: auto;
  padding: 0 9px;
  background: none;
  border: 0;
}
.menu-btn span { display: block; height: 2px; background: #fff; border-radius: 1px; }

.mobile-menu {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  padding: 0 24px 32px;
  background: var(--c-ink);
  transform: translateX(100%);
  visibility: hidden;
  transition: transform 0.35s ease, visibility 0.35s;
  overflow-y: auto;
}
.mobile-menu.open { transform: none; visibility: visible; }
.m-top { display: flex; align-items: center; justify-content: space-between; height: var(--header-h); }
.close-btn { width: 40px; height: 40px; padding: 10px; background: none; border: 0; }
.close-btn svg { width: 20px; height: 20px; stroke: #fff; stroke-width: 2; }
.mobile-menu ul { margin-top: 12px; }
.mobile-menu li a {
  display: block;
  padding: 14px 0;
  font-size: 18px;
  color: var(--c-on-dark);
  border-bottom: 1px solid var(--c-line-dark);
}
.mobile-menu li a.active { color: var(--c-signal); }
.m-phone { margin-top: auto; padding-top: 24px; font-size: 14px; color: var(--c-on-dark-2); }
.m-phone .num { font-size: 18px; color: #fff; }

@media (max-width: 1199px) {
  .pc-link { padding: 0 10px; font-size: 14px; }
  .price-link { display: none; }
}
@media (max-width: 991px) {
  .pc-nav, .right { display: none; }
  .menu-btn { display: flex; }
  .bar { padding: 0 16px; }
  .site-header { background: rgba(11, 31, 58, 0.94); border-bottom-color: var(--c-line-dark); }
}
</style>

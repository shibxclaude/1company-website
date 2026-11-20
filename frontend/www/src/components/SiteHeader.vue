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
  // 原站：滚动超过 500px 后导航变为半透明灰
  scrolled.value = window.scrollY > 500
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
            </RouterLink>
            <div v-if="item.children" class="dropdown">
              <RouterLink v-for="c in item.children" :key="c.to" :to="c.to">{{ c.label }}</RouterLink>
            </div>
          </li>
        </ul>
      </nav>

      <div class="right">
        <RouterLink v-if="user.loggedIn" to="/user/orders" class="user-link">我的订单</RouterLink>
        <RouterLink v-else to="/user/login" class="user-link">登录 / 注册</RouterLink>
        <img class="lang" src="/images/topRight.png" alt="" width="120" height="60" aria-hidden="true" />
      </div>

      <button
        class="menu-btn"
        type="button"
        aria-label="打开菜单"
        :aria-expanded="menuOpen"
        aria-controls="mobile-menu"
        @click="menuOpen = true"
      >
        <img src="/images/menuIcon.png" alt="" width="30" height="30" />
      </button>
    </div>

    <div id="mobile-menu" class="mobile-menu" :class="{ open: menuOpen }" :aria-hidden="!menuOpen">
      <button class="close-btn" type="button" aria-label="关闭菜单" @click="menuOpen = false">
        <img src="/images/closeIcon.png" alt="" width="30" height="30" />
      </button>
      <ul>
        <li v-for="item in nav" :key="item.key">
          <RouterLink :to="item.to" :class="{ active: activeKey === item.key }" :tabindex="menuOpen ? 0 : -1">
            <i class="m-icon" :class="`m-icon--${item.mobileIcon}`" aria-hidden="true" />
            {{ item.label }}
          </RouterLink>
        </li>
        <li>
          <RouterLink :to="user.loggedIn ? '/user/orders' : '/user/login'" :class="{ active: activeKey === 'user' }" :tabindex="menuOpen ? 0 : -1">
            <i class="m-icon m-icon--user" aria-hidden="true" />
            {{ user.loggedIn ? '我的订单' : '登录 / 注册' }}
          </RouterLink>
        </li>
      </ul>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 100;
  height: var(--header-h);
  transition: background-color 0.3s;
}
.site-header.is-scrolled { background-color: var(--c-nav-scrolled); }
.site-header.is-solid { background-color: var(--c-nav-bg); }

.bar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-around;
  border-bottom: 1px solid #889cb5;
  padding: 0 16px;
}
.logo img, .lang { width: 120px; height: 60px; }
.right { display: flex; align-items: center; gap: 12px; }
.user-link {
  padding: 6px 14px;
  font-size: 14px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  white-space: nowrap;
  transition: background-color 0.3s;
}
.user-link:hover { background: rgba(255, 255, 255, 0.2); }

.pc-nav ul { display: flex; }
.pc-item {
  position: relative;
  width: 160px;
  height: 70px;
  margin-top: 20px;
  text-align: center;
  border-radius: 10px 10px 0 0;
  transition: background-color 0.3s;
}
.pc-item:hover, .pc-item:focus-within { background-color: var(--c-nav-hover-bg); }
.pc-link {
  display: block;
  line-height: 70px;
  font-size: 17px;
  color: var(--c-nav-text);
}
.pc-link.active { color: var(--c-nav-active); }

.dropdown {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  width: 160px;
  padding: 10px;
  text-align: left;
  background: var(--c-dropdown-bg);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
  border-radius: 0 0 10px 10px;
}
.pc-item:hover .dropdown, .pc-item:focus-within .dropdown { display: block; }
.dropdown a {
  display: block;
  padding: 5px 0;
  line-height: 2em;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #d2d2d2;
  transition: color 0.3s;
}
.dropdown a:last-child { border-bottom: 0; }
.dropdown a:hover, .dropdown a.router-link-exact-active { color: var(--c-nav-active); }

.menu-btn {
  display: none;
  margin-left: auto;
  padding: 0;
  background: none;
  border: 0;
}

.mobile-menu {
  position: fixed;
  inset: 0;
  z-index: 200;
  padding: 20vh 0 10vh;
  background: var(--c-mobile-menu-bg);
  transform: translateX(100%);
  visibility: hidden;
  transition: transform 0.5s, visibility 0.5s;
  overflow-y: auto;
}
.mobile-menu.open { transform: none; visibility: visible; }
.close-btn {
  position: absolute;
  top: 5%;
  right: 5%;
  padding: 0;
  background: none;
  border: 0;
}
.mobile-menu a {
  display: flex;
  align-items: center;
  width: min(70%, 320px);
  margin: 0 auto;
  padding: 10px 20px;
  font-size: 18px;
  line-height: 50px;
  color: #fff;
  border-bottom: 1px solid #122b40;
}
.mobile-menu li:last-child a { border-bottom: 0; }
.mobile-menu a.active { color: var(--c-mobile-active); }
.m-icon {
  flex: none;
  width: 44px;
  height: 44px;
  margin-right: 20px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  background: no-repeat center / 100% auto;
}
.active .m-icon { border-color: rgba(0, 168, 255, 0.8); }
.m-icon--home { background-image: url('/images/homeIcon01.png'); }
.active .m-icon--home { background-image: url('/images/homeIcon02.png'); }
.m-icon--about { background-image: url('/images/aboutIcon01.png'); }
.active .m-icon--about { background-image: url('/images/aboutIcon02.png'); }
.m-icon--solution { background-image: url('/images/solutionIcon01.png'); }
.active .m-icon--solution { background-image: url('/images/solutionIcon02.png'); }
.m-icon--product { background-image: url('/images/productIcon01.png'); }
.active .m-icon--product { background-image: url('/images/productIcon02.png'); }
.m-icon--news { background-image: url('/images/newsIcon01.png'); }
.active .m-icon--news { background-image: url('/images/newsIcon02.png'); }
.m-icon--user::before {
  content: '';
  display: block;
  width: 16px;
  height: 16px;
  margin: 8px auto 0;
  border: 2px solid currentColor;
  border-radius: 50%;
  box-shadow: 0 14px 0 -2px transparent;
}
.m-icon--user::after {
  content: '';
  display: block;
  width: 24px;
  height: 10px;
  margin: 2px auto 0;
  border: 2px solid currentColor;
  border-bottom: 0;
  border-radius: 12px 12px 0 0;
}
.m-icon--faq::before {
  content: '?';
  display: block;
  text-align: center;
  line-height: 40px;
  font-size: 22px;
  font-weight: 700;
}

@media (max-width: 1399px) {
  .pc-item { width: 120px; }
  .pc-link { font-size: 16px; }
  .lang { display: none; }
}
@media (max-width: 1099px) {
  .pc-item { width: 100px; }
  .pc-link { font-size: 15px; }
}
@media (max-width: 991px) {
  .site-header { height: 60px; }
  .bar { justify-content: flex-start; }
  .logo img { width: 90px; height: 45px; }
  .pc-nav, .right { display: none; }
  .menu-btn { display: block; }
  /* 移动端始终给导航一个底色，避免白色 logo 压在浅色内容上看不清 */
  .site-header { background-color: rgba(0, 64, 129, 0.85); }
}
</style>

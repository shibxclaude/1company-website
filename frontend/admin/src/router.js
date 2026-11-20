import { createRouter, createWebHistory } from 'vue-router'
import { auth, setUnauthorizedHandler } from './api.js'
import AdminLayout from './components/AdminLayout.vue'

export const menus = [
  { path: '/dashboard', title: '概览', icon: 'DataBoard' },
  { path: '/solutions', title: '解决方案', icon: 'Goods' },
  { path: '/categories', title: '方案分类', icon: 'Menu' },
  { path: '/news', title: '新闻动态', icon: 'Document' },
  { path: '/orders', title: '订单管理', icon: 'Tickets' },
  { path: '/users', title: '用户管理', icon: 'User' },
]

export const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    { path: '/login', component: () => import('./pages/LoginPage.vue'), meta: { public: true, title: '登录' } },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('./pages/DashboardPage.vue'), meta: { title: '概览' } },
        { path: 'solutions', component: () => import('./pages/SolutionListPage.vue'), meta: { title: '解决方案' } },
        { path: 'solutions/new', component: () => import('./pages/SolutionEditPage.vue'), meta: { title: '新增解决方案', menu: '/solutions' } },
        { path: 'solutions/:id', component: () => import('./pages/SolutionEditPage.vue'), meta: { title: '编辑解决方案', menu: '/solutions' } },
        { path: 'categories', component: () => import('./pages/CategoryPage.vue'), meta: { title: '方案分类' } },
        { path: 'news', component: () => import('./pages/NewsListPage.vue'), meta: { title: '新闻动态' } },
        { path: 'news/new', component: () => import('./pages/NewsEditPage.vue'), meta: { title: '发布新闻', menu: '/news' } },
        { path: 'news/:id', component: () => import('./pages/NewsEditPage.vue'), meta: { title: '编辑新闻', menu: '/news' } },
        { path: 'orders', component: () => import('./pages/OrderListPage.vue'), meta: { title: '订单管理' } },
        { path: 'orders/:orderNo', component: () => import('./pages/OrderDetailPage.vue'), meta: { title: '订单详情', menu: '/orders' } },
        { path: 'users', component: () => import('./pages/UserListPage.vue'), meta: { title: '用户管理' } },
        { path: 'password', component: () => import('./pages/PasswordPage.vue'), meta: { title: '修改密码' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  document.title = `${to.meta.title || ''} · 一零科技管理后台`
  if (!to.meta.public && !auth.token) return { path: '/login', query: { redirect: to.fullPath } }
})

setUnauthorizedHandler(() => {
  const cur = router.currentRoute.value
  if (!cur.meta.public) router.replace({ path: '/login', query: { redirect: cur.fullPath } })
})

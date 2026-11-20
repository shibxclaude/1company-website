// 路由表。meta.prerender=false 的页面只在浏览器渲染（nginx 返回 spa.html 空壳）。
// 动态路由（/solutions/:slug、/news/:id）的预渲染路径由 vite.config.js 的 includedRoutes 枚举。
// meta.solidHeader：页面顶部没有 banner 图，导航栏使用实色底。
import { clientOnlyPaths } from './clientOnly.js'

export { clientOnlyPaths }

const page = (name) => () => import(`../pages/${name}.vue`)

export const routes = [
  { path: '/', name: 'home', component: page('HomePage'), meta: { nav: 'home' } },
  { path: '/about', name: 'about', component: page('AboutPage'), meta: { nav: 'about' } },
  { path: '/about/history', name: 'history', component: page('HistoryPage'), meta: { nav: 'about' } },
  { path: '/solutions', name: 'solutions', component: page('SolutionListPage'), meta: { nav: 'solutions' } },
  { path: '/solutions/:slug', name: 'solution', component: page('SolutionDetailPage'), meta: { nav: 'solutions' } },
  { path: '/products', name: 'capability', component: page('CapabilityPage'), meta: { nav: 'products' } },
  { path: '/products/ecosystem', name: 'ecosystem', component: page('EcosystemPage'), meta: { nav: 'products' } },
  { path: '/price', name: 'price', component: page('PricePage'), meta: { nav: 'products' } },
  { path: '/news', name: 'news', component: page('NewsListPage'), meta: { nav: 'news' } },
  { path: '/news/:id', name: 'newsDetail', component: page('NewsDetailPage'), meta: { nav: 'news' } },
  { path: '/faq', name: 'faq', component: page('FaqPage'), meta: { nav: 'faq' } },
  { path: '/agreement/purchase', name: 'agreementPurchase', component: page('AgreementPurchasePage'), meta: { solidHeader: true } },
  { path: '/agreement/privacy', name: 'agreementPrivacy', component: page('AgreementPrivacyPage'), meta: { solidHeader: true } },

  // 以下只在浏览器渲染。meta.shop：属于在线购买与会员服务，未开放时由 App.vue 显示"暂未开放"
  { path: '/order/confirm', name: 'orderConfirm', component: page('OrderConfirmPage'), meta: { shop: true, nav: 'solutions', prerender: false, solidHeader: true } },
  { path: '/pay/:orderNo', name: 'pay', component: page('PayPage'), meta: { shop: true, prerender: false, solidHeader: true } },
  { path: '/user', redirect: '/user/orders' },
  { path: '/user/login', name: 'login', component: page('LoginPage'), meta: { shop: true, nav: 'user', prerender: false, solidHeader: true } },
  { path: '/user/register', name: 'register', component: page('RegisterPage'), meta: { shop: true, nav: 'user', prerender: false, solidHeader: true } },
  { path: '/user/orders', name: 'myOrders', component: page('MyOrdersPage'), meta: { shop: true, nav: 'user', prerender: false, auth: true, solidHeader: true } },
  { path: '/user/orders/:orderNo', name: 'myOrder', component: page('OrderDetailPage'), meta: { shop: true, nav: 'user', prerender: false, solidHeader: true } },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: page('NotFoundPage'), meta: { prerender: false, solidHeader: true } },
]

export function scrollBehavior(to, from, saved) {
  if (saved) return saved
  if (to.hash) return { el: to.hash }
  return { top: 0 }
}

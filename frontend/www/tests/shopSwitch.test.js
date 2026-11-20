import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createHead } from '@unhead/vue/client'
import { createPinia, setActivePinia } from 'pinia'
import { routes } from '../src/router/routes.js'
import App from '../src/App.vue'
import SiteHeader from '../src/components/SiteHeader.vue'
import SiteFooter from '../src/components/SiteFooter.vue'
import SolutionDetailPage from '../src/pages/SolutionDetailPage.vue'

// 在线购买与会员服务开关：后端 /api/portal/site-config 告知前台；关闭时隐藏购买与登录入口，改为电话咨询
const detail = {
  slug: 'iot', name: '物联网开发平台', categoryName: '平台', subtitle: '', mainImage: '/x.png', gallery: [],
  deliveryLabel: '远程部署', servicePeriod: '', afterSales: '', content: '<p>介绍</p>', seoTitle: '', seoDescription: '',
  versions: [{ id: 1, name: '基础版', price: '100.00', originalPrice: null, unit: '年', billingLabel: '按年订阅', billingType: 'YEARLY' }],
  services: [],
}

function stubApi(shopEnabled) {
  vi.stubGlobal('fetch', vi.fn(async (url) => {
    let data = []
    if (url.includes('/api/portal/site-config')) data = { shopEnabled, servicePhone: '136 9928 5133' }
    else if (url.includes('/api/portal/solutions/iot')) data = detail
    return { status: 200, json: async () => ({ code: 0, data }) }
  }))
}

async function mountAt(component, path, shopEnabled) {
  stubApi(shopEnabled)
  const router = createRouter({ history: createMemoryHistory(), routes })
  const pinia = createPinia()
  setActivePinia(pinia)
  router.push(path)
  await router.isReady()
  const w = mount(component, { global: { plugins: [router, createHead(), pinia] } })
  await flushPromises()
  await flushPromises()
  return w
}

describe('购买开关', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('关闭时页头不显示登录/我的订单，开放时显示', async () => {
    const closed = await mountAt(App, '/', false)
    expect(closed.findComponent(SiteHeader).text()).not.toContain('登录 / 注册')
    expect(closed.findAll('a[href="/user/login"]')).toHaveLength(0)
    const open = await mountAt(App, '/', true)
    expect(open.findComponent(SiteHeader).text()).toContain('登录 / 注册')
  })

  it('关闭时页脚不显示订单查询', async () => {
    const w = await mountAt(App, '/', false)
    expect(w.findComponent(SiteFooter).text()).not.toContain('订单查询')
  })

  it('关闭时直接访问下单、登录、我的订单页面显示暂未开放', async () => {
    for (const path of ['/order/confirm?slug=iot', '/user/login', '/user/register', '/user/orders', '/pay/NO1']) {
      const w = await mountAt(App, path, false)
      expect(w.find('main').text(), path).toContain('暂未开放')
      expect(w.find('main').text(), path).toContain('136 9928 5133')
    }
  })

  it('关闭时方案详情页不提供立即购买，改为电话咨询', async () => {
    const w = await mountAt(App, '/solutions/iot', false)
    const box = w.findComponent(SolutionDetailPage).find('#buy')
    expect(box.find('button.buy').exists()).toBe(false)
    expect(box.find('a[href="tel:13699285133"]').text()).toContain('136 9928 5133')
    expect(box.text()).toContain('在线购买暂未开放')
  })

  it('开放时方案详情页保留立即购买', async () => {
    const w = await mountAt(App, '/solutions/iot', true)
    expect(w.findComponent(SolutionDetailPage).find('#buy button.buy').exists()).toBe(true)
  })
})

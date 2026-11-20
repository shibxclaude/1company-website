import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createHead } from '@unhead/vue/client'
import { createPinia, setActivePinia } from 'pinia'
import { routes } from '../src/router/routes.js'
import OrderConfirmPage from '../src/pages/OrderConfirmPage.vue'

// 下单页停留期间后台改了价：提交时带上页面显示的金额，服务端拒绝后页面刷新为新价格并提示
const detail = (price) => ({
  slug: 'iot', name: '物联网开发平台', mainImage: '/x.png', deliveryLabel: '远程部署', servicePeriod: '',
  versions: [{ id: 1, name: '基础版', price, unit: '年', billingLabel: '按年订阅' }], services: [],
})

describe('确认订单：价格变动', () => {
  let posted
  let detailCalls
  beforeEach(() => {
    posted = null
    detailCalls = 0
    localStorage.setItem('cn01_last_contact', JSON.stringify({ contactName: '张三', contactPhone: '13900000000', contactEmail: 'a@b.cn' }))
    vi.stubGlobal('fetch', vi.fn(async (url, init) => {
      if (url.includes('/api/portal/solutions/iot')) {
        detailCalls++
        return { status: 200, json: async () => ({ code: 0, data: detail(detailCalls === 1 ? '100.00' : '120.00') }) }
      }
      if (url.includes('/api/portal/orders') && init?.method === 'POST') {
        posted = JSON.parse(init.body)
        return { status: 409, json: async () => ({ code: 409, msg: '价格已更新：当前应付 ¥120.00，请确认后重新提交' }) }
      }
      return { status: 200, json: async () => ({ code: 0, data: null }) }
    }))
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('提交时带上页面显示的应付金额；价格已更新时刷新为新价格并提示', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes })
    setActivePinia(createPinia())
    router.push('/order/confirm?slug=iot&v=1&q=1')
    await router.isReady()
    const w = mount(OrderConfirmPage, { global: { plugins: [router, createHead(), createPinia()] } })
    await flushPromises()
    expect(w.find('.total').text()).toContain('¥100.00')

    await w.find('.agree input[type=checkbox]').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(posted.expectedTotal).toBe('100.00')
    expect(w.find('.submit-err').text()).toContain('价格已更新')
    expect(detailCalls).toBe(2)
    expect(w.find('.total').text()).toContain('¥120.00')
  })
})

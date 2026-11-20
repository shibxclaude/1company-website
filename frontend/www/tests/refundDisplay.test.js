import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createHead } from '@unhead/vue/client'
import { createPinia, setActivePinia } from 'pinia'
import { routes } from '../src/router/routes.js'
import OrderDetailPage from '../src/pages/OrderDetailPage.vue'

function order(over) {
  return {
    orderNo: 'NO1', status: 'PAID', statusLabel: '已支付', solutionName: '物联网开发平台', solutionSlug: 'iot',
    totalAmount: '1000.00', refundedAmount: '0', contactName: '张三', contactPhone: '13900000000', contactEmail: 'a@b.c',
    createdAt: '2026-11-20 10:00:00', paidAt: '2026-11-20 10:01:00', payChannel: 'WECHAT_NATIVE', items: [], logs: [], ...over,
  }
}

async function mountWith(data) {
  vi.stubGlobal('fetch', vi.fn(async () => ({ status: 200, json: async () => ({ code: 0, data }) })))
  const router = createRouter({ history: createMemoryHistory(), routes })
  setActivePinia(createPinia())
  router.push('/user/orders/NO1#t=tok')
  await router.isReady()
  const w = mount(OrderDetailPage, { global: { plugins: [router, createHead(), createPinia()] } })
  await flushPromises()
  return w
}

describe('订单详情：退款展示', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('全额退款：不显示服务步骤，提示已原路退回', async () => {
    const w = await mountWith(order({ status: 'REFUNDED', statusLabel: '已退款', refundedAmount: '1000.00' }))
    expect(w.find('.steps').exists()).toBe(false)
    expect(w.text()).toContain('已全额退款 ¥1,000.00')
    expect(w.text()).toContain('原路退回')
  })

  it('部分退款：订单金额旁显示已退金额', async () => {
    const w = await mountWith(order({ refundedAmount: '100.00' }))
    expect(w.find('.steps').exists()).toBe(true)
    expect(w.find('.total').text()).toContain('已退款 ¥100.00')
  })
})

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { resolveOrderToken, orderLink, getOrderToken } from '../src/utils/orderTokens.js'
import { api } from '../src/api/http.js'

describe('游客订单凭证', () => {
  beforeEach(() => localStorage.clear())

  it('从 #t= 读取并保存到本机', () => {
    expect(resolveOrderToken({ hash: '#t=abc', query: {} }, 'NO1')).toBe('abc')
    expect(getOrderToken('NO1')).toBe('abc')
  })

  it('兼容旧链接 ?t=', () => {
    expect(resolveOrderToken({ hash: '', query: { t: 'old' } }, 'NO2')).toBe('old')
    expect(getOrderToken('NO2')).toBe('old')
  })

  it('链接里没有时取本机保存的', () => {
    localStorage.setItem('cn01_order_tokens', JSON.stringify({ NO3: 'saved' }))
    expect(resolveOrderToken({ hash: '', query: {} }, 'NO3')).toBe('saved')
    expect(resolveOrderToken({ hash: '', query: {} }, 'NONE')).toBeUndefined()
  })

  it('站内链接把凭证放在 # 后面，不放查询参数（不会发到服务器、不进访问日志）', () => {
    expect(orderLink('/pay/NO1', 'abc')).toEqual({ path: '/pay/NO1', hash: '#t=abc' })
    expect(orderLink('/pay/NO1', undefined)).toEqual({ path: '/pay/NO1' })
  })
})

describe('订单接口', () => {
  let fetchMock
  beforeEach(() => {
    fetchMock = vi.fn(async () => ({ status: 200, json: async () => ({ code: 0, data: {} }) }))
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  it.each([
    ['order', () => api.order('NO1', 'secret')],
    ['orderStatus', () => api.orderStatus('NO1', 'secret')],
    ['pay', () => api.pay('NO1', 'secret')],
    ['cancelOrder', () => api.cancelOrder('NO1', 'secret')],
    ['mockPay', () => api.mockPay('NO1', 'secret')],
  ])('%s 用请求头 X-Order-Token 传凭证，URL 中不含凭证', async (_, call) => {
    await call()
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).not.toContain('secret')
    expect(init.headers['X-Order-Token']).toBe('secret')
  })
})

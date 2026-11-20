import { describe, it, expect } from 'vitest'
import { toCents, formatCents, money, estimateTotal } from '../src/utils/money.js'

describe('金额', () => {
  it('转分精确，不受浮点误差影响', () => {
    expect(toCents('0.1')).toBe(10)
    expect(toCents(0.1 + 0.2)).toBe(30)
    expect(toCents('368000.00')).toBe(36800000)
    expect(toCents(29700)).toBe(2970000)
    expect(toCents('19.9')).toBe(1990)
    expect(() => toCents('abc')).toThrow()
  })

  it('千分位格式化', () => {
    expect(formatCents(36800000)).toBe('368,000.00')
    expect(formatCents(5)).toBe('0.05')
    expect(money('1234567.8')).toBe('¥1,234,567.80')
  })

  it('订单预估金额与后端算法一致', () => {
    const total = estimateTotal({ price: '368000.00' }, 2, [
      { price: '29700.00', quantity: 3 },
      { price: '3000.00', quantity: 1 },
    ])
    expect(total).toBe((368000 * 2 + 29700 * 3 + 3000) * 100)
    expect(estimateTotal(null, 1)).toBe(0)
  })
})

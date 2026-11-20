// 金额统一用"分"（整数）计算，避免浮点误差；展示时再格式化。

/** 接口返回的金额（数字或字符串，最多两位小数）转为分 */
export function toCents(v) {
  if (v === null || v === undefined || v === '') return 0
  const s = typeof v === 'number' ? v.toFixed(2) : String(v).trim()
  const m = /^(-?)(\d+)(?:\.(\d{1,2}))?$/.exec(s)
  if (!m) throw new Error(`金额格式不正确：${v}`)
  const cents = Number(m[2]) * 100 + Number((m[3] || '').padEnd(2, '0'))
  return m[1] ? -cents : cents
}

/** 分 -> "368,000.00" */
export function formatCents(cents) {
  const neg = cents < 0
  const abs = Math.abs(Math.round(cents))
  const yuan = Math.floor(abs / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${neg ? '-' : ''}${yuan}.${String(abs % 100).padStart(2, '0')}`
}

/** 金额 -> "¥368,000.00" */
export function money(v) {
  return `¥${formatCents(toCents(v))}`
}

/**
 * 订单预估金额（与后端算法一致：版本单价×数量 + Σ增值服务单价×数量）。最终金额以服务端为准。
 * @param {{price:any}} version
 * @param {number} quantity
 * @param {{price:any, quantity:number}[]} services
 */
export function estimateTotal(version, quantity, services = []) {
  if (!version) return 0
  let total = toCents(version.price) * quantity
  for (const s of services) total += toCents(s.price) * s.quantity
  return total
}

// 游客订单的访问凭证保存在本机，便于刷新支付页或稍后回来继续支付。
const KEY = 'cn01_order_tokens'

function read() {
  try {
    return JSON.parse(localStorage.getItem(KEY) || '{}')
  } catch {
    return {}
  }
}

export function saveOrderToken(orderNo, token) {
  try {
    const all = read()
    all[orderNo] = token
    // 只保留最近 20 个
    const entries = Object.entries(all).slice(-20)
    localStorage.setItem(KEY, JSON.stringify(Object.fromEntries(entries)))
  } catch {
    /* ignore */
  }
}

export function getOrderToken(orderNo) {
  return read()[orderNo] || null
}

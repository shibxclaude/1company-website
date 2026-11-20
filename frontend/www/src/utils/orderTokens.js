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

/**
 * 读取当前页面的订单凭证：链接里的 #t=（新）或 ?t=（旧链接兼容），否则取本机保存的。
 * 凭证放在 # 后面不会发到服务器，也就不会出现在 nginx 访问日志里。
 */
export function resolveOrderToken(route, orderNo) {
  const fromHash = typeof route.hash === 'string' ? new URLSearchParams(route.hash.replace(/^#/, '')).get('t') : null
  const fromQuery = typeof route.query?.t === 'string' ? route.query.t : null
  const t = fromHash || fromQuery
  if (t) saveOrderToken(orderNo, t)
  return t || getOrderToken(orderNo) || undefined
}

/** 站内订单链接：凭证放在 # 后面 */
export function orderLink(path, token) {
  return token ? { path, hash: `#t=${token}` } : { path }
}

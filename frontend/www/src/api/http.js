// 接口请求封装。统一返回结构 {code, msg, data}：code=0 成功，否则抛 ApiError。
// 预渲染（SSR）时请求 SSG_API_BASE 指向的后端；浏览器里走同域 /api（nginx 反代）。

export class ApiError extends Error {
  constructor(code, msg, status) {
    super(msg || '请求失败')
    this.code = code
    this.status = status
  }
}

const TOKEN_KEY = 'cn01_user_token'

function base() {
  if (import.meta.env.SSR) return (globalThis.process?.env?.SSG_API_BASE || '').replace(/\/$/, '')
  return ''
}

export function getToken() {
  if (import.meta.env.SSR) return null
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    /* 隐私模式等情况下忽略 */
  }
}

/** 预渲染时没有配置后端地址：动态内容留给浏览器加载 */
export function ssrApiAvailable() {
  return !import.meta.env.SSR || !!base()
}

let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

export async function request(path, { method = 'GET', query, body, auth = true, headers: extraHeaders } = {}) {
  let url = base() + path
  if (query) {
    const qs = new URLSearchParams()
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.append(k, v)
    })
    const s = qs.toString()
    if (s) url += (url.includes('?') ? '&' : '?') + s
  }
  const headers = { Accept: 'application/json', ...extraHeaders }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = auth ? getToken() : null
  if (token) headers.Authorization = `Bearer ${token}`

  let resp
  try {
    resp = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new ApiError(-1, '网络异常，请检查网络后重试', 0)
  }
  let json = null
  try {
    json = await resp.json()
  } catch {
    throw new ApiError(resp.status, resp.status >= 500 ? '服务器开小差了，请稍后再试' : '请求失败', resp.status)
  }
  if (json.code !== 0) {
    if (resp.status === 401 && onUnauthorized) onUnauthorized()
    throw new ApiError(json.code, json.msg, resp.status)
  }
  return json.data
}

const orderHeader = (token) => (token ? { 'X-Order-Token': token } : {})

export const api = {
  categories: () => request('/api/portal/categories', { auth: false }),
  solutions: (query) => request('/api/portal/solutions', { query, auth: false }),
  solution: (slug) => request(`/api/portal/solutions/${encodeURIComponent(slug)}`, { auth: false }),
  price: () => request('/api/portal/price', { auth: false }),
  newsPage: (page = 1, size = 10) => request('/api/portal/news', { query: { page, size }, auth: false }),
  news: (id) => request(`/api/portal/news/${encodeURIComponent(id)}`, { auth: false }),

  sendSms: (phone, scene) => request('/api/portal/sms/code', { method: 'POST', body: { phone, scene }, auth: false }),
  register: (body) => request('/api/portal/auth/register', { method: 'POST', body, auth: false }),
  loginSms: (body) => request('/api/portal/auth/login/sms', { method: 'POST', body, auth: false }),
  loginPassword: (body) => request('/api/portal/auth/login/password', { method: 'POST', body, auth: false }),
  me: () => request('/api/user/me'),
  setPassword: (body) => request('/api/user/password', { method: 'PUT', body }),

  createOrder: (body) => request('/api/portal/orders', { method: 'POST', body }),
  // 游客订单凭证走请求头，不放进 URL（URL 会进 nginx 访问日志）
  order: (orderNo, token) => request(`/api/portal/orders/${orderNo}`, { headers: orderHeader(token) }),
  orderStatus: (orderNo, token) => request(`/api/portal/orders/${orderNo}/status`, { headers: orderHeader(token) }),
  pay: (orderNo, token) => request(`/api/portal/orders/${orderNo}/pay`, { method: 'POST', headers: orderHeader(token) }),
  cancelOrder: (orderNo, token) => request(`/api/portal/orders/${orderNo}/cancel`, { method: 'POST', headers: orderHeader(token) }),
  mockPay: (orderNo, token) => request(`/api/pay/mock/${orderNo}/succeed`, { method: 'POST', headers: orderHeader(token) }),
  myOrders: (query) => request('/api/user/orders', { query }),
}

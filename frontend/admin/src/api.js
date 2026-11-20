// 后台接口封装：统一 {code,msg,data}，401 时跳转登录
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'cn01_admin_token'
export const auth = {
  get token() {
    return localStorage.getItem(TOKEN_KEY)
  },
  set(token) {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  },
}

let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

export async function request(path, { method = 'GET', query, body, form, silent = false } = {}) {
  let url = path
  if (query) {
    const qs = new URLSearchParams()
    Object.entries(query).forEach(([k, v]) => v !== undefined && v !== null && v !== '' && qs.append(k, v))
    if (qs.toString()) url += '?' + qs
  }
  const headers = {}
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`
  let payload
  if (form) payload = form
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }
  let json
  try {
    const resp = await fetch(url, { method, headers, body: payload })
    json = await resp.json()
    if (resp.status === 401) {
      auth.set(null)
      onUnauthorized()
    }
  } catch {
    json = { code: -1, msg: '网络异常或服务器无响应' }
  }
  if (json.code !== 0) {
    if (!silent) ElMessage.error(json.msg || '操作失败')
    const err = new Error(json.msg)
    err.code = json.code
    throw err
  }
  return json.data
}

export const api = {
  login: (body) => request('/api/admin/auth/login', { method: 'POST', body }),
  me: () => request('/api/admin/auth/me', { silent: true }),
  changePassword: (body) => request('/api/admin/auth/password', { method: 'PUT', body }),
  dashboard: () => request('/api/admin/dashboard'),

  categories: () => request('/api/admin/categories'),
  createCategory: (body) => request('/api/admin/categories', { method: 'POST', body }),
  updateCategory: (id, body) => request(`/api/admin/categories/${id}`, { method: 'PUT', body }),
  deleteCategory: (id) => request(`/api/admin/categories/${id}`, { method: 'DELETE' }),

  solutions: (query) => request('/api/admin/solutions', { query }),
  solution: (id) => request(`/api/admin/solutions/${id}`),
  createSolution: (body) => request('/api/admin/solutions', { method: 'POST', body }),
  updateSolution: (id, body) => request(`/api/admin/solutions/${id}`, { method: 'PUT', body }),
  solutionStatus: (id, status) => request(`/api/admin/solutions/${id}/status`, { method: 'PUT', body: { status } }),
  solutionSort: (id, sort) => request(`/api/admin/solutions/${id}/sort`, { method: 'PUT', body: { sort } }),
  deleteSolution: (id) => request(`/api/admin/solutions/${id}`, { method: 'DELETE' }),

  newsList: (query) => request('/api/admin/news', { query }),
  news: (id) => request(`/api/admin/news/${id}`),
  createNews: (body) => request('/api/admin/news', { method: 'POST', body }),
  updateNews: (id, body) => request(`/api/admin/news/${id}`, { method: 'PUT', body }),
  newsStatus: (id, status) => request(`/api/admin/news/${id}/status`, { method: 'PUT', body: { status } }),
  deleteNews: (id) => request(`/api/admin/news/${id}`, { method: 'DELETE' }),

  orders: (query) => request('/api/admin/orders', { query }),
  order: (orderNo) => request(`/api/admin/orders/${orderNo}`),
  progress: (orderNo, body) => request(`/api/admin/orders/${orderNo}/progress`, { method: 'POST', body }),
  refund: (orderNo, body) => request(`/api/admin/orders/${orderNo}/refunds`, { method: 'POST', body }),
  refundStray: (orderNo, body) => request(`/api/admin/orders/${orderNo}/refunds/stray`, { method: 'POST', body }),

  users: (query) => request('/api/admin/users', { query }),
  userStatus: (id, status) => request(`/api/admin/users/${id}/status`, { method: 'PUT', body: { status } }),

  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request('/api/admin/files', { method: 'POST', form })
  },
}

export const ORDER_STATUS = {
  PENDING_PAY: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'primary' },
  IN_SERVICE: { label: '服务进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  REFUNDED: { label: '已退款', type: 'danger' },
}

export const money = (v) => (v === null || v === undefined ? '—' : `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`)

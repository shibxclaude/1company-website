import { describe, it, expect } from 'vitest'
import { existsSync } from 'node:fs'
import { resolve } from 'node:path'
import { nav, serviceAreas, banners, company } from '../src/data/site.js'
import { routes, clientOnlyPaths } from '../src/router/routes.js'

const pub = (p) => resolve(import.meta.dirname, '../public', p.replace(/^\//, ''))

const matchable = (path) =>
  routes.some((r) => {
    if (r.path.includes('*')) return false
    const re = new RegExp('^' + r.path.replace(/:[^/]+/g, '[^/]+') + '$')
    return re.test(path)
  })

describe('静态资源', () => {
  it('首页引用的图片都存在于 public/', () => {
    ;[...banners, ...serviceAreas.map((a) => a.icon)].forEach((p) => expect(existsSync(pub(p)), p).toBe(true))
  })
})

describe('导航与路由', () => {
  it('导航和下拉菜单中的每个链接都有对应路由', () => {
    nav.flatMap((n) => [n.to, ...(n.children || []).map((c) => c.to)]).forEach((to) => expect(matchable(to), to).toBe(true))
  })

  it('原站 HTML 中实际引用过的旧页面都有新地址（链接转换对照）', () => {
    const legacy = {
      'index.html': '/',
      'aboutYiling.html': '/about',
      'aboutYiling01.html': '/about/history',
      'solutions01.html': '/solutions/data-fusion',
      'solutions03.html': '/solutions/police-station',
      'solutions04.html': '/solutions/business-env',
      'solutions05.html': '/solutions/security-assistant',
      'solutions.html': '/solutions/bigdata-dev',
      'solutions02.html': '/solutions/iot',
      'productCenter.html': '/products',
      'product01.html': '/products/ecosystem',
      'faq.html': '/faq',
    }
    Object.values(legacy).forEach((to) => expect(matchable(to), to).toBe(true))
  })

  it('ICP 审核相关页面都有路由：价格公示、协议、下单、支付、用户中心', () => {
    ;['/price', '/agreement/purchase', '/agreement/privacy', '/order/confirm', '/pay/123', '/user/login', '/user/register', '/user/orders', '/user/orders/123'].forEach(
      (p) => expect(matchable(p), p).toBe(true),
    )
  })

  it('价格公示与协议页做预渲染；下单、支付、用户中心只在浏览器渲染', () => {
    expect(clientOnlyPaths).not.toContain('/price')
    expect(clientOnlyPaths).not.toContain('/agreement/purchase')
    expect(clientOnlyPaths).toEqual(expect.arrayContaining(['/order/confirm', '/user/login', '/user/orders', '/user']))
  })

  it('ICP 备案链接指向工信部', () => {
    expect(company.icp.url).toMatch(/^https:\/\/beian\.miit\.gov\.cn\//)
    expect(company.icp.text).toContain('京ICP备')
  })
})

describe('clientOnly 列表与路由表一致', () => {
  it('标记 prerender:false 的路由都在 clientOnlyPaths 中（404 兜底除外）', () => {
    const flagged = routes.filter((r) => r.meta?.prerender === false && !r.path.includes('*')).map((r) => r.path)
    flagged.forEach((p) => expect(clientOnlyPaths, p).toContain(p))
  })
})

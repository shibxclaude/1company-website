import { describe, it, expect } from 'vitest'
import { prerenderPaths, buildSitemap, buildSpaShell, fetchDynamicData } from '../build/prerender.js'

describe('prerenderPaths', () => {
  it('去掉动态段、兜底路由和只在浏览器渲染的路由，补上详情页并去重', () => {
    const out = prerenderPaths(['/', '/about', '/news/:id', '/:pathMatch(.*)*', '/solutions/:slug', '/about', '/user/login'], {
      solutions: [{ slug: 'a' }, { slug: 'b' }],
      news: [{ id: 7 }],
      exclude: ['/user/login'],
    })
    expect(out).toEqual(['/', '/about', '/solutions/a', '/solutions/b', '/news/7'])
  })
})

describe('fetchDynamicData', () => {
  it('未配置后端地址时返回空', async () => {
    expect(await fetchDynamicData('')).toEqual({ solutions: [], news: [] })
  })
  it('从接口取方案与新闻；接口报错时构建失败', async () => {
    const fake = async (url) => ({
      status: 200,
      text: async () =>
        JSON.stringify(url.includes('/solutions') ? { code: 0, data: [{ slug: 'x' }] } : { code: 0, data: { list: [{ id: 1 }], total: 1 } }),
    })
    expect(await fetchDynamicData('http://api/', fake)).toEqual({ solutions: [{ slug: 'x' }], news: [{ id: 1 }] })
    const bad = async () => ({ status: 200, text: async () => JSON.stringify({ code: 500, msg: 'down' }) })
    await expect(fetchDynamicData('http://api', bad)).rejects.toThrow('down')
  })
  it('后端返回 HTML（如 nginx 404）时给出可读的报错', async () => {
    const html = async () => ({ status: 404, text: async () => '<html><h1>404 Not Found</h1></html>' })
    await expect(fetchDynamicData('http://api', html)).rejects.toThrow(/HTTP 404.*SSG_API_BASE/)
  })
})

describe('buildSitemap', () => {
  it('首页排第一，使用绝对地址和优先级', () => {
    const xml = buildSitemap(['/faq', '/', '/solutions/iot'], { siteUrl: 'https://x.cn', lastmod: '2026-01-01' })
    const locs = [...xml.matchAll(/<loc>(.*?)<\/loc>/g)].map((m) => m[1])
    expect(locs).toEqual(['https://x.cn/', 'https://x.cn/faq', 'https://x.cn/solutions/iot'])
    expect(xml).toContain('<loc>https://x.cn/</loc><lastmod>2026-01-01</lastmod><priority>1.0</priority>')
    expect(xml).toContain('<loc>https://x.cn/solutions/iot</loc><lastmod>2026-01-01</lastmod><priority>0.8</priority>')
    expect(xml.startsWith('<?xml')).toBe(true)
  })
})

describe('buildSpaShell', () => {
  const index =
    '<html><head><title>首页</title><meta name="description" content="d"><meta property="og:title" content="t">' +
    '<link rel="canonical" href="https://x.cn/"><script type="application/ld+json">{}</script></head>' +
    '<body><div id="app" data-server-rendered="true"><header>导航</header><main>正文</main></div>' +
    '<script>window.__INITIAL_STATE__=\'{}\'</script><link rel="stylesheet" href="/a.css"></body></html>'

  it('清空正文与首页专属 head，保留样式并加 noindex', () => {
    const shell = buildSpaShell(index, '公司')
    expect(shell).toContain('<div id="app"></div>')
    expect(shell).not.toContain('正文')
    expect(shell).not.toContain('__INITIAL_STATE__')
    expect(shell).not.toContain('description')
    expect(shell).not.toContain('og:title')
    expect(shell).not.toContain('canonical')
    expect(shell).not.toContain('ld+json')
    expect(shell).toContain('<title>公司</title>')
    expect(shell).toContain('<link rel="stylesheet" href="/a.css">')
    expect(shell).toContain('<meta name="robots" content="noindex"></head>')
  })

  it('结构不符时报错而不是生成坏文件', () => {
    expect(() => buildSpaShell('<html></html>', 'x')).toThrow()
  })
})

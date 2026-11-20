// 预渲染路径与 sitemap 生成（vite.config.js 调用，单元测试也直接测这里）
export const SITE_URL = 'https://www.cn01.com.cn'

/**
 * 静态路由去掉动态段、兜底路由和只在浏览器渲染的路由，再补上动态详情页。
 * @param {string[]} paths vite-ssg 从路由表展开的路径
 * @param {{ solutions?: {slug:string}[], news?: {id:number}[], exclude?: string[] }} data
 */
export function prerenderPaths(paths, { solutions = [], news = [], exclude = [] } = {}) {
  const statics = paths.filter((p) => !p.includes(':') && !p.includes('*') && !exclude.includes(p))
  const details = [...solutions.map((s) => `/solutions/${s.slug}`), ...news.map((n) => `/news/${n.id}`)]
  return [...new Set([...statics, ...details])]
}

/** 构建时从后端取动态路由数据。未配置 SSG_API_BASE 时返回空（详情页留给浏览器渲染）。 */
export async function fetchDynamicData(apiBase, fetchImpl = fetch) {
  if (!apiBase) return { solutions: [], news: [] }
  const get = async (path) => {
    const url = apiBase.replace(/\/$/, '') + path
    const resp = await fetchImpl(url)
    const text = await resp.text()
    let json
    try {
      json = JSON.parse(text)
    } catch {
      throw new Error(
        `预渲染取数失败 ${url}：HTTP ${resp.status}，返回的不是 JSON（后端未上线或地址不对？）。` +
          '请把 SSG_API_BASE 设为可用的后端地址，或设为空（SSG_API_BASE= ）让动态内容改由浏览器加载',
      )
    }
    if (json.code !== 0) throw new Error(`预渲染取数失败 ${path}：${json.msg}`)
    return json.data
  }
  const [solutions, news] = await Promise.all([get('/api/portal/solutions'), get('/api/portal/news?page=1&size=50')])
  return { solutions, news: news.list }
}

const PRIORITY = { '/': '1.0', '/solutions': '0.9', '/price': '0.9' }

export function buildSitemap(paths, { siteUrl = SITE_URL, lastmod = new Date().toISOString().slice(0, 10) } = {}) {
  const urls = [...paths]
    .sort((a, b) => (a === '/' ? -1 : b === '/' ? 1 : a.localeCompare(b)))
    .map((p) => {
      const loc = siteUrl + p
      const priority = PRIORITY[p] || (p.startsWith('/solutions/') ? '0.8' : '0.6')
      return `  <url><loc>${loc}</loc><lastmod>${lastmod}</lastmod><priority>${priority}</priority></url>`
    })
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls.join('\n')}\n</urlset>\n`
}

/**
 * 由预渲染后的首页生成"空壳"页面 spa.html，供 nginx 兜底不做预渲染的路由（用户中心、下单、支付、新上架的方案、404）。
 * 去掉首页正文、首页专属的 head 信息和序列化状态，客户端挂载时会按当前 URL 完整渲染。
 */
export function buildSpaShell(indexHtml, title) {
  const start = indexHtml.indexOf('<div id="app"')
  const stateTag = '<script>window.__INITIAL_STATE__'
  const stateStart = indexHtml.indexOf(stateTag, start)
  if (start < 0 || stateStart < 0) throw new Error('buildSpaShell: 未找到 #app 或 __INITIAL_STATE__')
  const stateEnd = indexHtml.indexOf('</script>', stateStart) + '</script>'.length
  const html = indexHtml.slice(0, start) + '<div id="app"></div>' + indexHtml.slice(stateEnd)
  return html
    .replace(/<title>[\s\S]*?<\/title>/, `<title>${title}</title>`)
    .replace(/<meta (name="description"|property="og:[a-z_]+")[^>]*>/g, '')
    .replace(/<link rel="canonical"[^>]*>/g, '')
    .replace(/<script type="application\/ld\+json">[\s\S]*?<\/script>/g, '')
    .replace('</head>', '<meta name="robots" content="noindex"></head>')
}

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { prerenderPaths, buildSitemap, buildSpaShell, fetchDynamicData } from './build/prerender.js'
import { clientOnlyPaths } from './src/router/clientOnly.js'

// 预渲染时从这个地址的后端取数（如 https://www.yilinga.com 或 http://127.0.0.1:8080）。
// 不配置时，依赖接口的内容在浏览器端加载，详情页不做预渲染。
const API_BASE = process.env.SSG_API_BASE || ''
let rendered = []

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/uploads': 'http://127.0.0.1:8080',
    },
  },
  ssgOptions: {
    script: 'async',
    formatting: 'minify',
    dirStyle: 'nested', // /about -> about/index.html，nginx 无需额外改写
    beastiesOptions: { reduceInlineStyles: false },
    async includedRoutes(paths) {
      if (!API_BASE) console.warn('[ssg] 未设置 SSG_API_BASE：解决方案、新闻、价格等动态内容将在浏览器端加载')
      const data = await fetchDynamicData(API_BASE)
      rendered = prerenderPaths(paths, { ...data, exclude: clientOnlyPaths })
      return rendered
    },
    onFinished() {
      const dist = (f) => resolve(import.meta.dirname, 'dist', f)
      writeFileSync(dist('sitemap.xml'), buildSitemap(rendered))
      writeFileSync(dist('spa.html'), buildSpaShell(readFileSync(dist('index.html'), 'utf-8'), '北京一零科技有限公司'))
    },
  },
  test: {
    environment: 'jsdom',
  },
})

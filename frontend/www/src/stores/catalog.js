// 解决方案目录（分类、列表、详情、价格公示）。
// 预渲染时在服务端取数并序列化进 HTML；浏览器里挂载后再静默刷新一次，保证后台改价即时可见。
import { defineStore } from 'pinia'
import { api, ssrApiAvailable } from '../api/http.js'

const FRESH_MS = 30_000

export const useCatalogStore = defineStore('catalog', {
  state: () => ({
    categories: [],
    solutions: [],
    details: {},
    price: null,
    loadedAt: { list: 0, price: 0, details: {} },
    error: null,
  }),
  getters: {
    navSolutions: (s) => s.solutions.map((x) => ({ label: x.name, to: `/solutions/${x.slug}` })),
  },
  actions: {
    async loadList({ force = false } = {}) {
      if (!ssrApiAvailable()) return
      if (!force && this.solutions.length && Date.now() - this.loadedAt.list < FRESH_MS) return
      try {
        const [categories, solutions] = await Promise.all([api.categories(), api.solutions()])
        this.categories = categories
        this.solutions = solutions
        this.loadedAt.list = Date.now()
        this.error = null
      } catch (e) {
        this.error = e.message
        if (import.meta.env.SSR) throw e
      }
    },
    /** @returns 详情；不存在时返回 null */
    async loadDetail(slug, { force = false } = {}) {
      if (!ssrApiAvailable()) return null
      const fresh = Date.now() - (this.loadedAt.details[slug] || 0) < FRESH_MS
      if (!force && this.details[slug] !== undefined && fresh) return this.details[slug]
      try {
        this.details[slug] = await api.solution(slug)
      } catch (e) {
        if (e.status === 404) this.details[slug] = null
        else if (import.meta.env.SSR) throw e
        else if (this.details[slug] === undefined) throw e
      }
      this.loadedAt.details[slug] = Date.now()
      return this.details[slug]
    },
    async loadPrice({ force = false } = {}) {
      if (!ssrApiAvailable()) return
      if (!force && this.price && Date.now() - this.loadedAt.price < FRESH_MS) return
      try {
        this.price = await api.price()
        this.loadedAt.price = Date.now()
      } catch (e) {
        if (import.meta.env.SSR) throw e
        this.error = e.message
      }
    },
  },
})

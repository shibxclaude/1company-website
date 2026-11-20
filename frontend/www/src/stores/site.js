// 站点运行时配置（来自后端 /api/portal/site-config）。
// shopEnabled：在线购买与会员服务是否开放。默认关闭——配置没取到时宁可显示电话咨询，也不露出不可用的购买入口。
import { defineStore } from 'pinia'
import { api, ssrApiAvailable } from '../api/http.js'
import { company } from '../data/site.js'

export const useSiteStore = defineStore('site', {
  state: () => ({ shopEnabled: false, servicePhone: company.phoneDisplay, loaded: false }),
  getters: {
    telHref: (s) => `tel:${s.servicePhone.replace(/\s/g, '')}`,
  },
  actions: {
    async load() {
      if (!ssrApiAvailable()) return
      try {
        const c = await api.siteConfig()
        this.shopEnabled = !!c.shopEnabled
        if (c.servicePhone) this.servicePhone = c.servicePhone
      } catch {
        /* 取不到时保持关闭 */
      }
      this.loaded = true
    },
  },
})

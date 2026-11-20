import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createHead } from '@unhead/vue/client'
import { createPinia, setActivePinia } from 'pinia'
import { useCatalogStore } from '../src/stores/catalog.js'
import SiteHeader from '../src/components/SiteHeader.vue'
import SiteFooter from '../src/components/SiteFooter.vue'
import { routes } from '../src/router/routes.js'

async function mountAt(component, path, setup) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  const pinia = createPinia()
  setActivePinia(pinia)
  if (setup) setup()
  router.push(path)
  await router.isReady()
  const wrapper = mount(component, { global: { plugins: [router, createHead(), pinia] } })
  await flushPromises()
  return wrapper
}

describe('SiteHeader', () => {
  it('解决方案详情页时，一级菜单"解决方案"高亮', async () => {
    const w = await mountAt(SiteHeader, '/solutions/iot')
    const active = w.findAll('.pc-link.active')
    expect(active).toHaveLength(1)
    expect(active[0].text()).toBe('解决方案')
  })

  it('一级菜单都有可点击的链接（触屏可用）', async () => {
    const w = await mountAt(SiteHeader, '/')
    w.findAll('.pc-link').forEach((a) => expect(a.attributes('href')).toBeTruthy())
  })

  it('解决方案下拉菜单由接口数据生成', async () => {
    const w = await mountAt(SiteHeader, '/', () => {
      useCatalogStore().solutions = [{ slug: 'iot', name: '物联网开发平台' }]
    })
    const links = w.findAll('.dropdown a').map((a) => a.attributes('href'))
    expect(links).toContain('/solutions/iot')
    expect(links).toContain('/solutions')
  })

  it('未登录时显示登录入口', async () => {
    const w = await mountAt(SiteHeader, '/')
    expect(w.find('.user-link').text()).toBe('登录 / 注册')
    expect(w.find('.user-link').attributes('href')).toBe('/user/login')
  })

  it('移动端菜单可以打开和关闭', async () => {
    const w = await mountAt(SiteHeader, '/')
    expect(w.find('.mobile-menu').classes()).not.toContain('open')
    await w.find('.menu-btn').trigger('click')
    expect(w.find('.mobile-menu').classes()).toContain('open')
    await w.find('.close-btn').trigger('click')
    expect(w.find('.mobile-menu').classes()).not.toContain('open')
  })
})

describe('SiteFooter', () => {
  it('展示 ICP 备案号并外链工信部', async () => {
    const w = await mountAt(SiteFooter, '/')
    const icp = w.find('a.icp')
    expect(icp.text()).toContain('京ICP备 15030540号-16')
    expect(icp.attributes('href')).toMatch(/^https:\/\/beian\.miit\.gov\.cn\//)
    expect(icp.attributes('target')).toBe('_blank')
  })

  it('页脚有价格公示、购买协议、隐私政策入口', async () => {
    const w = await mountAt(SiteFooter, '/')
    const hrefs = w.findAll('.legal a').map((a) => a.attributes('href'))
    expect(hrefs).toEqual(expect.arrayContaining(['/price', '/agreement/purchase', '/agreement/privacy']))
  })

  it('网站地图包含常见问题（原站多数页面缺失）', async () => {
    const w = await mountAt(SiteFooter, '/')
    expect(w.find('.sitemap').text()).toContain('常见问题')
  })
})

describe('SiteHeader 实色底', () => {
  it('没有 banner 的页面（支付、用户中心、协议）导航为实色底', async () => {
    for (const p of ['/pay/1', '/user/login', '/agreement/purchase', '/order/confirm']) {
      const w = await mountAt(SiteHeader, p)
      expect(w.find('.site-header').classes(), p).toContain('is-solid')
    }
    const home = await mountAt(SiteHeader, '/')
    expect(home.find('.site-header').classes()).not.toContain('is-solid')
  })
})

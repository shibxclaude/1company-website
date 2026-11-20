import { useHead } from '@unhead/vue'
import { useRoute } from 'vue-router'
import { computed, unref } from 'vue'
import { SITE_URL, company } from '../data/site.js'

/**
 * 每页独立的 title / description / canonical / OG。
 * title、description 可以传 ref/computed（详情页数据加载后再更新）。
 */
export function useSeo({ title, description, jsonLd } = {}) {
  const route = useRoute()
  const fullTitle = computed(() => {
    const t = unref(title)
    return t ? `${t} - ${company.name}` : company.name
  })
  const desc = computed(() => unref(description) || company.description)
  const url = computed(() => SITE_URL + (route.path === '/' ? '/' : route.path.replace(/\/$/, '')))

  useHead({
    title: fullTitle,
    meta: [
      { name: 'description', content: desc },
      { property: 'og:title', content: fullTitle },
      { property: 'og:description', content: desc },
      { property: 'og:type', content: 'website' },
      { property: 'og:url', content: url },
      { property: 'og:site_name', content: company.name },
    ],
    link: [{ rel: 'canonical', href: url }],
    script: jsonLd
      ? [{ type: 'application/ld+json', innerHTML: computed(() => JSON.stringify(unref(jsonLd))) }]
      : [],
  })
}

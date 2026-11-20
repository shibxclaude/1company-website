<script setup>
import { computed, ref, onMounted, onServerPrefetch, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageBanner from '../components/PageBanner.vue'
import StateBlock from '../components/StateBlock.vue'
import NotFoundPage from './NotFoundPage.vue'
import { useSeo } from '../composables/useSeo.js'
import { api, ssrApiAvailable } from '../api/http.js'
import { useNewsStore } from '../stores/news.js'

const route = useRoute()
const store = useNewsStore()
const id = computed(() => String(route.params.id))
const news = computed(() => store.details[id.value])
const error = ref('')

async function load() {
  if (!ssrApiAvailable()) return
  error.value = ''
  try {
    store.details[id.value] = await api.news(id.value)
  } catch (e) {
    if (e.status === 404 || e.status === 400) store.details[id.value] = null
    else if (import.meta.env.SSR) throw e
    else error.value = e.message
  }
}
onServerPrefetch(load)
onMounted(load)
watch(id, load)

useSeo({
  title: computed(() => news.value?.title || (news.value === null ? '页面不存在' : '新闻动态')),
  description: computed(() => news.value?.summary || ''),
})
</script>

<template>
  <NotFoundPage v-if="news === null" />
  <template v-else>
    <PageBanner :title="news?.title || '新闻动态'" :parent="{ label: '新闻动态', to: '/news' }" />
    <StateBlock :loading="!news && !error" :error="error" @retry="load">
      <article v-if="news" class="news">
        <p class="meta"><time>{{ news.publishedAt }}</time></p>
        <div class="rich" v-html="news.content" />
        <p class="back"><RouterLink to="/news">‹ 返回新闻列表</RouterLink></p>
      </article>
    </StateBlock>
  </template>
</template>

<style scoped>
.news { max-width: 900px; margin: 0 auto; padding: 24px 16px 0; }
.crumbs { display: flex; gap: 6px; font-size: 14px; color: #999; }
.crumbs a:hover { color: var(--c-primary); }
.title { margin-top: 20px; font-size: 26px; line-height: 1.4; text-align: center; }
.meta { font-size: 13px; color: var(--c-text-3); padding-bottom: 18px; border-bottom: 1px solid var(--c-line); }
.rich { margin-top: 22px; font-size: 16px; line-height: 2; color: #444; overflow-wrap: anywhere; }
.rich :deep(p) { margin-bottom: 14px; }
.rich :deep(img) { display: block; max-width: 100%; height: auto; margin: 14px auto; }
.rich :deep(h2), .rich :deep(h3) { margin: 22px 0 10px; font-size: 18px; }
.rich :deep(ul) { margin-left: 1.4em; list-style: disc; }
.rich :deep(ol) { margin-left: 1.4em; list-style: decimal; }
.back { margin-top: 30px; }
.back a { color: var(--c-primary); }
@media (max-width: 599px) { .title { font-size: 21px; } .rich { font-size: 15px; } }
</style>

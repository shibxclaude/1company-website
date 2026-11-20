<script setup>
// 新闻动态列表：后台维护，接口提供。第一页在预渲染时取数，翻页在浏览器加载。
import { ref, onMounted, onServerPrefetch } from 'vue'
import PageBanner from '../components/PageBanner.vue'
import SectionTitle from '../components/SectionTitle.vue'
import StateBlock from '../components/StateBlock.vue'
import { useSeo } from '../composables/useSeo.js'
import { api, ssrApiAvailable } from '../api/http.js'
import { useNewsStore } from '../stores/news.js'

const store = useNewsStore()
const loading = ref(false)
const error = ref('')

async function load(page = 1) {
  if (!ssrApiAvailable()) return
  loading.value = true
  error.value = ''
  try {
    const r = await api.newsPage(page, 10)
    store.list = page === 1 ? r.list : [...store.list, ...r.list]
    store.total = r.total
    store.page = page
    store.loaded = true
  } catch (e) {
    error.value = e.message
    if (import.meta.env.SSR) throw e
  } finally {
    loading.value = false
  }
}
onServerPrefetch(() => load(1))
onMounted(() => load(1))

const day = (t) => (t ? t.slice(0, 10) : '')

useSeo({ title: '新闻动态', description: '一零科技新闻动态：公司新闻、项目进展与行业资讯。' })
</script>

<template>
  <PageBanner image="/images/guanyu.png" title="新闻动态" />
  <section class="section">
    <SectionTitle lead="新闻" highlight="动态" tip="了解一零科技的最新动态与项目进展" />
    <StateBlock
      :loading="!store.loaded && (loading || !error)"
      :error="!store.list.length ? error : ''"
      :empty="store.loaded && !store.list.length"
      empty-text="暂无新闻，敬请关注。"
      @retry="load(1)"
    >
      <ul class="list">
        <li v-for="n in store.list" :key="n.id">
          <RouterLink :to="`/news/${n.id}`" class="item">
            <img v-if="n.cover" :src="n.cover" :alt="n.title" loading="lazy" class="cover" />
            <div class="txt">
              <h3><span v-if="n.top" class="top">置顶</span>{{ n.title }}</h3>
              <p v-if="n.summary">{{ n.summary }}</p>
              <time :datetime="day(n.publishedAt)">{{ day(n.publishedAt) }}</time>
            </div>
          </RouterLink>
        </li>
      </ul>
      <p v-if="store.list.length < store.total" class="more">
        <button type="button" :disabled="loading" @click="load(store.page + 1)">{{ loading ? '加载中…' : '加载更多' }}</button>
      </p>
    </StateBlock>
  </section>
</template>

<style scoped>
.list { max-width: 960px; margin: 0 auto; padding: 0 16px; }
.item { display: flex; gap: 20px; padding: 20px 0; border-bottom: 1px solid #eee; }
.item:hover h3 { color: var(--c-primary); }
.cover { flex: none; width: 200px; height: 128px; object-fit: cover; border-radius: 6px; }
.txt { flex: 1; min-width: 0; }
h3 { font-size: 18px; line-height: 1.5; transition: color 0.2s; }
.top { margin-right: 8px; padding: 1px 6px; font-size: 12px; color: #fff; background: #e4393c; border-radius: 3px; vertical-align: 2px; }
.txt p { margin-top: 8px; font-size: 14px; line-height: 1.8; color: #777; }
time { display: block; margin-top: 8px; font-size: 13px; color: #aaa; }
.more { margin-top: 24px; text-align: center; }
.more button { padding: 8px 30px; color: #00a8ff; background: #fff; border: 1px solid #00a8ff; border-radius: 18px; }
@media (max-width: 599px) { .cover { width: 110px; height: 72px; } h3 { font-size: 16px; } }
</style>

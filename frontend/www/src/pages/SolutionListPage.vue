<script setup>
// 解决方案列表：分类筛选、关键字搜索、价格展示、立即购买。数据来自接口（App.vue 已统一加载）。
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageBanner from '../components/PageBanner.vue'
import PriceTag from '../components/PriceTag.vue'
import StateBlock from '../components/StateBlock.vue'
import { useSeo } from '../composables/useSeo.js'
import { useCatalogStore } from '../stores/catalog.js'
import { useSiteStore } from '../stores/site.js'

const site = useSiteStore()

const catalog = useCatalogStore()
const route = useRoute()
const router = useRouter()

const active = ref(typeof route.query.category === 'string' ? route.query.category : '')
const keyword = ref(typeof route.query.q === 'string' ? route.query.q : '')

const list = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return catalog.solutions.filter(
    (s) =>
      (!active.value || s.categoryCode === active.value) &&
      (!kw || [s.name, s.subtitle, s.summary].some((t) => t && t.toLowerCase().includes(kw))),
  )
})

watch([active, keyword], () => {
  const query = {}
  if (active.value) query.category = active.value
  if (keyword.value.trim()) query.q = keyword.value.trim()
  router.replace({ query })
})

useSeo({
  title: '解决方案',
  description: computed(() =>
    catalog.solutions.length
      ? `一零科技解决方案与价格：${catalog.solutions.map((s) => s.name).join('、')}。支持在线下单、微信支付。`
      : '一零科技解决方案：公安警务、政务服务、开发平台，价格公开，支持在线下单。',
  ),
})
</script>

<template>
  <PageBanner title="解决方案" subtitle="深入公安、政务与城市治理的实战场景。价格公开透明，支持在线下单、微信扫码支付。" />
  <section class="section">

    <div class="toolbar">
      <div class="tabs" role="tablist" aria-label="分类">
        <button type="button" role="tab" :aria-selected="active === ''" :class="{ on: active === '' }" @click="active = ''">全部</button>
        <button
          v-for="c in catalog.categories"
          :key="c.code"
          type="button"
          role="tab"
          :aria-selected="active === c.code"
          :class="{ on: active === c.code }"
          @click="active = c.code"
        >
          {{ c.name }}
        </button>
      </div>
      <label class="search">
        <span class="sr-only">搜索解决方案</span>
        <input v-model="keyword" type="search" placeholder="搜索解决方案" maxlength="30" />
      </label>
    </div>

    <StateBlock
      :loading="!catalog.solutions.length && !catalog.error"
      :error="!catalog.solutions.length ? catalog.error || '' : ''"
      :empty="!list.length"
      empty-text="没有找到匹配的解决方案"
      @retry="catalog.loadList({ force: true })"
    >
      <ul class="grid">
        <li v-for="s in list" :key="s.slug" class="card">
          <RouterLink :to="`/solutions/${s.slug}`" class="cover">
            <img :src="s.mainImage" :alt="s.name" width="380" height="244" loading="lazy" />
            <span v-if="s.recommended === 1" class="badge">推荐</span>
          </RouterLink>
          <div class="body">
            <span class="cat">{{ s.categoryName }}</span>
            <h3><RouterLink :to="`/solutions/${s.slug}`">{{ s.name }}</RouterLink></h3>
            <p class="summary">{{ s.summary }}</p>
            <div class="foot">
              <PriceTag :price="s.minPrice" :original-price="s.multipleVersions ? null : s.minOriginalPrice" :unit="s.priceUnit" :from="s.multipleVersions" />
              <div class="actions">
                <RouterLink :to="`/solutions/${s.slug}`" class="detail">详情</RouterLink>
                <RouterLink :to="`/solutions/${s.slug}#buy`" class="buy">{{ site.shopEnabled ? '立即购买' : '咨询购买' }}</RouterLink>
              </div>
            </div>
          </div>
        </li>
      </ul>
    </StateBlock>
    <p class="price-link"><RouterLink to="/price">查看价格公示（全部版本与增值服务价目）</RouterLink></p>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  max-width: var(--wide-w);
  margin: -10px auto 30px;
  padding: 0 16px;
}
.tabs { display: flex; flex-wrap: wrap; gap: 10px; }
.tabs button {
  min-width: 96px;
  padding: 8px 20px;
  font-size: 15px;
  color: #333;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
}
.tabs button.on { color: #fff; background: var(--c-brand); border-color: var(--c-brand); }
.search input {
  width: 240px;
  height: 38px;
  padding: 0 16px;
  font-size: 14px;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
  outline: none;
}
.search input:focus { border-color: var(--c-brand); }
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 0 16px;
}
.card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s, transform 0.3s;
}
.card:hover { box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14); transform: translateY(-2px); }
.cover { position: relative; display: block; aspect-ratio: 380 / 244; overflow: hidden; background: #c9d6ea; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.badge { position: absolute; top: 10px; left: 10px; padding: 2px 10px; font-size: 12px; color: #fff; background: var(--c-brand); border-radius: 3px; }
.body { display: flex; flex: 1; flex-direction: column; padding: 16px 20px 20px; }
.cat { font-size: 12px; color: var(--c-primary); }
.body h3 { margin-top: 4px; font-size: 18px; line-height: 1.4; }
.body h3 a:hover { color: var(--c-primary); }
.summary { flex: 1; margin: 10px 0 16px; font-size: 14px; line-height: 1.8; color: var(--c-text-body); }
.foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.actions { display: flex; gap: 8px; }
.detail, .buy { padding: 6px 16px; font-size: 14px; border-radius: var(--radius-sm); white-space: nowrap; }
.detail { color: var(--c-brand); border: 1px solid var(--c-brand); }
.buy { color: #fff; background: var(--c-brand); }
.buy:hover { background: var(--c-brand-strong); }
.price-link { margin-top: 30px; text-align: center; }
.price-link a { color: var(--c-primary); }
@media (max-width: 991px) { .grid { grid-template-columns: repeat(2, 1fr); gap: 20px; } }
@media (max-width: 599px) {
  .grid { grid-template-columns: 1fr; }
  .tabs button { min-width: 0; padding: 6px 14px; font-size: 14px; }
  .search, .search input { width: 100%; }
}
</style>

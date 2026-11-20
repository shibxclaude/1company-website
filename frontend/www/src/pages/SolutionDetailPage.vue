<script setup>
// 解决方案详情：图集轮播、富文本详情、版本选择（价格联动）、购买数量、增值服务、金额汇总、交付与服务说明、立即购买。
import { ref, computed, watch, onMounted, onServerPrefetch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageBanner from '../components/PageBanner.vue'
import ImageGallery from '../components/ImageGallery.vue'
import PriceTag from '../components/PriceTag.vue'
import QtyInput from '../components/QtyInput.vue'
import StateBlock from '../components/StateBlock.vue'
import NotFoundPage from './NotFoundPage.vue'
import { useSeo } from '../composables/useSeo.js'
import { useCatalogStore } from '../stores/catalog.js'
import { useSiteStore } from '../stores/site.js'
import { estimateTotal, formatCents } from '../utils/money.js'

const route = useRoute()
const site = useSiteStore()
const router = useRouter()
const catalog = useCatalogStore()

const slug = computed(() => String(route.params.slug))
const solution = computed(() => catalog.details[slug.value])
const loading = ref(false)
const loadError = ref('')

async function load(force) {
  loading.value = solution.value === undefined
  loadError.value = ''
  try {
    await catalog.loadDetail(slug.value, { force })
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}
onServerPrefetch(() => catalog.loadDetail(slug.value))
// 浏览器里总是再取一次最新数据：后台改价后，即使访问的是预渲染页面也能看到新价格
onMounted(() => load(true))
watch(slug, () => load(false))

// ---------- 购买选项 ----------
const versionId = ref(null)
const quantity = ref(1)
const picked = ref({}) // serviceId -> 数量（0 表示未选）

watch(
  solution,
  (s) => {
    if (!s) return
    if (!s.versions.some((v) => v.id === versionId.value)) versionId.value = s.versions[0]?.id ?? null
    const next = {}
    s.services.forEach((sv) => { next[sv.id] = picked.value[sv.id] || 0 })
    picked.value = next
  },
  { immediate: true },
)

const version = computed(() => solution.value?.versions.find((v) => v.id === versionId.value) || null)
const selectedServices = computed(() =>
  (solution.value?.services || []).filter((s) => picked.value[s.id] > 0).map((s) => ({ ...s, quantity: picked.value[s.id] })),
)
const totalCents = computed(() => estimateTotal(version.value, quantity.value, selectedServices.value))

function toggleService(id, checked) {
  picked.value = { ...picked.value, [id]: checked ? Math.max(1, picked.value[id] || 1) : 0 }
}
function setServiceQty(id, n) {
  picked.value = { ...picked.value, [id]: n }
}

function buy() {
  if (!version.value) return
  router.push({
    path: '/order/confirm',
    query: {
      slug: slug.value,
      v: version.value.id,
      q: quantity.value,
      s: selectedServices.value.map((s) => `${s.id}:${s.quantity}`).join(',') || undefined,
    },
  })
}

useSeo({
  title: computed(() => (solution.value ? solution.value.name : solution.value === null ? '页面不存在' : '解决方案')),
  description: computed(() => {
    const s = solution.value
    if (!s) return ''
    const min = s.versions.length ? Math.min(...s.versions.map((v) => Number(v.price))) : null
    return `${s.name}：${s.summary || s.subtitle || ''}${min ? ` 价格 ¥${min.toLocaleString('zh-CN')} 起，支持在线购买。` : ''}`
  }),
})
</script>

<template>
  <NotFoundPage v-if="solution === null" />
  <template v-else>
    <PageBanner :title="solution?.name || '解决方案'" :subtitle="solution?.subtitle || ''" :parent="{ label: '解决方案', to: '/solutions' }" />
    <StateBlock :loading="loading && !solution" :error="!solution ? loadError : ''" @retry="load(true)">
      <article v-if="solution" class="detail">

        <div class="head">
          <ImageGallery :images="solution.gallery.length ? solution.gallery : [solution.mainImage]" :alt="solution.name" />

          <section id="buy" class="buybox" aria-label="购买">
            <span class="tag">{{ solution.categoryName }}</span>
            <h2 class="title">{{ solution.name }}</h2>
            <p v-if="solution.subtitle" class="subtitle">{{ solution.subtitle }}</p>

            <div class="price-row">
              <PriceTag v-if="version" size="lg" :price="version.price" :original-price="version.originalPrice" :unit="version.unit" />
            </div>

            <dl class="opts">
              <dt>版本</dt>
              <dd>
                <div class="versions" role="radiogroup" aria-label="选择版本">
                  <button
                    v-for="v in solution.versions"
                    :key="v.id"
                    type="button"
                    role="radio"
                    :aria-checked="v.id === versionId"
                    :class="{ on: v.id === versionId }"
                    @click="versionId = v.id"
                  >
                    {{ v.name }}<small>{{ v.billingLabel }}</small>
                  </button>
                </div>
                <p v-if="version?.description" class="vdesc">{{ version.description }}</p>
              </dd>

              <dt>{{ version?.billingType === 'YEARLY' ? '订阅年限' : '购买数量' }}</dt>
              <dd class="qty-row">
                <QtyInput v-model="quantity" :label="version?.billingType === 'YEARLY' ? '订阅年限' : '购买数量'" />
                <span class="unit">{{ version?.unit }}</span>
              </dd>

              <template v-if="solution.services.length">
                <dt>增值服务</dt>
                <dd>
                  <ul class="services">
                    <li v-for="sv in solution.services" :key="sv.id" :class="{ on: picked[sv.id] > 0 }">
                      <label>
                        <input type="checkbox" :checked="picked[sv.id] > 0" @change="toggleService(sv.id, $event.target.checked)" />
                        <span class="sv-name">{{ sv.name }}</span>
                        <PriceTag size="sm" :price="sv.price" :unit="sv.unit" />
                      </label>
                      <QtyInput
                        v-if="picked[sv.id] > 0"
                        :model-value="picked[sv.id]"
                        :label="`${sv.name}数量`"
                        @update:model-value="setServiceQty(sv.id, $event)"
                      />
                      <p v-if="sv.description" class="sv-desc">{{ sv.description }}</p>
                    </li>
                  </ul>
                </dd>
              </template>
            </dl>

            <div class="total">
              <span>合计</span>
              <strong>¥{{ formatCents(totalCents) }}</strong>
            </div>
            <div v-if="site.shopEnabled" class="actions">
              <button type="button" class="buy" :disabled="!version" @click="buy">立即购买</button>
              <a class="consult" :href="site.telHref">电话咨询</a>
            </div>
            <p v-if="site.shopEnabled" class="hint">无需注册即可下单，支持微信扫码支付；最终金额以订单确认页为准。</p>
            <template v-else>
              <div class="actions">
                <a class="buy" :href="site.telHref">电话咨询 {{ site.servicePhone }}</a>
              </div>
              <p class="hint">在线购买暂未开放，请电话咨询下单；以上为公示价格。</p>
            </template>
          </section>
        </div>

        <section class="service-info" aria-label="交付与服务说明">
          <div><h4>交付方式</h4><p>{{ solution.deliveryLabel }}</p></div>
          <div><h4>售后支持</h4><p>{{ solution.afterSales || '—' }}</p></div>
          <div><h4>服务期限</h4><p>{{ solution.servicePeriod || '—' }}</p></div>
        </section>

        <section class="block">
          <h3 class="block-title">方案详情</h3>
          <!-- 富文本由后端白名单清洗后保存 -->
          <div class="rich" v-html="solution.content" />
        </section>

        <p class="links">
          购买即表示同意 <RouterLink to="/agreement/purchase">《产品购买服务协议》</RouterLink>，
          全部价格见 <RouterLink to="/price">价格公示</RouterLink>
        </p>
      </article>
    </StateBlock>
  </template>
</template>

<style scoped>
.detail { max-width: var(--wide-w); margin: 0 auto; padding: 24px 16px 0; }
.crumbs { display: flex; flex-wrap: wrap; gap: 6px; font-size: 14px; color: #999; }
.crumbs a:hover { color: var(--c-primary); }
.crumbs [aria-current] { color: var(--c-brand); }

.head { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-top: 24px; align-items: start; }
.buybox { scroll-margin-top: 100px; }
.tag { display: inline-block; padding: 2px 10px; font-size: 13px; color: var(--c-primary); background: var(--c-brand-soft); border-radius: 4px; }
.title { margin-top: 10px; font-size: 26px; line-height: 1.35; color: #222; }
.subtitle { margin-top: 8px; font-size: 15px; color: #888; }
.price-row { margin-top: 16px; padding: 14px 18px; background: #fff4ec; border-radius: 6px; }
.opts { display: grid; grid-template-columns: 80px 1fr; gap: 18px 12px; margin: 22px 0 0; }
.opts dt { padding-top: 7px; font-size: 14px; color: #888; }
.opts dd { margin: 0; }
.versions { display: flex; flex-wrap: wrap; gap: 10px; }
.versions button {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 108px;
  padding: 6px 14px;
  font-size: 15px;
  color: #333;
  background: #fff;
  border: 1px solid var(--c-line);
  border-radius: 4px;
}
.versions button small { font-size: 12px; color: #999; }
.versions button.on { color: var(--c-brand); border-color: var(--c-brand); box-shadow: inset 0 0 0 1px var(--c-brand); }
.vdesc { margin-top: 8px; font-size: 13px; line-height: 1.7; color: #888; }
.qty-row { display: flex; align-items: center; gap: 8px; }
.unit { font-size: 14px; color: #666; }
.services li { padding: 8px 0; border-bottom: 1px dashed #eee; }
.services li:last-child { border-bottom: 0; }
.services label { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; cursor: pointer; }
.services input[type='checkbox'] { width: 16px; height: 16px; accent-color: var(--c-brand); }
.sv-name { font-size: 14px; color: #333; }
.services .qty { margin: 8px 0 0 24px; }
.sv-desc { margin: 4px 0 0 24px; font-size: 12px; color: #999; }
.total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #eee;
  font-size: 15px;
}
.total strong { font-size: 26px; color: var(--c-price); }
.actions { display: flex; gap: 12px; margin-top: 14px; }
.buy {
  flex: 1;
  height: 46px;
  font-size: 18px;
  color: #fff;
  background: var(--c-brand);
  border: 0;
  border-radius: var(--radius-sm);
}
.buy:hover { background: var(--c-brand-strong); }
.buy:disabled { background: #ccc; cursor: not-allowed; }
.consult {
  display: inline-flex;
  align-items: center;
  padding: 0 24px;
  font-size: 16px;
  color: var(--c-brand);
  border: 1px solid var(--c-brand);
  border-radius: var(--radius-sm);
}
.hint { margin-top: 10px; font-size: 12px; color: #999; }

.service-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 36px;
}
.service-info > div { padding: 18px 20px; background: var(--c-section-alt); border-radius: 8px; }
.service-info h4 { font-size: 15px; color: var(--c-heading-blue); margin-bottom: 6px; }
.service-info p { font-size: 14px; line-height: 1.8; color: #555; }

.block { margin-top: 44px; }
.block-title { padding-left: 12px; margin-bottom: 20px; font-size: 22px; line-height: 1.2; border-left: 4px solid var(--c-primary); }
.rich { font-size: 16px; line-height: 2; color: #444; overflow-wrap: anywhere; }
.rich :deep(p) { margin-bottom: 12px; }
.rich :deep(h2), .rich :deep(h3) { margin: 24px 0 10px; font-size: 18px; color: var(--c-heading-blue); }
.rich :deep(ul), .rich :deep(ol) { margin: 0 0 12px 1.4em; }
.rich :deep(ul) { list-style: disc; }
.rich :deep(ol) { list-style: decimal; }
.rich :deep(img) { display: block; max-width: 100%; height: auto; margin: 12px auto; }
.rich :deep(table) { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
.rich :deep(td), .rich :deep(th) { padding: 6px 10px; border: 1px solid #e5e5e5; }
.links { margin-top: 30px; font-size: 13px; color: #999; text-align: center; }
.links a { color: var(--c-primary); }

@media (max-width: 991px) {
  .head { grid-template-columns: 1fr; gap: 24px; }
}
@media (max-width: 767px) {
  .title { font-size: 21px; }
  .opts { grid-template-columns: 1fr; gap: 6px; }
  .opts dt { padding-top: 10px; }
  .service-info { grid-template-columns: 1fr; }
  .rich { font-size: 15px; line-height: 1.9; }
}
</style>

<script setup>
import { computed } from 'vue'
import RegionMap from '../components/RegionMap.vue'
import SectionTitle from '../components/SectionTitle.vue'
import PriceTag from '../components/PriceTag.vue'
import { useSeo } from '../composables/useSeo.js'
import { useCatalogStore } from '../stores/catalog.js'
import { serviceAreas, company, SITE_URL } from '../data/site.js'
import { useSiteStore } from '../stores/site.js'

const site = useSiteStore()

// 方案列表由 App.vue 统一加载（预渲染时取数，浏览器里刷新）
const catalog = useCatalogStore()
const solutions = computed(() => catalog.solutions)

useSeo({
  title: '',
  description: `${company.name}（${company.shortName}），国家高新技术企业。${company.description}`,
  jsonLd: {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: company.name,
    alternateName: company.shortName,
    url: SITE_URL,
    logo: `${SITE_URL}/images/downlogo.png`,
    foundingDate: company.foundingDate,
    address: {
      '@type': 'PostalAddress',
      streetAddress: '上地信息路1号2号楼2104',
      addressLocality: '海淀区',
      addressRegion: '北京',
      postalCode: '100085',
      addressCountry: 'CN',
    },
    contactPoint: { '@type': 'ContactPoint', telephone: `+86-${company.phone}`, contactType: 'sales', availableLanguage: ['Chinese'] },
    description: company.description,
    award: '国家高新技术企业认证',
  },
})
</script>

<template>
  <section class="hero">
    <div class="hero-inner">
      <div class="hero-copy">
        <h1>让分散的数据，<br />汇成一张网</h1>
        <p class="lede">一零科技为公安、政务与城市治理提供数据融合、物联网和大数据平台，从数据采集、融合到实战应用一站交付。</p>
        <div class="cta">
          <RouterLink to="/solutions" class="btn btn-primary">查看解决方案与价格</RouterLink>
          <RouterLink to="/price" class="btn btn-ghost">价格公示</RouterLink>
        </div>
        <dl class="facts">
          <div><dt>成立</dt><dd class="num">2015</dd></div>
          <div><dt>资质</dt><dd>国家高新技术企业</dd></div>
          <div><dt>在线可购</dt><dd><span class="num">{{ solutions.length || 6 }}</span> 套解决方案</dd></div>
        </dl>
      </div>
      <div class="hero-map">
        <RegionMap />
      </div>
    </div>
  </section>

  <section class="section">
    <SectionTitle highlight="服务领域" tip="应用数字化、智能化、信息化能力，覆盖政务、警务与智慧城市的主要场景" />
    <ul class="areas">
      <li v-for="a in serviceAreas" :key="a.name">
        <img :src="a.icon" alt="" width="40" height="40" loading="lazy" />
        <span>{{ a.name }}</span>
      </li>
    </ul>
  </section>

  <section class="section section--alt">
    <SectionTitle highlight="解决方案与价格" tip="价格公开，无需注册即可在线下单，支持微信扫码支付">
      <template #aside><RouterLink to="/price">查看完整价格公示</RouterLink></template>
    </SectionTitle>
    <ul class="recs">
      <li v-for="s in solutions" :key="s.slug" class="rec">
        <RouterLink :to="`/solutions/${s.slug}`" class="rec-thumb" tabindex="-1" aria-hidden="true">
          <img :src="s.mainImage" alt="" width="200" height="128" loading="lazy" />
        </RouterLink>
        <div class="rec-main">
          <p class="rec-cat">{{ s.categoryName }}<span v-if="s.recommended === 1" class="rec-flag">推荐</span></p>
          <h3><RouterLink :to="`/solutions/${s.slug}`">{{ s.name }}</RouterLink></h3>
          <p class="rec-sum">{{ s.summary }}</p>
        </div>
        <div class="rec-side">
          <PriceTag :price="s.minPrice" :unit="s.priceUnit" :from="s.multipleVersions" />
          <div class="rec-actions">
            <RouterLink :to="`/solutions/${s.slug}`" class="link">了解详情</RouterLink>
            <RouterLink :to="`/solutions/${s.slug}#buy`" class="buy">{{ site.shopEnabled ? '立即购买' : '咨询购买' }}</RouterLink>
          </div>
        </div>
      </li>
    </ul>
  </section>

  <section class="section">
    <SectionTitle highlight="合作伙伴" tip="公安、政务、高校与云服务等领域的合作单位，按拼音排序，排名不分先后" />
    <div class="partners">
      <img src="/images/hezuo.png" alt="合作伙伴：公安部第一、二、三研究所，多省公安厅，清华大学、中国人民公安大学，阿里云、腾讯云、百度、京东，中国电信、中国移动、中国联通等" width="1920" height="611" loading="lazy" />
    </div>
  </section>

  <section class="band">
    <div class="band-inner">
      <div>
        <h2>需要定制开发或批量采购？</h2>
        <p>工作日 9:00–18:00 直接与方案工程师沟通需求、部署环境与报价。</p>
      </div>
      <a class="btn btn-primary" :href="`tel:${company.phone}`">致电 <span class="num">{{ company.phoneDisplay }}</span></a>
    </div>
  </section>
</template>

<style scoped>
/* ---------- 首屏 ---------- */
.hero {
  position: relative;
  overflow: hidden;
  color: var(--c-on-dark);
  background:
    radial-gradient(1200px 600px at 85% 30%, rgba(29, 91, 255, 0.22), transparent 60%),
    linear-gradient(160deg, var(--c-ink) 55%, var(--c-ink-2));
}
.hero-inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  align-items: center;
  gap: 40px;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: calc(var(--header-h) + 56px) 24px 64px;
}
.hero h1 {
  font-size: var(--fs-hero);
  line-height: 1.18;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
}
.lede { margin-top: 20px; max-width: 30em; font-size: 17px; line-height: 1.8; color: var(--c-on-dark-2); }
.cta { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 32px; }
.facts {
  display: flex;
  flex-wrap: wrap;
  margin-top: 44px;
  border-top: 1px solid var(--c-line-dark);
}
.facts > div { padding: 16px 28px 0 0; margin-right: 28px; border-right: 1px solid var(--c-line-dark); }
.facts > div:last-child { border-right: 0; margin-right: 0; }
.facts dt { font-size: 12px; color: var(--c-on-dark-2); }
.facts dd { margin-top: 2px; font-size: 16px; color: #fff; }
.facts dd.num, .facts dd .num { font-size: 22px; font-weight: 600; }
.hero-map { min-width: 0; }

/* ---------- 按钮 ---------- */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 46px;
  padding: 0 24px;
  font-size: 15px;
  border-radius: var(--radius-sm);
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}
.btn-primary { color: #fff; background: var(--c-brand); }
.btn-primary:hover { background: var(--c-brand-strong); }
.btn-ghost { color: #fff; border: 1px solid rgba(255, 255, 255, 0.35); }
.btn-ghost:hover { border-color: #fff; }
.btn .num { font-size: 17px; font-weight: 600; }

/* ---------- 服务领域：细线分隔清单 ---------- */
.areas {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 0 24px;
}
.areas li {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 20px 22px 0;
  border-top: 1px solid var(--c-line);
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--c-ink);
}
.areas img { flex: none; width: 40px; height: 40px; }

/* ---------- 解决方案目录 ---------- */
.recs { max-width: var(--wide-w); margin: 0 auto; padding: 0 24px; }
.rec {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr) auto;
  gap: 28px;
  align-items: center;
  padding: 22px 0;
  border-top: 1px solid var(--c-line);
}
.rec:last-child { border-bottom: 1px solid var(--c-line); }
.rec-thumb { display: block; overflow: hidden; border-radius: var(--radius-sm); aspect-ratio: 200 / 128; background: #c9d6ea; }
.rec-thumb img { width: 100%; height: 100%; object-fit: cover; }
.rec-cat { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--c-text-3); }
.rec-flag { padding: 0 6px; font-size: 12px; line-height: 20px; color: #8a5300; background: #fff1d9; border-radius: 3px; }
.rec h3 { margin-top: 4px; font-size: var(--fs-lg); line-height: 1.4; }
.rec h3 a:hover { color: var(--c-brand); }
.rec-sum { margin-top: 6px; max-width: 40em; font-size: var(--fs-sm); color: var(--c-text-2); }
.rec-side { display: flex; flex-direction: column; align-items: flex-end; gap: 12px; min-width: 220px; }
.rec-actions { display: flex; align-items: center; gap: 16px; }
.link { font-size: var(--fs-sm); color: var(--c-brand); }
.link:hover { text-decoration: underline; }
.buy { padding: 8px 18px; font-size: var(--fs-sm); color: #fff; background: var(--c-brand); border-radius: var(--radius-sm); white-space: nowrap; }
.buy:hover { background: var(--c-brand-strong); }

/* ---------- 合作伙伴 ---------- */
.partners { max-width: var(--wide-w); margin: 0 auto; padding: 0 24px; }
.partners img { display: block; width: 100%; }

/* ---------- 咨询横幅 ---------- */
.band { margin-bottom: -72px; background: var(--c-ink); color: var(--c-on-dark); }
@media (max-width: 767px) { .band { margin-bottom: -48px; } }
.band-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 48px 24px;
}
.band h2 { font-size: var(--fs-xl); color: #fff; }
.band p { margin-top: 6px; color: var(--c-on-dark-2); }

@media (max-width: 1099px) {
  .hero-inner { grid-template-columns: 1fr; padding-top: calc(var(--header-h) + 40px); }
  .hero-map { max-width: 640px; }
  .areas { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 767px) {
  .hero-inner { padding: calc(var(--header-h) + 28px) 16px 40px; gap: 24px; }
  .lede { font-size: 15px; }
  .facts > div { padding-right: 16px; margin-right: 16px; }
  .areas, .recs, .partners { padding: 0 16px; }
  .areas li { padding: 16px 8px 16px 0; font-size: 14px; gap: 10px; }
  .areas img { width: 32px; height: 32px; }
  .rec { grid-template-columns: 112px minmax(0, 1fr); gap: 14px; align-items: start; }
  .rec-side { grid-column: 1 / -1; flex-direction: row; justify-content: space-between; align-items: center; min-width: 0; }
  .rec h3 { font-size: 16px; }
  .rec-sum { display: none; }
  .band-inner { padding: 36px 16px; }
}
</style>

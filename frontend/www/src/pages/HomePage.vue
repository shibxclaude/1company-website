<script setup>
import { computed } from 'vue'
import BannerCarousel from '../components/BannerCarousel.vue'
import SectionTitle from '../components/SectionTitle.vue'
import PriceTag from '../components/PriceTag.vue'
import { useSeo } from '../composables/useSeo.js'
import { useCatalogStore } from '../stores/catalog.js'
import { banners, serviceAreas, company, SITE_URL } from '../data/site.js'

// 方案列表由 App.vue 统一加载（预渲染时取数，浏览器里刷新）
const catalog = useCatalogStore()
const homeSolutions = computed(() => catalog.solutions.slice(0, 6))
const recommended = computed(() => catalog.solutions.filter((s) => s.recommended === 1).slice(0, 3))

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
    contactPoint: {
      '@type': 'ContactPoint',
      telephone: `+86-${company.phone}`,
      contactType: 'sales',
      availableLanguage: ['Chinese'],
    },
    description: company.description,
    award: '国家高新技术企业认证',
  },
})
</script>

<template>
  <h1 class="sr-only">{{ company.name }}</h1>
  <BannerCarousel :images="banners" alt="国家高新技术企业认证：专业化软件产品、系统化解决方案、数字化科技服务" />

  <section class="section section--alt">
    <SectionTitle lead="我们的" highlight="服务领域" tip="应用数字化、智能化、信息化能力为合作伙伴提供全面丰富的产品" />
    <ul class="areas">
      <li v-for="a in serviceAreas" :key="a.name" class="area">
        <img :src="a.icon" alt="" width="50" height="50" loading="lazy" />
        <span>{{ a.name }}</span>
      </li>
    </ul>
  </section>

  <section class="section">
    <SectionTitle lead="我们的" highlight="解决方案" tip="深入实战应用场景，聚焦行业客户，在政府多领域打造定制化的解决方案" />
    <ul class="solutions">
      <li v-for="s in homeSolutions" :key="s.slug">
        <RouterLink :to="`/solutions/${s.slug}`" class="sol-card">
          <span class="thumb"><img :src="s.mainImage" :alt="s.name" width="280" height="180" loading="lazy" /></span>
          <span class="name">{{ s.name }}</span>
        </RouterLink>
      </li>
    </ul>
  </section>

  <section v-if="recommended.length" class="section section--alt">
    <SectionTitle lead="推荐" highlight="解决方案" tip="价格公开透明，支持在线下单、微信扫码支付" />
    <ul class="recs">
      <li v-for="s in recommended" :key="s.slug" class="rec">
        <RouterLink :to="`/solutions/${s.slug}`" class="rec-cover">
          <img :src="s.mainImage" :alt="s.name" width="380" height="244" loading="lazy" />
        </RouterLink>
        <div class="rec-body">
          <h3><RouterLink :to="`/solutions/${s.slug}`">{{ s.name }}</RouterLink></h3>
          <p class="rec-sum">{{ s.summary }}</p>
          <div class="rec-foot">
            <PriceTag :price="s.minPrice" :unit="s.priceUnit" :from="s.multipleVersions" />
            <RouterLink :to="`/solutions/${s.slug}#buy`" class="buy">立即购买</RouterLink>
          </div>
        </div>
      </li>
    </ul>
    <p class="more"><RouterLink to="/price">查看全部价格 →</RouterLink></p>
  </section>

  <section class="section" :class="{ 'section--alt': !recommended.length }">
    <SectionTitle lead="我们的" highlight="合作伙伴" />
    <div class="partners">
      <img src="/images/hezuo.png" alt="合作伙伴：公安部第一、二、三研究所，多省公安厅，清华大学、中国人民公安大学，阿里云、腾讯云、百度、京东，中国电信、中国移动、中国联通等（按拼音排序，排名不分先后）" width="1920" height="611" loading="lazy" />
    </div>
  </section>
</template>

<style scoped>
.areas {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  max-width: var(--content-w);
  margin: 0 auto;
  padding: 0 16px;
}
.area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 5px;
  transition: box-shadow 0.3s;
}
.area:hover { box-shadow: 0 0 10px rgba(0, 0, 0, 0.3); }
.area img { width: 50px; height: 50px; }
.area span { margin-top: 20px; font-size: 17px; font-weight: 700; text-align: center; }

.solutions {
  display: grid;
  grid-template-columns: repeat(3, 280px);
  justify-content: space-around;
  gap: 20px 0;
  max-width: var(--content-w);
  margin: 0 auto;
  padding: 0 16px;
}
.sol-card { display: block; text-align: center; }
.thumb { display: block; width: 100%; aspect-ratio: 280 / 180; overflow: hidden; background: #87ceeb; }
.thumb img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.sol-card:hover img { transform: scale(1.06); }
.name { display: block; margin-top: 5px; font-size: 17px; font-weight: 700; }
.sol-card:hover .name { color: var(--c-primary); }

.recs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 0 16px;
}
.rec { display: flex; flex-direction: column; overflow: hidden; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06); }
.rec-cover { display: block; aspect-ratio: 380 / 220; overflow: hidden; background: #87ceeb; }
.rec-cover img { width: 100%; height: 100%; object-fit: cover; }
.rec-body { display: flex; flex: 1; flex-direction: column; padding: 16px 18px 18px; }
.rec-body h3 { font-size: 17px; }
.rec-body h3 a:hover { color: var(--c-primary); }
.rec-sum { flex: 1; margin: 8px 0 14px; font-size: 14px; line-height: 1.7; color: var(--c-text-body); }
.rec-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.buy { padding: 7px 18px; font-size: 14px; color: #fff; background: #e4393c; border-radius: 18px; white-space: nowrap; }
.buy:hover { background: #c82f32; }
.more { margin-top: 24px; text-align: center; }
.more a { color: var(--c-primary); }

.partners { max-width: var(--wide-w); margin: 0 auto; padding: 0 16px; }
.partners img { display: block; width: 100%; }

@media (max-width: 991px) {
  .solutions { grid-template-columns: repeat(2, 1fr); gap: 20px; }
  .recs { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 599px) {
  .recs { grid-template-columns: 1fr; }
}
@media (max-width: 767px) {
  .areas { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .area { padding: 14px 8px; }
  .area span { margin-top: 10px; font-size: 15px; }
  .solutions { gap: 16px 12px; }
  .name { font-size: 14px; }
}
</style>

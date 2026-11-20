<script setup>
import { company, nav } from '../data/site.js'
import { useSiteStore } from '../stores/site.js'

const site = useSiteStore()

const year = new Date().getFullYear()
const siteMap = nav
</script>

<template>
  <footer class="site-footer">
    <div class="top">
      <div class="brand">
        <img class="logo" src="/images/topLeft.png" alt="一零科技" width="120" height="60" loading="lazy" />
        <p>{{ company.description }}</p>
      </div>

      <nav class="col sitemap" aria-label="网站地图">
        <h2>网站导航</h2>
        <RouterLink v-for="item in siteMap" :key="item.key" :to="item.to">{{ item.label }}</RouterLink>
      </nav>

      <nav class="col legal" aria-label="服务与协议">
        <h2>购买与服务</h2>
        <RouterLink to="/price">价格公示</RouterLink>
        <RouterLink to="/agreement/purchase">产品购买服务协议</RouterLink>
        <RouterLink to="/agreement/privacy">隐私政策</RouterLink>
        <RouterLink v-if="site.shopEnabled" to="/user/orders">订单查询</RouterLink>
      </nav>

      <div class="col contact">
        <h2>联系我们</h2>
        <a :href="`tel:${company.phone}`" class="phone num">{{ company.phoneDisplay }}</a>
        <p>{{ company.address }}</p>
        <div class="qrcodes">
          <figure><img src="/images/ewm.png" alt="联系我们二维码" width="136" height="157" loading="lazy" /><figcaption>联系试用</figcaption></figure>
          <figure><img src="/images/gzh.png" alt="公众号二维码" width="165" height="161" loading="lazy" /><figcaption>关注公众号</figcaption></figure>
        </div>
      </div>
    </div>

    <div class="bottom">
      <p>©{{ year }} {{ company.name }} 版权所有</p>
      <p class="beian">
        <a :href="company.icp.url" target="_blank" rel="noopener noreferrer" class="icp">{{ company.icp.text }}</a>
        <a :href="company.police.url" target="_blank" rel="noopener noreferrer" class="police">
          <img src="/images/beian.png" alt="" width="16" height="18" />{{ company.police.text }}
        </a>
      </p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer { background: #081830; color: var(--c-on-dark-2); font-size: 14px; }
.top {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1.4fr;
  gap: 40px;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 56px 24px 40px;
}
.logo { display: block; width: 104px; height: 52px; }
.brand p { margin-top: 14px; max-width: 24em; line-height: 1.8; }
.col { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
.col h2 { margin-bottom: 6px; font-size: 14px; font-weight: 600; color: #fff; }
.col a:hover { color: #fff; }
.phone { font-size: 24px; font-weight: 600; color: #fff; letter-spacing: 0.02em; }
.qrcodes { display: flex; gap: 12px; margin-top: 6px; }
.qrcodes figure { margin: 0; text-align: center; font-size: 12px; }
.qrcodes img { display: block; width: 76px; height: 76px; object-fit: cover; border-radius: var(--radius-sm); background: #fff; padding: 3px; }
.qrcodes figcaption { margin-top: 4px; }
.bottom {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 24px;
  max-width: var(--wide-w);
  margin: 0 auto;
  padding: 18px 24px;
  border-top: 1px solid var(--c-line-dark);
  font-size: 13px;
}
.beian { display: flex; flex-wrap: wrap; gap: 8px 20px; }
.beian a { display: inline-flex; align-items: center; gap: 6px; }
.beian a:hover { color: #fff; }
.icp { color: var(--c-on-dark); text-decoration: underline; text-underline-offset: 3px; }
.beian img { width: 16px; height: 18px; }

@media (max-width: 991px) {
  .top { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 599px) {
  .top { grid-template-columns: 1fr 1fr; gap: 28px 16px; padding: 40px 16px 28px; }
  .brand, .contact { grid-column: 1 / -1; }
  .bottom { padding: 16px; flex-direction: column; }
}
</style>

<script setup>
import { company, nav } from '../data/site.js'

const year = new Date().getFullYear()
const siteMap = nav
</script>

<template>
  <footer class="site-footer">
    <div class="top">
      <div class="qrcodes">
        <img src="/images/ewm.png" alt="联系我们 立即试用" width="136" height="157" loading="lazy" />
        <img src="/images/gzh.png" alt="关注公众号 查看更多产品" width="165" height="161" loading="lazy" />
      </div>
      <img class="logo" src="/images/downlogo.png" alt="一零科技" width="150" height="42" loading="lazy" />
      <div class="info">
        <nav class="row sitemap" aria-label="网站地图">
          <span>网站地图：</span>
          <RouterLink v-for="item in siteMap" :key="item.key" :to="item.to">|{{ item.label }}</RouterLink>
        </nav>
        <p class="row"><span>地址：</span>{{ company.address }}</p>
        <p class="row">
          <span>服务热线：</span><a :href="`tel:${company.phone}`">{{ company.phoneDisplay }}</a>
        </p>
        <nav class="row legal" aria-label="服务与协议">
          <RouterLink to="/price">价格公示</RouterLink>
          <RouterLink to="/agreement/purchase">产品购买服务协议</RouterLink>
          <RouterLink to="/agreement/privacy">隐私政策</RouterLink>
          <RouterLink to="/user/orders">订单查询</RouterLink>
        </nav>
      </div>
    </div>
    <div class="bottom">
      <p class="line">
        <img src="/images/beian.png" alt="" width="18" height="20" />
        <a :href="company.police.url" target="_blank" rel="noopener noreferrer">{{ company.police.text }}</a>
        <a :href="company.icp.url" target="_blank" rel="noopener noreferrer" class="icp">{{ company.icp.text }}</a>
        <span>©{{ year }} {{ company.name }} 版权所有</span>
      </p>
      <p class="line">
        <span>联系电话：{{ company.phoneDisplay }}</span>
        <span>技术支持：{{ company.name }}</span>
      </p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  margin-top: 80px;
  background: var(--c-footer-bg);
  color: #fff;
}
.top {
  display: flex;
  justify-content: space-evenly;
  align-items: center;
  gap: 24px;
  padding: 40px 16px;
}
.qrcodes { display: flex; gap: 8px; align-items: flex-start; }
.qrcodes img { width: 110px; height: auto; border-radius: 6px; }
.logo { width: 150px; height: auto; }
.row { display: flex; flex-wrap: wrap; margin-top: 15px; font-size: 14px; }
.row:first-child { margin-top: 0; }
.sitemap a { margin-right: 10px; }
.legal { gap: 6px 16px; }
.legal a { text-decoration: underline; text-underline-offset: 3px; }
.sitemap a:hover, .row a:hover { text-decoration: underline; }
.bottom {
  padding: 10px 16px;
  border-top: 1px solid var(--c-footer-line);
  font-size: 12px;
}
.line {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 4px 10px;
  margin-bottom: 5px;
}
.line img { width: 18px; height: 20px; }
.icp { text-decoration: underline; text-underline-offset: 2px; }

@media (max-width: 767px) {
  .site-footer { margin-top: 40px; }
  .top { flex-direction: column; text-align: center; }
  .row { justify-content: center; }
  .qrcodes img { width: 96px; }
}
</style>

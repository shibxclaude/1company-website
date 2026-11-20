<script setup>
import PageBanner from '../components/PageBanner.vue'
import SectionTitle from '../components/SectionTitle.vue'
import { useSeo } from '../composables/useSeo.js'
import { history } from '../data/site.js'

useSeo({
  title: '发展历程',
  description: '一零科技发展历程：2015年成立，2017年获国家高新技术企业证书，深耕公安、政务、营商环境、物联网等领域。',
})
</script>

<template>
  <PageBanner image="/images/guanyu.png" title="发展历程" />
  <section class="section">
    <SectionTitle highlight="发展历程" />
    <ol class="timeline">
      <li v-for="(h, i) in history" :key="h.year" :class="i % 2 ? 'right' : 'left'">
        <div class="card">
          <time class="year">{{ h.year }}</time>
          <p v-for="(e, j) in h.events" :key="j">{{ e }}</p>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.timeline {
  position: relative;
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}
.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 4px;
  margin-left: -2px;
  border-radius: 2px;
  background: linear-gradient(#a9c5ea, #2b5aa8 15%, #e3b417 25%, #9a9a9a 40%, #9a9a9a 80%, #3b4f9c);
}
.timeline li { position: relative; width: 50%; padding: 0 36px 28px; }
.timeline li.left { text-align: right; }
.timeline li.right { margin-left: 50%; }
.timeline li::after {
  content: '';
  position: absolute;
  top: 14px;
  width: 14px;
  height: 14px;
  border: 3px solid var(--c-primary);
  border-radius: 50%;
  background: #fff;
}
.timeline li.left::after { right: -7px; }
.timeline li.right::after { left: -7px; }
.year { display: block; font-size: 30px; font-weight: 700; line-height: 1.3; color: var(--c-heading-blue); }
.timeline li:nth-child(4n + 2) .year,
.timeline li:nth-child(4n + 3) .year { color: #4fb89a; }
.card p { font-size: 14px; line-height: 1.9; color: #555; }

@media (max-width: 767px) {
  .timeline::before { left: 24px; }
  .timeline li, .timeline li.right { width: auto; margin-left: 0; padding: 0 0 22px 36px; text-align: left; }
  .timeline li.left::after, .timeline li.right::after { left: -2px; right: auto; }
  .year { font-size: 24px; }
}
</style>

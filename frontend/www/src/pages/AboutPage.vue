<script setup>
import PageBanner from '../components/PageBanner.vue'
import CredentialIcon from '../components/CredentialIcon.vue'
import { useSeo } from '../composables/useSeo.js'
import { aboutParagraphs, credentials } from '../data/site.js'

useSeo({
  title: '公司简介',
  description:
    '北京一零科技有限公司成立于2015年，国家高新技术企业，研发团队以博士、硕士为主，从事物联网技术应用、平台架构设计、大数据分析和云计算服务研发。',
})
</script>

<template>
  <PageBanner title="公司简介" subtitle="2015 年成立于北京海淀，国家高新技术企业。" />
  <section class="section">
    <div class="intro">
      <p v-for="(p, i) in aboutParagraphs" :key="i">{{ p }}</p>
    </div>
    <ul class="creds">
      <li v-for="c in credentials" :key="c.name">
        <CredentialIcon :name="c.icon" />
        <span>{{ c.name }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.intro { max-width: 940px; margin: 0 auto; padding: 0 16px; }
.intro p { max-width: 42em; font-size: 17px; line-height: 2; color: var(--c-text); }
.intro p + p { margin-top: 12px; }
.creds {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 940px;
  margin: 60px auto 0;
  padding: 0 16px;
  text-align: center;
}
.creds { border-top: 1px solid var(--c-line); padding-top: 36px; }
.creds li { display: flex; flex-direction: column; align-items: center; gap: 14px; font-size: 18px; font-weight: 600; color: var(--c-ink); }
@media (max-width: 767px) {
  .intro p { font-size: 15px; line-height: 2; }
  .creds { grid-template-columns: repeat(2, 1fr); margin-top: 36px; }
  .creds li { font-size: 16px; }
}
</style>

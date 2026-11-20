<script setup>
import { ref } from 'vue'
import PageBanner from '../components/PageBanner.vue'
import SectionTitle from '../components/SectionTitle.vue'
import { useSeo } from '../composables/useSeo.js'
import { faqs, company } from '../data/site.js'

const open = ref(0)
function toggle(i) {
  open.value = open.value === i ? -1 : i
}

useSeo({
  title: '常见问题',
  description: '关于一零科技产品与服务的常见问题：服务领域、派出所多维数据融合平台、物联网平台部署、二次开发周期、成功案例与合作方式。',
  jsonLd: {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: faqs.map((f) => ({ '@type': 'Question', name: f.q, acceptedAnswer: { '@type': 'Answer', text: f.a } })),
  },
})
</script>

<template>
  <PageBanner image="/images/guanyu.png" title="常见问题" />
  <section class="section">
    <SectionTitle lead="常见" highlight="问题" tip="为您解答关于一零科技产品与服务的常见疑问" />
    <div class="faq-list">
      <div v-for="(f, i) in faqs" :key="i" class="faq-item" :class="{ active: open === i }">
        <h3>
          <button
            :id="`faq-q-${i}`"
            type="button"
            class="faq-question"
            :aria-expanded="open === i"
            :aria-controls="`faq-a-${i}`"
            @click="toggle(i)"
          >
            <span>{{ f.q }}</span>
            <span class="arrow" aria-hidden="true">▼</span>
          </button>
        </h3>
        <div :id="`faq-a-${i}`" class="faq-answer" role="region" :aria-labelledby="`faq-q-${i}`">
          <p>{{ f.a }}</p>
        </div>
      </div>
      <p class="faq-contact">
        还有其他问题？欢迎 <a :href="`tel:${company.phone}`">电话咨询</a> 或通过官网联系我们的团队
      </p>
    </div>
  </section>
</template>

<style scoped>
.faq-list { max-width: 960px; margin: 0 auto; padding: 0 16px; }
.faq-item {
  margin-bottom: 16px;
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.3s;
}
.faq-item:hover { box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1); }
.faq-question {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 20px 28px;
  font-size: 17px;
  font-weight: 600;
  color: var(--c-faq-title);
  text-align: left;
  background: none;
  border: 0;
}
.faq-question:hover { background: #f8faff; }
.arrow { flex: none; margin-left: 16px; font-size: 14px; color: var(--c-faq-accent); transition: transform 0.3s; }
.active .arrow { transform: rotate(180deg); }
/* 用 grid-template-rows 0fr→1fr 做高度动画，答案再长也不会被截断 */
.faq-answer { display: grid; grid-template-rows: 0fr; transition: grid-template-rows 0.4s ease; }
.faq-answer > p { overflow: hidden; padding: 0 28px; font-size: 15px; line-height: 1.7; color: #444; }
.active .faq-answer { grid-template-rows: 1fr; }
.active .faq-answer > p { padding-bottom: 24px; }
.faq-contact { padding: 24px 0 8px; font-size: 15px; color: #666; text-align: center; }
.faq-contact a { font-weight: 600; color: var(--c-faq-accent); }
.faq-contact a:hover { text-decoration: underline; }
@media (max-width: 768px) {
  .faq-question { padding: 16px 20px; font-size: 15px; }
  .faq-answer > p { padding: 0 20px; font-size: 14px; }
  .active .faq-answer > p { padding-bottom: 18px; }
  .faq-contact { font-size: 14px; }
}
</style>

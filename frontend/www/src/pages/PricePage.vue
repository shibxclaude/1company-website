<script setup>
// 价格公示：全部在售解决方案的版本价格、计费方式、增值服务价目。无需登录。
import { onMounted, onServerPrefetch } from 'vue'
import PageBanner from '../components/PageBanner.vue'
import SectionTitle from '../components/SectionTitle.vue'
import StateBlock from '../components/StateBlock.vue'
import { useSeo } from '../composables/useSeo.js'
import { useCatalogStore } from '../stores/catalog.js'
import { money } from '../utils/money.js'
import { company } from '../data/site.js'

const catalog = useCatalogStore()
onServerPrefetch(() => catalog.loadPrice())
onMounted(() => catalog.loadPrice({ force: true }))

const unitText = (u) => (u ? `/${u}` : '')

useSeo({
  title: '价格公示',
  description: '一零科技全部解决方案价格公示：各版本价格、计费方式（一次性买断/按年订阅）与增值服务（运维、部署、培训）价目，公开透明，支持在线购买。',
})
</script>

<template>
  <PageBanner image="/images/chanpinzhongxin.png" title="价格公示" />
  <section class="section">
    <SectionTitle lead="价格" highlight="公示" tip="以下为全部在售解决方案的公开价格（人民币，含税），可在线下单并通过微信扫码支付" />
    <StateBlock
      :loading="!catalog.price && !catalog.error"
      :error="!catalog.price ? catalog.error || '' : ''"
      :empty="!!catalog.price && !catalog.price.solutions.length"
      empty-text="暂无在售解决方案"
      @retry="catalog.loadPrice({ force: true })"
    >
      <div v-if="catalog.price" class="wrap">
        <h3 class="h">一、解决方案价格</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th scope="col">解决方案</th><th scope="col">版本</th><th scope="col">计费方式</th><th scope="col" class="num">价格</th><th scope="col">版本说明</th><th scope="col">交付方式</th><th scope="col"></th></tr>
            </thead>
            <tbody>
              <template v-for="s in catalog.price.solutions" :key="s.id">
                <tr v-for="(v, i) in s.versions" :key="v.id">
                  <th v-if="i === 0" :rowspan="s.versions.length" scope="row" class="sol">
                    {{ s.name }}<small>{{ s.categoryName }}</small>
                  </th>
                  <td>{{ v.name }}</td>
                  <td>{{ v.billingLabel }}</td>
                  <td class="num">
                    <strong>{{ money(v.price) }}</strong><span class="u">{{ unitText(v.unit) }}</span>
                    <del v-if="v.originalPrice">{{ money(v.originalPrice) }}</del>
                  </td>
                  <td class="desc">{{ v.description || '—' }}</td>
                  <td v-if="i === 0" :rowspan="s.versions.length">{{ s.deliveryLabel }}</td>
                  <td v-if="i === 0" :rowspan="s.versions.length"><RouterLink :to="`/solutions/${s.slug}#buy`" class="buy">购买</RouterLink></td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <h3 class="h">二、增值服务价目</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th scope="col">所属解决方案</th><th scope="col">增值服务</th><th scope="col" class="num">价格</th><th scope="col">说明</th></tr>
            </thead>
            <tbody>
              <template v-for="s in catalog.price.solutions" :key="s.id">
                <tr v-for="(sv, i) in s.services" :key="sv.id">
                  <th v-if="i === 0" :rowspan="s.services.length" scope="row" class="sol">{{ s.name }}</th>
                  <td>{{ sv.name }}</td>
                  <td class="num"><strong>{{ money(sv.price) }}</strong><span class="u">/{{ sv.unit }}</span></td>
                  <td class="desc">{{ sv.description || '—' }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <h3 class="h">三、说明</h3>
        <ol class="notes">
          <li>以上价格均为人民币含税价。按年订阅的版本按"年"计价，购买时可选择订阅年限。</li>
          <li>服务期限：{{ catalog.price.solutions.map((s) => `${s.name}——${s.servicePeriod || '见方案详情'}`).join('；') }}。</li>
          <li>下单无需注册，支持微信扫码支付；订单提交后 30 分钟内未支付将自动关闭。</li>
          <li>交付、退款等事项见<RouterLink to="/agreement/purchase">《产品购买服务协议》</RouterLink>。如需定制或批量采购，请致电 {{ company.phoneDisplay }}。</li>
          <li>价格更新时间：{{ catalog.price.generatedAt }}</li>
        </ol>
      </div>
    </StateBlock>
  </section>
</template>

<style scoped>
.wrap { max-width: var(--wide-w); margin: 0 auto; padding: 0 16px; }
.h { margin: 32px 0 14px; font-size: 18px; }
.h:first-child { margin-top: 0; }
.table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; border: 1px solid #ebeef5; border-radius: 6px; }
table { width: 100%; min-width: 760px; border-collapse: collapse; font-size: 14px; }
th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #ebeef5; vertical-align: middle; }
thead th { font-weight: 600; color: #555; background: #f5f7fa; white-space: nowrap; }
tbody th.sol { font-weight: 600; color: #222; background: #fafbfc; border-right: 1px solid #ebeef5; min-width: 180px; }
.sol small { display: block; margin-top: 2px; font-weight: 400; font-size: 12px; color: var(--c-primary); }
.num { text-align: right; white-space: nowrap; }
.num strong { color: #e4393c; font-size: 15px; }
.num .u { margin-left: 2px; font-size: 12px; color: #e4393c; }
.num del { display: block; font-size: 12px; color: #aaa; }
.desc { color: #777; min-width: 200px; }
.buy { padding: 4px 14px; font-size: 13px; color: #fff; background: #e4393c; border-radius: 14px; white-space: nowrap; }
.notes { margin-left: 1.4em; list-style: decimal; font-size: 14px; line-height: 2; color: #555; }
.notes a { color: var(--c-primary); }
</style>

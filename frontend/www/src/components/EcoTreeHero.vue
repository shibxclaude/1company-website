<script setup>
// 首页首屏右侧：产品生态三维树（精简版）。不响应滚轮、只显示朝向镜头的标签；下方一行说明数据流向，并链接到完整生态图。
import { ref } from 'vue'
import { useEcoScene } from '../composables/useEcoScene.js'
import { ecoLayers } from '../data/ecosystem.js'

const stage = ref(null)
const { status } = useEcoScene(stage, { zoom: false, labelMode: 'front' })
// 数据流向：自下而上
const flow = [...ecoLayers].reverse()
</script>

<template>
  <figure class="eco-hero">
    <div ref="stage" class="eco-stage" :class="`is-${status}`" role="img" aria-label="产品生态三维图：感知层的设备与场所采集数据，经传输层汇聚，支撑业务应用层与智能应用层">
      <img
        v-if="status === 'fallback'"
        class="eco-fallback"
        src="/images/chanpinshengtai.png"
        alt=""
        width="1920"
        height="1842"
      />
    </div>
    <figcaption>
      <ol class="flow" aria-label="数据流向">
        <li v-for="l in flow" :key="l.id" :style="{ '--c': l.color }">{{ l.name }}</li>
      </ol>
      <RouterLink to="/products/ecosystem" class="more">查看完整产品生态图</RouterLink>
    </figcaption>
  </figure>
</template>

<style scoped>
.eco-hero { margin: 0; }
.eco-stage {
  position: relative;
  height: 540px;
  /* 加载前的占位：树冠位置一团微光，避免首屏右侧空白 */
  background: radial-gradient(38% 34% at 50% 40%, rgba(91, 147, 255, 0.16), transparent 70%);
}
.eco-stage.is-ready { background: none; }
.eco-fallback {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
  border-radius: var(--radius);
}
figcaption {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 24px;
  margin-top: 4px;
  font-size: 13px;
}
.flow { display: flex; flex-wrap: wrap; align-items: center; color: var(--c-on-dark-2); }
.flow li { display: flex; align-items: center; gap: 6px; }
.flow li::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--c);
  box-shadow: 0 0 8px var(--c);
}
/* 层与层之间的流向箭头 */
.flow li:not(:last-child) { margin-right: 22px; position: relative; }
.flow li:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -15px;
  top: 50%;
  width: 8px;
  height: 8px;
  margin-top: -4px;
  border-top: 1px solid currentColor;
  border-right: 1px solid currentColor;
  transform: rotate(45deg);
  opacity: 0.6;
}
.more { color: #fff; border-bottom: 1px solid rgba(255, 255, 255, 0.4); }
.more:hover { border-bottom-color: #fff; }
@media (max-width: 1023px) {
  .eco-stage { height: 440px; }
}
@media (max-width: 767px) {
  .eco-stage { height: 380px; }
  figcaption { justify-content: center; }
  .flow { justify-content: center; row-gap: 6px; }
}
</style>

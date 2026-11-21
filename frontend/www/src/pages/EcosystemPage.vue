<script setup>
// 产品生态图：上半部分是 Three.js 三维"数据之树"（只在浏览器端加载），下半部分是可预渲染的分层清单。
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import PageBanner from '../components/PageBanner.vue'
import { useSeo } from '../composables/useSeo.js'
import { ecoLayers, layerItems } from '../data/ecosystem.js'

useSeo({
  title: '产品生态图总览',
  description:
    '一零科技产品生态：感知层、传输层、业务应用层、智能应用层，涵盖多维数据融合平台、物联网开发平台、低代码二次开发平台、营商环境门户平台等。',
})

const stage = ref(null)
const active = ref(null)
const status = ref('loading') // loading | ready | fallback
const scene = shallowRef(null)

const count = (layer) => layerItems(layer).length
const unit = (layer) => (layer.id === 'net' ? '类数据' : '项')

function select(id) {
  active.value = active.value === id ? null : id
  scene.value?.focus(active.value)
}

onMounted(async () => {
  try {
    const probe = document.createElement('canvas')
    if (!(probe.getContext('webgl2') || probe.getContext('webgl'))) throw new Error('no webgl')
    const { createEcoScene } = await import('../utils/ecoScene.js')
    if (!stage.value) return
    scene.value = createEcoScene(stage.value, {
      layers: ecoLayers,
      reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches,
      coarsePointer: window.matchMedia('(pointer: coarse)').matches,
    })
    status.value = 'ready'
  } catch {
    status.value = 'fallback'
  }
})

onBeforeUnmount(() => scene.value?.dispose())
</script>

<template>
  <PageBanner
    title="产品生态图总览"
    subtitle="数据从感知层的前端设备出发，经传输层汇聚，支撑上层的业务应用与智能应用。"
    :parent="{ label: '产品中心', to: '/products' }"
  />

  <section class="eco" aria-labelledby="eco-3d-title">
    <h2 id="eco-3d-title" class="sr-only">三维产品生态图</h2>
    <div class="eco-inner">
      <nav class="strata" aria-label="按层查看">
        <button
          v-for="layer in ecoLayers"
          :key="layer.id"
          type="button"
          class="stratum"
          :class="{ 'is-on': active === layer.id, 'is-off': active && active !== layer.id }"
          :style="{ '--c': layer.color }"
          :aria-pressed="active === layer.id"
          :disabled="status !== 'ready'"
          @click="select(layer.id)"
        >
          <span class="stratum-name">{{ layer.name }}</span>
          <span class="stratum-count"><b class="num">{{ count(layer) }}</b>{{ unit(layer) }}</span>
          <span class="stratum-role">{{ layer.role }}</span>
        </button>
        <button v-if="active" type="button" class="reset" @click="select(null)">查看整棵树</button>
      </nav>

      <div ref="stage" class="stage" :class="`is-${status}`">
        <p v-if="status === 'loading'" class="stage-note">正在加载三维生态图…</p>
        <img
          v-else-if="status === 'fallback'"
          class="stage-fallback"
          src="/images/chanpinshengtai.png"
          alt="产品生态图：感知层、传输层、业务应用层、智能应用层"
          width="1920"
          height="1842"
        />
        <p v-if="status === 'ready'" class="hint">拖动可旋转查看，点层名可单独查看某一层</p>
      </div>
    </div>
  </section>

  <section class="ledger" aria-labelledby="eco-list-title">
    <div class="ledger-head">
      <h2 id="eco-list-title">各层产品清单</h2>
      <a class="flat-link" href="/images/chanpinshengtai.png" target="_blank" rel="noopener">查看平面版生态图</a>
    </div>
    <ol class="rows">
      <li v-for="layer in ecoLayers" :key="layer.id" class="row" :style="{ '--c': layer.color }">
        <div class="row-head">
          <h3>{{ layer.name }}</h3>
          <p>{{ layer.role }}</p>
        </div>
        <div class="row-body">
          <div v-for="(g, gi) in layer.groups" :key="gi" class="group">
            <h4 v-if="g.label">{{ g.label }}</h4>
            <ul>
              <li v-for="item in g.items" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
/* —— 三维舞台：紧接墨蓝页头，形成一整块深色面 —— */
.eco {
  position: relative;
  color: var(--c-on-dark);
  background:
    radial-gradient(60% 70% at 62% 45%, rgba(29, 91, 255, 0.22), transparent 70%),
    linear-gradient(180deg, var(--c-ink-2) 0%, var(--c-ink) 38%, #081629 100%);
  border-top: 1px solid var(--c-line-dark);
}
.eco-inner {
  display: grid;
  grid-template-columns: 248px 1fr;
  max-width: 1360px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 左侧分层图例：自上而下对应场景中的高度，竖向虚线轴呼应原图左侧的分层标注 */
.strata {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 40px 0;
}
.strata::before {
  content: '';
  position: absolute;
  top: 48px;
  bottom: 48px;
  left: 0;
  border-left: 1px dashed rgba(159, 176, 204, 0.35);
}
.stratum {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 2px 12px;
  padding: 14px 16px 14px 20px;
  text-align: left;
  color: inherit;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  transition: background-color 0.2s, opacity 0.2s;
}
.stratum::before {
  content: '';
  position: absolute;
  left: -4px;
  top: 21px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--c);
  box-shadow: 0 0 0 4px rgba(11, 31, 58, 1), 0 0 12px var(--c);
}
.stratum:hover:not(:disabled) { background: rgba(255, 255, 255, 0.05); }
.stratum:disabled { cursor: default; }
.stratum.is-on { background: rgba(255, 255, 255, 0.08); }
.stratum.is-off { opacity: 0.45; }
.stratum-name { font-size: var(--fs-md); font-weight: 600; color: #fff; }
.stratum-count { font-size: var(--fs-xs); color: var(--c-on-dark-2); align-self: center; }
.stratum-count b { margin-right: 2px; font-size: var(--fs-lg); font-weight: 600; color: var(--c); }
.stratum-role { grid-column: 1 / -1; font-size: 13px; line-height: 1.6; color: var(--c-on-dark-2); }
.reset {
  align-self: flex-start;
  margin: 10px 0 0 20px;
  padding: 6px 14px;
  font-size: var(--fs-sm);
  color: #fff;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
}
.reset:hover { border-color: #fff; }

.stage {
  position: relative;
  height: min(82vh, 780px);
  min-height: 520px;
  margin-right: -24px;
}
.stage-note,
.hint {
  position: absolute;
  font-size: var(--fs-xs);
  color: var(--c-on-dark-2);
}
.stage-note { inset: 50% 0 auto; text-align: center; }
.hint { right: 24px; bottom: 18px; pointer-events: none; }
.stage-fallback {
  position: absolute;
  inset: 24px 24px 24px 0;
  width: calc(100% - 24px);
  height: calc(100% - 48px);
  object-fit: contain;
  background: #fff;
  border-radius: var(--radius);
}
.stage :deep(.eco-canvas) { position: absolute; inset: 0; display: block; outline: none; }
.stage :deep(.eco-labels) { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }

/* 场景标签（由 CSS2DRenderer 定位；--depth 由场景按前后位置写入） */
.stage :deep(.eco-label) {
  --depth: 1;
  padding: 2px 7px;
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
  color: #fff;
  background: rgba(8, 22, 41, 0.62);
  border-radius: 3px;
  opacity: var(--depth);
  transition: opacity 0.25s, background-color 0.25s;
}
.stage :deep(.eco-label--sense) { color: #e8dfcc; }
.stage :deep(.eco-label--tag) { padding: 0 5px; font-size: 11px; color: #ffcf85; background: transparent; }
.stage :deep(.eco-label.is-dim) { opacity: calc(var(--depth) * 0.12); }
.stage :deep(.eco-label.is-active) { opacity: max(var(--depth), 0.7); background: rgba(8, 22, 41, 0.85); }

/* —— 分层清单 —— */
.ledger { max-width: var(--wide-w); margin: 0 auto; padding: 72px 24px 0; }
.ledger-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px 24px;
  margin-bottom: 20px;
}
.ledger-head h2 { font-size: var(--fs-2xl); line-height: 1.3; }
.flat-link { font-size: var(--fs-sm); color: var(--c-brand); }
.flat-link:hover { text-decoration: underline; }
.row {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px 48px;
  padding: 28px 0;
  border-top: 1px dashed var(--c-line);
}
.row:last-child { border-bottom: 1px dashed var(--c-line); }
.row-head h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--fs-lg);
  line-height: 1.4;
}
.row-head h3::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--c);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--c) 25%, transparent);
}
.row-head p { margin-top: 6px; font-size: var(--fs-sm); color: var(--c-text-2); max-width: 20em; }
.group + .group { margin-top: 18px; }
.group h4 { margin-bottom: 8px; font-size: var(--fs-sm); font-weight: 600; color: var(--c-text-3); }
.group ul { display: flex; flex-wrap: wrap; gap: 8px 28px; }
.group li { font-size: var(--fs-md); color: var(--c-ink); }

@media (max-width: 1023px) {
  .eco-inner { grid-template-columns: 1fr; padding: 0; }
  .strata {
    order: 2;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
    padding: 0 16px 24px;
  }
  .strata::before { display: none; }
  .stratum { display: flex; gap: 8px; padding: 8px 14px 8px 26px; border: 1px solid rgba(255, 255, 255, 0.16); border-radius: 999px; }
  .stratum::before { left: 11px; top: 50%; width: 7px; height: 7px; margin-top: -3.5px; box-shadow: 0 0 8px var(--c); }
  .stratum-name { font-size: var(--fs-sm); }
  .stratum-count b { font-size: var(--fs-sm); }
  .stratum-role { display: none; }
  .reset { margin: 0; }
  .stage { height: 72vh; min-height: 420px; max-height: 640px; margin-right: 0; }
  .stage :deep(.eco-label) { padding: 1px 5px; font-size: 11px; }
  .stage-fallback { inset: 16px; width: calc(100% - 32px); height: calc(100% - 32px); }
  .hint { left: 16px; right: 16px; bottom: 10px; text-align: center; }
  .ledger { padding: 48px 16px 0; }
  .row { grid-template-columns: 1fr; gap: 12px; padding: 22px 0; }
  .group ul { gap: 6px 20px; }
  .group li { font-size: var(--fs-sm); }
}
</style>

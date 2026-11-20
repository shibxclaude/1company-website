<script setup>
// 首屏主视觉："区域一张网"。等高线地形 + 经纬刻度 + 一条轨迹依次串起业务点位。
// 这是全站唯一的非交互动画：轨迹在页面加载时画一次；系统开启"减少动态效果"时直接显示终态。

// 两座"山"的等高线：同一条闭合曲线按比例缩放
const hillA = 'M0,-1 C0.55,-1 1,-0.62 1,-0.08 C1,0.5 0.6,0.95 0.02,0.98 C-0.6,1 -1.02,0.55 -0.98,-0.02 C-0.95,-0.6 -0.52,-1 0,-1 Z'
const hillB = 'M0,-1 C0.7,-0.95 1,-0.4 0.92,0.12 C0.85,0.68 0.35,1 -0.12,0.96 C-0.7,0.9 -1,0.42 -0.95,-0.12 C-0.9,-0.65 -0.5,-1.02 0,-1 Z'
const ringsA = [1, 0.82, 0.64, 0.47, 0.31, 0.16]
const ringsB = [1, 0.78, 0.56, 0.35]

// 业务点位（与解决方案能力对应），按轨迹经过的顺序排列
const nodes = [
  { x: 92, y: 360, label: '卡口' },
  { x: 205, y: 270, label: '社区' },
  { x: 300, y: 318, label: '派出所' },
  { x: 386, y: 196, label: '物联感知' },
  { x: 496, y: 236, label: '政务数据' },
  { x: 540, y: 112, label: '指挥中心' },
]
const path = nodes.map((n, i) => `${i ? 'L' : 'M'}${n.x},${n.y}`).join(' ')
const lon = ['113.4°E', '113.6°E', '113.8°E', '114.0°E']
const lat = ['34.8°N', '34.6°N', '34.4°N']
</script>

<template>
  <svg class="region-map" viewBox="0 0 600 460" role="img" aria-label="区域一张网示意：卡口、社区、派出所、物联感知、政务数据汇聚到指挥中心">
    <defs>
      <radialGradient id="rm-glow" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="#1d5bff" stop-opacity="0.35" />
        <stop offset="1" stop-color="#1d5bff" stop-opacity="0" />
      </radialGradient>
    </defs>

    <!-- 经纬网格与刻度 -->
    <g class="grid">
      <line v-for="(l, i) in lon" :key="'x' + i" :x1="90 + i * 150" y1="16" :x2="90 + i * 150" y2="430" />
      <line v-for="(l, i) in lat" :key="'y' + i" x1="20" :y1="100 + i * 140" x2="590" :y2="100 + i * 140" />
      <text v-for="(l, i) in lon" :key="'tx' + i" :x="94 + i * 150" y="448">{{ l }}</text>
      <text v-for="(l, i) in lat" :key="'ty' + i" x="24" :y="94 + i * 140">{{ l }}</text>
    </g>

    <!-- 等高线 -->
    <ellipse cx="300" cy="250" rx="260" ry="190" fill="url(#rm-glow)" />
    <g class="contour">
      <path v-for="s in ringsA" :key="'a' + s" :d="hillA" :transform="`translate(250 290) scale(${210 * s} ${150 * s})`" />
      <path v-for="s in ringsB" :key="'b' + s" :d="hillB" :transform="`translate(470 150) scale(${120 * s} ${96 * s})`" />
    </g>

    <!-- 轨迹 -->
    <path class="track-shadow" :d="path" />
    <path class="track" :d="path" pathLength="1" />

    <!-- 点位 -->
    <g v-for="(n, i) in nodes" :key="n.label" class="node" :style="{ '--d': `${0.25 + i * 0.32}s` }">
      <circle class="halo" :cx="n.x" :cy="n.y" r="13" />
      <circle class="dot" :cx="n.x" :cy="n.y" :r="i === nodes.length - 1 ? 6.5 : 4.5" />
      <text :x="n.x + 12" :y="n.y - 10">{{ n.label }}</text>
    </g>
  </svg>
</template>

<style scoped>
.region-map { display: block; width: 100%; height: auto; overflow: visible; }
.grid line { stroke: rgba(255, 255, 255, 0.07); stroke-dasharray: 2 6; }
.grid text { font-family: var(--font-num); font-size: 12px; letter-spacing: 0.04em; fill: rgba(159, 176, 204, 0.75); }
.contour path { fill: none; stroke: rgba(160, 190, 255, 0.22); stroke-width: 1; vector-effect: non-scaling-stroke; }
.contour path:nth-child(odd) { stroke: rgba(160, 190, 255, 0.13); }
.track-shadow { fill: none; stroke: rgba(255, 170, 44, 0.18); stroke-width: 7; stroke-linejoin: round; stroke-linecap: round; }
.track {
  fill: none;
  stroke: var(--c-signal);
  stroke-width: 2;
  stroke-linejoin: round;
  stroke-linecap: round;
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: draw 2.2s cubic-bezier(0.55, 0.1, 0.3, 1) 0.2s forwards;
}
.node { opacity: 0; animation: appear 0.4s ease-out var(--d) forwards; }
.halo { fill: rgba(255, 170, 44, 0.16); stroke: rgba(255, 170, 44, 0.45); stroke-width: 1; }
.dot { fill: var(--c-signal); stroke: var(--c-ink); stroke-width: 2; }
.node text { font-size: 13px; fill: var(--c-on-dark); paint-order: stroke; stroke: rgba(11, 31, 58, 0.85); stroke-width: 4px; }
@keyframes draw { to { stroke-dashoffset: 0; } }
@keyframes appear { to { opacity: 1; } }
@media (prefers-reduced-motion: reduce) {
  .track { animation: none; stroke-dashoffset: 0; }
  .node { animation: none; opacity: 1; }
}
</style>

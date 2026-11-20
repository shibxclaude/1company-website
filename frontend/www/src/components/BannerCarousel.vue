<script setup>
// 轻量轮播，替代原站 owl.carousel：自动播放、悬停暂停、圆点切换、触摸滑动。
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  images: { type: Array, required: true },
  interval: { type: Number, default: 4500 },
  alt: { type: String, default: '' },
})

const index = ref(0)
let timer = null
let touchX = null

function go(i) {
  const n = props.images.length
  index.value = (i + n) % n
}
function start() {
  stop()
  if (props.images.length > 1) timer = setInterval(() => go(index.value + 1), props.interval)
}
function stop() {
  if (timer) clearInterval(timer)
  timer = null
}
function onTouchStart(e) { touchX = e.touches[0].clientX; stop() }
function onTouchEnd(e) {
  if (touchX === null) return
  const dx = e.changedTouches[0].clientX - touchX
  if (Math.abs(dx) > 40) go(index.value + (dx < 0 ? 1 : -1))
  touchX = null
  start()
}

onMounted(start)
onBeforeUnmount(stop)
</script>

<template>
  <section
    class="carousel"
    aria-roledescription="carousel"
    @mouseenter="stop"
    @mouseleave="start"
    @touchstart.passive="onTouchStart"
    @touchend="onTouchEnd"
  >
    <div class="track" :style="{ transform: `translateX(-${index * 100}%)` }">
      <div v-for="(src, i) in images" :key="src" class="slide" :aria-hidden="i !== index">
        <img
          :src="src"
          :alt="i === 0 ? alt : ''"
          width="1920"
          height="934"
          :class="{ zoom: i === index }"
          :loading="i === 0 ? 'eager' : 'lazy'"
          :fetchpriority="i === 0 ? 'high' : 'auto'"
        />
      </div>
    </div>
    <div class="dots">
      <button
        v-for="(src, i) in images"
        :key="src"
        type="button"
        :class="{ on: i === index }"
        :aria-label="`第 ${i + 1} 张`"
        @click="go(i)"
      />
    </div>
  </section>
</template>

<style scoped>
.carousel { position: relative; overflow: hidden; background: #3d7ab8; }
.track { display: flex; transition: transform 0.6s ease; }
.slide { flex: 0 0 100%; overflow: hidden; }
.slide img { display: block; width: 100%; height: auto; }
.zoom { animation: grow 4.5s ease-in-out forwards; }
@keyframes grow {
  0% { transform: scale(1); }
  99% { transform: scale(1.05); }
  100% { transform: scale(1); }
}
.dots {
  position: absolute;
  bottom: 12px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 8px;
}
.dots button {
  width: 10px;
  height: 10px;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: #fff;
  opacity: 0.8;
  transition: width 0.3s, background 0.3s;
}
.dots button.on { width: 24px; background: #00a8ff; opacity: 1; }
@media (prefers-reduced-motion: reduce) {
  .track { transition: none; }
  .zoom { animation: none; }
}
</style>

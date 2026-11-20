<script setup>
// 详情页图集轮播：大图 + 缩略图，支持左右切换与触摸滑动
import { ref, computed, watch } from 'vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  alt: { type: String, default: '' },
})
const list = computed(() => props.images.filter(Boolean))
const index = ref(0)
watch(list, () => { index.value = 0 })
let touchX = null

function go(i) {
  const n = list.value.length
  if (n) index.value = (i + n) % n
}
function onTouchStart(e) { touchX = e.touches[0].clientX }
function onTouchEnd(e) {
  if (touchX === null) return
  const dx = e.changedTouches[0].clientX - touchX
  if (Math.abs(dx) > 40) go(index.value + (dx < 0 ? 1 : -1))
  touchX = null
}
</script>

<template>
  <div v-if="list.length" class="gallery">
    <div class="stage" @touchstart.passive="onTouchStart" @touchend="onTouchEnd">
      <img :src="list[index]" :alt="`${alt}（${index + 1}/${list.length}）`" />
      <template v-if="list.length > 1">
        <button type="button" class="nav prev" aria-label="上一张" @click="go(index - 1)">‹</button>
        <button type="button" class="nav next" aria-label="下一张" @click="go(index + 1)">›</button>
        <span class="counter">{{ index + 1 }} / {{ list.length }}</span>
      </template>
    </div>
    <div v-if="list.length > 1" class="thumbs">
      <button
        v-for="(src, i) in list"
        :key="src + i"
        type="button"
        :class="{ on: i === index }"
        :aria-label="`第 ${i + 1} 张`"
        @click="go(i)"
      >
        <img :src="src" alt="" loading="lazy" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.stage {
  position: relative;
  aspect-ratio: 14 / 9;
  overflow: hidden;
  background: #f2f4f7;
  border-radius: 8px;
}
.stage img { width: 100%; height: 100%; object-fit: contain; }
.nav {
  position: absolute;
  top: 50%;
  width: 36px;
  height: 52px;
  margin-top: -26px;
  font-size: 30px;
  line-height: 1;
  color: #fff;
  background: rgba(0, 0, 0, 0.35);
  border: 0;
}
.prev { left: 0; border-radius: 0 6px 6px 0; }
.next { right: 0; border-radius: 6px 0 0 6px; }
.nav:hover { background: rgba(0, 0, 0, 0.55); }
.counter {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 2px 10px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 10px;
}
.thumbs { display: flex; gap: 8px; margin-top: 10px; overflow-x: auto; }
.thumbs button {
  flex: none;
  width: 72px;
  height: 48px;
  padding: 0;
  overflow: hidden;
  background: #f2f4f7;
  border: 2px solid transparent;
  border-radius: 4px;
}
.thumbs button.on { border-color: #00a8ff; }
.thumbs img { width: 100%; height: 100%; object-fit: cover; }
</style>

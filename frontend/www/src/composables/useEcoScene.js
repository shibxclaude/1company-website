// 在给定容器里挂载产品生态三维树。Three.js 在 onMounted 之后才动态加载，不进入预渲染与首屏包。
// status：loading → ready；浏览器不支持 WebGL 或加载失败时为 fallback，由页面显示替代内容。
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { ecoLayers } from '../data/ecosystem.js'

export function useEcoScene(stageRef, options = {}) {
  const status = ref('loading')
  const scene = shallowRef(null)
  let unmounted = false

  onMounted(async () => {
    try {
      const probe = document.createElement('canvas')
      if (!(probe.getContext('webgl2') || probe.getContext('webgl'))) throw new Error('no webgl')
      const { createEcoScene } = await import('../utils/ecoScene.js')
      if (unmounted || !stageRef.value) return
      scene.value = createEcoScene(stageRef.value, {
        layers: ecoLayers,
        reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        coarsePointer: window.matchMedia('(pointer: coarse)').matches,
        ...options,
      })
      status.value = 'ready'
    } catch {
      if (!unmounted) status.value = 'fallback'
    }
  })

  onBeforeUnmount(() => {
    unmounted = true
    scene.value?.dispose()
  })

  return { status, scene }
}

// 产品生态图 3D 场景（Three.js）。只在浏览器端通过动态 import 加载，不进入预渲染包。
// 构图：地面以下是感知层根系，树干是传输层，树冠下半是业务应用层、上半是智能应用层。
// 琥珀色粒子沿"根 → 干 → 枝"流动，表示数据自下而上汇聚。
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import { layerItems } from '../data/ecosystem.js'

const GOLDEN = Math.PI * (3 - Math.sqrt(5))
const TRUNK_TOP = 3.2
const LEADER_TOP = 6.2
// 各层在场景中的高度范围（与虚线分界圈一致）
const BANDS = { sense: [-4.4, 0], net: [0, 3.3], biz: [3.3, 6.8], smart: [6.8, 10.4] }

// 伪随机：同一次构建得到同样的树形
function rng(seed) {
  let s = seed
  return () => ((s = (s * 16807) % 2147483647) - 1) / 2147483646
}

function glowTexture() {
  const c = document.createElement('canvas')
  c.width = c.height = 64
  const g = c.getContext('2d')
  const grd = g.createRadialGradient(32, 32, 0, 32, 32, 32)
  grd.addColorStop(0, 'rgba(255,255,255,1)')
  grd.addColorStop(0.22, 'rgba(255,255,255,0.85)')
  grd.addColorStop(0.5, 'rgba(255,255,255,0.18)')
  grd.addColorStop(1, 'rgba(255,255,255,0)')
  g.fillStyle = grd
  g.fillRect(0, 0, 64, 64)
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

function crownRadius(y) {
  // 树冠为椭球：中心 y=6.2，水平半径 6.6，竖直半径 4.6
  const k = (y - 6.2) / 4.6
  return 6.6 * Math.sqrt(Math.max(0.06, 1 - k * k))
}

function placeNodes(layer, count, rand) {
  const pts = []
  const [y0, y1] = BANDS[layer]
  const phase = { smart: 0.4, biz: 1.7, sense: 2.9 }[layer]
  for (let i = 0; i < count; i++) {
    const a = phase + i * GOLDEN
    const f = (i + 0.5) / count
    let y, r
    if (layer === 'sense') {
      y = -1.3 - 2.6 * ((i * 0.618) % 1)
      r = 3.2 + 4.2 * Math.sqrt(f)
    } else {
      y = y0 + 0.35 + (y1 - y0 - 0.7) * ((i * 0.618 + 0.1) % 1)
      r = crownRadius(y) * (0.78 + 0.22 * rand())
    }
    pts.push(new THREE.Vector3(Math.cos(a) * r, y, Math.sin(a) * r))
  }
  return pts
}

function tube(curve, radius, color, opacity) {
  const geo = new THREE.TubeGeometry(curve, 48, radius, 5, false)
  const mat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity, depthWrite: false })
  return new THREE.Mesh(geo, mat)
}

function dashedRing(y, radius, color) {
  const pts = []
  for (let i = 0; i <= 128; i++) {
    const a = (i / 128) * Math.PI * 2
    pts.push(new THREE.Vector3(Math.cos(a) * radius, y, Math.sin(a) * radius))
  }
  const geo = new THREE.BufferGeometry().setFromPoints(pts)
  const line = new THREE.Line(geo, new THREE.LineDashedMaterial({ color, dashSize: 0.22, gapSize: 0.18, transparent: true, opacity: 0.32 }))
  line.computeLineDistances()
  return line
}

/**
 * @param {HTMLElement} host 容器
 * @param {object} opts { layers, reducedMotion, coarsePointer }
 * @returns {{ focus(id|null), dispose() }}
 */
export function createEcoScene(host, { layers, reducedMotion = false, coarsePointer = false }) {
  const rand = rng(20261120)
  const width = () => host.clientWidth || 1
  const height = () => host.clientHeight || 1

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(width(), height())
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.domElement.className = 'eco-canvas'
  host.appendChild(renderer.domElement)

  const labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(width(), height())
  labelRenderer.domElement.className = 'eco-labels'
  host.appendChild(labelRenderer.domElement)

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(38, width() / height(), 0.1, 200)
  const home = { target: new THREE.Vector3(0, 3.0, 0), dist: 26 }

  const controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.enablePan = false
  controls.minDistance = 9
  controls.maxDistance = 48
  controls.minPolarAngle = Math.PI * 0.18
  controls.maxPolarAngle = Math.PI * 0.62
  controls.autoRotate = !reducedMotion
  controls.autoRotateSpeed = 0.55
  if (coarsePointer) {
    // 触屏：保留横向拖动旋转，竖向滑动交给页面滚动
    controls.enableZoom = false
    renderer.domElement.style.touchAction = 'pan-y'
  }

  const glow = glowTexture()
  const byLayer = { smart: [], biz: [], net: [], sense: [] } // 可淡化的对象：{ mat, base }
  const labels = [] // { obj, el, layer }
  const track = (layer, mat) => byLayer[layer].push({ mat, base: mat.opacity })

  function addLabel(text, pos, layer, cls = '') {
    const el = document.createElement('div')
    el.className = `eco-label eco-label--${layer} ${cls}`.trim()
    el.textContent = text
    const obj = new CSS2DObject(el)
    obj.position.copy(pos)
    obj.center.set(0.5, layer === 'sense' ? -0.35 : 1.35)
    scene.add(obj)
    labels.push({ obj, el, layer })
  }

  function addNode(pos, layer, color, size) {
    const mat = new THREE.SpriteMaterial({ map: glow, color, transparent: true, opacity: 0.95, depthWrite: false, blending: THREE.AdditiveBlending })
    const s = new THREE.Sprite(mat)
    s.position.copy(pos)
    s.scale.setScalar(size)
    scene.add(s)
    track(layer, mat)
  }

  const colorOf = Object.fromEntries(layers.map((l) => [l.id, new THREE.Color(l.color)]))

  // —— 分层虚线圈 ——
  const ringColor = new THREE.Color('#9fb0cc')
  for (const [y, r] of [[-4.4, 7.6], [0, 8.2], [3.3, 5.4], [6.8, 6.4], [10.4, 2.6]]) scene.add(dashedRing(y, r, ringColor))

  // 地面：极坐标网格，淡
  const grid = new THREE.PolarGridHelper(8.2, 12, 6, 96, 0x2a4a7a, 0x1c3560)
  grid.material.transparent = true
  grid.material.opacity = 0.35
  scene.add(grid)

  // —— 树干（传输层）：六股螺旋缆线 + 中央引导干 ——
  const trunkCurves = []
  for (let k = 0; k < 6; k++) {
    const pts = []
    for (let i = 0; i <= 24; i++) {
      const t = i / 24
      const y = -0.3 + t * (TRUNK_TOP + 0.3)
      const r = 0.55 - 0.3 * t
      const a = (k / 6) * Math.PI * 2 + t * Math.PI * 1.2
      pts.push(new THREE.Vector3(Math.cos(a) * r, y, Math.sin(a) * r))
    }
    const c = new THREE.CatmullRomCurve3(pts)
    trunkCurves.push(c)
    const m = tube(c, 0.045, colorOf.net, 0.55)
    scene.add(m)
    track('net', m.material)
  }
  const leader = new THREE.LineCurve3(new THREE.Vector3(0, TRUNK_TOP - 0.2, 0), new THREE.Vector3(0, LEADER_TOP, 0))
  {
    const m = tube(leader, 0.07, colorOf.biz, 0.45)
    scene.add(m)
    track('biz', m.material)
  }
  const spine = new THREE.LineCurve3(new THREE.Vector3(0, -0.3, 0), new THREE.Vector3(0, TRUNK_TOP, 0))

  // 传输层标签：沿树干螺旋排布的数据类型
  const netItems = layerItems(layers.find((l) => l.id === 'net'))
  netItems.forEach((name, i) => {
    const a = i * 2.2
    const y = 0.35 + (i / Math.max(1, netItems.length - 1)) * 2.6
    addLabel(name, new THREE.Vector3(Math.cos(a) * 1.15, y, Math.sin(a) * 1.15), 'net', 'eco-label--tag')
  })

  // —— 树冠与根系 ——
  const branches = { smart: [], biz: [], sense: [] }
  for (const layer of ['smart', 'biz', 'sense']) {
    const items = layerItems(layers.find((l) => l.id === layer))
    const pts = placeNodes(layer, items.length, rand)
    pts.forEach((p, i) => {
      const dir = new THREE.Vector3(p.x, 0, p.z).normalize()
      const r = Math.hypot(p.x, p.z)
      let curve
      if (layer === 'sense') {
        curve = new THREE.CubicBezierCurve3(
          p.clone(),
          dir.clone().multiplyScalar(r * 0.55).setY(p.y * 0.35),
          dir.clone().multiplyScalar(0.6).setY(-0.9),
          new THREE.Vector3(0, -0.3, 0),
        ) // 方向：根端 → 树干底，粒子沿此流入
      } else {
        // 分枝沿主干错落分出：业务层从树干上段，智能层从引导干上段
        const oy = layer === 'smart' ? 4.6 + rand() * (LEADER_TOP - 4.6) : TRUNK_TOP - 0.8 + rand() * 2.0
        const o = new THREE.Vector3(0, oy, 0)
        curve = new THREE.CubicBezierCurve3(
          o,
          dir.clone().multiplyScalar(r * 0.3).setY(oy + (p.y - oy) * 0.55),
          dir.clone().multiplyScalar(r * 0.62).setY(p.y + 0.15),
          p.clone(),
        )
      }
      branches[layer].push(curve)
      const m = tube(curve, layer === 'sense' ? 0.028 : 0.032, colorOf[layer], layer === 'sense' ? 0.42 : 0.5)
      scene.add(m)
      track(layer, m.material)
      addNode(p, layer, colorOf[layer], layer === 'sense' ? 0.55 : 0.75)
      addLabel(items[i], p, layer)
    })
  }

  // —— 数据粒子：根 → 干 → 枝 ——
  const routes = []
  for (let i = 0; i < 48; i++) {
    const path = new THREE.CurvePath()
    path.add(branches.sense[i % branches.sense.length])
    path.add(i % 2 ? spine : trunkCurves[i % 6])
    const top = i % 3 === 0 ? branches.smart : branches.biz
    const b = top[(i * 7) % top.length]
    path.add(new THREE.LineCurve3(new THREE.Vector3(0, TRUNK_TOP, 0), b.v0.clone())) // 沿主干接到分枝起点
    path.add(b)
    path.getLengths(200)
    routes.push(path)
  }
  const PCOUNT = 180
  const pState = Array.from({ length: PCOUNT }, (_, i) => ({ route: routes[i % routes.length], t: rand(), v: 0.035 + rand() * 0.04 }))
  const pPos = new Float32Array(PCOUNT * 3)
  const pGeo = new THREE.BufferGeometry()
  pGeo.setAttribute('position', new THREE.BufferAttribute(pPos, 3))
  const pMat = new THREE.PointsMaterial({ map: glow, color: colorOf.net, size: 0.32, transparent: true, opacity: 0.95, depthWrite: false, blending: THREE.AdditiveBlending })
  const particles = new THREE.Points(pGeo, pMat)
  scene.add(particles)
  const tmp = new THREE.Vector3()
  function updateParticles(dt) {
    pState.forEach((p, i) => {
      p.t = (p.t + p.v * dt) % 1
      p.route.getPointAt(p.t, tmp)
      pPos.set([tmp.x, tmp.y, tmp.z], i * 3)
    })
    pGeo.attributes.position.needsUpdate = true
  }
  updateParticles(0)

  // —— 镜头与聚焦 ——
  const goal = { target: home.target.clone(), dist: home.dist }
  let focused = null

  function fitDistance(span) {
    // 保证 span 宽的内容在窄屏上也能放下
    const vfov = THREE.MathUtils.degToRad(camera.fov)
    const byH = span / (2 * Math.tan(vfov / 2))
    const byW = span / (2 * Math.tan(vfov / 2) * camera.aspect)
    return Math.max(byH, byW) * 1.08
  }
  function homeDist() {
    // 窄屏允许两侧根梢略出画面，换取树冠更大
    return Math.min(46, Math.max(25, fitDistance(camera.aspect < 0.8 ? 12.5 : 17.5)))
  }

  function focus(id) {
    focused = id
    for (const [layer, list] of Object.entries(byLayer)) {
      const k = !id || layer === id ? 1 : 0.14
      list.forEach(({ mat, base }) => (mat.opacity = base * k))
    }
    labels.forEach(({ el, layer }) => {
      el.classList.toggle('is-dim', !!id && layer !== id)
      el.classList.toggle('is-active', !!id && layer === id)
    })
    if (!id) {
      goal.target.copy(home.target)
      goal.dist = homeDist()
    } else {
      const [y0, y1] = BANDS[id]
      goal.target.set(0, (y0 + y1) / 2, 0)
      goal.dist = Math.min(32, Math.max(14, fitDistance(id === 'net' ? 6 : 14)))
    }
    animating = true
  }

  camera.position.set(0, home.target.y + 2.2, homeDist())
  controls.target.copy(home.target)
  goal.dist = homeDist()
  let animating = false
  let idleTimer = 0
  controls.addEventListener('start', () => {
    animating = false
    controls.autoRotate = false
    clearTimeout(idleTimer)
  })
  controls.addEventListener('end', () => {
    if (reducedMotion) return
    idleTimer = setTimeout(() => (controls.autoRotate = true), 6000)
  })

  // 标签亮度随前后位置变化：背面的标签变淡，减少重叠
  const view = new THREE.Vector3()
  function shadeLabels() {
    const d = camera.position.distanceTo(controls.target)
    for (const { obj, el } of labels) {
      view.copy(obj.position).applyMatrix4(camera.matrixWorldInverse)
      const depth = -view.z - d // <0 在前，>0 在后
      const o = THREE.MathUtils.clamp(0.9 - depth * 0.22, 0.08, 1)
      el.style.setProperty('--depth', o.toFixed(2))
    }
  }

  // —— 渲染循环 ——
  let visible = true
  let raf = 0
  let last = performance.now()
  const io = new IntersectionObserver(([e]) => (visible = e.isIntersecting))
  io.observe(host)

  function frame(now) {
    raf = requestAnimationFrame(frame)
    const dt = Math.min(0.05, (now - last) / 1000)
    last = now
    if (!visible) return
    if (animating) {
      controls.target.lerp(goal.target, 0.08)
      const off = camera.position.clone().sub(controls.target)
      off.setLength(THREE.MathUtils.lerp(off.length(), goal.dist, 0.08))
      camera.position.copy(controls.target).add(off)
      if (controls.target.distanceTo(goal.target) < 0.01 && Math.abs(off.length() - goal.dist) < 0.02) animating = false
    }
    controls.update(dt)
    if (!reducedMotion) updateParticles(dt)
    shadeLabels()
    renderer.render(scene, camera)
    labelRenderer.render(scene, camera)
  }
  raf = requestAnimationFrame(frame)

  const ro = new ResizeObserver(() => {
    camera.aspect = width() / height()
    camera.updateProjectionMatrix()
    renderer.setSize(width(), height())
    labelRenderer.setSize(width(), height())
    if (!focused) {
      goal.dist = homeDist()
      animating = true
    }
  })
  ro.observe(host)

  return {
    focus,
    dispose() {
      cancelAnimationFrame(raf)
      clearTimeout(idleTimer)
      io.disconnect()
      ro.disconnect()
      controls.dispose()
      scene.traverse((o) => {
        o.geometry?.dispose()
        o.material?.dispose?.()
      })
      glow.dispose()
      renderer.dispose()
      renderer.domElement.remove()
      labelRenderer.domElement.remove()
    },
  }
}

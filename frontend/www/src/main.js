import { ViteSSG } from 'vite-ssg'
import { createPinia } from 'pinia'
import App from './App.vue'
import { routes, scrollBehavior } from './router/routes.js'
import './styles/tokens.css'
import './styles/base.css'

export const createApp = ViteSSG(App, { routes, scrollBehavior }, ({ app, initialState }) => {
  const pinia = createPinia()
  app.use(pinia)
  // 预渲染时把 store 状态序列化进 HTML，客户端水合时恢复，避免重复请求
  if (import.meta.env.SSR) initialState.pinia = pinia.state.value
  else if (initialState.pinia) pinia.state.value = initialState.pinia
})

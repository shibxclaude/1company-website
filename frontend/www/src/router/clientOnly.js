// 只在浏览器渲染、不做预渲染的路由（vite.config.js 与 routes.js 共用；本文件不能引用 .vue 组件）
export const clientOnlyPaths = [
  '/order/confirm',
  '/pay/:orderNo',
  '/user',
  '/user/login',
  '/user/register',
  '/user/orders',
  '/user/orders/:orderNo',
]

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 管理后台独立构建，部署在 /admin/ 路径下（nginx: location /admin/）
export default defineConfig({
  base: '/admin/',
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/uploads': 'http://127.0.0.1:8080',
    },
  },
  build: {
    chunkSizeWarningLimit: 1500,
  },
})

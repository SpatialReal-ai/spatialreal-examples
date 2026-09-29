import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { spatialRealVitePlugin } from '@spatialreal/web-sdk/vite'

export default defineConfig({
  plugins: [vue(), spatialRealVitePlugin()],
  server: {
    port: 5173,
    // server.js issues the session tokens
    proxy: { '/api': 'http://localhost:3000' },
  },
})

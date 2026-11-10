import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { spatialRealVitePlugin } from '@spatialreal/web-sdk/vite'

export default defineConfig({
  plugins: [vue(), spatialRealVitePlugin()],
  // agora-rtc-sdk-ng ships CommonJS and the SDK loads it lazily: pre-bundle it so the
  // dev server hands the SDK an ES module (vite build converts it either way)
  optimizeDeps: { include: ['agora-rtc-sdk-ng'] },
  server: {
    port: 5173,
    // server.js issues the tokens
    proxy: { '/api': 'http://localhost:3000' },
  },
})

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { spatialRealVitePlugin } from '@spatialreal/web-sdk/vite'

export default defineConfig({
  plugins: [vue(), spatialRealVitePlugin()],
  server: {
    port: 5173,
    proxy: {
      // server.js issues the session token
      '/api': 'http://localhost:3000',
      // Pipecat's development runner (uv run bot.py) starts a bot session
      '/start': 'http://localhost:7860',
    },
  },
})

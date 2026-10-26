import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: env.VITE_BASE || '/admin/',
    plugins: [vue()],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
    },
    server: {
      port: Number(env.VITE_PORT || 5173),
      proxy: {
        '/api': { target: env.VITE_API_TARGET || 'http://127.0.0.1:8080', changeOrigin: true },
        '/uploads': { target: env.VITE_API_TARGET || 'http://127.0.0.1:8080', changeOrigin: true }
      }
    }
  }
})

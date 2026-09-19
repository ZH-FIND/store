import path from 'path'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  build: { sourcemap: 'hidden' },
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  plugins: [vue()],
  test: {
    environment: 'jsdom',
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
})

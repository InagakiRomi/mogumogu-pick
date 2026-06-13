import { copyFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath, URL } from 'node:url'

import { loadEnv } from 'vite'
import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

/** GitHub Pages 找不到路由時改回傳 index.html，避免 SPA 重新整理 404 */
function githubPagesSpaFallback(): Plugin {
  return {
    name: 'github-pages-spa-fallback',
    closeBundle() {
      const outDir = join(fileURLToPath(new URL('.', import.meta.url)), '../docs')
      copyFileSync(join(outDir, 'index.html'), join(outDir, '404.html'))
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: mode === 'production' ? '/mogumogu-pick/' : '/',
    build: {
      outDir: '../docs',
      emptyOutDir: true,
    },
    plugins: [
      vue(),
      vueDevTools(),
      tailwindcss(),
      ...(mode === 'production' ? [githubPagesSpaFallback()] : []),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy:
        mode === 'development'
          ? {
              '/backend': {
                target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/backend/, ''),
              },
            }
          : undefined,
    },
  }
})

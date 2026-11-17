import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = {
    '/admin/goods': {
      target: env.GATEWAY_PROXY_TARGET || 'http://localhost:8090',
      changeOrigin: true,
    },
    '/admin/members': {
      target: env.GATEWAY_PROXY_TARGET || 'http://localhost:8090',
      changeOrigin: true,
    },
    '/auth': {
      target: env.GATEWAY_PROXY_TARGET || 'http://localhost:8090',
      changeOrigin: true,
    },
  }
  return {
    plugins: [
      vue(),
      AutoImport({ resolvers: [ElementPlusResolver()], dts: false }),
      Components({ resolvers: [ElementPlusResolver()], dts: false }),
    ],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    // 按需组件在懒加载页面中出现，提前预构建以免首次登录时触发开发服务器整页刷新。
    optimizeDeps: {
      include: [
        'element-plus',
        'element-plus/es',
        ...[
          'base',
          'config-provider',
          'form',
          'form-item',
          'input',
          'button',
          'alert',
          'icon',
          'menu',
          'menu-item',
          'breadcrumb',
          'breadcrumb-item',
          'avatar',
          'dropdown',
          'dropdown-menu',
          'dropdown-item',
          'table',
          'table-column',
          'tag',
          'radio-group',
          'radio-button',
          'empty',
          'descriptions',
          'descriptions-item',
          'message',
          'message-box',
        ].map((name) => `element-plus/es/components/${name}/style/css`),
      ],
    },
    server: { port: 5174, strictPort: true, proxy },
    preview: { port: 4174, strictPort: true, proxy },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            'chart-renderer': ['zrender'],
            charts: ['echarts/core'],
          },
        },
      },
    },
  }
})

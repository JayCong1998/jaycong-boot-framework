import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import App from './App.vue'
import { router } from './router'
import { setUnauthorizedHandler } from './services/http'
import { useAuthStore } from './stores/auth'
import './styles/main.css'

const app = createApp(App)
app.use(createPinia())
setUnauthorizedHandler(() => {
  useAuthStore().clear()
  const current = router.currentRoute.value
  if (current.meta.requiresAuth) {
    ElMessage.warning('登录已失效，请重新登录')
    void router.replace({
      name: 'login',
      query: { redirect: current.fullPath, reason: 'expired' },
    })
  }
})
app.use(router)
app.mount('#app')

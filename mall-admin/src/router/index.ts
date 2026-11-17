import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/services/http'
import { readToken, SessionChangedError } from '@/utils/session'
import { navigation, safeRedirect } from './navigation'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    description?: string
  }
}

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/dashboard',
      children: [
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/ProductsView.vue'),
          meta: { title: '商品管理' },
        },
        {
          path: 'members',
          name: 'members',
          component: () => import('@/views/MembersView.vue'),
          meta: { title: '会员列表' },
        },
        {
          path: 'members/operations',
          name: 'member-operations',
          component: () => import('@/views/MemberOperationsView.vue'),
          meta: { title: '会员操作记录' },
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { title: '系统设置' },
        },
        ...navigation
          .filter((item) => !['/dashboard', '/settings', '/members', '/products'].includes(item.path))
          .map((item) => ({
            path: item.path.slice(1),
            name: item.path.slice(1),
            component: () => import('@/views/ModuleView.vue'),
            meta: { title: item.title, description: item.description },
          })),
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!to.meta.requiresAuth && to.name !== 'login') return true
  if (!readToken())
    return to.meta.requiresAuth
      ? { name: 'login', query: { redirect: to.fullPath } }
      : true
  try {
    const user = await auth.refreshUser()
    if (user?.role !== 'admin') {
      // 前端仅提供后台入口限制，业务接口仍需服务端独立授权。
      try {
        await auth.logout()
      } catch {
        return to.name === 'login'
          ? true
          : { name: 'login', query: { reason: 'forbidden-logout' } }
      }
      return to.name === 'login'
        ? true
        : { name: 'login', query: { reason: 'forbidden' } }
    }
    if (to.name === 'login') return safeRedirect(to.query.redirect)
    return true
  } catch (error) {
    // 401 清理由 HTTP 层按请求 Token 执行；旧导航不能清除新登录的会话。
    if (error instanceof SessionChangedError) return false
    if (to.name === 'login') return true
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
        reason:
          error instanceof ApiError && error.status === 401
            ? 'expired'
            : 'unavailable',
      },
    }
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '管理后台'} · mall-admin`
})

import { ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/services/auth'
import { ApiError, errorMessage } from '@/services/http'
import {
  readToken,
  removeToken,
  saveToken,
  SessionChangedError,
} from '@/utils/session'
import type { LoginRequest, User } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const loggingOut = ref(false)
  function clear() {
    removeToken()
    user.value = null
  }

  async function refreshUser() {
    const token = readToken()
    if (!token) {
      user.value = null
      return null
    }
    let current: User
    try {
      current = await authApi.me()
    } catch (error) {
      if (readToken() && readToken() !== token) throw new SessionChangedError()
      throw error
    }
    if (readToken() !== token) throw new SessionChangedError()
    user.value = current
    return current
  }

  async function login(request: LoginRequest) {
    const result = await authApi.login({
      account: request.account.trim(),
      password: request.password,
    })
    if (!result.tokenValue) throw new Error('认证服务未返回有效令牌')
    try {
      saveToken(result.tokenValue)
    } catch {
      throw new Error('浏览器无法保存会话，请允许此站点使用会话存储后重试')
    }
    // 角色由后端 /me 确认，不仅依赖登录响应或本地缓存。
    const current = await refreshUser()
    if (current?.role !== 'admin') {
      try {
        await logout()
      } catch (error) {
        throw new ApiError(
          `当前账号没有后台管理权限，且会话退出失败：${errorMessage(error)}。请重试退出当前会话`,
          403,
        )
      }
      throw new ApiError('当前账号没有后台管理权限，请使用管理员账号登录', 403)
    }
  }

  async function logout() {
    if (loggingOut.value) return
    loggingOut.value = true
    const token = readToken()
    try {
      if (token) await authApi.logout()
      if (readToken() && readToken() !== token) throw new SessionChangedError()
      clear()
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.status === 401 &&
        (!readToken() || readToken() === token)
      )
        clear()
      else throw error // 网络异常保留会话，允许用户重试撤销服务端 Token。
    } finally {
      loggingOut.value = false
    }
  }

  return { user, loggingOut, clear, refreshUser, login, logout }
})

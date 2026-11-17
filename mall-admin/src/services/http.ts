import axios from 'axios'
import type { Result } from '@/types/auth'
import { readToken } from '@/utils/session'

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

let onUnauthorized: (() => void) | undefined
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 12000,
})
http.interceptors.request.use((config) => {
  const token = readToken()
  if (token && config.url !== '/auth/login')
    config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

function handleUnauthorized(status: number, authorization: unknown) {
  // 旧请求的迟到 401 不得清除刚刚建立的新会话；错误密码不触发全局跳转。
  const token = readToken()
  if (status === 401 && token && authorization === `Bearer ${token}`)
    onUnauthorized?.()
}

http.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown> | undefined
    if (result?.code !== 0) {
      const status = result?.code === 401 ? 401 : response.status
      handleUnauthorized(status, response.config.headers.get('Authorization'))
      throw new ApiError(
        result?.message || '服务返回格式异常，请稍后重试',
        status,
      )
    }
    return response
  },
  (error: unknown) => {
    if (!axios.isAxiosError(error)) return Promise.reject(error)
    const status = error.response?.status ?? 0
    handleUnauthorized(status, error.config?.headers?.get('Authorization'))
    const message = error.response?.data?.message
    return Promise.reject(
      new ApiError(
        typeof message === 'string'
          ? message
          : error.code === 'ECONNABORTED'
            ? '请求超时，请稍后重试'
            : status === 0
              ? '无法连接服务，请检查网络或确认网关和 Auth 服务已启动'
              : `服务暂不可用（${status}），请稍后重试`,
        status,
      ),
    )
  },
)

export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

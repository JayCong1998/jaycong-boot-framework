import { http } from './http'
import type { LoginRequest, LoginResponse, Result, User } from '@/types/auth'

export const authApi = {
  async login(request: LoginRequest) {
    return (await http.post<Result<LoginResponse>>('/auth/login', request)).data
      .data
  },
  async me() {
    return (await http.get<Result<User>>('/auth/me')).data.data
  },
  async logout() {
    await http.delete<Result<null>>('/auth/session')
  },
}

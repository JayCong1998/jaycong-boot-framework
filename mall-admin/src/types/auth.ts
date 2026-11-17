export interface User {
  id: string
  mobile: string | null
  email: string | null
  nickname: string
  role: string
}

export interface LoginRequest {
  account: string
  password: string
}
export interface LoginResponse {
  tokenName: string
  tokenValue: string
  timeout: number
  user: User
}
export interface Result<T> {
  code: number
  message: string
  data: T
}

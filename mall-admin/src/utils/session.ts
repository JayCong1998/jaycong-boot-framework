// 与 mall-web 隔离，不保存密码或信任浏览器缓存的角色。
const TOKEN_KEY = 'mall-admin:token'
export class SessionChangedError extends Error {
  constructor() {
    super('登录会话已变化，请使用当前会话继续操作')
    this.name = 'SessionChangedError'
  }
}
export const readToken = () => sessionStorage.getItem(TOKEN_KEY)
export const saveToken = (token: string) =>
  sessionStorage.setItem(TOKEN_KEY, token)
export const removeToken = () => sessionStorage.removeItem(TOKEN_KEY)

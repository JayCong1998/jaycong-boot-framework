# 简选商城前端

基于 React 19、TypeScript、Vite、Tailwind CSS 4、Radix UI、Zustand 和 React Router。独立于 Maven 模块。

## 启动

建议 Node.js 22.19+（或符合 Vite 7 要求的版本）。在此目录执行：

```powershell
npm ci
npm run dev
```

打开 http://127.0.0.1:5173 。首页 `/`、登录 `/login`、注册 `/register`。

```powershell
npm run typecheck
npm run build
npm run preview
```

## 目录

- `src/views`：首页、登录注册页面容器。
- `src/components/layout`：品牌、导航、页脚。
- `src/components/catalog`：分类导航、活动轮播、商品卡片。
- `src/components/auth`：登录注册表单与校验。
- `src/components/cart`：购物车抽屉。
- `src/components/ui`：Button、Input 基础组件；购物车使用 Radix Dialog 实现模态抽屉。
- `src/services`：HTTP、超时、Result 响应与认证接口。
- `src/stores`：认证状态、购物车、轻提示。
- `src/data`、`src/types`：示例商品、分类与类型。
- `public/images`：从用户提供的原型截图裁取的演示素材，正式上线应替换为获得授权的原始高清素材。

## 认证接入

开发与本地预览服务器将 `/auth` 统一代理到后端网关 `http://localhost:8090`，网关再通过服务发现转发到 Auth。先按仓库 `docs/auth.md`、`docs/gateway.md` 启动 Auth 和网关（包括所需 MySQL、Redis、Nacos）。开发默认配置位于 `.env.development`；若网关地址变化，复制 `.env.example` 为 `.env.local`，修改 `GATEWAY_PROXY_TARGET` 后重启 Vite。

请求链路：浏览器 `/auth/login`、`/auth/register` → Vite 同源代理 → 网关 `8090` → Auth。无需前端直连 `8084`，也无需为开发代理额外开放跨域。

- 登录调用 `POST /auth/login`，会话 Token 保存在当前标签页 sessionStorage，后续请求携带 `Authorization: Bearer`。不保存密码。
- 刷新后调用 `GET /auth/me` 恢复用户；401 清理无效 Token。暂时网络错误保留 Token，重载后可重试。
- 注册调用 `POST /auth/register`，支持手机号或邮箱注册；成功后跳转登录，不自动登录。
- 退出调用 `POST /auth/logout`，服务端成功或已失效后清理本地会话；网络异常提示错误并保留会话以便重试。
- 暂无短信验证码、找回密码或第三方登录入口，避免展示后端未提供的能力。

## 功能边界与部署

商品通过同源 `/goods` 请求后端网关，再转发至 goods 服务；使用 `page`、`size`、`name` 查询。首页默认展示第一页前五件，查看更多显示本页完整列表；每页 20 件，按接口 total 显示分页。名称搜索由后端执行。接口没有分类、原价、标签和排序字段，因此分类按钮暂不可用，不展示虚构原价、推荐标签或价格排序。加载时显示提示，失败时提供重试，无数据时展示空状态。轮播展示三组文案，共用原型横幅素材，支持自动切换和手动切换，悬停或聚焦时暂停。

购物车仅为当前浏览器的示例选购清单，使用 localStorage 的 `mall-cart-v2` 保存接口商品快照和数量（旧版本的本地演示 ID 不迁移），不按账号隔离、不接入真实库存与订单。快照价格仅为选购时价格，真实结算必须由后端核价；支持数量 1–99、移除、合计展示；未实现订单与支付。

生产部署上传 `dist`，Web 服务器需要配置 SPA 路由回退到 `index.html`，并将同源 `/auth`、`/goods` 反向代理至网关。Vite 的代理配置不包含在生产产物中；`npm run preview` 支持本地网关联调，但不用于正式部署。不要把数据库密码等敏感配置放入前端环境变量。

## 实际验证

迁移前 Vue 版本曾执行 `npm run build`（包含 vue-tsc 类型检查），浏览器手工检查桌面首页、登录页、390px 手机首页和登录页、购物车加入/增加/移除及金额联动、注册空表单校验、搜索空状态。未新增或修改测试代码。未使用真实账号执行注册、登录与退出，不代表已验证完整后端认证流程。

### 网关联调（2026-11-10）

通过前端地址 `http://127.0.0.1:5173/auth/*` 验证实际网关链路：`/auth/status` 返回 200；使用已有开发初始账号登录成功、`/auth/me` 返回当前用户、退出成功、退出后访问 `/auth/me` 返回 401。注册空请求返回 400，确认请求到达后端参数校验；未创建新账号，未验证注册成功落库。验证中未输出 Token 或密码。

### 商品接口联调（2026-11-11）

已移除前端硬编码商品数据，增加 `src/services/goods.ts` 和商品分页响应类型；购物车保存已选接口商品快照，不依赖当前页。实际通过前端 5173 代理验证商品列表（6 件）、名称搜索（耳机 2 件）、分页和空结果；执行 `npm run build` 成功（包含类型检查）。未新增测试代码；本次未执行浏览器 UI 手工验证。商品图片地址由接口提供，缺失或加载失败时使用本地占位图。

### React 重构（2026-11-12）

- Vue 组件迁移为 React TSX，路由使用 React Router，认证、购物车和轻提示使用 Zustand，抽屉使用 Radix Dialog。
- 原有 CSS 样式、图片、HTTP 服务层及 `/auth`、`/goods` 网关代理保留；`mall-token`（sessionStorage）和 `mall-cart-v2`（localStorage）存储格式保持兼容。
- 已执行 `npm install`、`npm run typecheck`、`npm run build`，均通过；未新增或修改测试代码。
- 浏览器手工检查首页渲染、注册空表单校验、购物车空态、Escape 关闭和焦点恢复。当前商品接口不可用，未验证真实商品搜索分页、加入购物车及完整认证流程；本轮未完成手机视口验证。
- 开发端口被占用时 Vite 会自动选择下一个可用端口，以终端输出为准。

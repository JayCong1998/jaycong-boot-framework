# mall-admin

基于已确认原型的电商运营管理后台，独立于 `mall-web`；不是 Maven 模块。

技术栈：Vue 3 + TypeScript + Vite + Vue Router + Pinia + Axios + Element Plus + Element Plus Icons + ECharts。Element Plus 组件及样式按需引入，页面懒加载，图表使用模块化导入。

## 启动

需要 Node.js 20.19+ 或 22.12+（推荐 Node.js 22 LTS）。在本目录执行：

```powershell
npm ci
npm run dev
```

打开 <http://127.0.0.1:5174>。前台商城使用的 5173 不受影响。

默认 `/auth`、`/admin/members`、`/admin/goods` 及 `/admin/goods-categories` 由 Vite 代理到网关 `http://localhost:8090`，保留路径；网关通过 Nacos 发现 Auth、User 和 Goods。先按 [Auth 说明](../docs/auth.md)、[Gateway 说明](../docs/gateway.md)、[商品管理说明](../docs/product-management.md) 启动后端及其 MySQL、Redis、Nacos 依赖。

开发环境手工初始化的管理员账号见根目录 `AGENTS.md`，不会自动创建账号，不在页面预填账号和密码。仅 `role=admin` 可进入管理端，普通商城账号不能进入。

如需直连 Auth，复制 `.env.example` 为 `.env.local`，修改并重启 Vite：

```dotenv
GATEWAY_PROXY_TARGET=http://localhost:8084
VITE_API_BASE_URL=
```

`GATEWAY_PROXY_TARGET` 仅作用于开发/预览服务器，不进入客户端。`VITE_API_BASE_URL` 留空表示同源接口；设置跨域地址时，服务端必须明确允许前端来源和 Authorization 请求头。不要在 VITE 环境变量中放置密钥。

## 路由

| 路径 | 页面 |
| --- | --- |
| `/login` | 手机号或邮箱 + 密码登录 |
| `/dashboard` | 原型工作台，经营数据标注为示例 |
| `/products` | 单商品查询、新增、编辑、详情、上下架、库存调整、逻辑删除及固定分类选择、筛选 |
| `/orders` | 订单管理占位页，接收工作台待办的 status 查询参数 |
| `/members` | 会员查询、详情、昵称编辑、启用/禁用，详见 [会员管理](../docs/members.md) |
| `/marketing` | 营销中心占位页 |
| `/content` | 内容管理占位页 |
| `/analytics` | 数据统计占位页 |
| `/settings` | 来自 Auth 的当前账号信息 |
| 其他路径 | 404 页面 |

根路径跳转工作台。工作台及全部模块使用受保护的布局路由；未登录访问时跳转登录页，登录后只回跳到已注册的内部管理路由，防止外部地址跳转或登录循环。

## 认证行为

| 接口 | 用途 |
| --- | --- |
| `POST /auth/login` | 提交 `{ account, password }`；接收 tokenName、tokenValue、timeout、user |
| `GET /auth/me` | 登录后、页面刷新和受保护路由跳转时校验有效会话与角色 |
| `DELETE /auth/session` | 撤销当前 Token，其余设备不受影响 |

- Axios 使用 `Authorization: Bearer <tokenValue>`，与当前 Auth 配置一致。账号仅去除首尾空格；密码保持原样。
- 仅将 Token 保存在当前标签页的 `sessionStorage`，键为 `mall-admin:token`，与商城前台隔离。不会保存密码、缓存可信角色或提供虚假的自动登录选项。
- 用户昵称、角色和 ID 来自 `/auth/me`，ID 使用字符串以保留 BIGINT 精度。
- 认证请求失败时显示服务端错误消息或可读的网络/超时提示，不自动重试写请求。
- 受保护请求返回 HTTP 401 或 Result code=401 时清除当前会话并返回登录页；登录的错误密码不触发全局跳转。旧 Token 请求的迟到 401 不清除新的 Token。
- 退出前弹出确认，后端退出成功或返回 401 后清除本地会话并替换为登录页。网络异常保留会话并提示重试，不宣称已成功撤销。
- 普通用户在登录校验中被拒绝，并尝试撤销刚创建的会话。
- 若普通用户会话撤销失败，保留待退出状态、禁用登录表单并提供“退出当前会话”重试按钮；不会借用该会话开放后台。
- **前端角色检查只限制页面入口，不能代替后端接口授权。** 商品管理接口由 Goods 服务读取共享会话，并通过 User 服务核验有效用户和管理员角色。

## HTTP 方法约定

查询使用 GET，创建使用 POST，更新使用 PUT，删除使用 DELETE。会员资料与启禁用均通过 PUT 更新，并保留 lockVersion 并发检查；详情及编辑打开时重新 GET 读取最新数据，提供加载、失败重试及详情刷新。退出当前会话使用 DELETE /auth/session。

前端需要与新版 User、Auth 同步部署，旧服务不支持这些 PUT 和 DELETE 接口。Auth 旧 POST /auth/logout 仅保留给已有客户端兼容。

## 构建与部署

```powershell
npm run typecheck
npm run build
npm run preview
```

构建输出在 `dist/`；预览地址 <http://127.0.0.1:4174>，同样支持本地代理配置。

生产环境不能依赖 Vite 开发代理。使用 Nginx 等静态服务器承载 `dist/`，为 History 路由配置 SPA 回退，同时将 `/auth/` 代理到网关，避免登录页刷新 404 或认证路径返回 index.html。例如部署于域名根目录时：

```nginx
server {
    listen 80;
    server_name admin.example.com;
    root /srv/mall-admin/dist;

    location /auth/ {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location /admin/members {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location /admin/goods {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

示例域名与目录需替换为实际配置；正式部署应由入口提供 HTTPS。当前路由按域名根路径部署设计。

## 验证与范围

遵循根目录 AGENTS.md，不新增、编写或修改测试代码。通过 TypeScript 检查、Vite 生产构建和本机浏览器/HTTP 手工联调验证。

当前完成登录、退出、会话校验、后台路由与原型布局，以及会员和商品管理的真实接口接入。商品使用单商品模型，不引入 SKU；图片使用 URL 录入，详情为纯文本，库存通过独立入口调整，不保存库存流水。经营指标、趋势、订单及热销列表是明确标注的展示数据；其他业务管理模块为占位页。密码修改、找回、短信、验证码、记住密码均未开放。

2026-11-12 本机实际验证：

- `npm install` 完成，安装时审计报告 0 vulnerabilities；生成 package-lock.json。
- `npm run typecheck` 和 `npm run build` 通过，图表分包后无大块体积告警。
- 启动已有 Auth/Gateway JAR，通过 Nacos 注册发现与 Vite 同源代理联调；没有修改后端源码、数据库结构或初始账号。
- 在 Edge 浏览器实际检查未登录保护、错误密码、管理员登录回跳、真实用户信息、刷新恢复、普通账号拒绝、退出确认/取消、后端 Token 撤销后 `/auth/me` 返回 401、退出后的历史页面拦截。
- 手工阻断浏览器退出请求，确认管理员退出失败保留 Token；普通用户撤销失败保留会话、禁用登录，恢复网络后可重试退出。
- 通过后端退出接口撤销当前浏览器会话后访问受保护路由，确认返回登录且清除 Token；外部 redirect 地址被拒绝并回到工作台。
- 检查 390px 手机宽度的工作台与账号页、移动导航、图表时段切换；未出现页面级横向溢出，浏览器未记录页面脚本异常。

未验证生产 Nginx/HTTPS 部署、跨域部署或尚未实现的业务接口；不新增或修改测试代码。

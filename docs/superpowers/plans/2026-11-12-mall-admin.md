# Mall Admin 实施计划

**目标：** 按已确认的工作台原型创建独立 Vue 管理端，完成路由与真实 Auth 登录、会话恢复、退出。

**架构：** 新建 `mall-admin/`，不作为 Maven 模块，不修改已有 mall-web 或后端接口。Vue Router 使用 History 模式与懒加载页面；Pinia 保存当前用户；sessionStorage 使用独立键保存当前标签页 Token；Axios 统一发送 Bearer Token、解析 Result 和处理 401。当前用户以 `/auth/me` 为准，后台要求 admin 角色；前端角色检查不能替代后端接口授权。

**技术栈：** Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus、Element Plus Icons、ECharts。

## 实施步骤

- [x] 创建 package.json、TypeScript/Vite 配置、入口、环境变量示例。开发端口 5174，默认代理 `/auth` 到 localhost:8090，可改为直连 8084。
- [x] 创建类型、Axios 客户端、Auth API、会话存储和 Pinia。登录不裁剪密码；失败可读；退出成功或 401 才清除本地会话，网络错误保留会话以便重试。
- [x] 创建登录页、路由守卫、受保护布局、404 和业务占位页。刷新与受保护导航调用 me；重定向仅允许已知内部业务路由；普通用户不能进入后台。
- [x] 创建原型工作台，展示明确标注的示例数据、ECharts 趋势、最新订单及待办导航。侧栏可折叠，适配窄屏；无接口功能不伪装为已实现。
- [x] 运行 npm install、npm run typecheck、npm run build；启动 Vite，启动现有 Auth 和网关并通过浏览器验证登录、刷新、路由回跳、退出与失效会话。遵循 AGENTS.md，不新增或修改测试代码。
- [x] 完善 mall-admin/README.md：启动、接口、代理、生产反向代理、权限边界与验证记录。

## 验收

未登录访问工作台跳登录；正确管理员账号登录并显示真实昵称；错误密码保留在登录页并提示；刷新恢复会话；退出调用后端撤销 Token 且浏览器后退不能进入受保护页面；401 清除本地状态；网络故障不能被误报为成功退出。

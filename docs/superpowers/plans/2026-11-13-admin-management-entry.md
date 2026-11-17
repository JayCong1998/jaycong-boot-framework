# Admin 后台统一入口实施计划

**目标：** Gateway 的全部 /admin/** 请求进入 Admin，再通过类型化 Feign 契约调用 User、Goods；现有前端协议不变。

**设计：** Admin Controller → application → Feign → 业务 Facade → application → domain/infra。内部管理接口使用独立服务凭证，并透传登录 Token 供业务服务再次验证身份及审计；不信任客户端提供的操作人。商城公开查询和认证接口不变。

- [x] Common API 增加会员、流水、商品管理请求/响应与 Facade/Feign 契约；不依赖业务应用。
- [x] User、Goods 将管理 Controller 替换为 /internal/admin/** Facade；内部管理路径校验 ADMIN_SERVICE_TOKEN，空凭证拒绝调用。保留业务权限复核、事务、乐观锁与审计。
- [x] Admin 增加管理 Controller、应用服务、统一管理员校验及远程异常转换；仅指定客户端透传 Authorization 与服务凭证，写调用禁用重试。
- [x] 调整改动涉及的仓储分包，领域服务通过参数接收数据，应用层编排持久化。
- [x] Gateway 的 /admin/** 路由统一转到 admin，文档入口同步；更新使用说明及 Nacos 部署注意事项。
- [x] Maven 对 Admin、User、Goods、Gateway 及依赖执行 package -DskipTests；临时端口启动并手工 HTTP 检查权限、内部入口及真实 RPC 链路，结束后停止本次验证进程。不新增修改测试代码，不执行 SQL 迁移。

- [x] 按后续用户要求，Admin 与 Auth 统一为 controller/service/request/response/config/exception 分包；本地 HTTP 模型与 RPC 模型分离，远程错误在 Service 转换为 AdminException。

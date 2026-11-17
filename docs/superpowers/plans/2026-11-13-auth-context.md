# AuthContext Implementation Plan

**Goal:** 在 jaycong-common-satoken 提供全部通过 StpUtil 读取的当前登录身份，并接入现有应用。

**Architecture:** AuthContext 为可覆盖的注入式接口；默认实现调用 StpUtil，登录快照保存于 Token-Session。公共模块注册 jaycong-auth 登录域，官方 SaBeanInject 将 StpLogic Bean 绑定到 StpUtil。业务保留实时管理授权与会话撤销。

**Tech Stack:** Java 21、Spring Boot 3.5、Sa-Token 1.46、Redis、Maven。

**执行约束:** 当前工作区包含本功能依赖的未提交模块，在原工作区增量修改，不迁移或覆盖其他改动。不新增、修改测试代码，不自动提交。按当前会话执行，不创建子代理。

## 1. 公共身份能力

- [x] 在 common-satoken 新增 context/AuthContext、context/SaTokenAuthContext、model/LoginUser、session/AuthSessionKeys。
- [x] requireUserId 委托 StpUtil.getLoginIdAsLong；isLogin 委托 StpUtil.isLogin；LoginUser 实现 SaJsonType 允许默认 JSON 白名单还原；requireUser 使用 StpUtil.getTokenSession().getModel 读取并检查用户 ID、昵称和角色。
- [x] 匿名读取返回空；缺少请求上下文仅映射为无身份；快照失效使用 NotLoginException；Redis 读取故障使用 AuthContextUnavailableException。
- [x] 新增自动配置和 imports，注册可覆盖的 StpLogic、AuthContext，以及 MVC 异常映射；common-satoken 依赖 common-core 复用 Result。

## 2. 登录与身份接入

- [x] AuthService 登录使用 StpUtil，保留复查，写 LoginUser 后返回 Token；清理失败附加 suppressed exception，不覆盖原始错误。
- [x] 移除 Auth、Admin、User 的重复会话配置，移除 Goods、Order 配置中的会话 Bean。
- [x] Auth、Admin、User、Goods、Order 的身份读取使用 AuthContext；会话撤销继续保留。
- [x] User、Goods 审计适配使用 AuthContext；领域实体不增加框架依赖。

## 3. 文档与验证

- [x] 更新 docs/common.md 与 README 模块说明，新增 docs/satoken.md，记录接口、会话键、loginType、旧 Token 与资料新鲜度。
- [x] 执行 Maven：mvn -B -ntp -pl jaycong-auth,jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-business/jaycong-order -am package -DskipTests。
- [x] 条件允许时在临时端口启动更新后的 Auth，使用现有 User/Redis/Nacos 手工验证登录、当前用户、退出，不重启现有服务、不输出 Token。
- [x] 检查本次修改列表、会话绑定与 JSON 快照读取，不新增 src/test 文件；记录实际验证结果。

## 实际验证结果

- Maven 最终构建成功，17 个 reactor 项目全部 SUCCESS；使用 -DskipTests，未新增或修改测试代码。
- Auth 新版 JAR 在 18084 临时端口启动成功，通过 spring.config.name 使用独立的命令行配置，关闭 Nacos 注册及配置加载，User 客户端使用 spring.cloud.openfeign.client.config.authUser.url=http://localhost:8087。
- Redis 故障验证使用另一个 18085 临时实例，仅把该实例的 Redis 端口指向未提供服务的本机端口；未停止或修改本机 Redis。

| 手工操作 | 实际结果 |
| --- | --- |
| 无 Token 访问 /auth/me | HTTP 401 |
| 开发普通账号分别登录两个设备 | 两次 HTTP 200 |
| 后续独立请求读取 Redis 中 LoginUser，再查询最新资料 | HTTP 200 |
| 新 Token 访问现有 8084 Auth | HTTP 200，确认登录域共享 |
| 只提交 X-User-Id/X-User-Role | HTTP 401 |
| 删除第一个 Token 会话 | HTTP 200 |
| 已退出 Token 再次访问 | HTTP 401 |
| 第二个设备继续访问 | HTTP 200 |
| 旧版 Auth 签发无快照 Token，访问新版 /auth/me | HTTP 401 |
| 无快照旧 Token 在新版读取 ID 并退出 | HTTP 200 |
| Redis 不可用时使用 Token 访问 /auth/me | HTTP 503，Result.code=503 |

已清理本次创建的会话及临时 Auth 进程，原有服务保留运行。验证使用现有开发普通账号，登录流程会按既有行为更新其最后登录时间。

源码及产物检查确认：公共自动配置和身份模型已打包；旧的 AuthSessionConfiguration 未残留在新版 Auth JAR；Servlet 应用只在公共模块创建默认 StpLogic，Gateway 继续使用原有 Core 配置。

限制：未启动新版 Admin、User、Goods、Order 验证管理操作、数据库审计及下单全流程；未手工验证自定义 Bean 覆盖或用户禁用并发窗口。本次不修改角色授权规则。

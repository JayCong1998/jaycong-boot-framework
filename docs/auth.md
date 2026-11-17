# Auth 服务

Auth 是独立 Servlet 认证接口服务，默认端口 8084。Auth 通过 OpenFeign 远程调用 User 的注册、凭证校验和用户查询能力，使用 Redis 管理 Sa-Token 会话；不再访问 MySQL，也不处理密码哈希。两端共享 common-api 中的 User Facade 契约，不依赖对方应用 JAR。用户领域及数据库配置见 [User 使用说明](user.md)。

## 构建与启动

```powershell
mvn -B -ntp -pl jaycong-auth -am package -DskipTests
java -jar jaycong-auth/target/jaycong-auth-0.1.0-SNAPSHOT.jar
```

启动前先启动 User；当前内部接口不校验服务调用凭证，无需配置 USER_SERVICE_TOKEN。默认通过 Nacos 的 user 服务名寻址，Nacos namespace/group 必须一致；本地可用 USER_SERVICE_URL=http://localhost:8087 指定地址。Feign 连接超时 2 秒、读取超时 5 秒，不自动重试注册或登录；User 不可用时返回 503。通过 common-rpc 使用 HC5 客户端保留 User 的 401 错误响应体，避免 JDK 默认客户端丢失响应体后误报服务故障。

Redis 默认 localhost:6379、无密码、database 0，支持 AUTH_REDIS_HOST、AUTH_REDIS_PORT、AUTH_REDIS_PASSWORD、AUTH_REDIS_DATABASE；User 必须使用相同配置。应用端口使用 AUTH_PORT 覆盖。原 AUTH_DB_* 配置已移除，数据库由 User 的 USER_DB_* 配置管理。

## 接口

所有响应使用 Result，成功 code=0。失败时参数错误 HTTP 400、凭证无效或会话失效 401、账号重复或并发版本变化 409、User 服务不可用 503。

| 方法 | 路径 | 请求 / 行为 |
| --- | --- | --- |
| POST | /auth/register | mobile、email 至少一项，password、nickname 必填；返回用户信息，注册后需单独登录 |
| POST | /auth/login | account 为手机号或邮箱，password 为密码；返回 tokenName、tokenValue、timeout（秒）、user |
| DELETE | /auth/session | 退出当前 Token，其他设备会话保留 |
| GET | /auth/me | 返回当前有效用户，不返回密码哈希和审计信息 |
| GET | /auth/status | 仅验证服务可达，不证明已认证 |
| GET | /actuator/health | Auth 本地 Redis 健康状态，不包含远程 User，默认隐藏详情 |

```powershell
$base = 'http://localhost:8084'
$body = @{email='demo@example.com';password='Example@2026!';nickname='示例用户'} | ConvertTo-Json
Invoke-RestMethod "$base/auth/register" -Method Post -ContentType 'application/json' -Body $body
$body = @{account='demo@example.com';password='Example@2026!'} | ConvertTo-Json
$login = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType 'application/json' -Body $body
$headers = @{Authorization='Bearer '+$login.data.tokenValue}
Invoke-RestMethod "$base/auth/me" -Headers $headers
Invoke-RestMethod "$base/auth/session" -Method Delete -Headers $headers
```

## 规则与边界

- 邮箱去除首尾空白并统一小写，手机号限定中国大陆 11 位号码，移除空格和连字符，兼容 +86、0086 前缀。空白标识转为 NULL。数据库唯一索引最终处理并发重复，逻辑删除后仍占用标识。
- 注册角色固定 user，不接受客户端指定角色。密码不裁剪，要求 8–64 字符且 UTF-8 不超过 72 字节，使用 BCrypt（cost=12）保存。ID 由 MyBatis-Plus 生成，以字符串返回避免前端精度丢失。
- 注册和登录时间更新使用 MyBatis-Plus 审计自动填充；逻辑删除和乐观锁已启用。登录更新时间使用版本和启用状态条件，冲突时重试登录。User 服务中的数据库实体 `User` 继承 common-core 的 `BaseEntity`，统一维护主键及审计控制字段。
- 会话有效期 24 小时，使用 Authorization: Bearer 请求头，不读取 Cookie 或请求体 Token。Redis 会话 loginType 为 jaycong-auth，键空间为 Authorization:jaycong-auth:*，重启 Auth 后保留。Redis 重启后的数据持久性取决于 Redis 自身配置。
- 登录复查后将 `LoginUser(userId, nickname, role)` 写入当前 Token-Session；`AuthContext` 的数据全部从 StpUtil 获取。/auth/me 先读取登录快照再远程查询 User；发现禁用或逻辑删除后撤销该用户全部会话。会员禁用由 User 提供并同步撤销会话，暂不提供逻辑删除接口。旧 Token 未保存快照时访问 /auth/me 需重新登录；其他仅读取 loginId 的业务可继续使用旧 Token。详细契约见 [Sa-Token 接入说明](satoken.md)。
- 尚未实现短信/邮箱验证码或所有权验证、密码找回、修改密码、登录限流及网关统一鉴权。用户角色只作为当前用户信息返回，网关转发不代表其他业务接口已受保护。
- 初始账号遵循 AGENTS.md，首次登录应修改密码；本次不自动创建或修改已有账号。

## 历史验证（拆分前）

执行 Maven package -DskipTests 成功；使用临时端口 18084 手工验证邮箱和手机号注册登录、邮箱大小写及手机号前缀归一化、重复注册 409、错误密码和未登录 401、参数/邮箱/BCrypt 字节边界 400、退出后 Token 失效，以及重启后 Redis Token 仍可用。未新增或修改测试代码。

## Nacos 接入

Auth 已依赖 common-config，默认注册为 `auth`，加载 `auth.yml`。启动前除 User、Redis 外还需准备 Nacos 和目标 Data ID；连接参数及配置发布说明见 [Nacos 接入说明](config.md)。

## 分包与调用边界

- controller：/auth HTTP 入口，接收并校验请求，调用 Service。
- service.AuthService：注册委托、登录 Token 签发/复查及快照保存、当前用户查询和退出会话编排；通过 AuthContext 读取身份，通过 StpUtil 操作会话生命周期。
- service.UserService：委托 UserFeignClient 调用用户能力、校验远程用户 ID、转换远程错误；仅转发已知业务 400/401/409，其他下游故障统一返回 503。
- exception：认证异常和统一认证异常响应，包括未登录异常。
- request / response：认证 HTTP 请求和返回模型；不再为 Controller 和 Service 重复定义同形请求。
- 远程调用：直接注入 common-api 的 user.client.UserFeignClient；Auth 不再声明本地 Client。
- 会话配置统一由 common-satoken 自动配置提供，Auth 不再声明重复的 StpLogic。User 客户端专用配置位于 common-api 的 user.config 包。

Auth 使用普通 Controller / Service 分包，不建立 domain、application、port 或 infrastructure 分层，也不为远程用户能力增加空转 Gateway 接口。用户领域规则保留在 User 服务。

用户模型、仓储、Mapper、数据库实体、账号规范化、密码编码器和会员管理已移至 User。登录及注册请求不会在 toString() 或 Feign 请求日志中输出密码。

旧版本验证记录仅描述拆分前结果。本次拆分验证情况见 [User/Auth Facade 计划](superpowers/plans/2026-11-12-user-auth-facade.md)。

## API 远程调用配置

启动类通过 @EnableFeignClients(clients = UserFeignClient.class) 显式注册 API 客户端。UserService 直接使用 API 客户端，common-rpc 提供 OpenFeign Starter、LoadBalancer 和 HC5。配置使用 jaycong.rpc.user.url / token，继续支持 USER_SERVICE_URL / USER_SERVICE_TOKEN 和旧 jaycong.auth.user-service-* 回退。Feign 超时仍使用 spring.cloud.openfeign.client.config.authUser 配置，详见 [RPC 接入说明](rpc.md)。

管理端退出使用 `DELETE /auth/session` 删除当前 Token 会话；旧 `POST /auth/logout` 保留兼容现有商城客户端，行为一致。

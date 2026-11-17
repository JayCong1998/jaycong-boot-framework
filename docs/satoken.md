# Sa-Token 与 AuthContext

`jaycong-common-satoken` 为 Servlet 应用提供 Redis 会话、共享登录域和统一认证鉴权入口。`AuthContext` 的会话读取来自 `StpUtil`，角色鉴权通过 User RPC 获取实时有效账号，不保存额外的 ThreadLocal 或请求身份缓存，也不依赖业务模块实现。

## 接入

业务导入 BOM 后添加：

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-common-satoken</artifactId>
</dependency>
```

自动配置通过 `AutoConfiguration.imports` 注册，只有 Servlet 应用生效：

- 默认 `StpLogic` 使用 `jaycong-auth`，保留既有 Redis 键空间 `Authorization:jaycong-auth:*`。
- 官方 Starter 的 `SaBeanInject` 将 StpLogic Bean 绑定到 StpUtil，两种访问方式使用同一实例。
- 应用声明自己的 `StpLogic` 或 `AuthContext` Bean 时，对应默认 Bean 退让。自定义登录域后，各参与服务必须保持一致；只为同一账号体系提供一个候选 StpLogic。
- 需要实时角色鉴权的应用显式启用 Common API 的 `UserFeignClient`，并配置 `jaycong.rpc.user.token`、`authUser` 客户端超时及 User 服务发现或 URL；默认 AuthContext 只在鉴权时获取客户端。仅使用会话读取的应用无需启用该客户端，调用角色鉴权时缺少客户端返回 503。
- MVC 响应统一由 `jaycong-common-web` 的 `GlobalExceptionHandler` 处理，应用需引入 common-web；未登录映射为 401、上下文读取时的会话存储故障映射为 503。common-satoken 不再注册独立异常处理器。
- `sa-token.*`、`spring.data.redis.*` 使用原生配置。跨服务共享身份时，Redis 地址、database、Token 名称和 loginType 必须一致。

WebFlux Gateway 继续使用当前 Core 校验和 Token 转发方式，不依赖此 Servlet 模块。

## 读取身份

业务通过构造器注入 `io.github.jaycong.satoken.context.AuthContext`：

```java
private final AuthContext authContext;

public OrderController(AuthContext authContext, OrderManageService orders) {
    this.authContext = authContext;
    this.orders = orders;
}
```

| 方法 | 行为 |
| --- | --- |
| `isLogin()` | 委托 StpUtil.isLogin，只判断登录会话有效，不保证资料快照存在 |
| `requireUserId()` | 委托 StpUtil.getLoginIdAsLong，未登录为 401，兼容没有快照的旧 Token |
| `requireUser()` | 验证登录并读取 Token-Session 的 LoginUser，资料缺失、损坏或 ID 不一致为 401 |
| `currentUser()` | 无有效登录返回 Optional.empty；已登录但资料失效仍为 401 |
| `requireRole(role)` | 先确认会话，再查询 User 中的有效账号和实时角色；返回当前用户 ID，无权限为 403，核验不可用为 503 |
| `requireAdmin()` | 等同 `requireRole("admin")`；不赋予隐式权限绕过 |

`LoginUser` 是不可变 record，包含 `long userId`、`String nickname`、`String role`。它是服务内部快照；HTTP 响应继续使用各应用的响应模型，将 BIGINT ID 输出为字符串。

受保护操作使用 `requireUserId()` 或 `requireUser()`，不能将身份读取异常降级为系统操作人 0。无请求上下文时 `isLogin()` 为 false、`currentUser()` 为空，要求身份的方法抛出未登录异常。异步、MQ、定时任务显式传入操作人；历史操作人不能作为当前授权证明。

应用或入口读取身份后，将用户 ID 传入业务行为；纯领域实体不依赖 AuthContext。Goods 管理服务直接注入 AuthContext，并在各管理方法调用 `authContext.requireAdmin()`，不再单独维护 SessionGoodsAdmin、角色判断、远程身份异常解析或会话撤销逻辑。

## 登录快照

Auth 完成凭证认证及原有登录后复查后保存快照：

```java
StpUtil.login(userId);
// 由认证用例完成账号复查后，再保存资料并向客户端返回 Token。
StpUtil.getTokenSession().set(AuthSessionKeys.LOGIN_USER,
        new LoginUser(userId, nickname, role));
```

统一键为 `jaycong.login-user.v1`，存放在当前 Token-Session；各设备分别保存快照。LoginUser 实现 Sa-Token 的 `SaJsonType` 标记，允许默认 JSON 类型白名单还原该 record，再通过 `getModel` 读取。所有共享会话的服务都应使用包含该模型的兼容版本；自定义 DAO 或序列化器也必须还原为 LoginUser，类型不匹配时按资料失效处理。

Auth 已接入该流程：复查或快照写入失败时尝试撤销本次 Token；清理失败作为 suppressed exception 保留，不覆盖原始错误。数据库状态与 Redis 会话非原子事务，仍存在并发禁用窗口。

升级前签发的旧 Token 仍能通过 `requireUserId()` 获取有效 ID，但没有 LoginUser，调用 `requireUser()` 或 `/auth/me` 时需要重新登录。

## 新鲜度、授权与会话撤销

- 昵称和角色是登录时快照，不自动跟随用户表变化；`/auth/me` 继续远程获取最新资料。
- 管理操作不能仅凭快照中的 role 授权。Goods 统一调用 AuthContext 的实时鉴权；Admin、User 原有入口仍保留各自的实时校验调用，本次不调整其业务流程。
- `requireRole` 遇到 User 返回 HTTP 401 且响应体 `code=401` 时，撤销该账号的会话并返回 401；服务不可用、响应缺失、ID 不一致或角色为空返回 503，不回退使用登录快照。撤销会话的 Redis 操作失败同样返回 503。
- User RPC 的 HTTP 403、非标准 401 或其他技术失败按核验不可用处理，不误判为账号失效，也不撤销有效会话。
- 当前 Token 退出使用 `StpUtil.logoutByTokenValue`；禁用用户使用既有全账号会话撤销流程，其他设备的退出语义保持不变。
- RPC 继续显式透传经过验证的 Bearer Token，下游重新校验，不信任客户端提交的用户 ID 或角色头。
- 审计默认系统操作人仍为 0，User、Goods 的应用级审计适配从 AuthContext 获取实际操作人；common-datasource 不反向依赖身份模块。

## 构建

```powershell
mvn -B -ntp -pl jaycong-auth,jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-business/jaycong-order -am package -DskipTests
```

遵循项目约定，不新增或修改测试代码。运行验证应覆盖登录、快照读取、旧 Token、退出、管理授权和会话存储故障；本次实际执行的干净构建、Goods/Order 启动及 HTTP 验证结果见[实施记录](superpowers/plans/2026-11-16-goods-ddd-refactor.md#后续统一-authcontext-鉴权)，其中明确记录尚未联调的实时角色授权和会话撤销流程。

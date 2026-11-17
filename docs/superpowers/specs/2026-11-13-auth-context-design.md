# AuthContext 设计建议

日期：2026-11-13。状态：已按确认方案实施；当前接入方式见 [Sa-Token 接入说明](../../satoken.md)。

## 目标与现状

提供统一的当前登录身份访问入口，减少业务代码直接使用 Sa-Token 的情况。登录会话由 Sa-Token 和 Redis 保存；AuthContext 的全部身份数据统一通过 StpUtil 获取，不访问数据库、User RPC 或独立身份缓存，也不承担登录签发职责。

用户已明确的数据来源约束：AuthContext 的数据都从 StpUtil 中获取。本约束确定读取入口；用户资料的新鲜度和管理授权策略仍按下文分别约定。

当前实际代码已经拆分为 Auth 签发会话、User 管理用户数据：

- AuthService 使用自定义 StpLogic("jaycong-auth") 签发 Token，登录后再次调用 User 核验账号，再返回 Token；目前尚未保存用户资料快照。
- User 的 CurrentUserService 从会话读取 ID，再查询本地仓储确认有效状态。
- AdminAccessService、Goods 的 SessionGoodsAdmin 查询 User，实时核验状态和角色。
- OrderController 直接使用 StpLogic 获取下单用户 ID。
- UserAuditConfiguration 直接从 StpLogic 获取操作人，其他默认审计填充使用系统操作人 0。
- Gateway 使用 Sa-Token Core 和 Redis 校验 Token；它是 WebFlux 应用，不依赖 Servlet Starter。

## 方案与数据来源约定

| 方案 | 优点 | 代价 |
| --- | --- | --- |
| 登录身份快照，管理操作继续实时校验（建议第一版） | 普通身份读取无需访问 User，保留当前管理权限语义，迁移范围明确 | 角色快照不能直接用于管理授权，管理请求仍有查询成本 |
| 所有请求都从 User 读取最新用户 | 状态、角色、资料都以实时数据为准 | 普通业务也依赖 User 可用性和查询延迟；容易扩大基础组件职责 |
| 所有身份和角色都以登录快照为准 | 请求路径简单，降低 User 调用量 | 角色变更必须撤销全部会话；禁用提交与 Redis 撤销之间存在一致性窗口 |

本草案采用第一种方案，且 AuthContext 统一调用 StpUtil。既有管理操作的实时校验保留在对应业务用例或身份适配器中，不移入 AuthContext。若后续选择第三种，需明确变更现有实时授权行为，补齐角色变更撤销、撤销失败处理和并发边界，再设计基于快照的 requireRole。

## 模块和依赖

直接扩展现有 jaycong-common-satoken，不重新创建 jaycong-context 模块，也不把 Sa-Token 放进 common-core。

建议包结构：

```text
io.github.jaycong.satoken
├── context
│   ├── AuthContext
│   └── SaTokenAuthContext
├── model
│   └── LoginUser
├── session
│   └── AuthSessionKeys
└── autoconfigure
    └── JaycongSaTokenAutoConfiguration
```

AuthContext 是注入式接口，SaTokenAuthContext 是其默认实现，实现内部直接调用 StpUtil。AuthSessionKeys 仅统一登录用户的会话键；Auth 在登录时通过 StpUtil 写入快照，AuthContext 通过 StpUtil 读取。不再为快照读写增加一层 SessionStore。

自动配置使用 AutoConfiguration.imports 注册，针对 Servlet 环境生效。StpUtil 必须绑定应用实际使用的 StpLogic，loginType 继续统一为 jaycong-auth。官方 Starter 的 SaBeanInject 在启动时通过 StpUtil.setStpLogic 绑定该 Bean；不能在请求处理中切换全局 StpLogic。注入式 StpLogic 和 StpUtil.getStpLogic() 使用同一实例。

相关默认 Bean 使用 ConditionalOnMissingBean，允许应用覆盖，但覆盖后的 StpLogic 仍须按上述规则绑定。迁移时逐个收敛重复配置，保留现有 Redis loginType 键空间，不直接切换到默认 login。

common-satoken 不依赖 Auth、User 应用或 UserFeignClient。Gateway 保持现有 Core 校验和 Token 转发方式，不引入此 Servlet 模块。

## 对外契约

以下是接口建议，不是已实现代码：

```java
public record LoginUser(long userId, String nickname, String role) {
}

public interface AuthContext {
    Optional<LoginUser> currentUser();
    LoginUser requireUser();
    long requireUserId();
    boolean isLogin();
}
```

- currentUser：先调用 StpUtil.isLogin()，未携带 Token 或已知失效 Token 返回 Optional.empty；已登录时从 StpUtil.getTokenSession() 读取 LoginUser，并核对其 userId 与 StpUtil.getLoginIdAsLong() 一致。缺失或损坏快照按身份资料失效处理，Redis 故障继续抛出。
- requireUser：要求有效登录且会话中存在合法 LoginUser；缺少或失效时返回 401。
- requireUserId：直接调用 StpUtil.getLoginIdAsLong()，仅要求有效登录，不要求存在资料快照。不能从客户端 userId 参数或身份声明头取值。
- isLogin：直接调用 StpUtil.isLogin()，只表示会话登录有效；不承诺资料快照存在或拥有某资源的权限。

这些方法职责不同：旧 Token 可以仍然通过 isLogin/requireUserId，但获取完整 LoginUser 时会因缺少快照要求重新登录。

LoginUser 仅保存身份和基础展示信息。userId 在 Java 内使用 long，对外 JSON 仍按项目约定输出字符串；不直接把内部模型作为 HTTP 响应。role 当前是单个 admin/user 值，记录登录时快照。暂不加入租户、组织、权限集合、密码哈希、手机号、邮箱、Token 或用户数据库实体。

首版不提供 setCurrentUser、clear、login、logout、requireRole 等公共上下文方法。会话生命周期由认证用例控制，资源归属与管理授权由业务负责。

## 登录与请求流程

```text
登录：凭证 -> Auth -> User 校验及登录更新 -> StpUtil.login(userId)
     -> User 再次核验 -> StpUtil.getTokenSession().set(用户快照键, LoginUser)
     -> 返回 Token

请求：Authorization: Bearer Token -> AuthContext -> StpUtil
     -> 校验当前会话、读取 loginId 或 Token-Session 中的 LoginUser
     -> 业务用例
```

选择 Token-Session 保存快照，适配当前 is-share=false 和按 Token 退出的行为；同一用户多个设备分别持有各自的登录快照。使用固定的版本化键，例如 jaycong.login-user.v1。快照随对应会话生命周期维护，不再手工建立另一套用户 Token 缓存。

读取前必须验证当前 Token 的有效性，不得仅凭一个仍存在的 Session 对象认定已登录。快照 userId 必须与会话 loginId 一致；缺失、损坏或不一致时拒绝作为有效身份。

保留 Auth 当前的登录后复查：复查成功后才保存快照并返回 Token。复查或写入失败时不返回成功，尝试撤销本次 Token；撤销也失败时记录不含凭证的错误，由会话过期和运维处理残留。数据库状态与 Redis 写入并非原子事务，不能宣称完全消除并发禁用窗口。

AuthContext 默认实现是无用户状态的单例 Bean，每次访问均委托 StpUtil。上下文适配、会话读取及框架内部缓存由 Sa-Token 负责；不再新增 request attribute 身份缓存、自建 ThreadLocal 或单例用户字段，避免多套身份状态需要同步。

旧 Token 没有快照，按前述契约仍可获取有效 loginId，但完整用户读取要求重新登录，不在每个服务加入远程补全逻辑。上线前明确这一兼容性影响；共享快照模型和序列化格式需在 Auth、User、Admin、Goods、Order 之间保持兼容。LoginUser 实现官方 SaJsonType 标记，以允许 Sa-Token 1.46 JSON 类型白名单还原该 record；自定义序列化器也必须还原为兼容的 LoginUser。

## 用户资料和授权新鲜度

- /auth/me 保留查询 User 的行为，返回最新有效用户资料；它与登录身份快照有不同的新鲜度约定。
- AuthContext 的 nickname、role 是快照，不自动跟随数据库变化。第一版不新增跨设备资料快照刷新流程。
- Admin、Goods 和 User 的管理操作保留实时有效状态及角色校验，userId 的来源改用 AuthContext 即可。实时校验失败必须拒绝操作，不回退到快照授权。
- 用户禁用、逻辑删除继续由 User 撤销全部会话；Redis 撤销失败需报告和允许重试，不能当作已经完成。
- 将来新增角色变更时，建议在修改后撤销全部会话，要求重新登录；若希望仅凭会话角色授权，应单独确认并改变当前实时校验规则。
- 退出仅撤销当前 Token；其他设备继续登录。所有后续身份读取交由 StpUtil 按会话状态判断；撤销不能终止已经执行中的请求。

## 各模块接入边界

| 模块 | 建议调整 |
| --- | --- |
| Auth | 登录复查后写入快照；/auth/me 从上下文读取完整快照中的 ID 后查询 User；登录、退出仍操作会话生命周期 |
| User | CurrentUserService 从上下文取 ID，保留仓储状态校验；禁用、删除等用例保留会话撤销 |
| Admin | 从上下文取 ID，保留实时管理员校验及现有内部服务调用凭证检查 |
| Goods | 保留 CurrentGoodsAdmin 领域端口；infra 的 SessionGoodsAdmin 注入 AuthContext，继续远程核验，领域层不依赖 Sa-Token |
| Order | HTTP 入口从上下文获取下单人，放入 CreateOrderCommand；应用和领域继续通过显式参数接收身份 |
| 审计 | 先在应用现有 MetaObjectHandler 适配中读取上下文，common-datasource 不反向依赖 common-satoken |
| Gateway | 校验并转发原始 Bearer Token；业务服务独立校验会话，不信任客户端声明的用户和角色头 |

普通领域实体或纯领域规则不读取 AuthContext。需要身份的用例在入口或应用编排处解析后传入；Goods 按已有例外通过领域身份端口访问基础设施适配。

## RPC、异步与审计

AuthContext 只表示当前服务、当前请求的身份，不会自动跨服务传播。保留当前管理 Feign 客户端只透传已验证 Authorization 的规则和服务凭证；下游重新验证 Token。不要把身份字段拼成可信头，也不要让公共 Feign 拦截器向所有远程主机自动透传 Token。

普通 Servlet 适配下，无 HTTP 请求时不允许隐式读取登录身份；实现需将 Sa-Token 缺少请求上下文的特定情况映射为 currentUser 返回空、isLogin 返回 false，以及 requireUser/requireUserId 的无身份错误，不捕获其他异常后伪装为匿名。异步任务、MQ、定时任务用显式参数携带必要的操作人信息；历史消息中的操作人用于审计，不作为执行时仍有权限的证明。

公开注册或明确的系统任务可以使用审计操作人 0。受保护业务操作必须先要求有效身份，不能因为上下文读取失败就降级为系统身份。Redis 故障不能被兜底为匿名或系统用户。

## 异常和验证

- 缺少或失效的登录身份：401；无管理权限：403。
- Redis 不可用或需要实时校验时 User 不可用：503，拒绝继续受保护操作。
- 不向日志输出 Token、密码或完整会话内容。
- common-satoken 提供明确的异常语义，由各服务现有异常处理器映射 Result 和 HTTP 状态，不让 common-web 反向依赖 Sa-Token。

后续实现遵守 AGENTS.md，不新增或修改测试代码。编译验证可使用：

```powershell
mvn -B -ntp -pl jaycong-auth,jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-business/jaycong-order -am package -DskipTests
```

服务条件具备时手工验证：StpUtil 与注入式 StpLogic 使用同一实例且 loginType 为 jaycong-auth、登录后各服务读取同一身份、未登录和过期 401、普通用户管理操作 403、多设备单 Token 退出、禁用全部会话撤销、资料快照与 /auth/me 新鲜度、伪造身份头无效、旧 Token 可取 loginId 但完整用户读取要求重新登录、Redis/User 不可用 503，以及请求结束后的用户身份隔离。

已实现 common-satoken 身份能力及 Auth、Admin、User、Goods、Order 接入，并执行上述 Maven 构建。使用临时 Auth 实例手工验证了登录、完整快照读取、共享会话、双设备退出、伪造身份头、旧 Token 和 Redis 不可用响应。未新增或修改测试代码；未启动新版 Admin、User、Goods、Order 验证各自业务流程。详细结果见 [实施计划](../plans/2026-11-13-auth-context.md)。

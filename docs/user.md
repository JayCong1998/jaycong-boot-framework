# User 用户业务服务

模块位于 jaycong-business/jaycong-user，继承 Business 聚合父 POM，根 POM 通过 Business 间接聚合。User 是独立应用，不作为外部依赖加入 BOM。默认端口 8087，启动类 io.github.jaycong.user.UserApplication，Nacos 服务名 user，配置 Data ID 为 user.yml。

## 职责与依赖

User 持有用户领域能力、MySQL 持久化、BCrypt 密码处理和会员管理，不再暴露 /auth 认证 HTTP 接口，也不签发 Token。

严格参照 `NFTurbo_Server/nft-turbo-business/nft-turbo-order`，入口直接调用领域读写服务，领域服务直接使用 Mapper：

```text
io.github.jaycong.user
├── UserApplication
├── UserException
├── facade
│   ├── UserFacadeServiceImpl
│   ├── MemberManagementFacadeServiceImpl
│   └── MemberOperationManagementFacadeServiceImpl
├── domain
│   ├── entity
│   │   ├── User、MemberOperation、AccountNormalizer
│   │   └── convertor/UserConvertor、MemberOperationConvertor
│   └── service
│       ├── UserReadService、UserManageService
│       └── MemberOperationReadService、MemberOperationManageService
└── infrastructure
    ├── mapper/UserMapper、MemberOperationMapper
    └── config/UserPasswordConfiguration、AdminRpcConfiguration、UserAuditConfiguration
```

| 包 | 具体职责 |
| --- | --- |
| facade | 实现 Common API 内部 HTTP RPC 契约，直接调用读写服务并转换响应 |
| domain.entity | 账号规范化、普通用户注册、会员资料及状态变更、版本核验、操作流水阶段规则和表映射 |
| domain.entity.convertor | 认证资料、脱敏会员资料和流水摘要转换，不输出密码哈希或实体 |
| domain.service | 用户和流水读取、认证及管理授权、直接持久化、数据库事务与会话撤销编排 |
| infrastructure.mapper | MyBatis-Plus 用户表和会员操作流水表访问，直接映射领域实体 |
| infrastructure.config | BCrypt 编码器、内部管理调用凭证校验、审计操作人填充 |

User 和 MemberOperation 继承 BaseEntity，分别直接映射 sys_user 和 member_operation_log；不再维护 UserAccount、MemberOperationEntity 或对象复制仓储。已有 Common API 请求直接复用，不设置 application、repository、port 或重复 DTO。会员对外 HTTP 入口保留在 Admin，User 当前只有内部 facade，不创建空 controller、job 或 listener。

UserReadService 通过 AuthContext.requireUserId() 读取共享登录身份，并在本服务用户表确认账号有效及实时 admin 角色；失效账号撤销共享会话，不经 RPC 回调自身，也不使用登录快照的角色授权。UserManageService 维护 BCrypt 校验和写入用例，实体维护注册、昵称、状态和版本规则。

MemberOperationManageService 使用 REQUIRES_NEW 分别保存开始记录、执行会员数据库变更与阶段流水、记录最终结果。禁用先提交数据库再撤销 Redis 会话，启用前先撤销旧会话；失败时重读持久化流水，不把已提交但会话撤销失败的操作误记为数据库失败。

依赖方向为 Auth → common-api 契约 / common-rpc → User Facade；Auth 和 User 不依赖彼此的应用 JAR。Auth 不再引入 Datasource Starter、访问用户表或配置密码编码器。common-core 的 MyBatis 注解与 Spring Cloud 传递的 crypto 依赖仍随公共能力存在，不承载 Auth 的用户持久化或 BCrypt 逻辑。

## Facade 契约

模块内结构：

~~~text
io.github.jaycong.api.user
├── facade
│   └── UserFacade.java
├── request
│   ├── UserAuthenticateRequest.java
│   └── UserRegisterRequest.java
└── response
    └── UserInfo.java
~~~

Auth 与 User 均依赖该 API 模块。Facade 实现保留在 User，Feign 客户端和专用配置统一保留在 common-api，Auth 显式启用并直接注入客户端；Auth 使用普通 Controller / Service 分包，User 保留业务分层。各自 HTTP 请求/响应不混入远程契约。

契约统一位于 jaycong-common-api 模块：接口在 io.github.jaycong.api.user.facade，入参在 io.github.jaycong.api.user.request，返回模型在 io.github.jaycong.api.user.response。成功返回 UserInfo，失败返回标准错误响应及对应 HTTP 状态，不传输密码哈希或持久化实体。

| 方法 | 内部路径 | 能力 |
| --- | --- | --- |
| POST | /internal/users/register | 注册普通用户，不签发 Token |
| POST | /internal/users/authenticate | 校验手机号/邮箱和密码，乐观锁更新最后登录时间 |
| GET | /internal/users/{id} | 获取仍有效的用户，供 Auth 当前用户查询和登录复查 |

以上原认证内部接口本次未改变，不校验 USER_SERVICE_TOKEN。新增会员和流水管理 Facade 位于 `/internal/admin/members/**`，必须同时校验 ADMIN_SERVICE_TOKEN、共享会话及当前有效管理员角色；网关不暴露任何内部路径。

## 配置与启动

| 环境变量 | 用途 |
| --- | --- |
| USER_PORT | 默认 8087 |
| USER_DB_URL | 默认连接 localhost:3306/jaycong-framework |
| USER_DB_USERNAME、USER_DB_PASSWORD | 数据库账号，开发默认值沿用现有配置 |
| AUTH_REDIS_HOST、AUTH_REDIS_PORT、AUTH_REDIS_PASSWORD、AUTH_REDIS_DATABASE | 与 Auth 共用 Redis，会话必须指向相同实例及 database |
| USER_SERVICE_TOKEN | 原认证调用方兼容配置，不替代管理调用凭证 |
| ADMIN_SERVICE_TOKEN | Admin、User、Goods 必须配置一致的内部管理凭证，空值拒绝管理 RPC |
| NACOS_* | 公共注册发现与配置中心连接参数 |

MySQL 仍使用现有 sys_user 表，不自动建表或初始化账号。Nacos 启动前准备 user.yml，原 Auth 的数据库配置应移至 user.yml，使用 USER_DB_* 管理。

~~~powershell
mvn -B -ntp -pl :jaycong-auth,:jaycong-user,:jaycong-gateway -am package -DskipTests
java -jar jaycong-business/jaycong-user/target/jaycong-user-0.1.0-SNAPSHOT.jar
# 另一终端启动 Auth；再启动 Gateway。
java -jar jaycong-auth/target/jaycong-auth-0.1.0-SNAPSHOT.jar
~~~

Auth 默认使用 Nacos 发现 user。本地可设置 USER_SERVICE_URL=http://localhost:8087 直连；这只替代 Auth→User 寻址，其他 Nacos 配置仍按原配置加载。

两端共用 Sa-Token loginType jaycong-auth、token-name Authorization 和 Redis。User 使用 Auth 签发的 Token 校验会员管理权限；禁用先提交数据库再撤销共享会话，启用前清理旧会话。Auth 登录签发 Token 后再远程确认有效状态，复查失败撤销本次 Token。数据库和 Redis 不使用分布式事务，Redis 故障时禁用操作需重试撤销。

外部接口和使用方式见 [Auth](auth.md) 与 [会员管理](members.md)。Gateway 将 /auth/** 转发 Auth，将全部 /admin/** 转发 Admin，再由 Admin 调用 User/Goods。Nacos 已发布 routes 配置时也要同步更新路由与 ADMIN_SERVICE_TOKEN，见 [Admin](admin.md)。

# User / Auth Facade 拆分计划

**目标：** User 承载用户领域、数据库访问和会员管理，Auth 仅保留认证 HTTP 接口与 Redis 会话编排，通过 OpenFeign 调用 User。

**架构：** `common-api` 定义不含持久化实体和密码哈希的 User Facade 契约；User 的 `facade` 实现调用应用层，应用层调用领域服务和仓储接口；Auth 的基础设施适配器实现应用层端口。使用 Nacos 的 `user` 服务发现，允许 `USER_SERVICE_URL` 固定地址，内部请求以 `USER_SERVICE_TOKEN` 鉴权。会员接口由网关转发至 User，继续共享 `jaycong-auth` 会话空间。

**验证约束：** 按项目要求不新增、修改测试代码。通过编译打包、启动日志和命令行 HTTP 手工验证，明确报告未验证的外部依赖。

- [x] 在 `jaycong-common-api` 新增 `user.facade.UserFacade`、注册/凭证请求和无敏感凭证的用户响应。
- [x] User 将数据库实体移至 `infrastructure.persistence.entity`，仓储接口移至 `domain.repository`，以领域服务维护注册、账号校验及会员规则；移除认证 HTTP 入口和 Token 签发。
- [x] User 新增内部 Facade、调用凭证过滤器和异常响应，保留会员 HTTP 入口及共享会话撤销。
- [x] Auth 移除本地数据库、密码和会员能力，新增 Feign Client、远程错误转换和应用端口；保留原 `/auth` 响应结构与会话签发后二次用户状态确认。
- [x] 调整 Gateway 会员路由，更新 Auth/User/会员/RPC 使用说明和环境变量。
- [x] 构建 Auth、User、Gateway 及依赖，检查依赖边界和源文件；具备服务条件时手工验证注册、登录、退出、会员权限及内部接口保护。


## 实际验证

- 原 POM 执行：mvn -B -ntp -pl :jaycong-auth,:jaycong-user,:jaycong-gateway -am package -DskipTests -Dspring-boot.repackage.skip=true，编译及普通 JAR 打包成功。
- 默认 repackage 遇到现有 Auth/Gateway JAR 被 Windows 占用。未停止既有应用，使用模块目录中的临时 pom-facade-verify.xml，仅把 build.directory 改为 target/facade-verify，分别执行 mvn -B -ntp -f <module>/pom-facade-verify.xml package -DskipTests，三个可执行 JAR 打包成功；临时 POM 已清理。
- User 18087、Auth 18084、Gateway 18090 以临时运行配置启动，实际连接本机 MySQL 和 Redis。固定 URL 链路验证：注册、重复注册 409、密码错误 401、空载荷 400、登录、me、退出、会员分页/详情/修改、普通账号 403、乐观锁 409、禁用撤销 Token、启用不恢复旧 Token、重新登录与退出均符合预期。
- 内部接口无凭证返回 403；Gateway 内部路径返回 404；停止 User 后 Auth 返回 503。
- 独立 Nacos discovery group JAYCONG_FACADE_VERIFY 下，删除 Auth 固定 URL，Gateway 使用 lb://auth 与 lb://user，实际验证登录、me、会员分页及退出成功。未覆盖或发布现有 Nacos 配置，未验证远程配置热刷新。
- Feign JDK 默认客户端对本地 401 返回空响应体，导致错误转换为 503；Auth 引入 BOM 管理的 feign-hc5 后，错误密码正确返回 401。
- Auth 可执行产物无旧 domain、persistence 或会员类，无 MySQL/JDBC/MyBatis Starter。依赖树显示 crypto 经 Spring Cloud、mybatis-plus-annotation 经 common-core 传递，保留原公共能力边界。
- 独立只读审查未发现严重/重要阻断问题。不新增或修改测试代码，所有 Maven 验证均跳过测试。
- 新增手工联调账号 ID 2120813361768239106 已禁用，其会话已撤销；仅更新该账号的昵称/状态，既有管理员仅用于登录、查询与退出。临时运行进程已停止，既有服务未停止。

已知环境告警：关闭 Nacos 联调进程时，Nacos Client 的 NotifyCenter 销毁顺序触发空指针告警；三个临时进程均已退出。该告警未影响前述注册发现和 HTTP 验证，未在本次拆分中修改第三方客户端生命周期。

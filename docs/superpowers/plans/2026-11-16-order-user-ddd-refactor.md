# Order、User 参考 NFTurbo 的 DDD 分包调整

**目标：** 严格按本机 NFTurbo Order 的实际职责组织两个业务模块，删除仅用于转发和重复映射的层次，保留现有接口和业务流程。

**参考：** `D:/develop/CodeProject/NFTurbo/NFTurbo_Server/nft-turbo-business/nft-turbo-order`，以及已按相同模式调整的 `jaycong-business/jaycong-goods`。

**结构：** 入口直接调用 `domain.service`；业务实体和 ORM 映射统一在 `domain.entity`；响应转换位于 `domain.entity.convertor`；Mapper、HTTP 客户端和装配分别位于 `infrastructure.mapper`、`client`、`config`。异常放模块根包。不创建没有调用方的 ReadService、facade、job 或 listener。

**验证：** Java 21、Spring Boot 3.5、MyBatis-Plus；遵守项目约定，不新增、编写或修改测试代码，不提交用户工作区。

- [x] Order：合并 `domain.model.Order` 与 `infrastructure.persistence.entity.OrderDO`，订单创建规则保留在实体；Mapper 直接映射订单。
- [x] Order：用 `domain.service.OrderManageService` 直接协调商品客户端和 Mapper，删除 application、command、port、repository；Web 请求放 controller，响应放 Common API，通过 OrderConvertor 转换。
- [x] User：合并 UserAccount/User、MemberOperation/MemberOperationEntity；注册、会员更新及流水阶段行为归实体。
- [x] User：用 UserReadService/UserManageService、MemberOperationReadService/MemberOperationManageService 承接现有流程，直接调用 Mapper；保留乐观锁、独立流水事务、失败记录和 Redis 会话撤销顺序。
- [x] User：Facade 直接复用 Common API 请求，并集中转换响应；统一 infrastructure.config，删除重复 DTO、repository、application 和 CurrentUserService。
- [x] 更新 AGENTS.md 的模块补充、docs/order.md、docs/user.md 及引用旧服务名的 AuthContext 示例。
- [x] 执行 `mvn -B -ntp -pl :jaycong-order,:jaycong-user -am clean package -DskipTests`，确认没有旧类残留；检查实际可用的本机依赖并进行可行的启动/HTTP 手工验证。
- [x] 对照迁移前快照复核接口、表字段、金额转换、审计和状态逻辑；复核包名与路径一致且无旧层次引用。


## 实际验证记录

- 执行 `mvn -B -ntp -pl :jaycong-order,:jaycong-user -am clean package -DskipTests`，14 个 Reactor 项目全部 SUCCESS；未运行测试，未新增或修改测试代码。
- 使用 `java -jar <module>/target/<module>-0.1.0-SNAPSHOT.jar --spring.config.name=ddd-verification` 及命令行 MySQL、Sa-Token 参数，在 18086、18087 临时启动两个应用；关闭 Nacos 配置、注册发现和服务发现，未修改项目资源文件。
- HTTP 手工验证：`POST /orders {}` → 400；未登录 `POST /orders {"goodsId":"1"}` → 401；`GET /internal/users/0` → 401；`POST /internal/users/register {}` → 400；无内部管理凭证 `GET /internal/admin/members` → 403。
- `GET /internal/users/1` 从本机 MySQL 读取现有用户，响应字段为 email、id、mobile、nickname、role，不包含 passwordHash；未写入业务数据。
- 对照迁移前快照复核源码和资源：Order Java 文件 17 → 12、实际包 15 → 8；User Java 文件 30 → 19、实际包 13 → 7；资源和建表 SQL 完全保留。核验 package 与路径一致、JAR 无旧 application/infra/persistence/repository/domain.model 类残留。
- 独立只读代码评审未发现有证据的行为回归；`git diff --check` 通过。临时应用进程已停止。
- 限制：Redis、Nacos、Goods 未运行，未验证完整登录、成功下单、会员写入及会话撤销链路；数据库与 Redis 的原有非原子边界保持不变。

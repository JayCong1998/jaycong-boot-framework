# JayCong Boot Framework 开发约定

## 项目概述

JayCong Boot Framework 是一个基于 Java 21 和 Spring Boot 3.5 的 Maven 多模块基础框架，主要用于日常开发和技术学习。

当前 Maven 工程包含 6 个根级模块，其中 `jaycong-common` 聚合 9 个公共子模块，`jaycong-business` 聚合 4 个业务子模块，共 19 个模块（不含根聚合工程）。模块目录如下：

```text
jaycong-boot-framework/
├── jaycong-bom/                         # 独立依赖版本清单，不反向依赖根聚合 POM
├── jaycong-common/                      # 公共技术模块聚合父 POM
│   ├── jaycong-common-api/              # 微服务间共享的 Facade 接口、请求和响应契约
│   ├── jaycong-common-config/           # Nacos 服务注册发现与配置中心
│   ├── jaycong-common-core/             # 统一返回、错误码、业务异常及分页模型
│   ├── jaycong-common-datasource/       # MyBatis-Plus 分页、逻辑删除、乐观锁和审计填充
│   ├── jaycong-common-redis/            # Redis Starter 与统一序列化配置
│   ├── jaycong-common-rpc/              # OpenFeign 与 Spring Cloud LoadBalancer
│   ├── jaycong-common-satoken/          # Sa-Token、Redis 会话与 AuthContext
│   ├── jaycong-common-swagger/          # OpenAPI 文档与 Swagger UI 自动配置
│   └── jaycong-common-web/              # Servlet Web 自动配置、统一异常处理和参数校验
├── jaycong-gateway/                     # WebFlux 网关，默认端口 8090；不得引入 MVC Web Starter
├── jaycong-auth/                        # 认证 HTTP 入口，默认端口 8084，通过 User Facade 调用用户能力
├── jaycong-admin/                       # 管理端服务入口，默认端口 8088
├── jaycong-business/                    # 业务模块聚合父 POM
│   ├── jaycong-goods/                   # 商品服务，默认端口 8085
│   ├── jaycong-pay/                     # 支付业务模块骨架
│   ├── jaycong-order/                   # 订单服务，默认端口 8086
│   └── jaycong-user/                    # 用户与会员管理服务，默认端口 8087
├── mall-admin/                          # Vue 3 + Element Plus 运营后台，非 Maven 模块
├── mall-web/                            # React 商城前端，非 Maven 模块
└── docs/                                # 使用说明、设计文档和开发计划，非 Maven 模块
```

依赖方向应保持清晰：业务模块按需依赖公共模块，底层模块不得反向依赖业务模块。新增 Maven 模块时，更新所属聚合 POM 的 `<modules>`（根级模块更新根 `pom.xml`，公共或业务子模块更新对应父 POM）；如果模块需要被外部项目按 BOM 管理，也同步更新 `jaycong-bom/pom.xml`。

每次新增、删除或修改模块目录（包括重命名、移动及调整聚合归属）时，必须在同一次变更中同步更新本文件（`AGENTS.md`）的模块目录树、模块数量及相关说明，确保与实际目录结构和 Maven 聚合配置一致；非 Maven 模块的目录变更也需同步维护目录树及说明。

## 代码风格

- 使用 Java 21 语言特性，但优先选择团队容易阅读和调试的写法；不要为了炫技引入不必要的复杂抽象。
- 遵循 Spring Boot 的构造器注入，类和方法保持单一职责，避免无关的大型工具类或“大而全”配置类。
- 类型、方法和变量使用清晰的英文命名；包名全部小写，遵循 `io.github.jaycong.<module>` 命名空间。
- 一个源文件只放一个主要职责明确的顶层类；配置、Controller、生产者和消费者按功能或消息模式分包。
- 优先使用不可变数据；简单消息载荷可以使用 `record`。不要为示例引入不必要的 DTO 层或框架封装。
- 对外暴露的 Bean、配置属性、HTTP 接口和公共方法保持稳定、明确，并在行为不明显时补充说明。
- 使用现有 Maven 版本管理，不在子模块重复硬编码由父 POM 或 BOM 管理的版本。
- 修改已有代码时保留用户未提交的改动，不使用 `git reset --hard`、`git checkout --` 等会覆盖工作的命令。

## 业务模块开发约定

设计、实现或审查 Java/Spring 业务模块时，请参考并使用 [`ddd-module-design`](.agents/skills/ddd-module-design/SKILL.md) skill，按其中的架构规范和当前工程实际情况确定模块分包、分层及职责边界。

## 注释约定

- 注释解释“为什么这样做、有什么边界、如何使用”，不要逐行翻译代码或描述显而易见的 getter/setter。
- 自动配置类应说明生效条件、默认 Bean 的用途，以及用户 Bean 覆盖默认行为时的规则。
- RabbitMQ 示例应说明交换机类型、routing key、绑定关系、ACK/NACK、confirm/return、RPC 超时等关键概念；注释应帮助读者理解消息流，而不是重复类名。
- 注释使用简洁、准确的中文；类名、方法名、配置键、协议字段和代码符号保留原文。
- 公共 API 的 JavaDoc 说明参数、返回值、异常或重要限制；内部示例优先使用短注释，避免过度文档化。
- 不在注释中留下与当前实现不一致的未来计划、空泛的 TODO 或误导性的“保证可靠”等表述。

## Spring Boot 与自动配置约定

- Servlet 应用统一使用 `jaycong-common-web` 的 `GlobalExceptionHandler`；业务模块及其他公共模块不再各自声明 `ExceptionHandler` / `ControllerAdvice`。可公开的业务失败继承 `BusinessException`，数据库等技术异常在业务边界转换后交给公共处理器响应。
- Spring Boot 3 自动配置使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，不要新增旧版 `spring.factories` 作为唯一注册方式。
- 自动配置类使用 `@AutoConfiguration`、`@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件，默认能力必须允许业务 Bean 覆盖。
- Starter 不应扩大业务应用的组件扫描范围；优先通过自动配置注册框架 Bean。
- 配置属性使用 Spring Boot 标准命名和绑定方式；保留底层框架原生配置，不无故重新命名或屏蔽它们。

## 数据库建表约定

- 默认使用 MySQL 8、InnoDB、`utf8mb4` 字符集和 `utf8mb4_0900_ai_ci` 排序规则；表名、字段名使用小写下划线命名，并补充简洁中文注释。
- 主键统一使用 `id BIGINT`，由应用生成，不使用数据库自增。
- 业务表统一包含以下审计及控制字段：
  - `create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`：创建时间。
  - `update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`：更新时间，由应用维护，不配置 `ON UPDATE`。
  - `create_user_id BIGINT NOT NULL DEFAULT 0`：创建人ID，`0` 表示系统操作。
  - `update_user_id BIGINT NOT NULL DEFAULT 0`：更新人ID，`0` 表示系统操作。
  - `deleted TINYINT NOT NULL DEFAULT 0`：逻辑删除标识，`0` 未删除、`1` 已删除。
  - `lock_version INT NOT NULL DEFAULT 0`：乐观锁版本号。
- 审计字段由 MyBatis-Plus 自动填充维护；逻辑删除、乐观锁需配置对应能力，不能仅添加字段。更新语句应遵守乐观锁版本检查规则。
- 数据库约束保持简单：保留主键、必要的唯一索引和字段非空及默认值，不额外添加 `CHECK`、外键等约束；角色取值、状态及关联有效性由应用校验。按实际查询需要添加普通索引。
- 当前认证采用单表 `sys_user`，一个用户只有一个角色，直接保存到 `role VARCHAR(32) NOT NULL DEFAULT 'user'`，取值为 `admin`、`user`；暂不建立角色、权限及用户角色关联表。
- `sys_user` 不设置 `username`，使用 `mobile VARCHAR(32)` 和 `email VARCHAR(254)` 作为登录标识，分别建立唯一索引。两者允许为 `NULL`，由应用保证至少填写一项，空字符串统一转为 `NULL`，并统一手机号格式及邮箱不区分大小写的登录规则。
- 密码保存到 `password_hash VARCHAR(255) NOT NULL`，使用 BCrypt 哈希，不保存明文密码；用户表保留 `nickname`、`status` 和 `last_login_time` 字段。
- 用户逻辑删除后，原手机号和邮箱仍占用唯一值；登录查询必须排除已删除、已禁用用户。禁用或逻辑删除用户时，同步撤销其 Sa-Token 登录会话。

### 初始账号

以下为开发环境初始化数据，两个账号的初始密码均为 `Admin@2026!Init`，`password_hash` 为对应的 BCrypt 哈希。首次登录后修改密码；手机号为示例，可在执行前替换。超级管理员使用现有 `admin` 角色，不额外增加角色或隐式权限绕过逻辑。

| ID | 登录账号（手机号） | 初始密码 | 昵称 | 角色 |
| --- | --- | --- | --- | --- |
| 1 | `13800138000` | `Admin@2026!Init` | 超级管理员 | `admin` |
| 2 | `13800138001` | `Admin@2026!Init` | 普通用户 | `user` |

初始化前确认 ID 和手机号未被占用；以下 SQL 仅供手工执行，不作为应用启动时自动创建账号的配置。

```sql
INSERT INTO sys_user (
    id, mobile, email, password_hash, nickname, role, status,
    create_time, update_time, create_user_id, update_user_id,
    deleted, lock_version
) VALUES
(
    1, '13800138000', NULL,
    '$2a$12$QtWJSGkZPLCc6pb/pJClWeJW2A.rDS850Kcvd4KRH9P2IcRMPBECS',
    '超级管理员', 'admin', 1,
    CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0
),
(
    2, '13800138001', NULL,
    '$2a$12$QtWJSGkZPLCc6pb/pJClWeJW2A.rDS850Kcvd4KRH9P2IcRMPBECS',
    '普通用户', 'user', 1,
    CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0
);
```

## 测试约定

- 本项目当前开发约定是：**不要新增、编写或修改测试代码**，包括单元测试、集成测试、契约测试和 Testcontainers 测试。
- 需求实现以代码编译、应用启动、命令行或 HTTP 手工验证、日志检查和文档示例为主要验证手段。
- 不要为了“补测试”创建 `src/test` 文件、测试夹具或测试专用配置；如果已有测试因代码变更失败，应报告失败原因，不擅自扩展测试范围。

## 验证与交付

优先使用与改动范围匹配的 Maven 命令，例如：

```powershell
mvn -B -ntp -pl <module> -am package -DskipTests
```

涉及可运行应用时，应在条件允许的情况下启动应用并验证关键 HTTP/消息流程。RabbitMQ Demo 默认连接本机 `localhost:5672`，使用 `jaycong.demo.*` 资源前缀；不要删除或修改其他项目的 MQ 资源。

交付说明应简要列出修改的模块、实际执行的验证命令和已知限制。不要声称运行了未实际执行的测试或外部服务验证。

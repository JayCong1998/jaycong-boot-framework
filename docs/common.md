# Common 模块说明

`jaycong-common` 是 packaging=pom 的聚合父模块，只负责聚合和继承构建约定，不向业务统一引入依赖。

`jaycong-common-rpc` 提供 OpenFeign 与 Spring Cloud LoadBalancer 依赖组合，
goods、order、pay 均已引用；启用方式和服务发现边界见 [RPC 接入说明](rpc.md)。

| 原 artifactId | 当前 artifactId | 目录 |
| --- | --- | --- |
| jaycong-core | jaycong-common-core | jaycong-common/jaycong-common-core |
| jaycong-web-spring-boot-starter | jaycong-common-web | jaycong-common/jaycong-common-web |
| jaycong-mybatis-spring-boot-starter | jaycong-common-datasource | jaycong-common/jaycong-common-datasource |

groupId 仍为 io.github.jaycong，版本仍为 0.1.0-SNAPSHOT。Datasource 的 Java 包迁移至 `io.github.jaycong.datasource`，自动配置通过 AutoConfiguration.imports 注册。

## 依赖与继承

- 根 POM 聚合 BOM、Common 和应用/学习模块，Common 子模块继承 Common，Common 继承根 POM。
- 独立 BOM 管理六个公共 JAR 的版本，不继承根 POM，也不导入 Common。
- Web 和 Datasource 分别依赖 Core；Datasource 提供 MyBatis-Plus 的分页、逻辑删除、乐观锁与审计填充默认能力。
- Auth、RabbitMQ Demo 和 JVM 示例按需依赖公共 JAR，不依赖 Common 聚合 POM。
- Common Datasource 默认传递引入 MySQL JDBC 驱动（runtime），使用其他数据库时由业务项目排除 MySQL 驱动并添加对应驱动。
- Web 和 Datasource 仍通过 Spring Boot 自动配置接入。

## 构建与接入

全量打包：`mvn -B -ntp package -DskipTests`。

只构建 Web 及其依赖：`mvn -B -ntp -pl :jaycong-common-web -am package -DskipTests`。

外部项目先重新安装：`mvn -B -ntp install -DskipTests`，然后保留 BOM 导入，按上表使用新 artifactId。旧坐标没有提供兼容重定向。

历史设计和开发计划保留当时的模块名称，当前接入方式以本文和使用说明为准。

## Sa-Token 与身份上下文模块

`jaycong-common-satoken` 组合 `cn.dev33:sa-token-spring-boot3-starter`、`jaycong-common-redis` 和 `cn.dev33:sa-token-redis-template`，支持使用 Redis 存储 Sa-Token 会话，并提供全部通过 StpUtil 读取的 `AuthContext`、`LoginUser` 登录快照和默认 `jaycong-auth` 登录域。各依赖版本由 BOM 统一管理，登录接口和业务授权由应用提供。

业务项目导入 `jaycong-bom` 后添加：

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-common-satoken</artifactId>
</dependency>
```

该模块面向 Servlet 应用，保留 Sa-Token Starter 原生自动配置与 `sa-token.*` 配置项；默认 `StpLogic`、`AuthContext` 和身份异常处理器均可由应用 Bean 覆盖。WebFlux 网关保持 Sa-Token Core 与 Redis 集成，不引入 Servlet 模块。身份读取、登录快照写入及旧 Token 兼容规则见 [Sa-Token 接入说明](satoken.md)。

构建：`mvn -B -ntp -pl :jaycong-common-satoken -am package -DskipTests`。

## Redis 依赖模块

`jaycong-common-redis` 传递引入 Spring Boot Redis Starter，并自动注册 `RedisTemplate<Object, Object>`：键和 Hash 键使用字符串序列化，值和 Hash 值使用带类型信息的 JSON 序列化。业务声明名为 `jaycongRedisTemplate` 的 Bean 时可覆盖默认模板；Spring Boot 自带的 `StringRedisTemplate` 保持可用。

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-common-redis</artifactId>
</dependency>
```

Redis 连接地址、用户名和密码仍使用 Spring Boot 原生 `spring.data.redis.*` 配置。构建：`mvn -B -ntp -pl :jaycong-common-redis -am package -DskipTests`。

## Config 依赖模块

`jaycong-common-config` 组合 Nacos Discovery、Config Starter，Auth 和 Gateway 均已依赖。Gateway 额外使用 LoadBalancer 通过服务名转发。连接参数、Data ID 和手工验证见 [Nacos 接入说明](config.md)。

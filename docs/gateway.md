# Gateway 使用说明

jaycong-gateway 是独立运行的 Spring Cloud Gateway Server WebFlux 应用，默认监听 8090。使用 Java 21、Spring Boot 3.5.16、Spring Cloud 2025.0.3（Gateway 4.3.5），Cloud 版本由独立 BOM 管理。

版本依据：[Spring Cloud 兼容表](https://spring.io/projects/spring-cloud)、[2025.0 发布说明](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes)。

## 构建与运行

在仓库根目录执行：

```powershell
mvn -B -ntp -pl jaycong-gateway -am package -DskipTests
java -jar jaycong-gateway/target/jaycong-gateway-0.1.0-SNAPSHOT.jar
```

网关无需数据库或 RabbitMQ，但登录校验需连接 Auth 使用的 Redis；默认也需连接 Nacos 加载配置并注册服务。它不依赖任何业务应用的 JAR，也不引入基于 Servlet 的 common-web。Redis 连接使用 `AUTH_REDIS_HOST`、`AUTH_REDIS_PORT`、`AUTH_REDIS_PASSWORD`、`AUTH_REDIS_DATABASE`，须与 Auth 指向同一实例和数据库。

## 路由

| 网关路径 | 默认后端 | 环境变量 |
| --- | --- | --- |
| /auth、/auth/** | lb://auth（Nacos 服务发现） | AUTH_BASE_URL |
| /goods、/goods/** | lb://goods（Nacos 服务发现） | GOODS_BASE_URL |
| /orders、/orders/** | lb://order（Nacos 服务发现） | ORDER_BASE_URL |
| /admin、/admin/** | lb://admin（Nacos 服务发现） | ADMIN_BASE_URL |

请求路径、查询参数和业务响应保持原样，不移除路径前缀。网关不代理后端 Actuator，不自动暴露学习模块。

## 登录鉴权

`GatewayAuthenticationFilter` 使用 Auth 相同的 Sa-Token `loginType`（`jaycong-auth`）和 Redis 会话，读取 `Authorization: Bearer <token>`。`/admin/**`、`/orders/**` 及 Auth 的当前用户、退出和其他非公开接口要求有效登录会话；缺少、过期或无效 Token 时由网关返回 HTTP 401 JSON。匿名接口为 `POST /auth/login`、`POST /auth/register`、`GET /auth/status`、商品公开查询和聚合 OpenAPI 文档。

网关只统一校验登录状态，不解析角色，也不代替服务内的管理员、资源归属等授权规则。业务服务应继续校验具体操作权限；不要信任客户端自行提交的身份头。

覆盖网关端口可使用 GATEWAY_PORT 或 --server.port；后端地址使用上述环境变量。Gateway 原生配置前缀为 spring.cloud.gateway.server.webflux，连接超时默认 2000 毫秒，响应超时默认 5 秒。慢接口需按实际调用预算调整；客户端超时不代表后端事务未提交，不自动重试写请求。

## 手工验证

先按 [Auth 使用说明](auth.md) 启动认证服务，再在独立终端启动网关：

```powershell
Invoke-RestMethod http://localhost:8090/actuator/health
Invoke-RestMethod http://localhost:8090/auth/status
```

业务服务响应继续采用自身 Result 模型；未匹配路由返回 404，没有可用 Auth 实例返回 503，连接失败通常返回 500，响应超时通常返回 504，网关自产错误使用 WebFlux 默认格式，不加载 MVC 异常处理器。健康检查 UP 仅代表网关自身可用，不表示所有后端健康。

当前提供基础路由、健康检查与共享 Sa-Token 会话的登录鉴权；尚未接入限流与跨域配置。浏览器跨域来源应在明确前端地址后按需配置。

## 控制台访问日志

默认通过 `GatewayAccessLogConfiguration` 开启 Reactor Netty 原生访问日志，在响应完成时输出，无需 JVM accessLogEnabled 参数。
它覆盖已匹配和未匹配路由的 HTTP 请求，不引入 Servlet common-web，不开启可能输出请求内容的 wiretap。

```yaml
jaycong:
  gateway:
    access-log:
      enabled: true
logging:
  level:
    reactor.netty.http.server.AccessLog: INFO
```

设 `enabled: false` 可关闭本配置的启用行为；不要同时使用 JVM 参数或其他 Netty customizer 独立启用访问日志。
上述开关在应用启动时生效，修改后需要重启应用。默认随 Spring Boot 日志打印到控制台，不配置文件 Appender。

```text
event=http_access method=GET path=/auth/status status=200 durationMs=18
```

记录 method、path、status、durationMs，移除查询字符串并替换路径中的控制字符；不采集请求头、请求体或响应体。
日志耗时由 Netty 统计；连接提前中断且响应未完成时，不能依赖该日志作为完整连接审计。

本次验证使用临时端口 18090/18084、命令行覆盖配置位置并禁用 Nacos，以直连路由访问 Auth。
已验证直连与代理 `/auth/status` 返回 200、两端不存在的路径返回 404、Auth 空登录载荷返回 400，控制台均有对应访问日志，查询参数标记未出现在访问日志中。
未验证 Nacos 注册发现、数据库/Redis 登录流程及 Servlet 异步端点；三个业务模块仍为无启动入口的骨架。

## User / Auth 拆分

/auth、/auth/** 转发 lb://auth；/goods、/goods/** 转发 lb://goods；全部 /admin、/admin/** 统一转发 lb://admin，再由 Admin 调用 User/Goods 内部管理 Facade。内部 /internal/** 不配置路由。聚合文档 /openapi/admin 替换原 /openapi/user。若 Nacos gateway.yml 覆盖 routes，请删除旧会员、商品管理直连路由并同步此配置，发布说明见 [Admin](admin.md)。

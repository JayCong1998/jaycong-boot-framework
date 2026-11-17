# Swagger / OpenAPI 接口文档

项目使用 springdoc-openapi 2.8.17（匹配 Spring Boot 3.5），由各业务服务生成 OpenAPI 3 文档，Gateway 提供统一 Swagger UI。无需维护静态 JSON 文件。

## 访问入口

| 入口 | 默认地址 |
| --- | --- |
| 网关统一 Swagger UI | http://localhost:8090/swagger-ui/index.html |
| 网关 Auth 文档 JSON | http://localhost:8090/openapi/auth |
| 网关 User 文档 JSON | http://localhost:8090/openapi/user |
| Auth 独立 Swagger UI / JSON | http://localhost:8084/swagger-ui/index.html / http://localhost:8084/v3/api-docs |
| User 独立 Swagger UI / JSON | http://localhost:8087/swagger-ui/index.html / http://localhost:8087/v3/api-docs |

统一页面右上角选择 Auth 或 User。Auth 包含注册、登录、当前用户、当前会话和状态接口；User 只包含 `/admin/members` 会员管理接口。`/internal/users/**` 不进入公开文档，也没有网关转发路由。扫描范围仅控制文档展示，不是访问控制。

## 新服务接入

MVC 业务服务引入 common-web 即拥有 Swagger 能力，无需再直接依赖 common-swagger，版本由 jaycong-bom 管理：

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-common-web</artifactId>
</dependency>
```

依赖方向为：业务服务 → common-web → common-swagger → springdoc MVC UI。common-swagger 不反向依赖 common-web。所有引入 common-web 的 MVC 应用都会获得文档能力，扫描范围和文档开关由各应用自行配置。

common-swagger 通过 Spring Boot AutoConfiguration.imports 注册配置，无需增加组件扫描。默认 OpenAPI Bean 提供标题、版本、相对服务入口和 `bearerAuth` 认证方案；业务自定义 OpenAPI Bean 时默认 Bean 退让。默认不对所有接口声明认证，使用 `@SecurityRequirement(name = "bearerAuth")` 标记需要登录的类或方法。

```yaml
jaycong:
  swagger:
    title: 示例服务 API
    description: 示例服务接口说明
    version: v1
    server-url: /
springdoc:
  packages-to-scan: io.github.jaycong.example.controller
  paths-to-match: /examples/**
```

扫描、分组、UI 和开关继续使用 springdoc 原生配置。使用 `@Tag` 描述分组、`@Operation` 描述接口、`@Schema` 描述字段、`@Parameter` 描述查询参数。`server-url: /` 使 Try it out 沿用当前浏览器来源，直连 UI 调本服务，经网关 UI 调网关；如另有外部路径前缀，应按部署配置覆盖。

Gateway 是 WebFlux，单独依赖 `springdoc-openapi-starter-webflux-ui`，不能依赖包含 MVC 的 common-swagger。新增服务时，在 `spring.cloud.gateway.server.webflux.routes` 中添加专用文档路由，将 `/openapi/<service>` 重写至该服务的 `/v3/api-docs`，并在 `springdoc.swagger-ui.urls` 中添加菜单项。业务路由仍需单独配置。

## 带 Token 调试

1. 选择 Auth 文档，在 `POST /auth/login` 中使用已有账号登录。
2. 复制响应 `data.tokenValue`，点击 Authorize，仅粘贴 Token 值，不加 `Bearer ` 前缀。
3. 调用 `GET /auth/me`，Swagger UI 会添加 `Authorization: Bearer <token>`。
4. 切换 User 文档时，如未带认证，再点击 Authorize 填入同一个 Token。会员接口要求 admin 角色，普通用户无权管理会员。
5. 退出使用 `DELETE /auth/session`，仅撤销当前 Token；原 `POST /auth/logout` 作为兼容接口标记为 deprecated。

UI 未启用 Token 持久化；文档认证声明不替代服务中的真实认证、权限校验。执行注册、更新、禁用等接口会实际修改数据。

## 环境与启动

```powershell
mvn -B -ntp -pl jaycong-auth,jaycong-business/jaycong-user,jaycong-gateway -am package -DskipTests
java -jar jaycong-business/jaycong-user/target/jaycong-user-0.1.0-SNAPSHOT.jar
java -jar jaycong-auth/target/jaycong-auth-0.1.0-SNAPSHOT.jar
java -jar jaycong-gateway/target/jaycong-gateway-0.1.0-SNAPSHOT.jar
```

默认使用 Nacos 发现 `auth`、`user`，文档路由与业务路由使用相同的实例。需要 Nacos、Redis、MySQL，连接方式见 [配置说明](config.md) 和 [User 说明](user.md)。若 Nacos 的 `gateway.yml` 已覆盖 routes 列表，应同步加入两条文档路由；本次代码修改不会自动发布远程配置。

生产环境可在三个应用都设置 `API_DOCS_ENABLED=false`，同时关闭文档 JSON 和 UI。也可以通过原生 `springdoc.api-docs.enabled=false`、`springdoc.swagger-ui.enabled=false` 配置。只关闭 Gateway UI 不会关闭下游文档，需同步处理 Auth、User。关闭下游后，网关文档路由仍存在，但无法返回文档。

## 本次验证记录

2026-11-12 执行上述 Maven 构建，结果 BUILD SUCCESS；按项目约定未新增、修改或运行测试。检查 Gateway JAR 包含 WebFlux springdoc，不含 spring-webmvc。

在临时端口启动新产物：Auth 18084、User 18087、Gateway 18090。保留 Nacos Config 读取但关闭临时实例注册，Gateway 使用 SimpleDiscoveryClient 静态实例指向本次 Auth/User，Auth 使用 Feign URL 指向本次 User；Redis、MySQL 使用本机已有服务。没有发布 Nacos 配置或重启原有 8084、8087、8090 进程。

- Auth/User 直连 `/v3/api-docs`、网关 `/openapi/auth` 和 `/openapi/user` 均为 HTTP 200。
- 网关 `/v3/api-docs/swagger-config` 正确列出两个入口；Swagger UI HTML 和 JS 静态资源为 HTTP 200。
- 文档标题、相对 `servers.url=/`、匿名登录/注册、受保护接口的 bearerAuth 声明均核对通过；User 文档只包含三条会员资源路径，未包含内部 RPC。
- 经网关使用已有管理员登录，查询 `/auth/me` 和会员分页均成功（统一响应 code=0）；最后删除本次会话，旧 Token 请求返回 HTTP 401。登录验证会按业务逻辑更新管理员最近登录时间，未注册或修改会员资料。
- 匿名访问 `/auth/me`、`/admin/members` 返回 HTTP 401；经网关访问 `/internal/users/1` 返回 HTTP 404。

限制：没有进行浏览器交互操作或修改类接口验证；新文档链路采用静态发现实例验证，未让临时实例加入现有 Nacos 注册列表。关闭文档开关的额外启动验证被自动审批拒绝，仅返回 blocked by policy，因此未确认该开关的运行结果。默认端口已有旧版本服务，需重启三个应用才能使用本次新文档。

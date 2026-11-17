# Web Starter 使用说明

## 接入

引入 `jaycong-common-web` 即可获得 Spring MVC、Jakarta Validation、全局异常处理和控制台访问日志。
Starter 使用 `AutoConfiguration.imports` 注册，仅在 Servlet Web 应用中生效。
业务应用保留自己的 `@SpringBootApplication` 扫描范围，无需扫描 `io.github.jaycong.web`。
响应统一包含 `success`、`code`、`message`、`data`；code 为整数，success 为由 code 自动推导的布尔值。
成功结果由 controller 显式返回 `Result.success(...)`。

## HTTP 约定

| 情况 | HTTP 状态 | code | message |
| --- | --- | --- | --- |
| 正常请求 | 200 | `0` | 成功 |
| 请求 DTO 校验、MVC 方法参数校验、缺少参数、类型转换错误、JSON 格式错误 | 400 | `400` | 参数错误 |
| 使用 `ErrorCode` 构造的 `BusinessException` | 422 | 业务错误码 | 异常中的公开消息 |
| 使用 `status` 构造的 `BusinessException` 及业务子类 | 异常指定的 400～599 状态 | 与状态相同 | 异常中的公开消息 |
| Sa-Token `NotLoginException` | 401 | `401` | 未登录或登录已失效 |
| `AuthContextUnavailableException` | 503 | `503` | 登录会话服务暂不可用，请稍后重试 |
| 路由或静态资源不存在 | 404 | `404` | 资源不存在 |
| 其他 MVC 协议异常，如不支持的方法或媒体类型 | 保留原状态，如 405、415 | `405`、`415` 等 | HTTP 原因短语 |
| 未知异常或 MVC 服务端错误 | 500（MVC 原生服务端错误保留其状态） | `500` | 系统异常 |

正常响应的 `success` 为 true，错误响应为 false，错误响应的 `data` 为 null。
框架不会把异常堆栈、内部异常消息或被拒绝的参数值返回客户端；
未知异常记录完整服务端日志。业务异常消息应由业务方保证可以公开。
协议响应头（例如 405 的 Allow）保留。

DTO 使用 `@Valid @RequestBody`，字段使用 `@NotBlank` 等约束；查询参数可直接标注
`@Min` 等约束，使用 Spring MVC 原生方法校验。Controller 不添加类级 `@Validated`。
当前统一映射针对 MVC 请求处理，不接管 Filter、Spring Security 或异步后台任务异常。
实现基于 [Spring ResponseEntityExceptionHandler](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler.html)，
方法校验约定见 [Spring MVC Validation](https://springframework.org/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-validation.html)。

所有 Servlet 模块统一使用 common-web 的 `GlobalExceptionHandler`，业务模块及 common-satoken 不再声明自己的异常处理器。
业务异常继承 common-core 的 `BusinessException`，保留业务原因及公开消息；已有明确 HTTP 状态时调用 `super(status, message)`，需要诊断原因时调用 `super(status, message, cause)`。
原有 `ErrorCode` 构造方式仍默认返回 422，错误码与 HTTP 状态独立。数据库等技术异常由业务仓储在已知边界转换为业务异常，不在公共处理器中硬编码业务表的规则。
common-web 仅增加 `sa-token-core` 以识别登录异常，不引入认证 Starter 或 Redis，也不启用登录会话。
需要整体替换默认行为时，声明一个 `GlobalExceptionHandler` 类型（或其子类）的 Bean，自动配置会退让；框架 advice 优先级为 `LOWEST_PRECEDENCE`。

## 控制台请求日志

common-web 通过自动配置注册 `RequestLogFilter`，默认开启，无需扩大组件扫描范围。

- `HTTP请求开始`：requestId、method、uri（不含查询串）、ip（直接连接的远端地址）。
- `HTTP请求结束`：同一 requestId、method、uri、ip、status、cost；逃出调用链的异常另记录异常类型。
- 正常结束为 INFO；4xx 或耗时达到 3000ms 为 WARN；5xx 或逃出调用链的异常为 ERROR。MVC 已处理的异常按最终响应状态分类。

响应头 `X-Request-Id` 返回生成的请求编号。同步调用链的 MDC 保存 `requestId`，结束后恢复原值，业务及异常日志可用 `%X{requestId}` 关联。不采集查询参数、请求头、请求体和响应体；不要把敏感信息放进 URL 路径。

当前针对同步 Servlet 请求；异步请求的结束日志表示初次 Filter 调用返回，不代表异步响应完成，MDC 不会自动传播到异步线程。未处理异常的结束状态是 Filter 退出时的响应状态，容器后续错误处理可能改变实际状态。

```yaml
jaycong:
  web:
    access-log:
      enabled: true
logging:
  level:
    io.github.jaycong.web.access: INFO
```

`enabled: false` 关闭框架默认注册；应用可提供 `RequestLogFilter` Bean 替换默认实现。业务自定义 Bean 的生命周期和启用行为由业务负责。

Auth、Gateway 的 `application.yml` 已配置 UTF-8 控制台、root 和项目包 INFO 级别，以及时间、级别、应用名、线程、requestId、logger 和消息；异常堆栈继续输出。可通过原生 `logging.level.*` 覆盖级别，不建议全局 DEBUG。Nacos 若配置相同日志键，会覆盖本地默认值。

网关保留 Netty 的 `event=http_access` 响应结束摘要，不引入 Servlet Filter；网关日志的 requestId 为空，当前不提供跨服务链路追踪。

启动 Auth 后访问 `http://localhost:8084/auth/status`，控制台应出现同一 requestId 的开始、结束日志，响应头也包含该编号；通过网关访问时还可看到网关的访问摘要。端口以实际配置为准。

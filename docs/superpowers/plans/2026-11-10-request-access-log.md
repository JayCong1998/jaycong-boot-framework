# 请求访问日志实施清单

目标：网关和 Servlet 业务服务输出控制台访问日志，不采集请求体、查询参数和凭据。

方案：网关配置 Reactor Netty access log；common-web 自动注册 Servlet Filter，业务模块显式依赖 common-web。沿用 Spring Boot 默认控制台日志输出，不新增日志存储设施。

- [x] Gateway：增加可配置 Netty WebServerFactoryCustomizer，使用只包含 method、path、status、durationMs 的日志格式。
- [x] common-web：增加配置属性和访问日志过滤器，通过自动配置注册；支持开关、排除路径、用户 Bean 覆盖，异步请求完成后记录一次。
- [x] Business：goods、order、pay 显式增加 common-web 依赖；Auth 已有依赖。
- [x] 文档：说明字段、配置、敏感数据边界和手工验证命令。
- [x] 验证：按 AGENTS.md 不新增或修改测试代码，执行相关模块 Maven package -DskipTests，并使用临时端口及命令行配置覆盖手工验证 HTTP 日志。

不包含认证审计事件、跨服务追踪及请求/响应内容采集。保留工作区已有改动，不提交混合工作。

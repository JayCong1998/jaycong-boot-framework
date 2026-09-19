# Web Starter + Minimal Example Implementation Plan

**Goal:** 自动装配异常处理与参数校验，提供可运行 Example，验证三条请求路径。
**Architecture:** 使用 Boot 自动配置注册 Servlet MVC advice，复用 Core Result。Example 仅扫描自己的包。
**Tech Stack:** Java 21 / Spring Boot 3.5.16 / Jakarta Validation / JUnit 5 / MockMvc。
**Spec:** 用户已确认本次会话中的设计：成功 200、参数错误 400、业务异常 422、未知异常 500。

## 实施顺序

- [x] Starter：先编写真实 MVC 请求测试并运行失败，覆盖 JSON 校验、查询参数校验、业务异常、未知异常、HTTP 原生错误状态。
- [x] 增加 `web/autoconfigure/JaycongWebAutoConfiguration.java` 与 `web/exception/GlobalExceptionHandler.java`，通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。
- [x] 自动配置仅 Servlet 生效，用户提供 GlobalExceptionHandler 时退让；使用上下文测试验证。
- [x] Example：测试 `/example/greetings` 的成功、空白名称、业务拒绝三条路径；实现应用、DTO、controller 和 repackage。
- [x] 执行 `mvn -B -ntp clean verify`，启动打包 JAR 并通过 HTTP 验证三条路径。
- [x] 更新 README 与 `docs/web.md`，说明 HTTP 状态、启动命令和调用方法。

当前目录无 Git 元数据，直接修改工作目录，不创建提交。

## 验证记录

- Starter 首轮 10 项测试：7 项缺少统一 JSON 响应而失败，2 项异常未处理，1 项正常请求通过；实现后全部通过。
- Example 首轮 3 项测试均收到 404；增加接口后通过真实随机端口 HTTP 测试。
- 最终全量 `clean verify` 成功；Starter 13 项测试、Example 3 项测试通过，Core 原有测试通过。
- 独立启动打包 JAR，三条实际 HTTP 请求分别返回 200/0、400/INVALID_ARGUMENT、422/NAME_RESERVED；测试进程已关闭。
- 独立只读代码审查未发现范围内的正确性问题。

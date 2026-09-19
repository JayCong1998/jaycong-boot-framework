# Web Starter 与最小 Example

## 接入

引入 `jaycong-web-spring-boot-starter` 即可获得 Spring MVC、Jakarta Validation 和全局异常处理。
Starter 使用 `AutoConfiguration.imports` 注册，仅在 Servlet Web 应用中生效。
业务应用保留自己的 `@SpringBootApplication` 扫描范围，无需扫描 `io.github.jaycong.web`。
响应统一包含 `success`、`code`、`message`、`data`；code 为整数，success 为由 code 自动推导的布尔值。
成功结果由 controller 显式返回 `Result.success(...)`。

## HTTP 约定

| 情况 | HTTP 状态 | code | message |
| --- | --- | --- | --- |
| 正常请求 | 200 | `0` | 成功 |
| 请求 DTO 校验、MVC 方法参数校验、缺少参数、类型转换错误、JSON 格式错误 | 400 | `400` | 参数错误 |
| `BusinessException` | 422 | 业务错误码 | 异常中的公开消息 |
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

需要替换默认行为时，声明一个 `GlobalExceptionHandler` 类型（或其子类）的 Bean，自动配置会退让。
也可使用更高优先级的业务 `@RestControllerAdvice`；框架 advice 优先级为 `LOWEST_PRECEDENCE`。

## 运行 Example

```powershell
mvn -B -ntp clean verify
java -jar jaycong-example/target/jaycong-example-0.1.0-SNAPSHOT.jar
```

默认端口为 8080；已有 H2 驱动使数据源使用临时内存数据库，不需要安装 MySQL。
本步没有 Mapper，启动时 MyBatis 可能提示未发现 Mapper，不影响演示接口。
也可先在根目录执行 `mvn -B -ntp install`，再执行
`mvn -B -ntp -pl jaycong-example spring-boot:run`。

接口：`POST /example/greetings`，请求 Content-Type 为 `application/json`。

| 请求体 | HTTP | 返回 |
| --- | --- | --- |
| `{"name":"Jay"}` | 200 | `{"success":true,"code":0,"message":"成功","data":"Hello, Jay!"}` |
| `{"name":" "}` | 400 | `{"success":false,"code":400,"message":"参数错误","data":null}` |
| `{"name":"admin"}` | 422 | `{"success":false,"code":10001,"message":"该名称为系统保留名称","data":null}` |

`admin` 为演示用业务保留名称，忽略大小写及首尾空白。
在 PowerShell 中逐条调用并查看状态和响应（包括非 2xx）：

```powershell
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
try {
    foreach ($body in '{"name":"Jay"}', '{"name":" "}', '{"name":"admin"}') {
        $content = [System.Net.Http.StringContent]::new($body, [System.Text.Encoding]::UTF8, 'application/json')
        $response = $client.PostAsync('http://localhost:8080/example/greetings', $content).GetAwaiter().GetResult()
        try {
            [int]$response.StatusCode
            $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        } finally {
            $response.Dispose()
            $content.Dispose()
        }
    }
} finally {
    $client.Dispose()
}
```

## 验证

`mvn -B -ntp clean verify` 执行全部测试，并打包可执行 JAR。
Web 测试通过未扫描 Starter 包的应用验证自动发现及异常响应，另测 Servlet 条件和用户 Bean 退让。
Example 测试以随机端口启动真实服务器，检查上述三条路径的 HTTP 状态与 JSON 响应。

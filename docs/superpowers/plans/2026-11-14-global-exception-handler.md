# 统一异常处理实施清单

**Goal:** 所有 Servlet 模块只使用 common-web 的 GlobalExceptionHandler。

**Architecture:** 业务异常继承 common-core 的 BusinessException，携带公开消息、错误码和响应状态；common-web 统一映射业务及 Sa-Token 登录异常。商品数据库唯一键冲突在仓储边界转换，公共模块不依赖业务模块。

**Tech Stack:** Java 21、Spring Boot 3.5、Sa-Token、Maven。

**执行约束:** 在当前工作区增量修改，保留已有改动，不自动提交。不新增、修改测试代码，使用 Maven 打包和条件允许的 HTTP 手工验证。

- [x] 扩展 BusinessException，保留 ErrorCode 构造方式的默认 422，新增明确状态构造方式；GlobalExceptionHandler 使用异常状态，并统一处理 NotLoginException。
- [x] Auth、Admin、User、Order、Goods、AuthContextUnavailableException 继承公共业务异常，保留既有公开消息、状态及原因字段；商品仓储将唯一键冲突转换为 CONFLICT。
- [x] 删除六个独立处理器及 Sa-Token 处理器 Bean，common-web 增加 sa-token-core 依赖，移除 common-satoken 不再需要的 spring-web 直接依赖。
- [x] 更新 docs/web.md、docs/satoken.md、docs/admin.md、docs/order.md、docs/goods.md 及仓库异常处理约定。
- [x] 执行 `mvn -B -ntp -pl jaycong-auth,jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-business/jaycong-order -am clean package -DskipTests`；检查剩余 advice 仅 common-web 一个，并按实际条件记录 HTTP 验证与限制。

## 实际验证

- Maven 17 个 reactor 项目全部 SUCCESS，测试跳过，未新增或修改测试代码。
- 全仓源码仅 common-web 的 GlobalExceptionHandler 声明 RestControllerAdvice / ExceptionHandler。
- 最新 Auth JAR 在 127.0.0.1:18084 启动成功；使用独立 spring.config.name，关闭 Nacos 配置及发现，User Feign 地址指向未启动的本地 18087；验证结束停止该临时进程。
- curl 手工请求：无 Token 的 GET /auth/me 返回 401；POST /auth/login 空对象返回 400；合法格式请求在 User 不可达时返回 503；携带无效 Token 在 Redis 不可达时返回 503；不存在路径返回 404；POST /auth/me 返回 405 且保留 Allow: GET。
- 未启动完整业务链，未执行真实数据库商品冲突及真实登录流程；其他业务模块完成编译打包和异常映射代码复核。

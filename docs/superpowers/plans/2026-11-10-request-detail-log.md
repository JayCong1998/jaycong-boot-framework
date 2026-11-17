# Controller 双日志实现记录

按用户要求简化为一个 RequestLogAspect，替代此前多组件方案。

- [x] common-web 引入受 BOM 管理的 spring-boot-starter-aop。
- [x] 一个切面负责 Controller 开始/结束日志、参数和返回值、requestId、耗时、字段脱敏及输出长度限制；现有自动配置注册 Bean。
- [x] 不恢复用户删除的 Filter、Interceptor、BodyAdvice、Properties 等日志类，并移除其残留配置引用。
- [x] 更新 docs/web.md，说明切面只覆盖进入 Controller 方法的调用，不覆盖参数校验失败、最终异常响应、异步最终结果。
- [x] 执行 mvn -B -ntp -pl :jaycong-auth,:jaycong-goods,:jaycong-order,:jaycong-pay -am clean package -DskipTests，构建成功。首次因旧验证进程占用 Auth JAR 失败，停止对应进程后重试成功。
- [x] 临时端口 18084 手工访问 /auth/status、/auth/logout、/auth/login（非法账号但通过 DTO 校验），分别返回 200、401、400；产生三组开始/结束日志，开始含 Controller，查询 Token 和 JSON 密码标记未输出。未调用数据库和 Redis 登录流程。

未新增或修改测试代码。网关继续保留 Netty HTTP 访问摘要；新日志开关为 jaycong.web.access-log.enabled。

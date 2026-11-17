# Auth 普通分包重构计划

**目标：** Auth 不使用 DDD 或 application/port 分层，以普通 Controller、Service、Exception、Request、Response 组织认证接口。

**设计：** controller 调用 service.AuthService；AuthService 调用 service.UserService 委托 UserFeignClient，统一转换远程错误。client 保留远程契约适配，config 保留会话和 Feign 配置。用户规则仍由 User 负责，外部路径及响应不变。

- [x] 扁平化分包，移除重复应用请求和 UserGateway 接口。
- [x] 将远程用户调用放入 UserService，保持 400/401/409 转换和 503 故障边界。
- [x] 保留登录后状态复查、失败撤销当前 Token、无效账号撤销全部会话和退出当前 Token。
- [x] 更新 Auth 使用说明，Maven package -DskipTests 验证，检查产物不存在旧分层类。不新增或修改测试代码。

## 实际验证

- `mvn -B -ntp -pl jaycong-auth -am package -DskipTests` 编译成功，原 JAR 重打包因 Windows 无法重命名产物失败。
- 首次单模块临时 POM 构建使用了本机旧 common-api 依赖而编译失败；改用 Reactor 构建获取当前源码依赖。
- 临时根 POM 将 Auth 指向临时模块 POM，仅修改 Auth build.directory 为 target/flat-refactor-verify；执行 `mvn -B -ntp -f pom-flat-verify.xml -pl :jaycong-auth -am package -DskipTests` 成功。临时 POM 已删除。
- 检查可执行 JAR：仅包含 controller、service、exception、request、response、client、config，无 application、domain、infrastructure 或 Gateway 类。
- 独立端口 18086 启动成功，关闭 Nacos、使用独立运行参数。手工 HTTP 验证 status=200、未登录 me/logout=401、空登录请求=400、User 不可用时登录=503。
- 临时进程已停止，未新增或修改测试代码。未验证真实 User/Redis 登录签发、注册、禁用和会话撤销链路。
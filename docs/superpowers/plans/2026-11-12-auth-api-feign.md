# Auth 直接使用 API Feign 客户端

**目标：** 远程用户能力统一由 common-api 提供客户端声明，Auth 保持普通 Controller / Service 分包。

**方案：** API 的 user.client.UserFeignClient 继承纯 Java UserFacade，user.config.UserFeignConfiguration 仅作用于该客户端。Auth 显式启用 API Client，UserService 保留错误转换。common-rpc 提供 Starter、LoadBalancer 和 HC5，API 仅依赖声明所需的 OpenFeign Core、Spring Web，不自动注册客户端。

- [x] 迁移客户端和专用配置，Auth 直接导入 API Client。
- [x] 调整 API/RPC 依赖，HC5 统一下沉 RPC。
- [x] 使用 jaycong.rpc.user.url/token，兼容旧 Auth 配置；保留 authUser contextId 及其官方超时配置。
- [x] 更新 Auth、User、RPC 接入文档。
- [x] 执行 Auth/User Reactor 普通 JAR 编译打包，不新增或修改测试代码。
- [x] 独立输出目录生成可执行 JAR，并手工验证启动及远程故障响应。

## 验证

`mvn -B -ntp -pl :jaycong-auth,:jaycong-user -am package -DskipTests '-Dspring-boot.repackage.skip=true'` 成功。PowerShell 中带点的 -D 参数需用引号完整传入。

独立输出目录验证：临时根/模块 POM 仅改变 Auth 的 build.directory，运行 `mvn -B -ntp -f pom-api-verify.xml -pl :jaycong-auth -am package -DskipTests`，可执行 JAR 打包成功。临时 POM 已清理。

临时端口 18086 使用新 `jaycong.rpc.user.url` 参数启动成功，API 客户端注入成功；手工验证 status=200、未登录 me=401、远程服务不可用时 login=503。产物检查确认 UserFeignClient 和 UserFeignConfiguration 位于 common-api JAR，Auth 不再包含本地 Client，且包含 HC5 依赖。临时进程已停止。

未新增或修改测试代码；未验证真实 User 的成功注册/登录和 Nacos 发现链路。本次启动使用固定 URL，未修改或发布现有 Nacos 配置。
# Auth 服务骨架计划

目标：新增可独立运行的 Auth 服务和网关路由，延续当前逐步搭建框架模块的范围。

- 新增 jaycong-auth/pom.xml，继承根 POM，按需依赖 Common Web 和 Actuator，配置可执行 JAR。
- 新增 AuthApplication、服务状态接口及 8084 端口配置，不预设登录协议或增加数据库依赖。
- 根聚合新增模块，网关新增可配置 AUTH_BASE_URL 的 /auth 路由；应用不加入公共 BOM。
- 更新使用文档和开发约定；执行全量 package -DskipTests，启动 Auth 与网关手工验证健康和转发。

不新增或修改测试代码；状态接口不能作为身份认证结果使用。

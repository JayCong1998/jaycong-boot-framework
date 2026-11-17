# Auth / Gateway 配置拆分计划

目标：保留 Config Data 加载机制，将启动连接配置与可迁移业务配置分开。

- [x] 各模块新增 bootstrap.yml，保存服务名、Nacos 引用及 Gateway reactive 类型。
- [x] application.yml 显式导入 bootstrap，保留原业务配置。
- [x] docs/config.md 说明加载机制和 Nacos 迁移边界。
- [x] 执行 `mvn -B -ntp -pl jaycong-auth,jaycong-gateway -am package -DskipTests`，BUILD SUCCESS。
- [x] 两个 JAR 分别以 `--server.port=0 --logging.level.org.springframework.boot.context.config=TRACE` 启动，日志确认 bootstrap、公共 Nacos 配置和对应 Data ID 加载，两个应用均成功启动；验证进程已停止。

验证边界：远程 auth.yml、gateway.yml 均为空，未验证非空远程业务配置覆盖及热刷新；未新增或修改测试代码。

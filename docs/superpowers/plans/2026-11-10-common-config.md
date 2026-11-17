# Common Config Implementation Plan

**Goal:** Auth 和 Gateway 使用 Nacos 注册发现及配置中心。

**Architecture:** Common Config 组合官方 Starter 与显式导入的公共 YAML，应用声明自己的远程配置。网关使用 LoadBalancer 按服务名路由。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Cloud 2025.0.3、Alibaba 2025.0.0.0。

- [x] 新增 `jaycong-common/jaycong-common-config/pom.xml` 和 `src/main/resources/jaycong-nacos.yml`，注册到 Common POM；在 BOM 管理 Alibaba 及公共模块版本。
- [x] 修改 Auth、Gateway POM 引入 Common Config；Gateway 添加 `spring-cloud-starter-loadbalancer`。
- [x] 两个 `application.yml` 导入公共配置及服务对应的 Nacos Data ID；路由默认 URI 改为 `${AUTH_BASE_URL:lb://auth}`。
- [x] 新增 `docs/config.md` 并同步 README、Common、Auth、Gateway 接入说明。
- [x] 执行 `mvn -B -ntp -pl jaycong-auth,jaycong-gateway -am package -DskipTests`；检查产物、启动及可用外部服务，记录验证边界。不新增或修改测试代码。

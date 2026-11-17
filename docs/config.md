# Nacos 注册与配置中心

`jaycong-common-config` 集成 Nacos Discovery 和 Nacos Config，使用 Spring Cloud Alibaba 2025.0.0.0（Nacos Client 3.0.3），匹配项目 Spring Boot 3.5 / Spring Cloud 2025.0 系列。该模块不引入 MVC，也不包含业务 Bean，使用官方 Starter 自动配置。

## 接入方式

Auth、Gateway 已依赖该模块，启动时注册为 `auth`、`gateway`。Gateway 仅显式暴露 `/auth`、`/auth/**`，通过 `lb://auth` 查询注册实例并负载均衡；未启用自动暴露所有服务的 discovery locator。`AUTH_BASE_URL` 可覆盖为固定 HTTP 地址。

公共模块只提供 `jaycong-nacos.yml`，不在 JAR 中放置 `application.yml`。新增应用需引入该模块、设置唯一的 `spring.application.name`，并显式声明：

```yaml
spring:
  application:
    name: your-service
  config:
    import:
      - classpath:jaycong-nacos.yml
      - nacos:your-service.yml?refreshEnabled=true
```

根 POM 已通过 `jaycong-common` 聚合新模块；Common POM 注册子模块，独立 BOM 管理该模块及 Alibaba 版本。

## 连接参数

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| NACOS_SERVER_ADDR | localhost:8848 | Nacos 服务端地址 |
| NACOS_NAMESPACE | 空 | namespace ID，空表示 public；不要填写显示名称 |
| NACOS_GROUP | DEFAULT_GROUP | 注册发现及配置使用的 group |
| NACOS_USERNAME | 空 | 开启鉴权时填写用户名 |
| NACOS_PASSWORD | 空 | 开启鉴权时填写密码 |

仍可通过原生 `spring.cloud.nacos.discovery.*`、`spring.cloud.nacos.config.*` 分别覆盖两项能力。Gateway 和 Auth 的注册 namespace/group 必须一致。Nacos 需开放 HTTP 端口及客户端 gRPC 端口（默认 8848、9848）；使用容器或自定义端口时也需映射 gRPC 端口。

## 远程配置

### Auth / Gateway 配置拆分

两个模块的 `application.yml` 通过 `spring.config.import: classpath:bootstrap.yml` 显式加载本地 `bootstrap.yml`，后者再导入公共 `jaycong-nacos.yml` 和远程 `auth.yml` / `gateway.yml`。这里仍使用 Spring Boot Config Data，不引入传统 bootstrap Starter；`bootstrap.yml` 不会仅凭文件名自动加载。

- `bootstrap.yml`：服务名、公共 Nacos 连接配置引用、远程 Data ID；Gateway 还保留 `spring.main.web-application-type: reactive`。
- `application.yml`：端口、数据库、Redis、SQL 初始化、Sa-Token、网关路由和 HTTP 客户端、Actuator 等配置。

后续迁移时，将各模块 `application.yml` 中的业务配置发布到对应 Data ID，核对完整后删除本地对应项。本地必须保留 `spring.config.import: classpath:bootstrap.yml`，不要把该导入入口复制到 Nacos，避免循环导入。服务名和 Nacos 连接信息继续保留在本地；迁移前 application 继续提供原有默认值，迁移后需确保远程配置完整。

启动前在目标 namespace 和 group 中手工创建以下 YAML 配置：

| Data ID | 格式 | 最小内容示例 |
| --- | --- | --- |
| auth.yml | YAML | `jaycong.remote-config-marker: auth` |
| gateway.yml | YAML | `jaycong.remote-config-marker: gateway` |

上述标记仅用于确认配置加载，无业务含义。可在远程配置中按需设置数据库、Redis、网关路由等原生配置。Nacos 中配置通常优先于声明 import 的本地配置，命令行和环境变量仍可覆盖；不要把服务名和 Nacos 连接凭据放到只能连接成功后才能读取的配置中。

默认使用必需的 `nacos:` import，不配置 `optional:`，连接或读取异常会阻止正常启动；配置不存在或内容为空时的行为还取决于 Starter，不能依靠启动成功确认配置已发布，应在控制台和启动日志中核对。`refreshEnabled=true` 允许配置变更进入 Spring Environment，但具体 Bean 是否热更新取决于其绑定方式；端口等启动参数变更需要重启，路由变更也应验证刷新效果或重启。

本次本机验证中，Data ID 未发布时 Starter 记录空配置告警并继续启动、订阅对应 Data ID。应先发布配置再用于真实环境，不能把必需 import 理解为强制校验配置非空。

## 构建与手工验证

```powershell
mvn -B -ntp -pl jaycong-auth,jaycong-gateway -am package -DskipTests
java -jar jaycong-auth/target/jaycong-auth-0.1.0-SNAPSHOT.jar
# 另一个终端
java -jar jaycong-gateway/target/jaycong-gateway-0.1.0-SNAPSHOT.jar
Invoke-RestMethod http://localhost:8090/actuator/health
Invoke-RestMethod http://localhost:8090/auth/status
```

Auth 另需准备 MySQL 和 Redis，参见 [Auth](auth.md)。在 Nacos 服务列表确认两个服务与实例地址，在启动日志确认两个 Data ID 被加载。通过网关请求 Auth 验证发现路由；没有可用 Auth 实例时，负载均衡默认返回 503。仅健康接口 UP 不代表 Auth 或远程业务配置可用。

本次验证：上述 Maven 构建成功；以 18084、18090 临时端口启动 Auth、Gateway，日志确认两项服务注册成功且订阅各自 Data ID；网关健康检查、Auth 直连和经网关 `/auth/status` 均返回 200。检查网关产物含 Nacos 与 LoadBalancer，不含 `spring-webmvc`。本机未发布两个远程 Data ID，因此未验证非空远程配置读取和热刷新；未新增或修改测试代码。

## User / Auth 职责拆分后的配置

User 注册名为 user，加载 user.yml，数据库配置迁至 User 的 USER_DB_*。Auth 保留 Redis 和认证接口配置，通过 Nacos 发现 User，或使用 USER_SERVICE_URL 指定地址；Auth/User 配置同一 USER_SERVICE_TOKEN 和 AUTH_REDIS_*。Gateway 将全部 /admin/** 转发至 lb://admin，再由 Admin 调用 User/Goods；三端配置相同 ADMIN_SERVICE_TOKEN，已发布的 gateway.yml 中如覆盖 routes 也需要同步删除旧直连管理路由并新增 Admin 路由。详见 [User 使用说明](user.md)。

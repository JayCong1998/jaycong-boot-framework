# RPC 接入说明

`jaycong-common-rpc` 组合 `spring-cloud-starter-openfeign` 和
`spring-cloud-starter-loadbalancer`，同时提供 HC5 HTTP 客户端以保留 401 等响应体，版本沿用 BOM 管理。
goods、order、pay 三个业务模块已引用。该模块只提供依赖，不注册业务 Client，
不扩大组件扫描范围，不附加重试、认证头透传或异常转换策略。

```xml
<dependency>
    <groupId>io.github.jaycong</groupId>
    <artifactId>jaycong-common-rpc</artifactId>
</dependency>
```

## 启用调用

业务服务间调用的 Facade 契约、DTO 和 `@FeignClient` 接口统一放在 `jaycong-common-api`，
提供方在 `facade` 包实现契约，调用方引入 common-api 和 common-rpc，
通过 `@EnableFeignClients(clients = GoodsFeignClient.class)` 显式注册所需客户端。
Client 使用 Spring MVC 注解声明 HTTP 路径、请求参数和返回类型；业务模块不重复声明远程 DTO、路径或自行构建 RestClient。

`@FeignClient(name = "goods")` 不配置 `url` 时，LoadBalancer 按服务名选择实例；
需要业务接入 Nacos Discovery（可引用 `jaycong-common-config`）或配置静态实例。
RPC 模块本身不引入注册中心。pay 当前仅引用 RPC 依赖，尚未接入服务发现。
显式配置 `url` 时走固定地址，不通过服务发现负载均衡。

保留官方 `spring.cloud.openfeign.*` 和 `spring.cloud.loadbalancer.*` 配置项。
例如可在业务配置中按需设置默认超时：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000
            readTimeout: 5000
```

## Goods Facade

Common API 提供纯 Java 契约 `goods.facade.GoodsFacade` 和客户端 `goods.client.GoodsFeignClient`，
复用公开模型 `goods.model.GoodsView`。Goods 的 `facade.GoodsFacadeServiceImpl` 提供
`GET /internal/goods/{id}`，仅返回未删除且已上架的商品，价格单位为元；不存在或未上架返回 HTTP 404。
该只读入口与公开详情查询相同的资料，不要求用户登录，不返回管理字段；网关不公开内部路由。
公开 `GET /goods/{id}` 的 Result 响应保持原状，内部查询成功直接返回 GoodsView。

```java
import io.github.jaycong.api.goods.client.GoodsFeignClient;

@EnableFeignClients(clients = GoodsFeignClient.class)
```

Order 通过该客户端获取商品，`infrastructure.client.GoodsClient` 仅负责远程异常转换和订单成交快照适配。
商品不存在或未上架转换为 404；连接、超时、响应解码失败或非法商品数据转换为 503。
不添加自动重试，保留 OpenFeign 默认的 `Retryer.NEVER_RETRY`。

```yaml
jaycong:
  rpc:
    goods:
      url: ${GOODS_SERVICE_URL:} # 空值通过 Nacos 的 goods 服务名寻址
spring.cloud.openfeign.client.config.goodsRead:
  connectTimeout: 2000
  readTimeout: 3000
  loggerLevel: none
```

Order 兼容旧 `jaycong.order.goods-url` 和 `ORDER_GOODS_URL`，新部署优先使用 `jaycong.rpc.goods.url` 或 `GOODS_SERVICE_URL`。
未配置固定地址时改为服务发现，需确保 Nacos 存在可用的 goods 实例；本地直连可设置 `GOODS_SERVICE_URL=http://localhost:8085`。

## 构建

```powershell
mvn -B -ntp -pl :jaycong-goods,:jaycong-order,:jaycong-pay -am package -DskipTests
```

## User Facade

`jaycong-common-api` 同时提供纯 Java 契约 `user.facade.UserFacade`、DTO 和可复用客户端 `user.client.UserFeignClient`。API 模块依赖 OpenFeign Core 和 Spring Web 以声明客户端，不自动扫描或注册客户端，也不依赖 Auth/User 应用。User 的 `facade.UserFacadeServiceImpl` 实现纯 Java 契约。

调用方引入 common-api 和 common-rpc，显式启用 API 中的客户端：

```java
import io.github.jaycong.api.user.client.UserFeignClient;

@EnableFeignClients(clients = UserFeignClient.class)
```

Service 通过构造器注入 `UserFeignClient` 即可调用 `register`、`authenticate`、`findEnabledUser`，不需要在调用方重新声明接口。User 提供方不需要启用 Feign 客户端。

```yaml
jaycong:
  rpc:
    user:
      url: ${USER_SERVICE_URL:} # 空值通过 Nacos 的 user 服务名寻址
      token: ${USER_SERVICE_TOKEN:}
spring.cloud.openfeign.client.config.authUser:
  connectTimeout: 2000
  readTimeout: 5000
  loggerLevel: none
```

`authUser` 是为兼容既有配置保留的客户端 contextId，可供所有调用方使用。旧 `jaycong.auth.user-service-url`、`jaycong.auth.user-service-token` 仍作为兼容回退，新增调用方使用 `jaycong.rpc.user.*`。客户端专用配置位于 API 的 `user.config.UserFeignConfiguration`，仅应用于此客户端：注入内部调用凭证、关闭 Feign 日志、不自动重试。不要把该配置类注册成全局配置。

Auth 的 `UserService` 保留远程响应和错误转换，不重复声明 Feign 方法。两端配置相同的 `USER_SERVICE_TOKEN`。common-rpc 提供 HC5 客户端，调用方无需单独引入。详见 [User 使用说明](user.md)。

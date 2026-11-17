# 订单服务

`jaycong-business/jaycong-order` 为独立应用，默认端口 8086，Nacos 服务名 `order`，配置 Data ID `order.yml`。先手工执行模块中的 `src/main/resources/db/order-mysql.sql`，准备 MySQL、Redis、Nacos、Auth 和 Goods。

数据库使用 `ORDER_DB_URL`、`ORDER_DB_USERNAME`、`ORDER_DB_PASSWORD`，默认与 Goods 相同。端口使用 `ORDER_PORT`。Redis 使用与 Auth 相同的 `AUTH_REDIS_*` 环境变量，必须指向同一个 Redis 实例和数据库；`sa-token` 配置及 loginType `jaycong-auth` 必须与 Auth 一致。订单服务校验 Redis 登录态，不直接读取用户表。

商品调用统一使用 `jaycong-common-api` 的 `GoodsFeignClient` → Goods 的 `GoodsFacadeServiceImpl`（`GET /internal/goods/{id}`），不在订单服务构建 RestClient 或重复声明远程 DTO。默认通过 Nacos 的 `goods` 服务名负载均衡；固定地址使用 `jaycong.rpc.goods.url` 或 `GOODS_SERVICE_URL`，兼容旧 `jaycong.order.goods-url` 和 `ORDER_GOODS_URL`。本地直连可设置 `GOODS_SERVICE_URL=http://localhost:8085`。连接超时 2 秒，读取超时 3 秒，不自动重试；超时使用 `spring.cloud.openfeign.client.config.goodsRead.*` 配置。商品读取在数据库写入前完成；商品随后变价或下架不改变订单快照，不提供跨服务强一致性。

```powershell
mvn -B -ntp -pl jaycong-business/jaycong-order -am package -DskipTests
java -jar jaycong-business/jaycong-order/target/jaycong-order-0.1.0-SNAPSHOT.jar
```

## 下单

`POST /orders`，请求头 `Authorization: Bearer <Auth 登录返回的 token>`。一次只能购买一个商品，不接收数量、价格或用户 ID。

```json
{"goodsId":"1001","remark":"订单备注"}
```

goodsId 必填且为正 BIGINT；remark 可省略，最多 500 字符。成功 HTTP 200，统一 Result 的 data 示例：

```json
{"id":"1980000000000000001","orderNo":"JC1980000000000000001","goodsId":"1001","goodsCode":"DEMO-001","goodsName":"数字商品","goodsCoverUrl":"","totalAmount":1990,"status":0,"remark":"订单备注","expireTime":"2026-11-12T12:30:00"}
```

金额为人民币分，ID 为字符串。支付截止时间为下单时起 30 分钟，时间采用服务 JVM 本地时间，部署时统一使用 Asia/Shanghai。未登录返回 401，参数非法返回 400，商品已删除、下架或不存在返回 404，商品 HTTP 故障或非法价格返回 503。所有失败均使用统一 Result。

通过网关访问 `http://localhost:8090/orders`；若 Nacos `gateway.yml` 覆盖 routes，须增加 `Path=/orders,/orders/**` 指向 `lb://order`。

当前不包含支付、交付、库存和自动关单。相同请求重复提交会产生多笔订单，客户端不要自动重试不确定是否成功的下单请求。价格必须大于零；免费商品流程不在本次范围。

## DDD 分包

严格参照 `NFTurbo_Server/nft-turbo-business/nft-turbo-order` 的实际结构，与 Goods 采用相同模式：入口直接调用领域服务，领域服务直接使用 Mapper，实体同时维护业务行为和表映射。

```text
io.github.jaycong.order
├── OrderApplication
├── OrderException
├── controller
│   ├── OrderController
│   └── CreateOrderRequest
├── domain
│   ├── entity
│   │   ├── Order、OrderStatus、GoodsSnapshot
│   │   └── convertor/OrderConvertor
│   └── service/OrderManageService
└── infrastructure
    ├── mapper/OrderMapper
    ├── client/GoodsClient
    └── config/OrderConfiguration
```

| 包 | 具体职责 |
| --- | --- |
| controller | HTTP 路由、Web 参数校验、可信登录身份和响应转换 |
| domain.entity | 订单创建、成交快照校验、待支付状态、30 分钟支付期限及表映射 |
| domain.entity.convertor | 将订单实体转换为 Common API 的 OrderResponse，只输出公开字段 |
| domain.service | OrderManageService 获取商品快照、调用实体创建行为并直接持久化 |
| infrastructure.mapper | MyBatis-Plus 订单表访问，直接映射 Order |
| infrastructure.client | 通过 Common API 的 GoodsFeignClient 查询商品，完成外部错误转换及元到整数分的精确转换 |
| infrastructure.config | 显式启用 Common API 的 GoodsFeignClient，地址及超时使用配置文件中的 Feign 配置 |

下单链路：Controller 读取 AuthContext 身份 → OrderManageService 调用 GoodsClient → Common API 的 GoodsFeignClient 调用 GoodsFacade → 返回并转换 GoodsSnapshot → Order.create 创建待支付订单 → OrderMapper 插入 → OrderConvertor 转换 OrderResponse。

Order 继承 BaseEntity，直接映射 trade_order，保留审计填充、逻辑删除和乐观锁配置；业务字段不开放公共 setter。GoodsSnapshot 为不可变成交快照，金额为正整数分。当前仅创建订单，在商品远程读取完成后执行单条原子插入，不在 HTTP 调用期间开启额外的数据库事务。

对外响应位于 Common API 的 `io.github.jaycong.api.order.response.OrderResponse`，HTTP 字段与原接口一致，不直接返回实体。OrderException 位于模块根包并继承 BusinessException，由 common-web 的 GlobalExceptionHandler 统一处理。

当前没有独立订单读取、RPC、定时任务或消息消费，不创建空的 ReadService、facade、job 或 listener；不设置 application、command、port、repository 和独立 OrderDO，也不增加仅转发参数的中间类型。

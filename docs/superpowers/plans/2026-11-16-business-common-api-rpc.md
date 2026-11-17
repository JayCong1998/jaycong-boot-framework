# Business 远程调用统一 Common API

用户要求：业务服务间远程调用通过 Common API 契约及客户端，不在业务模块自行使用 RestClient。

检查结果：Order → Goods 使用自建 RestClient 和重复商品 DTO；Goods → User 已显式启用 Common API 的 UserFeignClient；User、Pay 没有自行编写的出站 HTTP 调用。

实施方案：沿用 User 的纯 Java Facade + Feign 客户端模式，在 Common API 中增加 GoodsFacade、GoodsFeignClient，复用 GoodsView。Goods 的轻量 Facade 调用 GoodsReadService，返回公开资料；Order 的 GoodsClient 仅负责订单快照转换及远程异常映射。

1. 增加 Common API 商品查询契约和 `/internal/goods/{id}` Feign 声明，Goods 提供 Facade 实现。公开 `/goods/{id}` 接口保持原响应。
2. Order 显式启用 GoodsFeignClient，删除 RestClient 装配和私有重复 DTO。保留商品不存在的 404、远程故障和异常金额的 503，以及整数分精确转换。
3. 默认使用 Nacos 的 goods 服务名；固定 URL 使用 `jaycong.rpc.goods.url` 或 `GOODS_SERVICE_URL`，兼容 `jaycong.order.goods-url` 和 `ORDER_GOODS_URL`。连接/读取超时沿用 2/3 秒，不启用自动重试。
4. 更新 RPC、Order、Goods 当前使用说明；不改历史方案，不新增或修改测试代码，不触碰其他工作区改动。
5. Maven 构建所有 business 模块及依赖，并编译 Auth/Admin 检查公共 API 兼容性；条件允许时启动实际 Goods/Order 产物进行只读 HTTP 验证。如实记录未完成的外部服务联调。

## 实施及验证结果

- 已完成上述契约、提供端、调用端及当前使用说明调整。静态搜索 business 主源码，已无 RestClient、RestTemplate、WebClient、JDK HTTP 客户端或自行声明的 Feign 接口；Goods → User 继续使用 Common API 原客户端。
- 执行 `mvn -B -ntp -pl jaycong-business,jaycong-auth,jaycong-admin -am clean package -DskipTests`，13 个模块构建成功。此命令只选中了 Business 聚合父模块，未包含其业务子模块，因此另行显式选择全部子模块构建。
- 执行 `mvn -B -ntp -pl :jaycong-goods,:jaycong-order,:jaycong-user,:jaycong-pay -am clean package -DskipTests`，17 个模块构建成功。未新增、修改或执行自动测试。
- 临时启动实际 Goods 打包产物（18085）；通过 JShell 从 Order 实际打包产物加载应用上下文（18086），直接调用 Spring 装配的 GoodsFeignClient / GoodsClient。使用进程参数跳过本地配置文件及 Nacos，提供现有 MySQL 的连接参数，没有创建测试专用配置或修改仓库运行参数。
- HTTP 手工验证：真实已上架商品 `202611110006` 的内部查询返回 200，公开详情仍返回原 Result 包装；两个接口的商品资料、字符串 ID 及元金额一致。不存在商品返回 404；匿名下单返回 401。
- 固定地址调用返回 GoodsView，订单适配得到 `priceInCents=529900`，对应接口的 `price=5299.00`；不存在商品经 Feign 转换为 `GOODS_NOT_PURCHASABLE / 404`。
- 未配置固定 URL 时，使用命令行静态实例 `spring.cloud.discovery.client.simple.instances.goods[0].uri=http://localhost:18085` 验证 goods 服务名及 LoadBalancer 调用成功。停止临时 Goods 后，同一 Order 适配调用转换为 `GOODS_UNAVAILABLE / 503`。
- 独立只读代码审查未发现需要修改的问题，`git diff --check` 通过。临时应用和 JShell 已停止，18085/18086 无残留监听；验证未写入数据库。
- 本机 Redis、Nacos 未运行，未验证真实 Nacos 注册发现和携带有效登录会话的完整下单写入。部署需先提供新版 Goods 内部查询接口，再切换 Order 调用。

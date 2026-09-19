# A 调 B：订单与库存示例

本示例用于后续分布式事务实验。A 先写订单，再通过 HTTP 调用 B 扣库存；两个服务分别使用自己的数据库和本地事务。

| 服务 | 模块 | 默认地址 |
| --- | --- | --- |
| A：订单 | jaycong-order-service | http://localhost:8081 |
| B：库存 | jaycong-stock-service | http://localhost:8082 |

技术栈沿用工程的 Java 21、Spring Boot 和 MyBatis-Plus；调用客户端为 Spring RestClient。没有引入 Seata、注册中心、网关或消息队列。原 jaycong-example 仍独立运行在 8080。

## 构建与启动

在工程根目录执行：

```powershell
mvn -B -ntp verify
```

打开两个终端，先启动 B：

```powershell
java -jar jaycong-stock-service/target/jaycong-stock-service-0.1.0-SNAPSHOT-exec.jar
```

再启动 A：

```powershell
java -jar jaycong-order-service/target/jaycong-order-service-0.1.0-SNAPSHOT-exec.jar
```

运行时请选带 `-exec.jar` 后缀的文件。普通 JAR 保留给测试依赖使用；A 对 B 的 Java 依赖仅存在于 test scope，运行时通过 HTTP 调用。

默认 H2 内存数据库互相独立，无需安装数据库。B 启动时初始化商品 1、库存 100；A 初始没有订单。重启各自进程会重置其 H2 数据。需要重跑完整演示时，请同时重启两个服务。

## 正常下单

以下命令在第三个 PowerShell 终端执行：

```powershell
Invoke-RestMethod http://localhost:8082/stocks/1 | ConvertTo-Json -Depth 5

$body = @{ productId = 1; quantity = 2 } | ConvertTo-Json
Invoke-RestMethod http://localhost:8081/orders -Method Post -ContentType 'application/json' -Body $body | ConvertTo-Json -Depth 5

Invoke-RestMethod http://localhost:8081/orders | ConvertTo-Json -Depth 5
Invoke-RestMethod http://localhost:8082/stocks/1 | ConvertTo-Json -Depth 5
```

预期：创建一条订单，库存从 100 变为 98。响应统一为 `{ success, code, message, data }`，成功时 code 为 0。可通过 `GET /orders/{id}` 查询返回的订单 ID。

## 演示跨服务不一致

接着执行一次注入故障的下单（使用 curl.exe 文件输入兼容 Windows PowerShell 的 JSON 参数转义）：

```powershell
$body = @{ productId = 1; quantity = 2; failAfterStockDeduction = $true } | ConvertTo-Json -Compress
$body | curl.exe -i http://localhost:8081/orders -H 'Content-Type: application/json' --data-binary '@-'

Invoke-RestMethod http://localhost:8081/orders | ConvertTo-Json -Depth 5
Invoke-RestMethod http://localhost:8082/stocks/1 | ConvertTo-Json -Depth 5
```

预期 HTTP 422、业务码 21002：B 的库存从 98 降至 96，A 的订单数量仍为 1。若在全新启动后直接运行故障演示，则订单数为 0，库存为 98。

调用顺序：

1. A 开启本地事务，插入订单。
2. A 调用 B，B 扣库存并提交自己的事务。
3. B 返回成功，A 根据故障开关抛出运行时业务异常。
4. A 回滚自己的订单事务；B 已提交的扣减不受影响。

`failAfterStockDeduction` 仅用于本地示例。此阶段没有补偿、全局回滚或幂等处理；重复发送请求可能多次扣库存。超时也可能发生在 B 已提交之后，不能据此推断库存未变化。

## 库存不足与服务不可用

```powershell
$body = @{ productId = 1; quantity = 1000 } | ConvertTo-Json -Compress
$body | curl.exe -i http://localhost:8081/orders -H 'Content-Type: application/json' --data-binary '@-'
```

预期 HTTP 422、业务码 21001，订单数和库存均不变。关闭 B 后再次正常下单，A 同样返回调用失败且不会提交订单。

B 在一条条件 UPDATE 中判断剩余数量并扣减，受影响行数为 0 时拒绝扣减，避免并发超卖。请求的商品 ID 和数量必须为正数，不允许缺失。

## 接口

| 服务 | 方法与路径 | 请求 / 说明 |
| --- | --- | --- |
| A | POST /orders | productId、quantity、可选 failAfterStockDeduction（默认 false） |
| A | GET /orders | 按 ID 升序的订单列表，供小规模演示 |
| A | GET /orders/{id} | 查询订单，不存在时 404 |
| B | GET /stocks/{productId} | 查询 productId、availableQuantity，不存在时 404 |
| B | POST /stocks/deduct | productId、quantity；库存不足返回 422 / 22001 |

A 将 B 的失败转换为 422 / 21001；参数校验失败返回 400。A 会同时检查 B 的 HTTP 状态及响应的 success/code，空响应、格式错误、业务失败都不会提交订单。

## 调用配置

在启动 A 的终端中可设置：

```powershell
$env:STOCK_BASE_URL = 'http://localhost:8082'
$env:STOCK_CONNECT_TIMEOUT = '2s'
$env:STOCK_READ_TIMEOUT = '5s'
```

默认连接超时 2 秒、读取超时 5 秒，不自动重试。也可通过 `--stock.base-url=...`、`--stock.connect-timeout=...`、`--stock.read-timeout=...` 覆盖。端口通过 `--server.port=...` 配置；修改 B 的端口时同时调整 A 的目标地址。

## 切换 MySQL

准备两个独立数据库 `jaycong_order` 和 `jaycong_stock`，使用各自的连接执行脚本：

- A：`jaycong-order-service/src/main/resources/db/order-mysql.sql`
- B：`jaycong-stock-service/src/main/resources/db/stock-mysql.sql`

表使用 InnoDB。脚本不删除表或重置已有库存；mysql profile 不自动执行 SQL。

在 B 的终端设置连接并启动：

```powershell
$env:STOCK_DB_URL = 'jdbc:mysql://localhost:3306/jaycong_stock'
$env:STOCK_DB_USERNAME = 'your_user'
$env:STOCK_DB_PASSWORD = 'your_password'
java -jar jaycong-stock-service/target/jaycong-stock-service-0.1.0-SNAPSHOT-exec.jar --spring.profiles.active=mysql
```

在 A 的终端设置连接并启动：

```powershell
$env:ORDER_DB_URL = 'jdbc:mysql://localhost:3306/jaycong_order'
$env:ORDER_DB_USERNAME = 'your_user'
$env:ORDER_DB_PASSWORD = 'your_password'
java -jar jaycong-order-service/target/jaycong-order-service-0.1.0-SNAPSHOT-exec.jar --spring.profiles.active=mysql
```

两库可以位于同一个 MySQL 实例，但应分别连接，不能共用业务表。MySQL 数据持久保存，重启进程不会重置数据。

## 自动化验证

```powershell
mvn -B -ntp -pl jaycong-order-service -am test
```

测试涵盖真实 HTTP 的正常调用、B 提交后 A 回滚、库存不足、商品不存在、非法请求、并发扣减，以及异常下游响应和读取超时导致 A 回滚。测试使用独立 H2 数据和随机端口；MySQL 需要另行准备环境进行联调。

# Order Stock Implementation Plan

> Execution: superpowers:executing-plans，在当前会话执行；用户已要求实现。

**Goal:** 提供可直接运行的 A 下单调用 B 扣库存示例。
**Architecture:** 两个 Spring Boot 应用、两个数据库、HTTP RestClient、本地事务。
**Tech Stack:** Java 21、现有 Spring Boot BOM、MyBatis-Plus、H2/MySQL、JUnit。
**Spec:** ../specs/2026-09-19-order-stock-design.md

## 约束与检查重点

- 不引入分布式事务、中间件或自动重试，不修改原 Example 行为。
- 请求字段缺失或为负数时必须拒绝；库存条件更新防止负库存。
- B 返回 HTTP 错误、空体、错误业务状态或超时时，A 必须回滚。
- 真实 HTTP 联调验证 B 成功后 A 故障时只回滚订单。
- H2 自动初始化；MySQL 手动建表，不清空已有数据。

## Task 1：库存服务

- [x] 新增 jaycong-stock-service/pom.xml、启动类和配置，并加入根聚合；应用 JAR 使用 exec classifier，普通 JAR 可作为测试依赖。
- [x] 新增 src/test/java/io/github/jaycong/stock/StockHttpTest.java，通过 HTTP 断言库存查询、扣减、数量校验、库存不足与并发行为。先运行确认缺少接口导致失败。
- [x] 新增 Stock、StockMapper、StockService、StockController、DeductRequest、StockErrorCode；mapper 使用 UPDATE demo_stock SET available_quantity = available_quantity - #{quantity} WHERE product_id = #{productId} AND available_quantity >= #{quantity}。
- [x] 执行 mvn -B -ntp -pl jaycong-stock-service -am test 并验证通过。

## Task 2：订单服务与跨服务联调

- [x] 新增 jaycong-order-service/pom.xml、启动类、配置，测试范围依赖库存模块。
- [x] 新增 OrderFlowTest，随机端口启动两个应用、独立 H2 数据源。通过 POST /orders 断言订单与库存状态，覆盖正常、库存不足、商品不存在、故障开关、非法参数与查询；先确认缺少接口导致失败。
- [x] 新增 Order、OrderMapper、OrderService、OrderController、CreateOrderRequest、OrderErrorCode、StockClient 与配置。OrderService.create 在 @Transactional 内插单、调用扣减，再按请求注入失败。
- [x] 新增 OrderDownstreamTest，HTTP 测试服务器提供空体、业务失败、非成功状态和延迟响应，验证异常与超时；先确认未实现时失败。
- [x] 执行 mvn -B -ntp -pl jaycong-order-service -am test。

## Task 3：交付与整体验证

- [x] 添加各模块 MySQL profile、建表脚本、docs/order-stock.md，更新 README。
- [x] 执行 mvn -B -ntp verify；独立启动两个 exec JAR，使用 HTTP 演示成功与故障场景。
- [x] 复核事务边界、接口校验、错误转换及文档命令。报告真实验证结果与 MySQL 未联调限制。

## 执行记录

- 当前目录没有 Git 元数据，直接在用户工作目录实现，不创建 worktree 或 Git 提交。
- 两模块共享接口为 HTTP JSON；订单模块对库存模块的 Java 依赖仅用于集成测试。
- 库存 4 项、订单流程 5 项、下游错误 9 项测试均先观察到接口缺失导致的失败，再实现并通过。
- 下游错误测试采用完整订单 HTTP 入口与真实本地 HTTP 测试服务器，直接验证数据库回滚，替代只测试 StockClient 的单元测试。
- 全工程 mvn -B -ntp verify 通过：55 项测试，0 失败、0 错误、0 跳过；新增测试 18 项。
- 独立 exec JAR 验证通过：正常下单订单数 1、库存 98；注入故障后订单数仍为 1、库存 96；库存不足不改变数据；关闭 B 后 A 返回 21001 且订单数仍为 1。临时启动的进程已关闭。
- 独立只读代码审查未发现重要正确性问题。MySQL 脚本和配置仅完成静态检查，未做真实数据库联调。


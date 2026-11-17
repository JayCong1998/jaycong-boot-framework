# 单商品下单实现计划

**Goal:** 实现已登录用户单商品下单。

**Architecture:** 商品服务公开可购买商品详情；订单服务使用有超时的 HTTP 客户端读取快照，以 Sa-Token 校验会话，通过 MyBatis-Plus 写入 trade_order。

**Tech Stack:** Java 21、Spring Boot、RestClient、MyBatis-Plus、Sa-Token、Redis、Nacos。

- [x] 商品：在 GoodsService 和 GoodsController 增加 GET /goods/{id}，仅返回未删除且上架商品；不存在返回 404。
- [x] 订单：实现请求校验、商品 HTTP 客户端、会话配置、实体、Mapper、Service、Controller 和 401/404/503 异常响应。请求不提供数量、价格和用户 ID。
- [x] 配置：复用商品服务 POM 和配置结构，增加 Sa-Token 依赖与 Auth 会话配置；默认端口 8086；网关增加 /orders 路由。
- [x] 文档：保存用户提供的 trade_order DDL，记录启动、请求示例、共享 Redis 要求和非幂等边界。
- [x] 验证：执行 mvn -B -ntp -pl jaycong-business/jaycong-order,jaycong-business/jaycong-goods,jaycong-gateway -am package -DskipTests；检查差异，不新增测试。

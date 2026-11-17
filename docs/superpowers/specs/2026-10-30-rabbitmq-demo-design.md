# RabbitMQ Demo 设计

## 目标

新增一个独立、可运行的 `jaycong-rabbitmq-demo` Spring Boot 模块，直接使用 Spring AMQP 原生 API，分别演示 RabbitMQ 官方教程中的简单队列、工作队列、Fanout、Direct、Topic、RPC 和发布者确认。

## 结构

- 单应用运行在 8083 端口，各模式按包隔离。
- 每种模式拥有独立队列、交换机、绑定和 HTTP 入口。
- 队列与交换机统一使用 `jaycong.demo.*` 前缀，避免影响已有资源。
- 默认连接本机 `localhost:5672`，凭据为 `admin/admin`，允许通过 Spring Boot 标准属性覆盖。
- 不提供自定义消息框架、统一 Publisher 或自定义注解，示例直接展示 `RabbitTemplate`、`@RabbitListener` 和 AMQP 声明 API。

## 行为

- 简单队列：一个生产者对应一个消费者。
- 工作队列：两个消费者竞争同一队列，使用手动 ACK。
- Fanout：一条消息广播到两个队列。
- Direct：按精确 routing key 路由。
- Topic：使用 `*` 与 `#` 通配符路由。
- RPC：HTTP 请求通过 RabbitMQ 请求/响应机制获得计算结果，并设置超时。
- 发布者确认：分别演示单条、批量和异步确认，同时记录不可路由消息的 return 回调。

## 验证与文档

按用户要求不新增自动化测试。使用 Maven 编译、应用启动和本机 RabbitMQ 的实际 HTTP 调用进行验证，并在模块 README 中列出每个接口、调用示例和预期现象。

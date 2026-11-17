# Order 轻量 DDD 分包

用户已确认先在 Order 模块落地此前讨论的四层方案。保留单一 Maven 模块与现有 POST /orders 契约。

- interfaces.rest：请求/响应、Sa-Token 身份读取、Controller、HTTP 异常映射。
- application：CreateOrderCommand、OrderApplicationService、GoodsGateway 端口。负责获取商品快照并编排订单创建和保存。
- domain：Order 聚合根、GoodsSnapshot 值对象、OrderStatus、OrderRepository、OrderException。纯 Java，不引用 Spring、MyBatis、Sa-Token 或 HTTP。
- infrastructure：OrderDO 与 MyBatis Mapper/仓储实现、HTTP 商品适配器、Spring 配置。

Order.create 维护单商品、正金额、待支付状态和 30 分钟期限；无公共 setter。OrderDO 继续继承公共 BaseEntity，仓储设置业务操作人并由自动填充维护其余审计字段。只实现新订单写入，不扩展支付或更新流程。商品适配器将外部元价格精确转换为领域整数分，领域异常由 REST 层转换为原有 404/503，认证和参数错误保持 401/400。

不调整 Goods、Auth、数据库及配置契约，不新增或修改测试。采用 clean package -DskipTests 排除旧类残留，启动及手工 HTTP 检查验证分包后的装配。

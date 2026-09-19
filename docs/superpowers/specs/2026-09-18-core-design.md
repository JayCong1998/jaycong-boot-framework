# Core 基础契约

范围沿用已确认的 Core 方案：Result、ErrorCode、CommonErrorCode、BusinessException、PageRequest、PageResult。Java 21，包名 io.github.jaycong.core，零运行时依赖，JUnit 仅用于测试。

采用字符串错误码，成功为 "0"，通用失败使用可读标识 INVALID_ARGUMENT、NOT_FOUND、SYSTEM_ERROR。业务模块实现 ErrorCode 扩展领域错误码；错误码不绑定 HTTP 状态。

Result<T> 使用不可变 record，字段 code/message/data。success 工厂支持空数据；failure 工厂接受 ErrorCode 或自定义错误消息，禁止成功码。code/message 不允许 null 或空白。通用异常不自动向客户端暴露 cause。

BusinessException 继承 RuntimeException，保存错误码字符串，支持默认消息、自定义消息和 cause，拒绝成功码。Web 层后续决定响应映射。

PageRequest 使用不可变 record，page 为 int，从 1 开始，size 为 int，默认 20，范围 1–1000；offset 使用 long 计算。非法输入抛出 IllegalArgumentException，不静默修正。

PageResult<T> 使用不可变 record，字段 page/size/total/records，total 为非负 long，列表防御性复制，不接受 null 列表或元素。records 数量不得超过 size 或 total；允许超出末页时返回空列表。totalPages 用除法和余数计算以避免溢出。不依赖 MyBatis 类型。

验证涵盖业务错误扩展、异常原因保留、非法输入、列表隔离、空页和 long 边界。后续 Web 层验证 JSON 和请求绑定，当前不引入序列化依赖。

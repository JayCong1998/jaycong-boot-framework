# Core 使用说明

`jaycong-core` 使用 Java 21，无运行时第三方依赖。包前缀为 `io.github.jaycong.core`。

## 返回对象

```java
import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.model.Result;

Result<String> success = Result.success("hello");
Result<Void> empty = Result.success();
Result<Void> failure = Result.failure(CommonErrorCode.INVALID_ARGUMENT, "用户名不能为空");
Integer code = success.code();
boolean successful = success.isSuccess();
```

Result 是不可变类，包含 `boolean success`、`Integer code`、`String message` 和 `T data`。
`success` 自动根据 `code == 0` 推导，不能独立指定。成功码为整数 `0`。
保留 `code()`、`message()`、`data()`，并提供 `isSuccess()`、`getCode()`、`getMessage()`、`getData()`。
失败工厂不接受成功码；code/message 不接受 null，message 不接受空白字符串。data 可以为 null。
容器保留值相等语义，但不对 data 做深拷贝。

## 业务错误

```java
import io.github.jaycong.core.error.ErrorCode;
import io.github.jaycong.core.exception.BusinessException;

enum OrderError implements ErrorCode {
    CLOSED;

    public Integer getCode() { return 10002; }
    public String getMessage() { return "订单已关闭"; }
}

// 在业务方法中抛出：
// throw new BusinessException(OrderError.CLOSED);
// throw new BusinessException(OrderError.CLOSED, "当前订单不允许修改");
// throw new BusinessException(OrderError.CLOSED, "当前订单不允许修改", cause);
```

通用错误码为 SUCCESS(0)、INVALID_ARGUMENT(400)、NOT_FOUND(404)、SYSTEM_ERROR(500)。
业务模块自行定义不重复的整数领域错误码，如 Example 的 NAME_RESERVED(10001)。
业务错误码与 HTTP 状态独立，例如 NAME_RESERVED 响应中的 code 为 10001，HTTP 状态为 422。
BusinessException 通过 `getCode()`、`getMessage()` 和 `getCause()` 访问。
message 应可向调用方展示；cause 用于内部诊断，Web 层负责异常到响应的映射。

## 分页

```java
import java.util.List;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;

PageRequest defaults = new PageRequest(); // 第 1 页，每页 20 条
PageRequest request = new PageRequest(2, 20);
long offset = request.offset(); // 20
PageResult<String> page = new PageResult<>(21L, List.of("last"));
long total = page.total();
List<String> rows = page.rows();
```

- PageRequest 的 page 为 int，从 1 开始；size 范围为 1–1000，非法输入抛出 IllegalArgumentException。
- 分页结果仅包含 `long total` 和 `List<T> rows`，不携带请求页码或其他分页派生字段。
- rows 进行浅层防御性复制，不接受 null 列表或 null 元素，返回列表不可修改。
- total 不得为负，rows 数量不得超过 total；total 大于零时也可返回空列表（例如请求超出末页）。
- 模型不依赖 MyBatis；响应序列化由 Web 模块测试验证。

分页响应示例：

```json
{"success":true,"code":0,"message":"成功","data":{"total":21,"rows":["last"]}}
```

## 从旧契约迁移

- 自定义 ErrorCode 的 `getCode()` 返回类型改为 `Integer`，字符串错误码改为业务定义的整数。
- `new PageResult<>(page, size, total, records)` 改为 `new PageResult<>(total, rows)`；`records()` 改为 `rows()`。
- 客户端按数字读取 code，并使用新增的 success 布尔字段；HTTP 状态约定不变。

## 测试

在项目根目录运行 `mvn -B -ntp -pl jaycong-core -am test`。

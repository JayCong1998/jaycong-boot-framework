package io.github.jaycong.core.model;

import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.error.ErrorCode;

import java.util.Objects;

/** 统一响应。容器不可变，data 的可变性由调用方管理。 */
public final class Result<T> {
    private final boolean success;
    private final Integer code;
    private final String message;
    private final T data;

    /** success 始终由 code 推导，不允许调用方传入不一致的成功标记。 */
    public Result(Integer code, String message, T data) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        this.success = CommonErrorCode.SUCCESS.getCode().equals(code);
        this.data = data;
    }

    public boolean isSuccess() { return success; }

    public Integer getCode() { return code; }

    public String getMessage() { return message; }

    public T getData() { return data; }

    // 保留原 record 风格的访问方法，JavaBean getter 用于 JSON 序列化。
    public Integer code() { return code; }

    public String message() { return message; }

    public T data() { return data; }

    public static <T> Result<T> success(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode");
        return failure(errorCode, errorCode.getMessage());
    }

    /** message 应是可向调用方展示的消息，不能直接使用内部异常详情。 */
    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode");
        Integer code = errorCode.getCode();
        if (CommonErrorCode.SUCCESS.getCode().equals(code)) {
            throw new IllegalArgumentException("failure must not use the success code");
        }
        return new Result<>(code, message, null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Result<?> result)) {
            return false;
        }
        return code.equals(result.code) && message.equals(result.message) && Objects.equals(data, result.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, data);
    }

    @Override
    public String toString() {
        return "Result[success=" + success + ", code=" + code + ", message=" + message + ", data=" + data + "]";
    }
}

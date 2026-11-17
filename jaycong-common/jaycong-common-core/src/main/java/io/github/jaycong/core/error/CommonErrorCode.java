package io.github.jaycong.core.error;

/** 框架通用错误码，领域错误由业务模块定义。 */
public enum CommonErrorCode implements ErrorCode {
    SUCCESS(0, "成功"),
    INVALID_ARGUMENT(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统异常");

    private final Integer code;
    private final String message;

    CommonErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

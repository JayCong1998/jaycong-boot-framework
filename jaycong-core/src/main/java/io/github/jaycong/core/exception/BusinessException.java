package io.github.jaycong.core.exception;

import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.error.ErrorCode;

import java.io.Serial;
import java.util.Objects;

/** 可预期的业务失败；cause 保留供日志诊断，由 Web 层决定响应映射。 */
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Objects.requireNonNull(errorCode, "errorCode").getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message"), cause);
        Objects.requireNonNull(errorCode, "errorCode");
        this.code = Objects.requireNonNull(errorCode.getCode(), "code");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (CommonErrorCode.SUCCESS.getCode().equals(code)) {
            throw new IllegalArgumentException("business exception must not use the success code");
        }
    }

    public Integer getCode() {
        return code;
    }
}

package io.github.jaycong.core.exception;

import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.error.ErrorCode;

import java.io.Serial;
import java.util.Objects;

/** 可预期的业务失败；消息可公开，cause 仅供日志诊断，类型不依赖 Web 框架。 */
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;
    private final int status;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Objects.requireNonNull(errorCode, "errorCode").getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        this(Objects.requireNonNull(errorCode, "errorCode").getCode(), message, cause, 422);
    }

    /**
     * 创建错误码与 HTTP 状态相同的业务失败。
     * @param status 400 至 599 的失败状态
     * @param message 可以向客户端公开的消息
     * @throws IllegalArgumentException 状态无效或消息为空白
     */
    public BusinessException(int status, String message) {
        this(status, message, null);
    }

    /**
     * 创建带诊断原因的业务失败，cause 不返回客户端。
     * @param status 400 至 599 的失败状态，同时作为错误码
     * @param message 可以向客户端公开的消息
     * @param cause 底层异常，可为 null
     * @throws IllegalArgumentException 状态无效或消息为空白
     */
    public BusinessException(int status, String message, Throwable cause) {
        this(status, message, cause, status);
    }

    private BusinessException(Integer code, String message, Throwable cause, int status) {
        super(Objects.requireNonNull(message, "message"), cause);
        this.code = Objects.requireNonNull(code, "code");
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("business exception status must be between 400 and 599");
        }
        this.status = status;
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

    /** @return HTTP 失败状态；使用 ErrorCode 构造时默认为 422 */
    public int getStatus() {
        return status;
    }
}

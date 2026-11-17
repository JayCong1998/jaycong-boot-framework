package io.github.jaycong.satoken.exception;

import io.github.jaycong.core.exception.BusinessException;

/** 会话存储异常不能降级成匿名身份或系统操作人。 */
public class AuthContextUnavailableException extends BusinessException {
    public AuthContextUnavailableException(Throwable cause) {
        super(503, "登录会话服务暂不可用，请稍后重试", cause);
    }
}

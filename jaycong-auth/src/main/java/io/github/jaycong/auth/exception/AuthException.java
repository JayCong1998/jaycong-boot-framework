package io.github.jaycong.auth.exception;

import io.github.jaycong.core.exception.BusinessException;

/** 可向客户端公开的认证失败，不携带底层异常和凭证。 */
public class AuthException extends BusinessException {
    public AuthException(int status, String message) {
        super(status, message);
    }
}

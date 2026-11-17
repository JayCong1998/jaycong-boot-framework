package io.github.jaycong.user;

import io.github.jaycong.core.exception.BusinessException;

/** 可向调用方公开的用户业务失败，不携带底层异常和凭证。 */
public class UserException extends BusinessException {
    public UserException(int status, String message) {
        super(status, message);
    }
}

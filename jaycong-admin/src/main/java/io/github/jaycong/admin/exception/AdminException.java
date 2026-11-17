package io.github.jaycong.admin.exception;

import io.github.jaycong.core.exception.BusinessException;

/** 可向管理端展示的业务或远程调用错误，status 对应 HTTP 状态。 */
public class AdminException extends BusinessException {
    public AdminException(int status, String message) {
        super(status, message);
    }
}

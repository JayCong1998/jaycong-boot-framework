package io.github.jaycong.goods;

import io.github.jaycong.core.exception.BusinessException;

/** 商品模块可公开的业务失败，统一交给 common-web 返回既有错误状态。 */
public class GoodsException extends BusinessException {
    public GoodsException(int status, String message) {
        super(status, message);
    }

    public GoodsException(int status, String message, Throwable cause) {
        super(status, message, cause);
    }
}

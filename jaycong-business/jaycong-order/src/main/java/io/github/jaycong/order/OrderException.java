package io.github.jaycong.order;

import io.github.jaycong.core.exception.BusinessException;

/** 表达订单失败原因，通过公共异常契约交给 Web 层统一响应。 */
public class OrderException extends BusinessException {
    public enum Reason { INVALID_ORDER, GOODS_NOT_PURCHASABLE, GOODS_UNAVAILABLE }

    private final Reason reason;

    public OrderException(Reason reason, String message) {
        this(reason, message, null);
    }

    public OrderException(Reason reason, String message, Throwable cause) {
        super(switch (reason) {
            case INVALID_ORDER -> 400;
            case GOODS_NOT_PURCHASABLE -> 404;
            case GOODS_UNAVAILABLE -> 503;
        }, message, cause);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}

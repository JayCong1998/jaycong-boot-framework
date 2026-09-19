package io.github.jaycong.order;

import io.github.jaycong.core.error.ErrorCode;

public enum OrderErrorCode implements ErrorCode {
    STOCK_CALL_FAILED(21001, "库存服务调用失败"),
    DEMO_FAILURE(21002, "演示故障：库存已扣减，订单已回滚");

    private final int code;
    private final String message;
    OrderErrorCode(int code, String message) { this.code = code; this.message = message; }
    @Override public Integer getCode() { return code; }
    @Override public String getMessage() { return message; }
}

package io.github.jaycong.order.domain.entity;

/** 编码对应 trade_order.status，不能依赖枚举声明顺序持久化。 */
public enum OrderStatus {
    PENDING_PAYMENT(0), PAID(1), COMPLETED(2), CANCELLED(3);

    private final int code;

    OrderStatus(int code) { this.code = code; }

    public int getCode() { return code; }
}

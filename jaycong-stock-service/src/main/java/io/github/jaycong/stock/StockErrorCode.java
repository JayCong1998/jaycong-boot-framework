package io.github.jaycong.stock;

import io.github.jaycong.core.error.ErrorCode;

public enum StockErrorCode implements ErrorCode {
    INSUFFICIENT_STOCK;
    @Override public Integer getCode() { return 22001; }
    @Override public String getMessage() { return "库存不足"; }
}

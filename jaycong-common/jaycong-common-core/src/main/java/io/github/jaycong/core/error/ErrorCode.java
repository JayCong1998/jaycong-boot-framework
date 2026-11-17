package io.github.jaycong.core.error;

/** 业务模块可通过枚举实现此契约；错误码与 HTTP 状态无关。 */
public interface ErrorCode {
    Integer getCode();

    String getMessage();
}

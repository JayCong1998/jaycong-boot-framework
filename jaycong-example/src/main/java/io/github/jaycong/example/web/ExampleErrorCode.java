package io.github.jaycong.example.web;

import io.github.jaycong.core.error.ErrorCode;

public enum ExampleErrorCode implements ErrorCode {
    NAME_RESERVED;

    @Override
    public Integer getCode() { return 10001; }

    @Override
    public String getMessage() { return "该名称为系统保留名称"; }
}

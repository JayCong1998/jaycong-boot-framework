package io.github.jaycong.example.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 写入字段白名单：主键及审计字段不接受客户端赋值。 */
public record ProductRequest(@NotBlank @Size(max = 100) String name) { }

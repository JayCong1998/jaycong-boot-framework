package io.github.jaycong.api.goods.management.response;


/** 管理 RPC 与 Admin 共用的响应字段，不包含持久化模型。 */
public record GoodsCategoryView(String value, String label) {}

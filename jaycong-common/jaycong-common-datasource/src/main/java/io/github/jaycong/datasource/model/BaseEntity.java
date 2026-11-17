package io.github.jaycong.datasource.model;

/**
 * 旧包兼容入口，新数据库实体统一继承 core 模块中的基类。
 * @deprecated 使用 {@link io.github.jaycong.core.model.BaseEntity}。
 */
@Deprecated
public abstract class BaseEntity extends io.github.jaycong.core.model.BaseEntity {
}

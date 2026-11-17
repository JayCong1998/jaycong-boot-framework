package io.github.jaycong.api.goods.management.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理 RPC 与 Admin 共用的响应字段，不包含持久化模型。 */
public record ManagedGoodsView(String id, String goodsCode, String name, String subtitle,
        String category, String coverUrl,
        String description, BigDecimal price, BigDecimal originalPrice, int stock,
        int stockWarning, int sortOrder, int status, int lockVersion,
        LocalDateTime createTime, LocalDateTime updateTime) {}

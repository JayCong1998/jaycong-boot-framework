package io.github.jaycong.admin.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Admin HTTP 响应，与远程协议分别维护。 */
public record ManagedGoodsView(String id, String goodsCode, String name, String subtitle,
        String category, String coverUrl,
        String description, BigDecimal price, BigDecimal originalPrice, int stock,
        int stockWarning, int sortOrder, int status, int lockVersion,
        LocalDateTime createTime, LocalDateTime updateTime) {
    public static ManagedGoodsView from(io.github.jaycong.api.goods.management.response.ManagedGoodsView value) {
        return new ManagedGoodsView(
                value.id(),
                value.goodsCode(),
                value.name(),
                value.subtitle(),
                value.category(),
                value.coverUrl(),
                value.description(),
                value.price(),
                value.originalPrice(),
                value.stock(),
                value.stockWarning(),
                value.sortOrder(),
                value.status(),
                value.lockVersion(),
                value.createTime(),
                value.updateTime());
    }
}

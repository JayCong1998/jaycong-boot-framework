package io.github.jaycong.api.goods.model;

import java.math.BigDecimal;

/** ID 使用字符串传输，避免浏览器丢失 BIGINT 精度；价格从数据库的分转换为元。 */
public record GoodsView(String id, String goodsCode, String name, String subtitle,
                        BigDecimal price, String image, String description) {
}

package io.github.jaycong.goods.domain.entity;

import io.github.jaycong.goods.GoodsException;
import java.net.URI;

/** 商品资料使用分保存金额；图片仅保存地址，详情为纯文本。 */
public record GoodsDetails(String name, String goodsCode, String subtitle, String category,
        String coverUrl, String description, long price,
        Long originalPrice, int stockWarning, int sortOrder) {
    /** 写入和展示时规范化；重建历史资料时保留原字段，避免仅调库存就顺带改写资料。 */
    public GoodsDetails normalized() {
        return new GoodsDetails(text(name), text(goodsCode), text(subtitle), category,
                text(coverUrl), text(description), price, originalPrice, stockWarning, sortOrder);
    }
    /** 读取兼容未分配分类的旧商品，新增或维护时执行完整校验。 */
    public void validateForWrite() {
        if (name.isEmpty() || name.length() > 128) invalid("商品名称必填且不能超过128字");
        if (goodsCode.isEmpty() || goodsCode.length() > 64) invalid("商品编码必填且不能超过64字");
        if (subtitle.length() > 255 || description.length() > 20000) invalid("商品简介或详情过长");
        if (category == null || category.isBlank()) invalid("请选择商品分类");
        if (price <= 0 || price > 99999999999L) invalid("售价须在0.01至999999999.99元之间");
        if (originalPrice != null && (originalPrice < price || originalPrice > 99999999999L))
            invalid("划线价不能低于售价或超出金额范围");
        if (stockWarning < 0 || sortOrder < 0) invalid("库存预警及排序不能为负数");
        if (!coverUrl.isEmpty()) image(coverUrl);
    }
    public void requirePublishable() {
        validateForWrite();
        if (coverUrl.isEmpty()) invalid("上架前请设置商品主图");
    }
    private static String text(String value) { return value == null ? "" : value.strip(); }
    private static void image(String value) {
        if (value.isEmpty() || value.length() > 512 || value.contains("\\") || value.chars().anyMatch(Character::isWhitespace))
            invalid("图片须为有效的http(s)地址或站内路径，最多512字符");
        try {
            URI uri = URI.create(value);
            boolean local = value.startsWith("/") && !value.startsWith("//") && uri.getRawAuthority() == null;
            boolean remote = ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
            if (!local && !remote) invalid("图片地址仅支持http(s)地址或站内路径");
        } catch (IllegalArgumentException exception) { invalid("图片地址格式不正确"); }
    }
    private static void invalid(String message) { throw new GoodsException(400, message); }
}

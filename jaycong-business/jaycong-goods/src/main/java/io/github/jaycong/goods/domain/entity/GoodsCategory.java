package io.github.jaycong.goods.domain.entity;

/** 分类中文值直接保存到商品表；修改名称时需同步迁移已保存的商品分类。 */
public enum GoodsCategory {
    DIGITAL("手机数码"), OFFICE("电脑办公"), APPLIANCES("家用电器"),
    LIVING("家居生活"), FASHION("服饰鞋包"), OTHER("其他商品");

    private final String value;
    GoodsCategory(String value) { this.value = value; }
    public String value() { return value; }
    public static GoodsCategory find(String value) {
        if (value == null) return null;
        for (var category : values()) if (category.value.equals(value)) return category;
        return null;
    }
}

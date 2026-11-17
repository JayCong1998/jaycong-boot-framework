package io.github.jaycong.admin.response;


/** Admin HTTP 响应，与远程协议分别维护。 */
public record GoodsCategoryView(String value, String label) {
    public static GoodsCategoryView from(io.github.jaycong.api.goods.management.response.GoodsCategoryView value) {
        return new GoodsCategoryView(value.value(), value.label());
    }
}

package io.github.jaycong.goods.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.jaycong.api.goods.management.request.GoodsSaveRequest;
import io.github.jaycong.core.model.BaseEntity;
import io.github.jaycong.goods.GoodsException;
import io.github.jaycong.goods.domain.entity.convertor.GoodsConvertor;
import lombok.Getter;

/** 商品实体同时承载表映射与业务行为，业务字段不开放公共 setter。 */
@Getter
@TableName("goods")
public class Goods extends BaseEntity {
    private String name;
    private String goodsCode;
    private String subtitle;
    private Long price;
    private String coverUrl;
    private String description;
    private Integer status;
    private Integer stock;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String category;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long originalPrice;
    private Integer stockWarning;
    private Integer sortOrder;

    /** 新商品默认下架、零库存；主键在首次持久化时由应用侧生成。 */
    public static Goods create(GoodsSaveRequest request) {
        var details = GoodsConvertor.toDetails(request).normalized();
        details.validateForWrite();
        requireValidCategory(details.category());
        var goods = new Goods();
        goods.applyDetails(details);
        goods.stock = 0;
        goods.status = 0;
        goods.setLockVersion(0);
        return goods;
    }

    public void changeDetails(GoodsSaveRequest request) {
        var details = GoodsConvertor.toDetails(request).normalized();
        details.validateForWrite();
        if (!goodsCode.equals(details.goodsCode())) {
            throw new GoodsException(400, "商品编码创建后不能修改");
        }
        requireValidCategory(details.category());
        if (status == 1) {
            details.requirePublishable();
        }
        applyDetails(details);
    }

    public void changeStatus(int status) {
        if (status != 0 && status != 1) {
            throw new GoodsException(400, "状态只能是0下架或1上架");
        }
        if (status == 1) {
            details().normalized().requirePublishable();
            requireValidCategory(category);
        }
        this.status = status;
    }

    public void adjustStock(int quantity) {
        if (quantity == 0) {
            throw new GoodsException(400, "库存调整数量不能为0");
        }
        long result = (long) stock + quantity;
        if (result < 0 || result > Integer.MAX_VALUE) {
            throw new GoodsException(409, "调整后库存不能为负数或超出范围");
        }
        stock = (int) result;
    }

    public void requireDeletable() {
        if (status != 0) {
            throw new GoodsException(409, "请先下架商品再删除");
        }
    }

    public void requireVersion(int version) {
        if (version < 0 || getLockVersion() != version) {
            throw new GoodsException(409, "数据已发生变化，请刷新后重试");
        }
    }

    private static void requireValidCategory(String category) {
        if (GoodsCategory.find(category) == null) {
            throw new GoodsException(400, "商品分类不合法，请重新选择");
        }
    }

    /** 读取原始资料，不在库存或状态更新时顺带改写历史文本。 */
    public GoodsDetails details() {
        return new GoodsDetails(name, goodsCode, subtitle, category, coverUrl, description,
                price, originalPrice, stockWarning, sortOrder);
    }

    private void applyDetails(GoodsDetails details) {
        name = details.name();
        goodsCode = details.goodsCode();
        subtitle = details.subtitle();
        category = details.category();
        coverUrl = details.coverUrl();
        description = details.description();
        price = details.price();
        originalPrice = details.originalPrice();
        stockWarning = details.stockWarning();
        sortOrder = details.sortOrder();
    }
}

package io.github.jaycong.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.jaycong.core.model.BaseEntity;
import io.github.jaycong.order.OrderException;
import io.github.jaycong.order.OrderException.Reason;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Getter;

/** 订单实体同时承载表映射与创建规则，业务字段不开放公共 setter。 */
@Getter
@TableName("trade_order")
public class Order extends BaseEntity {
    private static final Duration PAYMENT_WINDOW = Duration.ofMinutes(30);

    private String orderNo;
    private Long userId;
    private Long goodsId;
    private String goodsCode;
    private String goodsName;
    private String goodsCoverUrl;
    private Long totalAmount;
    private Integer status;
    private String remark;
    private LocalDateTime expireTime;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;

    /** 创建单商品待支付订单；支付期限与成交快照由服务端确定。 */
    public static Order create(long id, long userId, GoodsSnapshot goods, String remark, LocalDateTime now) {
        if (id <= 0 || userId <= 0 || goods == null || now == null) {
            throw new OrderException(Reason.INVALID_ORDER, "订单标识、用户或商品无效");
        }
        if (remark != null && remark.length() > 500) {
            throw new OrderException(Reason.INVALID_ORDER, "订单备注不能超过500个字符");
        }
        var order = new Order();
        order.setId(id);
        order.orderNo = "JC" + id;
        order.userId = userId;
        order.goodsId = goods.goodsId();
        order.goodsCode = goods.goodsCode();
        order.goodsName = goods.goodsName();
        order.goodsCoverUrl = goods.goodsCoverUrl();
        order.totalAmount = goods.priceInCents();
        order.status = OrderStatus.PENDING_PAYMENT.getCode();
        order.remark = remark == null ? "" : remark.strip();
        order.expireTime = now.plus(PAYMENT_WINDOW);
        order.cancelReason = "";
        // 下单人是可信身份，其余审计、逻辑删除及版本字段由公共能力填充。
        order.setCreateUserId(userId);
        order.setUpdateUserId(userId);
        return order;
    }
}

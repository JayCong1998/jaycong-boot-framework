package io.github.jaycong.api.order.response;

import java.time.LocalDateTime;

/** ID 使用字符串避免浏览器精度损失，totalAmount 单位为人民币分。 */
public record OrderResponse(String id, String orderNo, String goodsId, String goodsCode,
        String goodsName, String goodsCoverUrl, long totalAmount, int status,
        String remark, LocalDateTime expireTime) {}

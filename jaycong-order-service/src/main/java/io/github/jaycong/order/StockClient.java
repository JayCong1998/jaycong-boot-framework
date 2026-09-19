package io.github.jaycong.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jaycong.core.exception.BusinessException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 只依赖 HTTP 契约；不会重试或补偿已提交的库存扣减。 */
public class StockClient {
    private final RestClient http;
    private final ObjectMapper json;
    public StockClient(RestClient http, ObjectMapper json) { this.http = http; this.json = json; }

    public void deduct(long productId, int quantity) {
        try {
            var result = http.post().uri("/stocks/deduct").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("productId", productId, "quantity", quantity))
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            String message = switch (status) {
                                case 404 -> "商品库存不存在";
                                case 422 -> "库存服务拒绝扣减：库存不足";
                                default -> "库存服务调用失败";
                            };
                            throw new BusinessException(OrderErrorCode.STOCK_CALL_FAILED, message);
                        }
                        return json.readTree(response.getBody());
                    });
            // Result 的 success/code 均检查，空体、缺字段、错误类型不能视为成功。
            if (result == null || !result.path("success").isBoolean() || !result.path("success").booleanValue()
                    || !result.path("code").isIntegralNumber() || !result.path("code").canConvertToInt()
                    || result.path("code").intValue() != 0) {
                throw new BusinessException(OrderErrorCode.STOCK_CALL_FAILED, "库存服务返回失败或无效响应");
            }
        } catch (RestClientException e) {
            throw new BusinessException(OrderErrorCode.STOCK_CALL_FAILED, "库存服务不可用、响应异常或调用超时", e);
        }
    }
}

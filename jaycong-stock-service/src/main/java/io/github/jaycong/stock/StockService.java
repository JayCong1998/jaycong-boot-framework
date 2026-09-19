package io.github.jaycong.stock;

import io.github.jaycong.core.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StockService {
    private final StockMapper mapper;
    public StockService(StockMapper mapper) { this.mapper = mapper; }

    public Stock get(long productId) {
        var stock = mapper.selectById(productId);
        if (stock == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return stock;
    }

    @Transactional
    public void deduct(DeductRequest request) {
        // 数量条件与扣减在同一条 SQL 中完成，避免先查询再更新的并发竞争。
        if (mapper.deduct(request.productId(), request.quantity()) == 0) {
            get(request.productId());
            throw new BusinessException(StockErrorCode.INSUFFICIENT_STOCK);
        }
    }
}

package io.github.jaycong.api.goods.client;

import io.github.jaycong.api.goods.facade.GoodsFacade;
import io.github.jaycong.api.goods.model.GoodsView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 由调用方显式启用；URL 为空时通过 goods 服务名负载均衡。 */
@FeignClient(name = "goods", contextId = "goodsRead", url = "${jaycong.rpc.goods.url:}")
public interface GoodsFeignClient extends GoodsFacade {
    @Override
    @GetMapping("/internal/goods/{id}")
    GoodsView getAvailable(@PathVariable("id") long id);
}

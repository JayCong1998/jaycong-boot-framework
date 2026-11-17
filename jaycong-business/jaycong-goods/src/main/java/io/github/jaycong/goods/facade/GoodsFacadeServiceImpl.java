package io.github.jaycong.goods.facade;

import io.github.jaycong.api.goods.facade.GoodsFacade;
import io.github.jaycong.api.goods.model.GoodsView;
import io.github.jaycong.goods.domain.entity.convertor.GoodsConvertor;
import io.github.jaycong.goods.domain.service.GoodsReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 服务间商品读取入口，与公开详情复用上架及逻辑删除筛选，不返回管理字段。 */
@RestController
@RequestMapping("/internal/goods")
public class GoodsFacadeServiceImpl implements GoodsFacade {
    private final GoodsReadService goods;

    public GoodsFacadeServiceImpl(GoodsReadService goods) {
        this.goods = goods;
    }

    @Override
    @GetMapping("/{id}")
    public GoodsView getAvailable(@PathVariable("id") long id) {
        return GoodsConvertor.toView(goods.getAvailable(id));
    }
}

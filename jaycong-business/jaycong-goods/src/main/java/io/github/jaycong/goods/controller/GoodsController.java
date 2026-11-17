package io.github.jaycong.goods.controller;

import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.core.model.Result;
import io.github.jaycong.api.goods.model.GoodsView;
import io.github.jaycong.goods.domain.entity.convertor.GoodsConvertor;
import io.github.jaycong.goods.domain.service.GoodsReadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goods")
public class GoodsController {
    private final GoodsReadService goodsService;

    public GoodsController(GoodsReadService goodsService) {
        this.goodsService = goodsService;
    }

    /** 分页查询公开商品，参数绑定和校验失败由公共 Web 模块返回 HTTP 400。 */
    @GetMapping
    public Result<PageResult<GoodsView>> page(@Valid @ModelAttribute GoodsQueryRequest query) {
        var result = goodsService.pageAvailable(query.getPage(), query.getSize(), query.getName());
        return Result.success(new PageResult<>(result.total(), result.rows().stream().map(GoodsConvertor::toView).toList()));
    }
    @GetMapping("/{id}")
    public Result<GoodsView> detail(@PathVariable long id) {
        return Result.success(GoodsConvertor.toView(goodsService.getAvailable(id)));
    }
}

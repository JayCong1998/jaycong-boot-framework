package io.github.jaycong.goods.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.datasource.pagination.MybatisPages;
import io.github.jaycong.goods.GoodsException;
import io.github.jaycong.goods.domain.entity.Goods;
import io.github.jaycong.goods.domain.entity.GoodsCategory;
import io.github.jaycong.goods.infrastructure.mapper.GoodsMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 集中提供商品读取和筛选，直接通过 Mapper 查询实体。 */
@Service
public class GoodsReadService {
    private final GoodsMapper mapper;

    public GoodsReadService(GoodsMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<Goods> pageAvailable(int page, int size, String name) {
        if (page < 1 || size < 1 || size > PageRequest.MAX_SIZE) {
            throw new GoodsException(400, "页码至少1，每页1至1000条");
        }
        if (name != null && name.length() > 128) {
            throw new GoodsException(400, "商品名称筛选不能超过128字");
        }
        var condition = Wrappers.<Goods>lambdaQuery().eq(Goods::getStatus, 1);
        if (StringUtils.hasText(name)) {
            condition.like(Goods::getName, literal(name));
        }
        // 唯一 ID 为排序值和创建时间相同的商品提供稳定顺序。
        condition.orderByAsc(Goods::getSortOrder).orderByDesc(Goods::getCreateTime).orderByDesc(Goods::getId);
        var result = mapper.selectPage(MybatisPages.<Goods>toPage(new PageRequest(page, size)), condition);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public Goods getAvailable(long id) {
        var goods = mapper.selectOne(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getId, id).eq(Goods::getStatus, 1));
        if (goods == null) {
            throw new GoodsException(404, "商品不存在或未上架");
        }
        return goods;
    }

    /** 管理查询由 GoodsManageService 完成权限核验后调用，不作为公开查询使用。 */
    public PageResult<Goods> pageManaged(int page, int size, String name, String goodsCode,
            String category, Integer status, boolean lowStock) {
        if (page < 1 || size < 1 || size > 100) {
            throw new GoodsException(400, "页码至少1，每页1至100条");
        }
        if ((name != null && name.length() > 128) || (goodsCode != null && goodsCode.length() > 64)) {
            throw new GoodsException(400, "筛选条件过长");
        }
        if (category != null && GoodsCategory.find(category) == null) {
            throw new GoodsException(400, "商品分类不合法");
        }
        if (status != null && status != 0 && status != 1) {
            throw new GoodsException(400, "商品状态不合法");
        }
        var condition = Wrappers.<Goods>lambdaQuery();
        if (StringUtils.hasText(name)) {
            condition.like(Goods::getName, literal(name));
        }
        if (StringUtils.hasText(goodsCode)) {
            condition.eq(Goods::getGoodsCode, goodsCode.strip());
        }
        if (category != null) {
            condition.eq(Goods::getCategory, category);
        }
        if (status != null) {
            condition.eq(Goods::getStatus, status);
        }
        if (lowStock) {
            condition.apply("stock <= stock_warning");
        }
        condition.orderByAsc(Goods::getSortOrder).orderByDesc(Goods::getCreateTime).orderByDesc(Goods::getId);
        var result = mapper.selectPage(MybatisPages.<Goods>toPage(new PageRequest(page, size)), condition);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public Goods getGoods(long id) {
        var goods = id > 0 ? mapper.selectById(id) : null;
        if (goods == null) {
            throw new GoodsException(404, "商品不存在");
        }
        return goods;
    }

    private String literal(String value) {
        // MySQL 默认使用反斜杠转义 LIKE 通配符，保留按字面包含匹配的筛选行为。
        return value.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

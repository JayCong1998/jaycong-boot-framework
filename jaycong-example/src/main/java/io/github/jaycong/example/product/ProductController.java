package io.github.jaycong.example.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.core.model.Result;
import io.github.jaycong.mybatis.pagination.MybatisPages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 最小 CRUD 示例；复杂业务可在 Controller 与 Mapper 之间增加事务 Service。
 */
@RestController
@RequestMapping("/example/products")
public class ProductController {
    private final ProductMapper mapper;

    public ProductController(ProductMapper mapper) {
        this.mapper = mapper;
    }

    @PostMapping
    public Result<Product> create(@Valid @RequestBody ProductRequest request) {
        var product = new Product();
        product.setName(request.name().strip());
        mapper.insert(product);
        return Result.success(product);
    }

    @GetMapping("/{id}")
    public Result<Product> get(@PathVariable Long id) {
        var product = mapper.selectById(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Result.success(product);
    }

    @GetMapping
    public Result<PageResult<Product>> list(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(PageRequest.MAX_SIZE) int size) {
        var result = mapper.selectPage(MybatisPages.<Product>toPage(new PageRequest(page, size)),
            new LambdaQueryWrapper<Product>().orderByAsc(Product::getId));
        return Result.success(MybatisPages.toResult(result));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        var product = new Product();
        product.setId(id);
        product.setName(request.name().strip());
        if (mapper.updateById(product) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (mapper.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Result.success();
    }
}
